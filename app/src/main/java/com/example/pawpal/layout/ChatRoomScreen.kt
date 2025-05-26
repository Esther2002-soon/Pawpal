package com.example.pawpal.layout

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import com.example.pawpal.components.OwnerCard
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import com.google.firebase.firestore.FieldValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*

@Composable
fun ChatRoomScreen(navController: NavController, currentUserId: String) {
    val db = FirebaseFirestore.getInstance()
    var chatRefs by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        db.collection("users").document(currentUserId).collection("chatRefs")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    chatRefs = snapshot.documents
                        .mapNotNull { it.data }
                        .filter { (it["deletedFor"] as? List<*>)?.contains(currentUserId) != true }
                        .sortedByDescending { it["lastMessageTime"] as? Long ?: 0L }
                }
            }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Conversations", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(chatRefs) { ref ->
            val chatId = ref["chatId"]?.toString() ?: return@items
            val otherId = ref["with"]?.toString() ?: return@items

            ChatRoomItem(
                chatId = chatId,
                otherUserId = otherId,
                navController = navController,
                currentUserId = currentUserId
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
}
@Composable
fun ChatRoomItem(
    chatId: String,
    otherUserId: String,
    navController: NavController,
    currentUserId: String
) {
    val db = FirebaseFirestore.getInstance()
    var username by remember { mutableStateOf("...") }
    var profilePic by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var lastMessage by remember { mutableStateOf("") }
    var profileUrl by remember { mutableStateOf("") }
    var showOwnerDialog by remember { mutableStateOf(false) }
    var ownerProfile by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(otherUserId) {
        try {
            val userDoc = db.collection("users").document(otherUserId).get().await()
            username = userDoc.getString("username") ?: "Unknown"
            profileUrl = userDoc.getString("imageUrl") ?: ""

            if (profileUrl.isNotBlank()) {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(profileUrl)
                val bytes = ref.getBytes(200 * 512).await()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profilePic = bmp.asImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(showOwnerDialog) {
        if (showOwnerDialog) {
            val doc = db.collection("users").document(otherUserId).get().await()
            ownerProfile = doc.data
        }
    }
    var showUnread by remember { mutableStateOf(false) }

    LaunchedEffect(chatId) {
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val latestMsg = snapshot.documents.firstOrNull()
                    lastMessage = latestMsg?.getString("text") ?: ""

                    val lastTime = latestMsg?.getLong("timestamp") ?: 0L
                    val senderId = latestMsg?.getString("senderId") ?: ""

                    if (senderId != currentUserId) {
                        val userChatRef = db.collection("users").document(currentUserId)
                            .collection("chatRefs").document(chatId)

                        userChatRef.get().addOnSuccessListener { doc ->
                            val lastSeen = doc.getLong("lastSeen") ?: 0L
                            showUnread = lastTime > lastSeen
                        }
                    } else {
                        showUnread = false
                    }
                }
            }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showDeleteDialog = true
                    },
                    onTap = {
                        val usernameSafe = username.ifBlank { "Unknown" }
                        val profileSafe = profileUrl.ifBlank { "none" }
                        val encodedUsername = URLEncoder.encode(usernameSafe, "UTF-8")
                        val encodedProfile = URLEncoder.encode(profileSafe, "UTF-8")
                        navController.navigate("chat/$chatId/$otherUserId/$encodedUsername/$encodedProfile")
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                profilePic?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { showOwnerDialog = true },
                        contentScale = ContentScale.Crop
                    )
                } ?: Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(username, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    if (showUnread) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = -20.dp, y = (-4).dp)
                                .background(Color.Red, shape = CircleShape)
                        )
                    }
                }
            }
        }
    }

    if (showOwnerDialog && ownerProfile != null) {
        OwnerCard(
            navController = navController,
            onDismiss = { showOwnerDialog = false },
            profileUrl = ownerProfile!!["imageUrl"]?.toString(),
            backgroundUrl = ownerProfile!!["backgroundUrl"]?.toString(),
            username = ownerProfile!!["username"]?.toString() ?: "Unknown",
            bio = ownerProfile!!["bio"]?.toString() ?: "",
            role = ownerProfile!!["role"]?.toString() ?: "N/A",
            gender = ownerProfile!!["gender"]?.toString() ?: "N/A",
            location = ownerProfile!!["location"]?.toString() ?: "N/A",
            ownerId = otherUserId
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Chat?") },
            text = { Text("Are you sure you want to permanently delete this chat?") },
            confirmButton = {
                TextButton(onClick = {
                    val db = FirebaseFirestore.getInstance()
                    val chatDocRef = db.collection("chats").document(chatId)

                    // Remove from current user's chatRefs
                    db.collection("users").document(currentUserId)
                        .collection("chatRefs").document(chatId).delete()

                    // Mark this chat as deleted for this user
                    chatDocRef.update("deletedFor.$currentUserId", true)

                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

