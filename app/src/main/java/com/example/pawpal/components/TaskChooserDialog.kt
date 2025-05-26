package com.example.pawpal.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@Composable
fun TaskChooserDialog(
    userId: String,
    allowedTaskIds: List<String>? = null,
    onDismiss: () -> Unit,
    onTaskSelected: (Map<String, Any>) -> Unit

) {
    var tasks by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Fetch tasks
    LaunchedEffect(userId) {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("task")
            .get()
            .await()

        tasks = snapshot.documents.mapNotNull { doc ->
            val data = doc.data?.plus("id" to doc.id)
            if (allowedTaskIds == null || allowedTaskIds.contains(doc.id)) data else null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a Task") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(tasks) { task ->
                    val petInfo = task["petInfo"] as? Map<*, *> ?: emptyMap<Any, Any>()
                    val title = task["title"]?.toString() ?: "Untitled"
                    val location = task["manualLocation"]?.toString() ?: "Untitled"
                    val petName = petInfo["name"]?.toString() ?: "Unknown"
                    val imageUrl = petInfo["imageUrl"]?.toString()

                    var imageBitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

                    LaunchedEffect(imageUrl) {
                        if (!imageUrl.isNullOrEmpty()) {
                            try {
                                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                                val bytes = ref.getBytes(200 * 512).await()
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                imageBitmap = bmp.asImageBitmap()
                            } catch (_: Exception) { }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskSelected(task) }
                            .padding(12.dp)
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .fillMaxSize().clip(CircleShape)

                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("📝 $title")
                            Text("🐾 $petName", style = MaterialTheme.typography.bodySmall)
                            Text("📍 $location", style = MaterialTheme.typography.bodySmall)

                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
