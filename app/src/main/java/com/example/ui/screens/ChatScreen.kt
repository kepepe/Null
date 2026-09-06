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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.FriendUser
import com.example.model.GroupChat
import com.example.model.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    currentChannelId: String,
    chatMessages: List<ChatMessage>,
    friends: List<FriendUser>,
    groupChats: List<GroupChat>,
    userProfile: UserProfile,
    onSelectChannel: (String) -> Unit,
    onSendMessage: (String, String) -> Unit,
    onCreateGroupChat: (name: String, memberFriendIds: List<String>) -> Unit,
    onDeleteGroupChat: (groupId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Ensure valid channel is selected if possible
    LaunchedEffect(currentChannelId, friends, groupChats) {
        val channelExists = groupChats.any { it.id == currentChannelId } ||
                friends.any { it.id == currentChannelId || it.handle.equals(currentChannelId, ignoreCase = true) }
        if (!channelExists) {
            if (groupChats.isNotEmpty()) {
                onSelectChannel(groupChats.first().id)
            } else if (friends.isNotEmpty()) {
                onSelectChannel(friends.first().id)
            }
        }
    }

    LaunchedEffect(currentChannelId) {
        if (currentChannelId.isNotBlank()) {
            onSelectChannel(currentChannelId)
        }
    }

    val activeGroup = groupChats.firstOrNull { it.id == currentChannelId }
    val activeFriend = friends.firstOrNull {
        it.id == currentChannelId ||
                it.handle.equals(currentChannelId, ignoreCase = true) ||
                it.handle.trim().lowercase().removePrefix("@") == currentChannelId.trim().lowercase().removePrefix("@")
    }

    val activeTitle = when {
        activeGroup != null -> activeGroup.name
        activeFriend != null -> activeFriend.displayName
        else -> "Выберите чат"
    }

    val activeSubtitle = when {
        activeGroup != null -> {
            val memberNames = friends.filter { activeGroup.memberFriendIds.contains(it.id) }
                .map { it.displayName.split(" ").first() }
            if (memberNames.isEmpty()) "Вы в группе" else "Участники: ${memberNames.joinToString(", ")} + Вы"
        }
        activeFriend != null -> activeFriend.handle
        else -> "Нажмите на друга или создайте группу"
    }

    val filteredMessages = remember(chatMessages, currentChannelId, activeFriend) {
        if (currentChannelId.isBlank()) emptyList()
        else chatMessages.filter {
            it.channelId == currentChannelId ||
                    (activeFriend != null && (it.channelId == activeFriend.id || it.channelId == activeFriend.handle))
        }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            listState.animateScrollToItem(filteredMessages.size - 1)
        }
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Чаты",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary
                    )
                )
                Text(
                    text = "Диалоги с друзьями и группы",
                    style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Button to open Create Group Dialog
            FilledTonalButton(
                onClick = { showCreateGroupDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = BentoPrimaryContainer,
                    contentColor = BentoOnPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_open_create_group")
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+ Группа",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Channel Selector (Custom Group Chats + Friend Chats)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Group chats created with friends
            items(groupChats, key = { it.id }) { group ->
                val isSelected = currentChannelId == group.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) BentoPrimary else BentoSurface,
                    border = if (isSelected) null else CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectChannel(group.id) }
                        .testTag("channel_group_${group.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                            text = group.name,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BentoOnSurface
                            )
                        )
                    }
                }
            }

            // 1-on-1 Chats with Friends
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
                        .testTag("channel_friend_${friend.id}")
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (activeGroup != null) BentoPrimaryContainer else BentoSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeGroup != null) {
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
                                text = activeFriend?.avatarInitials ?: "💬",
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Delete group button if active is custom group
                if (activeGroup != null) {
                    IconButton(
                        onClick = { onDeleteGroupChat(activeGroup.id) },
                        modifier = Modifier.testTag("btn_delete_group_${activeGroup.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить группу",
                            tint = BentoCoral
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List
        if (currentChannelId.isBlank() || (activeGroup == null && activeFriend == null)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = BentoPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Нет выбранного чата",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Создайте групповой чат со своими друзьями с помощью кнопки «+ Группа» или выберите друга сверху",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = BentoOnSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        } else {
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
                                    imageVector = if (activeGroup != null) Icons.Default.Group else Icons.Default.Person,
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

    // Create Group Chat Dialog
    if (showCreateGroupDialog) {
        CreateGroupChatDialog(
            friends = friends,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { groupName, selectedIds ->
                onCreateGroupChat(groupName, selectedIds)
                showCreateGroupDialog = false
            }
        )
    }
}

@Composable
fun CreateGroupChatDialog(
    friends: List<FriendUser>,
    onDismiss: () -> Unit,
    onCreate: (name: String, memberFriendIds: List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedFriends = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Создать группу",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Text(
                    text = "Создавайте групповые чаты со своими друзьями и называйте их как угодно:",
                    style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                )

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Название группы") },
                    placeholder = { Text("например, ПО-21 Семинары") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name")
                )

                Text(
                    text = "Выберите друзей для добавления:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                if (friends.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BentoSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "У вас пока нет друзей. Добавьте друзей во вкладке «Друзья», чтобы создать группу!",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = BentoCoral)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(friends, key = { it.id }) { friend ->
                            val isChecked = selectedFriends.contains(friend.id)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) BentoPrimaryContainer else BentoSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedFriends.remove(friend.id)
                                        } else {
                                            selectedFriends.add(friend.id)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = friend.displayName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            Text(
                                                text = friend.handle,
                                                style = MaterialTheme.typography.labelSmall.copy(color = BentoOnSurfaceVariant)
                                            )
                                        }
                                    }

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedFriends.add(friend.id)
                                            else selectedFriends.remove(friend.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

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
                            if (groupName.isNotBlank() && selectedFriends.isNotEmpty()) {
                                onCreate(groupName.trim(), selectedFriends.toList())
                            }
                        },
                        enabled = groupName.isNotBlank() && selectedFriends.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_confirm_create_group")
                    ) {
                        Text("Создать группу")
                    }
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

