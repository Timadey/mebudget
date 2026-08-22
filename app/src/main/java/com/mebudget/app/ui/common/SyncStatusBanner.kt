package com.mebudget.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mebudget.app.data.sync.SyncState

@Composable
fun SyncStatusBanner(
    syncState: SyncState,
    onRetryClick: () -> Unit = {},
    onPausedClick: () -> Unit = {}
) {
    when (syncState) {
        is SyncState.Idle -> Unit

        is SyncState.Syncing -> {
            SyncBannerCard(container = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYNCING DATA...",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 3.dp)
                }
            }
        }

        is SyncState.Error -> {
            SyncBannerCard(container = MaterialTheme.colorScheme.errorContainer) {
                Column {
                    Text(
                        text = "SYNC FAILED",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = SyncState.friendlyMessage(syncState.message),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    if (syncState.retryable) {
                        TextButton(
                            onClick = onRetryClick,
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("RETRY", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        is SyncState.Pending -> {
            SyncBannerCard(container = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    text = "${syncState.count} CHANGES WAITING TO SYNC",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        is SyncState.Paused -> {
            SyncBannerCard(
                container = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.clickable(onClick = onPausedClick),
                content = {
                    Text(
                        text = "SYNC PAUSED: ${syncState.reason.uppercase()}",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Tap to sign in",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            )
        }
    }
}

@Composable
private fun SyncBannerCard(
    container: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}
