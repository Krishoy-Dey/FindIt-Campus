package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ItemCategory
import com.example.data.model.ItemStatus
import com.example.data.model.ItemType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FindItRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createCampusItem_validPayload_createsAndObservesInRealtime() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FindItRepository(firestore, auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCampusItem(
                reporterName = "Alice Student",
                title = "Black Sony Headphones",
                description = "Left on the 3rd floor desk in Engineering Hall.",
                category = ItemCategory.ELECTRONICS.key,
                type = ItemType.LOST.key,
                location = "Engineering Hall",
                contactPhone = "+1 555-234-5678",
                photoBase64 = null
            )
        }
        assertTrue(createResult.isSuccess)
        val itemId = createResult.getOrThrow()
        assertTrue(itemId.isNotBlank())

        val items = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCampusItems().first { list -> list.any { it.id == itemId } }
        }
        val created = items.first { it.id == itemId }
        assertEquals("Black Sony Headphones", created.title)
        assertEquals("+1 555-234-5678", created.contactPhone)
        assertEquals(ItemStatus.OPEN, created.itemStatus)
    }

    @Test
    fun submitClaimAndNotify_crossUser_createsClaimAndNotification() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = FindItRepository(firestore, auth)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCampusItem(
                reporterName = "Alice",
                title = "Blue Campus ID Card",
                description = "Dropped near Cafeteria.",
                category = ItemCategory.ID_KEYS.key,
                type = ItemType.LOST.key,
                location = "Cafeteria",
                contactPhone = "+1 555-100-2000",
                photoBase64 = null
            ).getOrThrow()
        }

        val item = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeItem(itemId).first { it != null }!!
        }

        // Bob finds the item and submits a claim + notification
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = FindItRepository(firestore, auth)
        val claimResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.submitClaimAndNotify(
                item = item,
                claimantName = "Bob Finder",
                claimantPhone = "+1 555-999-8888",
                message = "Found it at the Cafeteria counter!"
            )
        }
        assertTrue(claimResult.isSuccess)

        // Alice sees the claim and real-time notification
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val claims = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeClaimsForItem(itemId, aliceUid, isReporter = true)
                .first { it.isNotEmpty() }
        }
        assertEquals(bobUid, claims.first().claimantId)
        assertEquals("+1 555-999-8888", claims.first().claimantPhone)

        val notifications = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeNotifications(aliceUid).first { it.any { n -> n.itemId == itemId } }
        }
        assertTrue(notifications.any { it.itemId == itemId && !it.read })
    }

    @Test
    fun updateCampusItem_crossUserOrTerminalState_failsWithPermissionDenied() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = FindItRepository(firestore, auth)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCampusItem(
                reporterName = "Alice",
                title = "Graphing Calculator",
                description = "TI-84 in Math Building.",
                category = ItemCategory.ELECTRONICS.key,
                type = ItemType.FOUND.key,
                location = "Math Building",
                contactPhone = "+1 555-321-7654",
                photoBase64 = null
            ).getOrThrow()
        }

        // Bob cannot delete Alice's item
        signInTestUser(BOB_EMAIL)
        val bobRepo = FindItRepository(firestore, auth)
        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deleteCampusItem(itemId) }
        assertTrue(deleteResult.isFailure)
        val deleteEx = deleteResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(deleteEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, deleteEx?.code)

        // Alice marks as recovered (terminal state)
        signInTestUser(ALICE_EMAIL)
        val recoverResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateCampusItemStatus(itemId, ItemStatus.RECOVERED)
        }
        assertTrue(recoverResult.isSuccess)

        // Subsequent update on recovered item fails
        val reopenResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.updateCampusItemStatus(itemId, ItemStatus.OPEN)
        }
        assertTrue(reopenResult.isFailure)
        val reopenEx = reopenResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(reopenEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, reopenEx?.code)
    }

    @Test
    fun unauthenticatedQuery_campusItems_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection(FindItRepository.COLLECTION_ITEMS)
                    .whereEqualTo("visibility", FindItRepository.VISIBILITY_CAMPUS)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                firestoreException?.code
            )
        }
    }

    @Test
    fun userProfile_crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FindItRepository(firestore, auth)
        val upsertResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertUserProfile(
                displayName = "Alice",
                email = ALICE_EMAIL,
                photoUrl = null,
                phoneNumber = "+1 555-777-1234"
            )
        }
        assertTrue(upsertResult.isSuccess)

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(FindItRepository.COLLECTION_USERS)
                    .document(aliceUid)
                    .get()
                    .await()
            }
            fail("Expected PERMISSION_DENIED reading another user's profile")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull(firestoreException)
            assertEquals(
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
                firestoreException?.code
            )
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_findit@test.com"
        const val BOB_EMAIL = "bob_findit@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
