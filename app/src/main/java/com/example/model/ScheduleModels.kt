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
