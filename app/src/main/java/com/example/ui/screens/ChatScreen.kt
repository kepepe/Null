package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.FriendUser
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    currentChannelId: String,
    chatMessages: List<ChatMessage>,
    friends: List<FriendUser>,
    userProfile: UserProfile,
    onSelectChannel: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val filteredMessages = remember(chatMessages, currentChannelId) {
        chatMessages.filter { it.channelId == currentChannelId }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
    }

    val activeFriend = friends.firstOrNull { it.id == currentChannelId }
    val activeTitle = if (currentChannelId == "group_chat") {
        "Общий чат группы"
    } else {
        activeFriend?.displayName ?: "Чат с другом"
    }
    val activeSubtitle = if (currentChannelId == "group_chat") {
        "Курс • Поток ПО-21"
    } else {
        activeFriend?.handle ?: "@студент"
    }

    val quickChips = listOf(
        "Где пара?",
        "Идёшь на лекцию?",
        "Скинь конспект 📚",
        "Встретимся в буфете ☕",
        "Какая аудитория?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BentoBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Чаты",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary
                    )
                )
                Text(
                    text = "Обсуждение пар, лекций и домашних заданий",
                    style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Channel Selector (Group Chat + Friend Chats)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = currentChannelId == "group_chat"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) BentoPrimary else BentoSurface,
                    border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectChannel("group_chat") }
                        .testTag("channel_group_chat")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else BentoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Общий чат группы",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BentoOnSurface
                            )
                        )
                    }
                }
            }

            items(friends, key = { it.id }) { friend ->
                val isSelected = currentChannelId == friend.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) BentoPrimary else BentoSurface,
                    border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectChannel(friend.id) }
                        .testTag("channel_${friend.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    runCatching { Color(android.graphics.Color.parseColor(friend.avatarBgColorHex)) }
                                        .getOrDefault(BentoPrimary)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = friend.avatarInitials.take(1),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = friend.displayName.split(" ").firstOrNull() ?: friend.displayName,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BentoOnSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Chat Header Info Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (currentChannelId == "group_chat") BentoPrimaryContainer else BentoSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentChannelId == "group_chat") {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (activeFriend?.avatarUri != null) {
                        AsyncImage(
                            model = activeFriend.avatarUri,
                            contentDescription = activeFriend.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = activeFriend?.avatarInitials ?: "??",
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoOnSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (filteredMessages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BentoPrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = BentoPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "История сообщений чиста",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoOnSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Напишите первое сообщение или выберите быстрый ответ ниже",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoOnSurfaceVariant
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            items(filteredMessages, key = { it.id }) { message ->
                ChatMessageBubble(message = message)
            }
        }

        // Quick Reply Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            items(quickChips) { chip ->
                SuggestionChip(
                    onClick = {
                        onSendMessage(currentChannelId, chip)
                    },
                    label = {
                        Text(
                            text = chip,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = BentoSurface
                    )
                )
            }
        }

        // Message Input Row
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = BentoSurface,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                width = 1.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 80.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Сообщение...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(currentChannelId, inputText.trim())
                            inputText = ""
                        }
                    },
                    modifier = Modifier.testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Отправить",
                        tint = BentoPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isMe = message.isFromMe

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = BentoPrimary
                    )
                )
                Text(
                    text = message.senderHandle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = BentoOnSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) BentoPrimary else BentoSurface,
            border = if (isMe) null else CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                width = 1.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isMe) Color.White else BentoOnSurface,
                        lineHeight = 18.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.timestamp,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else BentoOnSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
