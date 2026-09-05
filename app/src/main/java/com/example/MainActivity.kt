package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.ScheduleRepository
import com.example.domain.ObserveCurrentClassUseCase
import com.example.model.ClassSlot
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.BentoBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ScheduleRepository(database.classDao(), applicationContext)
        val observeUseCase = ObserveCurrentClassUseCase(repository)

        val viewModelFactory = MainViewModel.Factory(repository, observeUseCase)

        setContent {
            val viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = viewModelFactory
            )

            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = userProfile.themeMode) {
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val currentStatus by viewModel.currentClassStatus.collectAsStateWithLifecycle()
                val allClasses by viewModel.allClasses.collectAsStateWithLifecycle()
                val friends by viewModel.friends.collectAsStateWithLifecycle()
                val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                val selectedChatChannel by viewModel.selectedChatChannel.collectAsStateWithLifecycle()
                val viewingFriendSchedule by viewModel.viewingFriendSchedule.collectAsStateWithLifecycle()
                val selectedDay by viewModel.selectedTimetableDay.collectAsStateWithLifecycle()
                val selectedParity by viewModel.selectedParityFilter.collectAsStateWithLifecycle()
                val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                var showAddEditDialog by remember { mutableStateOf(false) }
                var editingSlot by remember { mutableStateOf<ClassSlot?>(null) }
                var showAddFriendDialog by remember { mutableStateOf(false) }
                var showRegisterDialog by remember { mutableStateOf(false) }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearToast()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BentoBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        BentoNavigationBar(
                            selectedIndex = currentTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            0 -> DashboardScreen(
                                currentStatus = currentStatus,
                                friends = friends,
                                userProfile = userProfile,
                                allClassesCount = allClasses.size,
                                onOpenSchedule = { viewModel.selectTab(1) },
                                onOpenFriends = { viewModel.selectTab(2) },
                                onOpenProfile = { viewModel.selectTab(4) },
                                onAddNewClass = {
                                    editingSlot = null
                                    showAddEditDialog = true
                                },
                                onToggleParityMode = {
                                    viewModel.toggleParityMode()
                                }
                            )

                            1 -> TimetableScreen(
                                allClasses = allClasses,
                                selectedDay = selectedDay,
                                selectedParity = selectedParity,
                                onSelectDay = { viewModel.selectTimetableDay(it) },
                                onSelectParity = { viewModel.selectParityFilter(it) },
                                onEditClass = { slot ->
                                    editingSlot = slot
                                    showAddEditDialog = true
                                },
                                onAddNewClass = {
                                    editingSlot = null
                                    showAddEditDialog = true
                                }
                            )

                            2 -> FriendsScreen(
                                friends = friends,
                                userProfile = userProfile,
                                onAddFriendClick = { showAddFriendDialog = true },
                                onRemoveFriend = { viewModel.removeFriend(it) },
                                onOpenRegisterDialog = { showRegisterDialog = true },
                                onAddSuggestedFriend = { viewModel.addFriend(it) },
                                onViewSchedule = { friend ->
                                    viewModel.openFriendSchedule(friend)
                                },
                                onOpenChat = { friendId ->
                                    viewModel.openFriendChat(friendId)
                                }
                            )

                            3 -> ChatScreen(
                                currentChannelId = selectedChatChannel,
                                chatMessages = chatMessages,
                                friends = friends,
                                userProfile = userProfile,
                                onSelectChannel = { viewModel.selectChatChannel(it) },
                                onSendMessage = { channelId, text ->
                                    viewModel.sendMessage(channelId, text)
                                }
                            )

                            4 -> ProfileScreen(
                                userProfile = userProfile,
                                totalClasses = allClasses.size,
                                onOpenRegisterDialog = { showRegisterDialog = true },
                                onUpdateAvatar = { uri ->
                                    viewModel.updateUserAvatar(uri)
                                },
                                onSelectParityMode = { mode ->
                                    viewModel.setWeekParityMode(mode)
                                },
                                onSelectThemeMode = { mode ->
                                    viewModel.setThemeMode(mode)
                                },
                                onToggleNotifications = { enabled ->
                                    viewModel.setNotificationsEnabled(enabled)
                                },
                                onTestNotification = {
                                    viewModel.testNotification(applicationContext)
                                },
                                onLoadDemoSchedule = { viewModel.loadDemoSchedule() },
                                onClearSchedule = { viewModel.clearSchedule() },
                                onClearChat = { viewModel.clearChat() }
                            )
                        }
                    }

                    if (showAddEditDialog) {
                        AddEditClassDialog(
                            initialSlot = editingSlot,
                            defaultDay = selectedDay,
                            onDismiss = {
                                showAddEditDialog = false
                                editingSlot = null
                            },
                            onSave = { slot ->
                                viewModel.saveClass(slot)
                                showAddEditDialog = false
                                editingSlot = null
                            },
                            onDelete = { id ->
                                viewModel.deleteClass(id)
                                showAddEditDialog = false
                                editingSlot = null
                            }
                        )
                    }

                    if (showAddFriendDialog) {
                        AddFriendDialog(
                            onDismiss = { showAddFriendDialog = false },
                            onAddFriend = { query ->
                                viewModel.addFriend(query)
                            }
                        )
                    }

                    if (showRegisterDialog) {
                        RegisterProfileDialog(
                            initialProfile = userProfile,
                            onDismiss = { showRegisterDialog = false },
                            onRegister = { name, handle, univ, avatarUri ->
                                viewModel.registerUser(name, handle, univ, avatarUri)
                            }
                        )
                    }

                    viewingFriendSchedule?.let { friend ->
                        FriendScheduleDialog(
                            friend = friend,
                            onDismiss = { viewModel.openFriendSchedule(null) },
                            onOpenChat = {
                                viewModel.openFriendChat(friend.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Привет, $name!", modifier = modifier)
}
