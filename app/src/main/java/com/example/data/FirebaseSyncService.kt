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

        val cleanHandle = profile.handle.trim().lowercase().removePrefix("@")
        val normalizedHandle = "@$cleanHandle"

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
            "cleanHandle" to cleanHandle,
            "university" to profile.university,
            "avatarInitials" to profile.initials,
            "avatarBgColorHex" to "#0061A4",
            "avatarUri" to profile.avatarUri,
            "currentClass" to currentClassTitle,
            "currentRoom" to currentRoom,
            "classEndTime" to endTimeStr,
            "isAttendingClass" to isAttending,
            "updatedAt" to System.currentTimeMillis(),
            "schedule" to serializedSchedule
        )

        try {
            // Save by clean handle as direct primary key (e.g. users/kirill_v)
            usersCollection.document(cleanHandle).set(userData).await()
            // Also save by @cleanHandle and by UUID to ensure backwards compatibility
            usersCollection.document(normalizedHandle).set(userData).await()
            usersCollection.document(userId).set(userData).await()
            Log.d("FirebaseSyncService", "Profile synced to Firestore for $normalizedHandle (key: $cleanHandle)")
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to sync profile: ${e.message}")
        }
    }

    suspend fun searchUserByHandle(query: String): FriendUser? {
        val cleanQuery = query.trim().lowercase().removePrefix("@")
        if (cleanQuery.length < 2) return null
        val normalized = "@$cleanQuery"

        // 1. Direct document lookup by clean handle
        try {
            val docClean = usersCollection.document(cleanQuery).get().await()
            if (docClean.exists()) {
                val data = docClean.data
                if (data != null) return parseDocToFriend(docClean.id, data)
            }
        } catch (e: Exception) {
            Log.d("FirebaseSyncService", "Direct lookup cleanHandle failed: ${e.message}")
        }

        // 2. Direct document lookup by @handle
        try {
            val docWithAt = usersCollection.document(normalized).get().await()
            if (docWithAt.exists()) {
                val data = docWithAt.data
                if (data != null) return parseDocToFriend(docWithAt.id, data)
            }
        } catch (e: Exception) {
            Log.d("FirebaseSyncService", "Direct lookup normalized failed: ${e.message}")
        }

        // 3. Fallback query by handle field
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
                // 4. Try querying cleanHandle field
                val snap2 = usersCollection
                    .whereEqualTo("cleanHandle", cleanQuery)
                    .limit(1)
                    .get()
                    .await()
                if (!snap2.isEmpty) {
                    val doc = snap2.documents.first()
                    parseDocToFriend(doc.id, doc.data ?: emptyMap())
                } else null
            }
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Error searching user $normalized: ${e.message}")
            null
        }
    }

    fun observeFriend(friendIdOrHandle: String): Flow<FriendUser?> = callbackFlow {
        val clean = friendIdOrHandle.trim().lowercase().removePrefix("@")
        val targetDoc = usersCollection.document(clean)

        val listener: ListenerRegistration = targetDoc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirebaseSyncService", "Error observing friend $clean: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val friend = parseDocToFriend(snapshot.id, snapshot.data ?: emptyMap())
                trySend(friend)
            } else {
                // If clean not found, try friendIdOrHandle directly
                usersCollection.document(friendIdOrHandle).get().addOnSuccessListener { s ->
                    if (s.exists()) {
                        trySend(parseDocToFriend(s.id, s.data ?: emptyMap()))
                    }
                }
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    // --- Realtime Chat Messages ---

    fun observeMessages(
        channelId: String,
        currentUserId: String,
        currentUserHandle: String = ""
    ): Flow<List<ChatMessage>> = callbackFlow {
        if (channelId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val myCleanHandle = currentUserHandle.lowercase().trim().removePrefix("@")

        val listener: ListenerRegistration = chatsCollection.document(channelId)
            .collection("messages")
            .orderBy("timestampEpoch", Query.Direction.ASCENDING)
            .limit(150)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseSyncService", "Error listening to messages in $channelId: ${error.message}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val senderId = data["senderId"] as? String ?: ""
                    val senderHandle = (data["senderHandle"] as? String ?: "").lowercase().trim().removePrefix("@")
                    val isFromMe = (senderId == currentUserId || (myCleanHandle.isNotEmpty() && senderHandle == myCleanHandle))

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
            avatarUri = data["avatarUri"] as? String,
            currentClass = data["currentClass"] as? String,
            currentRoom = data["currentRoom"] as? String,
            classEndTime = data["classEndTime"] as? String,
            isAttendingClass = data["isAttendingClass"] as? Boolean ?: false,
            schedule = parsedSchedule
        )
    }

    // --- Group Chats Cloud Sync ---

    suspend fun publishGroupChat(group: GroupChat, creatorHandle: String, memberHandles: List<String>) {
        val cleanCreator = creatorHandle.trim().lowercase().removePrefix("@")
        val cleanMembers = (listOf(cleanCreator) + memberHandles.map { it.trim().lowercase().removePrefix("@") }).distinct()

        val groupData = hashMapOf<String, Any?>(
            "id" to group.id,
            "name" to group.name,
            "creator" to cleanCreator,
            "members" to cleanMembers,
            "memberFriendIds" to group.memberFriendIds,
            "updatedAt" to System.currentTimeMillis()
        )

        try {
            groupChatsCollection.document(group.id).set(groupData).await()
        } catch (e: Exception) {
            Log.w("FirebaseSyncService", "Failed to sync group chat: ${e.message}")
        }
    }

    fun observeGroupChats(userHandle: String): Flow<List<GroupChat>> = callbackFlow {
        val cleanHandle = userHandle.trim().lowercase().removePrefix("@")
        if (cleanHandle.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val listener = groupChatsCollection
            .whereArrayContains("members", cleanHandle)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseSyncService", "Error listening to group chats: ${error.message}")
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val memberIds = (data["memberFriendIds"] as? List<String>) ?: emptyList()
                    GroupChat(
                        id = doc.id,
                        name = data["name"] as? String ?: "Группа",
                        memberFriendIds = memberIds
                    )
                } ?: emptyList()

                trySend(groups)
            }

        awaitClose {
            listener.remove()
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
