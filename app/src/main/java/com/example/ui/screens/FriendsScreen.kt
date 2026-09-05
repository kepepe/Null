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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FriendUser
import com.example.model.UserProfile
import com.example.ui.theme.*

@Composable
fun FriendsScreen(
    friends: List<FriendUser>,
    userProfile: UserProfile,
    onAddFriendClick: () -> Unit,
    onRemoveFriend: (String) -> Unit,
    onOpenRegisterDialog: () -> Unit,
    onAddSuggestedFriend: (String) -> Unit,
    onViewSchedule: (FriendUser) -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredFriends = remember(friends, searchQuery) {
        if (searchQuery.isBlank()) friends
        else friends.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.handle.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BentoBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFriendClick,
                containerColor = BentoPrimary,
                contentColor = BentoOnPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .testTag("fab_add_friend")
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Добавить друга")
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

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Друзья",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )
                    Text(
                        text = "Отслеживание пар, расписания и аудиторий",
                        style = MaterialTheme.typography.bodySmall.copy(color = BentoOnSurfaceVariant)
                    )
                }

                Button(
                    onClick = onAddFriendClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BentoPrimaryContainer,
                        contentColor = BentoOnPrimaryContainer
                    ),
                    modifier = Modifier.testTag("add_friend_header_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Найти", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Profile Mini Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BentoPrimaryContainer),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BentoBorderContainer),
                    width = 1.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (userProfile.isRegistered) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BentoPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (userProfile.avatarUri != null) {
                                    AsyncImage(
                                        model = userProfile.avatarUri,
                                        contentDescription = "Аватар",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = userProfile.initials,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoOnPrimaryContainer
                                    )
                                )
                                Text(
                                    text = "${userProfile.handle} • ${userProfile.university}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Заполните профиль",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimary
                                )
                            )
                            Text(
                                text = "Укажите имя и никнейм (@тег), чтобы друзья могли находить вас",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = BentoOnPrimaryContainer,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Button(
                            onClick = onOpenRegisterDialog,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                        ) {
                            Text("Создать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar by Tag or Name
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по @тегу или имени...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_friends_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = BentoSurface,
                    focusedContainerColor = BentoSurface,
                    unfocusedBorderColor = BentoBorderLight,
                    focusedBorderColor = BentoPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Friends List or Clean Empty State
            if (filteredFriends.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BentoSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BentoBorderLight),
                        width = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Список друзей пуст",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoOnSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Найдите одногруппников по их университетскому @тегу, чтобы просматривать их расписание и аудитории.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoOnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Быстрый поиск по @тегу:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = { onAddSuggestedFriend("@alex_sm") },
                                label = { Text("@alex_sm") }
                            )
                            SuggestionChip(
                                onClick = { onAddSuggestedFriend("@maria_n") },
                                label = { Text("@maria_n") }
                            )
                            SuggestionChip(
                                onClick = { onAddSuggestedFriend("@daniil_k") },
                                label = { Text("@daniil_k") }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(filteredFriends, key = { it.id }) { friend ->
                        RussianFriendPresenceCard(
                            friend = friend,
                            onRemove = { onRemoveFriend(friend.id) },
                            onViewSchedule = { onViewSchedule(friend) },
                            onOpenChat = { onOpenChat(friend.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RussianFriendPresenceCard(
    friend: FriendUser,
    onRemove: () -> Unit,
    onViewSchedule: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("friend_card_${friend.id}"),
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                runCatching { Color(android.graphics.Color.parseColor(friend.avatarBgColorHex)) }
                                    .getOrDefault(BentoPrimary)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (friend.avatarUri != null) {
                            AsyncImage(
                                model = friend.avatarUri,
                                contentDescription = friend.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = friend.avatarInitials,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = friend.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BentoOnSurface
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = friend.handle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = BentoPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Live status pill
                    if (friend.isAttendingClass) {
                        Surface(
                            shape = CircleShape,
                            color = BentoGreenContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(BentoSuccessGreen)
                                )
                                Text(
                                    text = "НА ПАРЕ",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BentoSuccessGreen,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = BentoSurfaceVariant
                        ) {
                            Text(
                                text = "СВОБОДЕН",
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = BentoOnSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Удалить друга",
                            tint = BentoOnSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Real-time Class Details
            if (friend.isAttendingClass && friend.currentClass != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BentoBorderLight)
                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(
                        text = friend.currentClass,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoOnPrimaryContainer
                        ),
                        maxLines = 1
                    )

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
                                modifier = Modifier.size(16.dp),
                                tint = BentoPrimary
                            )
                            Text(
                                text = friend.currentRoom ?: "В корпусе",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BentoPrimary
                                )
                            )
                        }

                        if (friend.classEndTime != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = BentoOnSurfaceVariant
                                )
                                Text(
                                    text = "до ${friend.classEndTime}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = BentoOnSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: View Schedule & Chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewSchedule,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BentoPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Расписание",
                        maxLines = 1,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onOpenChat,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Написать",
                        maxLines = 1,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
