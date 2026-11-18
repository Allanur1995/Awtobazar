package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MiscellaneousServices
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Listing
import com.example.data.model.ListingBadge
import com.example.data.model.ListingCategory
import com.example.data.model.ListingStatus
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.TurboEmerald
import com.example.ui.theme.UrgentCrimson
import com.example.ui.theme.VipAmber
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

fun formatTmt(priceTmt: Long): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    val grouped = formatter.format(priceTmt).replace(',', ' ')
    return "$grouped TMT"
}

fun formatMileage(km: Long): String {
    if (km <= 0L) return "Täze"
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    val grouped = formatter.format(km).replace(',', ' ')
    return "$grouped km"
}

fun formatRelativeTimeTm(timestamp: Timestamp?): String {
    if (timestamp == null) return "Şu wagt"
    val diffSec = ((System.currentTimeMillis() - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 120L -> "Şu wagt"
        diffSec < 3600L -> "${diffSec / 60L} min öň"
        diffSec < 86400L -> "${diffSec / 3600L} sagat öň"
        diffSec < 172800L -> "Düýn"
        else -> "${diffSec / 86400L} gün öň"
    }
}

fun categoryIcon(category: ListingCategory): ImageVector = when (category) {
    ListingCategory.CARS -> Icons.Rounded.DirectionsCar
    ListingCategory.PARTS -> Icons.Rounded.Build
    ListingCategory.REAL_ESTATE -> Icons.Rounded.HomeWork
    ListingCategory.SERVICES -> Icons.Rounded.MiscellaneousServices
    ListingCategory.OTHER -> Icons.Rounded.Category
}

fun drawableForImageKey(imageKey: String): Int = when (imageKey) {
    "suv" -> R.drawable.img_car_suv
    "luxury" -> R.drawable.img_car_luxury
    "showroom" -> R.drawable.img_hero_showroom
    else -> R.drawable.img_car_sedan
}

suspend fun encodePickedImageToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext null
        val original = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            ?: return@withContext null
        val maxDim = 560
        val scale = minOf(1f, maxDim.toFloat() / maxOf(original.width, original.height))
        val targetW = (original.width * scale).toInt().coerceAtLeast(1)
        val targetH = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, targetW, targetH, true)
        } else {
            original
        }
        val out = ByteArrayOutputStream()
        var quality = 72
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > 110_000 && quality > 30) {
            out.reset()
            quality -= 12
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ListingImage(
    imageKey: String,
    photoData: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val decodedBitmap = remember(photoData) {
        if (photoData.isNullOrBlank()) {
            null
        } else {
            try {
                val bytes = Base64.decode(photoData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = drawableForImageKey(imageKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun BadgePill(badge: ListingBadge, modifier: Modifier = Modifier) {
    if (badge == ListingBadge.NONE) return
    val (bg, fg) = when (badge) {
        ListingBadge.VIP -> VipAmber to Color.White
        ListingBadge.URGENT -> UrgentCrimson to Color.White
        ListingBadge.NEW -> TurboEmerald to Color.White
        ListingBadge.NONE -> Color.Transparent to Color.Transparent
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (badge == ListingBadge.VIP) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = badge.labelTm,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun VipShowcaseCard(
    listing: Listing,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .width(268.dp)
            .clickable(onClick = onClick)
            .testTag("vip_card_${listing.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
        ) {
            ListingImage(
                imageKey = listing.imageKey,
                photoData = listing.photoData,
                contentDescription = listing.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xBB0A192F))
                        )
                    )
            )
            BadgePill(
                badge = listing.badgeEnum(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("fav_toggle_${listing.id}")
            ) {
                Surface(
                    shape = CircleShape,
                    color = MidnightNavy.copy(alpha = 0.6f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Saýlananlara goş",
                            tint = if (isFavorite) UrgentCrimson else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = formatTmt(listing.priceTmt),
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${listing.year} • ${formatMileage(listing.mileageKm)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = listing.region,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListingCard(
    listing: Listing,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("listing_card_${listing.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
            ) {
                ListingImage(
                    imageKey = listing.imageKey,
                    photoData = listing.photoData,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BadgePill(badge = listing.badgeEnum())
                    if (listing.statusEnum() == ListingStatus.SOLD) {
                        Surface(
                            color = MidnightNavy.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "SATYLDY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("card_fav_${listing.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MidnightNavy.copy(alpha = 0.55f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Saýla",
                                tint = if (isFavorite) UrgentCrimson else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTmt(listing.priceTmt),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = formatRelativeTimeTm(listing.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SpecMiniTag("${listing.year}")
                    if (listing.categoryEnum() == ListingCategory.CARS) {
                        SpecMiniTag(formatMileage(listing.mileageKm))
                        SpecMiniTag(listing.engineVolume)
                        SpecMiniTag(listing.transmission)
                    } else {
                        SpecMiniTag(listing.categoryEnum().labelTm)
                    }
                    if (listing.creditAvailable) {
                        SpecMiniTag("Kredit", highlight = true)
                    }
                    if (listing.tradeAvailable) {
                        SpecMiniTag("Obmen", highlight = true)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = listing.region,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (listing.negotiable) {
                        Text(
                            text = "Ylaşykly",
                            style = MaterialTheme.typography.labelSmall,
                            color = TurboEmerald,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecMiniTag(label: String, highlight: Boolean = false) {
    Surface(
        color = if (highlight) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
