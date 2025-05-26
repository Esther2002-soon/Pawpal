package com.example.pawpal.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun createOrGetChatId(currentUserId: String, otherUserId: String): String {
    val db = FirebaseFirestore.getInstance()

    val chatId = listOf(currentUserId, otherUserId).sorted().joinToString("_")

    val chatDoc = db.collection("chats").document(chatId)
    val snapshot = chatDoc.get().await()

    if (!snapshot.exists()) {
        val chatInfo = mapOf(
            "userIds" to listOf(currentUserId, otherUserId),
            "lastMessage" to "",
            "timestamp" to System.currentTimeMillis()
        )
        chatDoc.set(chatInfo).await()

        val userRefData = mapOf("chatId" to chatId, "with" to otherUserId)
        val otherRefData = mapOf("chatId" to chatId, "with" to currentUserId)

        db.collection("users").document(currentUserId)
            .collection("chatRefs").document(chatId).set(userRefData).await()

        db.collection("users").document(otherUserId)
            .collection("chatRefs").document(chatId).set(otherRefData).await()

    }

    return chatId
}
