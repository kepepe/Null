package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ClassSlot
import com.example.model.SrsTask
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSrsTaskDialog(
    allClasses: List<ClassSlot>,
    initialTask: SrsTask? = null,
    onDismiss: () -> Unit,
    onSave: (SrsTask) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    // ONLY user-added subjects from their schedule!
    val availableSubjects = remember(allClasses) {
        allClasses.map { it.subjectTitle.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    var selectedSubject by remember {
        mutableStateOf(
            initialTask?.subjectTitle?.ifBlank { availableSubjects.firstOrNull() ?: "" }
                ?: (availableSubjects.firstOrNull() ?: "")
        )
    }
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var deadlineNote by remember { mutableStateOf(initialTask?.deadlineNote ?: "К следующей паре") }
    var selectedDeadlineDate by remember { mutableStateOf<LocalDate?>(initialTask?.deadlineDate) }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                width = 1.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_edit_srs_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTask == null) "Новое задание СРС" else "Редактировать СРС",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )

                    if (initialTask != null && onDelete != null) {
                        IconButton(
                            onClick = {
                                onDelete(initialTask.id)
                                onDismiss()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить задание",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                if (availableSubjects.isEmpty()) {
                    // Alert that no subjects exist yet
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BentoCoralContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BentoCoral
                            )
                            Text(
                                text = "В расписании пока нет предметов. Сначала добавьте пары во вкладке «Пары», чтобы привязать к ним задания СРС.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoCoral,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Понятно")
                    }
                } else {
                    // Subject Selector (STRICTLY from user's added subjects)
                    Text(
                        text = "Предмет из вашего расписания:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableSubjects.forEach { subject ->
                            val isChosen = subject == selectedSubject
                            val associatedClass = allClasses.firstOrNull { it.subjectTitle.equals(subject, ignoreCase = true) }
                            val chipBg = if (isChosen) BentoPrimaryContainer else BentoSurfaceVariant
                            val chipBorder = if (isChosen) BentoPrimary else BentoBorderLight
                            val chipFg = if (isChosen) BentoOnPrimaryContainer else BentoOnSurfaceVariant

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chipBg,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(chipBorder),
                                    width = if (isChosen) 2.dp else 1.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSubject = subject
                                        errorMessage = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isChosen) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isChosen) BentoPrimary else BentoOnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = subject,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                                color = chipFg
                                            )
                                        )
                                    }

                                    if (associatedClass?.classroom?.isNotBlank() == true) {
                                        Text(
                                            text = associatedClass.classroom,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = BentoOnSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Task Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorMessage = null
                        },
                        label = { Text("Что нужно сделать") },
                        placeholder = { Text("напр. Лабораторная №2, доклад, конспект") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("srs_task_title_input"),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Deadline: Date Picker Only
                    Text(
                        text = "Срок сдачи:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (selectedDeadlineDate != null) MaterialTheme.colorScheme.primary else BentoBorderLight
                            ),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("srs_deadline_date_picker_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = if (selectedDeadlineDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (selectedDeadlineDate != null) {
                                            val russianLocale = java.util.Locale("ru", "RU")
                                            selectedDeadlineDate!!.format(DateTimeFormatter.ofPattern("d MMMM yyyy (EEEE)", russianLocale))
                                        } else {
                                            "Выбрать дату сдачи"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (selectedDeadlineDate != null) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedDeadlineDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    Text(
                                        text = if (selectedDeadlineDate != null) "Нажмите, чтобы изменить дату" else "Укажите дедлайн задания",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            if (selectedDeadlineDate != null) {
                                IconButton(
                                    onClick = {
                                        selectedDeadlineDate = null
                                        deadlineNote = ""
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Очистить дату",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Notes / Links
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Заметки и полезные ссылки (опционально)") },
                        placeholder = { Text("напр. страницы 45-52, ссылка на диск") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .testTag("srs_task_notes_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedSubject.isBlank()) {
                                    errorMessage = "Выберите предмет из списка"
                                    return@Button
                                }
                                if (title.trim().isBlank()) {
                                    errorMessage = "Введите описание задания"
                                    return@Button
                                }
                                val task = SrsTask(
                                    id = initialTask?.id ?: UUID.randomUUID().toString(),
                                    subjectTitle = selectedSubject.trim(),
                                    title = title.trim(),
                                    deadlineDate = selectedDeadlineDate,
                                    deadlineNote = if (selectedDeadlineDate != null) {
                                        "до ${selectedDeadlineDate!!.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("ru")))}"
                                    } else {
                                        deadlineNote
                                    },
                                    isCompleted = initialTask?.isCompleted ?: false,
                                    notes = notes.trim(),
                                    createdAt = initialTask?.createdAt ?: System.currentTimeMillis()
                                )
                                onSave(task)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                            modifier = Modifier.testTag("save_srs_task_button")
                        ) {
                            Text(if (initialTask == null) "Добавить" else "Сохранить")
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            selectedDeadlineDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
