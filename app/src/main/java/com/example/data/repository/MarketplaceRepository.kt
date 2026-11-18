package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FavoriteItem
import com.example.data.model.Inquiry
import com.example.data.model.InquiryStatus
import com.example.data.model.Listing
import com.example.data.model.ListingBadge
import com.example.data.model.ListingCategory
import com.example.data.model.ListingStatus
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Amallar üçin Google bilen ulgama girmeli.")

    fun observeActiveListings(): Flow<List<Listing>> =
        db.collection("listings")
            .whereEqualTo("status", ListingStatus.ACTIVE.key)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Listing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeMyListings(userId: String): Flow<List<Listing>> =
        db.collection("listings")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Listing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeFavorites(userId: String): Flow<List<FavoriteItem>> =
        db.collection("users").document(userId).collection("favorites")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FavoriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeInquiries(userId: String): Flow<List<Inquiry>> =
        db.collection("inquiries")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Inquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }

    suspend fun ensureUserProfile(
        displayName: String,
        photoUrl: String?,
        defaultRegion: String = "Aşgabat",
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Signed out"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val cleanName = displayName.trim().ifBlank { "AwtoBazar Ulanyjy" }.take(60)
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", cleanName)
                    put("region", defaultRegion.take(40))
                    if (!photoUrl.isNullOrBlank()) {
                        put("photoUrl", photoUrl.take(500))
                    }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        region: String,
        phone: String,
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val cleanName = displayName.trim().ifBlank { "Ulanyjy" }.take(60)
            val cleanRegion = region.trim().ifBlank { "Aşgabat" }.take(40)
            val cleanPhone = phone.trim().take(30)
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "region" to cleanRegion,
                    "phone" to cleanPhone,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createListing(listing: Listing): Result<String> {
        var path = "listings"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("listings").document()
            path = docRef.path
            val payload = buildListingPayload(uid, listing, isCreate = true)
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateListing(listingId: String, listing: Listing): Result<Unit> {
        var path = "listings/$listingId"
        return try {
            val uid = requireUserId()
            require(listingId.isNotBlank()) { "Bildiriş ID boş bolup bilmez." }
            val docRef = db.collection("listings").document(listingId)
            path = docRef.path
            val payload = buildMap<String, Any> {
                put("sellerName", listing.sellerName.trim().ifBlank { "Satyjy" }.take(60))
                put("title", listing.title.trim().take(120))
                put("category", ListingCategory.fromKey(listing.category).key)
                put("make", listing.make.trim().ifBlank { "Beýleki" }.take(50))
                put("model", listing.model.trim().ifBlank { "Model" }.take(50))
                put("year", listing.year.coerceIn(1950L, 2030L))
                put("priceTmt", listing.priceTmt.coerceIn(0L, 100_000_000L))
                put("mileageKm", listing.mileageKm.coerceIn(0L, 5_000_000L))
                put("transmission", listing.transmission.trim().ifBlank { "Awtomat" }.take(40))
                put("fuelType", listing.fuelType.trim().ifBlank { "Benzin" }.take(40))
                put("engineVolume", listing.engineVolume.trim().ifBlank { "2.0 L" }.take(20))
                put("bodyType", listing.bodyType.trim().ifBlank { "Sedan" }.take(40))
                put("color", listing.color.trim().ifBlank { "Ak" }.take(30))
                put("region", listing.region.trim().ifBlank { "Aşgabat" }.take(40))
                put("description", listing.description.trim().ifBlank { "Maglumat ýok" }.take(1500))
                put("badge", ListingBadge.fromKey(listing.badge).key)
                put("status", ListingStatus.fromKey(listing.status).key)
                put("negotiable", listing.negotiable)
                put("tradeAvailable", listing.tradeAvailable)
                put("creditAvailable", listing.creditAvailable)
                put("imageKey", listing.imageKey.trim().ifBlank { "sedan" }.take(40))
                val cleanPhoto = listing.photoData?.takeIf { it.isNotBlank() && it.length <= 160_000 }
                if (cleanPhoto != null) {
                    put("photoData", cleanPhoto)
                }
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateListingStatus(listingId: String, status: ListingStatus): Result<Unit> {
        val path = "listings/$listingId"
        return try {
            requireUserId()
            require(listingId.isNotBlank())
            db.collection("listings").document(listingId).update(
                mapOf(
                    "status" to status.key,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        val path = "listings/$listingId"
        return try {
            requireUserId()
            require(listingId.isNotBlank())
            db.collection("listings").document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(listing: Listing, currentlyFavorite: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(listing.id.isNotBlank())
            val favRef = db.collection("users").document(uid)
                .collection("favorites").document(listing.id)
            path = favRef.path
            if (currentlyFavorite) {
                favRef.delete().await()
            } else {
                val payload = buildMap<String, Any> {
                    put("listingId", listing.id)
                    put("title", listing.title.trim().ifBlank { "Bildiriş" }.take(120))
                    put("category", ListingCategory.fromKey(listing.category).key)
                    put("priceTmt", listing.priceTmt.coerceIn(0L, 100_000_000L))
                    put("year", listing.year.coerceIn(1950L, 2030L))
                    put("mileageKm", listing.mileageKm.coerceIn(0L, 5_000_000L))
                    put("region", listing.region.trim().ifBlank { "Aşgabat" }.take(40))
                    put("imageKey", listing.imageKey.trim().ifBlank { "sedan" }.take(40))
                    put("badge", ListingBadge.fromKey(listing.badge).key)
                    val cleanPhoto = listing.photoData?.takeIf { it.isNotBlank() && it.length <= 160_000 }
                    if (cleanPhoto != null) {
                        put("photoData", cleanPhoto)
                    }
                    put("createdAt", FieldValue.serverTimestamp())
                }
                favRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(listingId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val favRef = db.collection("users").document(uid)
                .collection("favorites").document(listingId)
            path = favRef.path
            favRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sendInquiry(
        listing: Listing,
        senderName: String,
        message: String,
        offerPriceTmt: Long,
        contactNote: String,
    ): Result<Unit> {
        var path = "inquiries"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("inquiries").document()
            path = docRef.path
            val cleanMsg = message.trim().take(500)
            require(cleanMsg.isNotEmpty()) { "Habar boş bolup bilmez." }
            val payload = buildMap<String, Any> {
                put("listingId", listing.id)
                put("listingTitle", listing.title.trim().ifBlank { "Bildiriş" }.take(120))
                put("senderId", uid)
                put("senderName", senderName.trim().ifBlank { "Alyjy" }.take(60))
                put("sellerId", listing.userId)
                put("sellerName", listing.sellerName.trim().ifBlank { "Satyjy" }.take(60))
                put("participantIds", listOf(uid, listing.userId))
                put("message", cleanMsg)
                put("offerPriceTmt", offerPriceTmt.coerceIn(0L, 100_000_000L))
                val cleanNote = contactNote.trim().take(80)
                if (cleanNote.isNotEmpty()) {
                    put("contactNote", cleanNote)
                }
                put("status", InquiryStatus.NEW.key)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun replyToInquiry(inquiryId: String, replyMessage: String): Result<Unit> {
        val path = "inquiries/$inquiryId"
        return try {
            requireUserId()
            val cleanReply = replyMessage.trim().take(500)
            require(cleanReply.isNotEmpty()) { "Jogap teksti boş bolup bilmez." }
            db.collection("inquiries").document(inquiryId).update(
                mapOf(
                    "status" to InquiryStatus.REPLIED.key,
                    "replyMessage" to cleanReply,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterListings(sellerName: String): Result<Int> {
        var path = "listings"
        return try {
            val uid = requireUserId()
            val cleanSeller = sellerName.trim().ifBlank { "AwtoBazar Salon" }.take(60)
            val samples = starterShowcaseListings(uid, cleanSeller)
            val batch = db.batch()
            samples.forEach { sample ->
                val ref = db.collection("listings").document()
                path = ref.path
                batch.set(ref, buildListingPayload(uid, sample, isCreate = true))
            }
            batch.commit().await()
            Result.success(samples.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun buildListingPayload(uid: String, listing: Listing, isCreate: Boolean): Map<String, Any> =
        buildMap {
            put("userId", uid)
            put("sellerName", listing.sellerName.trim().ifBlank { "Satyjy" }.take(60))
            put("title", listing.title.trim().take(120))
            put("category", ListingCategory.fromKey(listing.category).key)
            put("make", listing.make.trim().ifBlank { "Beýleki" }.take(50))
            put("model", listing.model.trim().ifBlank { "Model" }.take(50))
            put("year", listing.year.coerceIn(1950L, 2030L))
            put("priceTmt", listing.priceTmt.coerceIn(0L, 100_000_000L))
            put("mileageKm", listing.mileageKm.coerceIn(0L, 5_000_000L))
            put("transmission", listing.transmission.trim().ifBlank { "Awtomat" }.take(40))
            put("fuelType", listing.fuelType.trim().ifBlank { "Benzin" }.take(40))
            put("engineVolume", listing.engineVolume.trim().ifBlank { "2.5 L" }.take(20))
            put("bodyType", listing.bodyType.trim().ifBlank { "Sedan" }.take(40))
            put("color", listing.color.trim().ifBlank { "Ak" }.take(30))
            put("region", listing.region.trim().ifBlank { "Aşgabat" }.take(40))
            put("description", listing.description.trim().ifBlank { "Maglumat ýok" }.take(1500))
            put("badge", ListingBadge.fromKey(listing.badge).key)
            put("status", ListingStatus.fromKey(listing.status).key)
            put("negotiable", listing.negotiable)
            put("tradeAvailable", listing.tradeAvailable)
            put("creditAvailable", listing.creditAvailable)
            put("imageKey", listing.imageKey.trim().ifBlank { "sedan" }.take(40))
            val cleanPhoto = listing.photoData?.takeIf { it.isNotBlank() && it.length <= 160_000 }
            if (cleanPhoto != null) {
                put("photoData", cleanPhoto)
            }
            if (isCreate) {
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }

    private fun starterShowcaseListings(uid: String, sellerName: String): List<Listing> = listOf(
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Toyota Camry XSE V6 2022",
            category = ListingCategory.CARS.key,
            make = "Toyota",
            model = "Camry",
            year = 2022L,
            priceTmt = 685000L,
            mileageKm = 28400L,
            transmission = "Awtomat",
            fuelType = "Benzin",
            engineVolume = "3.5 L",
            bodyType = "Sedan",
            color = "Ak",
            region = "Aşgabat",
            description = "Degim ýok, zawot reňk. Panarama üçek, JBL ses ulgamy, 360 kamera we öňki oturgyçlar gyzdyryjy bar. Baha ýerinde ylaşykly.",
            badge = ListingBadge.VIP.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = true,
            tradeAvailable = true,
            creditAvailable = false,
            imageKey = "sedan",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Toyota Land Cruiser Prado TX-L",
            category = ListingCategory.CARS.key,
            make = "Toyota",
            model = "Land Cruiser Prado",
            year = 2021L,
            priceTmt = 1140000L,
            mileageKm = 41200L,
            transmission = "Awtomat",
            fuelType = "Benzin",
            engineVolume = "4.0 L",
            bodyType = "Ýolsuz (4x4)",
            color = "Ak",
            region = "Aşgabat",
            description = "Doly toplum (Full), 7 orunlyk, deri salon, sowadyjy bar. Ähli tehniki hyzmatlary wagtynda geçilen.",
            badge = ListingBadge.VIP.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = true,
            tradeAvailable = true,
            creditAvailable = true,
            imageKey = "suv",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Mercedes-Benz E 350 AMG Line",
            category = ListingCategory.CARS.key,
            make = "Mercedes-Benz",
            model = "E-Class",
            year = 2020L,
            priceTmt = 795000L,
            mileageKm = 54000L,
            transmission = "Awtomat",
            fuelType = "Benzin",
            engineVolume = "3.0 L",
            bodyType = "Sedan",
            color = "Kümüşsöw",
            region = "Mary",
            description = "AMG paket, Burmester audio, sanly panel. Gyssagly satylýar, gowy ulag bilen obmen seredýäris.",
            badge = ListingBadge.URGENT.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = true,
            tradeAvailable = true,
            creditAvailable = false,
            imageKey = "luxury",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Lexus LX 570 Superior 2019",
            category = ListingCategory.CARS.key,
            make = "Lexus",
            model = "LX 570",
            year = 2019L,
            priceTmt = 1680000L,
            mileageKm = 67500L,
            transmission = "Awtomat",
            fuelType = "Benzin",
            engineVolume = "5.7 L",
            bodyType = "Ýolsuz (4x4)",
            color = "Ak",
            region = "Balkan",
            description = "Superior komplektasiýa, Mark Levinson audio, pnewmo asma ulgamy kämil işleýär.",
            badge = ListingBadge.VIP.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = false,
            tradeAvailable = false,
            creditAvailable = true,
            imageKey = "suv",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Hyundai Sonata Limited Hybrid",
            category = ListingCategory.CARS.key,
            make = "Hyundai",
            model = "Sonata",
            year = 2023L,
            priceTmt = 535000L,
            mileageKm = 11500L,
            transmission = "Awtomat",
            fuelType = "Gibrid",
            engineVolume = "2.0 L",
            bodyType = "Sedan",
            color = "Ak",
            region = "Lebap",
            description = "Örän tygşytly gibrid, 100 km-e 4.5 litr ýangyç sarp edýär. Täze ýagdaýda.",
            badge = ListingBadge.NEW.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = true,
            tradeAvailable = false,
            creditAvailable = true,
            imageKey = "sedan",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Bridgestone 235/55 R18 Tekerler (4 sany)",
            category = ListingCategory.PARTS.key,
            make = "Bridgestone",
            model = "Turanza T005",
            year = 2025L,
            priceTmt = 14500L,
            mileageKm = 0L,
            transmission = "Ählisi",
            fuelType = "Ählisi",
            engineVolume = "R18",
            bodyType = "Teker",
            color = "Gara",
            region = "Aşgabat",
            description = "Ýapon öndürijisi, 2025-nji ýyl täze tekerler toplumy. Camry, Avalon, Optima we K5 üçin laýyk.",
            badge = ListingBadge.NEW.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = true,
            tradeAvailable = false,
            creditAvailable = false,
            imageKey = "showroom",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "Mir-7/2, 3 otagly taýyn öý (Emläk)",
            category = ListingCategory.REAL_ESTATE.key,
            make = "Emläk",
            model = "Kwartira",
            year = 2022L,
            priceTmt = 1450000L,
            mileageKm = 0L,
            transmission = "4-nji gat",
            fuelType = "112 m²",
            engineVolume = "3 otag",
            bodyType = "Elitka",
            color = "Ak",
            region = "Aşgabat",
            description = "Döwrebap remontly, mebel we tehnika bilen bile satylýar. Resminamalary doly taýyn.",
            badge = ListingBadge.VIP.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = true,
            tradeAvailable = true,
            creditAvailable = false,
            imageKey = "showroom",
        ),
        Listing(
            userId = uid,
            sellerName = sellerName,
            title = "AwtoDetailing & Keramika 9H Hyzmaty",
            category = ListingCategory.SERVICES.key,
            make = "AwtoSerwis",
            model = "Detailing",
            year = 2026L,
            priceTmt = 4500L,
            mileageKm = 0L,
            transmission = "1 gün",
            fuelType = "Köp gatly",
            engineVolume = "9H",
            bodyType = "Hyzmat",
            color = "Gök",
            region = "Ahal",
            description = "Kuzow polirlemesi, 9H keramika gorag örtügi, salon himçistkasy we faralary arassalamak.",
            badge = ListingBadge.NONE.key,
            status = ListingStatus.ACTIVE.key,
            negotiable = false,
            tradeAvailable = false,
            creditAvailable = false,
            imageKey = "luxury",
        ),
    )
}
