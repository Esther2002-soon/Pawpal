package com.example.pawpal.components

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale

@Composable
fun RatingCard(
    onDismiss: () -> Unit,
    rateeId: String,
    petImageUrl: String?,
    taskTitle: String,
    petName: String,
    location: String,
    chatId: String,
    messageId: String
)
{
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRating by remember { mutableStateOf(0) }
    var submitted by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(petImageUrl) {
        petImageUrl?.takeIf { it.isNotBlank() }?.let {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(it)
            val bytes = ref.getBytes(200 * 512).await()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageBitmap = bmp.asImageBitmap()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column(modifier = Modifier.padding(12.dp)) {
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text("Task: $taskTitle")
                Text("Pet: $petName")
                Text("Location: $location")

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconToggleButton(
                            checked = i <= selectedRating,
                            onCheckedChange = { selectedRating = i }
                        ) {
                            Icon(
                                imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (i <= selectedRating)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (selectedRating in 1..5 && !submitted) {
                            submitted = true
                            scope.launch {
                                try {
                                    val userRef = db.collection("users").document(rateeId)
                                    val doc = userRef.get().await()
                                    val current = doc.getDouble("rate") ?: 0.0
                                    val ratedBy = doc.getLong("ratedBy") ?: 0
                                    val newRatedBy = ratedBy + 1
                                    val newAvg = ((current * ratedBy) + selectedRating) / newRatedBy
                                    userRef.update("rate", newAvg, "ratedBy", newRatedBy).await()

                                    // 🗑️ Delete message from chat
                                    db.collection("chats").document(chatId)
                                        .collection("messages").document(messageId)
                                        .delete()

                                    Toast.makeText(context, "Thank you for rating!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Rating failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !submitted && selectedRating > 0
                ) {
                    Text("Submit")
                }
            }
        }
    )
}