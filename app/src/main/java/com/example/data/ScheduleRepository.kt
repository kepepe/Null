package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ScheduleRepository(
    private val classDao: ClassDao,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE)

    // User profile state
    private val _userProfileFlow = MutableStateFlow(loadUserProfile())
    val userProfileFlow: Flow<UserProfile> = _userProfileFlow.asStateFlow()

    // Friends list (starts empty by default)
    private val _friendsFlow = MutableStateFlow<List<FriendUser>>(emptyList())
    val friendsFlow: Flow<List<FriendUser>> = _friendsFlow.asStateFlow()

    // Chat messages (starts empty by default per user request)
    private val _chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessagesFlow: Flow<List<ChatMessage>> = _chatMessagesFlow.asStateFlow()

    // Sample schedules for directory friends
    private val alexSchedule = listOf(
        ClassSlot("as1", "Алгоритмы и структуры данных", ClassType.LECTURE, "проф. Соколов А.В.", "Ауд. 412", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30)),
        ClassSlot("as2", "Архитектура ЭВМ", ClassType.SEMINAR, "доц. Громов В.С.", "Ауд. 305", DayOfWeek.MONDAY, LocalTime.of(10, 45), LocalTime.of(12, 15)),
        ClassSlot("as3", "Дискретная математика", ClassType.LECTURE, "доц. Петрова Е.И.", "Зал 201", DayOfWeek.TUESDAY, LocalTime.of(11, 0), LocalTime.of(12, 30)),
        ClassSlot("as4", "Разработка на Kotlin / Compose", ClassType.LAB, "преп. Васильев И.Д.", "Лаб. 4", DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
        ClassSlot("as5", "Операционные системы", ClassType.LECTURE, "проф. Ильин Д.А.", "Ауд. 108", DayOfWeek.THURSDAY, LocalTime.of(13, 0), LocalTime.of(14, 30)),
        ClassSlot("as6", "Английский язык в IT", ClassType.PRACTICUM, "преп. Смит М.В.", "Ауд. 510", DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(11, 30))
    )

    private val mariaSchedule = listOf(
        ClassSlot("ms1", "Математический анализ", ClassType.LECTURE, "проф. Ковалева Н.Н.", "Зал 101", DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 30)),
        ClassSlot("ms2", "Линейная алгебра", ClassType.SEMINAR, "доц. Кузнецов П.А.", "Ауд. 218", DayOfWeek.MONDAY, LocalTime.of(10, 45), LocalTime.of(12, 15)),
        ClassSlot("ms3", "Теория вероятностей", ClassType.LECTURE, "проф. Сорокин Б.А.", "Ауд. 312", DayOfWeek.WEDNESDAY, LocalTime.of(11, 0), LocalTime.of(12, 30)),
        ClassSlot("ms4", "Экономика IT-проектов", ClassType.SEMINAR, "доц. Белова О.В.", "Ауд. 405", DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 30)),
        ClassSlot("ms5", "Философия науки", ClassType.LECTURE, "проф. Волков С.М.", "Зал 300", DayOfWeek.FRIDAY, LocalTime.of(12, 0), LocalTime.of(13, 30))
    )

    private val daniilSchedule = listOf(
        ClassSlot("ds1", "Базы данных (SQL / NoSQL)", ClassType.LAB, "преп. Васильев И.Д.", "Лаб. 3В", DayOfWeek.MONDAY, LocalTime.of(11, 30), LocalTime.of(13, 0)),
        ClassSlot("ds2", "Компьютерные сети", ClassType.LECTURE, "доц. Смирнов А.А.", "Ауд. 204", DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(10, 30)),
        ClassSlot("ds3", "Информационная безопасность", ClassType.SEMINAR, "проф. Романов К.В.", "Ауд. 411", DayOfWeek.THURSDAY, LocalTime.of(12, 0), LocalTime.of(13, 30)),
        ClassSlot("ds4", "Web-разработка", ClassType.PRACTICUM, "преп. Чернов Д.И.", "Лаб. 2", DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(15, 30))
    )

    // Public campus directory to discover friends by handle / tag
    private val campusDirectory = listOf(
        FriendUser(
            id = "f1",
            displayName = "Алексей Смирнов",
            handle = "@alex_sm",
            avatarInitials = "АС",
            avatarBgColorHex = "#0061A4",
            currentClass = "Алгоритмы и структуры данных",
            currentRoom = "Ауд. 412",
            classEndTime = "11:45",
            isAttendingClass = true,
            schedule = alexSchedule
        ),
        FriendUser(
            id = "f2",
            displayName = "Мария Новикова",
            handle = "@maria_n",
            avatarInitials = "МН",
            avatarBgColorHex = "#6750A4",
            currentClass = "Математический анализ",
            currentRoom = "Зал 101",
            classEndTime = "12:15",
            isAttendingClass = true,
            schedule = mariaSchedule
        ),
        FriendUser(
            id = "f3",
            displayName = "Даниил Кузнецов",
            handle = "@daniil_k",
            avatarInitials = "ДК",
            avatarBgColorHex = "#7D5260",
            currentClass = "Базы данных (Лаб)",
            currentRoom = "Лаб. 3В",
            classEndTime = "13:00",
            isAttendingClass = true,
            schedule = daniilSchedule
        ),
        FriendUser(
            id = "f4",
            displayName = "Екатерина Морозова",
            handle = "@katya_m",
            avatarInitials = "ЕМ",
            avatarBgColorHex = "#386A20",
            currentClass = "Философия",
            currentRoom = "Зал 300",
            classEndTime = "14:00",
            isAttendingClass = false,
            schedule = emptyList()
        ),
        FriendUser(
            id = "f5",
            displayName = "Кирилл Васильев",
            handle = "@kirill_v",
            avatarInitials = "КВ",
            avatarBgColorHex = "#006874",
            currentClass = "Компьютерные сети",
            currentRoom = "Ауд. 204",
            classEndTime = "10:30",
            isAttendingClass = true,
            schedule = daniilSchedule
        )
    )

    fun observeAllClasses(): Flow<List<ClassSlot>> {
        return classDao.getAllClasses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addOrUpdateClass(slot: ClassSlot) {
        classDao.insertOrUpdate(ClassEntity.fromDomain(slot))
    }

    suspend fun deleteClass(id: String) {
        classDao.deleteById(id)
    }

    suspend fun clearAllClasses() {
        classDao.clearAll()
    }

    private fun loadUserProfile(): UserProfile {
        val isRegistered = prefs.getBoolean("user_registered", false)
        val name = prefs.getString("user_name", "") ?: ""
        val handle = prefs.getString("user_handle", "") ?: ""
        val university = prefs.getString("user_university", "") ?: ""
        val avatarUri = prefs.getString("user_avatar_uri", null)
        val parityModeStr = prefs.getString("user_parity_mode", WeekParityMode.AUTO.name) ?: WeekParityMode.AUTO.name
        val parityMode = runCatching { WeekParityMode.valueOf(parityModeStr) }.getOrDefault(WeekParityMode.AUTO)
        val notifications = prefs.getBoolean("user_notifications_enabled", true)
        val themeModeStr = prefs.getString("user_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val themeMode = runCatching { AppThemeMode.valueOf(themeModeStr) }.getOrDefault(AppThemeMode.SYSTEM)

        return UserProfile(
            isRegistered = isRegistered,
            name = name,
            handle = handle,
            university = university,
            avatarUri = avatarUri,
            parityMode = parityMode,
            notificationsEnabled = notifications,
            themeMode = themeMode
        )
    }

    fun saveUserProfile(name: String, handle: String, university: String, avatarUri: String? = null): UserProfile {
        val cleanHandle = if (handle.startsWith("@")) handle else "@$handle"
        val current = _userProfileFlow.value

        prefs.edit()
            .putBoolean("user_registered", true)
            .putString("user_name", name)
            .putString("user_handle", cleanHandle)
            .putString("user_university", university)
            .apply()

        if (avatarUri != null) {
            prefs.edit().putString("user_avatar_uri", avatarUri).apply()
        }

        val updated = current.copy(
            isRegistered = true,
            name = name,
            handle = cleanHandle,
            university = university,
            avatarUri = avatarUri ?: current.avatarUri
        )
        _userProfileFlow.value = updated
        return updated
    }

    fun updateUserAvatar(uri: String?) {
        prefs.edit().putString("user_avatar_uri", uri).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(avatarUri = uri)
    }

    fun setWeekParityMode(mode: WeekParityMode) {
        prefs.edit().putString("user_parity_mode", mode.name).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(parityMode = mode)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("user_theme_mode", mode.name).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(themeMode = mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("user_notifications_enabled", enabled).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(notificationsEnabled = enabled)
    }

    fun addFriendByHandle(query: String): Pair<Boolean, String> {
        val cleanQuery = query.trim().lowercase()
        val normalized = if (cleanQuery.startsWith("@")) cleanQuery else "@$cleanQuery"
        val currentFriends = _friendsFlow.value

        if (currentFriends.any { it.handle.lowercase() == normalized }) {
            return Pair(false, "Этот друг уже в вашем списке!")
        }

        val fromDirectory = campusDirectory.firstOrNull { it.handle.lowercase() == normalized }

        val friendToAdd = fromDirectory ?: FriendUser(
            id = UUID.randomUUID().toString(),
            displayName = if (cleanQuery.contains("_")) {
                cleanQuery.replace("@", "").replace("_", " ").split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            } else {
                "Студент ${cleanQuery.replace("@", "")}"
            },
            handle = normalized,
            avatarInitials = cleanQuery.replace("@", "").take(2).uppercase(),
            avatarBgColorHex = "#0061A4",
            currentClass = "Физика (Семинар)",
            currentRoom = "Ауд. 204",
            classEndTime = "12:30",
            isAttendingClass = true,
            schedule = alexSchedule
        )

        _friendsFlow.value = currentFriends + friendToAdd
        return Pair(true, "Друг ${friendToAdd.displayName} успешно добавлен!")
    }

    fun removeFriend(friendId: String) {
        _friendsFlow.value = _friendsFlow.value.filterNot { it.id == friendId }
    }

    fun sendMessage(channelId: String, text: String) {
        if (text.isBlank()) return
        val user = _userProfileFlow.value
        val myName = if (user.name.isNotBlank()) user.name else "Я"
        val myHandle = if (user.handle.isNotBlank()) user.handle else "@me"
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val newMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            channelId = channelId,
            senderName = myName,
            senderHandle = myHandle,
            senderAvatarUri = user.avatarUri,
            senderAvatarBgColorHex = "#0061A4",
            text = text.trim(),
            timestamp = currentTime,
            isFromMe = true
        )
        _chatMessagesFlow.value = _chatMessagesFlow.value + newMsg
    }

    fun clearChatMessages() {
        _chatMessagesFlow.value = emptyList()
    }

    // Optional demo loader if user wants to see sample schedule
    suspend fun loadDemoSchedule() {
        val now = LocalTime.now()
        val currentSlotStart = now.minusMinutes(20)
        val currentSlotEnd = now.plusMinutes(70)
        val nextSlotStart = currentSlotEnd.plusMinutes(20)
        val nextSlotEnd = nextSlotStart.plusMinutes(90)

        val sampleList = listOf(
            ClassSlot(
                id = UUID.randomUUID().toString(),
                subjectTitle = "Алгоритмы и структуры данных",
                classType = ClassType.LECTURE,
                professor = "проф. Соколов А.В.",
                classroom = "Ауд. 402",
                dayOfWeek = java.time.LocalDate.now().dayOfWeek,
                startTime = currentSlotStart,
                endTime = currentSlotEnd,
                weekParity = WeekParity.ALL,
                colorHex = "#0061A4"
            ),
            ClassSlot(
                id = UUID.randomUUID().toString(),
                subjectTitle = "Математический анализ",
                classType = ClassType.SEMINAR,
                professor = "доц. Петрова Е.И.",
                classroom = "Ауд. 215",
                dayOfWeek = java.time.LocalDate.now().dayOfWeek,
                startTime = nextSlotStart,
                endTime = nextSlotEnd,
                weekParity = WeekParity.ALL,
                colorHex = "#6750A4"
            ),
            ClassSlot(
                id = UUID.randomUUID().toString(),
                subjectTitle = "Базы данных (SQL / NoSQL)",
                classType = ClassType.LAB,
                professor = "преп. Васильев И.Д.",
                classroom = "Компьютерный класс 3",
                dayOfWeek = DayOfWeek.TUESDAY,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 30),
                weekParity = WeekParity.EVEN,
                colorHex = "#006874"
            )
        )
        classDao.insertAll(sampleList.map { ClassEntity.fromDomain(it) })
    }
}
