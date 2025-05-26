package com.example.pawpal.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.Brush
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.spartapps.swipeablecards.ui.SwipeableCardDirection
import com.example.pawpal.components.OwnerCard
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SwipeTaskCard(
    task: Map<String, Any>,
    pet: Map<String, Any>,
    onClick: () -> Unit,
    navController: NavController,
    direction: SwipeableCardDirection? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val petImageUrl = pet["imageUrl"]?.toString()
    var petImage by remember(petImageUrl) { mutableStateOf<ImageBitmap?>(null) }

    // Load pet image
    LaunchedEffect(petImageUrl) {
        if (!petImageUrl.isNullOrEmpty()) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(petImageUrl)
            val bytes = ref.getBytes(1024 * 1024).await()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            petImage = bmp.asImageBitmap()
        }
    }

    val ownerProfileId = pet["ownerProfileId"]?.toString()
    var ownerImage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(ownerProfileId) {
        if (!ownerProfileId.isNullOrEmpty()) {
            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(ownerProfileId).get().await()
                val url = doc.getString("imageUrl")
                if (!url.isNullOrEmpty()) {
                    val ref = FirebaseStorage.getInstance().getReferenceFromUrl(url)
                    val bytes = ref.getBytes(100 * 1024).await()
                    ownerImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .clickable { onClick() }
    )
    {
        direction?.let {
            val visible = remember { mutableStateOf(true) }

            LaunchedEffect(it) {
                visible.value = true
                delay(500)
                visible.value = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = alpha
                    }
                    .zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (it == SwipeableCardDirection.Right) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (it == SwipeableCardDirection.Right) Color(0xFF4CAF50) else Color.Red,
                    modifier = Modifier.size(196.dp)
                )
            }
        }
        petImage?.let {
            Image(
                bitmap = it,
                contentDescription = "Pet Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        // Owner profile picture
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(48.dp)
                .clip(CircleShape)
                .align(Alignment.TopStart)
                .clickable { showDialog = true }
        ) {
            if (ownerImage != null) {
                Image(
                    bitmap = ownerImage!!,
                    contentDescription = "Owner Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Default Avatar",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Task info at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent
                        ),
                        startY = 300f,
                        endY = 0f
                    )
                )
                .padding(16.dp)
        ){
            Column {
                Text("${task["title"]}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Text("🐾 ${pet["name"]}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("🕒 ${task["startTime"]} - ${task["endTime"]}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text("📍 ${task["manualLocation"]}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    if (showDialog) {
        var ownerProfile by remember(ownerProfileId) { mutableStateOf<Map<String, Any>?>(null) }

        LaunchedEffect(ownerProfileId) {
            val doc = FirebaseFirestore.getInstance().collection("users").document(ownerProfileId!!).get().await()
            ownerProfile = doc.data
        }

        ownerProfile?.let {
            OwnerCard(
                navController = navController,
                onDismiss = { showDialog = false },
                profileUrl = it["imageUrl"]?.toString(),
                backgroundUrl = it["backgroundUrl"]?.toString(),
                username = it["username"]?.toString() ?: "Unknown",
                bio = it["bio"]?.toString() ?: "",
                role = it["role"]?.toString() ?: "N/A",
                gender = it["gender"]?.toString() ?: "N/A",
                location = it["location"]?.toString() ?: "N/A",
                ownerId = ownerProfileId ?: ""
            )
        }
    }
}