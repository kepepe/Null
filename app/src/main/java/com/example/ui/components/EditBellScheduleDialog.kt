package com.example.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BellSlot
import com.example.model.standardBellSchedule
import com.example.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun EditBellScheduleDialog(
    initialSlots: List<BellSlot>,
    onDismiss: () -> Unit,
    onSave: (List<BellSlot>) -> Unit
) {
    val context = LocalContext.current
    var slots by remember {
        mutableStateOf(
            if (initialSlots.isNotEmpty()) initialSlots.toMutableList()
            else standardBellSchedule.toMutableList()
        )
    }

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    fun showPicker(currentTime: LocalTime, onSelected: (LocalTime) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onSelected(LocalTime.of(hour, minute))
            },
            currentTime.hour,
            currentTime.minute,
            true
        ).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = BentoSurface,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BentoPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Сетка звонков",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                            )
                            Text(
                                text = "Настройте время начала и конца каждой пары",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoOnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action to reset to standard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Пары в вашем вузе:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoOnSurface
                        )
                    )
                    TextButton(
                        onClick = {
                            slots = standardBellSchedule.toMutableList()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Сброс к стандарту", fontSize = 11.sp)
                    }
                }

                // Pairs List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(slots) { index, slot ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BentoSurfaceVariant.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                                width = 1.dp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Pair Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = BentoPrimaryContainer
                                ) {
                                    Text(
                                        text = "${slot.pairNumber} пара",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BentoPrimary
                                        )
                                    )
                                }

                                // Times: Start and End
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Start Time Button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(BentoPrimary.copy(alpha = 0.5f)),
                                            width = 1.dp
                                        ),
                                        modifier = Modifier.clickable {
                                            showPicker(slot.startTime) { newStart ->
                                                val list = slots.toMutableList()
                                                val duration = java.time.Duration.between(slot.startTime, slot.endTime).toMinutes().coerceAtLeast(30)
                                                val newEnd = newStart.plusMinutes(duration)
                                                list[index] = slot.copy(startTime = newStart, endTime = newEnd)
                                                slots = list
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = slot.startTime.format(timeFormatter),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BentoPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    Text("—", fontWeight = FontWeight.Bold, color = BentoOnSurfaceVariant)

                                    // End Time Button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(BentoPrimary.copy(alpha = 0.5f)),
                                            width = 1.dp
                                        ),
                                        modifier = Modifier.clickable {
                                            showPicker(slot.endTime) { newEnd ->
                                                val list = slots.toMutableList()
                                                list[index] = slot.copy(endTime = newEnd)
                                                slots = list
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = slot.endTime.format(timeFormatter),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BentoPrimary
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // Delete button if > 1 slot
                                if (slots.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val list = slots.toMutableList()
                                            list.removeAt(index)
                                            // Renumber
                                            slots = list.mapIndexed { idx, s ->
                                                s.copy(pairNumber = idx + 1)
                                            }.toMutableList()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                val lastSlot = slots.lastOrNull()
                                val nextPairNum = (lastSlot?.pairNumber ?: 0) + 1
                                val nextStart = lastSlot?.endTime?.plusMinutes(15) ?: LocalTime.of(8, 0)
                                val nextEnd = nextStart.plusMinutes(90)
                                val newSlot = BellSlot(
                                    pairNumber = nextPairNum,
                                    startTime = nextStart,
                                    endTime = nextEnd,
                                    breakAfterMinutes = 15,
                                    breakDescription = "Перемена 15 мин"
                                )
                                slots = (slots + newSlot).toMutableList()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Добавить ещё пару", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            onSave(slots)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
