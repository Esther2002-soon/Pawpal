package com.example.pawpal.layout
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pawpal.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

@Composable
fun LauncherScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .get(Source.SERVER)
                .addOnSuccessListener { document ->
                    val isComplete = document.getBoolean("profileComplete") ?: false
                    if (isComplete) {
                        navController.navigate(Routes.PROFILE) {
                            popUpTo(Routes.LAUNCHER) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.SETTING) {
                            popUpTo(Routes.LAUNCHER) { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                    navController.navigate(Routes.SETTING) {
                        popUpTo(Routes.LAUNCHER) { inclusive = true }
                    }
                }
        } else {
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.LAUNCHER) { inclusive = true }
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
