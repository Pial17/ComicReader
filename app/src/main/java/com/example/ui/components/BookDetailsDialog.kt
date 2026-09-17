package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.BookEntity
import com.example.ui.theme.CbzDarkSurfaceVariant
import com.example.ui.theme.CbzFavoriteRed
import com.example.ui.theme.CbzGoldOnPrimary
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookDetailsDialog(
    book: BookEntity,
    onRead: () -> Unit,
    onStartBeginning: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRescan: () -> Unit,
    onDelete: (deleteFile: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Comic?") },
            text = {
                Text("Remove \"${book.title}\" from your CBZ library? You can also choose whether to remove the file from storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete from Device")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(false)
                    }
                ) {
                    Text("Remove from Library Only")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Cover
                CbzCoverImage(
                    coverPath = book.coverPath,
                    title = book.title,
                    modifier = Modifier
                        .size(width = 130.dp, height = 185.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CbzDarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailRow("Page Count", "${book.pageCount} pages")
                        DetailRow("File Size", formatFileSize(book.fileSize))
                        DetailRow(
                            "Reading Progress",
                            if (book.progressPercent > 0f) "Page ${book.currentPage} (${book.progressPercent.toInt()}%)" else "Unread"
                        )
                        if (book.lastOpenedTime > 0L) {
                            DetailRow(
                                "Last Opened",
                                SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(book.lastOpenedTime))
                            )
                        }
                        DetailRow("Location", book.filePath.takeLast(40))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Favorite
                    TextButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (book.isFavorite) CbzFavoriteRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (book.isFavorite) "Favorited" else "Favorite")
                    }

                    // Rescan
                    TextButton(onClick = onRescan) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rescan")
                    }

                    // Share
                    TextButton(
                        onClick = { shareBook(context, book) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delete button
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Comic")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRead,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CbzGoldPrimary,
                    contentColor = CbzGoldOnPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("details_read_button")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (book.currentPage > 1) "Continue" else "Read", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (book.currentPage > 1) {
                OutlinedButton(
                    onClick = onStartBeginning,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("From Beginning")
                }
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = CbzTextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun shareBook(context: Context, book: BookEntity) {
    try {
        val file = File(book.filePath)
        if (file.exists()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/x-cbz"
                putExtra(Intent.EXTRA_SUBJECT, book.title)
                putExtra(Intent.EXTRA_TEXT, "Reading comic: ${book.title}")
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Comic"))
        }
    } catch (e: Exception) {
        // ignore
    }
}
