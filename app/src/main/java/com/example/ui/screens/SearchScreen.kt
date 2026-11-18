package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Listing
import com.example.data.model.MarketplaceCatalog
import com.example.data.model.SearchFilter
import com.example.data.model.SortOrder
import com.example.ui.components.ListingCard
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    listingsState: UiState<List<Listing>>,
    filter: SearchFilter,
    favoriteIds: Set<String>,
    onUpdateFilter: ((SearchFilter) -> SearchFilter) -> Unit,
    onResetFilter: () -> Unit,
    onOpenListing: (String) -> Unit,
    onToggleFavorite: (Listing) -> Unit,
) {
    var showAdvancedSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 720.dp) 2 else 1

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Giňişleýin gözleg",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    TextButton(
                        onClick = onResetFilter,
                        modifier = Modifier.testTag("search_reset_button")
                    ) {
                        Text(stringResource(R.string.filter_reset))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = { q -> onUpdateFilter { it.copy(query = q) } },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filter.query.isNotEmpty()) {
                                IconButton(onClick = { onUpdateFilter { it.copy(query = "") } }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Arassala")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_query_input")
                    )
                    Button(
                        onClick = { showAdvancedSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        modifier = Modifier.testTag("open_advanced_filter_button")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = "Filtrler")
                    }
                }
            }

            // Popular Car Makes
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Meşhur markalar", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filter.make.isBlank(),
                                onClick = { onUpdateFilter { it.copy(make = "") } },
                                label = { Text("Ählisi") },
                            )
                        }
                        items(MarketplaceCatalog.popularMakesAndModels.keys.toList()) { make ->
                            FilterChip(
                                selected = filter.make == make,
                                onClick = {
                                    onUpdateFilter {
                                        it.copy(make = if (it.make == make) "" else make)
                                    }
                                },
                                label = { Text(make) },
                                modifier = Modifier.testTag("make_chip_$make")
                            )
                        }
                    }
                }
            }

            // Sort Order Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SortOrder.entries) { order ->
                        FilterChip(
                            selected = filter.sortOrder == order,
                            onClick = { onUpdateFilter { it.copy(sortOrder = order) } },
                            label = { Text(order.labelTm) },
                            modifier = Modifier.testTag("sort_${order.name}")
                        )
                    }
                }
            }

            when (listingsState) {
                is UiState.Loading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Ýüklenýär...")
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Text(
                            text = listingsState.message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                is UiState.Success -> {
                    val items = listingsState.data
                    item {
                        Text(
                            text = "Tapylan bildiriş: ${items.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (items.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("Saýlanan şertler boýunça bildiriş tapylmady", style = MaterialTheme.typography.titleMedium)
                                    OutlinedButton(onClick = onResetFilter) {
                                        Text("Filtrleri arassala")
                                    }
                                }
                            }
                        }
                    } else {
                        val rows = items.chunked(columns)
                        items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowItems.forEach { listing ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ListingCard(
                                            listing = listing,
                                            isFavorite = favoriteIds.contains(listing.id),
                                            onClick = { onOpenListing(listing.id) },
                                            onToggleFavorite = { onToggleFavorite(listing) },
                                        )
                                    }
                                }
                                if (rowItems.size < columns) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdvancedSheet) {
        var minYearText by remember(filter.minYear) { mutableStateOf(filter.minYear?.toString() ?: "") }
        var maxYearText by remember(filter.maxYear) { mutableStateOf(filter.maxYear?.toString() ?: "") }
        var minPriceText by remember(filter.minPriceTmt) { mutableStateOf(filter.minPriceTmt?.toString() ?: "") }
        var maxPriceText by remember(filter.maxPriceTmt) { mutableStateOf(filter.maxPriceTmt?.toString() ?: "") }

        ModalBottomSheet(onDismissRequest = { showAdvancedSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Goşmaça filtrler", style = MaterialTheme.typography.titleLarge)

                Text("Ýyly", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minYearText,
                        onValueChange = { minYearText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Başlap (2015)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxYearText,
                        onValueChange = { maxYearText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Çenli (2026)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Baha aralygy (TMT)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = minPriceText,
                        onValueChange = { minPriceText = it.filter(Char::isDigit).take(9) },
                        label = { Text("Iň az") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxPriceText,
                        onValueChange = { maxPriceText = it.filter(Char::isDigit).take(9) },
                        label = { Text("Iň köp") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Korobka", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketplaceCatalog.transmissions.forEach { trans ->
                        FilterChip(
                            selected = filter.transmission == trans,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(transmission = if (it.transmission == trans) "" else trans)
                                }
                            },
                            label = { Text(trans) },
                        )
                    }
                }

                Text("Ýangyç görnüşi", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarketplaceCatalog.fuelTypes.forEach { fuel ->
                        FilterChip(
                            selected = filter.fuelType == fuel,
                            onClick = {
                                onUpdateFilter {
                                    it.copy(fuelType = if (it.fuelType == fuel) "" else fuel)
                                }
                            },
                            label = { Text(fuel) },
                        )
                    }
                }

                Button(
                    onClick = {
                        onUpdateFilter {
                            it.copy(
                                minYear = minYearText.toIntOrNull(),
                                maxYear = maxYearText.toIntOrNull(),
                                minPriceTmt = minPriceText.toLongOrNull(),
                                maxPriceTmt = maxPriceText.toLongOrNull(),
                            )
                        }
                        showAdvancedSheet = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("apply_advanced_filter_button")
                ) {
                    Text(stringResource(R.string.filter_apply))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
