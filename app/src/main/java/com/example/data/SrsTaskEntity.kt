package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.SrsTask
import java.time.LocalDate

@Entity(tableName = "srs_tasks")
data class SrsTaskEntity(
    @PrimaryKey
    val id: String,
    val subjectTitle: String,
    val title: String,
    val deadlineDate: String?, // ISO string e.g. "2026-09-15"
    val deadlineNote: String,
    val isCompleted: Boolean,
    val notes: String,
    val createdAt: Long
) {
    fun toDomain(): SrsTask {
        val parsedDate = deadlineDate?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        return SrsTask(
            id = id,
            subjectTitle = subjectTitle,
            title = title,
            deadlineDate = parsedDate,
            deadlineNote = deadlineNote,
            isCompleted = isCompleted,
            notes = notes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(task: SrsTask): SrsTaskEntity {
            return SrsTaskEntity(
                id = task.id,
                subjectTitle = task.subjectTitle,
                title = task.title,
                deadlineDate = task.deadlineDate?.toString(),
                deadlineNote = task.deadlineNote,
                isCompleted = task.isCompleted,
                notes = task.notes,
                createdAt = task.createdAt
            )
        }
    }
}
