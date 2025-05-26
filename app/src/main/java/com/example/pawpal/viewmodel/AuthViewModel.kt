package com.example.pawpal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<String?>(null)
    val authState: StateFlow<String?> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _authState.value = if (task.isSuccessful) {
                        "success"
                    } else {
                        task.exception?.message ?: "Login failed"
                    }
                }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _authState.value = if (task.isSuccessful) {
                        "success"
                    } else {
                        task.exception?.message ?: "Registration failed"
                    }
                }
        }
    }

    fun clearAuthState() {
        _authState.value = null
    }
}
