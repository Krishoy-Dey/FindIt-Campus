package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CampusItem
import com.example.data.model.ItemCategory
import com.example.data.model.ItemStatus
import com.example.data.model.ItemType
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.CoralRose
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.OnAmberContainer
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OnRoseContainer
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.WarmAmber
import com.example.util.ImageUtils
import com.example.util.PhoneUtils

fun ItemCategory.icon(): ImageVector = when (this) {
    ItemCategory.ELECTRONICS -> Icons.Filled.Devices
    ItemCategory.ID_KEYS -> Icons.Filled.Key
    ItemCategory.BAGS -> Icons.Filled.Backpack
    ItemCategory.BOOKS -> Icons.Filled.MenuBook
    ItemCategory.ACCESSORIES -> Icons.Filled.Watch
    ItemCategory.CLOTHING -> Icons.Filled.Checkroom
    ItemCategory.OTHER -> Icons.Filled.Category
}

@Composable
fun ItemTypeBadge(
    type: ItemType,
    status: ItemStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, label) = when {
        status == ItemStatus.RECOVERED -> Triple(EmeraldContainer, OnEmeraldContainer, "RECOVERED")
        status == ItemStatus.CLAIMED -> Triple(AmberContainer, OnAmberContainer, "PICKUP PENDING")
        type == ItemType.LOST -> Triple(RoseContainer, OnRoseContainer, "LOST")
        else -> Triple(CyanContainer, OnCyanContainer, "FOUND")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Base64ItemImage(
    photoBase64: String?,
    category: ItemCategory,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(photoBase64) {
        ImageUtils.decodeBase64ToImageBitmap(photoBase64)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(SlateSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = contentDescription,
                tint = ElectricCyan,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun CampusItemCard(
    item: CampusItem,
    isOwner: Boolean,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("campus_item_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.itemStatus == ItemStatus.RECOVERED) {
                EmeraldMint.copy(alpha = 0.35f)
            } else {
                SlateOutline.copy(alpha = 0.7f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Base64ItemImage(
                photoBase64 = item.photoBase64,
                category = item.itemCategory,
                contentDescription = "${item.title} thumbnail",
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ItemTypeBadge(type = item.itemType, status = item.itemStatus)
                    if (isOwner) {
                        Surface(
                            color = SlateSurfaceElevated,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Campus location",
                        tint = ElectricCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Posted time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${PhoneUtils.formatRelativeTime(item.createdAt)} • ${item.reporterName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (item.contactPhone.isNotBlank() && item.itemStatus != ItemStatus.RECOVERED) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("quick_call_button_${item.id}"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = EmeraldMint.copy(alpha = 0.16f),
                        contentColor = EmeraldMint
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call ${item.reporterName} at ${item.contactPhone}"
                    )
                }
            } else if (item.itemStatus == ItemStatus.RECOVERED) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Recovered",
                    tint = EmeraldMint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricCyan
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SlateSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
