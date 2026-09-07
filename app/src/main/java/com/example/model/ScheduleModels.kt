package com.example.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter


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

enum class BellSchedulePreset(
    val title: String,
    val description: String,
    val slots: List<BellSlot>
) {
    STANDARD(
        title = "Стандартная (с 8:30)",
        description = "8:30–10:00, 10:15–11:45, 12:00–13:30...",
        slots = listOf(
            BellSlot(1, LocalTime.of(8, 30), LocalTime.of(10, 0), 15, "Перемена 15 мин"),
            BellSlot(2, LocalTime.of(10, 15), LocalTime.of(11, 45), 15, "Перемена 15 мин"),
            BellSlot(3, LocalTime.of(12, 0), LocalTime.of(13, 30), 30, "Большая перемена (обед)"),
            BellSlot(4, LocalTime.of(14, 0), LocalTime.of(15, 30), 15, "Перемена 15 мин"),
            BellSlot(5, LocalTime.of(15, 45), LocalTime.of(17, 15), 15, "Перемена 15 мин"),
            BellSlot(6, LocalTime.of(17, 30), LocalTime.of(19, 0), 0, "Конец пар")
        )
    ),
    CLASSIC_8AM(
        title = "Ранняя (с 8:00)",
        description = "8:00–9:35, 9:50–11:25, 11:40–13:15...",
        slots = listOf(
            BellSlot(1, LocalTime.of(8, 0), LocalTime.of(9, 35), 15, "Перемена 15 мин"),
            BellSlot(2, LocalTime.of(9, 50), LocalTime.of(11, 25), 15, "Перемена 15 мин"),
            BellSlot(3, LocalTime.of(11, 40), LocalTime.of(13, 15), 45, "Большая перемена 45 мин"),
            BellSlot(4, LocalTime.of(14, 0), LocalTime.of(15, 35), 15, "Перемена 15 мин"),
            BellSlot(5, LocalTime.of(15, 50), LocalTime.of(17, 25), 15, "Перемена 15 мин"),
            BellSlot(6, LocalTime.of(17, 40), LocalTime.of(19, 15), 0, "Конец пар")
        )
    ),
    LATE_9AM(
        title = "Поздняя (с 9:00)",
        description = "9:00–10:30, 10:45–12:15, 13:00–14:30...",
        slots = listOf(
            BellSlot(1, LocalTime.of(9, 0), LocalTime.of(10, 30), 15, "Перемена 15 мин"),
            BellSlot(2, LocalTime.of(10, 45), LocalTime.of(12, 15), 45, "Обеденный перерыв 45 мин"),
            BellSlot(3, LocalTime.of(13, 0), LocalTime.of(14, 30), 15, "Перемена 15 мин"),
            BellSlot(4, LocalTime.of(14, 45), LocalTime.of(16, 15), 15, "Перемена 15 мин"),
            BellSlot(5, LocalTime.of(16, 30), LocalTime.of(18, 0), 15, "Перемена 15 мин"),
            BellSlot(6, LocalTime.of(18, 15), LocalTime.of(19, 45), 0, "Конец пар")
        )
    )
}

data class UserProfile(
    val isRegistered: Boolean = false,
    val name: String = "",
    val handle: String = "",
    val university: String = "",
    val avatarUri: String? = null,
    val parityMode: WeekParityMode = WeekParityMode.AUTO,
    val notificationsEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val bellPreset: BellSchedulePreset = BellSchedulePreset.STANDARD,
    val bellSlots: List<BellSlot> = standardBellSchedule
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
    val classType: String = "Лекция",
    val professor: String,
    val classroom: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val weekParity: WeekParity = WeekParity.ALL,
    val colorHex: String? = null,
    val allowedSkips: Int = 3,
    val skippedCount: Int = 0
) {
    val remainingSkips: Int
        get() = (allowedSkips - skippedCount).coerceAtLeast(0)

    val isSkipLimitExceeded: Boolean
        get() = skippedCount >= allowedSkips

    val formattedTimeSpan: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${startTime.format(formatter)} - ${endTime.format(formatter)}"
        }
}

data class ScheduleWindow(
    val previousSlot: ClassSlot,
    val nextSlot: ClassSlot,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val durationMinutes: Long
) {
    val formattedTimeSpan: String
        get() {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            return "${startTime.format(formatter)} — ${endTime.format(formatter)}"
        }

    val formattedDuration: String
        get() {
            val hours = durationMinutes / 60
            val mins = durationMinutes % 60
            return if (hours > 0) "${hours} ч ${mins} мин" else "${mins} мин"
        }
}

data class SubjectPreset(
    val title: String,
    val professor: String = "",
    val classroom: String = "",
    val classType: String = "Лекция",
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
        val remainingSeconds: Long = 0,
        val elapsedMinutes: Long = 0,
        val totalDurationMinutes: Long = 90,
        val progressPercent: Int = 0,
        val nextSlot: ClassSlot? = null
    ) : CurrentClassStatus {
        val formattedTimer: String
            get() {
                val totalSec = remainingSeconds.coerceAtLeast(0)
                val hours = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                return if (hours > 0) {
                    String.format(java.util.Locale.US, "%d:%02d:%02d", hours, mins, secs)
                } else {
                    String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                }
            }

        val formattedHumanTime: String
            get() {
                val totalSec = remainingSeconds.coerceAtLeast(0)
                val hours = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                return when {
                    hours > 0 && mins > 0 -> "$hours ч $mins мин"
                    hours > 0 -> "$hours ч"
                    mins > 0 -> "$mins мин"
                    else -> "${totalSec} с"
                }
            }
    }

    data class FreePeriod(
        val nextSlot: ClassSlot,
        val startsInMinutes: Long,
        val startsInSeconds: Long = 0,
        val isBeforeFirstClass: Boolean = false
    ) : CurrentClassStatus {
        val formattedTimer: String
            get() {
                val totalSec = startsInSeconds.coerceAtLeast(0)
                val hours = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                val secs = totalSec % 60
                return if (hours > 0) {
                    String.format(java.util.Locale.US, "%d:%02d:%02d", hours, mins, secs)
                } else {
                    String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
                }
            }

        val formattedHumanTime: String
            get() {
                val totalSec = startsInSeconds.coerceAtLeast(0)
                val hours = totalSec / 3600
                val mins = (totalSec % 3600) / 60
                return when {
                    hours > 0 && mins > 0 -> "$hours ч $mins мин"
                    hours > 0 -> "$hours ч"
                    mins > 0 -> "$mins мин"
                    else -> "${totalSec} с"
                }
            }
    }

    data object DoneForToday : CurrentClassStatus
    data object NoClassesToday : CurrentClassStatus
}
