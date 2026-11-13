package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ItemType(val key: String, val label: String) {
    LOST("lost", "Lost"),
    FOUND("found", "Found");

    companion object {
        fun fromKey(key: String): ItemType =
            entries.firstOrNull { it.key == key } ?: LOST
    }
}

enum class ItemStatus(val key: String, val label: String) {
    OPEN("open", "Open"),
    CLAIMED("claimed", "Claimed"),
    RECOVERED("recovered", "Recovered");

    companion object {
        fun fromKey(key: String): ItemStatus =
            entries.firstOrNull { it.key == key } ?: OPEN
    }
}

enum class ItemCategory(val key: String, val label: String) {
    ELECTRONICS("electronics", "Electronics"),
    ID_KEYS("id_keys", "ID & Keys"),
    BAGS("bags", "Bags"),
    BOOKS("books", "Books"),
    ACCESSORIES("accessories", "Accessories"),
    CLOTHING("clothing", "Clothing"),
    OTHER("other", "Other");

    companion object {
        fun fromKey(key: String): ItemCategory =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}

@IgnoreExtraProperties
data class CampusItem(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = ItemCategory.OTHER.key,
    val type: String = ItemType.LOST.key,
    val status: String = ItemStatus.OPEN.key,
    val location: String = "",
    val contactPhone: String = "",
    val photoBase64: String? = null,
    val visibility: String = "campus",
    val claimCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val itemType: ItemType get() = ItemType.fromKey(type)
    val itemStatus: ItemStatus get() = ItemStatus.fromKey(status)
    val itemCategory: ItemCategory get() = ItemCategory.fromKey(category)
}

@IgnoreExtraProperties
data class ItemClaim(
    @DocumentId val id: String = "",
    val itemId: String = "",
    val itemReporterId: String = "",
    val claimantId: String = "",
    val claimantName: String = "",
    val claimantPhone: String = "",
    val message: String = "",
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhone: String? = null,
    val itemId: String = "",
    val itemTitle: String = "",
    val type: String = "claim_received",
    val title: String = "",
    val body: String = "",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val notificationsEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
