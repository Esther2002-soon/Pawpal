package com.example.pawpal.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.tasks.await
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.Brush
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import java.net.URLEncoder
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import com.google.firebase.auth.FirebaseAuth

@Composable
fun OwnerCard(
    navController: NavController,
    onDismiss: () -> Unit,
    profileUrl: String?,
    backgroundUrl: String?,
    username: String,
    bio: String,
    role: String,
    gender: String,
    location: String,
    ownerId: String,
    preloadTaskInfo: Map<String, Any?>? = null
)
{
    var profileImage by remember(profileUrl) { mutableStateOf<ImageBitmap?>(null) }
    var backgroundImage by remember(backgroundUrl) { mutableStateOf<ImageBitmap?>(null) }
    var ownerUsername by remember { mutableStateOf<String?>(null) }
    var bioText by remember { mutableStateOf<String?>(null) }
    var roleText by remember { mutableStateOf<String?>(null) }
    var genderText by remember { mutableStateOf<String?>(null) }
    var locationText by remember { mutableStateOf<String?>(null) }
    var profileUrlState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ownerId) {
        try {
            val doc = FirebaseFirestore.getInstance().collection("users").document(ownerId).get().await()
            ownerUsername = doc.getString("username")
            bioText = doc.getString("bio")
            roleText = doc.getString("role")
            genderText = doc.getString("gender")
            locationText = doc.getString("location")

            val profile = doc.getString("imageUrl")
            profileUrlState = profile
            val background = doc.getString("backgroundUrl")
            if (!background.isNullOrEmpty()) {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(background)
                val bytes = ref.getBytes(200 * 512).await()
                backgroundImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
            }
            if (!profile.isNullOrEmpty()) {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(profile)
                val bytes = ref.getBytes(200 * 512).await()
                profileImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier
                        .height(160.dp)
                        .fillMaxWidth()) {
                        if (backgroundImage != null) {
                            Image(
                                bitmap = backgroundImage!!,
                                contentDescription = "Background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .offset(y = (-48).dp)
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImage != null) {
                            Image(
                                bitmap = profileImage!!,
                                contentDescription = "Owner Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Default Avatar",
                                modifier = Modifier.fillMaxSize(),
                                tint = Color.Gray
                            )
                        }
                    }
                    Text(text = ownerUsername ?: "Loading...", style = MaterialTheme.typography.titleLarge)
                    Text(text = bioText ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Role: $roleText", color = Color(0xffc7621a))
                    Text(text = "Gender: $genderText", color = Color(0xffc7621a))
                    Text(text = "Location: $locationText", color = Color(0xffc7621a))

                    Spacer(modifier = Modifier.height(8.dp))
                    RatingRow(userId = ownerId, centered = true)
                    Spacer(modifier = Modifier.height(20.dp))
                    val context = LocalContext.current
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@AlertDialog

                    IconButton(
                        onClick = {
                            if (currentUserId == ownerId) {
                                Toast.makeText(context, "You can't message yourself.", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }

                            val chatId = listOf(currentUserId, ownerId).sorted().joinToString("_")
                            val encodedUsername = URLEncoder.encode(ownerUsername ?: "Unknown", "UTF-8")
                            val encodedProfileUrl = URLEncoder.encode(profileUrlState ?: "", "UTF-8")
                            val fullTaskParams = preloadTaskInfo?.toMutableMap()?.apply {
                                put("source", "map")
                            }
                            val taskParams = fullTaskParams?.entries?.joinToString("&") {
                                "${it.key}=${URLEncoder.encode(it.value?.toString() ?: "", "UTF-8")}"
                            }
                            navController.navigate("chat/$chatId/$ownerId/$encodedUsername/$encodedProfileUrl?${taskParams ?: ""}")
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(top = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "Message",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    )
}