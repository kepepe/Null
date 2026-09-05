package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ClassSlot
import com.example.model.ClassType
import com.example.model.WeekParity
import com.example.ui.theme.BentoPrimary
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClassDialog(
    initialSlot: ClassSlot? = null,
    defaultDay: DayOfWeek = DayOfWeek.MONDAY,
    onDismiss: () -> Unit,
    onSave: (ClassSlot) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var subjectTitle by remember { mutableStateOf(initialSlot?.subjectTitle ?: "") }
    var classType by remember { mutableStateOf(initialSlot?.classType ?: ClassType.LECTURE) }
    var professor by remember { mutableStateOf(initialSlot?.professor ?: "") }
    var classroom by remember { mutableStateOf(initialSlot?.classroom ?: "") }
    var dayOfWeek by remember { mutableStateOf(initialSlot?.dayOfWeek ?: defaultDay) }
    var startTimeText by remember {
        mutableStateOf(initialSlot?.startTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "10:00")
    }
    var endTimeText by remember {
        mutableStateOf(initialSlot?.endTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "11:30")
    }
    var weekParity by remember { mutableStateOf(initialSlot?.weekParity ?: WeekParity.ALL) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_edit_class_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialSlot == null) "Добавить пару" else "Редактировать пару",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )

                    if (initialSlot != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(initialSlot.id)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("delete_class_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить пару",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Subject Title
                OutlinedTextField(
                    value = subjectTitle,
                    onValueChange = {
                        subjectTitle = it
                        errorMessage = null
                    },
                    label = { Text("Название предмета") },
                    placeholder = { Text("напр. Базы данных") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Professor
                OutlinedTextField(
                    value = professor,
                    onValueChange = { professor = it },
                    label = { Text("Преподаватель") },
                    placeholder = { Text("напр. проф. Соколов А.В.") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("professor_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Classroom
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { Text("Аудитория / Корпус") },
                    placeholder = { Text("напр. Ауд. 402, Корпус 2") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("classroom_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Class Type Chips
                Text(
                    text = "Тип занятия",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClassType.values().forEach { type ->
                        FilterChip(
                            selected = classType == type,
                            onClick = { classType = type },
                            label = { Text(type.displayName, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Day of Week
                Text(
                    text = "День недели",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        DayOfWeek.MONDAY to "Пн",
                        DayOfWeek.TUESDAY to "Вт",
                        DayOfWeek.WEDNESDAY to "Ср",
                        DayOfWeek.THURSDAY to "Чт",
                        DayOfWeek.FRIDAY to "Пт",
                        DayOfWeek.SATURDAY to "Сб"
                    ).forEach { (day, label) ->
                        FilterChip(
                            selected = dayOfWeek == day,
                            onClick = { dayOfWeek = day },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Time Pickers (Start & End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTimeText,
                        onValueChange = { startTimeText = it },
                        label = { Text("Начало") },
                        placeholder = { Text("10:00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endTimeText,
                        onValueChange = { endTimeText = it },
                        label = { Text("Окончание") },
                        placeholder = { Text("11:30") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Alternating Week Parity
                Text(
                    text = "Периодичность (Чётная / Нечётная)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WeekParity.values().forEach { parity ->
                        FilterChip(
                            selected = weekParity == parity,
                            onClick = { weekParity = parity },
                            label = { Text(parity.displayName, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subjectTitle.isBlank()) {
                                errorMessage = "Укажите название предмета"
                                return@Button
                            }
                            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                            val parsedStart = runCatching { LocalTime.parse(startTimeText.trim(), formatter) }.getOrNull()
                            val parsedEnd = runCatching { LocalTime.parse(endTimeText.trim(), formatter) }.getOrNull()

                            if (parsedStart == null || parsedEnd == null) {
                                errorMessage = "Время должно быть в формате ЧЧ:ММ (напр. 09:30)"
                                return@Button
                            }
                            if (!parsedEnd.isAfter(parsedStart)) {
                                errorMessage = "Окончание должно быть позже начала"
                                return@Button
                            }

                            val slot = ClassSlot(
                                id = initialSlot?.id ?: UUID.randomUUID().toString(),
                                subjectTitle = subjectTitle.trim(),
                                classType = classType,
                                professor = professor.trim().ifBlank { "Преподаватель не указан" },
                                classroom = classroom.trim().ifBlank { "Аудитория уточняется" },
                                dayOfWeek = dayOfWeek,
                                startTime = parsedStart,
                                endTime = parsedEnd,
                                weekParity = weekParity,
                                colorHex = initialSlot?.colorHex ?: "#0061A4"
                            )
                            onSave(slot)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_class_button")
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
