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

    // Subject Presets & Memory Suggestions for fast schedule adding
    private val _subjectPresetsFlow = MutableStateFlow<List<SubjectPreset>>(loadSubjectPresets())
    val subjectPresetsFlow: Flow<List<SubjectPreset>> = _subjectPresetsFlow.asStateFlow()

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
        // Auto-sync profile to cloud on app launch if user has registered
        if (_userProfileFlow.value.isRegistered && _userProfileFlow.value.handle.isNotBlank()) {
            pushMyProfileToCloud()
        }
        // Seed directory users to Firestore so classmates exist in cloud database
        seedCampusDirectoryToFirestore()
        // Observe and sync presence of active friends from Firestore in background
        syncFriendsFromFirestore()
        observeGroupChatsFromCloud()
    }

    private fun seedCampusDirectoryToFirestore() {
        repoScope.launch {
            try {
                campusDirectory.forEach { friend ->
                    val clean = friend.handle.trim().lowercase().removePrefix("@")
                    firebaseService.publishUserProfile(
                        userId = friend.id,
                        profile = UserProfile(
                            isRegistered = true,
                            name = friend.displayName,
                            handle = friend.handle,
                            university = "СПбПУ / ИТМО"
                        ),
                        currentClasses = friend.schedule,
                        currentStatus = if (friend.isAttendingClass && friend.currentClass != null) {
                            val slot = friend.schedule.firstOrNull { it.subjectTitle == friend.currentClass } ?: friend.schedule.first()
                            CurrentClassStatus.ActiveClass(slot, 45L, friend.schedule.getOrNull(1))
                        } else CurrentClassStatus.NoClassesToday
                    )
                }
            } catch (e: Exception) {
                Log.w("ScheduleRepository", "Campus directory seeding notice: ${e.message}")
            }
        }
    }

    private fun syncFriendsFromFirestore() {
        repoScope.launch {
            _friendsFlow.value.forEach { friend ->
                launch {
                    firebaseService.observeFriend(friend.id).collect { updatedFriend ->
                        if (updatedFriend != null) {
                            val current = _friendsFlow.value.toMutableList()
                            val index = current.indexOfFirst { it.id == updatedFriend.id || it.handle.equals(updatedFriend.handle, ignoreCase = true) }
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

    private fun observeGroupChatsFromCloud() {
        val myHandle = _userProfileFlow.value.handle
        if (myHandle.isBlank()) return
        repoScope.launch {
            firebaseService.observeGroupChats(myHandle).collect { cloudGroups ->
                if (cloudGroups.isNotEmpty()) {
                    val local = _groupChatsFlow.value
                    val localIds = local.map { it.id }.toSet()
                    val newOnes = cloudGroups.filterNot { localIds.contains(it.id) }
                    if (newOnes.isNotEmpty()) {
                        val merged = local + newOnes
                        _groupChatsFlow.value = merged
                        saveGroupChats(merged)
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
        saveSubjectPreset(slot)
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
        val cleanQuery = query.trim().lowercase().removePrefix("@")
        if (cleanQuery.length < 2) {
            return Pair(false, "Тег слишком короткий")
        }
        val normalized = "@$cleanQuery"
        val currentFriends = _friendsFlow.value

        val myClean = _userProfileFlow.value.handle.trim().lowercase().removePrefix("@")
        if (cleanQuery == myClean) {
            return Pair(false, "Вы не можете добавить самого себя в друзья")
        }

        if (currentFriends.any { it.handle.trim().lowercase().removePrefix("@") == cleanQuery }) {
            return Pair(false, "Пользователь $normalized уже есть в вашем списке друзей")
        }

        // Search strictly from Firebase Cloud Firestore
        val cloudFriend = firebaseService.searchUserByHandle(cleanQuery)
        if (cloudFriend != null) {
            val updated = currentFriends + cloudFriend
            _friendsFlow.value = updated
            saveFriends(updated)
            // Start observing this friend in real-time
            repoScope.launch {
                firebaseService.observeFriend(cloudFriend.id).collect { updatedFriend ->
                    if (updatedFriend != null) {
                        val curr = _friendsFlow.value.toMutableList()
                        val idx = curr.indexOfFirst { it.id == updatedFriend.id || it.handle.equals(updatedFriend.handle, ignoreCase = true) }
                        if (idx != -1) {
                            curr[idx] = updatedFriend
                            _friendsFlow.value = curr
                        }
                    }
                }
            }
            return Pair(true, "Студент ${cloudFriend.displayName} ($normalized) добавлен из базы данных!")
        }

        // Strict rejection - random text will NOT add anyone!
        return Pair(false, "Пользователь с тегом $normalized не найден в базе данных. Проверьте правильность тега.")
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

    private var channelObserverJob: kotlinx.coroutines.Job? = null

    // --- Chat Realtime Connection with Firebase ---

    fun getCanonicalChatChannelId(targetIdOrHandle: String): String {
        if (targetIdOrHandle.startsWith("grp_")) return targetIdOrHandle

        val myHandle = _userProfileFlow.value.handle.trim().lowercase().removePrefix("@")
            .ifBlank { "u_${myUserId.take(6)}" }
        val friend = _friendsFlow.value.firstOrNull {
            it.id == targetIdOrHandle ||
                    it.handle.equals(targetIdOrHandle, ignoreCase = true) ||
                    it.handle.trim().lowercase().removePrefix("@") == targetIdOrHandle.trim().lowercase().removePrefix("@")
        }
        val otherHandle = (friend?.handle ?: targetIdOrHandle).trim().lowercase().removePrefix("@")

        val first = minOf(myHandle, otherHandle)
        val second = maxOf(myHandle, otherHandle)
        return "dm_${first}_${second}"
    }

    fun setActiveChannel(channelId: String) {
        currentActiveChannelId = channelId
        if (channelId.isBlank()) return

        val canonicalId = getCanonicalChatChannelId(channelId)
        val myHandle = _userProfileFlow.value.handle

        channelObserverJob?.cancel()
        channelObserverJob = repoScope.launch {
            firebaseService.observeMessages(canonicalId, myUserId, myHandle).collect { cloudMessages ->
                val mapped = cloudMessages.map { it.copy(channelId = channelId) }
                val currentLocal = _chatMessagesFlow.value.filter { it.channelId == channelId }
                val merged = (currentLocal + mapped).distinctBy { msg ->
                    if (msg.id.isNotBlank()) msg.id else "${msg.senderHandle}_${msg.text}_${msg.timestamp}"
                }
                val otherChannels = _chatMessagesFlow.value.filter { it.channelId != channelId }
                _chatMessagesFlow.value = (otherChannels + (if (mapped.isNotEmpty()) mapped else merged)).sortedBy { it.timestamp }
            }
        }
    }

    fun sendMessage(channelId: String, text: String) {
        if (text.isBlank()) return
        val user = _userProfileFlow.value
        val myName = if (user.name.isNotBlank()) user.name else "Студент"
        val myHandle = if (user.handle.isNotBlank()) user.handle else "@student"
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val canonicalId = getCanonicalChatChannelId(channelId)

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

        // Send to Firebase Firestore using canonicalId
        repoScope.launch {
            firebaseService.sendMessage(
                channelId = canonicalId,
                text = text,
                senderId = myUserId,
                senderName = myName,
                senderHandle = myHandle,
                senderAvatarUri = user.avatarUri,
                timestampText = currentTime
            )
        }

        // Automatic smart response from classmate if chatting with a directory friend
        val friend = _friendsFlow.value.firstOrNull {
            it.id == channelId ||
                    it.handle.equals(channelId, ignoreCase = true) ||
                    it.handle.trim().lowercase().removePrefix("@") == channelId.trim().lowercase().removePrefix("@")
        }
        if (friend != null && !channelId.startsWith("grp_")) {
            repoScope.launch {
                kotlinx.coroutines.delay(1200)
                val replyText = generateSmartStudentReply(friend, text)
                val replyTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                val replyMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    channelId = channelId,
                    senderName = friend.displayName,
                    senderHandle = friend.handle,
                    senderAvatarUri = friend.avatarUri,
                    senderAvatarBgColorHex = friend.avatarBgColorHex,
                    text = replyText,
                    timestamp = replyTime,
                    isFromMe = false
                )
                _chatMessagesFlow.value = _chatMessagesFlow.value + replyMsg
                firebaseService.sendMessage(
                    channelId = canonicalId,
                    text = replyText,
                    senderId = friend.id,
                    senderName = friend.displayName,
                    senderHandle = friend.handle,
                    senderAvatarUri = friend.avatarUri,
                    timestampText = replyTime
                )
            }
        }
    }

    private fun generateSmartStudentReply(friend: FriendUser, userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("где") || lower.contains("аудитор") || lower.contains("пара") -> {
                if (friend.isAttendingClass && friend.currentClass != null) {
                    "Я сейчас на паре «${friend.currentClass}» в ${friend.currentRoom ?: "аудитории"} (до ${friend.classEndTime ?: "конца пары"}) 🎓"
                } else {
                    "Сейчас свободен, пар нет ☕ Встретимся в буфете или в коворкинге?"
                }
            }
            lower.contains("конспект") || lower.contains("скинь") || lower.contains("материал") -> {
                "Держи конспекты! Загрузил последние записи в общую папку 👍"
            }
            lower.contains("расписани") -> {
                "Спасибо за расписание! Сверил со своим — совпадает на этой неделе 👍"
            }
            lower.contains("привет") || lower.contains("ку") || lower.contains("здравствуй") -> {
                "Привет! Как успехи на парах сегодня?"
            }
            else -> {
                "Привет! Сообщение получил. Увидимся на следующей паре!"
            }
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

        // Sync to cloud
        val myHandle = _userProfileFlow.value.handle
        val memberHandles = _friendsFlow.value
            .filter { memberFriendIds.contains(it.id) || memberFriendIds.contains(it.handle) }
            .map { it.handle }
        repoScope.launch {
            firebaseService.publishGroupChat(newGroup, myHandle, memberHandles)
        }
        return newGroup
    }

    fun deleteGroupChat(groupId: String) {
        val updated = _groupChatsFlow.value.filterNot { it.id == groupId }
        _groupChatsFlow.value = updated
        saveGroupChats(updated)
    }

    // --- Subject Presets & Memory Suggestions ---

    private fun loadSubjectPresets(): List<SubjectPreset> {
        val raw = prefs.getString("saved_subject_presets", null) ?: return defaultSubjectPresets()
        val list = raw.split(";;;").mapNotNull { item ->
            val p = item.split("|||")
            if (p.isNotEmpty() && p[0].isNotBlank()) {
                SubjectPreset(
                    title = p[0],
                    professor = p.getOrElse(1) { "" },
                    classroom = p.getOrElse(2) { "" },
                    classType = runCatching { ClassType.valueOf(p.getOrElse(3) { "LECTURE" }) }.getOrDefault(ClassType.LECTURE),
                    colorHex = p.getOrElse(4) { "#0061A4" }
                )
            } else null
        }
        return if (list.isEmpty()) defaultSubjectPresets() else list
    }

    private fun defaultSubjectPresets(): List<SubjectPreset> = listOf(
        SubjectPreset("Высшая математика", "проф. Белов М.Ю.", "Ауд. 310", ClassType.LECTURE, "#6750A4"),
        SubjectPreset("Информатика и программирование", "преп. Ильин Д.А.", "Комп. класс 5", ClassType.LAB, "#0061A4"),
        SubjectPreset("Базы данных (SQL)", "преп. Васильев И.Д.", "Комп. класс 3", ClassType.LAB, "#006874"),
        SubjectPreset("Алгоритмы и структуры данных", "проф. Соколов А.В.", "Ауд. 412", ClassType.LECTURE, "#0061A4"),
        SubjectPreset("Физика", "доц. Петрова Е.И.", "Лаб. 12", ClassType.LAB, "#E65100"),
        SubjectPreset("Иностранный язык", "ст. преп. Смирнова О.П.", "Ауд. 118", ClassType.PRACTICUM, "#B3261E"),
        SubjectPreset("История России", "доц. Павлов Н.А.", "Ауд. 201", ClassType.SEMINAR, "#2E7D32")
    )

    private fun saveSubjectPreset(slot: ClassSlot) {
        val current = _subjectPresetsFlow.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.title.equals(slot.subjectTitle, ignoreCase = true) }
        val newPreset = SubjectPreset(
            title = slot.subjectTitle.trim(),
            professor = slot.professor.trim(),
            classroom = slot.classroom.trim(),
            classType = slot.classType,
            colorHex = slot.colorHex ?: "#0061A4"
        )
        if (existingIndex >= 0) {
            current[existingIndex] = newPreset
        } else {
            current.add(0, newPreset)
        }
        _subjectPresetsFlow.value = current
        val serialized = current.joinToString(";;;") {
            "${it.title}|||${it.professor}|||${it.classroom}|||${it.classType.name}|||${it.colorHex}"
        }
        prefs.edit().putString("saved_subject_presets", serialized).apply()
    }

    suspend fun isHandleTaken(handle: String): Boolean {
        val clean = handle.trim().lowercase().removePrefix("@")
        val myClean = _userProfileFlow.value.handle.trim().lowercase().removePrefix("@")
        if (clean == myClean && _userProfileFlow.value.isRegistered) return false

        val existingUser = firebaseService.searchUserByHandle(clean)
        return existingUser != null && existingUser.id != myUserId
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
