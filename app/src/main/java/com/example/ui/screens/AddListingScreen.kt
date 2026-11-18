package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Listing
import com.example.data.model.ListingBadge
import com.example.data.model.ListingCategory
import com.example.data.model.ListingStatus
import com.example.data.model.MarketplaceCatalog
import com.example.ui.components.ListingImage
import com.example.ui.components.encodePickedImageToBase64
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddListingScreen(
    existingListing: Listing?,
    isBusy: Boolean,
    onSave: (Listing) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var category by remember(existingListing?.id) {
        mutableStateOf(existingListing?.categoryEnum() ?: ListingCategory.CARS)
    }
    var title by remember(existingListing?.id) { mutableStateOf(existingListing?.title ?: "") }
    var make by remember(existingListing?.id) { mutableStateOf(existingListing?.make ?: "Toyota") }
    var model by remember(existingListing?.id) { mutableStateOf(existingListing?.model ?: "Camry") }
    var yearText by remember(existingListing?.id) { mutableStateOf((existingListing?.year ?: 2022L).toString()) }
    var priceText by remember(existingListing?.id) { mutableStateOf(existingListing?.priceTmt?.takeIf { it > 0 }?.toString() ?: "") }
    var mileageText by remember(existingListing?.id) { mutableStateOf((existingListing?.mileageKm ?: 0L).toString()) }
    var engineVolume by remember(existingListing?.id) { mutableStateOf(existingListing?.engineVolume ?: "2.5 L") }
    var transmission by remember(existingListing?.id) { mutableStateOf(existingListing?.transmission ?: "Awtomat") }
    var fuelType by remember(existingListing?.id) { mutableStateOf(existingListing?.fuelType ?: "Benzin") }
    var bodyType by remember(existingListing?.id) { mutableStateOf(existingListing?.bodyType ?: "Sedan") }
    var color by remember(existingListing?.id) { mutableStateOf(existingListing?.color ?: "Ak") }
    var region by remember(existingListing?.id) { mutableStateOf(existingListing?.region ?: "Aşgabat") }
    var badge by remember(existingListing?.id) { mutableStateOf(existingListing?.badgeEnum() ?: ListingBadge.NONE) }
    var description by remember(existingListing?.id) { mutableStateOf(existingListing?.description ?: "") }
    var negotiable by remember(existingListing?.id) { mutableStateOf(existingListing?.negotiable ?: true) }
    var tradeAvailable by remember(existingListing?.id) { mutableStateOf(existingListing?.tradeAvailable ?: false) }
    var creditAvailable by remember(existingListing?.id) { mutableStateOf(existingListing?.creditAvailable ?: false) }
    var imageKey by remember(existingListing?.id) { mutableStateOf(existingListing?.imageKey ?: "sedan") }
    var photoData by remember(existingListing?.id) { mutableStateOf(existingListing?.photoData) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = encodePickedImageToBase64(context, uri)
                if (encoded != null) {
                    photoData = encoded
                } else {
                    validationError = "Suraty işläp bolmady. Başga surat saýlaň."
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (existingListing == null) "Täze bildiriş goş" else "Bildirişi üýtget",
                    style = MaterialTheme.typography.headlineMedium,
                )
                OutlinedButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = "Ýap", modifier = Modifier.size(18.dp))
                }
            }

            // Photo Picker + Preset Gallery
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                    ) {
                        ListingImage(
                            imageKey = imageKey,
                            photoData = photoData,
                            contentDescription = "Bildiriş suraty",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_photo_button")
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Galereýadan surat")
                        }
                        if (photoData != null) {
                            OutlinedButton(onClick = { photoData = null }) {
                                Text("Suraty aýyr")
                            }
                        }
                    }

                    Text("Ýa-da taýyn salon suraty:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("sedan" to "Sedan", "suv" to "4x4 SUV", "luxury" to "Lýuks", "showroom" to "Salon").forEach { (key, label) ->
                            FilterChip(
                                selected = photoData == null && imageKey == key,
                                onClick = {
                                    photoData = null
                                    imageKey = key
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            // Category
            Text("Bölüm", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ListingCategory.entries) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.labelTm) },
                        modifier = Modifier.testTag("add_cat_${cat.key}")
                    )
                }
            }

            // Headline
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                label = { Text("Bildiriş ady *") },
                placeholder = { Text("Mysal: Toyota Camry XSE 2022") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title")
            )

            // Make & Model quick selectors for Cars
            if (category == ListingCategory.CARS) {
                Text("Marka saýlaň", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MarketplaceCatalog.popularMakesAndModels.keys.toList()) { m ->
                        FilterChip(
                            selected = make == m,
                            onClick = {
                                make = m
                                model = MarketplaceCatalog.popularMakesAndModels[m]?.firstOrNull() ?: ""
                            },
                            label = { Text(m) },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = make,
                    onValueChange = { make = it.take(50) },
                    label = { Text(if (category == ListingCategory.CARS) "Marka" else "Öndüriji / Görnüş") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_make")
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it.take(50) },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_model")
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter(Char::isDigit).take(9) },
                    label = { Text("Bahasy (TMT) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_price")
                )
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Ýyly *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("input_year")
                )
            }

            if (category == ListingCategory.CARS) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mileageText,
                        onValueChange = { mileageText = it.filter(Char::isDigit).take(7) },
                        label = { Text("Ýören ýoly (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_mileage")
                    )
                    OutlinedTextField(
                        value = engineVolume,
                        onValueChange = { engineVolume = it.take(20) },
                        label = { Text("Motor (2.5 L)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Korobka", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketplaceCatalog.transmissions.forEach { tr ->
                        FilterChip(selected = transmission == tr, onClick = { transmission = tr }, label = { Text(tr) })
                    }
                }

                Text("Ýangyç", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketplaceCatalog.fuelTypes.forEach { fl ->
                        FilterChip(selected = fuelType == fl, onClick = { fuelType = fl }, label = { Text(fl) })
                    }
                }

                Text("Kuzow", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketplaceCatalog.bodyTypes.forEach { bd ->
                        FilterChip(selected = bodyType == bd, onClick = { bodyType = bd }, label = { Text(bd) })
                    }
                }

                Text("Reňki", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MarketplaceCatalog.colors) { clr ->
                        FilterChip(selected = color == clr, onClick = { color = clr }, label = { Text(clr) })
                    }
                }
            }

            Text("Welaýat / Şäher", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MarketplaceCatalog.carRegions.forEach { reg ->
                    FilterChip(
                        selected = region == reg,
                        onClick = { region = reg },
                        label = { Text(reg) },
                        modifier = Modifier.testTag("add_region_$reg")
                    )
                }
            }

            Text("Bildiriş derejesi", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListingBadge.entries.forEach { b ->
                    FilterChip(
                        selected = badge == b,
                        onClick = { badge = b },
                        label = { Text(b.labelTm) },
                    )
                }
            }

            // Options Switches
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SwitchRow("Baha ylaşykly", negotiable) { negotiable = it }
                    SwitchRow("Obmen (Çalyşmak) mümkin", tradeAvailable) { tradeAvailable = it }
                    SwitchRow("Kredit / Bölekleýin töleg bar", creditAvailable) { creditAvailable = it }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(1500) },
                label = { Text("Goşmaça düşündiriş *") },
                placeholder = { Text("Ulagyň ýagdaýy, aýratynlyklary, dokumentleri barada ýazyň...") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_description")
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    val parsedPrice = priceText.toLongOrNull()
                    val parsedYear = yearText.toLongOrNull() ?: 2022L
                    val parsedMileage = mileageText.toLongOrNull() ?: 0L
                    val autoTitle = title.trim().ifBlank { "$make $model $parsedYear".trim() }
                    when {
                        autoTitle.length < 2 -> validationError = "Bildiriş adyny ýazyň (iň az 2 harp)."
                        parsedPrice == null || parsedPrice <= 0L -> validationError = "Dogry baha (TMT) giriziň."
                        parsedYear !in 1950L..2030L -> validationError = "Ýyly 1950–2030 aralygynda bolmaly."
                        description.trim().isEmpty() -> validationError = "Gysgaça düşündiriş ýazyň."
                        else -> {
                            validationError = null
                            onSave(
                                Listing(
                                    id = existingListing?.id ?: "",
                                    userId = existingListing?.userId ?: "",
                                    sellerName = existingListing?.sellerName ?: "",
                                    title = autoTitle,
                                    category = category.key,
                                    make = make.ifBlank { "Beýleki" },
                                    model = model.ifBlank { "Model" },
                                    year = parsedYear,
                                    priceTmt = parsedPrice,
                                    mileageKm = parsedMileage,
                                    transmission = transmission,
                                    fuelType = fuelType,
                                    engineVolume = engineVolume,
                                    bodyType = bodyType,
                                    color = color,
                                    region = region,
                                    description = description.trim(),
                                    badge = badge.key,
                                    status = existingListing?.status ?: ListingStatus.ACTIVE.key,
                                    negotiable = negotiable,
                                    tradeAvailable = tradeAvailable,
                                    creditAvailable = creditAvailable,
                                    imageKey = imageKey,
                                    photoData = photoData,
                                )
                            )
                        }
                    }
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_listing_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (existingListing == null) "Bildirişi ýerleşdir" else "Üýtgeşmeleri ýatda sakla")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
