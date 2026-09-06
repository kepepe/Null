package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import com.example.util.SilentModeHelper
import com.example.widget.ScheduleWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class ScheduleRepository(
    private val classDao: ClassDao,
    private val srsTaskDao: SrsTaskDao,
    private val context: Context
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE)

    // User profile state
    private val _userProfileFlow = MutableStateFlow(loadUserProfile())
    val userProfileFlow: Flow<UserProfile> = _userProfileFlow.asStateFlow()

    // Friends list (kept for compatibility, offline)
    private val _friendsFlow = MutableStateFlow<List<FriendUser>>(emptyList())
    val friendsFlow: Flow<List<FriendUser>> = _friendsFlow.asStateFlow()

    // Chat messages (kept for compatibility, offline)
    private val _chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessagesFlow: Flow<List<ChatMessage>> = _chatMessagesFlow.asStateFlow()

    // Group chats (kept for compatibility)
    private val _groupChatsFlow = MutableStateFlow<List<GroupChat>>(emptyList())
    val groupChatsFlow: Flow<List<GroupChat>> = _groupChatsFlow.asStateFlow()

    // Attendance map: key is "${classId}_${dateString}", value is AttendanceStatus
    private val _attendanceFlow = MutableStateFlow<Map<String, AttendanceStatus>>(loadAttendance())
    val attendanceFlow: StateFlow<Map<String, AttendanceStatus>> = _attendanceFlow.asStateFlow()

    fun getAttendanceStatus(classId: String, dateString: String): AttendanceStatus =
        _attendanceFlow.value["${classId}_$dateString"] ?: AttendanceStatus.NOT_MARKED

    // Subject Presets & Memory Suggestions for fast schedule adding
    private val _subjectPresetsFlow = MutableStateFlow<List<SubjectPreset>>(loadSubjectPresets())
    val subjectPresetsFlow: Flow<List<SubjectPreset>> = _subjectPresetsFlow.asStateFlow()

    fun observeAllClasses(): Flow<List<ClassSlot>> {
        return classDao.getAllClasses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAllClassesSync(): List<ClassSlot> {
        return classDao.getAllClassesSync().map { it.toDomain() }
    }

    suspend fun addOrUpdateClass(slot: ClassSlot) {
        classDao.insertOrUpdate(ClassEntity.fromDomain(slot))
        saveSubjectPreset(slot)
        ScheduleWidgetProvider.updateAllWidgets(context)
    }

    suspend fun deleteClass(id: String) {
        classDao.deleteById(id)
        ScheduleWidgetProvider.updateAllWidgets(context)
    }

    suspend fun clearAllClasses() {
        classDao.clearAll()
        ScheduleWidgetProvider.updateAllWidgets(context)
    }

    suspend fun incrementSkip(classId: String) {
        val entity = classDao.getClassById(classId) ?: return
        val updated = entity.copy(skippedCount = entity.skippedCount + 1)
        classDao.insertOrUpdate(updated)
    }

    suspend fun decrementSkip(classId: String) {
        val entity = classDao.getClassById(classId) ?: return
        val updated = entity.copy(skippedCount = (entity.skippedCount - 1).coerceAtLeast(0))
        classDao.insertOrUpdate(updated)
    }

    suspend fun updateAllowedSkips(classId: String, allowed: Int) {
        val entity = classDao.getClassById(classId) ?: return
        val updated = entity.copy(allowedSkips = allowed.coerceAtLeast(0))
        classDao.insertOrUpdate(updated)
    }

    private fun loadUserProfile(): UserProfile {
        val isRegistered = prefs.getBoolean("user_is_registered", false)
        val name = prefs.getString("user_name", "") ?: ""
        val handle = prefs.getString("user_handle", "") ?: ""
        val university = prefs.getString("user_university", "") ?: ""
        val avatarUri = prefs.getString("user_avatar_uri", null)
        val parityModeStr = prefs.getString("user_parity_mode", WeekParityMode.AUTO.name)
        val parityMode = runCatching { WeekParityMode.valueOf(parityModeStr!!) }.getOrDefault(WeekParityMode.AUTO)
        val notifEnabled = prefs.getBoolean("user_notifications_enabled", true)
        val themeModeStr = prefs.getString("user_theme_mode", AppThemeMode.SYSTEM.name)
        val themeMode = runCatching { AppThemeMode.valueOf(themeModeStr!!) }.getOrDefault(AppThemeMode.SYSTEM)
        val autoSilent = prefs.getBoolean("user_auto_silent_mode", false)
        val bellPresetStr = prefs.getString("user_bell_preset", BellSchedulePreset.STANDARD.name)
        val bellPreset = runCatching { BellSchedulePreset.valueOf(bellPresetStr!!) }.getOrDefault(BellSchedulePreset.STANDARD)
        val bellSlots = loadBellSlots()

        return UserProfile(
            isRegistered = isRegistered,
            name = name,
            handle = handle,
            university = university,
            avatarUri = avatarUri,
            parityMode = parityMode,
            notificationsEnabled = notifEnabled,
            themeMode = themeMode,
            autoSilentMode = autoSilent,
            bellPreset = bellPreset,
            bellSlots = bellSlots
        )
    }

    private fun loadBellSlots(): List<BellSlot> {
        val saved = prefs.getString("user_bell_slots_custom", null)
        if (saved.isNullOrBlank()) return standardBellSchedule
        return runCatching {
            saved.lines().filter { it.isNotBlank() }.map { line ->
                val parts = line.split(";")
                val pairNum = parts[0].toInt()
                val start = java.time.LocalTime.parse(parts[1])
                val end = java.time.LocalTime.parse(parts[2])
                val brk = parts.getOrNull(3)?.toIntOrNull() ?: 15
                BellSlot(
                    pairNumber = pairNum,
                    startTime = start,
                    endTime = end,
                    breakAfterMinutes = brk,
                    breakDescription = if (brk > 0) "Перемена $brk мин" else "Конец пар"
                )
            }
        }.getOrDefault(standardBellSchedule)
    }

    fun saveBellSlots(slots: List<BellSlot>) {
        val sorted = slots.sortedBy { it.pairNumber }
        val serialized = sorted.joinToString("\n") {
            "${it.pairNumber};${it.startTime};${it.endTime};${it.breakAfterMinutes}"
        }
        prefs.edit().putString("user_bell_slots_custom", serialized).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(bellSlots = sorted)
        ScheduleWidgetProvider.updateAllWidgets(context)
    }

    fun saveUserProfile(name: String, handle: String, university: String, avatarUri: String? = null): UserProfile {
        val cleanHandle = if (handle.startsWith("@")) handle else "@$handle"
        val updated = _userProfileFlow.value.copy(
            isRegistered = true,
            name = name.trim(),
            handle = cleanHandle.trim(),
            university = university.trim(),
            avatarUri = avatarUri ?: _userProfileFlow.value.avatarUri
        )
        prefs.edit()
            .putBoolean("user_is_registered", true)
            .putString("user_name", updated.name)
            .putString("user_handle", updated.handle)
            .putString("user_university", updated.university)
            .apply()

        if (updated.avatarUri != null) {
            prefs.edit().putString("user_avatar_uri", updated.avatarUri).apply()
        }

        _userProfileFlow.value = updated
        return updated
    }

    fun updateUserAvatar(uri: String?): String? {
        if (uri == null) {
            prefs.edit().remove("user_avatar_uri").apply()
            _userProfileFlow.value = _userProfileFlow.value.copy(avatarUri = null)
            return null
        }
        prefs.edit().putString("user_avatar_uri", uri).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(avatarUri = uri)
        return uri
    }

    fun setAutoSilentMode(enabled: Boolean) {
        prefs.edit().putBoolean("user_auto_silent_mode", enabled).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(autoSilentMode = enabled)
        if (!enabled) {
            SilentModeHelper.applyClassSilentMode(context, false)
        }
    }

    fun applyAutoSilentMode(status: CurrentClassStatus) {
        if (!_userProfileFlow.value.autoSilentMode) return
        val shouldBeSilent = status is CurrentClassStatus.ActiveClass
        SilentModeHelper.applyClassSilentMode(context, shouldBeSilent)
    }

    fun setWeekParityMode(mode: WeekParityMode) {
        prefs.edit().putString("user_parity_mode", mode.name).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(parityMode = mode)
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("user_theme_mode", mode.name).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(themeMode = mode)
    }

    fun setBellPreset(preset: BellSchedulePreset) {
        prefs.edit().putString("user_bell_preset", preset.name).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(bellPreset = preset)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("user_notifications_enabled", enabled).apply()
        _userProfileFlow.value = _userProfileFlow.value.copy(notificationsEnabled = enabled)
    }

    suspend fun addFriendByHandle(query: String): Pair<Boolean, String> {
        return Pair(false, "Функция друзей отключена (офлайн-режим)")
    }

    fun removeFriend(friendId: String) {
        val updated = _friendsFlow.value.filterNot { it.id == friendId }
        _friendsFlow.value = updated
    }

    fun setActiveChannel(channelId: String) {}

    fun sendMessage(channelId: String, text: String) {}

    fun createGroupChat(name: String, memberHandles: List<String>) {}

    fun deleteGroupChat(groupId: String) {}

    fun clearChatMessages() {
        _chatMessagesFlow.value = emptyList()
    }

    fun pushMyProfileToCloud(currentStatus: CurrentClassStatus? = null) {}

    private fun loadSubjectPresets(): List<SubjectPreset> {
        val raw = prefs.getString("saved_subject_presets", null) ?: return defaultSubjectPresets()
        val list = raw.split(";;;").mapNotNull { line ->
            val p = line.split("|||")
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

    // --- SRS (Independent Work / Homework) Methods ---
    fun observeAllSrsTasks(): Flow<List<SrsTask>> {
        return srsTaskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun addOrUpdateSrsTask(task: SrsTask) {
        srsTaskDao.insertOrUpdate(SrsTaskEntity.fromDomain(task))
    }

    suspend fun toggleSrsTaskCompleted(id: String, isCompleted: Boolean) {
        srsTaskDao.updateCompletion(id, isCompleted)
    }

    suspend fun deleteSrsTask(id: String) {
        srsTaskDao.deleteById(id)
    }

    private fun defaultSubjectPresets(): List<SubjectPreset> = emptyList()

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

    suspend fun isHandleTaken(handle: String): Boolean = false

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
                colorHex = "#0061A4",
                allowedSkips = 3,
                skippedCount = 1
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
                colorHex = "#6750A4",
                allowedSkips = 4,
                skippedCount = 0
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
                colorHex = "#006874",
                allowedSkips = 2,
                skippedCount = 0
            )
        )
        classDao.insertAll(sampleList.map { ClassEntity.fromDomain(it) })
        ScheduleWidgetProvider.updateAllWidgets(context)
    }
}
