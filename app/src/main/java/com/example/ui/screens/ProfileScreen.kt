package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Inquiry
import com.example.data.model.InquiryStatus
import com.example.data.model.Listing
import com.example.data.model.ListingStatus
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.UserProfile
import com.example.ui.components.ListingImage
import com.example.ui.components.formatRelativeTimeTm
import com.example.ui.components.formatTmt
import com.example.ui.theme.TurboEmerald
import com.example.ui.theme.UrgentCrimson
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    fallbackDisplayName: String,
    myListingsState: UiState<List<Listing>>,
    inquiriesState: UiState<List<Inquiry>>,
    isBusy: Boolean,
    onOpenListing: (String) -> Unit,
    onUpdateProfile: (String, String, String, () -> Unit) -> Unit,
    onReplyInquiry: (String, String, () -> Unit) -> Unit,
    onSignOut: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditProfile by remember { mutableStateOf(false) }
    var replyingInquiry by remember { mutableStateOf<Inquiry?>(null) }

    val displayName = userProfile?.displayName?.ifBlank { fallbackDisplayName } ?: fallbackDisplayName
    val userRegion = userProfile?.region?.ifBlank { "Aşgabat" } ?: "Aşgabat"
    val userPhone = userProfile?.phone.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, style = MaterialTheme.typography.titleLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(userRegion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (userPhone.isNotBlank()) {
                                    Spacer(Modifier.width(10.dp))
                                    Icon(Icons.Rounded.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(userPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        IconButton(
                            onClick = { showEditProfile = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Profili üýtget")
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_sign_out))
                    }
                }
            }
        }

        item {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.profile_my_listings)) },
                    modifier = Modifier.testTag("tab_my_listings")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.profile_inquiries)) },
                    modifier = Modifier.testTag("tab_inquiries")
                )
            }
        }

        if (selectedTab == 0) {
            when (myListingsState) {
                is UiState.Loading -> {
                    item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                is UiState.Error -> {
                    item { Text(myListingsState.message, color = MaterialTheme.colorScheme.error) }
                }
                is UiState.Success -> {
                    val items = myListingsState.data
                    if (items.isEmpty()) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Häzirki wagtda goşan bildirişiňiz ýok.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { listing ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenListing(listing.id) }
                                    .testTag("my_listing_${listing.id}")
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 96.dp, height = 72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        ListingImage(
                                            imageKey = listing.imageKey,
                                            photoData = listing.photoData,
                                            contentDescription = listing.title,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = listing.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = formatTmt(listing.priceTmt),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        val isSold = listing.statusEnum() == ListingStatus.SOLD
                                        Surface(
                                            color = if (isSold) UrgentCrimson.copy(alpha = 0.14f) else TurboEmerald.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = listing.statusEnum().labelTm,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSold) UrgentCrimson else TurboEmerald,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            when (inquiriesState) {
                is UiState.Loading -> {
                    item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                is UiState.Error -> {
                    item { Text(inquiriesState.message, color = MaterialTheme.colorScheme.error) }
                }
                is UiState.Success -> {
                    val items = inquiriesState.data
                    if (items.isEmpty()) {
                        item {
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Söwda habarlary we baha teklipleri şu ýerde görkeziler.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { inquiry ->
                            val isSellerSide = inquiry.sellerId == currentUserId
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("inquiry_card_${inquiry.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = inquiry.listingTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = formatRelativeTimeTm(inquiry.updatedAt ?: inquiry.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = if (isSellerSide) "Alyjy: ${inquiry.senderName}" else "Satyjy: ${inquiry.sellerName}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(inquiry.message, style = MaterialTheme.typography.bodyMedium)
                                    if (inquiry.offerPriceTmt > 0L) {
                                        Text(
                                            text = "Teklip edilen baha: ${formatTmt(inquiry.offerPriceTmt)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = TurboEmerald,
                                        )
                                    }
                                    if (!inquiry.replyMessage.isNullOrBlank()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Jogap: ${inquiry.replyMessage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    if (isSellerSide && inquiry.statusEnum() != InquiryStatus.CLOSED) {
                                        OutlinedButton(
                                            onClick = { replyingInquiry = inquiry },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Jogap ber")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        var nameInput by remember(displayName) { mutableStateOf(displayName) }
        var regionInput by remember(userRegion) { mutableStateOf(userRegion) }
        var phoneInput by remember(userPhone) { mutableStateOf(userPhone) }

        ModalBottomSheet(onDismissRequest = { showEditProfile = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.profile_edit), style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(60) },
                    label = { Text("Adyňyz") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it.take(30) },
                    label = { Text("Telefon belgiňiz (+993 ...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input")
                )
                Text("Welaýat", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketplaceCatalog.carRegions.forEach { reg ->
                        FilterChip(
                            selected = regionInput == reg,
                            onClick = { regionInput = reg },
                            label = { Text(reg) },
                        )
                    }
                }
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, regionInput, phoneInput) {
                            showEditProfile = false
                        }
                    },
                    enabled = !isBusy && nameInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_profile_button")
                ) {
                    Text("Ýatda sakla")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (replyingInquiry != null) {
        val target = replyingInquiry!!
        var replyText by remember(target.id) { mutableStateOf(target.replyMessage ?: "") }
        ModalBottomSheet(onDismissRequest = { replyingInquiry = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${target.senderName} üçin jogap", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it.take(500) },
                    label = { Text("Jogabyňyz") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            onReplyInquiry(target.id, replyText) { replyingInquiry = null }
                        }
                    },
                    enabled = !isBusy && replyText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Jogap iber")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
