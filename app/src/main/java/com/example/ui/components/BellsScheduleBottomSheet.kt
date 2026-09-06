package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BellSlot
import com.example.model.standardBellSchedule
import com.example.ui.theme.*
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BellsScheduleBottomSheet(
    onDismiss: () -> Unit,
    bellSchedule: List<BellSlot> = standardBellSchedule,
    onConfigureBells: (() -> Unit)? = null
) {
    val currentTime = LocalTime.now()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BentoSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
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
                            .clip(CircleShape)
                            .background(BentoPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Сетка звонков",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        )
                        Text(
                            text = "Стандартное расписание пар и перемен",
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_bells")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bellSchedule) { slot ->
                    val isCurrent = !currentTime.isBefore(slot.startTime) && currentTime.isBefore(slot.endTime)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCurrent) BentoPrimaryContainer.copy(alpha = 0.6f) else BentoSurfaceVariant.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isCurrent) BentoPrimary else BentoBorderLight
                            ),
                            width = if (isCurrent) 1.5.dp else 1.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) BentoPrimary else BentoPrimaryContainer
                                    ) {
                                        Text(
                                            text = "${slot.pairNumber} пара",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) BentoOnPrimary else BentoPrimary
                                            )
                                        )
                                    }

                                    if (isCurrent) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BentoGreenContainer
                                        ) {
                                            Text(
                                                text = "ИДЁТ СЕЙЧАС",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BentoSuccessGreen
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = BentoPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = slot.formattedTimeSpan,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BentoOnSurface
                                        )
                                    )
                                }
                            }

                            if (slot.breakAfterMinutes > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "→ ${slot.breakDescription}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = BentoOnSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }

                if (onConfigureBells != null) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onConfigureBells()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Настроить время звонков вручную")
                        }
                    }
                }
            }
        }
    }
}
