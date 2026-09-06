package com.example.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class ClassType(val displayName: String) {
    LECTURE("Лекция"),
    SEMINAR("Семинар"),
    LAB("Лабораторная"),
    PRACTICUM("Практика")
}

enum class WeekParity(val displayName: String) {
    ALL("Все недели"),
    ODD("Нечётная"),
    EVEN("Чётная")
}

enum class WeekParityMode(val displayName: String) {
    AUTO("Автоматически"),
    ODD("Нечётная"),
    EVEN("Чётная")
}

enum class AppThemeMode(val displayName: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

data class UserProfile(
    val isRegistered: Boolean = false,
    val name: String = "",
    val handle: String = "",
    val university: String = "",
    val avatarUri: String? = null,
    val parityMode: WeekParityMode = WeekParityMode.AUTO,
    val notificationsEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
) {
    val initials: String
        get() {
            val parts = name.trim().split(" ")
            return when {
                parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                parts.isNotEmpty() && parts[0].isNotBlank() -> parts[0].take(2).uppercase()
                else -> "??"
            }
        }
}

data class ClassSlot(
    val id: String,
    val subjectTitle: String,
    val classType: ClassType,
    val professor: String,
    val classroom: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val weekParity: WeekParity = WeekParity.ALL,
    val colorHex: String? = null
) {
    val formattedTimeSpan: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        }
}

data class SubjectPreset(
    val title: String,
    val professor: String = "",
    val classroom: String = "",
    val classType: ClassType = ClassType.LECTURE,
    val colorHex: String = "#0061A4"
)

data class GroupChat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val memberFriendIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class AttendanceStatus(val displayName: String, val shortName: String, val colorHex: String) {
    ATTENDED("Присутствовал", "Был", "#2E7D32"),
    MISSED("Пропуск", "Пропуск", "#C62828"),
    EXCUSED("Уважительная", "Уваж.", "#E65100"),
    NOT_MARKED("Не отмечено", "—", "#757575")
}

data class AttendanceRecord(
    val classId: String,
    val dateString: String,
    val status: AttendanceStatus
)

data class BellSlot(
    val pairNumber: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val breakAfterMinutes: Int,
    val breakDescription: String = if (breakAfterMinutes > 0) "Перемена $breakAfterMinutes мин" else "Конец занятий"
) {
    val formattedTimeSpan: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        }
}

val standardBellSchedule = listOf(
    BellSlot(1, LocalTime.of(8, 0), LocalTime.of(9, 35), 15, "Перемена 15 минут"),
    BellSlot(2, LocalTime.of(9, 50), LocalTime.of(11, 25), 15, "Перемена 15 минут"),
    BellSlot(3, LocalTime.of(11, 40), LocalTime.of(13, 15), 45, "Большая перемена 45 минут (обед)"),
    BellSlot(4, LocalTime.of(14, 0), LocalTime.of(15, 35), 15, "Перемена 15 минут"),
    BellSlot(5, LocalTime.of(15, 50), LocalTime.of(17, 25), 15, "Перемена 15 минут"),
    BellSlot(6, LocalTime.of(17, 40), LocalTime.of(19, 15), 0, "Конец занятий")
)

val subjectColorPalette = listOf(
    "#0061A4" to "Синий",
    "#6750A4" to "Фиолетовый",
    "#2E7D32" to "Зелёный",
    "#E65100" to "Оранжевый",
    "#C2185B" to "Розовый",
    "#00838F" to "Морской",
    "#303F9F" to "Индиго",
    "#D32F2F" to "Красный",
    "#5D4037" to "Коричневый",
    "#455A64" to "Графит"
)

data class FriendUser(
    val id: String,
    val displayName: String,
    val handle: String,
    val avatarInitials: String,
    val avatarBgColorHex: String,
    val avatarUri: String? = null,
    val currentClass: String? = null,
    val currentRoom: String? = null,
    val classEndTime: String? = null,
    val isAttendingClass: Boolean = false,
    val schedule: List<ClassSlot> = emptyList()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val channelId: String, // friend id or "group_chat"
    val senderName: String,
    val senderHandle: String,
    val senderAvatarUri: String? = null,
    val senderAvatarBgColorHex: String = "#3B82F6",
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean = false
)

sealed interface CurrentClassStatus {
    data class ActiveClass(
        val currentSlot: ClassSlot,
        val remainingMinutes: Long,
        val nextSlot: ClassSlot?
    ) : CurrentClassStatus

    data class FreePeriod(
        val nextSlot: ClassSlot,
        val startsInMinutes: Long
    ) : CurrentClassStatus

    data object DoneForToday : CurrentClassStatus
    data object NoClassesToday : CurrentClassStatus
}
