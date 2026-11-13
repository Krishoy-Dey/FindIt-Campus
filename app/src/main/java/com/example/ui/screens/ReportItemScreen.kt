package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ItemCategory
import com.example.data.model.ItemType
import com.example.ui.components.icon
import com.example.ui.theme.CoralRose
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnCyanContainer
import com.example.ui.theme.OnRoseContainer
import com.example.ui.theme.RoseContainer
import com.example.ui.theme.SlateOutline
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.util.PhoneUtils
import kotlinx.coroutines.launch

private val CampusLandmarks = listOf(
    "Main Library",
    "Student Union",
    "Science Hall",
    "Engineering Quad",
    "Cafeteria",
    "Campus Gym"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportItemScreen(
    savedPhone: String,
    isSubmitting: Boolean,
    onCompressPhoto: suspend (android.net.Uri) -> String?,
    onSubmit: (
        title: String,
        description: String,
        category: ItemCategory,
        type: ItemType,
        location: String,
        contactPhone: String,
        photoBase64: String?
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(ItemType.LOST) }
    var selectedCategory by remember { mutableStateOf(ItemCategory.ELECTRONICS) }
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf(savedPhone) }

    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var compressedPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var isCompressingPhoto by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedPhone) {
        if (contactPhone.isBlank() && savedPhone.isNotBlank()) {
            contactPhone = savedPhone
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                isCompressingPhoto = true
                scope.launch {
                    val encoded = onCompressPhoto(uri)
                    compressedPhotoBase64 = encoded
                    isCompressingPhoto = false
                    if (encoded == null) {
                        validationError = "Selected photo could not be processed. Try another image."
                        selectedPhotoUri = null
                    }
                }
            }
        }
    )

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.action_report_item),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("report_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Lost / Found Toggle
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_type_selector")
                ) {
                    ItemType.entries.forEachIndexed { index, type ->
                        val isSelected = selectedType == type
                        SegmentedButton(
                            selected = isSelected,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ItemType.entries.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = if (type == ItemType.LOST) RoseContainer else CyanContainer,
                                activeContentColor = if (type == ItemType.LOST) OnRoseContainer else OnCyanContainer,
                                inactiveContainerColor = SlateSurface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("type_option_${type.key}")
                        ) {
                            Text(
                                text = if (type == ItemType.LOST) "I Lost Something" else "I Found Something",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                // Photo Upload Section (Zero-Permission Android Photo Picker)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, SlateOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = !isCompressingPhoto) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("photo_upload_picker")
                ) {
                    if (selectedPhotoUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedPhotoUri,
                                contentDescription = "Selected item photo preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (isCompressingPhoto) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ObsidianBg.copy(alpha = 0.65f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ElectricCyan)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = ObsidianBg.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedPhotoUri = null
                                            compressedPhotoBase64 = null
                                        },
                                        modifier = Modifier.testTag("remove_photo_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Remove photo",
                                            tint = CoralRose,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Remove", color = CoralRose)
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Upload photo",
                                tint = ElectricCyan,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Item Photo (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to choose from gallery — helps students verify faster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Item Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Item Title *") },
                    placeholder = { Text("e.g., Silver MacBook Charger, Blue ID Card") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reportFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_title_input")
                )

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ItemCategory.entries.forEach { cat ->
                            val selected = selectedCategory == cat
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategory = cat },
                                leadingIcon = {
                                    Icon(
                                        imageVector = cat.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selected) OnCyanContainer else ElectricCyan
                                    )
                                },
                                label = { Text(cat.label) },
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
                                ),
                                modifier = Modifier.testTag("report_category_${cat.key}")
                            )
                        }
                    }
                }

                // Campus Location + Quick Chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {
                            location = it
                            validationError = null
                        },
                        label = { Text("Campus Location *") },
                        placeholder = { Text("e.g., Main Library 2nd Floor") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = ElectricCyan
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = reportFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_location_input")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CampusLandmarks.forEach { landmark ->
                            FilterChip(
                                selected = location.equals(landmark, ignoreCase = true),
                                onClick = {
                                    location = landmark
                                    validationError = null
                                },
                                label = { Text(landmark, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SlateSurfaceElevated,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = CyanContainer,
                                    selectedLabelColor = OnCyanContainer
                                )
                            )
                        }
                    }
                }

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        validationError = null
                    },
                    label = { Text("Description & Identifying Marks *") },
                    placeholder = { Text("Color, stickers, time last seen, or desk number...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = reportFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_description_input")
                )

                // Required Contact Phone Number
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = {
                        contactPhone = it
                        validationError = null
                    },
                    label = { Text("Your Contact Phone Number *") },
                    placeholder = { Text("+1 555-234-5678") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    supportingText = {
                        Text(
                            text = stringResource(R.string.phone_visibility_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reportFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("report_phone_input")
                )

                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        when {
                            title.trim().length < 2 ->
                                validationError = "Please enter an item title (at least 2 characters)."
                            location.trim().length < 2 ->
                                validationError = "Please enter the campus location."
                            description.trim().length < 2 ->
                                validationError = "Please add a short description to help verify the item."
                            !PhoneUtils.isValidPhoneNumber(contactPhone) ->
                                validationError = "Please enter a valid contact phone number (at least 7 digits)."
                            isCompressingPhoto ->
                                validationError = "Photo is still processing, please wait a moment."
                            else -> {
                                onSubmit(
                                    title,
                                    description,
                                    selectedCategory,
                                    selectedType,
                                    location,
                                    contactPhone,
                                    compressedPhotoBase64
                                )
                            }
                        }
                    },
                    enabled = !isSubmitting && !isCompressingPhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("submit_report_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Posting...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Text(
                            text = "Publish ${selectedType.label} Report",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun reportFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = SlateSurface,
    unfocusedContainerColor = SlateSurface,
    focusedBorderColor = ElectricCyan,
    unfocusedBorderColor = SlateOutline,
    focusedLabelColor = ElectricCyan,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)
