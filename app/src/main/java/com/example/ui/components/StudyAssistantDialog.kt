package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiStudyAssistant
import com.example.model.ClassSlot
import com.example.model.SrsTask
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class AssistantMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: String = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyAssistantDialog(
    tasks: List<SrsTask>,
    allClasses: List<ClassSlot>,
    initialSubject: String? = null,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(initialSubject) }
    var isLoading by remember { mutableStateOf(false) }

    val distinctSubjects = remember(allClasses) {
        allClasses.map { it.subjectTitle.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val activeTasks = remember(tasks, selectedSubject) {
        if (selectedSubject != null) {
            tasks.filter { !it.isCompleted && it.subjectTitle.equals(selectedSubject, ignoreCase = true) }
        } else {
            tasks.filter { !it.isCompleted }
        }
    }

    val messages = remember {
        mutableStateListOf(
            AssistantMessage(
                text = "Привет! Я твой персональный AI-тьютор на базе Gemini 2.5. Чем могу помочь? Объяснить тему, разобрать сложную задачу, написать план подготовки к зачёту или помочь с лабораторной?",
                isUser = false
            )
        )
    }

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
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
                .testTag("study_assistant_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(BentoPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Умный помощник",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                            )
                            Text(
                                text = "Академический тьютор Gemini 2.5",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoOnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = BentoOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subject Context Filter
                if (distinctSubjects.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedSubject == null,
                                onClick = { selectedSubject = null },
                                label = { Text("Все предметы", fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        items(distinctSubjects) { subj ->
                            FilterChip(
                                selected = selectedSubject == subj,
                                onClick = {
                                    selectedSubject = if (selectedSubject == subj) null else subj
                                },
                                label = { Text(subj, fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Quick Prompt Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SuggestionChip(
                            onClick = {
                                val prompt = if (selectedSubject != null) {
                                    "Составь план подготовки к зачёту по предмету $selectedSubject"
                                } else {
                                    "Как спланировать подготовку к сессии без выгорания?"
                                }
                                inputText = prompt
                            },
                            label = { Text("📋 План к зачёту", fontSize = 11.sp) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                val targetTask = activeTasks.firstOrNull()
                                val prompt = if (targetTask != null) {
                                    "Помоги с заданием «${targetTask.title}» (${targetTask.subjectTitle}): с чего начать выполнение?"
                                } else {
                                    "Объясни сложную тему простыми словами"
                                }
                                inputText = prompt
                            },
                            label = { Text("💡 Помощь с заданием", fontSize = 11.sp) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                inputText = "Посоветуй литературу и ресурсы для изучения темы"
                            },
                            label = { Text("📚 Ресурсы и книги", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chat Messages
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        AssistantChatBubble(message = msg)
                    }

                    if (isLoading) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BentoPrimaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BentoPrimary
                                    )
                                    Text(
                                        text = "Тьютор формулирует ответ...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BentoPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Задайте вопрос по учёбе...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        maxLines = 3
                    )

                    IconButton(
                        onClick = {
                            val question = inputText.trim()
                            if (question.isBlank() || isLoading) return@IconButton

                            inputText = ""
                            messages.add(AssistantMessage(text = question, isUser = true))
                            isLoading = true

                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)

                                val taskContext = activeTasks.firstOrNull()?.let {
                                    "Текущее активное задание: «${it.title}» (${it.notes})"
                                }

                                val result = GeminiStudyAssistant.askStudyQuestion(
                                    question = question,
                                    subjectContext = selectedSubject,
                                    taskContext = taskContext
                                )

                                isLoading = false
                                result.onSuccess { reply ->
                                    messages.add(AssistantMessage(text = reply, isUser = false))
                                    listState.animateScrollToItem(messages.size - 1)
                                }.onFailure { err ->
                                    messages.add(
                                        AssistantMessage(
                                            text = "⚠️ Ошибка: ${err.message ?: "Не удалось получить ответ"}",
                                            isUser = false
                                        )
                                    )
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) BentoPrimary else BentoPrimary.copy(alpha = 0.3f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Отправить",
                            tint = BentoOnPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantChatBubble(message: AssistantMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = if (message.isUser) BentoPrimary else BentoSurfaceVariant,
            border = if (!message.isUser) CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                width = 1.dp
            ) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (message.isUser) BentoOnPrimary else BentoOnSurface,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        color = if (message.isUser) BentoOnPrimary.copy(alpha = 0.7f) else BentoOnSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
