package com.mastercompanion.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mastercompanion.data.update.UpdateStatus

@Composable
fun UpdateDialog(
    status: UpdateStatus,
    onDownloadAndInstall: (String) -> Unit,
    onDismiss: () -> Unit,
    onCheckAgain: () -> Unit = {},
    whiteTheme: Boolean = false
) {
    val containerBg = if (whiteTheme) Color.White else Color(0xFF0F1117)
    val cardBg = if (whiteTheme) Color(0xFFF1F5F9) else Color(0xFF161922)
    val cardBorder = if (whiteTheme) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)
    val textPrimary = if (whiteTheme) Color(0xFF0F172A) else Color.White
    val textSecondary = if (whiteTheme) Color(0xFF64748B) else Color.White.copy(alpha = 0.65f)
    val accentColor = Color(0xFF10B981)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (status) {
                                is UpdateStatus.UpdateAvailable -> Icons.Filled.RocketLaunch
                                is UpdateStatus.UpToDate -> Icons.Filled.CheckCircle
                                is UpdateStatus.Error -> Icons.Filled.ErrorOutline
                                else -> Icons.Filled.CloudDownload
                            },
                            contentDescription = null,
                            tint = when (status) {
                                is UpdateStatus.Error -> Color(0xFFEF4444)
                                else -> accentColor
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = when (status) {
                                is UpdateStatus.Checking -> "Checking for Updates..."
                                is UpdateStatus.UpdateAvailable -> "New Update Available!"
                                is UpdateStatus.Downloading -> "Downloading Update..."
                                is UpdateStatus.VerifyingSecurity -> "Verifying Update..."
                                is UpdateStatus.Installing -> "Installing Update..."
                                is UpdateStatus.ReadyToInstall -> "Installing Update..."
                                is UpdateStatus.UpToDate -> "You're Up to Date"
                                is UpdateStatus.Error -> "Update Check Failed"
                                else -> "Software Updates"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "GitHub Over-the-Air Delivery",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
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
            when (status) {
                is UpdateStatus.Checking -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(36.dp))
                        Text(
                            text = "Querying GitHub repository releases...",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                }

                is UpdateStatus.UpdateAvailable -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Version banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = status.releaseName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Tag: ${status.newVersion}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = accentColor
                                )
                            }

                            if (status.apkSize > 0) {
                                val sizeMb = status.apkSize / (1024f * 1024f)
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f MB", sizeMb),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                        }

                        // Changelog notes box
                        if (status.changelog.isNotBlank()) {
                            Text(
                                text = "CHANGELOG & RELEASE NOTES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = textSecondary
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = status.changelog,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = textPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                is UpdateStatus.Downloading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { status.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = cardBg
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Downloading app-release.apk...",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                            Text(
                                text = "${status.progressPercent}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                is UpdateStatus.VerifyingSecurity -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Verifying package identity & security signatures...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                    }
                }

                is UpdateStatus.Installing -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = accentColor,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = status.message,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                    }
                }

                is UpdateStatus.ReadyToInstall -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Download complete. Launching installer...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                    }
                }

                is UpdateStatus.UpToDate -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Your Master Companion installation is up to date with the latest GitHub release.",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                }

                is UpdateStatus.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = status.message,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                else -> {}
            }
        },
        confirmButton = {
            when (status) {
                is UpdateStatus.UpdateAvailable -> {
                    Button(
                        onClick = { onDownloadAndInstall(status.downloadUrl) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Update Now", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateStatus.Error -> {
                    Button(
                        onClick = onCheckAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateStatus.UpToDate -> {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Done", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (status is UpdateStatus.UpdateAvailable) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Later", color = textSecondary)
                }
            }
        }
    )
}
