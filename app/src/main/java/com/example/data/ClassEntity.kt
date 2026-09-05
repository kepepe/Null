package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ClassSlot
import com.example.model.ClassType
import com.example.model.WeekParity
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "schedule_classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val subjectTitle: String,
    val classType: String,
    val professor: String,
    val classroom: String,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val weekParity: String,
    val colorHex: String?
) {
    fun toDomain(): ClassSlot {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return ClassSlot(
            id = id,
            subjectTitle = subjectTitle,
            classType = runCatching { ClassType.valueOf(classType) }.getOrDefault(ClassType.LECTURE),
            professor = professor,
            classroom = classroom,
            dayOfWeek = DayOfWeek.of(dayOfWeek.coerceIn(1, 7)),
            startTime = LocalTime.parse(startTime, timeFormatter),
            endTime = LocalTime.parse(endTime, timeFormatter),
            weekParity = runCatching { WeekParity.valueOf(weekParity) }.getOrDefault(WeekParity.ALL),
            colorHex = colorHex
        )
    }

    companion object {
        fun fromDomain(slot: ClassSlot): ClassEntity {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            return ClassEntity(
                id = slot.id,
                subjectTitle = slot.subjectTitle,
                classType = slot.classType.name,
                professor = slot.professor,
                classroom = slot.classroom,
                dayOfWeek = slot.dayOfWeek.value,
                startTime = slot.startTime.format(timeFormatter),
                endTime = slot.endTime.format(timeFormatter),
                weekParity = slot.weekParity.name,
                colorHex = slot.colorHex
            )
        }
    }
}
