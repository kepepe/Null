package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

object GeminiScheduleParser {

    private const val PHOTO_PARSE_PROMPT = """
Ты — интеллектуальный ассистент студента. Твоя задача — внимательно изучить предоставленное фото или скриншот университетского расписания и извлечь все пары.

Верни СТРОГО валидный JSON массив объектов без какого-либо дополнительного текста, markdown-обёрток, пояснений или кавычек ```json.
Формат каждого объекта:
{
  "subjectTitle": "Название предмета",
  "classType": "LECTURE" | "PRACTICUM" | "LAB" | "SEMINAR",
  "professor": "ФИО преподавателя или пусто",
  "classroom": "Номер аудитории/кабинета или пусто",
  "dayOfWeek": "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY",
  "startTime": "HH:mm",
  "endTime": "HH:mm",
  "weekParity": "ALL" | "ODD" | "EVEN"
}

Правила:
1. "classType": LECTURE (лекция), PRACTICUM (практика, практическое занятие), LAB (лабораторная), SEMINAR (семинар).
2. "dayOfWeek": MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY.
3. Время должно быть строго в формате HH:mm (например, "08:30", "10:15", "12:00").
4. Если неделя верхняя/чирикатель/нечётная -> ODD; если нижняя/знаменатель/чётная -> EVEN; если каждую неделю -> ALL.
5. Если в расписании указаны номера пар (1 пара, 2 пара и т.д.) без явных часов, используй стандартные часы (1 пара: 08:30-10:00, 2 пара: 10:15-11:45, 3 пара: 12:00-13:30, 4 пара: 14:00-15:30, 5 пара: 15:45-17:15).
"""

    private const val TEXT_PARSE_PROMPT = """
Ты — интеллектуальный ассистент студента. Извлеки все пары и занятия из предоставленного свободного текста (заметка, сообщение из старостата, список).

Верни СТРОГО валидный JSON массив объектов без какого-либо дополнительного текста, без markdown обёртки.
Формат каждого объекта:
{
  "subjectTitle": "Название предмета",
  "classType": "LECTURE" | "PRACTICUM" | "LAB" | "SEMINAR",
  "professor": "ФИО преподавателя или пусто",
  "classroom": "Номер аудитории или пусто",
  "dayOfWeek": "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY",
  "startTime": "HH:mm",
  "endTime": "HH:mm",
  "weekParity": "ALL" | "ODD" | "EVEN"
}
"""

    suspend fun parseScheduleFromPhoto(bitmap: Bitmap): Result<List<ClassSlot>> = withContext(Dispatchers.Default) {
        val result = GeminiApiClient.generateRawContent(
            prompt = PHOTO_PARSE_PROMPT,
            bitmap = bitmap,
            systemInstruction = "You are a precise document and image OCR extractor for Russian university timetables. Output pure JSON only."
        )

        result.mapCatching { rawText ->
            val cleanJson = extractJsonArray(rawText)
            parseClassesFromJson(cleanJson)
        }
    }

    suspend fun parseScheduleFromText(rawInput: String): Result<List<ClassSlot>> = withContext(Dispatchers.Default) {
        val prompt = "$TEXT_PARSE_PROMPT\n\nТекст для распознавания:\n$rawInput"
        val result = GeminiApiClient.generateRawContent(
            prompt = prompt,
            systemInstruction = "You are a precise timetable parser. Output pure JSON only."
        )

        result.mapCatching { rawText ->
            val cleanJson = extractJsonArray(rawText)
            parseClassesFromJson(cleanJson)
        }
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

    private fun parseClassesFromJson(jsonArrayStr: String): List<ClassSlot> {
        val jsonArray = JSONArray(jsonArrayStr)
        val list = mutableListOf<ClassSlot>()
        val timeFormatter = DateTimeFormatter.ofPattern("H:m")

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val subjectTitle = obj.optString("subjectTitle", "").trim()
            if (subjectTitle.isBlank()) continue

            val typeStr = obj.optString("classType", "LECTURE").uppercase()
            val classType = when {
                typeStr.contains("LAB") -> ClassType.LAB
                typeStr.contains("PRACT") -> ClassType.PRACTICUM
                typeStr.contains("SEM") -> ClassType.SEMINAR
                else -> ClassType.LECTURE
            }

            val professor = obj.optString("professor", "").trim()
            val classroom = obj.optString("classroom", "").trim()

            val dayStr = obj.optString("dayOfWeek", "MONDAY").uppercase()
            val dayOfWeek = runCatching { DayOfWeek.valueOf(dayStr) }.getOrDefault(DayOfWeek.MONDAY)

            val startTimeStr = obj.optString("startTime", "09:00").trim()
            val endTimeStr = obj.optString("endTime", "10:30").trim()

            val startTime = parseTimeSafe(startTimeStr, LocalTime.of(9, 0))
            val endTime = parseTimeSafe(endTimeStr, startTime.plusMinutes(90))

            val parityStr = obj.optString("weekParity", "ALL").uppercase()
            val weekParity = when {
                parityStr.contains("ODD") -> WeekParity.ODD
                parityStr.contains("EVEN") -> WeekParity.EVEN
                else -> WeekParity.ALL
            }

            list.add(
                ClassSlot(
                    id = UUID.randomUUID().toString(),
                    subjectTitle = subjectTitle,
                    classType = classType,
                    professor = professor,
                    classroom = classroom,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    weekParity = weekParity,
                    allowedSkips = 3,
                    skippedCount = 0
                )
            )
        }
        return list
    }

    private fun parseTimeSafe(timeStr: String, fallback: LocalTime): LocalTime {
        return runCatching {
            val parts = timeStr.replace(".", ":").split(":")
            val h = parts[0].toInt()
            val m = if (parts.size > 1) parts[1].toInt() else 0
            LocalTime.of(h.coerceIn(0, 23), m.coerceIn(0, 59))
        }.getOrDefault(fallback)
    }
}
