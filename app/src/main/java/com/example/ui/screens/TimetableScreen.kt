package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.BellsScheduleBottomSheet
import com.example.ui.components.ScanScheduleDialog
import com.example.ui.components.ScheduleWindowItemCard
import com.example.ui.components.ShareScheduleDialog
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private sealed interface TimetableDisplayItem {
    data class ClassItem(val slot: ClassSlot) : TimetableDisplayItem
    data class WindowItem(val window: ScheduleWindow) : TimetableDisplayItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    allClasses: List<ClassSlot>,
    selectedDay: DayOfWeek,
    selectedParity: WeekParity,
    bellSchedule: List<BellSlot> = standardBellSchedule,
    attendanceMap: Map<String, AttendanceStatus> = emptyMap(),
    onSelectDay: (DayOfWeek) -> Unit,
    onSelectParity: (WeekParity) -> Unit,
    onEditClass: (ClassSlot) -> Unit,
    onAddNewClass: () -> Unit,
    onSetAttendance: (String, LocalDate, AttendanceStatus) -> Unit = { _, _, _ -> },
    onIncrementSkip: (String) -> Unit = {},
    onDecrementSkip: (String) -> Unit = {},
    onImportClasses: (List<ClassSlot>) -> Unit = {},
    onOpenEditBells: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val days = listOf(
        DayOfWeek.MONDAY to "Пн",
        DayOfWeek.TUESDAY to "Вт",
        DayOfWeek.WEDNESDAY to "Ср",
        DayOfWeek.THURSDAY to "Чт",
        DayOfWeek.FRIDAY to "Пт",
        DayOfWeek.SATURDAY to "Сб"
    )

    val russianLocale = Locale("ru", "RU")
    var showBellsSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showScanScheduleDialog by remember { mutableStateOf(false) }

    val dayFilteredClasses = remember(allClasses, selectedDay, selectedParity) {
        allClasses
            .filter { it.dayOfWeek == selectedDay }
            .filter { selectedParity == WeekParity.ALL || it.weekParity == WeekParity.ALL || it.weekParity == selectedParity }
            .sortedBy { it.startTime }
    }

    // Build items with windows between classes
    val timetableItems = remember(dayFilteredClasses) {
        val items = mutableListOf<TimetableDisplayItem>()
        for (i in dayFilteredClasses.indices) {
            val slot = dayFilteredClasses[i]
            items.add(TimetableDisplayItem.ClassItem(slot))
            if (i < dayFilteredClasses.size - 1) {
                val nextSlot = dayFilteredClasses[i + 1]
                val gapMinutes = java.time.Duration.between(slot.endTime, nextSlot.startTime).toMinutes()
                if (gapMinutes >= 15) {
                    items.add(
                        TimetableDisplayItem.WindowItem(
                            ScheduleWindow(
                                previousSlot = slot,
                                nextSlot = nextSlot,
                                startTime = slot.endTime,
                                endTime = nextSlot.startTime,
                                durationMinutes = gapMinutes
                            )
                        )
                    )
                }
            }
        }
        items
    }

    // Determine representative date for the selected day of this week
    val today = remember { LocalDate.now() }
    val selectedDayDate = remember(selectedDay) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusDays((selectedDay.value - 1).toLong())
    }

    // Attendance & Skip stats for the current day
    val totalMarked = attendanceMap.values.count { it != AttendanceStatus.NOT_MARKED }
    val attendedCount = attendanceMap.values.count { it == AttendanceStatus.ATTENDED }
    val attendanceRate = if (totalMarked > 0) (attendedCount * 100) / totalMarked else 100

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BentoBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewClass,
                containerColor = BentoPrimary,
                contentColor = BentoOnPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("fab_add_class")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить пару"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Timetable Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Расписание",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = BentoPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Пары, звонки, окна и счётчик пропусков",
                        style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // AI Photo/Text Schedule Scanner Trigger
                    IconButton(
                        onClick = { showScanScheduleDialog = true },
                        modifier = Modifier.testTag("btn_ai_scan_schedule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Распознать расписание по фото (ИИ)",
                            tint = BentoPrimary
                        )
                    }

                    // Bells Schedule Modal Trigger
                    IconButton(
                        onClick = { showBellsSheet = true },
                        modifier = Modifier.testTag("btn_bell_schedule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Сетка звонков",
                            tint = BentoPrimary
                        )
                    }

                    // Share schedule (QR and link) trigger
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("btn_share_schedule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Поделиться расписанием (QR / Ссылка)",
                            tint = BentoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overall Attendance & Skips Indicator Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BentoSurface,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                    width = 1.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BentoGreenContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BentoSuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Посещаемость семестра: $attendanceRate%",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoOnSurface
                                )
                            )
                            Text(
                                text = "Отмечено: $attendedCount из $totalMarked пар",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = BentoOnSurfaceVariant
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoPrimaryContainer
                    ) {
                        Text(
                            text = "${dayFilteredClasses.size} пар",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Parity Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekParity.values().forEach { parity ->
                    val isSelected = selectedParity == parity
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectParity(parity) },
                        label = {
                            Text(
                                text = parity.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BentoPrimary,
                            selectedLabelColor = BentoOnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days of Week Strip (Пн - Сб)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { (day, label) ->
                    val isSelected = selectedDay == day
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) BentoPrimary else BentoSurface,
                        border = if (!isSelected) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                            width = 1.dp
                        ) else null,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clickable { onSelectDay(day) }
                            .testTag("day_chip_${day.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BentoOnPrimary else BentoOnSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Class and Window List
            if (dayFilteredClasses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎉 Выходной день!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoOnSurfaceVariant
                            )
                        )
                        Text(
                            text = "В этот день занятий не запланировано",
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onAddNewClass,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Добавить пару")
                            }

                            Button(
                                onClick = { showScanScheduleDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Скан по фото")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(timetableItems, key = {
                        when (it) {
                            is TimetableDisplayItem.ClassItem -> "class_${it.slot.id}"
                            is TimetableDisplayItem.WindowItem -> "window_${it.window.startTime}_${it.window.endTime}"
                        }
                    }) { item ->
                        when (item) {
                            is TimetableDisplayItem.ClassItem -> {
                                val slot = item.slot
                                val attendanceKey = "${slot.id}_$selectedDayDate"
                                val currentAttendance = attendanceMap[attendanceKey] ?: AttendanceStatus.NOT_MARKED

                                TimetableClassCard(
                                    slot = slot,
                                    attendanceStatus = currentAttendance,
                                    onEdit = { onEditClass(slot) },
                                    onStatusChange = { newStatus ->
                                        onSetAttendance(slot.id, selectedDayDate, newStatus)
                                    },
                                    onIncrementSkip = { onIncrementSkip(slot.id) },
                                    onDecrementSkip = { onDecrementSkip(slot.id) }
                                )
                            }
                            is TimetableDisplayItem.WindowItem -> {
                                ScheduleWindowItemCard(window = item.window)
                            }
                        }
                    }
                }
            }
        }
    }

    // Bells Schedule Modal Sheet
    if (showBellsSheet) {
        BellsScheduleBottomSheet(
            onDismiss = { showBellsSheet = false },
            bellSchedule = bellSchedule,
            onConfigureBells = onOpenEditBells
        )
    }

    // QR & Link Share Dialog
    if (showShareDialog) {
        ShareScheduleDialog(
            allClasses = allClasses,
            onDismiss = { showShareDialog = false },
            onImportClasses = { imported ->
                onImportClasses(imported)
            }
        )
    }

    // AI Schedule Scanner Dialog (Photo OCR & Notes)
    if (showScanScheduleDialog) {
        ScanScheduleDialog(
            onDismiss = { showScanScheduleDialog = false },
            onImportSuccess = { imported ->
                onImportClasses(imported)
            }
        )
    }
}

@Composable
fun TimetableClassCard(
    slot: ClassSlot,
    attendanceStatus: AttendanceStatus = AttendanceStatus.NOT_MARKED,
    onEdit: () -> Unit,
    onStatusChange: (AttendanceStatus) -> Unit = {},
    onIncrementSkip: () -> Unit = {},
    onDecrementSkip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (typeBg, typeFg) = when (slot.classType) {
        ClassType.LECTURE -> BentoPrimaryContainer to BentoOnPrimaryContainer
        ClassType.SEMINAR -> BentoPurpleContainer to BentoOnPurpleContainer
        ClassType.LAB -> BentoCoralContainer to BentoOnCoralContainer
        ClassType.PRACTICUM -> BentoGreenContainer to BentoSuccessGreen
    }

    val defaultTagColor = BentoPrimary
    val tagColor = remember(slot.colorHex, defaultTagColor) {
        slot.colorHex?.let {
            runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
        } ?: defaultTagColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("class_card_${slot.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
            width = 1.dp
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Color Tag accent line on the left
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(170.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(tagColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Span Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BentoSurfaceVariant
                    ) {
                        Text(
                            text = slot.formattedTimeSpan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        )
                    }

                    // Class Type Chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeBg
                    ) {
                        Text(
                            text = slot.classType.displayName,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = typeFg
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Color Tag Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )

                    Text(
                        text = slot.subjectTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoOnSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = BentoPrimary
                        )
                        Text(
                            text = slot.classroom,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = BentoOnSurfaceVariant
                        )
                        Text(
                            text = slot.professor,
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Лимит допустимых пропусков за семестр
                val isExceeded = slot.skippedCount >= slot.allowedSkips && slot.allowedSkips > 0

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isExceeded -> Color(0xFFFFEBEE)
                        else -> BentoSurfaceVariant
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Лимит допустимых пропусков за семестр:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = BentoOnSurfaceVariant
                                    )
                                )
                                Text(
                                    text = "${slot.allowedSkips}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoPrimary
                                    )
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Пропусков:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoOnSurfaceVariant
                                    )
                                )
                                Text(
                                    text = "${slot.skippedCount} из ${slot.allowedSkips}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = if (isExceeded) Color(0xFFC62828) else BentoPrimary
                                    )
                                )
                                if (isExceeded) {
                                    Text(
                                        text = "• ⚠️ Лимит исчерпан!",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Attendance Tracking Bar (Индикатор посещаемости)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Посещение сегодня:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BentoOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // "Был" chip
                        val isAttended = attendanceStatus == AttendanceStatus.ATTENDED
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAttended) BentoSuccessGreen else BentoSurfaceVariant,
                            modifier = Modifier.clickable {
                                onStatusChange(if (isAttended) AttendanceStatus.NOT_MARKED else AttendanceStatus.ATTENDED)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (isAttended) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = "Был",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAttended) Color.White else BentoOnSurfaceVariant
                                )
                            }
                        }

                        // "Пропуск" chip
                        val isMissed = attendanceStatus == AttendanceStatus.MISSED
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isMissed) BentoCoral else BentoSurfaceVariant,
                            modifier = Modifier.clickable {
                                onStatusChange(if (isMissed) AttendanceStatus.NOT_MARKED else AttendanceStatus.MISSED)
                            }
                        ) {
                            Text(
                                text = "Пропуск",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMissed) Color.White else BentoOnSurfaceVariant
                            )
                        }

                        // "Уваж." chip
                        val isExcused = attendanceStatus == AttendanceStatus.EXCUSED
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isExcused) BentoOnPurpleContainer else BentoSurfaceVariant,
                            modifier = Modifier.clickable {
                                onStatusChange(if (isExcused) AttendanceStatus.NOT_MARKED else AttendanceStatus.EXCUSED)
                            }
                        ) {
                            Text(
                                text = "Уваж.",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExcused) Color.White else BentoOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
