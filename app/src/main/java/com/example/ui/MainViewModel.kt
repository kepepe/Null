package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ScheduleRepository
import com.example.domain.ObserveCurrentClassUseCase
import com.example.model.*
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields

class MainViewModel(
    private val repository: ScheduleRepository,
    private val observeCurrentClassUseCase: ObserveCurrentClassUseCase
) : ViewModel() {

    val userProfile: StateFlow<UserProfile> = repository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    val currentClassStatus: StateFlow<CurrentClassStatus> = observeCurrentClassUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CurrentClassStatus.NoClassesToday
        )

    val allClasses: StateFlow<List<ClassSlot>> = repository.observeAllClasses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val friends: StateFlow<List<FriendUser>> = repository.friendsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val groupChats: StateFlow<List<GroupChat>> = repository.groupChatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subjectPresets: StateFlow<List<SubjectPreset>> = repository.subjectPresetsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val attendanceMap: StateFlow<Map<String, AttendanceStatus>> = repository.attendanceFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val srsTasks: StateFlow<List<SrsTask>> = repository.observeAllSrsTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayWindows: StateFlow<List<ScheduleWindow>> = observeCurrentClassUseCase.observeTodayWindows()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun computeInitialTimetableDay(): DayOfWeek {
        val today = LocalDate.now().dayOfWeek
        return if (today == DayOfWeek.SUNDAY) DayOfWeek.MONDAY else today
    }

    private fun computeCalendarWeekParity(): WeekParity {
        val weekNumber = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return if (weekNumber % 2 == 0) WeekParity.EVEN else WeekParity.ODD
    }

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedTimetableDay = MutableStateFlow(computeInitialTimetableDay())
    val selectedTimetableDay: StateFlow<DayOfWeek> = _selectedTimetableDay.asStateFlow()

    private val _selectedParityFilter = MutableStateFlow(computeCalendarWeekParity())
    val selectedParityFilter: StateFlow<WeekParity> = _selectedParityFilter.asStateFlow()

    private val _selectedChatChannel = MutableStateFlow("")
    val selectedChatChannel: StateFlow<String> = _selectedChatChannel.asStateFlow()

    private val _viewingFriendSchedule = MutableStateFlow<FriendUser?>(null)
    val viewingFriendSchedule: StateFlow<FriendUser?> = _viewingFriendSchedule.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            currentClassStatus.collect { status ->
            }
        }
    }

    fun registerUser(name: String, handle: String, university: String, avatarUri: String? = null) {
        val updated = repository.saveUserProfile(name, handle, university, avatarUri)
        _toastMessage.value = "Профиль обновлен: ${updated.name} (${updated.handle})"
    }

    fun updateUserAvatar(uri: String?) {
        repository.updateUserAvatar(uri)
        _toastMessage.value = "Фото профиля успешно обновлено"
    }

    fun setWeekParityMode(mode: WeekParityMode) {
        repository.setWeekParityMode(mode)
        _toastMessage.value = "Режим чётности: ${mode.displayName}"
    }

    fun toggleParityMode() {
        val current = userProfile.value.parityMode
        val next = when (current) {
            WeekParityMode.AUTO -> WeekParityMode.ODD
            WeekParityMode.ODD -> WeekParityMode.EVEN
            WeekParityMode.EVEN -> WeekParityMode.AUTO
        }
        setWeekParityMode(next)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.setNotificationsEnabled(enabled)
        _toastMessage.value = if (enabled) "Уведомления включены" else "Уведомления выключены"
    }

    fun setThemeMode(mode: AppThemeMode) {
        repository.setThemeMode(mode)
        _toastMessage.value = "Тема оформления: ${mode.displayName}"
    }

    fun testNotification(context: Context) {
        when (val result = NotificationHelper.showClassReminderNotification(
            context = context,
            title = "Через 15 минут: Лекция в ауд. 402",
            message = "«Алгоритмы и структуры данных» (проф. Соколов А.В.). Подготовьте конспект!"
        )) {
            is NotificationHelper.NotificationStatus.Success -> {
                _toastMessage.value = "🔔 Пуш-уведомление отправлено! Проверьте шторку."
            }
            is NotificationHelper.NotificationStatus.PermissionRequired -> {
                _toastMessage.value = "⚠️ Требуется разрешение на отправку уведомлений"
            }
            is NotificationHelper.NotificationStatus.NotificationsDisabledInSystem -> {
                _toastMessage.value = "⚠️ Уведомления отключены в системных настройках телефона"
            }
            is NotificationHelper.NotificationStatus.Error -> {
                _toastMessage.value = "Ошибка уведомления: ${result.message}"
            }
        }
    }

    fun clearChat() {
        repository.clearChatMessages()
        _toastMessage.value = "История чата очищена"
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
        if (index == 1) {
            _selectedTimetableDay.value = computeInitialTimetableDay()
            _selectedParityFilter.value = computeCalendarWeekParity()
        }
    }

    fun addOrUpdateSrsTask(task: SrsTask) {
        viewModelScope.launch {
            repository.addOrUpdateSrsTask(task)
            _toastMessage.value = "Задание «${task.title}» сохранено"
        }
    }

    fun toggleSrsTask(id: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleSrsTaskCompleted(id, isCompleted)
        }
    }

    fun deleteSrsTask(id: String) {
        viewModelScope.launch {
            repository.deleteSrsTask(id)
            _toastMessage.value = "Задание удалено"
        }
    }

    fun selectTimetableDay(day: DayOfWeek) {
        _selectedTimetableDay.value = day
    }

    fun selectParityFilter(parity: WeekParity) {
        _selectedParityFilter.value = parity
    }

    fun setAttendance(classId: String, date: LocalDate, status: AttendanceStatus) {
        val dateStr = date.toString()
        val prevStatus = repository.getAttendanceStatus(classId, dateStr)
        repository.setAttendance(classId, dateStr, status)
        if (status == AttendanceStatus.MISSED && prevStatus != AttendanceStatus.MISSED) {
            incrementSkip(classId)
        } else if (prevStatus == AttendanceStatus.MISSED && status != AttendanceStatus.MISSED) {
            decrementSkip(classId)
        }
    }

    fun setBellPreset(preset: BellSchedulePreset) {
        repository.setBellPreset(preset)
        _toastMessage.value = "Сетка звонков: ${preset.title}"
    }

    fun saveClass(slot: ClassSlot) {
        viewModelScope.launch {
            repository.addOrUpdateClass(slot)
            _toastMessage.value = "Пара «${slot.subjectTitle}» сохранена"
        }
    }

    fun deleteClass(id: String) {
        viewModelScope.launch {
            repository.deleteClass(id)
            _toastMessage.value = "Пара удалена из расписания"
        }
    }

    fun clearSchedule() {
        viewModelScope.launch {
            repository.clearAllClasses()
            _toastMessage.value = "Расписание очищено"
        }
    }

    fun loadDemoSchedule() {
        viewModelScope.launch {
            repository.loadDemoSchedule()
            _toastMessage.value = "Демо-расписание успешно загружено"
        }
    }

    fun incrementSkip(classId: String) {
        viewModelScope.launch {
            repository.incrementSkip(classId)
        }
    }

    fun decrementSkip(classId: String) {
        viewModelScope.launch {
            repository.decrementSkip(classId)
        }
    }

    fun updateAllowedSkips(classId: String, allowed: Int) {
        viewModelScope.launch {
            repository.updateAllowedSkips(classId, allowed)
        }
    }

    fun importClasses(classes: List<ClassSlot>) {
        viewModelScope.launch {
            classes.forEach { slot ->
                repository.addOrUpdateClass(slot)
            }
            _toastMessage.value = "Импортировано пар: ${classes.size}"
        }
    }

    fun importSrsTasks(tasks: List<SrsTask>) {
        viewModelScope.launch {
            tasks.forEach { task ->
                repository.addOrUpdateSrsTask(task)
            }
            _toastMessage.value = "Добавлено заданий СРС: ${tasks.size}"
        }
    }


    fun updateBellSlots(slots: List<BellSlot>) {
        repository.saveBellSlots(slots)
        _toastMessage.value = "Сетка звонков успешно обновлена"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    class Factory(
        private val repository: ScheduleRepository,
        private val observeUseCase: ObserveCurrentClassUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, observeUseCase) as T
        }
    }
}
