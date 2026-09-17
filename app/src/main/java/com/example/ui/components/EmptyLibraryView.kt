package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CbzDarkSurfaceContainer
import com.example.ui.theme.CbzGoldOnPrimary
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary

@Composable
fun EmptyLibraryView(
    onScanNow: () -> Unit,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit,
    hasStoragePermission: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic Icon with glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CbzGoldPrimary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = CbzDarkSurfaceContainer
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = CbzGoldPrimary,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your Library is Empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (hasStoragePermission) {
                "Scan your whole device to automatically detect all CBZ and comic archives across every folder, SD card, and download directory."
            } else {
                "Grant all files access so the app can scan all folders on your device to find and organize your CBZ comic books."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = CbzTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Button(
            onClick = onScanNow,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CbzGoldPrimary,
                contentColor = CbzGoldOnPrimary
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp)
                .testTag("empty_scan_button")
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasStoragePermission) "Scan Whole Device" else "Grant Storage Access & Scan",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onChooseFolder,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(44.dp)
                .testTag("empty_choose_folder_button")
        ) {
            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Specific Folder")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenFile,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(44.dp)
                .testTag("empty_open_file_button")
        ) {
            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Single CBZ File")
        }
    }
}
