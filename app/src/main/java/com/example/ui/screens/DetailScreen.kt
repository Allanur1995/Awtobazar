package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Listing
import com.example.data.model.ListingStatus
import com.example.ui.components.BadgePill
import com.example.ui.components.ListingImage
import com.example.ui.components.formatMileage
import com.example.ui.components.formatRelativeTimeTm
import com.example.ui.components.formatTmt
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.TurboEmerald
import com.example.ui.theme.UrgentCrimson

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    listing: Listing?,
    currentUserId: String,
    isFavorite: Boolean,
    isBusy: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (Listing) -> Unit,
    onEditListing: (Listing) -> Unit,
    onMarkSold: (String) -> Unit,
    onDeleteListing: (String) -> Unit,
    onSendInquiry: (Listing, String, Long, String, () -> Unit) -> Unit,
) {
    var showInquirySheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (listing == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bildiriş tapylmady ýa-da pozuldy", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onBack) { Text("Yza gaýt") }
            }
        }
        return
    }

    val isOwner = listing.userId == currentUserId
    val isSold = listing.statusEnum() == ListingStatus.SOLD

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
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
                                colors = listOf(Color(0x880A192F), Color.Transparent, Color(0xAA0A192F))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MidnightNavy.copy(alpha = 0.65f))
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Yza",
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { onToggleFavorite(listing) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MidnightNavy.copy(alpha = 0.65f))
                            .testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Saýlananlar",
                            tint = if (isFavorite) UrgentCrimson else Color.White,
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BadgePill(badge = listing.badgeEnum())
                    if (isSold) {
                        Surface(color = UrgentCrimson, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "SATYLDY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatTmt(listing.priceTmt),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = formatRelativeTimeTm(listing.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = listing.region,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (listing.negotiable) DealPill("Ylaşykly", TurboEmerald)
                    if (listing.tradeAvailable) DealPill("Obmen bar", MaterialTheme.colorScheme.primary)
                    if (listing.creditAvailable) DealPill("Kredit bar", MaterialTheme.colorScheme.tertiary)
                }

                // Specs Grid Card
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.detail_specs_title), style = MaterialTheme.typography.titleMedium)
                        SpecRow("Bölümi", listing.categoryEnum().labelTm)
                        SpecRow("Marka / Model", "${listing.make} ${listing.model}")
                        SpecRow("Çykan ýyly", "${listing.year}")
                        SpecRow("Ýören ýoly", formatMileage(listing.mileageKm))
                        SpecRow("Motor", listing.engineVolume)
                        SpecRow("Korobka", listing.transmission)
                        SpecRow("Ýangyç", listing.fuelType)
                        SpecRow("Kuzow / Görnüş", listing.bodyType)
                        SpecRow("Reňki", listing.color)
                    }
                }

                // Description
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.detail_desc_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = listing.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Seller Info Card
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.detail_seller_title), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(listing.sellerName, style = MaterialTheme.typography.titleMedium)
                            Text(listing.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Bottom Sticky Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isOwner) {
                    if (!isSold) {
                        OutlinedButton(
                            onClick = { onEditListing(listing) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.detail_edit))
                        }
                        Button(
                            onClick = { onMarkSold(listing.id) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = TurboEmerald),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("detail_mark_sold_button")
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Satyldy")
                        }
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .testTag("detail_delete_button")
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Poz", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                } else {
                    Button(
                        onClick = { showInquirySheet = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("contact_seller_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.detail_contact_seller))
                    }
                }
            }
        }
    }

    if (showInquirySheet) {
        var message by remember { mutableStateOf("Salam! Ulagyňyz bilen gyzyklanýaryn, ýagdaýy nähili?") }
        var offerText by remember { mutableStateOf(listing.priceTmt.toString()) }
        var contactNote by remember { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { showInquirySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Satyja habar we baha teklip et", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(500) },
                    label = { Text("Habaryňyz *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("inquiry_message_input")
                )
                OutlinedTextField(
                    value = offerText,
                    onValueChange = { offerText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Teklip edýän bahaňyz (TMT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("inquiry_offer_input")
                )
                OutlinedTextField(
                    value = contactNote,
                    onValueChange = { contactNote = it.take(80) },
                    label = { Text("Goşmaça bellik (islege görä)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (message.isNotBlank()) {
                            onSendInquiry(
                                listing,
                                message,
                                offerText.toLongOrNull() ?: 0L,
                                contactNote,
                            ) { showInquirySheet = false }
                        }
                    },
                    enabled = !isBusy && message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("send_inquiry_submit")
                ) {
                    Text("Habary iber")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Bildirişi pozmak") },
            text = { Text("Bu bildirişi hemişelik pozmak isleýärsiňizmi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteListing(listing.id)
                    }
                ) { Text("Poz", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Ýatyrmak") }
            }
        )
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DealPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
