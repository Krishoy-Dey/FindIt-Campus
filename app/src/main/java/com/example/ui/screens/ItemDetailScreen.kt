package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CampusItem
import com.example.data.model.ItemClaim
import com.example.data.model.ItemStatus
import com.example.data.model.ItemType
import com.example.ui.components.Base64ItemImage
import com.example.ui.components.ItemTypeBadge
import com.example.ui.theme.CoralRose
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.WarmAmber
import com.example.ui.viewmodel.UiState
import com.example.util.PhoneUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemState: UiState<CampusItem?>,
    claims: List<ItemClaim>,
    currentUserId: String,
    savedPhone: String,
    isSubmittingClaim: Boolean,
    onUpdateStatus: (ItemStatus) -> Unit,
    onDeleteItem: () -> Unit,
    onSubmitClaim: (phone: String, message: String, onDone: () -> Unit) -> Unit,
    onUpdateClaimStatus: (claimId: String, status: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClaimDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Item Details", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    val currentItem = (itemState as? UiState.Success)?.data
                    if (currentItem != null && currentItem.reporterId == currentUserId) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_item_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete_report),
                                tint = CoralRose
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (itemState) {
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
                        text = itemState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                is UiState.Success -> {
                    val item = itemState.data
                    if (item == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "This report is no longer available.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val isOwner = item.reporterId == currentUserId
                        val isRecovered = item.itemStatus == ItemStatus.RECOVERED

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Photo Hero
                            Base64ItemImage(
                                photoBase64 = item.photoBase64,
                                category = item.itemCategory,
                                contentDescription = "${item.title} photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                                    .clip(RoundedCornerShape(22.dp))
                            )

                            // Status & Category Badges
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ItemTypeBadge(type = item.itemType, status = item.itemStatus)
                                Surface(
                                    color = SlateSurfaceElevated,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = item.itemCategory.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            // Metadata Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.7f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DetailMetaRow(
                                        icon = Icons.Filled.LocationOn,
                                        label = "Campus Ground",
                                        value = item.location
                                    )
                                    DetailMetaRow(
                                        icon = Icons.Filled.Person,
                                        label = "Reported By",
                                        value = if (isOwner) "${item.reporterName} (You)" else item.reporterName
                                    )
                                    DetailMetaRow(
                                        icon = Icons.Filled.Schedule,
                                        label = "Posted",
                                        value = PhoneUtils.formatRelativeTime(item.createdAt)
                                    )
                                }
                            }

                            // Description Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.7f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Description",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ElectricCyan
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Direct Call & Pickup Coordination Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
                                border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.55f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Phone,
                                            contentDescription = "Contact phone",
                                            tint = EmeraldMint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Pickup Contact Number",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = item.contactPhone,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = { PhoneUtils.dialPhoneNumber(context, item.contactPhone) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .testTag("call_reporter_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldMint,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Call,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.action_call_to_coordinate),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(item.contactPhone))
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Phone number copied")
                                                }
                                            },
                                            modifier = Modifier
                                                .height(50.dp)
                                                .testTag("copy_phone_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, SlateOutline)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ContentCopy,
                                                contentDescription = stringResource(R.string.action_copy_number),
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (!isOwner && !isRecovered) {
                                        OutlinedButton(
                                            onClick = { showClaimDialog = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("claim_item_button"),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, ElectricCyan)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Handshake,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (item.itemType == ItemType.LOST) {
                                                    "I Found This — Notify Reporter"
                                                } else {
                                                    "This Is Mine — Request Pickup"
                                                },
                                                color = ElectricCyan,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Owner Status Management
                            if (isOwner) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                    border = BorderStroke(1.dp, SlateOutline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Manage Report Status",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isRecovered) {
                                            Surface(
                                                color = EmeraldContainer,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = OnEmeraldContainer
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = "Item marked as Recovered and archived.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = OnEmeraldContainer
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                if (item.itemStatus == ItemStatus.OPEN) {
                                                    OutlinedButton(
                                                        onClick = { onUpdateStatus(ItemStatus.CLAIMED) },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                            .testTag("mark_claimed_button"),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.dp, WarmAmber)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.action_mark_claimed),
                                                            color = WarmAmber,
                                                            style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                } else {
                                                    OutlinedButton(
                                                        onClick = { onUpdateStatus(ItemStatus.OPEN) },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                            .testTag("reopen_item_button"),
                                                        shape = RoundedCornerShape(12.dp),
                                                        border = BorderStroke(1.dp, ElectricCyan)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.action_reopen_item),
                                                            color = ElectricCyan,
                                                            style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = { onUpdateStatus(ItemStatus.RECOVERED) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                        .testTag("mark_recovered_button"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = EmeraldMint,
                                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                                    )
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.action_mark_recovered),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Claims / Pickup Coordination Notes
                            if (claims.isNotEmpty()) {
                                Text(
                                    text = if (isOwner) "Pickup Requests (${claims.size})" else "Your Pickup Note",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                claims.forEach { claim ->
                                    ClaimCard(
                                        claim = claim,
                                        isReporter = isOwner,
                                        onCallClaimant = { PhoneUtils.dialPhoneNumber(context, claim.claimantPhone) },
                                        onAccept = { onUpdateClaimStatus(claim.id, "accepted") },
                                        onDismiss = { onUpdateClaimStatus(claim.id, "dismissed") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        if (showClaimDialog) {
                            ClaimItemDialog(
                                itemTitle = item.title,
                                isLostItem = item.itemType == ItemType.LOST,
                                initialPhone = savedPhone,
                                isSubmitting = isSubmittingClaim,
                                onDismiss = { showClaimDialog = false },
                                onSubmit = { phone, msg ->
                                    onSubmitClaim(phone, msg) {
                                        showClaimDialog = false
                                    }
                                }
                            )
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                containerColor = SlateSurface,
                                title = { Text("Delete Report?") },
                                text = { Text("This will remove \"${item.title}\" from FindIt Campus.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showDeleteConfirm = false
                                            onDeleteItem()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralRose)
                                    ) {
                                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancel")
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
private fun DetailMetaRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ElectricCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ClaimCard(
    claim: ItemClaim,
    isReporter: Boolean,
    onCallClaimant: () -> Unit,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateOutline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = claim.claimantName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${claim.claimantPhone} • ${PhoneUtils.formatRelativeTime(claim.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricCyan
                    )
                }
                if (isReporter) {
                    IconButton(
                        onClick = onCallClaimant,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Call ${claim.claimantName}",
                            tint = EmeraldMint
                        )
                    }
                }
            }

            Text(
                text = claim.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isReporter && claim.status == "pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldMint),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Confirm Pickup", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            } else {
                Text(
                    text = "Status: ${claim.status.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (claim.status == "accepted") EmeraldMint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ClaimItemDialog(
    itemTitle: String,
    isLostItem: Boolean,
    initialPhone: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (phone: String, message: String) -> Unit
) {
    var phone by remember(initialPhone) { mutableStateOf(initialPhone) }
    var message by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateSurface,
        title = {
            Text(
                text = if (isLostItem) "Coordinate Return" else "Request Pickup",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Send your contact number & pickup note for \"$itemTitle\". The reporter will get an instant notification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        errorText = null
                    },
                    label = { Text("Your Phone Number *") },
                    placeholder = { Text("+1 555-234-5678") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SlateOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("claim_phone_input")
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                        errorText = null
                    },
                    label = { Text("Pickup Message *") },
                    placeholder = {
                        Text(
                            if (isLostItem) "I found this near Library desk — free to meet!"
                            else "I can describe the inside / stickers to verify!"
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SlateOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("claim_message_input")
                )
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralRose
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        !PhoneUtils.isValidPhoneNumber(phone) ->
                            errorText = "Enter a valid phone number (at least 7 digits)"
                        message.trim().length < 2 ->
                            errorText = "Enter a brief pickup message"
                        else -> onSubmit(phone, message)
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                modifier = Modifier.testTag("submit_claim_button")
            ) {
                Text(
                    text = if (isSubmitting) "Sending..." else "Send & Notify",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
