package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    allClasses: List<ClassSlot>,
    selectedDay: DayOfWeek,
    selectedParity: WeekParity,
    attendanceMap: Map<String, AttendanceStatus> = emptyMap(),
    friends: List<FriendUser> = emptyList(),
    groupChats: List<GroupChat> = emptyList(),
    onSelectDay: (DayOfWeek) -> Unit,
    onSelectParity: (WeekParity) -> Unit,
    onEditClass: (ClassSlot) -> Unit,
    onAddNewClass: () -> Unit,
    onSetAttendance: (String, LocalDate, AttendanceStatus) -> Unit = { _, _, _ -> },
    onShareSchedule: (channelId: String, classes: List<ClassSlot>, title: String) -> Unit = { _, _, _ -> },
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

    val dayFilteredClasses = remember(allClasses, selectedDay, selectedParity) {
        allClasses
            .filter { it.dayOfWeek == selectedDay }
            .filter { selectedParity == WeekParity.ALL || it.weekParity == WeekParity.ALL || it.weekParity == selectedParity }
            .sortedBy { it.startTime }
    }

    // Determine representative date for the selected day of this week
    val today = remember { LocalDate.now() }
    val selectedDayDate = remember(selectedDay) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusDays((selectedDay.value - 1).toLong())
    }

    // Attendance stats
    val totalMarked = attendanceMap.values.count { it != AttendanceStatus.NOT_MARKED }
    val attendedCount = attendanceMap.values.count { it == AttendanceStatus.ATTENDED }
    val missedCount = attendanceMap.values.count { it == AttendanceStatus.MISSED }
    val attendanceRate = if (totalMarked > 0) (attendedCount * 100) / totalMarked else 100

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BentoBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewClass,
                containerColor = BentoPrimary,
                contentColor = BentoOnPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .testTag("fab_add_class")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить пару")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header row with Title and Quick Actions (Bells & Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Расписание пар",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )
                    Text(
                        text = "Пары, звонки, перемены и посещаемость",
                        style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

                    // Share schedule to chat trigger
                    IconButton(
                        onClick = { showShareDialog = true },
                        modifier = Modifier.testTag("btn_share_schedule")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться в чат",
                            tint = BentoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Overall Attendance Indicator Card
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
                        Text(
                            text = "📊 Посещаемость:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (attendanceRate >= 80) BentoGreenContainer else BentoCoralContainer
                        ) {
                            Text(
                                text = "$attendanceRate%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendanceRate >= 80) BentoSuccessGreen else BentoCoral
                                )
                            )
                        }
                    }

                    Text(
                        text = "Посещено $attendedCount • Пропусков $missedCount",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Week Parity Selector (Все недели, Нечётная, Чётная)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeekParity.values().forEach { parity ->
                    val isSelected = selectedParity == parity
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) BentoPrimary else BentoSurface,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) BentoPrimary else BentoBorderLight),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectParity(parity) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = parity.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) BentoOnPrimary else BentoOnSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Selector Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEach { (day, label) ->
                    val isSelected = selectedDay == day
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) BentoPrimaryContainer else BentoSurface,
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) BentoBorderContainer else BentoBorderLight),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectDay(day) }
                            .testTag("day_tab_${day.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) BentoOnPrimaryContainer else BentoOnSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Class List
            if (dayFilteredClasses.isEmpty()) {
                val fullDayName = selectedDay.getDisplayName(TextStyle.FULL, russianLocale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "На $fullDayName пар нет",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Нажмите «+» внизу, чтобы добавить лекцию, семинар или лабораторную в этот день",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoOnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(dayFilteredClasses, key = { it.id }) { slot ->
                        val attendanceKey = "${slot.id}_$selectedDayDate"
                        val currentAttendance = attendanceMap[attendanceKey] ?: AttendanceStatus.NOT_MARKED

                        TimetableClassCard(
                            slot = slot,
                            attendanceStatus = currentAttendance,
                            onEdit = { onEditClass(slot) },
                            onStatusChange = { newStatus ->
                                onSetAttendance(slot.id, selectedDayDate, newStatus)
                            }
                        )
                    }
                }
            }
        }
    }

    // Bell Schedule Bottom Sheet
    if (showBellsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBellsSheet = false },
            containerColor = BentoSurface,
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
                    Column {
                        Text(
                            text = "🔔 Звонки и перемены",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        )
                        Text(
                            text = "Сетка учебных пар и длительность перемен",
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                        )
                    }
                    IconButton(onClick = { showBellsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                standardBellSchedule.forEach { bell ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BentoSurfaceVariant.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BentoPrimary,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${bell.pairNumber}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BentoOnPrimary
                                            )
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "${bell.pairNumber} пара",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = bell.formattedTimeSpan,
                                        style = MaterialTheme.typography.bodySmall.copy(color = BentoPrimary)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (bell.breakAfterMinutes >= 20) BentoCoralContainer else BentoSurface
                            ) {
                                Text(
                                    text = bell.breakDescription,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (bell.breakAfterMinutes >= 20) BentoCoral else BentoOnSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Share Schedule Dialog
    if (showShareDialog) {
        val fullDayName = selectedDay.getDisplayName(TextStyle.FULL, russianLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }

        Dialog(onDismissRequest = { showShareDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Отправить расписание",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Выберите друга или группу для отправки расписания на $fullDayName (${dayFilteredClasses.size} пар):",
                        style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                    )

                    if (friends.isEmpty() && groupChats.isEmpty()) {
                        Text(
                            text = "У вас пока нет друзей или групп. Добавьте друзей во вкладке «Друзья».",
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoCoral)
                        )
                    } else {
                        // Group Chats
                        if (groupChats.isNotEmpty()) {
                            Text(
                                text = "Групповые чаты:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            groupChats.forEach { grp ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BentoSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onShareSchedule(grp.id, dayFilteredClasses, "Расписание на $fullDayName")
                                            showShareDialog = false
                                        }
                                ) {
                                    Text(
                                        text = "👥 ${grp.name}",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }

                        // Friends
                        if (friends.isNotEmpty()) {
                            Text(
                                text = "Друзья:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            friends.forEach { friend ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BentoSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onShareSchedule(friend.id, dayFilteredClasses, "Расписание на $fullDayName")
                                            showShareDialog = false
                                        }
                                ) {
                                    Text(
                                        text = "💬 ${friend.displayName} (${friend.handle})",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showShareDialog = false }) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableClassCard(
    slot: ClassSlot,
    attendanceStatus: AttendanceStatus = AttendanceStatus.NOT_MARKED,
    onEdit: () -> Unit,
    onStatusChange: (AttendanceStatus) -> Unit = {},
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
                    .height(140.dp)
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

                // Interactive Attendance Tracking Bar (Индикатор посещаемости)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Посещение:",
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

