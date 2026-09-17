package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.PageFit
import com.example.ui.theme.CbzGoldPrimary
import com.example.ui.viewmodel.ReaderViewModel

@Composable
fun CbzPageItem(
    bookId: Long,
    pageIndex: Int,
    viewModel: ReaderViewModel,
    pageFit: PageFit,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(bookId, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(bookId, pageIndex) { mutableStateOf(true) }

    LaunchedEffect(bookId, pageIndex) {
        isLoading = true
        bitmap = viewModel.getPageBitmap(bookId, pageIndex)
        isLoading = false
    }

    val contentScale = when (pageFit) {
        PageFit.FIT_WIDTH -> ContentScale.FillWidth
        PageFit.FIT_HEIGHT -> ContentScale.FillHeight
        PageFit.AUTO -> ContentScale.Fit
        PageFit.ORIGINAL -> ContentScale.None
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = bitmap, label = "page_crossfade") { currentBitmap ->
            if (currentBitmap != null && !currentBitmap.isRecycled) {
                val ratio = currentBitmap.width.toFloat() / currentBitmap.height.toFloat().coerceAtLeast(1f)
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                )
            } else {
                // Placeholder while loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .background(Color(0xFF14151B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = CbzGoldPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Text(
                            text = "Page ${pageIndex + 1} unavailable",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
