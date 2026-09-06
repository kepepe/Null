package com.example.model

import java.time.LocalDate

data class SrsTask(
    val id: String,
    val subjectTitle: String,
    val title: String,
    val deadlineDate: LocalDate? = null,
    val deadlineNote: String = "",
    val isCompleted: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
