package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiApiClient
import com.example.ai.GeminiStudyAssistant
import com.example.model.SrsTask
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesToSrsDialog(
    knownSubjects: List<String>,
    onDismiss: () -> Unit,
    onImportSuccess: (List<SrsTask>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var extractedTasks by remember { mutableStateOf<List<SrsTask>?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            selectedBitmap = GeminiApiClient.loadBitmapFromUri(context, uri)
            errorMessage = null
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
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
                .testTag("notes_to_srs_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Title Bar
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
                                .background(BentoPurpleContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = BentoOnPurpleContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Парсинг заметок в задания",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                            )
                            Text(
                                text = "Превращение текста и фото в дедлайны СРС",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoOnSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (!isLoading) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = BentoOnSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Review Extracted Tasks
                if (extractedTasks != null) {
                    val tasks = extractedTasks!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BentoGreenContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BentoSuccessGreen
                                )
                                Text(
                                    text = "Распознано заданий: ${tasks.size}. Подтвердите добавление:",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = BentoSuccessGreen
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tasks) { task ->
                                ExtractedTaskItem(task = task)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { extractedTasks = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Назад")
                            }

                            Button(
                                onClick = {
                                    onImportSuccess(tasks)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Добавить все")
                            }
                        }
                    }
                } else {
                    // Input Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            label = { Text("Текст заметки или сообщение из чата") },
                            placeholder = {
                                Text("Например:\nК следующей среде сделать лабу 3 по матану (номера 12, 15, 18). По физике выучить теорию к коллоквиуму до пятницы.")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 130.dp, max = 180.dp),
                            shape = RoundedCornerShape(16.dp)
                        )

                        // Attach Photo of Note / Whiteboard
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BentoSurfaceVariant.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (selectedBitmap != null) BentoPrimary else BentoBorderLight
                                ),
                                width = 1.dp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { photoPickerLauncher.launch("image/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedBitmap != null) BentoPrimary else BentoPrimaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedBitmap != null) Icons.Default.Check else Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = if (selectedBitmap != null) BentoOnPrimary else BentoPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (selectedBitmap != null) "Фото прикреплено" else "Прикрепить фото доски / конспекта",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = BentoOnSurface
                                        )
                                    )
                                    Text(
                                        text = if (selectedBitmap != null) "Нажмите, чтобы изменить" else "Необязательно. Можно сочетать с текстом",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BentoOnSurfaceVariant,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }

                                if (selectedBitmap != null) {
                                    IconButton(
                                        onClick = {
                                            selectedBitmap = null
                                            selectedImageUri = null
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Убрать фото",
                                            tint = BentoOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Error Container
                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    )
                                }
                            }
                        }

                        // Progress Indicator
                        if (isLoading) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = BentoPrimaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = BentoPrimary
                                    )
                                    Text(
                                        text = "Gemini извлекает задания и дедлайны...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = BentoPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Parse Action Button
                        Button(
                            onClick = {
                                if (textInput.isBlank() && selectedBitmap == null) {
                                    errorMessage = "Введите текст заметки или прикрепите фото с заданиями"
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null

                                coroutineScope.launch {
                                    val result = GeminiStudyAssistant.parseNotesToTasks(
                                        textInput = textInput,
                                        bitmap = selectedBitmap,
                                        knownSubjects = knownSubjects
                                    )

                                    isLoading = false
                                    result.onSuccess { tasks ->
                                        if (tasks.isEmpty()) {
                                            errorMessage = "Заданий в тексте не найдено. Укажите конкретные требования или дедлайн."
                                        } else {
                                            extractedTasks = tasks
                                        }
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Не удалось обработать запрос через Gemini AI."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_parse_notes"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Извлечь задания через ИИ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractedTaskItem(task: SrsTask) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BentoSurfaceVariant.copy(alpha = 0.6f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
            width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BentoPrimaryContainer
                ) {
                    Text(
                        text = task.subjectTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (task.deadlineNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BentoCoralContainer
                    ) {
                        Text(
                            text = task.deadlineNote,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoOnCoralContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = BentoOnSurface
                )
            )

            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BentoOnSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )
            }
        }
    }
}
