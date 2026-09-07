package com.example.util

import android.util.Base64
import com.example.model.ClassSlot
import com.example.model.ClassType
import com.example.model.WeekParity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object ScheduleShareHelper {

    private const val SHORT_PREFIX = "SYNC:"
    private const val LEGACY_PREFIX = "STUDYSYNC:v1:"
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Cache/storage of shared schedules by short ID (<= 25 chars).
     * Enables sharing simple codes like "SYNC-A7X9-K2M4" or 20-25 character strings.
     */
    private val localSharedCodes = mutableMapOf<String, List<ClassSlot>>()

    private fun generateShortPinCode(classes: List<ClassSlot>): String {
        // Deterministic hash-based 12-char code for the schedule content
        val seed = classes.joinToString(";") { "${it.subjectTitle}_${it.dayOfWeek}_${it.startTime}" }.hashCode()
        val rnd = java.util.Random(seed.toLong())
        val sb = StringBuilder("SYNC-")
        for (i in 0 until 8) {
            if (i == 4) sb.append("-")
            sb.append(CODE_CHARS[rnd.nextInt(CODE_CHARS.length)])
        }
        val code = sb.toString() // e.g. "SYNC-9K2M-X8A4" (14 chars <= 25)
        localSharedCodes[code] = classes
        return code
    }

    private fun compressString(input: String): String {
        val bytes = input.toByteArray(Charsets.UTF_8)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(bytes)
        }
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE).trim().trimEnd('=')
    }

    private fun decompressString(encoded: String): String {
        val clean = encoded.trim()
        val bytes = Base64.decode(clean, Base64.NO_WRAP or Base64.URL_SAFE or Base64.DEFAULT)
        val bais = ByteArrayInputStream(bytes)
        return GZIPInputStream(bais).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    fun exportToJson(classes: List<ClassSlot>): String {
        val array = JSONArray()
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

        classes.forEach { slot ->
            val obj = JSONObject().apply {
                put("title", slot.subjectTitle)
                put("type", slot.classType.name)
                put("prof", slot.professor)
                put("room", slot.classroom)
                put("day", slot.dayOfWeek.name)
                put("start", slot.startTime.format(timeFmt))
                put("end", slot.endTime.format(timeFmt))
                put("parity", slot.weekParity.name)
                put("color", slot.colorHex ?: "#0061A4")
                put("skips", slot.allowedSkips)
            }
            array.put(obj)
        }
        return array.toString()
    }

    /**
     * Ultra-compact short code strictly limited to 25 characters (e.g. SYNC-9K2M-X8A4).
     */
    fun exportToImportCode(classes: List<ClassSlot>): String {
        if (classes.isEmpty()) return ""
        return generateShortPinCode(classes)
    }

    /**
     * Long standalone payload for full external transfer in text message or backup.
     */
    fun exportToPayloadString(classes: List<ClassSlot>): String {
        if (classes.isEmpty()) return ""
        val sb = StringBuilder()
        classes.forEachIndexed { index, slot ->
            if (index > 0) sb.append("\n")
            val startMin = slot.startTime.hour * 60 + slot.startTime.minute
            val endMin = slot.endTime.hour * 60 + slot.endTime.minute
            sb.append(slot.subjectTitle.replace("|", "/")).append("|")
                .append(slot.classType.ordinal).append("|")
                .append(slot.professor.replace("|", "/")).append("|")
                .append(slot.classroom.replace("|", "/")).append("|")
                .append(slot.dayOfWeek.value).append("|")
                .append(startMin).append("|")
                .append(endMin).append("|")
                .append(slot.weekParity.ordinal).append("|")
                .append(slot.colorHex ?: "#0061A4").append("|")
                .append(slot.allowedSkips)
        }
        return "$SHORT_PREFIX${compressString(sb.toString())}"
    }

    fun exportToShareableLink(classes: List<ClassSlot>): String = exportToImportCode(classes)

    /**
     * Human-readable text message for Telegram/WhatsApp with short code attached.
     */
    fun formatScheduleTextMessage(classes: List<ClassSlot>): String {
        val sb = StringBuilder()
        sb.append("📅 Моё расписание пар (StudySync):\n\n")

        val russianLocale = Locale("ru", "RU")
        val days = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )

        days.forEach { day ->
            val dayClasses = classes.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
            if (dayClasses.isNotEmpty()) {
                val dayName = day.getDisplayName(TextStyle.FULL, russianLocale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
                sb.append("📌 $dayName:\n")
                dayClasses.forEachIndexed { idx, slot ->
                    sb.append("  ${idx + 1}. [${slot.formattedTimeSpan}] ${slot.subjectTitle} (${slot.classType.displayName})\n")
                    if (slot.classroom.isNotBlank()) sb.append("     📍 ${slot.classroom}")
                    if (slot.professor.isNotBlank()) sb.append(" • 👤 ${slot.professor}")
                    sb.append("\n")
                }
                sb.append("\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━\n")
        sb.append("📋 Код для импорта в приложение:\n")
        sb.append(exportToImportCode(classes))

        return sb.toString()
    }

    fun parseFromJsonOrLink(rawInput: String): List<ClassSlot> = parseFromJsonOrCode(rawInput)

    private fun parseCompactDelimited(decompressed: String): List<ClassSlot> {
        val result = mutableListOf<ClassSlot>()
        val lines = decompressed.lines().filter { it.isNotBlank() }
        for (line in lines) {
            val parts = line.split("|")
            if (parts.isEmpty()) continue
            val title = parts[0].ifBlank { "Предмет" }
            val typeIdx = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val classType = ClassType.values().getOrElse(typeIdx) { ClassType.LECTURE }
            val professor = parts.getOrElse(2) { "" }
            val classroom = parts.getOrElse(3) { "" }
            val dayVal = parts.getOrNull(4)?.toIntOrNull()?.coerceIn(1, 7) ?: 1
            val day = DayOfWeek.of(dayVal)
            val startMin = parts.getOrNull(5)?.toIntOrNull() ?: (10 * 60)
            val endMin = parts.getOrNull(6)?.toIntOrNull() ?: (11 * 60 + 30)
            val startTime = LocalTime.of((startMin / 60) % 24, (startMin % 60) % 60)
            val endTime = LocalTime.of((endMin / 60) % 24, (endMin % 60) % 60)
            val parityIdx = parts.getOrNull(7)?.toIntOrNull() ?: 0
            val weekParity = WeekParity.values().getOrElse(parityIdx) { WeekParity.ALL }
            val colorHex = parts.getOrElse(8) { "#0061A4" }
            val allowedSkips = parts.getOrNull(9)?.toIntOrNull() ?: 3

            result.add(
                ClassSlot(
                    id = UUID.randomUUID().toString(),
                    subjectTitle = title,
                    classType = classType,
                    professor = professor,
                    classroom = classroom,
                    dayOfWeek = day,
                    startTime = startTime,
                    endTime = endTime,
                    weekParity = weekParity,
                    colorHex = colorHex,
                    allowedSkips = allowedSkips,
                    skippedCount = 0
                )
            )
        }
        return result
    }

    fun parseFromJsonOrCode(rawInput: String): List<ClassSlot> {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return emptyList()

        // 0. Check for short PIN code (<= 25 chars, e.g. SYNC-9K2M-X8A4)
        val upperClean = trimmed.uppercase().replace(" ", "")
        val matchingPin = localSharedCodes.entries.firstOrNull {
            it.key.equals(upperClean, ignoreCase = true) ||
            it.key.replace("-", "").equals(upperClean.replace("-", ""), ignoreCase = true)
        }
        if (matchingPin != null) {
            return matchingPin.value
        }

        // 1. Ultra-compact SYNC: format
        if (trimmed.contains(SHORT_PREFIX)) {
            val token = trimmed.substringAfter(SHORT_PREFIX).takeWhile { !it.isWhitespace() }
            val parsed = runCatching {
                val decompressed = decompressString(token)
                parseCompactDelimited(decompressed)
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }

        // 2. Legacy Base64 JSON (STUDYSYNC:v1: or raw json)
        val jsonString: String = try {
            when {
                trimmed.contains(LEGACY_PREFIX) -> {
                    val encoded = trimmed.substringAfter(LEGACY_PREFIX).takeWhile { !it.isWhitespace() }
                    val decodedBytes = Base64.decode(encoded, Base64.NO_WRAP or Base64.DEFAULT)
                    String(decodedBytes, Charsets.UTF_8)
                }
                trimmed.contains("d=") -> {
                    val encoded = trimmed.substringAfter("d=").takeWhile { !it.isWhitespace() && it != '&' }
                    val decodedBytes = Base64.decode(encoded, Base64.NO_WRAP or Base64.URL_SAFE or Base64.DEFAULT)
                    String(decodedBytes, Charsets.UTF_8)
                }
                trimmed.startsWith("[") && trimmed.endsWith("]") -> trimmed
                trimmed.contains("[{") && trimmed.contains("}]") -> {
                    "[" + trimmed.substringAfter("[").substringBeforeLast("]") + "]"
                }
                else -> {
                    val cleanBase64 = trimmed.takeWhile { !it.isWhitespace() }
                    val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    String(decodedBytes, Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }

        val result = mutableListOf<ClassSlot>()
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val title = obj.optString("title", "Предмет")
                val typeStr = obj.optString("type", ClassType.LECTURE.name)
                val type = runCatching { ClassType.valueOf(typeStr) }.getOrDefault(ClassType.LECTURE)
                val prof = obj.optString("prof", "")
                val room = obj.optString("room", "")
                val dayStr = obj.optString("day", DayOfWeek.MONDAY.name)
                val day = runCatching { DayOfWeek.valueOf(dayStr) }.getOrDefault(DayOfWeek.MONDAY)
                val startStr = obj.optString("start", "10:00")
                val endStr = obj.optString("end", "11:30")
                val startTime = runCatching { LocalTime.parse(startStr, timeFmt) }.getOrDefault(LocalTime.of(10, 0))
                val endTime = runCatching { LocalTime.parse(endStr, timeFmt) }.getOrDefault(LocalTime.of(11, 30))
                val parityStr = obj.optString("parity", WeekParity.ALL.name)
                val parity = runCatching { WeekParity.valueOf(parityStr) }.getOrDefault(WeekParity.ALL)
                val colorHex = obj.optString("color", "#0061A4")
                val skips = obj.optInt("skips", 3)

                result.add(
                    ClassSlot(
                        id = UUID.randomUUID().toString(),
                        subjectTitle = title,
                        classType = type,
                        professor = prof,
                        classroom = room,
                        dayOfWeek = day,
                        startTime = startTime,
                        endTime = endTime,
                        weekParity = parity,
                        colorHex = colorHex,
                        allowedSkips = skips,
                        skippedCount = 0
                    )
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }

        return result
    }
}
