package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassSlot
import com.example.model.SrsTask
import com.example.ui.theme.*

private enum class SrsFilter(val displayName: String) {
    ALL("Все"),
    ACTIVE("В процессе"),
    COMPLETED("Выполнено")
}

@Composable
fun SrsScreen(
    tasks: List<SrsTask>,
    allClasses: List<ClassSlot>,
    onToggleTask: (String, Boolean) -> Unit,
    onEditTask: (SrsTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddNewTask: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(SrsFilter.ALL) }
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }

    val userSubjects = remember(allClasses) {
        allClasses.map { it.subjectTitle.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filteredTasks = remember(tasks, selectedFilter, selectedSubjectFilter) {
        tasks.filter { task ->
            val matchesStatus = when (selectedFilter) {
                SrsFilter.ALL -> true
                SrsFilter.ACTIVE -> !task.isCompleted
                SrsFilter.COMPLETED -> task.isCompleted
            }
            val matchesSubject = selectedSubjectFilter == null || task.subjectTitle.equals(selectedSubjectFilter, ignoreCase = true)
            matchesStatus && matchesSubject
        }
    }

    val activeCount = remember(tasks) { tasks.count { !it.isCompleted } }
    val completedCount = remember(tasks) { tasks.count { it.isCompleted } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BentoBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNewTask,
                containerColor = BentoPrimary,
                contentColor = BentoOnPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("add_srs_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Задание СРС",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))

                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Самостоятельная работа",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = BentoPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Домашние задания, лабораторные и дедлайны",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoOnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = BentoPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "В ПРОЦЕССЕ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoOnPrimaryContainer.copy(alpha = 0.8f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$activeCount",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = BentoPrimary
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = BentoGreenContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ВЫПОЛНЕНО",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoSuccessGreen.copy(alpha = 0.8f)
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedCount",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = BentoSuccessGreen
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SrsFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = when (filter) {
                                        SrsFilter.ALL -> "Все (${tasks.size})"
                                        SrsFilter.ACTIVE -> "Активные ($activeCount)"
                                        SrsFilter.COMPLETED -> "Сдано ($completedCount)"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Subject Filter Chips (if any user subjects)
                if (userSubjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedSubjectFilter == null,
                            onClick = { selectedSubjectFilter = null },
                            label = { Text("Все предметы", fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                        userSubjects.forEach { subj ->
                            val isChosen = selectedSubjectFilter.equals(subj, ignoreCase = true)
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    selectedSubjectFilter = if (isChosen) null else subj
                                },
                                label = { Text(subj, fontSize = 11.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Task List or Empty State
            if (filteredTasks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = BentoSurface),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                            width = 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(BentoPrimaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentLate,
                                    contentDescription = null,
                                    tint = BentoPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = if (tasks.isEmpty()) "Нет заданий СРС" else "Ничего не найдено",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoOnSurface
                                )
                            )

                            Text(
                                text = if (userSubjects.isEmpty()) {
                                    "В вашем расписании пока нет предметов. Добавьте пары во вкладке «Пары», чтобы привязать к ним домашние задания."
                                } else if (tasks.isEmpty()) {
                                    "Здесь будут отображаться ваши домашние задания, лабораторные работы и темы для подготовки."
                                } else {
                                    "По выбранному фильтру заданий не найдено."
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = BentoOnSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )

                            if (userSubjects.isEmpty()) {
                                Button(
                                    onClick = onNavigateToSchedule,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Перейти к расписанию")
                                }
                            } else if (tasks.isEmpty()) {
                                Button(
                                    onClick = onAddNewTask,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Добавить задание")
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    SrsTaskCard(
                        task = task,
                        onToggle = { isCompleted -> onToggleTask(task.id, isCompleted) },
                        onClick = { onEditTask(task) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun SrsTaskCard(
    task: SrsTask,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg by animateColorAsState(
        targetValue = if (task.isCompleted) BentoSurface.copy(alpha = 0.6f) else BentoSurface,
        label = "taskBg"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (task.isCompleted) BentoBorderLight.copy(alpha = 0.4f) else BentoBorderLight
            ),
            width = 1.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("srs_task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = BentoSuccessGreen,
                    uncheckedColor = BentoOnSurfaceVariant
                ),
                modifier = Modifier.size(24.dp)
            )

            // Details
            Column(modifier = Modifier.weight(1f)) {
                // Subject badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BentoPrimaryContainer.copy(alpha = if (task.isCompleted) 0.4f else 1f)
                ) {
                    Text(
                        text = task.subjectTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = BentoOnPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) BentoOnSurfaceVariant else BentoOnSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Deadline
                if (task.deadlineNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = if (task.isCompleted) BentoOnSurfaceVariant else BentoCoral,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = task.deadlineNote,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (task.isCompleted) BentoOnSurfaceVariant else BentoCoral,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Notes
                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoOnSurfaceVariant.copy(alpha = 0.8f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Delete
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    tint = BentoOnSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
