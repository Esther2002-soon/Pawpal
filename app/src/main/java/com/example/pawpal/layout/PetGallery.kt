package com.example.pawpal.layout

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pawpal.components.PetCard
import com.example.pawpal.utils.imageBitmapToAndroidBitmap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import java.io.ByteArrayOutputStream

@Composable
fun PetGallery(userId: String?, pets: List<Map<String, Any>>, onPetsUpdated: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    var pets by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedPet by remember { mutableStateOf<Map<String, Any>?>(null) }
    var editingPet by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showAddPetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (!userId.isNullOrEmpty()) {
            db.collection("users").document(userId).collection("pets").get()
                .addOnSuccessListener { result ->
                    pets = result.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load pets", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("My Pets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            pets.forEach { pet ->
                PetCard(
                    pet = pet,
                    onClick = { selectedPet = it },
                    onEdit = { editingPet = it },
                    onDelete = { deletedId ->
                        pets = pets.filterNot { it["id"]?.toString() == deletedId }
                    }
                )
            }
            // Add new pet card
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                    .clickable {
                        showAddPetDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
    selectedPet?.let { pet ->
        val imageUrl = pet["imageUrl"]?.toString()
        var imageBitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

        LaunchedEffect(imageUrl) {
            if (!imageUrl.isNullOrEmpty()) {
                try {
                    val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                    val bytes = ref.getBytes(200 * 1024).await()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageBitmap = bmp.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedPet = null },
            title = { Text(pet["name"].toString()) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 500.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    listOf(
                        "Age" to pet["age"],
                        "Breed" to pet["breed"],
                        "Gender" to pet["gender"],
                        "More" to pet["personality"]
                    ).forEachIndexed { index, (label, value) ->
                        Text(
                            text = "$label: ${value ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        if (index < 3) {
                            Divider(color = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    imageBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Pet Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPet = null }) {
                    Text("Close")
                }
            }
        )
    }
    if (showAddPetDialog) {
        var isSaving by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var breed by remember { mutableStateOf("") }
        var gender by remember { mutableStateOf("") }
        var personality by remember { mutableStateOf("") }
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageBitmap = bitmap?.asImageBitmap()
                Toast.makeText(context, "Image selected!", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = { showAddPetDialog = false },
            title = { Text("Add Pet") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 500.dp)
                ) {
                    if (isSaving) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 500.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                                        .clickable { imagePicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imageBitmap != null) {
                                        Image(
                                            bitmap = imageBitmap!!,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("Tap to select image", color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") })
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed") })
                                Spacer(modifier = Modifier.height(16.dp))
                                val genderOptions = listOf("Male", "Female", "Other", "Unknown")
                                var genderDropdownExpanded by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                                        .clickable { genderDropdownExpanded = true }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = if (gender.isNotBlank()) gender else "Select Gender",
                                        color = if (gender.isNotBlank()) Color.Black else Color.Gray
                                    )
                                }
                                DropdownMenu(
                                    expanded = genderDropdownExpanded,
                                    onDismissRequest = { genderDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                gender = option
                                                genderDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("Personality/Vaccination") })
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            ,
            confirmButton = {
                TextButton(onClick = {
                    isSaving = true
                    scope.launch {
                        try {
                            val petCollection = db.collection("users").document(userId!!).collection("pets")
                            val docRef = petCollection.document()
                            val petId = docRef.id

                            var uploadedUrl: String? = null
                            if (imageBitmap != null) {
                                val baos = ByteArrayOutputStream()
                                val bitmap = imageBitmapToAndroidBitmap(imageBitmap!!, 1024, 1024)
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                val imageData = baos.toByteArray()
                                val imageRef = storage.reference.child("users/$userId/pets/$petId.jpg")
                                imageRef.putBytes(imageData).await()
                                uploadedUrl = imageRef.downloadUrl.await().toString()
                            }

                            val petData = mapOf(
                                "name" to name,
                                "age" to age,
                                "breed" to breed,
                                "gender" to gender,
                                "personality" to personality,
                                "imageUrl" to (uploadedUrl ?: ""),
                                "ownerProfileId" to userId
                            )

                            docRef.set(petData).await()
                            Toast.makeText(context, "Pet added!", Toast.LENGTH_SHORT).show()

                            // Refresh pets
                            val snapshot = db.collection("users").document(userId).collection("pets").get().await()
                            pets = snapshot.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }
                            onPetsUpdated()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        } finally {
                            isSaving = false
                            showAddPetDialog = false
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    editingPet?.let { pet ->
        var isSaving by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf(pet["name"]?.toString() ?: "") }
        var age by remember { mutableStateOf(pet["age"]?.toString() ?: "") }
        var breed by remember { mutableStateOf(pet["breed"]?.toString() ?: "") }
        var gender by remember { mutableStateOf(pet["gender"]?.toString() ?: "") }
        var personality by remember { mutableStateOf(pet["personality"]?.toString() ?: "") }
        var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
        val existingImageUrl = pet["imageUrl"]?.toString()
        LaunchedEffect(existingImageUrl) {
            if (imageBitmap == null && !existingImageUrl.isNullOrEmpty()) {
                try {
                    val ref = FirebaseStorage.getInstance().getReferenceFromUrl(existingImageUrl)
                    val bytes = ref.getBytes(100 * 1024).await()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageBitmap = bmp.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                imageBitmap = bitmap?.asImageBitmap()
                Toast.makeText(context, "Image selected!", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog(
            onDismissRequest = { editingPet = null },
            title = { Text("Edit Pet") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 500.dp)
                ) {
                    if (isSaving) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 500.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
                                        .clickable { imagePicker.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (imageBitmap != null) {
                                        Image(
                                            bitmap = imageBitmap!!,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("Tap to change image", color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") })
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed") })
                                Spacer(modifier = Modifier.height(12.dp))
                                val genderOptions = listOf("Male", "Female", "Other", "Unknown")
                                var genderDropdownExpanded by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                                        .clickable { genderDropdownExpanded = true }
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = if (gender.isNotBlank()) gender else "Select Gender",
                                        color = if (gender.isNotBlank()) Color.Black else Color.Gray
                                    )
                                }
                                DropdownMenu(
                                    expanded = genderDropdownExpanded,
                                    onDismissRequest = { genderDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                gender = option
                                                genderDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(value = personality, onValueChange = { personality = it }, label = { Text("Personality/Vaccination") })

                                if (isSaving) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                    return@AlertDialog
                                }
                            }
                        }
                    }
                }
            }

            ,
            confirmButton = {
                TextButton(onClick = {
                    isSaving = true
                    scope.launch {
                        try {
                            val docRef = db.collection("users").document(userId!!).collection("pets").document(pet["id"].toString())

                            var uploadedUrl = pet["imageUrl"]?.toString()
                            if (imageBitmap != null) {
                                val baos = ByteArrayOutputStream()
                                val bitmap = imageBitmapToAndroidBitmap(imageBitmap!!, 640, 640)
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                val imageData = baos.toByteArray()
                                val imageRef = storage.reference.child("users/$userId/pets/${pet["id"]}.jpg")
                                imageRef.putBytes(imageData).await()
                                uploadedUrl = imageRef.downloadUrl.await().toString()
                            }

                            val updatedData = mapOf(
                                "name" to name,
                                "age" to age,
                                "breed" to breed,
                                "gender" to gender,
                                "personality" to personality,
                                "imageUrl" to uploadedUrl,
                                "ownerProfileId" to userId
                            )

                            docRef.set(updatedData).await()

                            // Refresh pet list
                            val snapshot = db.collection("users").document(userId).collection("pets").get().await()
                            pets = snapshot.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }
                            onPetsUpdated()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        } finally {
                            isSaving = false
                            editingPet = null
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPet = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}