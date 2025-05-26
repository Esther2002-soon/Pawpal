package com.example.pawpal.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TaskCard(
    task: Map<String, Any>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(140.dp)
            .padding(8.dp)
            .border(1.dp, Color.Gray, MaterialTheme.shapes.medium)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(task["title"].toString(), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pet: ${(task["petInfo"] as? Map<*, *>)?.get("name") ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(2.dp))
            Text("${task["startTime"]}", style = MaterialTheme.typography.bodySmall)
        }

        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}