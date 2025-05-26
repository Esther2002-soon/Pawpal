package com.example.pawpal.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import java.text.SimpleDateFormat
import androidx.compose.ui.Alignment
import com.google.firebase.Timestamp
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.lazy.itemsIndexed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.runtime.produceState
import com.google.firebase.firestore.Query
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavController
import androidx.compose.material.icons.filled.Comment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures

@Composable
fun ProfilePicture(userId: String, onClick: (String) -> Unit) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(userId) {
        val doc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
        val imageUrl = doc.getString("imageUrl")
        if (!imageUrl.isNullOrEmpty()) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            val bytes = ref.getBytes(512 * 512).await()
            imageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
        }
    }

    Box(modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .clickable { onClick(userId) }) {
        if (imageBitmap != null) {
            Image(bitmap = imageBitmap!!, contentDescription = null, contentScale = ContentScale.Crop,modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.AccountCircle, contentDescription = null)
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    post: Map<String, Any>,
    navController: androidx.navigation.NavController,
    onOwnerClick: (String) -> Unit,
    showHeader: Boolean = true,
    showLikeInteractive: Boolean = true
) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val postId = post["id"]?.toString() ?: return
    val ownerId = post["ownerId"]?.toString() ?: return
    val caption = post["caption"]?.toString() ?: ""
    val imageUrls = post["imageUrls"] as? List<String> ?: emptyList()
    val likes = (post["likes"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val isLiked = userId in likes
    val comments by produceState<List<Map<String, Any>>>(initialValue = emptyList(), postId) {
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                value = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
    }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }
    var imageBitmaps by remember(postId) { mutableStateOf<List<ImageBitmap>>(emptyList()) }
    var commenterProfile by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrls) {
        val bitmaps = imageUrls.mapNotNull {
            try {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(it)
                val bytes = ref.getBytes(512 * 1024).await()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
            } catch (_: Exception) { null }
        }
        imageBitmaps = bitmaps
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showHeader) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfilePicture(ownerId, onClick = onOwnerClick)
                    Spacer(Modifier.width(8.dp))
                    val timestamp = post["timestamp"]
                    val formattedTime = remember(timestamp) {
                        try {
                            val date = (timestamp as? Timestamp)?.toDate()
                            SimpleDateFormat("dd/MM/yyyy HH:mm").format(date!!)
                        } catch (e: Exception) {
                            "Unknown time"
                        }
                    }
                    Column(modifier = Modifier.padding(2.dp)) {
                        Text(post["username"]?.toString() ?: "User", style = MaterialTheme.typography.titleMedium)
                        Text(formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (imageBitmaps.isNotEmpty()) {
                val listState = rememberLazyListState()
                val snappingFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                var currentImage by remember { mutableStateOf(0) }

                LaunchedEffect(listState.firstVisibleItemIndex) {
                    currentImage = listState.firstVisibleItemIndex
                }

                Box {
                    LazyRow(
                        state = listState,
                        flingBehavior = snappingFlingBehavior,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        itemsIndexed(imageBitmaps) { _, img ->
                            Image(
                                bitmap = img,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Text(
                        text = "${currentImage + 1}/${imageBitmaps.size}",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // ✅ Group like icon + count together
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconToggleButton(
                        checked = isLiked,
                        onCheckedChange = {
                            if (userId != null) {
                                val ref = db.collection("posts").document(postId)
                                if (isLiked) {
                                    ref.update("likes", FieldValue.arrayRemove(userId))
                                } else {
                                    ref.update("likes", FieldValue.arrayUnion(userId))
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("${likes.size}")
                }

                // ✅ Comment section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showCommentDialog = true }
                ) {
                    Icon(Icons.Filled.Comment, contentDescription = "Comments")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${comments.size}")
                }
            }

            // ✅ Comment dialog
            if (showCommentDialog) {
                AlertDialog(
                    onDismissRequest = { showCommentDialog = false },
                    confirmButton = {},
                    title = { Text("Comments") },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            var newComment by remember { mutableStateOf("") }

                            Box(modifier = Modifier.height(250.dp)) {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    comments.forEach { comment ->
                                        CommentRow(
                                            comment = comment,
                                            userId = userId,
                                            onOwnerClick = onOwnerClick,
                                            onLongClickDelete = {
                                                val id = comment["userId"]?.toString() ?: ""
                                                val text = comment["text"]?.toString() ?: ""
                                                commentToDelete = id to text
                                                showDeleteConfirm = true
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newComment,
                                    onValueChange = { newComment = it },
                                    placeholder = { Text("Write a comment...") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                Button(onClick = {
                                    if (newComment.isNotBlank() && userId != null) {
                                        db.collection("posts").document(postId)
                                            .collection("comments")
                                            .add(
                                                mapOf(
                                                    "userId" to userId,
                                                    "text" to newComment,
                                                    "timestamp" to FieldValue.serverTimestamp()
                                                )
                                            )
                                        newComment = ""
                                    }
                                }) {
                                    Text("Post")
                                }
                            }
                        }
                    }
                )
                if (showDeleteConfirm && commentToDelete != null) {
                    val (cid, ctext) = commentToDelete!!
                    AlertDialog(
                        onDismissRequest = {
                            showDeleteConfirm = false
                            commentToDelete = null
                        },
                        confirmButton = {
                            val context = LocalContext.current
                            TextButton(onClick = {
                                val dbRef = db.collection("posts").document(postId).collection("comments")
                                dbRef.whereEqualTo("userId", cid)
                                    .whereEqualTo("text", ctext)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        snapshot.documents.firstOrNull()?.reference?.delete()
                                            ?.addOnSuccessListener {
                                                Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                                            }
                                            ?.addOnFailureListener {
                                                Toast.makeText(context, "Failed to delete comment", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                showDeleteConfirm = false
                                commentToDelete = null
                            }) {
                                Text("Confirm")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                commentToDelete = null
                            }) {
                                Text("Cancel")
                            }
                        },
                        title = { Text("Delete Comment") },
                        text = { Text("Are you sure you want to delete this comment?") }
                    )
                }

            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentRow(
    comment: Map<String, Any>,
    userId: String?,
    onOwnerClick: (String) -> Unit,
    onLongClickDelete: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val commenterId = comment["userId"]?.toString() ?: ""
    val commentText = comment["text"]?.toString() ?: ""
    var commenterName by remember { mutableStateOf("User") }

    LaunchedEffect(commenterId) {
        val doc = db.collection("users").document(commenterId).get().await()
        commenterName = doc.getString("username") ?: "User"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (userId == commenterId) {
                            onLongClickDelete()
                        }
                    }
                )
            }
            .padding(vertical = 4.dp)
    )
    {
        ProfilePicture(commenterId) { onOwnerClick(commenterId) }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(commenterName, style = MaterialTheme.typography.bodyMedium)
            Text(commentText, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditableMediaCard(
    post: Map<String, Any>,
    onEdit: (Map<String, Any>) -> Unit,
    onDelete: (Map<String, Any>) -> Unit,
    navController: NavController
) {
    var showMenu by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val postId = post["id"]?.toString() ?: return

    val livePost by produceState<Map<String, Any>>(initialValue = post, key1 = postId) {
        db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.data
                if (data != null) {
                    // merge back the critical fields if missing
                    val merged = data.toMutableMap()
                    if (!merged.containsKey("ownerId")) {
                        merged["ownerId"] = post["ownerId"] ?: ""
                    }
                    if (!merged.containsKey("username")) {
                        merged["username"] = post["username"] ?: ""
                    }
                    merged["id"] = postId
                    value = merged
                } else {
                    value = post
                }
            }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .padding(4.dp)
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
        ) {
            // Scrollable MediaCard content
            Box(
                modifier = Modifier
                    .weight(1f) // fills available space
                    .verticalScroll(rememberScrollState())
            ) {
                MediaCard(
                    post = livePost,
                    navController = navController,
                    onOwnerClick = {},
                    showHeader = false,
                    showLikeInteractive = false
                )
            }

            // Fixed 3-dot menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit(post)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete(post)
                        }
                    )
                }
            }
        }
    }
}
