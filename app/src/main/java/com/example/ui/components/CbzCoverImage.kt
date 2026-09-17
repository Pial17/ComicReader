package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CbzDarkSurfaceHighlight
import com.example.ui.theme.CbzDarkSurfaceVariant
import com.example.ui.theme.CbzGoldPrimary
import java.io.File

@Composable
fun CbzCoverImage(
    coverPath: String?,
    title: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp)
) {
    val file = coverPath?.let { File(it) }
    val isValidImage = file != null && file.exists() && file.length() > 0

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = CbzDarkSurfaceVariant
    ) {
        if (isValidImage) {
            AsyncImage(
                model = file,
                contentDescription = "Cover for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant styled CBZ placeholder card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CbzDarkSurfaceHighlight,
                                CbzDarkSurfaceVariant,
                                Color(0xFF14161C)
                            )
                        )
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = CbzGoldPrimary.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )

                Text(
                    text = "CBZ",
                    color = CbzGoldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(Color(0x66000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Text(
                    text = title.take(20),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}
