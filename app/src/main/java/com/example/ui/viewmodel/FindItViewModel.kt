package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CampusItem
import com.example.data.model.ItemCategory
import com.example.data.model.ItemClaim
import com.example.data.model.ItemStatus
import com.example.data.model.ItemType
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.repository.FindItRepository
import com.example.data.repository.PreferencesRepository
import com.example.util.CampusNotificationHelper
import com.example.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class FeedFilter(val label: String) {
    ALL("All"),
    LOST("Lost"),
    FOUND("Found"),
    RECOVERED("Recovered")
}

data class FeedFiltersState(
    val searchQuery: String = "",
    val feedFilter: FeedFilter = FeedFilter.ALL,
    val selectedCategory: ItemCategory? = null
)

class FindItViewModel(
    private val repository: FindItRepository,
    private val preferences: PreferencesRepository,
    private val appContext: Context,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userPhotoUrl: String?
) : ViewModel() {

    private val _filters = MutableStateFlow(FeedFiltersState())
    val filters: StateFlow<FeedFiltersState> = _filters.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _isSubmittingReport = MutableStateFlow(false)
    val isSubmittingReport: StateFlow<Boolean> = _isSubmittingReport.asStateFlow()

    private val _isSubmittingClaim = MutableStateFlow(false)
    val isSubmittingClaim: StateFlow<Boolean> = _isSubmittingClaim.asStateFlow()

    val savedPhoneNumber: StateFlow<String> = preferences.savedPhoneNumber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val systemNotificationsEnabled: StateFlow<Boolean> = preferences.systemNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    private val knownCampusItemIds = mutableSetOf<String>()
    private var campusFeedSeeded = false

    private val knownNotificationIds = mutableSetOf<String>()
    private var notificationsSeeded = false

    val campusItemsState: StateFlow<UiState<List<CampusItem>>> = repository.observeCampusItems()
        .map<List<CampusItem>, UiState<List<CampusItem>>> { items ->
            detectNewCampusItemsForAlerts(items)
            UiState.Success(items)
        }
        .catch { error ->
            val root = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Error observing campus items", root)
            emit(UiState.Error(root.message ?: "Failed to load campus items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val filteredItemsState: StateFlow<UiState<List<CampusItem>>> = combine(
        campusItemsState,
        _filters
    ) { state, filterState ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val query = filterState.searchQuery.trim().lowercase()
                val filtered = state.data.filter { item ->
                    val matchesTypeOrStatus = when (filterState.feedFilter) {
                        FeedFilter.ALL -> item.itemStatus != ItemStatus.RECOVERED
                        FeedFilter.LOST -> item.itemType == ItemType.LOST && item.itemStatus != ItemStatus.RECOVERED
                        FeedFilter.FOUND -> item.itemType == ItemType.FOUND && item.itemStatus != ItemStatus.RECOVERED
                        FeedFilter.RECOVERED -> item.itemStatus == ItemStatus.RECOVERED
                    }
                    val matchesCategory = filterState.selectedCategory == null ||
                        item.itemCategory == filterState.selectedCategory
                    val matchesSearch = query.isEmpty() ||
                        item.title.lowercase().contains(query) ||
                        item.location.lowercase().contains(query) ||
                        item.description.lowercase().contains(query) ||
                        item.reporterName.lowercase().contains(query)
                    matchesTypeOrStatus && matchesCategory && matchesSearch
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading
    )

    val myItemsState: StateFlow<UiState<List<CampusItem>>> = repository.observeMyItems(currentUserId)
        .map<List<CampusItem>, UiState<List<CampusItem>>> { UiState.Success(it) }
        .catch { error ->
            val root = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Error observing user items", root)
            emit(UiState.Error(root.message ?: "Failed to load your items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val notificationsState: StateFlow<UiState<List<UserNotification>>> = repository.observeNotifications(currentUserId)
        .map<List<UserNotification>, UiState<List<UserNotification>>> { list ->
            detectNewUserNotifications(list)
            UiState.Success(list)
        }
        .catch { error ->
            val root = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Error observing notifications", root)
            emit(UiState.Error(root.message ?: "Failed to load notifications"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UiState.Loading
        )

    val unreadAlertsCount: StateFlow<Int> = notificationsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.count { !it.read }
                else -> 0
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException && unwrapFirestoreError(error) == null) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    init {
        syncUserProfileOnStart()
    }

    private fun syncUserProfileOnStart() {
        viewModelScope.launch {
            val savedPhone = preferences.savedPhoneNumber.first()
            repository.upsertUserProfile(
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl,
                phoneNumber = savedPhone.ifBlank { null }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _filters.value = _filters.value.copy(searchQuery = query)
    }

    fun selectFeedFilter(filter: FeedFilter) {
        _filters.value = _filters.value.copy(feedFilter = filter)
    }

    fun toggleCategoryFilter(category: ItemCategory) {
        val current = _filters.value.selectedCategory
        _filters.value = _filters.value.copy(
            selectedCategory = if (current == category) null else category
        )
    }

    fun clearFilters() {
        _filters.value = FeedFiltersState()
    }

    fun observeItemDetail(itemId: String): Flow<UiState<CampusItem?>> {
        return repository.observeItem(itemId)
            .map<CampusItem?, UiState<CampusItem?>> { UiState.Success(it) }
            .catch { error ->
                val root = unwrapFirestoreErrorOrThrow(error)
                emit(UiState.Error(root.message ?: "Failed to load item details"))
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeClaimsForDetail(itemId: String): Flow<List<ItemClaim>> {
        return repository.observeItem(itemId)
            .flatMapLatest { item ->
                if (item == null) {
                    flowOf(emptyList())
                } else {
                    val isReporter = item.reporterId == currentUserId
                    repository.observeClaimsForItem(itemId, currentUserId, isReporter)
                }
            }
            .catch { error ->
                if (error is CancellationException && unwrapFirestoreError(error) == null) throw error
                emit(emptyList())
            }
    }

    suspend fun compressSelectedPhoto(uri: Uri): String? {
        return ImageUtils.compressUriToBase64(appContext, uri)
    }

    fun submitItemReport(
        title: String,
        description: String,
        category: ItemCategory,
        type: ItemType,
        location: String,
        contactPhone: String,
        photoBase64: String?,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmittingReport.value = true
            preferences.savePhoneNumber(contactPhone)
            repository.updateSavedPhone(contactPhone)
            val result = repository.createCampusItem(
                reporterName = userDisplayName,
                title = title,
                description = description,
                category = category.key,
                type = type.key,
                location = location,
                contactPhone = contactPhone,
                photoBase64 = photoBase64
            )
            _isSubmittingReport.value = false
            result.onSuccess { newItemId ->
                knownCampusItemIds.add(newItemId)
                _snackbarEvents.tryEmit("${type.label} item posted to FindIt Campus")
                onSuccess(newItemId)
            }.onFailure { error ->
                _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to post report")
            }
        }
    }

    fun submitClaim(
        item: CampusItem,
        claimantPhone: String,
        message: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmittingClaim.value = true
            preferences.savePhoneNumber(claimantPhone)
            repository.updateSavedPhone(claimantPhone)
            val result = repository.submitClaimAndNotify(
                item = item,
                claimantName = userDisplayName,
                claimantPhone = claimantPhone,
                message = message
            )
            _isSubmittingClaim.value = false
            result.onSuccess {
                _snackbarEvents.tryEmit("Pickup message sent! The reporter was notified.")
                onSuccess()
            }.onFailure { error ->
                _snackbarEvents.tryEmit(error.localizedMessage ?: "Could not submit pickup note")
            }
        }
    }

    fun updateItemStatus(itemId: String, newStatus: ItemStatus) {
        viewModelScope.launch {
            val result = repository.updateCampusItemStatus(itemId, newStatus)
            result.onSuccess {
                val msg = when (newStatus) {
                    ItemStatus.RECOVERED -> "Item marked as Recovered!"
                    ItemStatus.CLAIMED -> "Marked as Pending Pickup"
                    ItemStatus.OPEN -> "Listing reopened"
                }
                _snackbarEvents.tryEmit(msg)
            }.onFailure { error ->
                _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to update status")
            }
        }
    }

    fun deleteItem(itemId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteCampusItem(itemId)
            result.onSuccess {
                _snackbarEvents.tryEmit("Report deleted")
                onDeleted()
            }.onFailure { error ->
                _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to delete report")
            }
        }
    }

    fun updateClaimStatus(itemId: String, claimId: String, status: String) {
        viewModelScope.launch {
            repository.updateClaimStatus(itemId, claimId, status)
                .onFailure { error ->
                    _snackbarEvents.tryEmit(error.localizedMessage ?: "Failed to update claim")
                }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun markAllNotificationsRead(notifications: List<UserNotification>) {
        val unreadIds = notifications.filter { !it.read }.map { it.id }
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(currentUserId, unreadIds)
                .onSuccess { _snackbarEvents.tryEmit("All alerts marked as read") }
        }
    }

    fun saveDefaultPhone(phone: String) {
        viewModelScope.launch {
            preferences.savePhoneNumber(phone)
            repository.updateSavedPhone(phone)
            _snackbarEvents.tryEmit("Contact number saved")
        }
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSystemNotificationsEnabled(enabled)
        }
    }

    private fun detectNewCampusItemsForAlerts(items: List<CampusItem>) {
        if (!campusFeedSeeded) {
            items.forEach { knownCampusItemIds.add(it.id) }
            campusFeedSeeded = true
            return
        }
        val newItems = items.filter { it.id !in knownCampusItemIds }
        newItems.forEach { item ->
            knownCampusItemIds.add(item.id)
            if (item.reporterId != currentUserId && systemNotificationsEnabled.value) {
                val prefix = if (item.itemType == ItemType.LOST) "New Lost Item" else "New Found Item"
                CampusNotificationHelper.postAlert(
                    context = appContext,
                    notificationId = item.id.hashCode(),
                    title = "$prefix: ${item.title}",
                    body = "${item.location} • Call ${item.contactPhone}"
                )
            }
        }
    }

    private fun detectNewUserNotifications(notifications: List<UserNotification>) {
        if (!notificationsSeeded) {
            notifications.forEach { knownNotificationIds.add(it.id) }
            notificationsSeeded = true
            return
        }
        val newNotifs = notifications.filter { it.id !in knownNotificationIds && !it.read }
        newNotifs.forEach { notif ->
            knownNotificationIds.add(notif.id)
            if (systemNotificationsEnabled.value) {
                CampusNotificationHelper.postAlert(
                    context = appContext,
                    notificationId = notif.id.hashCode(),
                    title = notif.title,
                    body = notif.body
                )
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private fun unwrapFirestoreErrorOrThrow(error: Throwable): Throwable {
        val firestoreError = unwrapFirestoreError(error)
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private companion object {
        const val TAG = "FindItViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
