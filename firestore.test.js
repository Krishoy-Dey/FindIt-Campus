const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validItemPayload(reporterId = ALICE_UID, overrides = {}) {
  return {
    reporterId,
    reporterName: "Alice Student",
    title: "Silver MacBook Charger",
    description: "Left near Main Library 2nd floor outlets.",
    category: "electronics",
    type: "lost",
    status: "open",
    location: "Main Library",
    contactPhone: "+1 555-234-5678",
    visibility: "campus",
    claimCount: 0,
    createdAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
    ...overrides,
  };
}

// --- 1. Unauthenticated Denials ---
test("Unauthenticated user: cannot list campus_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("campus_items").where("visibility", "==", "campus").get()
  );
});

test("Unauthenticated user: cannot create campus_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("campus_items").doc("item_1").set(validItemPayload(ALICE_UID))
  );
});

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

// --- 2. User Profile PII Isolation ---
test("Authenticated user: can create and read own profile, cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@campus.edu",
      phoneNumber: "+1 555-111-2222",
      notificationsEnabled: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      email: "hack@campus.edu",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

// --- 3. Campus Items CRUD, Query Alignment, Shadow Update & Terminal State Locking ---
test("Authenticated user: can create and query campus_items with visibility filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("campus_items").doc("item_1").set(validItemPayload(ALICE_UID))
  );

  // Client query matching visibility == campus succeeds
  await assertSucceeds(
    bobDb.collection("campus_items").where("visibility", "==", "campus").get()
  );

  // Client query matching reporterId == ALICE_UID succeeds for Alice
  await assertSucceeds(
    aliceDb.collection("campus_items").where("reporterId", "==", ALICE_UID).get()
  );

  // Unfiltered collection scan fails (rules are not filters)
  await assertFails(bobDb.collection("campus_items").get());
});

test("Shadow update & identity spoofing: rejected on campus_items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Cannot spoof reporterId
  await assertFails(
    bobDb.collection("campus_items").doc("item_spoof").set(validItemPayload(ALICE_UID))
  );

  // Cannot inject ghost field
  await assertFails(
    aliceDb
      .collection("campus_items")
      .doc("item_ghost")
      .set(validItemPayload(ALICE_UID, { isVerified: true }))
  );

  await assertSucceeds(
    aliceDb.collection("campus_items").doc("item_1").set(validItemPayload(ALICE_UID))
  );

  // Bob cannot edit Alice's item title or delete it
  await assertFails(
    bobDb.collection("campus_items").doc("item_1").update({
      title: "Hacked Title",
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(bobDb.collection("campus_items").doc("item_1").delete());
});

test("Terminal state locking: recovered items cannot be updated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("campus_items").doc("item_1").set(validItemPayload(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb.collection("campus_items").doc("item_1").update({
      status: "recovered",
      updatedAt: pastTimestamp(),
    })
  );

  // Subsequent update on recovered item fails
  await assertFails(
    aliceDb.collection("campus_items").doc("item_1").update({
      status: "open",
      updatedAt: pastTimestamp(),
    })
  );
});

// --- 4. Claims Subcollection & Notifications ---
test("Claims & Notifications: finder can submit claim and notify reporter, cross-user read blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("campus_items").doc("item_1").set(validItemPayload(ALICE_UID))
  );

  // Bob submits a claim on Alice's item
  await assertSucceeds(
    bobDb
      .collection("campus_items")
      .doc("item_1")
      .collection("claims")
      .doc("claim_1")
      .set({
        itemId: "item_1",
        itemReporterId: ALICE_UID,
        claimantId: BOB_UID,
        claimantName: "Bob Finder",
        claimantPhone: "+1 555-987-6543",
        message: "Found it at Library desk!",
        status: "pending",
        createdAt: pastTimestamp(),
        updatedAt: pastTimestamp(),
      })
  );

  // Bob increments claimCount by 1
  await assertSucceeds(
    bobDb.collection("campus_items").doc("item_1").update({
      claimCount: 1,
      updatedAt: pastTimestamp(),
    })
  );

  // Bob creates notification for Alice referencing item_1
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notifications")
      .doc("notif_1")
      .set({
        recipientId: ALICE_UID,
        senderId: BOB_UID,
        senderName: "Bob Finder",
        senderPhone: "+1 555-987-6543",
        itemId: "item_1",
        itemTitle: "Silver MacBook Charger",
        type: "claim_received",
        title: "Pickup update from Bob Finder",
        body: "Found it at Library desk!",
        read: false,
        createdAt: pastTimestamp(),
      })
  );

  // Alice can read claims for her item and her notifications
  await assertSucceeds(
    aliceDb
      .collection("campus_items")
      .doc("item_1")
      .collection("claims")
      .where("itemReporterId", "==", ALICE_UID)
      .get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notifications").get()
  );

  // Bob cannot read Alice's notifications
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("notifications").get()
  );

  // Alice can mark her notification read
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notifications")
      .doc("notif_1")
      .update({ read: true })
  );
});
