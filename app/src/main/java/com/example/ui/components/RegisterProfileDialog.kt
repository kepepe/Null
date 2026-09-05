package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.UserProfile
import com.example.ui.theme.BentoOnPrimary
import com.example.ui.theme.BentoOnPrimaryContainer
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryContainer

@Composable
fun RegisterProfileDialog(
    initialProfile: UserProfile? = null,
    onDismiss: () -> Unit,
    onRegister: (name: String, handle: String, university: String, avatarUri: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var handle by remember { mutableStateOf(initialProfile?.handle ?: "") }
    var university by remember { mutableStateOf(initialProfile?.university ?: "") }
    var avatarUri by remember { mutableStateOf(initialProfile?.avatarUri) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("register_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar with Photo Picker
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BentoPrimaryContainer)
                        .border(2.dp, BentoPrimary, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "Аватар",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Добавить фото",
                                tint = BentoPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Фото",
                                fontSize = 10.sp,
                                color = BentoPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = if (initialProfile?.isRegistered == true) "Редактировать профиль" else "Создание профиля",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimary
                    )
                )

                Text(
                    text = "Укажите имя, никнейм (@тег) и фото, чтобы друзья легко находили вас в расписании.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("Имя и Фамилия") },
                    placeholder = { Text("напр. Кирилл Васильев") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = handle,
                    onValueChange = {
                        handle = it
                        errorText = null
                    },
                    label = { Text("Никнейм (@тег)") },
                    placeholder = { Text("напр. @kirill_v") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_handle_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = university,
                    onValueChange = { university = it },
                    label = { Text("ВУЗ и группа") },
                    placeholder = { Text("напр. ИТМО, гр. К3220") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.School, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_univ_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
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
                            if (name.isBlank()) {
                                errorText = "Пожалуйста, введите ваше имя"
                                return@Button
                            }
                            if (handle.isBlank()) {
                                errorText = "Пожалуйста, укажите никнейм"
                                return@Button
                            }
                            onRegister(name.trim(), handle.trim(), university.trim(), avatarUri)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text(if (initialProfile?.isRegistered == true) "Сохранить" else "Зарегистрироваться")
                    }
                }
            }
        }
    }
}
