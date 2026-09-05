package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ScheduleRepository(
    private val classDao: ClassDao,
    private val context: Context,
    val firebaseService: FirebaseSyncService = FirebaseSyncService()
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE)

    // Stable User ID for this installation
    val myUserId: String = prefs.getString("local_user_uuid", null) ?: run {
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString("local_user_uuid", newId).apply()
        newId
    }

    // User profile state
    private val _userProfileFlow = MutableStateFlow(loadUserProfile())
    val userProfileFlow: Flow<UserProfile> = _userProfileFlow.asStateFlow()

    // Friends list
    private val _friendsFlow = MutableStateFlow<List<FriendUser>>(loadSavedFriends())
    val friendsFlow: Flow<List<FriendUser>> = _friendsFlow.asStateFlow()

    // Chat messages: combined local + real-time Firestore
    private val _chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessagesFlow: Flow<List<ChatMessage>> = _chatMessagesFlow.asStateFlow()

    // Custom Group Chats created with friends
    private val _groupChatsFlow = MutableStateFlow<List<GroupChat>>(loadGroupChats())
    val groupChatsFlow: Flow<List<GroupChat>> = _groupChatsFlow.asStateFlow()

    // Attendance map: key is "${classId}_${dateString}", value is AttendanceStatus
    private val _attendanceFlow = MutableStateFlow<Map<String, AttendanceStatus>>(loadAttendance())
    val attendanceFlow: Flow<Map<String, AttendanceStatus>> = _attendanceFlow.asStateFlow()

    private var currentActiveChannelId: String = ""

    // Sample schedules for directory friends
    private val alexSchedule = listOf(
        ClassSlot("as1", "Алгоритмы и структуры данных", ClassType.LECTURE, "проф. Соколов А.В.", "Ауд. 412", DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 35), colorHex = "#0061A4"),
        ClassSlot("as2", "Архитектура ЭВМ", ClassType.SEMINAR, "доц. Громов В.С.", "Ауд. 305", DayOfWeek.MONDAY, LocalTime.of(9, 50), LocalTime.of(11, 25), colorHex = "#6750A4"),
        ClassSlot("as3", "Дискретная математика", ClassType.LECTURE, "доц. Петрова Е.И.", "Зал 201", DayOfWeek.TUESDAY, LocalTime.of(11, 40), LocalTime.of(13, 15), colorHex = "#2E7D32"),
        ClassSlot("as4", "Разработка мобильных приложений", ClassType.LAB, "преп. Ильин Д.А.", "Комп. класс 5", DayOfWeek.WEDNESDAY, LocalTime.of(13, 30), LocalTime.of(15, 5), colorHex = "#006874"),
        ClassSlot("as5", "Операционные системы", ClassType.LECTURE, "проф. Соколов А.В.", "Ауд. 402", DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(9, 35), colorHex = "#0061A4"),
        ClassSlot("as6", "Иностранный язык в проф. сфере", ClassType.PRACTICUM, "ст. преп. Смирнова О.П.", "Ауд. 118", DayOfWeek.FRIDAY, LocalTime.of(9, 50), LocalTime.of(11, 25), colorHex = "#B3261E")
    )

    private val mariaSchedule = listOf(
        ClassSlot("ms1", "Высшая математика (Мат. анализ)", ClassType.LECTURE, "проф. Белов М.Ю.", "Ауд. 310", DayOfWeek.MONDAY, LocalTime.of(9, 50), LocalTime.of(11, 25), colorHex = "#6750A4"),
        ClassSlot("ms2", "Теория вероятностей", ClassType.SEMINAR, "доц. Козлова Т.В.", "Ауд. 214", DayOfWeek.MONDAY, LocalTime.of(11, 40), LocalTime.of(13, 15), colorHex = "#0061A4"),
        ClassSlot("ms3", "Общая физика", ClassType.LAB, "преп. Мельников С.А.", "Лаб. 12", DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 35), colorHex = "#006874"),
        ClassSlot("ms4", "Инженерная графика", ClassType.PRACTICUM, "доц. Орлов К.Е.", "Ауд. 418", DayOfWeek.WEDNESDAY, LocalTime.of(9, 50), LocalTime.of(11, 25), colorHex = "#E65100"),
        ClassSlot("ms5", "Экономика и менеджмент", ClassType.LECTURE, "доц. Павлов Н.А.", "Ауд. 501", DayOfWeek.THURSDAY, LocalTime.of(11, 40), LocalTime.of(13, 15), colorHex = "#2E7D32"),
        ClassSlot("ms6", "Физическая культура", ClassType.PRACTICUM, "инстр. Кузнецов П.Р.", "Спорткомплекс", DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 35), colorHex = "#455A64")
    )

    private val daniilSchedule = listOf(
        ClassSlot("ds1", "Базы данных (PostgreSQL / Redis)", ClassType.LAB, "преп. Васильев И.Д.", "Комп. класс 3", DayOfWeek.MONDAY, LocalTime.of(11, 40), LocalTime.of(13, 15), colorHex = "#006874"),
        ClassSlot("ds2", "Компьютерные сети", ClassType.LECTURE, "проф. Лебедев А.Н.", "Ауд. 204", DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(9, 35), colorHex = "#0061A4"),
        ClassSlot("ds3", "Безопасность информ. систем", ClassType.SEMINAR, "доц. Григорьев С.М.", "Ауд. 312", DayOfWeek.WEDNESDAY, LocalTime.of(13, 30), LocalTime.of(15, 5), colorHex = "#B3261E"),
        ClassSlot("ds4", "Web-программирование", ClassType.LAB, "преп. Романов В.В.", "Комп. класс 2", DayOfWeek.THURSDAY, LocalTime.of(9, 50), LocalTime.of(11, 25), colorHex = "#2E7D32"),
        ClassSlot("ds5", "Машинное обучение и анализ данных", ClassType.LECTURE, "доц. Смирнов А.А.", "Зал 101", DayOfWeek.FRIDAY, LocalTime.of(11, 40), LocalTime.of(13, 15), colorHex = "#6750A4")
    )

    val campusDirectory = listOf(
        FriendUser(
            id = "f1",
            displayName = "Александр Смирнов",
            handle = "@alex_sm",
            avatarInitials = "АС",
            avatarBgColorHex = "#0061A4",
            currentClass = "Алгоритмы и структуры данных",
            currentRoom = "Ауд. 412",
            classEndTime = "09:35",
            isAttendingClass = true,
            schedule = alexSchedule
        ),
        FriendUser(
            id = "f2",
            displayName = "Мария Новикова",
            handle = "@maria_n",
            avatarInitials = "МН",
            avatarBgColorHex = "#6750A4",
            currentClass = "Высшая математика",
            currentRoom = "Ауд. 310",
            classEndTime = "11:25",
            isAttendingClass = true,
            schedule = mariaSchedule
        ),
        FriendUser(
            id = "f3",
            displayName = "Даниил Кузнецов",
            handle = "@daniil_k",
            avatarInitials = "ДК",
            avatarBgColorHex = "#2E7D32",
            currentClass = "Базы данных (PostgreSQL)",
            currentRoom = "Комп. класс 3",
            classEndTime = "13:15",
            isAttendingClass = true,
            schedule = daniilSchedule
        ),
        FriendUser(
            id = "f4",
            displayName = "Екатерина Морозова",
            handle = "@kate_m",
            avatarInitials = "ЕМ",
            avatarBgColorHex = "#E65100",
            currentClass = null,
            currentRoom = null,
            classEndTime = null,
            isAttendingClass = false,
            schedule = mariaSchedule
        ),
        FriendUser(
            id = "f5",
            displayName = "Кирилл Васильев",
            handle = "@kirill_v",
            avatarInitials = "КВ",
            avatarBgColorHex = "#006874",
            currentClass = "Компьютерные сети",
            currentRoom = "Ауд. 204",
            classEndTime = "09:35",
            isAttendingClass = true,
            schedule = daniilSchedule
        )
    )

    init {
        // Observe and sync presence of active friends from Firestore in background
        syncFriendsFromFirestore()
    }

    private fun syncFriendsFromFirestore() {
        repoScope.launch {
            _friendsFlow.value.forEach { friend ->
                if (!friend.id.startsWith("f")) { // Cloud friend
                    launch {
                        firebaseService.observeFriend(friend.id).collect { updatedFriend ->
                            if (updatedFriend != null) {
                                val current = _friendsFlow.value.toMutableList()
                                val index = current.indexOfFirst { it.id == updatedFriend.id }
                                if (index != -1) {
                                    current[index] = updatedFriend
                                    _friendsFlow.value = current
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun observeAllClasses(): Flow<List<ClassSlot>> {
        return classDao.getAllClasses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addOrUpdateClass(slot: ClassSlot) {
        classDao.insertOrUpdate(ClassEntity.fromDomain(slot))
        pushMyProfileToCloud()
    }

    suspend fun deleteClass(id: String) {
        classDao.deleteById(id)
        pushMyProfileToCloud()
    }

    suspend fun clearAllClasses() {
        classDao.clearAll()
        pushMyProfileToCloud()
    }

    fun pushMyProfileToCloud(currentStatus: CurrentClassStatus = CurrentClassStatus.NoClassesToday) {
        repoScope.launch {
            try {
                val profile = _userProfileFlow.value
                val classes = classDao.getAllClassesSync().map { it.toDomain() }
                firebaseService.publishUserProfile(
                    userId = myUserId,
                    profile = profile,
                    currentClasses = classes,
                    currentStatus = currentStatus
                )
            } catch (e: Exception) {
                Log.w("ScheduleRepository", "Failed to sync to cloud: ${e.message}")
            }
        }
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
        pushMyProfileToCloud()
        return updated
    }

    fun updateUserAvatar(uri: String?) {
        prefs.edit().putString("user_avatar_uri", uri).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(avatarUri = uri)
        pushMyProfileToCloud()
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

    suspend fun addFriendByHandle(query: String): Pair<Boolean, String> {
        val cleanQuery = query.trim().lowercase()
        val normalized = if (cleanQuery.startsWith("@")) cleanQuery else "@$cleanQuery"
        val currentFriends = _friendsFlow.value

        if (currentFriends.any { it.handle.lowercase() == normalized }) {
            return Pair(false, "Этот друг уже в вашем списке!")
        }

        // 1. Try finding in Firebase Cloud Firestore first
        val cloudFriend = firebaseService.searchUserByHandle(normalized)
        if (cloudFriend != null) {
            val updated = currentFriends + cloudFriend
            _friendsFlow.value = updated
            saveFriends(updated)
            // Start observing this friend
            repoScope.launch {
                firebaseService.observeFriend(cloudFriend.id).collect { updatedFriend ->
                    if (updatedFriend != null) {
                        val curr = _friendsFlow.value.toMutableList()
                        val idx = curr.indexOfFirst { it.id == updatedFriend.id }
                        if (idx != -1) {
                            curr[idx] = updatedFriend
                            _friendsFlow.value = curr
                        }
                    }
                }
            }
            return Pair(true, "Друг ${cloudFriend.displayName} (${cloudFriend.handle}) найден в облаке Firebase!")
        }

        // 2. Check local campus directory
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

        val updated = currentFriends + friendToAdd
        _friendsFlow.value = updated
        saveFriends(updated)
        return Pair(true, "Друг ${friendToAdd.displayName} успешно добавлен!")
    }

    fun removeFriend(friendId: String) {
        val updated = _friendsFlow.value.filterNot { it.id == friendId }
        _friendsFlow.value = updated
        saveFriends(updated)
    }

    private fun saveFriends(friends: List<FriendUser>) {
        val serialized = friends.joinToString(";;;") { f ->
            "${f.id}|||${f.displayName}|||${f.handle}|||${f.avatarInitials}|||${f.avatarBgColorHex}|||${f.currentClass.orEmpty()}|||${f.currentRoom.orEmpty()}|||${f.classEndTime.orEmpty()}|||${f.isAttendingClass}"
        }
        prefs.edit().putString("saved_friends_list", serialized).apply()
    }

    private fun loadSavedFriends(): List<FriendUser> {
        val raw = prefs.getString("saved_friends_list", null) ?: return emptyList()
        return raw.split(";;;").mapNotNull { friendStr ->
            val parts = friendStr.split("|||")
            if (parts.size >= 9) {
                FriendUser(
                    id = parts[0],
                    displayName = parts[1],
                    handle = parts[2],
                    avatarInitials = parts[3],
                    avatarBgColorHex = parts[4],
                    currentClass = parts[5].ifBlank { null },
                    currentRoom = parts[6].ifBlank { null },
                    classEndTime = parts[7].ifBlank { null },
                    isAttendingClass = parts[8].toBooleanStrictOrNull() ?: false,
                    schedule = if (parts[2].contains("maria")) mariaSchedule else alexSchedule
                )
            } else null
        }
    }

    // --- Chat Realtime Connection with Firebase ---

    fun setActiveChannel(channelId: String) {
        currentActiveChannelId = channelId
        if (channelId.isBlank()) return

        // Listen to Firestore real-time collection
        repoScope.launch {
            firebaseService.observeMessages(channelId, myUserId).collect { cloudMessages ->
                // Merge cloud messages with local ones
                val localForChannel = _chatMessagesFlow.value.filter { it.channelId == channelId && !it.id.startsWith("msg_cloud_") }
                val cloudIds = cloudMessages.map { it.id }.toSet()
                val merged = (localForChannel.filterNot { cloudIds.contains(it.id) } + cloudMessages)
                    .sortedBy { it.timestamp }

                val otherChannels = _chatMessagesFlow.value.filter { it.channelId != channelId }
                _chatMessagesFlow.value = otherChannels + merged
            }
        }
    }

    fun sendMessage(channelId: String, text: String) {
        if (text.isBlank()) return
        val user = _userProfileFlow.value
        val myName = if (user.name.isNotBlank()) user.name else "Я"
        val myHandle = if (user.handle.isNotBlank()) user.handle else "@me"
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val localId = UUID.randomUUID().toString()
        val newMsg = ChatMessage(
            id = localId,
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

        // Send to Firebase Firestore
        repoScope.launch {
            firebaseService.sendMessage(
                channelId = channelId,
                text = text,
                senderId = myUserId,
                senderName = myName,
                senderHandle = myHandle,
                senderAvatarUri = user.avatarUri,
                timestampText = currentTime
            )
        }
    }

    fun clearChatMessages() {
        _chatMessagesFlow.value = emptyList()
    }

    // --- Custom Group Chats Persistence & Operations ---
    private fun loadGroupChats(): List<GroupChat> {
        val raw = prefs.getString("custom_group_chats", null) ?: return emptyList()
        return raw.split(";;;").mapNotNull { groupStr ->
            val parts = groupStr.split("|||")
            if (parts.size >= 3) {
                val id = parts[0]
                val name = parts[1]
                val members = if (parts[2].isNotBlank()) parts[2].split(",") else emptyList()
                GroupChat(id = id, name = name, memberFriendIds = members)
            } else null
        }
    }

    private fun saveGroupChats(groups: List<GroupChat>) {
        val serialized = groups.joinToString(";;;") { g ->
            "${g.id}|||${g.name}|||${g.memberFriendIds.joinToString(",")}"
        }
        prefs.edit().putString("custom_group_chats", serialized).apply()
    }

    fun createGroupChat(name: String, memberFriendIds: List<String>): GroupChat {
        val newGroup = GroupChat(
            id = "grp_" + UUID.randomUUID().toString().take(8),
            name = name.trim(),
            memberFriendIds = memberFriendIds
        )
        val updated = _groupChatsFlow.value + newGroup
        _groupChatsFlow.value = updated
        saveGroupChats(updated)
        return newGroup
    }

    fun deleteGroupChat(groupId: String) {
        val updated = _groupChatsFlow.value.filterNot { it.id == groupId }
        _groupChatsFlow.value = updated
        saveGroupChats(updated)
    }

    // --- Attendance Operations ---
    private fun loadAttendance(): Map<String, AttendanceStatus> {
        val raw = prefs.getString("attendance_map", null) ?: return emptyMap()
        val result = mutableMapOf<String, AttendanceStatus>()
        raw.split(";").forEach { item ->
            val parts = item.split("=")
            if (parts.size == 2) {
                val key = parts[0]
                val status = runCatching { AttendanceStatus.valueOf(parts[1]) }.getOrNull()
                if (status != null) {
                    result[key] = status
                }
            }
        }
        return result
    }

    private fun saveAttendance(map: Map<String, AttendanceStatus>) {
        val serialized = map.entries.joinToString(";") { "${it.key}=${it.value.name}" }
        prefs.edit().putString("attendance_map", serialized).apply()
    }

    fun setAttendance(classId: String, dateString: String, status: AttendanceStatus) {
        val key = "${classId}_${dateString}"
        val current = _attendanceFlow.value.toMutableMap()
        if (status == AttendanceStatus.NOT_MARKED) {
            current.remove(key)
        } else {
            current[key] = status
        }
        _attendanceFlow.value = current
        saveAttendance(current)
    }

    fun formatScheduleForSharing(classes: List<ClassSlot>, title: String): String {
        val sb = java.lang.StringBuilder()
        sb.append("📅 Расписание: $title\n")
        if (classes.isEmpty()) {
            sb.append("Пар нет. Отдыхаем! 🎉")
        } else {
            classes.sortedBy { it.startTime }.forEachIndexed { index, slot ->
                sb.append("${index + 1}. [${slot.formattedTimeSpan}] ${slot.subjectTitle} (${slot.classType.displayName})\n")
                sb.append("   📍 ${slot.classroom} • 👤 ${slot.professor}\n")
            }
        }
        return sb.toString().trim()
    }

    suspend fun loadDemoSchedule() {
        val sampleList = listOf(
            ClassSlot(
                id = UUID.randomUUID().toString(),
                subjectTitle = "Алгоритмы и структуры данных",
                classType = ClassType.LECTURE,
                professor = "проф. Соколов А.В.",
                classroom = "Ауд. 402 (Главный корпус)",
                dayOfWeek = java.time.LocalDate.now().dayOfWeek,
                startTime = LocalTime.now().minusMinutes(15),
                endTime = LocalTime.now().plusMinutes(75),
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
                startTime = LocalTime.now().plusMinutes(85),
                endTime = LocalTime.now().plusMinutes(175),
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
                startTime = LocalTime.of(9, 50),
                endTime = LocalTime.of(11, 25),
                weekParity = WeekParity.EVEN,
                colorHex = "#006874"
            )
        )
        classDao.insertAll(sampleList.map { ClassEntity.fromDomain(it) })
        pushMyProfileToCloud()
    }
}
