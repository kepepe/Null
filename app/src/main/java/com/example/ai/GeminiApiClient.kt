package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.model.ClassSlot

import com.example.model.SrsTask
import com.example.model.WeekParity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

object GeminiApiClient {

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    fun hasApiKey(): Boolean {
        val key = getApiKey()
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateRawContent(
        prompt: String,
        bitmap: Bitmap? = null,
        systemInstruction: String? = null,
        temperature: Float = 0.2f
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API ключ не настроен. Добавьте GEMINI_API_KEY в панели Secrets AI Studio или введите его в профиле.")
            )
        }

        try {
            val rootJson = JSONObject()

            // System Instruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysPart = JSONObject().put("text", systemInstruction)
                val sysContent = JSONObject().put("parts", JSONArray().put(sysPart))
                rootJson.put("systemInstruction", sysContent)
            }

            // Generation config
            val genConfig = JSONObject()
                .put("temperature", temperature)
                .put("topP", 0.95)
            rootJson.put("generationConfig", genConfig)

            // Content Parts
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            if (bitmap != null) {
                val base64Image = bitmapToBase64(bitmap)
                val inlineData = JSONObject()
                    .put("mimeType", "image/jpeg")
                    .put("data", base64Image)
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val contentObj = JSONObject().put("parts", partsArray)
            rootJson.put("contents", JSONArray().put(contentObj))

            val requestBody = rootJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseBody, response.code)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val candidateContent = firstCandidate?.optJSONObject("content")
            val candidateParts = candidateContent?.optJSONArray("parts")

            val textResult = buildString {
                if (candidateParts != null) {
                    for (i in 0 until candidateParts.length()) {
                        val part = candidateParts.optJSONObject(i)
                        val t = part?.optString("text", "") ?: ""
                        append(t)
                    }
                }
            }

            if (textResult.isBlank()) {
                Result.failure(Exception("Gemini вернул пустой ответ"))
            } else {
                Result.success(textResult)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return runCatching {
            val errObj = JSONObject(responseBody).optJSONObject("error")
            val message = errObj?.optString("message") ?: "HTTP $statusCode"
            "Ошибка Gemini ($statusCode): $message"
        }.getOrDefault("Ошибка связи с сервером Gemini (Код $statusCode)")
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Resize down if too big to avoid memory issues and payload limits
        val maxDim = 1280
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newW = if (ratio > 1f) maxDim else (maxDim * ratio).toInt()
            val newH = if (ratio > 1f) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }
}
