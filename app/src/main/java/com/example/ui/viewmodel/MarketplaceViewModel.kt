package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FavoriteItem
import com.example.data.model.Inquiry
import com.example.data.model.Listing
import com.example.data.model.ListingBadge
import com.example.data.model.ListingCategory
import com.example.data.model.ListingStatus
import com.example.data.model.SearchFilter
import com.example.data.model.SortOrder
import com.example.data.model.UserProfile
import com.example.data.repository.MarketplaceRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    private val currentUserId: String,
    private val initialDisplayName: String,
    private val initialPhotoUrl: String?,
) : ViewModel() {

    private val _searchFilter = MutableStateFlow(SearchFilter())
    val searchFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", firestoreError ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val activeListingsState: StateFlow<UiState<List<Listing>>> = repository.observeActiveListings()
        .map<List<Listing>, UiState<List<Listing>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing active listings", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Bildirişleri ýükläp bolmady"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val filteredListingsState: StateFlow<UiState<List<Listing>>> =
        combine(activeListingsState, _searchFilter) { state, filter ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> UiState.Success(applyFilter(state.data, filter))
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val myListingsState: StateFlow<UiState<List<Listing>>> = repository.observeMyListings(currentUserId)
        .map<List<Listing>, UiState<List<Listing>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing user listings", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Bildirişleriňizi ýükläp bolmady"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> = repository.observeFavorites(currentUserId)
        .map<List<FavoriteItem>, UiState<List<FavoriteItem>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing favorites", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Saýlananlary ýükläp bolmady"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val favoriteIds: StateFlow<Set<String>> = favoritesState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.map { it.listingId }.toSet()
                else -> emptySet()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    val inquiriesState: StateFlow<UiState<List<Inquiry>>> = repository.observeInquiries(currentUserId)
        .map<List<Inquiry>, UiState<List<Inquiry>>> { list ->
            UiState.Success(list.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) })
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing inquiries", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Habarlary ýükläp bolmady"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = initialDisplayName,
                photoUrl = initialPhotoUrl,
            )
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun updateFilter(transform: (SearchFilter) -> SearchFilter) {
        _searchFilter.update(transform)
    }

    fun resetFilter() {
        _searchFilter.value = SearchFilter()
    }

    fun selectCategory(category: ListingCategory?) {
        _searchFilter.update { it.copy(category = category) }
    }

    fun selectRegion(region: String) {
        _searchFilter.update { it.copy(region = region) }
    }

    fun saveListing(existingId: String?, listing: Listing, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val sellerName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val prepared = listing.copy(sellerName = sellerName)
            val result = if (existingId.isNullOrBlank()) {
                repository.createListing(prepared).map { Unit }
            } else {
                repository.updateListing(existingId, prepared)
            }
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = if (existingId == null) {
                    "Bildirişiňiz üstünlikli goşuldy!"
                } else {
                    "Bildiriş täzelendi!"
                }
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Ýatda saklap bolmady"
            }
        }
    }

    fun markListingSold(listingId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateListingStatus(listingId, ListingStatus.SOLD)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Bildiriş \"Satyldy\" diýlip bellendi."
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Üýtgedip bolmady"
            }
        }
    }

    fun deleteListing(listingId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteListing(listingId)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Bildiriş pozuldy."
                onDeleted()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Pozup bolmady"
            }
        }
    }

    fun toggleFavorite(listing: Listing) {
        val isFav = favoriteIds.value.contains(listing.id)
        viewModelScope.launch {
            repository.toggleFavorite(listing, isFav).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Saýlananlara goşup bolmady"
            }
        }
    }

    fun removeFavorite(listingId: String) {
        viewModelScope.launch {
            repository.removeFavorite(listingId).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Pozup bolmady"
            }
        }
    }

    fun sendInquiry(
        listing: Listing,
        message: String,
        offerPriceTmt: Long,
        contactNote: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val senderName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val result = repository.sendInquiry(listing, senderName, message, offerPriceTmt, contactNote)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Habaryňyz satyja iberildi!"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Habar iberilmedi"
            }
        }
    }

    fun replyToInquiry(inquiryId: String, replyMessage: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.replyToInquiry(inquiryId, replyMessage)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Jogabyňyz iberildi!"
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Jogap iberilmedi"
            }
        }
    }

    fun updateProfile(displayName: String, region: String, phone: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateUserProfile(displayName, region, phone)
            _isBusy.value = false
            result.onSuccess {
                _actionMessage.value = "Profil maglumatlary täzelendi."
                onSuccess()
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Profili täzeläp bolmady"
            }
        }
    }

    fun loadStarterCatalog() {
        viewModelScope.launch {
            _isBusy.value = true
            val sellerName = userProfile.value?.displayName?.ifBlank { initialDisplayName } ?: initialDisplayName
            val result = repository.seedStarterListings(sellerName)
            _isBusy.value = false
            result.onSuccess { count ->
                _actionMessage.value = "$count sany taýyn bildiriş kataloga goşuldy!"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Katalog ýüklenmedi"
            }
        }
    }

    private fun applyFilter(items: List<Listing>, filter: SearchFilter): List<Listing> {
        val q = filter.query.trim().lowercase()
        val filtered = items.filter { item ->
            val matchesQuery = q.isEmpty() ||
                item.title.lowercase().contains(q) ||
                item.make.lowercase().contains(q) ||
                item.model.lowercase().contains(q) ||
                item.region.lowercase().contains(q) ||
                item.description.lowercase().contains(q)
            val matchesCategory = filter.category == null || item.category == filter.category.key
            val matchesRegion = filter.region == "Ähli welaýatlar" || item.region == filter.region
            val matchesMake = filter.make.isBlank() || item.make.equals(filter.make, ignoreCase = true)
            val matchesMinYear = filter.minYear == null || item.year >= filter.minYear
            val matchesMaxYear = filter.maxYear == null || item.year <= filter.maxYear
            val matchesMinPrice = filter.minPriceTmt == null || item.priceTmt >= filter.minPriceTmt
            val matchesMaxPrice = filter.maxPriceTmt == null || item.priceTmt <= filter.maxPriceTmt
            val matchesTrans = filter.transmission.isBlank() || item.transmission == filter.transmission
            val matchesFuel = filter.fuelType.isBlank() || item.fuelType == filter.fuelType
            val matchesVip = !filter.onlyVip || item.badge == ListingBadge.VIP.key
            val matchesUrgent = !filter.onlyUrgent || item.badge == ListingBadge.URGENT.key
            val matchesCredit = !filter.onlyCredit || item.creditAvailable
            val matchesTrade = !filter.onlyTrade || item.tradeAvailable

            matchesQuery && matchesCategory && matchesRegion && matchesMake &&
                matchesMinYear && matchesMaxYear && matchesMinPrice && matchesMaxPrice &&
                matchesTrans && matchesFuel && matchesVip && matchesUrgent &&
                matchesCredit && matchesTrade
        }

        return when (filter.sortOrder) {
            SortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            SortOrder.PRICE_LOW -> filtered.sortedBy { it.priceTmt }
            SortOrder.PRICE_HIGH -> filtered.sortedByDescending { it.priceTmt }
            SortOrder.YEAR_NEW -> filtered.sortedByDescending { it.year }
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "MarketplaceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
