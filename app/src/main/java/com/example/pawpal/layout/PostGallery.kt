package com.example.pawpal.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import com.example.pawpal.navigation.Routes
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.*
import com.example.pawpal.components.MediaCard
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.pawpal.components.AddPostDialog
import androidx.compose.runtime.setValue
import com.google.firebase.firestore.Query
import com.example.pawpal.components.EditableMediaCard
import kotlinx.coroutines.tasks.await

@Composable
fun PostGallery(userId: String, navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var posts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showAddPostDialog by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val postList = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data
                        if (data != null) {
                            data + mapOf("id" to doc.id, "ownerId" to userId)
                        } else null
                    }
                    posts = postList
                    println("PostGallery loaded ${postList.size} posts for $userId")
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("My Posts", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAddPostDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Post")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 2000.dp), // you can adjust this as needed
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(posts) { post ->
                EditableMediaCard(
                    post = post,
                    onEdit = { editingPost = it },
                    onDelete = {
                        scope.launch {
                            val postId = it["id"].toString()
                            val storage = FirebaseStorage.getInstance()

                            // 1. Delete all images in Firebase Storage under users/$userId/posts/$postId
                            try {
                                val folderRef = storage.reference.child("users/$userId/posts/$postId")
                                val allItems = folderRef.listAll().await()
                                allItems.items.forEach { it.delete().await() }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // 2. Delete Firestore entries
                            db.collection("users").document(userId).collection("posts").document(postId).delete().await()
                            db.collection("posts").document(postId).delete().await()

                            navController.navigate(Routes.LAUNCHER) {
                                popUpTo(Routes.PROFILE) { inclusive = false } // adjust if needed
                                launchSingleTop = true
                            }
                        }
                    }
                    ,
                    navController = navController
                )
            }
        }
    }

    if (showAddPostDialog || editingPost != null) {
        AddPostDialog(
            userId = userId,
            initialPost = editingPost,
            onDismiss = {
                showAddPostDialog = false
                editingPost = null
            },
            onPostSaved = { newPost ->
                posts = listOf(newPost) + posts.filterNot { it["id"] == newPost["id"] }
            }
        )
    }
}