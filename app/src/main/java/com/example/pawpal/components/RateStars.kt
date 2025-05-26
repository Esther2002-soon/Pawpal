package com.example.pawpal.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.Arrangement

@Composable
fun RatingRow(userId: String?, centered: Boolean = false) {
    val db = FirebaseFirestore.getInstance()
    var rating by remember { mutableStateOf(0.0) }
    var ratedBy by remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        if (!userId.isNullOrEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    rating = doc.getDouble("rate") ?: 0.0
                    ratedBy = (doc.getLong("ratedBy") ?: 0L).toInt()
                }
        }
    }

    val fullStars = rating.toInt()
    val emptyStars = 5 - fullStars

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        repeat(fullStars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Full Star",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(17.dp)
            )
        }

        repeat(emptyStars) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Empty Star",
                tint = Color.LightGray,
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = String.format("%.1f (%d)", rating, ratedBy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}