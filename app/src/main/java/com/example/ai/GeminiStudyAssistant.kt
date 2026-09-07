package com.example.ai

import android.graphics.Bitmap
import com.example.model.SrsTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

object GeminiStudyAssistant {

    private const val NOTES_PARSE_PROMPT = """
Ты — умный помощник студента. Твоя задача — проанализировать текст заметки (или фото/скриншот записей, конспекта, чата группы, списка заданий) и извлечь структурированные учебные задания (СРС, ДЗ, лабораторные работы, дедлайны).

Доступные известные предметы студента:
%SUBJECTS%

Верни СТРОГО валидный JSON массив объектов без какого-либо markdown текста и без тройных кавычек ```json.
Формат каждого объекта:
{
  "subjectTitle": "Точное или наиболее подходящее название предмета из списка или из текста",
  "title": "Краткое и ёмкое название задания (например: 'Лабораторная работа №3: Сортировки')",
  "deadlineDaysFromNow": 7, // целое число дней от сегодняшнего дня до дедлайна, либо null если не указано
  "deadlineNote": "Текстовое пояснение срока (например: 'К следующей среде', 'До 23:59')",
  "notes": "Подробные требования, номера задач, ссылки или указания к выполнению"

"""

    suspend fun parseNotesToTasks(
        textInput: String,
        bitmap: Bitmap? = null,
        knownSubjects: List<String> = emptyList()
    ): Result<List<SrsTask>> = withContext(Dispatchers.Default) {
        val subjectsStr = if (knownSubjects.isNotEmpty()) {
            knownSubjects.joinToString(", ")
        } else {
            "Любые университетские дисциплины"
        }

        val prompt = NOTES_PARSE_PROMPT.replace("%SUBJECTS%", subjectsStr) +
                "\n\nСегодняшняя дата: ${LocalDate.now()}.\n" +
                if (textInput.isNotBlank()) "Заметка / Сообщение:\n$textInput" else "Проанализируй прикреплённое фото записей/заданий."

        val result = GeminiApiClient.generateRawContent(
            prompt = prompt,
            bitmap = bitmap,
            systemInstruction = "You are a university study assistant that extracts homework and assignments into JSON tasks."
        )

        result.mapCatching { rawText ->
            val cleanJson = extractJsonArray(rawText)
            parseTasksFromJson(cleanJson)
        }
    }

    suspend fun askStudyQuestion(
        question: String,
        subjectContext: String? = null,
        taskContext: String? = null
    ): Result<String> = withContext(Dispatchers.Default) {
        val prompt = buildString {
            append("Вопрос студента: $question\n\n")
            if (!subjectContext.isNullOrBlank()) {
                append("Контекст предмета: $subjectContext\n")
            }
            if (!taskContext.isNullOrBlank()) {
                append("Контекст задания: $taskContext\n")
            }
            append("\nДай структурированный, полезный и понятный ответ для студента: ключевые тезисы, формулы/примеры кода при необходимости, план решения или объяснение терминов простыми словами.")
        }

        GeminiApiClient.generateRawContent(
            prompt = prompt,
            systemInstruction = "Ты — дружелюбный и компетентный университетский тьютор и академический наставник. Объясняй материал глубоко, но наглядно и без лишней 'воды'. Отвечай на русском языке с красивой вёрсткой."
        )
    }

    suspend fun generateStudyPlan(
        taskTitle: String,
        subjectTitle: String,
        deadlineDays: Int?
    ): Result<String> = withContext(Dispatchers.Default) {
        val prompt = """
Составь пошаговый план подготовки и выполнения задания для студента:
Предмет: $subjectTitle
Задание: $taskTitle
Осталось дней: ${deadlineDays ?: 7}

Разбей план на 3-4 конкретных этапа с таймингом (например, Понимание теории -> Практическая реализация -> Оформление отчёта -> Подготовка к защите/сдаче). Включи короткие практические советы, на что обратить внимание преподавателя.
"""
        GeminiApiClient.generateRawContent(
            prompt = prompt,
            systemInstruction = "Ты — опытный методист и академический тьютор. Помогай студентам сдать работу вовремя без стресса."
        )
    }

    private fun extractJsonArray(text: String): String {
        var clean = text.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }

        val firstBracket = clean.indexOf('[')
        val lastBracket = clean.lastIndexOf(']')
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            clean = clean.substring(firstBracket, lastBracket + 1)
        }
        return clean
    }

    private fun parseTasksFromJson(jsonArrayStr: String): List<SrsTask> {
        val jsonArray = JSONArray(jsonArrayStr)
        val list = mutableListOf<SrsTask>()
        val today = LocalDate.now()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val subjectTitle = obj.optString("subjectTitle", "").trim().ifBlank { "Общие задания" }
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) continue

            val deadlineDays = if (obj.has("deadlineDaysFromNow") && !obj.isNull("deadlineDaysFromNow")) {
                obj.optInt("deadlineDaysFromNow", 7)
            } else null

            val deadlineDate = deadlineDays?.let { today.plusDays(it.toLong()) }
            val deadlineNote = obj.optString("deadlineNote", "").trim().ifBlank {
                deadlineDate?.let { "Дедлайн: $it" } ?: "Срок не указан"
            }
            val notes = obj.optString("notes", "").trim()

            list.add(
                SrsTask(
                    id = UUID.randomUUID().toString(),
                    subjectTitle = subjectTitle,
                    title = title,
                    deadlineDate = deadlineDate,
                    deadlineNote = deadlineNote,
                    isCompleted = false,
                    notes = notes,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        return list
    }

    suspend fun chatWithBot(
        chatHistory: String,
        newQuestion: String,
        bitmap: Bitmap? = null
    ): Result<String> = withContext(Dispatchers.Default) {
        val prompt = buildString {
            append("История переписки:\n")
            append(chatHistory)
            append("\n\nНовое сообщение студента: $newQuestion")
            append("\n\n(Учитывай историю при ответе, но отвечай только на новое сообщение. Если прикреплено фото, анализируй его вместе с вопросом.)")
        }

        GeminiApiClient.generateRawContent(
            prompt = prompt,
            bitmap = bitmap,
            systemInstruction = "Ты — дружелюбный и компетентный университетский ИИ-тьютор. ВАЖНОЕ ПРАВИЛО: В самом начале общения обязательно спроси у студента, по какому предмету или теме ему нужна помощь. НЕ давай подробных ответов, пока студент не укажет предмет. После того как предмет известен, помогай студенту, объясняй формулы, код или теорию наглядно и без лишней 'воды'. Отвечай на русском языке."
        )
    }
}
