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
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Color.Black)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            textStyle = TextStyle(color = Color.Black)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val db = FirebaseFirestore.getInstance()

                            if (userId != null) {
                                db.collection("users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        val isComplete = document.getBoolean("profileComplete") ?: false

                                        if (isComplete) {
                                            navController.navigate("profile") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("setting") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Don't have an account? Register",
            modifier = Modifier.clickable {
                navController.navigate(Routes.REGISTER)
            }
        )
    }
}
