package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.ui.theme.CbzFavoriteRed
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.theme.CbzTextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CbzCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("cbz_card_${book.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image with Badge & Favorite
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
            ) {
                CbzCoverImage(
                    coverPath = book.coverPath,
                    title = book.title,
                    modifier = Modifier.matchParentSize(),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                )

                // Favorite Heart Button (Top End)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp),
                    shape = CircleShape,
                    color = Color(0x77000000)
                ) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (book.isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (book.isFavorite) CbzFavoriteRed else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Page Count Tag (Bottom Start)
                if (book.pageCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xBB101115)
                    ) {
                        Text(
                            text = "${book.pageCount}p",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Progress Badge (Bottom End)
                if (book.progressPercent > 0f) {
                    val isFinished = book.progressPercent >= 100f
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isFinished) Color(0xDD2E7D32) else Color(0xDD211400)
                    ) {
                        Text(
                            text = if (isFinished) "Read" else "${book.progressPercent.toInt()}%",
                            color = if (isFinished) Color(0xFFE8F5E9) else CbzGoldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Progress Bar if in progress
            if (book.progressPercent > 0f && book.progressPercent < 100f) {
                LinearProgressIndicator(
                    progress = { book.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = CbzGoldPrimary,
                    trackColor = Color(0x22FFFFFF)
                )
            }

            // Title & Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (book.lastOpenedTime > 0L && book.currentPage > 1) {
                        Text(
                            text = "p. ${book.currentPage}/${book.pageCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CbzGoldPrimary,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = if (book.pageCount > 0) "${book.pageCount} pages" else "Comic",
                            style = MaterialTheme.typography.labelSmall,
                            color = CbzTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
