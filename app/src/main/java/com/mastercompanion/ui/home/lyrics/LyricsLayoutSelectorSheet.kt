package com.mastercompanion.ui.home.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LyricsLayoutSelectorDialog(
    currentLayoutIndex: Int,
    onSelectLayout: (Int) -> Unit,
    onDismiss: () -> Unit,
    whiteTheme: Boolean = false
) {
    val modes = LyricsLayoutMode.values()

    val cardBg = if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF13151D)
    val cardBorder = if (whiteTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.09f)
    val activeBorder = Color(0xFF1DB954)
    val activeBg = if (whiteTheme) Color(0xFFDCFCE7) else Color(0xFF1DB954).copy(alpha = 0.12f)
    val textPrimary = if (whiteTheme) Color(0xFF0F172A) else Color.White
    val textSecondary = if (whiteTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.6f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (whiteTheme) Color.White else Color(0xFF0C0E14),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lyrics Presentation Faces",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Choose your preferred karaoke layout",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = textPrimary
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                itemsIndexed(modes) { index, mode ->
                    val isSelected = index == currentLayoutIndex
                    val icon = getModeIcon(mode)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) activeBg else cardBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) activeBorder else cardBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                onSelectLayout(index)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Pill
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1DB954) else Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.Black else textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Text details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = mode.subtitle,
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }

                        // Selected indicator
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DB954)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun getModeIcon(mode: LyricsLayoutMode): ImageVector {
    return when (mode) {
        LyricsLayoutMode.CLASSIC_SPLIT -> Icons.Filled.ViewColumn
        LyricsLayoutMode.SWAPPED_SPLIT -> Icons.Filled.ViewCarousel
        LyricsLayoutMode.TOP_BANNER -> Icons.Filled.ViewAgenda
        LyricsLayoutMode.FULLSCREEN_CENTER -> Icons.Filled.ViewCompact
        LyricsLayoutMode.FULLSCREEN_LEFT -> Icons.Filled.ViewHeadline
    }
}
