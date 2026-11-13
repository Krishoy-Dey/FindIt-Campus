package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CampusItem
import com.example.data.model.ItemClaim
import com.example.data.model.ItemStatus
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FindItRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCampusItems(): Flow<List<CampusItem>> = flow {
        val uid = requireUserId()
        val path = COLLECTION_ITEMS
        emitAll(
            db.collection(COLLECTION_ITEMS)
                .whereEqualTo("visibility", VISIBILITY_CAMPUS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyItems(userId: String): Flow<List<CampusItem>> = flow {
        val path = COLLECTION_ITEMS
        emitAll(
            db.collection(COLLECTION_ITEMS)
                .whereEqualTo("reporterId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CampusItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeItem(itemId: String): Flow<CampusItem?> = flow {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CampusItem::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, docRef.path, uid)
                    throw error
                }
        )
    }

    fun observeClaimsForItem(
        itemId: String,
        currentUserId: String,
        isReporter: Boolean
    ): Flow<List<ItemClaim>> = flow {
        val claimsRef = db.collection(COLLECTION_ITEMS).document(itemId).collection(SUBCOLLECTION_CLAIMS)
        val filterField = if (isReporter) "itemReporterId" else "claimantId"
        emitAll(
            claimsRef.whereEqualTo(filterField, currentUserId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ItemClaim::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, claimsRef.path, currentUserId)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        val notifRef = db.collection(COLLECTION_USERS).document(userId).collection(SUBCOLLECTION_NOTIFICATIONS)
        emitAll(
            notifRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserNotification::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, notifRef.path, userId)
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val userDoc = db.collection(COLLECTION_USERS).document(userId)
        emitAll(
            userDoc.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, userDoc.path, userId)
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(
        displayName: String,
        email: String,
        photoUrl: String?,
        phoneNumber: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        try {
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Campus Student" }.take(80)
            val safeEmail = email.trim().ifBlank { "student@campus.edu" }.take(120)
            if (!existing.exists()) {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!phoneNumber.isNullOrBlank()) {
                    createPayload["phoneNumber"] = phoneNumber.trim().take(30)
                }
                if (!photoUrl.isNullOrBlank()) {
                    createPayload["photoUrl"] = photoUrl.trim().take(500)
                }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!phoneNumber.isNullOrBlank()) {
                    updatePayload["phoneNumber"] = phoneNumber.trim().take(30)
                }
                if (!photoUrl.isNullOrBlank()) {
                    updatePayload["photoUrl"] = photoUrl.trim().take(500)
                }
                docRef.update(updatePayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateSavedPhone(phoneNumber: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        try {
            docRef.update(
                mapOf(
                    "phoneNumber" to phoneNumber.trim().take(30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun createCampusItem(
        reporterName: String,
        title: String,
        description: String,
        category: String,
        type: String,
        location: String,
        contactPhone: String,
        photoBase64: String?
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val itemId = UUID.randomUUID().toString()
        val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
        val payload = mutableMapOf<String, Any>(
            "reporterId" to uid,
            "reporterName" to reporterName.trim().ifBlank { "Campus Student" }.take(80),
            "title" to title.trim().take(100),
            "description" to description.trim().take(1000),
            "category" to category,
            "type" to type,
            "status" to ItemStatus.OPEN.key,
            "location" to location.trim().take(120),
            "contactPhone" to contactPhone.trim().take(25),
            "visibility" to VISIBILITY_CAMPUS,
            "claimCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoBase64.isNullOrBlank()) {
            payload["photoBase64"] = photoBase64
        }
        try {
            docRef.set(payload).await()
            itemId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateCampusItemStatus(itemId: String, newStatus: ItemStatus): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
        try {
            docRef.update(
                mapOf(
                    "status" to newStatus.key,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCampusItem(itemId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection(COLLECTION_ITEMS).document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    suspend fun submitClaimAndNotify(
        item: CampusItem,
        claimantName: String,
        claimantPhone: String,
        message: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val claimId = UUID.randomUUID().toString()
        val itemRef = db.collection(COLLECTION_ITEMS).document(item.id)
        val claimRef = itemRef.collection(SUBCOLLECTION_CLAIMS).document(claimId)

        val safeName = claimantName.trim().ifBlank { "Campus Student" }.take(80)
        val safePhone = claimantPhone.trim().take(25)
        val safeMessage = message.trim().take(400)

        val claimPayload = mapOf(
            "itemId" to item.id,
            "itemReporterId" to item.reporterId,
            "claimantId" to uid,
            "claimantName" to safeName,
            "claimantPhone" to safePhone,
            "message" to safeMessage,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            claimRef.set(claimPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, claimRef.path, auth)
            throw e
        }

        // Increment claimCount on parent item
        try {
            itemRef.update(
                mapOf(
                    "claimCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, itemRef.path, auth)
            throw e
        }

        // Deliver real-time notification to item reporter
        val notifId = UUID.randomUUID().toString()
        val notifRef = db.collection(COLLECTION_USERS)
            .document(item.reporterId)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
            .document(notifId)

        val notifTitle = if (item.type == "lost") {
            "$safeName may have found your ${item.title}"
        } else {
            "$safeName wants to claim ${item.title}"
        }.take(120)

        val notifPayload = mapOf(
            "recipientId" to item.reporterId,
            "senderId" to uid,
            "senderName" to safeName,
            "senderPhone" to safePhone,
            "itemId" to item.id,
            "itemTitle" to item.title.take(100),
            "type" to "claim_received",
            "title" to notifTitle,
            "body" to safeMessage,
            "read" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            notifRef.set(notifPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, notifRef.path, auth)
            throw e
        }

        claimId
    }

    suspend fun updateClaimStatus(
        itemId: String,
        claimId: String,
        newStatus: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val claimRef = db.collection(COLLECTION_ITEMS)
            .document(itemId)
            .collection(SUBCOLLECTION_CLAIMS)
            .document(claimId)
        try {
            claimRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, claimRef.path, auth)
            throw e
        }
    }

    suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> = runCatching {
        requireUserId()
        val notifRef = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
            .document(notificationId)
        try {
            notifRef.update("read", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, notifRef.path, auth)
            throw e
        }
    }

    suspend fun markAllNotificationsRead(
        userId: String,
        unreadIds: List<String>
    ): Result<Unit> = runCatching {
        requireUserId()
        if (unreadIds.isEmpty()) return@runCatching
        val userNotifs = db.collection(COLLECTION_USERS)
            .document(userId)
            .collection(SUBCOLLECTION_NOTIFICATIONS)
        val batch = db.batch()
        unreadIds.forEach { id ->
            batch.update(userNotifs.document(id), "read", true)
        }
        try {
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userNotifs.path, auth)
            throw e
        }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        uid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
    }

    companion object {
        const val COLLECTION_ITEMS = "campus_items"
        const val SUBCOLLECTION_CLAIMS = "claims"
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_NOTIFICATIONS = "notifications"
        const val VISIBILITY_CAMPUS = "campus"
    }
}
