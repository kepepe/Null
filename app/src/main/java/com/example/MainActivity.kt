package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ScheduleRepository
import com.example.domain.ObserveCurrentClassUseCase
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(applicationContext)
        val repository = ScheduleRepository(db.classDao(), db.srsTaskDao(), applicationContext)
        val observeUseCase = ObserveCurrentClassUseCase(repository)

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(repository, observeUseCase)
            )
            val userProfile by viewModel.userProfile.collectAsState()
            
            MyApplicationTheme(
                themeMode = userProfile.themeMode
            ) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val allClasses by viewModel.allClasses.collectAsState()
    val todayWindows by viewModel.todayWindows.collectAsState()
    val currentStatus by viewModel.currentClassStatus.collectAsState()
    val selectedDay by viewModel.selectedTimetableDay.collectAsState()
    val selectedParity by viewModel.selectedParityFilter.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val attendanceMap by viewModel.attendanceMap.collectAsState()
    val srsTasks by viewModel.srsTasks.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingSlot by remember { mutableStateOf<com.example.model.ClassSlot?>(null) }

    var showAddEditSrsDialog by remember { mutableStateOf(false) }
    var editingSrsTask by remember { mutableStateOf<com.example.model.SrsTask?>(null) }

    var showRegisterDialog by remember { mutableStateOf(false) }
    var showEditBellsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
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
                onOpenProfile = { viewModel.selectTab(4) },
                onAddNewClass = {
                    editingSlot = null
                    showAddEditDialog = true
                },
                onToggleParityMode = { viewModel.toggleParityMode() }
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
                onIncrementSkip = { classId -> viewModel.incrementSkip(classId) },
                onDecrementSkip = { classId -> viewModel.decrementSkip(classId) },
                onImportClasses = { imported -> viewModel.importClasses(imported) }
            )
            2 -> SrsScreen(
                tasks = srsTasks,
                allClasses = allClasses,
                onToggleTask = { id, isCompleted -> viewModel.toggleSrsTask(id, isCompleted) },
                onEditTask = { task ->
                    editingSrsTask = task
                    showAddEditSrsDialog = true
                },
                onDeleteTask = { id -> viewModel.deleteSrsTask(id) },
                onAddNewTask = {
                    editingSrsTask = null
                    showAddEditSrsDialog = true
                },
                onImportTasks = { importedTasks -> viewModel.importSrsTasks(importedTasks) },
                onNavigateToSchedule = { viewModel.selectTab(1) }
            )
            4 -> ProfileScreen(
                userProfile = userProfile,
                totalClasses = allClasses.size,
                currentStatus = currentStatus,
                onOpenRegisterDialog = { showRegisterDialog = true },
                onUpdateAvatar = { uri -> viewModel.updateUserAvatar(uri) },
                onSelectBellPreset = { preset -> viewModel.setBellPreset(preset) },
                onSelectParityMode = { mode -> viewModel.setWeekParityMode(mode) },
                onSelectThemeMode = { mode -> viewModel.setThemeMode(mode) },
                onToggleNotifications = { enabled -> viewModel.setNotificationsEnabled(enabled) },
                onTestNotification = { viewModel.testNotification(context) },
                onOpenEditBellsDialog = { showEditBellsDialog = true },
                onLoadDemoSchedule = { viewModel.loadDemoSchedule() },
                onClearSchedule = { viewModel.clearSchedule() }
            )
            3 -> ChatBotScreen(
                modifier = Modifier.fillMaxSize()
            )
        }

        }

        if (showAddEditDialog) {
            AddEditClassDialog(
                initialSlot = editingSlot,
                defaultDay = selectedDay,
                bellSlots = userProfile.bellSlots,
                onDismiss = {
                    showAddEditDialog = false
                    editingSlot = null
                },
                onSave = { slots ->
                    slots.forEach { viewModel.saveClass(it) }
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
