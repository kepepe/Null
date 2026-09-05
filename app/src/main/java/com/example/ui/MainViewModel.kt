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

    val attendanceMap: StateFlow<Map<String, AttendanceStatus>> = repository.attendanceFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedTimetableDay = MutableStateFlow(LocalDate.now().dayOfWeek)
    val selectedTimetableDay: StateFlow<DayOfWeek> = _selectedTimetableDay.asStateFlow()

    private val _selectedParityFilter = MutableStateFlow(WeekParity.ALL)
    val selectedParityFilter: StateFlow<WeekParity> = _selectedParityFilter.asStateFlow()

    private val _selectedChatChannel = MutableStateFlow("")
    val selectedChatChannel: StateFlow<String> = _selectedChatChannel.asStateFlow()

    private val _viewingFriendSchedule = MutableStateFlow<FriendUser?>(null)
    val viewingFriendSchedule: StateFlow<FriendUser?> = _viewingFriendSchedule.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

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
    }

    fun selectTimetableDay(day: DayOfWeek) {
        _selectedTimetableDay.value = day
    }

    fun selectParityFilter(parity: WeekParity) {
        _selectedParityFilter.value = parity
    }

    fun selectChatChannel(channelId: String) {
        _selectedChatChannel.value = channelId
        repository.setActiveChannel(channelId)
    }

    fun openFriendSchedule(friend: FriendUser?) {
        _viewingFriendSchedule.value = friend
    }

    fun openFriendChat(friendId: String) {
        _selectedChatChannel.value = friendId
        _currentTab.value = 3 // Chat tab
    }

    fun sendMessage(channelId: String, text: String) {
        repository.sendMessage(channelId, text)
    }

    fun createGroupChat(name: String, memberFriendIds: List<String>) {
        val group = repository.createGroupChat(name, memberFriendIds)
        _selectedChatChannel.value = group.id
        _toastMessage.value = "Группа «${group.name}» создана"
    }

    fun deleteGroupChat(groupId: String) {
        repository.deleteGroupChat(groupId)
        if (_selectedChatChannel.value == groupId) {
            _selectedChatChannel.value = ""
        }
        _toastMessage.value = "Групповой чат удалён"
    }

    fun setAttendance(classId: String, date: LocalDate, status: AttendanceStatus) {
        val dateStr = date.toString()
        repository.setAttendance(classId, dateStr, status)
    }

    fun shareScheduleToChat(channelId: String, classes: List<ClassSlot>, title: String) {
        val text = repository.formatScheduleForSharing(classes, title)
        repository.sendMessage(channelId, text)
        _toastMessage.value = "Расписание отправлено в чат"
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

    suspend fun addFriend(handle: String): Boolean {
        val (success, message) = repository.addFriendByHandle(handle)
        _toastMessage.value = message
        return success
    }

    fun addFriendInBg(handle: String) {
        viewModelScope.launch {
            addFriend(handle)
        }
    }

    fun syncWithCloud() {
        repository.pushMyProfileToCloud(currentClassStatus.value)
        _toastMessage.value = "Синхронизация с Firebase выполнена"
    }

    fun removeFriend(id: String) {
        repository.removeFriend(id)
        _toastMessage.value = "Друг удалён из списка"
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
