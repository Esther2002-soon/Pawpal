package com.example.pawpal.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.pawpal.ui.theme.Purple40
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import android.graphics.Bitmap
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import android.Manifest
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import com.example.pawpal.components.RatingRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.pawpal.utils.imageBitmapToAndroidBitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var backgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pets by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var userPosts by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var showAddPostDialog by remember { mutableStateOf(false) }

    fun refreshPets() {
        if (userId != null) {
            db.collection("users").document(userId).collection("pets").get()
                .addOnSuccessListener { result ->
                    pets = result.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }
                }
        }
    }

    val backgroundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val stream = context.contentResolver.openInputStream(uri)
            val bmp = BitmapFactory.decodeStream(stream)
            backgroundBitmap = bmp.asImageBitmap()
            val storageRef = FirebaseStorage.getInstance().reference
            val backgroundRef = storageRef.child("background_pic/$userId")

            val resized = imageBitmapToAndroidBitmap(bmp.asImageBitmap(), 640, 640)
            val baos = java.io.ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val compressedBytes = baos.toByteArray()

            backgroundRef.putBytes(compressedBytes)
                .addOnSuccessListener {
                    backgroundRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        db.collection("users").document(userId!!).update("backgroundUrl", downloadUri.toString())
                    }
                }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }
    )


    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        refreshPets()
        val postSnapshot = db.collection("users").document(currentUserId).collection("posts").get().await()
        userPosts = postSnapshot.documents.mapNotNull { it.data?.plus("id" to it.id) }
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    username = document.getString("username") ?: ""
                    bio = document.getString("bio") ?: ""
                    role = document.getString("role") ?: ""
                    gender = document.getString("gender") ?: ""
                    location = document.getString("location") ?: ""

                    val imageUrl = document.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                        storageRef.getBytes(512 * 1024)
                            .addOnSuccessListener { bytes ->
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                imageBitmap = bmp.asImageBitmap()
                            }
                    }
                    val backgroundUrl = document.getString("backgroundUrl")
                    if (!backgroundUrl.isNullOrEmpty()) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(backgroundUrl)
                        storageRef.getBytes(512 * 1024)
                            .addOnSuccessListener { bytes ->
                                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                backgroundBitmap = bmp.asImageBitmap()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {

        // Profile container with no side gaps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(Modifier.defaultMinSize(minHeight = 160.dp))
        ) {
            if (backgroundBitmap != null) {
                Image(
                    bitmap = backgroundBitmap!!,
                    contentDescription = "Background Image",
                    contentScale = ContentScale.Crop, // Stretch to fill space
                    modifier = Modifier
                        .matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Purple40)
                )
            }

            // DARK OVERLAY FOR READABILITY
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            startY = 600f,
                            endY = 0f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = { dropdownExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSecondary)
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Background") },
                                onClick = {
                                    dropdownExpanded = false
                                    backgroundPickerLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Remove Background") },
                                onClick = {
                                    dropdownExpanded = false
                                    backgroundBitmap = null
                                    if (userId != null) {
                                        db.collection("users").document(userId)
                                            .update("backgroundUrl", null)
                                        val bgRef = FirebaseStorage.getInstance().reference.child("background_pic/$userId")
                                        bgRef.delete()
                                    }
                                }
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile picture
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap!!,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = username,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSecondary
                        )

                        if (bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
            }
        }


        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            ProfileInfoRow(label = "Role", value = role)
            ProfileInfoRow(label = "Gender", value = gender)
            ProfileInfoRow(label = "Location", value = location)
            RatingRow(userId = userId)
            PetGallery(userId = userId, pets = pets, onPetsUpdated = { refreshPets() })
            Spacer(modifier = Modifier.height(24.dp))
            Text("My Task", style = MaterialTheme.typography.titleMedium)
            userId?.let { uid ->
                TaskGallery(userId = userId!!, pets = pets)
            }
            if (!userId.isNullOrBlank()) {
                PostGallery(userId = userId!!, navController = navController)
            }
        }

    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(80.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}