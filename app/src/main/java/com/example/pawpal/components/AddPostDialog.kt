package com.example.pawpal.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.util.*
import androidx.compose.runtime.*
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.util.Date

@Composable
fun AddPostDialog(
    userId: String,
    initialPost: Map<String, Any>? = null,
    onDismiss: () -> Unit,
    onPostSaved: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var caption by remember { mutableStateOf(initialPost?.get("caption")?.toString() ?: "") }
    var removableUrls by remember { mutableStateOf(initialPost?.get("imageUrls") as? List<String> ?: emptyList()) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSaving by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.size + imageUris.size <= 5) {
            imageUris = imageUris + uris
        } else {
            Toast.makeText(context, "Max 5 images allowed", Toast.LENGTH_SHORT).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialPost == null) "Add Post" else "Edit Post") },
        text = {
            Column {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                LazyRow {
                    itemsIndexed(imageUris) { index, uri ->
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(100.dp)
                        ) {
                            val bitmap = remember(uri) {
                                context.contentResolver.openInputStream(uri)?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clickable {
                                        imageUris = imageUris.filterIndexed { i, _ -> i != index }
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
                if (removableUrls.isNotEmpty()) {
                    Text("Current Images", style = MaterialTheme.typography.labelMedium)
                    LazyRow {
                        itemsIndexed(removableUrls) { index, url ->
                            val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = url) {
                                try {
                                    val bytes = FirebaseStorage.getInstance()
                                        .getReferenceFromUrl(url)
                                        .getBytes(512 * 1024).await()
                                    value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                                } catch (e: Exception) {
                                    value = null
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(100.dp)
                            ) {
                                bitmapState.value?.let {
                                    Image(
                                        bitmap = it,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }

                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove Image",
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clickable {
                                            removableUrls = removableUrls.filterIndexed { i, _ -> i != index }
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { launcher.launch("image/*") }) {
                    Text("Select Images")
                }
                if (isSaving) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            val scope = rememberCoroutineScope()

            TextButton(
                onClick = {
                    if (caption.isBlank()) {
                        Toast.makeText(context, "Caption cannot be empty", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }

                    isSaving = true

                    scope.launch {
                        try {
                            val postId = initialPost?.get("id")?.toString()
                                ?: db.collection("users").document(userId).collection("posts").document().id
                            val userDoc = db.collection("users").document(userId).get().await()
                            val username = userDoc.getString("username") ?: "Unknown"
                            val postData = mutableMapOf<String, Any>(
                                "caption" to caption,
                                "timestamp" to Date(),
                                "ownerId" to userId,
                                "username" to username
                            )

                            if (initialPost == null) {
                                postData["likes"] = emptyList<String>()
                            }

                            val urls = mutableListOf<String>()
                            val startIndex = removableUrls.size

                            imageUris.forEachIndexed { i, uri ->
                                val stream = context.contentResolver.openInputStream(uri)
                                val bitmap = BitmapFactory.decodeStream(stream)
                                val baos = ByteArrayOutputStream()
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                val data = baos.toByteArray()
                                val ref = storage.reference.child("users/$userId/posts/$postId/${startIndex + i}.jpg")
                                ref.putBytes(data).await()
                                val url = ref.downloadUrl.await().toString()
                                urls.add(url)
                            }
                            postData["imageUrls"] = if (initialPost != null) {
                                removableUrls + urls
                            } else {
                                urls
                            }
                            val userPostRef = db.collection("users").document(userId).collection("posts").document(postId)
                            userPostRef.set(postData, SetOptions.merge()).await()

                            val globalPostRef = db.collection("posts").document(postId)
                            globalPostRef.set(postData, SetOptions.merge()).await()

                            onPostSaved(postData.toMap() + mapOf("id" to postId))
                            onDismiss()

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}