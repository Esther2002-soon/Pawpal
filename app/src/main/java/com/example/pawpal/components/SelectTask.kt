// components/SelectTask.kt
package com.example.pawpal.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable

@Composable
fun SelectTaskDialog(
    task: Map<String, Any>,
    pet: Map<String, Any>,
    onDismiss: () -> Unit
) {
    val imageUrl = pet["imageUrl"]?.toString()
    val lat = task["lat"] as? Double
    val lng = task["lng"] as? Double
    val context = LocalContext.current
    var imageBitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrl) {
        if (!imageUrl.isNullOrEmpty()) {
            try {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                val bytes = ref.getBytes(1024 * 1024).await()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                imageBitmap = bmp.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Pet Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = task["title"]?.toString() ?: "Untitled Task",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val fields = listOfNotNull(
                    "Description" to task["description"],
                    "Time" to "${task["startTime"]} \n${task["endTime"]}",
                    "Location Details" to task["manualLocation"],
                    "Location" to task["location"],
                    "Pet Name" to pet["name"],
                    "Age" to pet["age"],
                    "Breed" to pet["breed"],
                    "Gender" to pet["gender"],
                    "More" to pet["personality"]
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    fields.forEachIndexed { index, (label, value) ->
                        val isClickable = (label == "Location" || label == "Location Details") && lat != null && lng != null
                        Text(
                            text = "$label:\n${value ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isClickable) Color(0xFF1565C0) else Color.Unspecified
                            ),
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .then(
                                    if (isClickable) Modifier.clickable {
                                        val mapsUrl = "https://maps.google.com/?q=$lat,$lng"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                        context.startActivity(intent)
                                    } else Modifier
                                )
                        )

                        if (index < fields.lastIndex) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}