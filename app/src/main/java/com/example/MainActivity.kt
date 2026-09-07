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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.ScheduleRepository
import com.example.domain.ObserveCurrentClassUseCase
import com.example.model.ClassSlot
import com.example.model.SrsTask
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
        val repository = ScheduleRepository(database.classDao(), database.srsTaskDao(), applicationContext)
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
                val todayWindows by viewModel.todayWindows.collectAsStateWithLifecycle()
                val srsTasks by viewModel.srsTasks.collectAsStateWithLifecycle()
                val attendanceMap by viewModel.attendanceMap.collectAsStateWithLifecycle()
                val selectedDay by viewModel.selectedTimetableDay.collectAsStateWithLifecycle()
                val selectedParity by viewModel.selectedParityFilter.collectAsStateWithLifecycle()
                val subjectPresets by viewModel.subjectPresets.collectAsStateWithLifecycle()
                val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }

                var showAddEditDialog by remember { mutableStateOf(false) }
                var editingSlot by remember { mutableStateOf<ClassSlot?>(null) }
                var showAddEditSrsDialog by remember { mutableStateOf(false) }
                var editingSrsTask by remember { mutableStateOf<SrsTask?>(null) }
                var showRegisterDialog by remember { mutableStateOf(false) }
                var showEditBellsDialog by remember { mutableStateOf(false) }

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
                                windows = todayWindows,
                                allClasses = allClasses,
                                userProfile = userProfile,
                                onOpenSchedule = { viewModel.selectTab(1) },
                                onOpenProfile = { viewModel.selectTab(3) },
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
                                bellSchedule = userProfile.bellSlots,
                                attendanceMap = attendanceMap,
                                onSelectDay = { viewModel.selectTimetableDay(it) },
                                onSelectParity = { viewModel.selectParityFilter(it) },
                                onEditClass = { slot ->
                                    editingSlot = slot
                                    showAddEditDialog = true
                                },
                                onAddNewClass = {
                                    editingSlot = null
                                    showAddEditDialog = true
                                },
                                onSetAttendance = { classId, date, status ->
                                    viewModel.setAttendance(classId, date, status)
                                },
                                onIncrementSkip = { classId ->
                                    viewModel.incrementSkip(classId)
                                },
                                onDecrementSkip = { classId ->
                                    viewModel.decrementSkip(classId)
                                },
                                onImportClasses = { imported ->
                                    viewModel.importClasses(imported)
                                }
                            )

                            2 -> SrsScreen(
                                tasks = srsTasks,
                                allClasses = allClasses,
                                onToggleTask = { id, isCompleted ->
                                    viewModel.toggleSrsTask(id, isCompleted)
                                },
                                onEditTask = { task ->
                                    editingSrsTask = task
                                    showAddEditSrsDialog = true
                                },
                                onDeleteTask = { id ->
                                    viewModel.deleteSrsTask(id)
                                },
                                onAddNewTask = {
                                    editingSrsTask = null
                                    showAddEditSrsDialog = true
                                },
                                onImportTasks = { importedTasks ->
                                    viewModel.importSrsTasks(importedTasks)
                                },
                                onNavigateToSchedule = {
                                    viewModel.selectTab(1)
                                }
                            )

                            3 -> ProfileScreen(
                                userProfile = userProfile,
                                totalClasses = allClasses.size,
                                currentStatus = currentStatus,
                                onOpenRegisterDialog = { showRegisterDialog = true },
                                onUpdateAvatar = { uri ->
                                    viewModel.updateUserAvatar(uri)
                                },
                                onSelectBellPreset = { preset ->
                                    viewModel.setBellPreset(preset)
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
                                onToggleAutoSilentMode = { enabled ->
                                    viewModel.setAutoSilentMode(enabled)
                                },
                                onOpenEditBellsDialog = { showEditBellsDialog = true },
                                onLoadDemoSchedule = { viewModel.loadDemoSchedule() },
                                onClearSchedule = { viewModel.clearSchedule() }
                            )
                        }
                    }

                    if (showAddEditDialog) {
                        AddEditClassDialog(
                            initialSlot = editingSlot,
                            defaultDay = selectedDay,
                            bellSlots = userProfile.bellSlots,
                            subjectPresets = subjectPresets,
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

                    if (showAddEditSrsDialog) {
                        AddEditSrsTaskDialog(
                            allClasses = allClasses,
                            initialTask = editingSrsTask,
                            onDismiss = {
                                showAddEditSrsDialog = false
                                editingSrsTask = null
                            },
                            onSave = { task ->
                                viewModel.addOrUpdateSrsTask(task)
                                showAddEditSrsDialog = false
                                editingSrsTask = null
                            },
                            onDelete = { id ->
                                viewModel.deleteSrsTask(id)
                                showAddEditSrsDialog = false
                                editingSrsTask = null
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

                    if (showEditBellsDialog) {
                        EditBellScheduleDialog(
                            initialSlots = userProfile.bellSlots,
                            onDismiss = { showEditBellsDialog = false },
                            onSave = { updatedSlots ->
                                viewModel.updateBellSlots(updatedSlots)
                            }
                        )
                    }
                }
            }
        }
    }
}
