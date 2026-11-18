package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val region: String = "Aşgabat",
    val phone: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Listing(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sellerName: String = "",
    val title: String = "",
    val category: String = ListingCategory.CARS.key,
    val make: String = "",
    val model: String = "",
    val year: Long = 2020L,
    val priceTmt: Long = 0L,
    val mileageKm: Long = 0L,
    val transmission: String = "Awtomat",
    val fuelType: String = "Benzin",
    val engineVolume: String = "2.5 L",
    val bodyType: String = "Sedan",
    val color: String = "Ak",
    val region: String = "Aşgabat",
    val description: String = "",
    val badge: String = ListingBadge.NONE.key,
    val status: String = ListingStatus.ACTIVE.key,
    val negotiable: Boolean = true,
    val tradeAvailable: Boolean = false,
    val creditAvailable: Boolean = false,
    val imageKey: String = "sedan",
    val photoData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun categoryEnum(): ListingCategory = ListingCategory.fromKey(category)
    fun badgeEnum(): ListingBadge = ListingBadge.fromKey(badge)
    fun statusEnum(): ListingStatus = ListingStatus.fromKey(status)
}

@Keep
@IgnoreExtraProperties
data class FavoriteItem(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val title: String = "",
    val category: String = ListingCategory.CARS.key,
    val priceTmt: Long = 0L,
    val year: Long = 2020L,
    val mileageKm: Long = 0L,
    val region: String = "Aşgabat",
    val imageKey: String = "sedan",
    val badge: String = ListingBadge.NONE.key,
    val photoData: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Inquiry(
    @DocumentId val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val participantIds: List<String> = emptyList(),
    val message: String = "",
    val offerPriceTmt: Long = 0L,
    val contactNote: String? = null,
    val status: String = InquiryStatus.NEW.key,
    val replyMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun statusEnum(): InquiryStatus = InquiryStatus.fromKey(status)
}

enum class ListingCategory(val key: String, val labelTm: String) {
    CARS("cars", "Awtoulaglar"),
    PARTS("parts", "Şaýlar"),
    REAL_ESTATE("realestate", "Emläk"),
    SERVICES("services", "Hyzmatlar"),
    OTHER("other", "Başga");

    companion object {
        fun fromKey(raw: String): ListingCategory =
            entries.firstOrNull { it.key == raw } ?: CARS
    }
}

enum class ListingBadge(val key: String, val labelTm: String) {
    NONE("none", "Adaty"),
    VIP("vip", "VIP"),
    URGENT("urgent", "Gyssagly"),
    NEW("new", "Täze");

    companion object {
        fun fromKey(raw: String): ListingBadge =
            entries.firstOrNull { it.key == raw } ?: NONE
    }
}

enum class ListingStatus(val key: String, val labelTm: String) {
    ACTIVE("active", "Aktiw"),
    PAUSED("paused", "Duruzylan"),
    SOLD("sold", "Satyldy");

    companion object {
        fun fromKey(raw: String): ListingStatus =
            entries.firstOrNull { it.key == raw } ?: ACTIVE
    }
}

enum class InquiryStatus(val key: String, val labelTm: String) {
    NEW("new", "Täze"),
    REPLIED("replied", "Jogap berildi"),
    CLOSED("closed", "Ýapyldy");

    companion object {
        fun fromKey(raw: String): InquiryStatus =
            entries.firstOrNull { it.key == raw } ?: NEW
    }
}

enum class SortOrder(val labelTm: String) {
    NEWEST("Täzeler öňde"),
    PRICE_LOW("Arzandan gymmada"),
    PRICE_HIGH("Gymmatdan arzana"),
    YEAR_NEW("Ýyly täze"),
}

data class SearchFilter(
    val query: String = "",
    val category: ListingCategory? = null,
    val region: String = "Ähli welaýatlar",
    val make: String = "",
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val minPriceTmt: Long? = null,
    val maxPriceTmt: Long? = null,
    val transmission: String = "",
    val fuelType: String = "",
    val onlyVip: Boolean = false,
    val onlyUrgent: Boolean = false,
    val onlyCredit: Boolean = false,
    val onlyTrade: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST,
)

object MarketplaceCatalog {
    val regions = listOf(
        "Ähli welaýatlar",
        "Aşgabat",
        "Ahal",
        "Balkan",
        "Daşoguz",
        "Lebap",
        "Mary",
    )

    val carRegions = regions.drop(1)

    val popularMakesAndModels: Map<String, List<String>> = linkedMapOf(
        "Toyota" to listOf("Camry", "Corolla", "Land Cruiser", "Land Cruiser Prado", "Avalon", "RAV4", "Highlander", "Hilux"),
        "Lexus" to listOf("LX 570", "RX 350", "ES 350", "GX 460", "NX 300"),
        "Mercedes-Benz" to listOf("E-Class", "S-Class", "C-Class", "GLE", "G-Class"),
        "Hyundai" to listOf("Sonata", "Elantra", "Tucson", "Santa Fe", "Accent"),
        "Kia" to listOf("K5", "Optima", "Sportage", "Sorento", "Rio"),
        "BMW" to listOf("X5", "5-Series", "3-Series", "X6", "7-Series"),
        "Nissan" to listOf("Patrol", "Altima", "X-Trail", "Maxima"),
        "Lada" to listOf("Vesta", "Niva", "Granta"),
    )

    val transmissions = listOf("Awtomat", "Mehanika", "Tiptronik", "Wariator")
    val fuelTypes = listOf("Benzin", "Gibrid", "Dizel", "Elektro", "Gaz")
    val bodyTypes = listOf("Sedan", "Ýolsuz (4x4)", "Krossover", "Kupe", "Universal", "Miniven")
    val colors = listOf("Ak", "Gara", "Kümüşsöw", "Çal", "Gök", "Gyzyl", "Goňur")
}
