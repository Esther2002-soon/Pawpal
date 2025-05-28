package com.example.pawpal.layout

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pawpal.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import java.io.InputStream
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import kotlinx.coroutines.launch
import com.example.pawpal.utils.imageBitmapToAndroidBitmap
import com.google.firebase.firestore.SetOptions

@Composable
fun RoleDropdown(selected: String, onSelectedChange: (String) -> Unit) {
    val options = listOf("Pet Owner", "Pet Lover", "Both", "Other")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("User Type")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(16.dp)
        ) {
            Text(text = selected.ifEmpty { "Select a type" }, color = Color.Black)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GenderDropdown(selected: String, onSelectedChange: (String) -> Unit) {
    val options = listOf("Male", "Female", "Other")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Gender")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                .clickable { expanded = true }
                .padding(16.dp)
        ) {
            Text(text = selected.ifEmpty { "Select gender" }, color = Color.Black)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectedChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingScreen(navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val auth = FirebaseAuth.getInstance()
    val isSavingState = remember { mutableStateOf(false) }
    var isSaving by isSavingState
    var username by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }
    var customRole by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("") }
    var customGender by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageBitmap = bitmap?.asImageBitmap()
        }
    }
    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        username = document.getString("username") ?: ""
                        selectedRole = document.getString("role") ?: ""
                        bio = document.getString("bio") ?: ""
                        location = document.getString("location") ?: ""
                        selectedGender = document.getString("gender") ?: ""

                        val imageUrl = document.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            val storage = FirebaseStorage.getInstance()
                            val imageRef = storage.getReferenceFromUrl(imageUrl)
                            imageRef.getBytes(100 * 1024)
                                .addOnSuccessListener { bytes ->
                                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    imageBitmap = bmp.asImageBitmap()
                                }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally)
        {
            Text("Set up your profile", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { imageLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Default Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Text("Tap to choose photo", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.Black)
            )

            Spacer(modifier = Modifier.height(8.dp))

            RoleDropdown(selected = selectedRole, onSelectedChange = { selectedRole = it })
            if (selectedRole == "Other") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customRole,
                    onValueChange = { customRole = it },
                    label = { Text("Enter your role") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio (optional)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.Black)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.Black)
            )

            Spacer(modifier = Modifier.height(8.dp))

            GenderDropdown(selected = selectedGender, onSelectedChange = { selectedGender = it })

            if (selectedGender == "Other") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customGender,
                    onValueChange = { customGender = it },
                    label = { Text("Enter gender") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.Black)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isSaving = true

                        val userId = auth.currentUser?.uid
                        if (userId == null) {
                            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                            isSaving = false
                            return@launch
                        }

                        val db = FirebaseFirestore.getInstance()
                        val storage = FirebaseStorage.getInstance()
                        val profileRef = db.collection("users").document(userId)
                        val trimmedUsername = username.trim()

                        db.collection("users")
                            .whereEqualTo("username", trimmedUsername)
                            .get()
                            .addOnSuccessListener { documents ->
                                val usernameTakenByOthers = documents.any { it.id != userId }

                                if (!usernameTakenByOthers) {
                                    if (imageBitmap != null) {
                                        val baos = ByteArrayOutputStream()
                                        val bitmap = imageBitmapToAndroidBitmap(imageBitmap!!, 640, 640)
                                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, baos)
                                        val imageData = baos.toByteArray()

                                        val imagePath = "profile_images/$userId.webp"
                                        val imageRef = storage.reference.child(imagePath)

                                        imageRef.putBytes(imageData)
                                            .addOnSuccessListener {
                                                imageRef.downloadUrl.addOnSuccessListener { uri ->
                                                    saveProfileToFirestore(
                                                        profileRef,
                                                        trimmedUsername,
                                                        selectedRole,
                                                        customRole,
                                                        bio,
                                                        location,
                                                        selectedGender,
                                                        customGender,
                                                        uri.toString(),
                                                        context,
                                                        navController,
                                                        isSaving = isSavingState
                                                    )
                                                }
                                            }
                                            .addOnFailureListener {
                                                isSaving = false
                                                Toast.makeText(context, "Image upload failed.", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        saveProfileToFirestore(
                                            profileRef,
                                            trimmedUsername,
                                            selectedRole,
                                            customRole,
                                            bio,
                                            location,
                                            selectedGender,
                                            customGender,
                                            null,
                                            context,
                                            navController,
                                            isSaving = isSavingState
                                        )
                                    }
                                } else {
                                    isSaving = false
                                    Toast.makeText(context, "Username already taken!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Failed to check username.", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    auth.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SETTING) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp), // adjust if needed
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

fun saveProfileToFirestore(
    docRef: com.google.firebase.firestore.DocumentReference,
    username: String,
    role: String,
    customRole: String,
    bio: String,
    location: String,
    gender: String,
    customGender: String,
    imageUrl: String?,
    context: android.content.Context,
    navController: NavController,
    isSaving: MutableState<Boolean>,
) {
    docRef.get().addOnSuccessListener { snapshot ->
        val existingRate = snapshot.getDouble("rate") ?: 0.0
        val existingRatedBy = snapshot.getLong("ratedBy") ?: 0

        val profileData = hashMapOf(
            "username" to username,
            "role" to if (role == "Other") customRole else role,
            "bio" to bio,
            "location" to location,
            "gender" to if (gender == "Other") customGender else gender,
            "imageUrl" to imageUrl,
            "profileComplete" to true,
            "rate" to existingRate,
            "ratedBy" to existingRatedBy
        )

        docRef.set(profileData, SetOptions.merge())
            .addOnSuccessListener {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    isSaving.value = false
                }
                Toast.makeText(context, "Profile saved!", Toast.LENGTH_SHORT).show()
                navController.navigate(Routes.PROFILE) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
            .addOnFailureListener {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    isSaving.value = false
                }
                Toast.makeText(context, "Error saving profile.", Toast.LENGTH_SHORT).show()
            }
    }
}
