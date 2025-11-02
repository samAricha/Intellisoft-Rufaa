package com.teka.rufaa.utils.offline_sync_utils

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teka.rufaa.ui.theme.quicksand


/**
 * Banner that shows sync status at the top of screens
 */
@Composable
fun SyncStatusBanner(
    syncStatus: SyncStatus,
    onManualSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !syncStatus.isConnected || syncStatus.unsyncedCount > 0,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = if (syncStatus.isConnected) {
                Color(0xFFFBBF24).copy(alpha = 0.2f) // Yellow for unsynced
            } else {
                Color(0xFFEF4444).copy(alpha = 0.2f) // Red for offline
            },
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            !syncStatus.isConnected -> Icons.Default.CloudOff
                            syncStatus.unsyncedCount > 0 -> Icons.Default.CloudSync
                            else -> Icons.Default.CloudDone
                        },
                        contentDescription = null,
                        tint = if (syncStatus.isConnected) Color(0xFFFBBF24) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Column {
                        Text(
                            text = when {
                                !syncStatus.isConnected -> "No Internet Connection"
                                syncStatus.unsyncedCount > 0 -> "${syncStatus.unsyncedCount} items waiting to sync"
                                else -> "All data synced"
                            },
                            fontFamily = quicksand,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF1A1A1A)
                        )
                        
                        if (!syncStatus.isConnected) {
                            Text(
                                text = "Data saved locally. Will sync when online.",
                                fontFamily = quicksand,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        } else if (syncStatus.unsyncedCount > 0) {
                            Text(
                                text = getNetworkTypeText(syncStatus.networkType),
                                fontFamily = quicksand,
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
                
                // Manual sync button when online and has unsynced data
                if (syncStatus.isConnected && syncStatus.unsyncedCount > 0) {
                    IconButton(
                        onClick = onManualSyncClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync now",
                            tint = Color(0xFF006A72),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun getNetworkTypeText(networkType: NetworkType): String {
    return when (networkType) {
        NetworkType.WIFI -> "Connected via WiFi"
        NetworkType.CELLULAR -> "Connected via Mobile Data"
        NetworkType.ETHERNET -> "Connected via Ethernet"
        NetworkType.OTHER -> "Connected"
        NetworkType.NONE -> "No connection"
    }
}

/**
 * Compact sync indicator for use in AppBar or bottom of screen
 */
@Composable
fun CompactSyncIndicator(
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    if (syncStatus.unsyncedCount > 0 || !syncStatus.isConnected) {
        Row(
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (syncStatus.isConnected) Icons.Default.CloudSync else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (syncStatus.isConnected) Color(0xFFFBBF24) else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
            
            if (syncStatus.unsyncedCount > 0) {
                Text(
                    text = syncStatus.unsyncedCount.toString(),
                    fontFamily = quicksand,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (syncStatus.isConnected) Color(0xFFFBBF24) else Color(0xFFEF4444)
                )
            }
        }
    }
}