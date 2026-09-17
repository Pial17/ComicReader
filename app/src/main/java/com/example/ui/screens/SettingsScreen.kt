package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScannedFolderEntity
import com.example.ui.model.LibraryViewMode
import com.example.ui.model.PageFit
import com.example.ui.model.ReadingDirection
import com.example.ui.model.ReadingMode
import com.example.ui.model.SortOrder
import com.example.ui.theme.CbzDarkBackground
import com.example.ui.theme.CbzDarkSurfaceContainer
import com.example.ui.theme.CbzDarkSurfaceVariant
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary
import com.example.ui.viewmodel.LibraryViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LibraryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = viewModel.settingsManager

    val readingMode by settingsManager.readingMode.collectAsState()
    val readingDirection by settingsManager.readingDirection.collectAsState()
    val pageFit by settingsManager.pageFit.collectAsState()
    val libraryViewMode by settingsManager.libraryViewMode.collectAsState()
    val sortOrder by settingsManager.sortOrder.collectAsState()
    val autoScan by settingsManager.autoScanOnStart.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var cacheSize by remember { mutableStateOf(viewModel.getThumbnailCacheSize()) }
    var folderToRemove by remember { mutableStateOf<ScannedFolderEntity?>(null) }

    if (folderToRemove != null) {
        AlertDialog(
            onDismissRequest = { folderToRemove = null },
            title = { Text("Remove Folder?") },
            text = { Text("Stop scanning \"${folderToRemove!!.displayName}\"? Files already in library will remain.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeFolder(folderToRemove!!.uriString)
                        folderToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CbzDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Reading Preferences Section
            SettingsSectionHeader(title = "Reading Preferences", icon = Icons.Default.MenuBook)

            Card(
                colors = CardDefaults.cardColors(containerColor = CbzDarkSurfaceContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Default Reading Mode
                    SettingsDropdownItem(
                        title = "Default Reading Mode",
                        currentValue = readingMode.label,
                        options = ReadingMode.values().map { it.label to it },
                        onSelect = { settingsManager.setReadingMode(it) }
                    )

                    // Default Direction
                    SettingsDropdownItem(
                        title = "Default Reading Direction",
                        currentValue = readingDirection.label,
                        options = ReadingDirection.values().map { it.label to it },
                        onSelect = { settingsManager.setReadingDirection(it) }
                    )

                    // Default Page Fit
                    SettingsDropdownItem(
                        title = "Default Page Fit",
                        currentValue = pageFit.label,
                        options = PageFit.values().map { it.label to it },
                        onSelect = { settingsManager.setPageFit(it) }
                    )
                }
            }

            // Library Preferences Section
            SettingsSectionHeader(title = "Library Preferences", icon = Icons.Default.Storage)

            Card(
                colors = CardDefaults.cardColors(containerColor = CbzDarkSurfaceContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Default View Mode
                    SettingsDropdownItem(
                        title = "Default Layout View",
                        currentValue = if (libraryViewMode == LibraryViewMode.GRID) "Grid View" else "List View",
                        options = listOf(
                            "Grid View" to LibraryViewMode.GRID,
                            "List View" to LibraryViewMode.LIST
                        ),
                        onSelect = { settingsManager.setLibraryViewMode(it) }
                    )

                    // Default Sort Order
                    SettingsDropdownItem(
                        title = "Default Sort Order",
                        currentValue = sortOrder.label,
                        options = SortOrder.values().map { it.label to it },
                        onSelect = { settingsManager.setSortOrder(it) }
                    )

                    // Auto-scan on launch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Scan on Launch",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Automatically search for new CBZ files when opening the app",
                                style = MaterialTheme.typography.bodySmall,
                                color = CbzTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = autoScan,
                            onCheckedChange = { settingsManager.setAutoScanOnStart(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CbzGoldPrimary,
                                checkedTrackColor = CbzDarkSurfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0x1AFFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                    // Whole Device Scan Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Scan Whole Device Now",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Deep search across all device storage, SD cards & downloads",
                                style = MaterialTheme.typography.bodySmall,
                                color = CbzTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.scanStorage()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CbzGoldPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Custom Scanned Folders
            if (folders.isNotEmpty()) {
                SettingsSectionHeader(title = "Custom Scanned Folders", icon = Icons.Default.Folder)
                Card(
                    colors = CardDefaults.cardColors(containerColor = CbzDarkSurfaceContainer),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        folders.forEach { folder ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = CbzGoldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = folder.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { folderToRemove = folder },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Folder",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Performance & Cache Section
            SettingsSectionHeader(title = "Performance & Storage", icon = Icons.Default.Speed)

            Card(
                colors = CardDefaults.cardColors(containerColor = CbzDarkSurfaceContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Thumbnail Cache Size",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatFileSize(cacheSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = CbzGoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.clearThumbnailCache()
                                cacheSize = 0L
                                Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cache", fontSize = 12.sp)
                        }
                    }
                }
            }

            // About Section
            SettingsSectionHeader(title = "About", icon = Icons.Default.Info)

            Card(
                colors = CardDefaults.cardColors(containerColor = CbzDarkSurfaceContainer),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CBZ Reader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CbzGoldPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0 • Offline Comic & Manga Reader",
                        style = MaterialTheme.typography.bodySmall,
                        color = CbzTextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Designed for seamless offline reading of comic archives (.cbz, .zip). No account, no cloud, no tracking. Your comics stay entirely on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CbzGoldPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun <T> SettingsDropdownItem(
    title: String,
    currentValue: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = currentValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CbzGoldPrimary
            )

            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (label, value) ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                fontWeight = if (label == currentValue) FontWeight.Bold else FontWeight.Normal,
                                color = if (label == currentValue) CbzGoldPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelect(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
