package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryContainer
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditClassDialog(
    initialSlot: ClassSlot? = null,
    defaultDay: DayOfWeek = DayOfWeek.MONDAY,
    subjectPresets: List<SubjectPreset> = emptyList(),
    bellSlots: List<BellSlot> = standardBellSchedule,
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
    var allowedSkips by remember { mutableIntStateOf(initialSlot?.allowedSkips ?: 3) }
    var selectedColorHex by remember {
        mutableStateOf(initialSlot?.colorHex ?: subjectColorPalette.first().first)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var smartInputText by remember { mutableStateOf("") }
    var showSmartInput by remember { mutableStateOf(false) }

    fun parseSmartLine(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        // Extract pair number e.g. "1 пара", "2 пара"
        val pairMatch = Regex("(\\d+)\\s*(?:пара|пары|парой)").find(trimmed)
        if (pairMatch != null) {
            val pNum = pairMatch.groupValues[1].toIntOrNull()
            val bell = bellSlots.find { it.pairNumber == pNum }
            if (bell != null) {
                val fmt = DateTimeFormatter.ofPattern("HH:mm")
                startTimeText = bell.startTime.format(fmt)
                endTimeText = bell.endTime.format(fmt)
            }
        }

        // Extract time range e.g. "10:00-11:30" or "8:30 - 10:00"
        val timeMatch = Regex("(\\d{1,2}:\\d{2})\\s*[-—–]\\s*(\\d{1,2}:\\d{2})").find(trimmed)
        if (timeMatch != null) {
            startTimeText = timeMatch.groupValues[1].padStart(5, '0')
            endTimeText = timeMatch.groupValues[2].padStart(5, '0')
        }

        // Extract ClassType
        val lower = trimmed.lowercase()
        when {
            lower.contains("лекц") || lower.contains("лек") -> classType = ClassType.LECTURE
            lower.contains("лаб") -> classType = ClassType.LAB
            lower.contains("практ") -> classType = ClassType.PRACTICUM
            lower.contains("сем") -> classType = ClassType.SEMINAR
        }

        // Extract day of week
        when {
            lower.contains("пн") || lower.contains("понедельник") -> dayOfWeek = DayOfWeek.MONDAY
            lower.contains("вт") || lower.contains("вторник") -> dayOfWeek = DayOfWeek.TUESDAY
            lower.contains("ср") || lower.contains("среда") -> dayOfWeek = DayOfWeek.WEDNESDAY
            lower.contains("чт") || lower.contains("четверг") -> dayOfWeek = DayOfWeek.THURSDAY
            lower.contains("пт") || lower.contains("пятница") -> dayOfWeek = DayOfWeek.FRIDAY
            lower.contains("сб") || lower.contains("суббота") -> dayOfWeek = DayOfWeek.SATURDAY
        }

        // Extract Classroom (e.g. "ауд. 402", "каб. 312", "ауд 301")
        val roomMatch = Regex("(?:ауд\\.?|каб\\.?|кабинет|аудитория)\\s*([A-Za-zА-Яа-я0-9\\-]+)", RegexOption.IGNORE_CASE).find(trimmed)
        if (roomMatch != null) {
            classroom = "Ауд. " + roomMatch.groupValues[1]
        }

        // Extract Professor (words with initials e.g. "Иванов И.И." or "проф. Смирнов")
        val profMatch = Regex("(?:проф\\.?|доц\\.?|преп\\.?)?\\s*([А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ]\\.[А-ЯЁ]\\.|\\s+[А-ЯЁ][а-яё]+))").find(trimmed)
        if (profMatch != null) {
            professor = profMatch.value.trim()
        }

        // Clean up title by removing recognized patterns
        var titleCandidate = trimmed
            .replace(Regex("(?:ауд\\.?|каб\\.?|кабинет|аудитория)\\s*([A-Za-zА-Яа-я0-9\\-]+)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(\\d{1,2}:\\d{2})\\s*[-—–]\\s*(\\d{1,2}:\\d{2})"), "")
            .replace(Regex("(\\d+)\\s*(?:пара|пары|парой)"), "")
            .replace(Regex("(?:лекция|лек|практика|пр|лабораторная|лаб|семинар)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:понедельник|вторник|среда|четверг|пятница|суббота|пн|вт|ср|чт|пт|сб)", RegexOption.IGNORE_CASE), "")
            .trim()
            .trim(',', '.', '-', '—')

        if (profMatch != null) {
            titleCandidate = titleCandidate.replace(profMatch.value, "").trim()
        }

        if (titleCandidate.isNotBlank()) {
            subjectTitle = titleCandidate.trim().capitalize(java.util.Locale.ROOT)
        }
    }

    val hasEnteredSubjectData = subjectTitle.isNotBlank() && (professor.isNotBlank() || classroom.isNotBlank())
    val activeSuggestions = remember(subjectTitle, professor, classroom, subjectPresets) {
        if (subjectPresets.isEmpty()) {
            emptyList()
        } else if (subjectTitle.isBlank()) {
            subjectPresets.take(5)
        } else {
            val trimmed = subjectTitle.trim()
            subjectPresets.filter {
                it.title.contains(trimmed, ignoreCase = true)
            }.take(5)
        }
    }

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

                // Smart Express Text Bar
                if (initialSlot == null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BentoPrimaryContainer.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoPrimary.copy(alpha = 0.3f)),
                            width = 1.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BentoPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Умный ввод одной строкой",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BentoPrimary
                                        )
                                    )
                                }
                                TextButton(
                                    onClick = { showSmartInput = !showSmartInput },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (showSmartInput) "Скрыть" else "Открыть",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (showSmartInput) {
                                OutlinedTextField(
                                    value = smartInputText,
                                    onValueChange = { smartInputText = it },
                                    placeholder = {
                                        Text(
                                            "напр. Матанализ лекция ауд. 402 Соколов 1 пара",
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (smartInputText.isNotBlank()) {
                                            parseSmartLine(smartInputText)
                                            smartInputText = ""
                                            showSmartInput = false
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Заполнить форму")
                                }
                            }
                        }
                    }
                }

                // Autocomplete Subject Presets / Fast Fill Chips
                if (activeSuggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "⚡ Быстрое заполнение из предметов:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeSuggestions) { preset ->
                                SuggestionChip(
                                    onClick = {
                                        subjectTitle = preset.title
                                        if (preset.professor.isNotBlank()) professor = preset.professor
                                        if (preset.classroom.isNotBlank()) classroom = preset.classroom
                                        classType = preset.classType
                                        selectedColorHex = preset.colorHex
                                        errorMessage = null
                                    },
                                    label = {
                                        Text(
                                            text = preset.title,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    runCatching { Color(android.graphics.Color.parseColor(preset.colorHex)) }
                                                        .getOrDefault(BentoPrimary)
                                                )
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
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

                // Class Type Chips (горизонтальная прокрутка)
                Text(
                    text = "Тип занятия",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ClassType.values()) { type ->
                        FilterChip(
                            selected = classType == type,
                            onClick = { classType = type },
                            label = { Text(type.displayName, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Day of Week
                val dayRussianName = when (dayOfWeek) {
                    DayOfWeek.MONDAY -> "Понедельник"
                    DayOfWeek.TUESDAY -> "Вторник"
                    DayOfWeek.WEDNESDAY -> "Среда"
                    DayOfWeek.THURSDAY -> "Четверг"
                    DayOfWeek.FRIDAY -> "Пятница"
                    DayOfWeek.SATURDAY -> "Суббота"
                    DayOfWeek.SUNDAY -> "Воскресенье"
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "День недели:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = dayRussianName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )
                }
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

                // Bell Schedule Quick-Select (Звонки и перемены)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🔔 Быстрый выбор по сетке звонков",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bellSlots) { bell ->
                            SuggestionChip(
                                onClick = {
                                    val fmt = DateTimeFormatter.ofPattern("HH:mm")
                                    startTimeText = bell.startTime.format(fmt)
                                    endTimeText = bell.endTime.format(fmt)
                                },
                                label = {
                                    Text(
                                        text = "${bell.pairNumber} пара (${bell.formattedTimeSpan})",
                                        fontSize = 11.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Subject Color Tag (Цветовые теги для предметов)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🎨 Цветовой тег предмета",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjectColorPalette) { (hex, title) ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Выбран цвет $title",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
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

                // Allowed Skips Counter (Лимит допустимых пропусков за семестр)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Лимит допустимых пропусков",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (allowedSkips > 1) allowedSkips-- },
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$allowedSkips",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = BentoPrimary
                            ),
                            modifier = Modifier.widthIn(min = 24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        FilledTonalIconButton(
                            onClick = { if (allowedSkips < 40) allowedSkips++ },
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Увеличить", modifier = Modifier.size(16.dp))
                        }
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
                                colorHex = selectedColorHex,
                                allowedSkips = allowedSkips,
                                skippedCount = initialSlot?.skippedCount ?: 0
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
