package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object AvatarStorageHelper {

    fun saveAvatarLocally(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val avatarFile = File(context.filesDir, "user_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(avatarFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            avatarFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
