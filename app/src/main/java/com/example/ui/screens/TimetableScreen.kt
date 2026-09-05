package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassSlot
import com.example.model.ClassType
import com.example.model.WeekParity
import com.example.ui.theme.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TimetableScreen(
    allClasses: List<ClassSlot>,
    selectedDay: DayOfWeek,
    selectedParity: WeekParity,
    onSelectDay: (DayOfWeek) -> Unit,
    onSelectParity: (WeekParity) -> Unit,
    onEditClass: (ClassSlot) -> Unit,
    onAddNewClass: () -> Unit,
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

    val dayFilteredClasses = remember(allClasses, selectedDay, selectedParity) {
        allClasses
            .filter { it.dayOfWeek == selectedDay }
            .filter { selectedParity == WeekParity.ALL || it.weekParity == WeekParity.ALL || it.weekParity == selectedParity }
            .sortedBy { it.startTime }
    }

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

            // Title
            Text(
                text = "Расписание пар",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoPrimary
                )
            )
            Text(
                text = "Управляйте учебными парами по дням и чётности недель",
                style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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
                        TimetableClassCard(
                            slot = slot,
                            onEdit = { onEditClass(slot) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableClassCard(
    slot: ClassSlot,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (typeBg, typeFg) = when (slot.classType) {
        ClassType.LECTURE -> BentoPrimaryContainer to BentoOnPrimaryContainer
        ClassType.SEMINAR -> BentoPurpleContainer to BentoOnPurpleContainer
        ClassType.LAB -> BentoCoralContainer to BentoOnCoralContainer
        ClassType.PRACTICUM -> BentoGreenContainer to BentoSuccessGreen
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = slot.subjectTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurface
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

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
                        modifier = Modifier.size(16.dp),
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
                        modifier = Modifier.size(16.dp),
                        tint = BentoOnSurfaceVariant
                    )
                    Text(
                        text = slot.professor,
                        style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                    )
                }
            }

            if (slot.weekParity != WeekParity.ALL) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Неделя: ${slot.weekParity.displayName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BentoPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
