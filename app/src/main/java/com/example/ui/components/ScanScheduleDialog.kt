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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiApiClient
import com.example.ai.GeminiScheduleParser
import com.example.model.ClassSlot
import com.example.model.ClassType
import com.example.model.WeekParity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScheduleDialog(
    onDismiss: () -> Unit,
    onImportSuccess: (List<ClassSlot>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var manualTextInput by remember { mutableStateOf("") }
    var isTextMode by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var loadingStatusText by remember { mutableStateOf("Обработка через Gemini AI...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recognizedClasses by remember { mutableStateOf<List<ClassSlot>?>(null) }

    // Photo picker (Zero-permission recommended approach)
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
                .testTag("scan_schedule_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Dialog Title Bar
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
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Распознать расписание",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                            )
                            Text(
                                text = "Gemini 2.5 Flash Vision OCR",
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

                Spacer(modifier = Modifier.height(16.dp))

                // If classes were successfully recognized, show review list
                if (recognizedClasses != null) {
                    val classes = recognizedClasses!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = BentoGreenContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BentoSuccessGreen
                                )
                                Text(
                                    text = "Найдено пар: ${classes.size}. Проверьте список перед сохранением:",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = BentoSuccessGreen
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(classes) { slot ->
                                RecognizedClassItem(slot = slot)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    recognizedClasses = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Назад")
                            }

                            Button(
                                onClick = {
                                    onImportSuccess(classes)
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
                                Text("Импортировать")
                            }
                        }
                    }
                } else {
                    // Selection & Input View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Switch between Photo and Text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isTextMode,
                                onClick = { isTextMode = false },
                                label = { Text("Фото / Скриншот") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isTextMode,
                                onClick = { isTextMode = true },
                                label = { Text("Текст / Заметка") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!isTextMode) {
                            // Photo Picker Area
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = BentoSurfaceVariant.copy(alpha = 0.5f),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (selectedBitmap != null) BentoPrimary else BentoBorderLight
                                    ),
                                    width = if (selectedBitmap != null) 2.dp else 1.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        photoPickerLauncher.launch("image/*")
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedBitmap != null) BentoPrimary else BentoPrimaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (selectedBitmap != null) Icons.Default.Check else Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = if (selectedBitmap != null) BentoOnPrimary else BentoPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Text(
                                        text = if (selectedBitmap != null) "Изображение выбрано!" else "Нажмите, чтобы загрузить фото расписания",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedBitmap != null) BentoPrimary else BentoOnSurface
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = "Подойдёт скриншот из вузовского портала, фото стенда расписания или листа из блокнота",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = BentoOnSurfaceVariant,
                                            fontSize = 11.5.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    if (selectedBitmap != null) {
                                        Button(
                                            onClick = { photoPickerLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BentoSurfaceVariant,
                                                contentColor = BentoPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Выбрать другое фото", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Text Input Area
                            OutlinedTextField(
                                value = manualTextInput,
                                onValueChange = { manualTextInput = it },
                                label = { Text("Вставьте текст расписания") },
                                placeholder = {
                                    Text("Например:\nПн: 9:00 Высшая математика ауд. 302 лекция\nВт: 10:45 Физика ауд. 104 лаб (четная)")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp, max = 220.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        // Error message
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
                                        text = loadingStatusText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = BentoPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Action Button
                        Button(
                            onClick = {
                                if (isTextMode && manualTextInput.isBlank()) {
                                    errorMessage = "Введите или вставьте текст расписания"
                                    return@Button
                                }
                                if (!isTextMode && selectedBitmap == null) {
                                    errorMessage = "Сначала выберите изображение из галереи"
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                loadingStatusText = if (isTextMode) "Анализ текста расписания..." else "Сканирование и распознавание пар по фото..."

                                coroutineScope.launch {
                                    val result = if (isTextMode) {
                                        GeminiScheduleParser.parseScheduleFromText(manualTextInput)
                                    } else {
                                        GeminiScheduleParser.parseScheduleFromPhoto(selectedBitmap!!)
                                    }

                                    isLoading = false
                                    result.onSuccess { parsed ->
                                        if (parsed.isEmpty()) {
                                            errorMessage = "Не удалось обнаружить пары на изображении. Попробуйте более чёткое фото или вставьте текст."
                                        } else {
                                            recognizedClasses = parsed
                                        }
                                    }.onFailure { err ->
                                        errorMessage = err.message ?: "Произошла ошибка при обращении к Gemini AI."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_schedule_scan"),
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
                                text = if (isTextMode) "Распознать из текста" else "Распознать по фото",
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
private fun RecognizedClassItem(slot: ClassSlot) {
    val dayName = when (slot.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "Пн"
        java.time.DayOfWeek.TUESDAY -> "Вт"
        java.time.DayOfWeek.WEDNESDAY -> "Ср"
        java.time.DayOfWeek.THURSDAY -> "Чт"
        java.time.DayOfWeek.FRIDAY -> "Пт"
        java.time.DayOfWeek.SATURDAY -> "Сб"
        java.time.DayOfWeek.SUNDAY -> "Вс"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BentoSurfaceVariant.copy(alpha = 0.6f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
            width = 1.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BentoPrimaryContainer
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BentoSurface
                    ) {
                        Text(
                            text = slot.classType.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BentoOnSurfaceVariant,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (slot.weekParity != WeekParity.ALL) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BentoPurpleContainer
                        ) {
                            Text(
                                text = slot.weekParity.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = BentoOnPurpleContainer,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = slot.subjectTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoOnSurface
                    )
                )

                if (slot.professor.isNotBlank() || slot.classroom.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (slot.classroom.isNotBlank()) append("Ауд. ${slot.classroom}")
                            if (slot.professor.isNotBlank()) {
                                if (isNotEmpty()) append(" • ")
                                append(slot.professor)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoOnSurfaceVariant,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BentoPrimary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = slot.formattedTimeSpan,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
