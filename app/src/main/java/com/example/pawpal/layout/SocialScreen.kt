package com.example.pawpal.layout

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.example.pawpal.components.MediaCard
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.AccountCircle
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.tasks.await
import java.util.Date
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.firebase.firestore.Query
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.pawpal.components.OwnerCard
import androidx.compose.ui.draw.clip
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.horizontalScroll
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RememberedOwnerCard(
    ownerId: String,
    navController: NavController,
    onDismiss: () -> Unit
) {
    var ownerProfile by remember(ownerId) { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(ownerId) {
        val doc = FirebaseFirestore.getInstance().collection("users").document(ownerId).get().await()
        ownerProfile = doc.data
    }

    ownerProfile?.let { owner ->
        OwnerCard(
            navController = navController,
            onDismiss = onDismiss,
            profileUrl = owner["imageUrl"]?.toString(),
            backgroundUrl = owner["backgroundUrl"]?.toString(),
            username = owner["username"]?.toString() ?: "Unknown",
            bio = owner["bio"]?.toString() ?: "",
            role = owner["role"]?.toString() ?: "N/A",
            gender = owner["gender"]?.toString() ?: "N/A",
            location = owner["location"]?.toString() ?: "N/A",
            ownerId = ownerId
        )
    }
}
@Composable
fun SocialScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var posts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedOwnerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    posts = snapshot.documents.mapNotNull { it.data?.plus("id" to it.id) }
                }
            }
    }
    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
        Text("Posts", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(posts) { post ->
                MediaCard(
                    post = post,
                    navController = navController,
                    onOwnerClick = { selectedOwnerId = it }
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    selectedOwnerId?.let {
        RememberedOwnerCard(
            ownerId = it,
            navController = navController,
            onDismiss = { selectedOwnerId = null }
        )
    }
}