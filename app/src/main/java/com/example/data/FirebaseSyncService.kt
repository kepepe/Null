package com.example.data

import android.util.Log
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class FirebaseSyncService {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val usersCollection get() = firestore.collection("users")
    private val chatsCollection get() = firestore.collection("chats")
    private val groupChatsCollection get() = firestore.collection("group_chats")

    // --- User Profile & Presence in Cloud Firestore ---

    suspend fun publishUserProfile(
        userId: String,
        profile: UserProfile,
        currentClasses: List<ClassSlot>,
        currentStatus: CurrentClassStatus
    ) {
        if (profile.handle.isBlank()) return

        val normalizedHandle = if (profile.handle.startsWith("@")) {
            profile.handle.lowercase()
        } else {
            "@" + profile.handle.lowercase()
        }

        val serializedSchedule = currentClasses.map { slot ->
            mapOf(
                "id" to slot.id,
                "subjectTitle" to slot.subjectTitle,
                "classType" to slot.classType.name,
                "professor" to slot.professor,
                "classroom" to slot.classroom,
                "dayOfWeek" to slot.dayOfWeek.name,
                "startTime" to slot.startTime.toString(),
                "endTime" to slot.endTime.toString(),
                "weekParity" to slot.weekParity.name,
                "colorHex" to (slot.colorHex ?: "#0061A4")
            )
        }

        val (currentClassTitle, currentRoom, endTimeStr, isAttending) = when (currentStatus) {
            is CurrentClassStatus.ActiveClass -> {
                val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
                Quadruple(
                    currentStatus.currentSlot.subjectTitle,
                    currentStatus.currentSlot.classroom,
                    currentStatus.currentSlot.endTime.format(timeFmt),
                    true
                )
            }
            else -> Quadruple(null, null, null, false)
        }

        val userData = hashMapOf<String, Any?>(
            "id" to userId,
            "displayName" to profile.name.ifBlank { "Студент" },
            "handle" to normalizedHandle,
            "university" to profile.university,
            "avatarInitials" to profile.initials,
            "avatarBgColorHex" to "#0061A4",
            "currentClass" to currentClassTitle,
            "currentRoom" to currentRoom,
            "classEndTime" to endTimeStr,
            "isAttendingClass" to isAttending,
            "updatedAt" to System.currentTimeMillis(),
            "schedule" to serializedSchedule
        )

        try {
            usersCollection.document(userId).set(userData).await()
            Log.d("FirebaseSyncService", "Profile synced to Firestore for $normalizedHandle")
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to sync profile: ${e.message}")
        }
    }

    suspend fun searchUserByHandle(query: String): FriendUser? {
        val cleanQuery = query.trim().lowercase()
        val normalized = if (cleanQuery.startsWith("@")) cleanQuery else "@$cleanQuery"

        return try {
            val snapshot = usersCollection
                .whereEqualTo("handle", normalized)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                parseDocToFriend(doc.id, doc.data ?: emptyMap())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Error searching user $normalized: ${e.message}")
            null
        }
    }

    fun observeFriend(friendId: String): Flow<FriendUser?> = callbackFlow {
        val listener: ListenerRegistration = usersCollection.document(friendId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseSyncService", "Error observing friend $friendId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val friend = parseDocToFriend(snapshot.id, snapshot.data ?: emptyMap())
                    trySend(friend)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    // --- Realtime Chat Messages ---

    fun observeMessages(channelId: String, currentUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener: ListenerRegistration = chatsCollection.document(channelId)
            .collection("messages")
            .orderBy("timestampEpoch", Query.Direction.ASCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseSyncService", "Error listening to messages in $channelId: ${error.message}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val senderId = data["senderId"] as? String ?: ""
                    val isFromMe = (senderId == currentUserId)

                    ChatMessage(
                        id = doc.id,
                        channelId = channelId,
                        senderName = data["senderName"] as? String ?: "Студент",
                        senderHandle = data["senderHandle"] as? String ?: "@anon",
                        senderAvatarUri = data["senderAvatarUri"] as? String,
                        senderAvatarBgColorHex = data["senderAvatarBgColorHex"] as? String ?: "#0061A4",
                        text = data["text"] as? String ?: "",
                        timestamp = data["timestampText"] as? String ?: "",
                        isFromMe = isFromMe
                    )
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun sendMessage(
        channelId: String,
        text: String,
        senderId: String,
        senderName: String,
        senderHandle: String,
        senderAvatarUri: String?,
        timestampText: String
    ) {
        if (channelId.isBlank() || text.isBlank()) return

        val msgData = hashMapOf<String, Any?>(
            "senderId" to senderId,
            "senderName" to senderName,
            "senderHandle" to senderHandle,
            "senderAvatarUri" to senderAvatarUri,
            "senderAvatarBgColorHex" to "#0061A4",
            "text" to text.trim(),
            "timestampText" to timestampText,
            "timestampEpoch" to System.currentTimeMillis()
        )

        try {
            chatsCollection.document(channelId).collection("messages").add(msgData).await()
            chatsCollection.document(channelId).set(
                mapOf(
                    "lastMessage" to text.trim(),
                    "lastSender" to senderName,
                    "lastUpdated" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to send message to $channelId: ${e.message}")
        }
    }

    // --- Helper Parsing ---

    @Suppress("UNCHECKED_CAST")
    private fun parseDocToFriend(docId: String, data: Map<String, Any?>): FriendUser {
        val rawSchedule = data["schedule"] as? List<Map<String, Any?>> ?: emptyList()
        val parsedSchedule = rawSchedule.mapNotNull { item ->
            try {
                ClassSlot(
                    id = item["id"] as? String ?: java.util.UUID.randomUUID().toString(),
                    subjectTitle = item["subjectTitle"] as? String ?: "Предмет",
                    classType = runCatching { ClassType.valueOf(item["classType"] as String) }.getOrDefault(ClassType.LECTURE),
                    professor = item["professor"] as? String ?: "",
                    classroom = item["classroom"] as? String ?: "",
                    dayOfWeek = runCatching { DayOfWeek.valueOf(item["dayOfWeek"] as String) }.getOrDefault(DayOfWeek.MONDAY),
                    startTime = runCatching { LocalTime.parse(item["startTime"] as String) }.getOrDefault(LocalTime.of(8, 0)),
                    endTime = runCatching { LocalTime.parse(item["endTime"] as String) }.getOrDefault(LocalTime.of(9, 35)),
                    weekParity = runCatching { WeekParity.valueOf(item["weekParity"] as String) }.getOrDefault(WeekParity.ALL),
                    colorHex = item["colorHex"] as? String ?: "#0061A4"
                )
            } catch (e: Exception) {
                null
            }
        }

        return FriendUser(
            id = docId,
            displayName = data["displayName"] as? String ?: "Студент",
            handle = data["handle"] as? String ?: "@student",
            avatarInitials = data["avatarInitials"] as? String ?: "СТ",
            avatarBgColorHex = data["avatarBgColorHex"] as? String ?: "#0061A4",
            avatarUri = null,
            currentClass = data["currentClass"] as? String,
            currentRoom = data["currentRoom"] as? String,
            classEndTime = data["classEndTime"] as? String,
            isAttendingClass = data["isAttendingClass"] as? Boolean ?: false,
            schedule = parsedSchedule
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
