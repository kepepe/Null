package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BentoPrimary
import kotlinx.coroutines.launch

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAddFriend: suspend (String) -> Boolean
) {
    var inputQuery by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_friend_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = BentoPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Добавить друга",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimary
                        )
                    )
                }

                Text(
                    text = "Введите @тег друга. Приложение найдёт его профиль и расписание в реальном времени через Google Firebase или каталог вуза.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = {
                        inputQuery = it
                        errorMessage = null
                    },
                    label = { Text("Никнейм (@тег) друга") },
                    placeholder = { Text("напр. @maria_n или @alex_sm") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("friend_query_input"),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputQuery.isBlank()) {
                                errorMessage = "Пожалуйста, введите никнейм друга"
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                val added = onAddFriend(inputQuery.trim())
                                isLoading = false
                                if (added) {
                                    onDismiss()
                                } else {
                                    errorMessage = "Не удалось найти или уже в списке"
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_add_friend_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Поиск...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Найти")
                        }
                    }
                }
            }
        }
    }
}
