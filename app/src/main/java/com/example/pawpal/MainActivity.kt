package com.example.pawpal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.pawpal.navigation.AppNavGraph
import com.example.pawpal.ui.theme.PawpalTheme
import com.google.firebase.FirebaseApp
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Places.initialize(applicationContext, "AIzaSyAOlcfB-2SUrKshu8s6TjBpYvnlUJbjnTQ")
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) {
            com.example.pawpal.navigation.Routes.LAUNCHER // 👈 NEW temporary screen
        } else {
            com.example.pawpal.navigation.Routes.LOGIN
        }

        setContent {
            PawpalTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, startDestination = startDestination)
            }
        }
    }
}