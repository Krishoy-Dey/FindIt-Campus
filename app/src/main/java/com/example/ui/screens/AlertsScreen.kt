package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserNotification
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.viewmodel.UiState
import com.example.util.CampusNotificationHelper
import com.example.util.PhoneUtils

@Composable
fun AlertsScreen(
    notificationsState: UiState<List<UserNotification>>,
    systemNotificationsEnabled: Boolean,
    onToggleSystemNotifications: (Boolean) -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: (List<UserNotification>) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasOsPermission by remember {
        mutableStateOf(CampusNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasOsPermission = granted
            onToggleSystemNotifications(granted)
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Real-time system alert preferences card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CyanContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Real-time campus notifications",
                        tint = ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Device Alerts",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasOsPermission) {
                            "Notify on new campus reports & pickup claims"
                        } else {
                            "Tap switch to allow Android notifications"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = systemNotificationsEnabled && hasOsPermission,
                    onCheckedChange = { wantEnable ->
                        if (wantEnable && !hasOsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onToggleSystemNotifications(wantEnable)
                        }
                    },
                    modifier = Modifier.testTag("system_notifications_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = ElectricCyan
                    )
                )
            }
        }

        when (notificationsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                }
            }

            is UiState.Error -> {
                Text(
                    text = notificationsState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            is UiState.Success -> {
                val list = notificationsState.data
                val unreadCount = list.count { !it.read }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (unreadCount > 0) "$unreadCount unread alerts" else "Recent alerts",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { onMarkAllRead(list) },
                            modifier = Modifier.testTag("mark_all_read_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_mark_all_read),
                                color = ElectricCyan
                            )
                        }
                    }
                }

                if (list.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Outlined.NotificationsNone,
                            title = stringResource(R.string.empty_alerts_title),
                            subtitle = stringResource(R.string.empty_alerts_subtitle)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onClick = {
                                    if (!notif.read) onMarkRead(notif.id)
                                    onOpenItem(notif.itemId)
                                },
                                onCallSender = {
                                    val phone = notif.senderPhone
                                    if (!phone.isNullOrBlank()) {
                                        if (!notif.read) onMarkRead(notif.id)
                                        PhoneUtils.dialPhoneNumber(context, phone)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: UserNotification,
    onClick: () -> Unit,
    onCallSender: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) SlateSurface else SlateSurfaceElevated
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (notification.read) SlateOutline.copy(alpha = 0.6f) else ElectricCyan.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!notification.read) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.read) FontWeight.Medium else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${notification.itemTitle} • ${PhoneUtils.formatRelativeTime(notification.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
            }

            if (!notification.senderPhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onCallSender,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("notification_call_button_${notification.id}"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = EmeraldMint.copy(alpha = 0.16f),
                        contentColor = EmeraldMint
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call ${notification.senderName}"
                    )
                }
            }
        }
    }
}
