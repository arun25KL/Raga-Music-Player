package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemePreset
import com.example.model.FolderLocation
import com.example.model.Subfolder
import com.example.model.SupportedAudioFormat
import com.example.model.ThemeCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndSourceDialog(
    onDismiss: () -> Unit,
    folderLocations: List<FolderLocation>,
    enabledFormats: Set<String>,
    subfolders: List<Subfolder>,
    includeSubfolders: Boolean,
    excludedSubfolderIds: Set<String>,
    appThemePreset: AppThemePreset = AppThemePreset.SYSTEM_DEFAULT,
    isDynamicThemeEnabled: Boolean = false,
    isOledPureBlackEnabled: Boolean = false,
    isDynamicArtworkColorEnabled: Boolean = false,
    isOnlyCallsInterruptEnabled: Boolean = true,
    onAddFolderClicked: () -> Unit,
    onScanDeviceClicked: () -> Unit,
    onRemoveFolderLocation: (String) -> Unit,
    onToggleAudioFormat: (SupportedAudioFormat) -> Unit,
    onEnableAllFormats: () -> Unit,
    onDisableAllFormats: () -> Unit,
    onToggleIncludeSubfolders: (Boolean) -> Unit,
    onToggleSubfolder: (String) -> Unit,
    onIncludeAllSubfolders: () -> Unit,
    onExcludeAllSubfolders: () -> Unit,
    onSelectAppThemePreset: (AppThemePreset) -> Unit = {},
    onToggleDynamicTheme: (Boolean) -> Unit = {},
    onToggleOledPureBlack: (Boolean) -> Unit = {},
    onToggleDynamicArtworkColor: (Boolean) -> Unit = {},
    onToggleOnlyCallsInterrupt: (Boolean) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nonRootSubfolders = subfolders.filter { !it.isRoot }
    val includedSubCount = nonRootSubfolders.count { it.id !in excludedSubfolderIds }
    var selectedCategoryFilter by remember { mutableStateOf<ThemeCategory?>(null) }
    var isThemePresetsExpanded by remember { mutableStateOf(false) }
    var isAudioFormatsExpanded by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val bgColor = MaterialTheme.colorScheme.background

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(borderColor)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Settings & Customization",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Text(
                                text = "Folders, subfolders, theme presets & audio formats",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_settings_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Folder Selection / Locations Management Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_folder_selection"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "1. FOLDER SELECTION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "${folderLocations.size} source(s) configured",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (folderLocations.isEmpty()) {
                            Text(
                                text = "No folder locations added yet. Add folders or scan device audio below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                folderLocations.forEach { loc ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(containerColor)
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (loc.isDeviceStorage) Icons.Default.PhoneAndroid else Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = loc.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = primaryColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${loc.trackCount} tracks • ${if (loc.isDeviceStorage) "Device Storage" else "Selected Folder"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onRemoveFolderLocation(loc.id) },
                                            modifier = Modifier.size(32.dp).testTag("btn_remove_folder_${loc.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Remove location",
                                                tint = Color(0xFFFF8A80),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons to add folder location or scan device
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onAddFolderClicked,
                                modifier = Modifier.weight(1f).testTag("btn_add_folder_location"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Folder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = onScanDeviceClicked,
                                modifier = Modifier.weight(1f).testTag("btn_scan_device_location"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.7f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = secondaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan Device",
                                    color = secondaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Include Subfolders (Merged master switch + selective subfolders list)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_include_subfolders"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "2. INCLUDE SUBFOLDERS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = if (includeSubfolders) "$includedSubCount of ${nonRootSubfolders.size} subfolders active" else "Disabled (root only)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (includeSubfolders) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = if (includeSubfolders) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }

                            Switch(
                                checked = includeSubfolders,
                                onCheckedChange = onToggleIncludeSubfolders,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = primaryColor,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = borderColor
                                ),
                                modifier = Modifier.size(36.dp).testTag("switch_include_subfolders")
                            )
                        }

                        if (includeSubfolders && nonRootSubfolders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = borderColor.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECTIVE SUBFOLDERS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.8.sp
                                )
                                Row {
                                    TextButton(onClick = onIncludeAllSubfolders) {
                                        Text("All", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = onExcludeAllSubfolders) {
                                        Text("None", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            nonRootSubfolders.forEach { sub ->
                                val isChecked = sub.id !in excludedSubfolderIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleSubfolder(sub.id) }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onToggleSubfolder(sub.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = primaryColor,
                                            checkmarkColor = Color.Black,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.size(32.dp).testTag("check_subfolder_${sub.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sub.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isChecked) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${sub.trackCount} tracks • ${sub.relativePath}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Theme Presets (Dropdown Accordion)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_theme_presets"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isThemePresetsExpanded = !isThemePresetsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "3. THEME PRESETS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "Active: ${appThemePreset.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Background preview swatch
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(appThemePreset.backgroundColor)
                                        .border(1.dp, appThemePreset.primaryColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(appThemePreset.primaryColor)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = primaryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${AppThemePreset.entries.size} Themes",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isThemePresetsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isThemePresetsExpanded) "Collapse" else "Expand",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isThemePresetsExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = borderColor.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Album Art Dynamic Color Engine Switch
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(containerColor.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Album Art Dynamic Color Engine",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Extract vibrant artwork palette to tint player controls",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isDynamicArtworkColorEnabled,
                                        onCheckedChange = onToggleDynamicArtworkColor,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = primaryColor
                                        ),
                                        modifier = Modifier.testTag("switch_dynamic_artwork_color")
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // OLED Pure Black Mode Switch
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(containerColor.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Brightness2,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "OLED Pure Black Mode",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color.Black,
                                                    border = BorderStroke(1.dp, primaryColor)
                                                ) {
                                                    Text(
                                                        text = "#000000",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        color = primaryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Pitch black background for dark themes to save OLED battery",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isOledPureBlackEnabled,
                                        onCheckedChange = onToggleOledPureBlack,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = primaryColor
                                        ),
                                        modifier = Modifier.testTag("switch_oled_pure_black")
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = borderColor.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Category Filter Tabs (All, Monochrome & Light, Dark & OLED, Single Colors)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedCategoryFilter == null,
                                        onClick = { selectedCategoryFilter = null },
                                        label = { Text("All (${AppThemePreset.entries.size})", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = containerColor,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedContainerColor = primaryColor.copy(alpha = 0.25f),
                                            selectedLabelColor = primaryColor
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    ThemeCategory.entries.forEach { cat ->
                                        val isCatSelected = selectedCategoryFilter == cat
                                        val count = AppThemePreset.entries.count { it.category == cat }
                                        FilterChip(
                                            selected = isCatSelected,
                                            onClick = { selectedCategoryFilter = if (isCatSelected) null else cat },
                                            label = { Text("${cat.title} ($count)", fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = containerColor,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                selectedContainerColor = primaryColor.copy(alpha = 0.25f),
                                                selectedLabelColor = primaryColor
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val filteredPresets = if (selectedCategoryFilter != null) {
                                    AppThemePreset.entries.filter { it.category == selectedCategoryFilter }
                                } else {
                                    AppThemePreset.entries
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredPresets.forEach { preset ->
                                        val isSelected = preset == appThemePreset
                                        val isLuxury = preset.category == ThemeCategory.LUXURY_AND_STUDIO
                                        Surface(
                                            onClick = { onSelectAppThemePreset(preset) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) preset.primaryColor.copy(alpha = 0.22f) else containerColor,
                                            border = BorderStroke(
                                                if (isSelected) 2.dp else 1.dp,
                                                if (isSelected) preset.primaryColor else borderColor.copy(alpha = 0.6f)
                                            ),
                                            modifier = Modifier
                                                .shadow(
                                                    elevation = if (isSelected) 6.dp else 0.dp,
                                                    shape = RoundedCornerShape(12.dp),
                                                    spotColor = preset.primaryColor.copy(alpha = 0.4f)
                                                )
                                                .testTag("chip_settings_theme_${preset.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Dual accent pill preview
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = 22.dp, height = 14.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(preset.backgroundColor)
                                                        .border(1.dp, preset.primaryColor.copy(alpha = 0.8f), RoundedCornerShape(7.dp))
                                                ) {
                                                    Row(modifier = Modifier.fillMaxSize()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .background(preset.primaryColor)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .background(preset.secondaryColor)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(
                                                    text = when {
                                                        preset == AppThemePreset.SYSTEM_DEFAULT -> "⚙ " + preset.title
                                                        isLuxury -> "✦ " + preset.title
                                                        preset.isLight -> "☀ " + preset.title
                                                        else -> preset.title
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) preset.primaryColor else MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Dynamic Album Art Theming Switch Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_dynamic_theming"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "4. DYNAMIC ALBUM ART THEMING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = if (isDynamicThemeEnabled) "Active: Tints accent colors to match album vinyl art" else "Disabled: Using static app preset colors",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDynamicThemeEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (isDynamicThemeEnabled) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Switch(
                            checked = isDynamicThemeEnabled,
                            onCheckedChange = onToggleDynamicTheme,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = containerColor
                            ),
                            modifier = Modifier.testTag("switch_dynamic_theming_settings")
                        )
                    }
                }
            }

            // 5. Supported Audio Formats (Dropdown Accordion)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_supported_formats"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isAudioFormatsExpanded = !isAudioFormatsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "5. SUPPORTED AUDIO FORMATS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "${enabledFormats.size} of ${SupportedAudioFormat.entries.size} formats enabled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = primaryColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${enabledFormats.size} Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isAudioFormatsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isAudioFormatsExpanded) "Collapse" else "Expand",
                                    tint = primaryColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isAudioFormatsExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = borderColor.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "SELECTIVE CODECS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                    Row {
                                        TextButton(onClick = onEnableAllFormats) {
                                            Text("All", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = onDisableAllFormats) {
                                            Text("None", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Format chips/toggles
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SupportedAudioFormat.entries.forEach { format ->
                                        val isEnabled = format.name in enabledFormats
                                        FilterChip(
                                            selected = isEnabled,
                                            onClick = { onToggleAudioFormat(format) },
                                            label = {
                                                Column {
                                                    Text(
                                                        text = format.displayName,
                                                        fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.sp
                                                    )
                                                    Text(
                                                        text = format.description,
                                                        fontSize = 9.sp,
                                                        color = if (isEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                containerColor = containerColor,
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                selectedContainerColor = primaryColor.copy(alpha = 0.25f),
                                                selectedLabelColor = primaryColor
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isEnabled,
                                                borderColor = if (isEnabled) primaryColor else borderColor
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("chip_format_${format.name}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Notifications & Call Interruption Setting Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_notification_interruption"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOnlyCallsInterruptEnabled) Icons.Default.PhoneAndroid else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "6. NOTIFICATIONS & CALLS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = if (isOnlyCallsInterruptEnabled) {
                                        "Only calls can interrupt music (notifications won't duck or pause songs)"
                                    } else {
                                        "All notifications & calls can interrupt music (music ducks for alerts)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnlyCallsInterruptEnabled) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = if (isOnlyCallsInterruptEnabled) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Switch(
                            checked = isOnlyCallsInterruptEnabled,
                            onCheckedChange = onToggleOnlyCallsInterrupt,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = primaryColor,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = containerColor
                            ),
                            modifier = Modifier.testTag("switch_notification_interruption")
                        )
                    }
                }
            }

            // 7. About Details Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_about_details"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "7. ABOUT DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Made in India",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🇮🇳",
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "With Help of Google By Arun",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Support Me if You Like Our App",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val uriHandler = LocalUriHandler.current
                        val linkUrl = "https://buymeachai.in/arun25"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = primaryColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable {
                                    try {
                                        uriHandler.openUri(linkUrl)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .testTag("support_hyperlink")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Support Link",
                                    tint = primaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "buymeachai.in/arun25",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
