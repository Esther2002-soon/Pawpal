package com.example.pawpal.layout

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.clickable
import com.google.firebase.firestore.ktx.firestore
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.pawpal.components.RatingCard
import com.example.pawpal.components.TaskChooserDialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.net.URLDecoder
import java.net.URLEncoder
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.Color
import com.example.pawpal.components.SelectTaskDialog
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import com.google.firebase.firestore.SetOptions
import com.example.pawpal.utils.imageBitmapToAndroidBitmap
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.layout.ime
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    chatId: String,
    currentUserId: String,
    otherUserId: String,
    otherUsername: String,
    preloadTaskInfo: Map<String, Any?>? = null,
    otherUserProfileUrl: String?
) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) }
    var messageText by remember { mutableStateOf(TextFieldValue("")) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteMessageId by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showSelectTaskDialog by remember { mutableStateOf(false) }
    var profileImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showTaskChooserDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Map<String, Any>?>(null) }
    var resolvedUsername by remember { mutableStateOf(otherUsername) }
    var resolvedProfileUrl by remember { mutableStateOf(otherUserProfileUrl) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }
    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)

                val squareBitmap = run {
                    val width = bitmap.width
                    val height = bitmap.height
                    val size = minOf(width, height)
                    val xOffset = (width - size) / 2
                    val yOffset = (height - size) / 2
                    Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
                }

                val resized = imageBitmapToAndroidBitmap(squareBitmap.asImageBitmap(), 1080, 1080)

                val baos = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, baos)
                val data = baos.toByteArray()

                val filename = UUID.randomUUID().toString() + ".webp"
                val ref = FirebaseStorage.getInstance().reference
                    .child("chat_images/$chatId/$filename")

                ref.putBytes(data).await()
                val url = ref.downloadUrl.await().toString()

                val message = mapOf(
                    "type" to "image",
                    "imageUrl" to url,
                    "senderId" to currentUserId,
                    "timestamp" to System.currentTimeMillis()
                )

                val chatDoc = db.collection("chats").document(chatId)
                chatDoc.collection("messages").add(message).await()
                chatDoc.update("lastMessageTime", System.currentTimeMillis())
                val senderRef = db.collection("users").document(currentUserId)
                    .collection("chatRefs").document(chatId)
                val receiverRef = db.collection("users").document(otherUserId)
                    .collection("chatRefs").document(chatId)

                val chatRefDataForSender = mapOf(
                    "chatId" to chatId,
                    "with" to otherUserId,
                    "lastMessageTime" to System.currentTimeMillis()
                )
                val chatRefDataForReceiver = mapOf(
                    "chatId" to chatId,
                    "with" to currentUserId,
                    "lastMessageTime" to System.currentTimeMillis()
                )

                senderRef.set(chatRefDataForSender, SetOptions.merge())
                receiverRef.set(chatRefDataForReceiver, SetOptions.merge())
                selectedImageUri = null
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to send image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(150)
            coroutineScope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(resolvedProfileUrl) {
        resolvedProfileUrl?.takeIf { it.isNotBlank() }?.let {
            try {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(it)
                val bytes = ref.getBytes(200 * 512).await()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                profileImage = bmp.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    LaunchedEffect(chatId) {
        val chatDoc = db.collection("chats").document(chatId).get().await()
        val deletedFor = chatDoc.get("deletedFor") as? List<*> ?: emptyList<Any>()

        if (deletedFor.contains(currentUserId)) {
            return@LaunchedEffect
        }
        if (resolvedUsername.isBlank() || resolvedProfileUrl.isNullOrBlank()) {
            try {
                val userDoc = db.collection("users").document(otherUserId).get().await()
                resolvedUsername = userDoc.getString("username") ?: otherUserId
                resolvedProfileUrl = userDoc.getString("profileUrl")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    messages = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) doc.id to data else null
                    }
                }
            }
        db.collection("users").document(currentUserId)
            .collection("chatRefs").document(chatId)
            .update("lastSeen", System.currentTimeMillis())
        if (!preloadTaskInfo.isNullOrEmpty() && preloadTaskInfo["taskId"] != null) {
            val timestamp = System.currentTimeMillis()

            val taskInfoMessage = mapOf(
                "type" to "task_info",
                "senderId" to currentUserId,
                "timestamp" to timestamp,
                "taskTitle" to preloadTaskInfo["taskTitle"],
                "taskId" to preloadTaskInfo["taskId"],
                "petName" to preloadTaskInfo["petName"],
                "manualLocation" to preloadTaskInfo["manualLocation"],
                "lat" to preloadTaskInfo["lat"],
                "lng" to preloadTaskInfo["lng"]
            ).filterValues { it != null }
            val chatRef = db.collection("chats").document(chatId)

            coroutineScope.launch {
                try {
                    chatRef.collection("messages").add(taskInfoMessage).await()
                    val senderRef = db.collection("users").document(currentUserId)
                        .collection("chatRefs").document(chatId)
                    val receiverRef = db.collection("users").document(otherUserId)
                        .collection("chatRefs").document(chatId)

                    val chatRefDataForSender = mapOf(
                        "chatId" to chatId,
                        "with" to otherUserId,
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                    val chatRefDataForReceiver = mapOf(
                        "chatId" to chatId,
                        "with" to currentUserId,
                        "lastMessageTime" to System.currentTimeMillis()
                    )

                    senderRef.set(chatRefDataForSender, SetOptions.merge())
                    receiverRef.set(chatRefDataForReceiver, SetOptions.merge())
                    chatRef.update("lastMessageTime", System.currentTimeMillis())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    suspend fun fetchAndShowTask(taskId: String) {
        try {
            val taskSnapshot = db.collectionGroup("task").get().await()
            val taskDoc = taskSnapshot.firstOrNull { it.id == taskId }
            val taskData = taskDoc?.data ?: return

            val ownerId = taskDoc.reference.parent.parent?.id ?: return
            val petId = taskData["petId"]?.toString() ?: return

            val petSnapshot = db.collection("users").document(ownerId)
                .collection("pets").document(petId)
                .get().await()

            val petData = petSnapshot.data ?: return

            val fullTask = taskData.toMutableMap()
            fullTask["petInfo"] = petData
            fullTask["id"] = taskId // Optional, ensure it's present

            selectedTask = fullTask
            showSelectTaskDialog = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to load task info", Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        modifier = Modifier
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        profileImage?.let {
                            Image(
                                bitmap = it,
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(text = resolvedUsername)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    IconButton(onClick = { showAttachmentMenu = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Task")
                    }

                    DropdownMenu(
                        expanded = showAttachmentMenu,
                        onDismissRequest = { showAttachmentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Send Image") },
                            onClick = {
                                showAttachmentMenu = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Assign Task") },
                            onClick = {
                                showAttachmentMenu = false
                                showTaskChooserDialog = true
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFFF0F0F0), shape = CircleShape)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                IconButton(
                    onClick = {
                        val text = messageText.text.trim()
                        if (text.isNotEmpty()) {
                            val message = mapOf(
                                "text" to text,
                                "senderId" to currentUserId,
                                "timestamp" to System.currentTimeMillis()
                            )
                            val chatDoc = db.collection("chats").document(chatId)
                            val senderRef = db.collection("users").document(currentUserId)
                                .collection("chatRefs").document(chatId)
                            val receiverRef = db.collection("users").document(otherUserId)
                                .collection("chatRefs").document(chatId)

                            val chatRefDataForSender = mapOf(
                                "chatId" to chatId,
                                "with" to otherUserId,
                                "lastMessageTime" to System.currentTimeMillis()
                            )
                            val chatRefDataForReceiver = mapOf(
                                "chatId" to chatId,
                                "with" to currentUserId,
                                "lastMessageTime" to System.currentTimeMillis()
                            )
                            senderRef.set(chatRefDataForSender, SetOptions.merge())
                            receiverRef.set(chatRefDataForReceiver, SetOptions.merge())
                            chatDoc.collection("messages").add(message)
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                    it.printStackTrace()
                                }
                            chatDoc.update("lastMessageTime", System.currentTimeMillis())

                            messageText = TextFieldValue("")
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 30.dp)
                .imePadding()
        ) {
            items(messages) { (id, msg) ->
                val isMe = msg["senderId"] == currentUserId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical =5.dp)
                        .pointerInput(id) {
                            detectTapGestures(
                                onLongPress = {
                                    if (isMe) {
                                        pendingDeleteMessageId = id
                                        showDeleteDialog = true
                                    }
                                }
                            )
                        },
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    when (msg["type"]) {
                        "image" -> {
                            val imageUrl = msg["imageUrl"]?.toString()
                            imageUrl?.let {
                                val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

                                LaunchedEffect(it) {
                                    try {
                                        val ref = FirebaseStorage.getInstance().getReferenceFromUrl(it)
                                        val bytes = ref.getBytes(2 * 1024 * 1024).await()
                                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        imageBitmap.value = bmp.asImageBitmap()
                                    } catch (_: Exception) { }
                                }

                                imageBitmap.value?.let { bmp ->
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = "Sent Image",
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        "rate" -> {
                            val targetUserId = msg["targetUserId"]?.toString()
                            var showRatingDialog by remember { mutableStateOf(false) }
                            var targetUsername by remember { mutableStateOf("this user") }

                            // Fetch target username
                            LaunchedEffect(targetUserId) {
                                if (!targetUserId.isNullOrEmpty()) {
                                    db.collection("users").document(targetUserId).get().addOnSuccessListener {
                                        targetUsername = it.getString("username") ?: "this user"
                                    }
                                }
                            }
                            val canRate = currentUserId != targetUserId

                            if (showRatingDialog && canRate) {
                                RatingCard(
                                    onDismiss = { showRatingDialog = false },
                                    rateeId = targetUserId ?: "",
                                    petImageUrl = msg["petImageUrl"]?.toString(),
                                    taskTitle = msg["taskTitle"]?.toString() ?: "",
                                    petName = msg["petName"]?.toString() ?: "",
                                    location = msg["manualLocation"]?.toString() ?: "",
                                    chatId = chatId,
                                    messageId = id
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.LightGray, shape = RoundedCornerShape(12.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                        .clickable {
                                            if (!canRate) {
                                                Toast.makeText(context, "You cannot rate yourself", Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }
                                            showRatingDialog = true
                                        }
                                ) {
                                    Text(
                                        text = "⭐ Rate $targetUsername",
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                        "task_info" -> {
                            val lat = (msg["lat"] as? Number)?.toDouble()
                            val lng = (msg["lng"] as? Number)?.toDouble()
                            val fromSwipe = msg["source"] == "swipe"
                            val source = msg["source"]?.toString()
                            val isFromMap = source == "map"
                            val isFromSwipe = source == "swipe"
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isMe) Color(0xFFD1E7FF) else Color(0xFFECECEC),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "📝 Task: ${msg["taskTitle"]}",
                                        modifier = Modifier.clickable {
                                            val taskId = msg["taskId"]?.toString() ?: return@clickable
                                            coroutineScope.launch {
                                                fetchAndShowTask(taskId)
                                            }
                                        }
                                    )
                                    Text(
                                        text = "🐾 Pet: ${msg["petName"]}",
                                        color = Color.Blue,
                                        modifier = Modifier.clickable {
                                            val taskId = msg["taskId"]?.toString() ?: return@clickable
                                            coroutineScope.launch {
                                                fetchAndShowTask(taskId)
                                            }
                                        }
                                    )
                                    Text("📍 ${msg["manualLocation"]}",
                                        color = if (lat != null && lng != null) Color.Blue else Color.Gray,
                                        modifier = Modifier.clickable(enabled = lat != null && lng != null) {
                                            if (lat != null && lng != null) {
                                                val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        "task_complete_button" -> {
                            var completed by remember { mutableStateOf(false) }

                            if (msg["senderId"] == currentUserId && !completed) {
                                Button(
                                    onClick = {
                                        val taskId = msg["taskId"]?.toString() ?: return@Button
                                        val taskRef = db.collection("users").document(currentUserId)
                                            .collection("task").document(taskId)
                                        taskRef.delete()
                                        val userIds = chatId.split("_")
                                        val user1 = userIds.getOrNull(0) ?: ""
                                        val user2 = userIds.getOrNull(1) ?: ""
                                        val rateUserAMessage = mapOf(
                                            "type" to "rate",
                                            "targetUserId" to user1,
                                            "senderId" to "system",
                                            "timestamp" to System.currentTimeMillis(),
                                            "taskId" to taskId,
                                            "taskTitle" to msg["taskTitle"],
                                            "petName" to msg["petName"],
                                            "manualLocation" to msg["manualLocation"],
                                            Pair("petImageUrl", selectedTask?.get("petInfo")
                                                ?.let { (it as? Map<*, *>)?.get("imageUrl")?.toString() } ?: ""),
                                        )

                                        val rateUserBMessage = mapOf(
                                            "type" to "rate",
                                            "targetUserId" to user2,
                                            "senderId" to "system",
                                            "timestamp" to System.currentTimeMillis() + 1,
                                            "taskId" to taskId,
                                            "taskTitle" to msg["taskTitle"],
                                            "petName" to msg["petName"],
                                            "manualLocation" to msg["manualLocation"],
                                            Pair("petImageUrl", selectedTask?.get("petInfo")
                                                ?.let { (it as? Map<*, *>)?.get("imageUrl")?.toString() } ?: ""),
                                        )

                                        val chatRef = db.collection("chats").document(chatId)
                                        chatRef.collection("messages").add(rateUserAMessage)
                                        chatRef.collection("messages").add(rateUserBMessage)

                                        db.collection("chats").document(chatId)
                                            .collection("messages").document(id)
                                            .delete()

                                        completed = true
                                    },
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                ) {
                                    Text("✅ Complete")
                                }
                            } else {
                                Text(
                                    text = "Only the task creator can mark it complete.",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isMe) Color(0xFFD1E7FF) else Color(0xFFECECEC),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = msg["text"]?.toString() ?: "",
                                    color = Color.Black
                                )
                            }
                        }
                    }

                }
            }
            item {
                Spacer(modifier = Modifier.height(with(LocalDensity.current) { imeBottom.toDp() }))
            }
        }
    }
    if (showDeleteDialog && pendingDeleteMessageId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete message?") },
            text = { Text("Are you sure you want to delete this message? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    val chatDoc = db.collection("chats").document(chatId)
                    val messageDocRef = chatDoc.collection("messages").document(pendingDeleteMessageId!!)

                    messageDocRef.get().addOnSuccessListener { doc ->
                        val data = doc.data
                        val type = data?.get("type")
                        val imageUrl = data?.get("imageUrl")?.toString()
                        messageDocRef.delete().addOnSuccessListener {
                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to delete message", Toast.LENGTH_SHORT).show()
                        }

                        if (type == "image" && !imageUrl.isNullOrBlank()) {
                            try {
                                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                                ref.delete().addOnSuccessListener {
                                    // Optional: log or toast
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Failed to delete image from storage", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Invalid image URL", Toast.LENGTH_SHORT).show()
                            }
                        }

                        showDeleteDialog = false
                        pendingDeleteMessageId = null
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    pendingDeleteMessageId = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showTaskChooserDialog) {
        TaskChooserDialog(
            userId = currentUserId,
            onDismiss = { showTaskChooserDialog = false },
            onTaskSelected = { task ->
                showTaskChooserDialog = false
                selectedTask = task
                val timestamp = System.currentTimeMillis()

                val taskInfoMessage = mapOf(
                    "type" to "task_info",
                    "senderId" to currentUserId,
                    "timestamp" to timestamp,
                    "taskTitle" to task["title"],
                    "taskId" to task["id"],
                    "petName" to (task["petInfo"] as? Map<*, *>)?.get("name"),
                    "manualLocation" to task["manualLocation"],
                    "lat" to (task["lat"] as? Double),
                    "lng" to (task["lng"] as? Double),
                )

                val taskCompleteButtonMessage = mapOf(
                    "type" to "task_complete_button",
                    "senderId" to currentUserId,
                    "timestamp" to timestamp + 1,
                    "taskId" to task["id"],
                    "taskTitle" to task["title"],
                    "petName" to (task["petInfo"] as? Map<*, *>)?.get("name"),
                    "manualLocation" to task["manualLocation"]
                )

                val chatDoc = db.collection("chats").document(chatId)
                chatDoc.collection("messages").add(taskInfoMessage)
                chatDoc.collection("messages").add(taskCompleteButtonMessage)
                chatDoc.update("lastMessageTime", System.currentTimeMillis())
                val senderRef = db.collection("users").document(currentUserId)
                    .collection("chatRefs").document(chatId)
                val receiverRef = db.collection("users").document(otherUserId)
                    .collection("chatRefs").document(chatId)

                val chatRefDataForSender = mapOf(
                    "chatId" to chatId,
                    "with" to otherUserId,
                    "lastMessageTime" to System.currentTimeMillis()
                )
                val chatRefDataForReceiver = mapOf(
                    "chatId" to chatId,
                    "with" to currentUserId,
                    "lastMessageTime" to System.currentTimeMillis()
                )

                senderRef.set(chatRefDataForSender, SetOptions.merge())
                receiverRef.set(chatRefDataForReceiver, SetOptions.merge())
            }
        )
    }
    if (showSelectTaskDialog && selectedTask != null) {
        val pet = selectedTask!!["petInfo"] as? Map<String, Any> ?: emptyMap()
        SelectTaskDialog(
            task = selectedTask!!,
            pet = pet,
            onDismiss = { showSelectTaskDialog = false }
        )
    }
}