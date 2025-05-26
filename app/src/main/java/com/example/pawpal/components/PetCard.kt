package com.example.pawpal.components

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Composable
fun PetCard(
    pet: Map<String, Any>,
    onClick: (Map<String, Any>) -> Unit,
    onEdit: (Map<String, Any>) -> Unit,
    onDelete: (String) -> Unit
)
{
    val imageUrl = pet["imageUrl"] as? String
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        if (!imageUrl.isNullOrEmpty()) {
            val ref = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            ref.getBytes(100 * 1024)
                .addOnSuccessListener { bytes ->
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageBitmap = bmp.asImageBitmap()
                }
        }
    }

    Box(
        modifier = Modifier
            .size(140.dp)
            .padding(8.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick(pet) }
    ) {
        imageBitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "Pet Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Overlay info
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(pet["name"].toString(), style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text("${pet["age"]} y/o • ${pet["breed"]}", style = MaterialTheme.typography.bodySmall, color = Color.White)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit(pet)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        val petName = pet["name"]?.toString()
                        if (!userId.isNullOrEmpty() && !petName.isNullOrEmpty()) {
                            val petId = pet["id"]?.toString()
                            val imageUrl = pet["imageUrl"]?.toString()

                            if (!userId.isNullOrEmpty() && !petId.isNullOrEmpty()) {
                                val petRef = db.collection("users").document(userId).collection("pets").document(petId)

                                petRef.delete().addOnSuccessListener {
                                    if (!imageUrl.isNullOrEmpty()) {
                                        val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
                                        imageRef.delete()
                                    }
                                    onDelete(petId)
                                    Toast.makeText(context, "Deleted $petName", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
