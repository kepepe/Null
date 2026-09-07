package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.GeminiStudyAssistant
import com.example.ui.components.AssistantMessage
import com.example.ui.components.AssistantChatBubble
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBotScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    
    // We keep state locally in the screen for simplicity, or it could be hoisted.
    val messages = remember { mutableStateListOf<AssistantMessage>() }
    
    // Initial greeting if empty
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                AssistantMessage(
                    text = "Привет! Я твой ИИ-тьютор. По какому предмету или теме тебе нужна помощь?",
                    isUser = false
                )
            )
        }
    }

    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        val uri = selectedImageUri
        if (text.isBlank() && uri == null) return

        inputText = ""
        selectedImageUri = null
        
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        messages.add(AssistantMessage(text = text, isUser = true, timestamp = timeStr))
        
        isThinking = true
        
        // Scroll to bottom
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
        
        coroutineScope.launch {
            var bitmap: Bitmap? = null
            if (uri != null) {
                bitmap = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    } else {
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val history = messages.dropLast(1).joinToString("\n") { 
                "${if(it.isUser) "Студент" else "Тьютор"}: ${it.text}" 
            }

            val result = GeminiStudyAssistant.chatWithBot(
                chatHistory = history,
                newQuestion = text,
                bitmap = bitmap
            )

            isThinking = false
            result.onSuccess { response ->
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                messages.add(AssistantMessage(text = response, isUser = false, timestamp = time))
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }.onFailure {
                Toast.makeText(context, "Ошибка ответа ИИ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(BentoSurface)) {
        // Header
        Surface(
            color = BentoSurface,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ИИ-Тьютор",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = BentoPrimary
                    )
                )
                Text(
                    text = "Объяснит тему, решит задачу, подскажет по расписанию",
                    style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                )
            }
        }

        // Chat list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                AssistantChatBubble(msg)
            }
            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                            color = BentoSurfaceVariant,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = "Печатает...",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(color = BentoOnSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }

        // Selected image preview
        if (selectedImageUri != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BentoSurfaceVariant)
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Прикрепленное фото",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить фото",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Input row
        Surface(
            color = BentoSurface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Прикрепить фото",
                        tint = BentoPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Напиши сообщение...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BentoPrimary,
                        unfocusedBorderColor = BentoBorderLight,
                        focusedContainerColor = BentoSurfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = BentoSurfaceVariant.copy(alpha = 0.3f)
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = { sendMessage() },
                    modifier = Modifier.size(48.dp),
                    enabled = !isThinking && (inputText.isNotBlank() || selectedImageUri != null),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = BentoPrimary,
                        contentColor = BentoOnPrimary
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Отправить", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
