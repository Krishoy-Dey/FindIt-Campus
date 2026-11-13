package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CampusItem
import com.example.data.model.ItemCategory
import com.example.ui.components.CampusItemCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.icon
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.viewmodel.FeedFilter
import com.example.ui.viewmodel.FeedFiltersState
import com.example.ui.viewmodel.UiState
import com.example.util.PhoneUtils

@Composable
fun FeedScreen(
    itemsState: UiState<List<CampusItem>>,
    filters: FeedFiltersState,
    currentUserId: String,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (FeedFilter) -> Unit,
    onCategoryToggle: (ItemCategory) -> Unit,
    onClearFilters: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = filters.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("feed_search_input"),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search campus items",
                    tint = ElectricCyan
                )
            },
            trailingIcon = {
                if (filters.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchChange("") },
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = SlateOutline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Status / Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeedFilter.entries.forEach { filter ->
                val selected = filters.feedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelect(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateSurface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = CyanContainer,
                        selectedLabelColor = OnCyanContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SlateOutline,
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ItemCategory.entries.forEach { category ->
                val selected = filters.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { onCategoryToggle(category) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (selected) OnCyanContainer else ElectricCyan
                        )
                    },
                    label = {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.testTag("category_chip_${category.key}"),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SlateSurfaceElevated,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = CyanContainer,
                        selectedLabelColor = OnCyanContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SlateOutline.copy(alpha = 0.6f),
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (itemsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }

            is UiState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = itemsState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is UiState.Success -> {
                if (itemsState.data.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Outlined.Explore,
                            title = stringResource(R.string.empty_feed_title),
                            subtitle = stringResource(R.string.empty_feed_subtitle)
                        )
                        if (filters.searchQuery.isNotEmpty() ||
                            filters.feedFilter != FeedFilter.ALL ||
                            filters.selectedCategory != null
                        ) {
                            TextButton(
                                onClick = onClearFilters,
                                modifier = Modifier.testTag("reset_filters_button")
                            ) {
                                Text("Reset filters", color = ElectricCyan)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(key = "feed_count_header") {
                            Text(
                                text = "${itemsState.data.size} campus ${if (itemsState.data.size == 1) "item" else "items"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        items(itemsState.data, key = { it.id }) { item ->
                            CampusItemCard(
                                item = item,
                                isOwner = item.reporterId == currentUserId,
                                onClick = { onItemClick(item.id) },
                                onCallClick = { PhoneUtils.dialPhoneNumber(context, item.contactPhone) }
                            )
                        }
                    }
                }
            }
        }
    }
}
