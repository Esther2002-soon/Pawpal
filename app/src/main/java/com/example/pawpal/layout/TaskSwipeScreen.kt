package com.example.pawpal.layout

import com.spartapps.swipeablecards.ui.SwipeableCardDirection
import com.spartapps.swipeablecards.ui.SwipeableCardsFactors
import com.spartapps.swipeablecards.ui.SwipeableCardsProperties
import com.spartapps.swipeablecards.ui.lazy.LazySwipeableCards
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import com.example.pawpal.utils.getCurrentLocation
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.withContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.spartapps.swipeablecards.state.rememberSwipeableCardsState
import com.spartapps.swipeablecards.ui.lazy.items
import com.example.pawpal.components.SwipeTaskCard
import com.example.pawpal.components.SelectTaskDialog
import androidx.navigation.NavController
import java.net.URLEncoder
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.pawpal.components.FilterFAB
import android.location.Location
import java.util.Calendar
import com.example.pawpal.utils.showDateTimePicker
import androidx.compose.foundation.layout.Row
import com.google.firebase.firestore.SetOptions
import androidx.compose.material3.Button
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import com.example.pawpal.utils.isGpsEnabled

@Composable
fun TaskSwipeScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val now = System.currentTimeMillis()

    var taskCards by remember { mutableStateOf(listOf<Pair<Map<String, Any>, Map<String, Any>>>()) }
    var selectedTask by remember { mutableStateOf<Pair<Map<String, Any>, Map<String, Any>>?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var swipeSessionId by remember { mutableStateOf(0) }
    var localIndex by remember { mutableStateOf(0) }
    var keyword by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    val calendarFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var filterStartCal by remember { mutableStateOf<Calendar?>(null) }
    var filterEndCal by remember { mutableStateOf<Calendar?>(null) }
    var maxDistanceKm by remember { mutableStateOf(10000f) }
    val filterStartTime = filterStartCal?.let { calendarFormat.format(it.time) } ?: ""
    val filterEndTime = filterEndCal?.let { calendarFormat.format(it.time) } ?: ""
    var showFilterDialog by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var distanceInput by remember { mutableStateOf("50") }
    val isGpsEnabled = isGpsEnabled(context)
    var showGpsDialog by remember { mutableStateOf(!isGpsEnabled) }

    LaunchedEffect(swipeSessionId) {
        val results = mutableListOf<Pair<Map<String, Any>, Map<String, Any>>>()
        val users = db.collection("users").get().await()
        userLocation = getCurrentLocation(context)

        for (user in users.documents) {
            val userId = user.id
            val petMap = db.collection("users").document(userId).collection("pets").get().await()
                .associateBy(
                    keySelector = { it.id },
                    valueTransform = { it.data ?: emptyMap() }
                )

            val taskDocs = db.collection("users").document(userId).collection("task").get().await()

            for (doc in taskDocs.documents) {
                val task = doc.data ?: continue
                task["id"] = doc.id
                if (task["title"] == null || task["petId"] == null) {
                    Log.w("TaskSwipe", "Missing critical fields in task: ${doc.id}")
                    continue
                }

                val endTime = try {
                    dateFormat.parse(task["endTime"]?.toString() ?: "")?.time ?: 0
                } catch (e: Exception) {
                    0
                }
                if (endTime > now) {
                    val petId = task["petId"]?.toString() ?: continue
                    val pet = petMap[petId] ?: continue
                    results.add(task to pet)
                }
            }
        }

        taskCards = results
    }
    val filteredTasks = taskCards.filter { (task, pet) ->
        val combinedText = listOf(
            task["title"] as? String,
            task["description"] as? String,
            task["manualLocation"] as? String,
            pet["name"] as? String,
            pet["breed"] as? String,
            pet["personality"] as? String
        ).joinToString(" ") { it?.toString()?.lowercase() ?: ""
        }

        val matchKeyword = keyword.isBlank() || keyword.lowercase() in combinedText

        val matchDistance = run {
            val taskLat = task["lat"] as? Double ?: return@run false
            val taskLng = task["lng"] as? Double ?: return@run false
            val userLoc = userLocation ?: return@run false

            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude, userLoc.longitude,
                taskLat, taskLng, results
            )
            results[0] / 1000 <= maxDistanceKm
        }

        val matchTime = run {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val taskStart = (task["startTime"] as? String)?.let { format.parse(it)?.time } ?: 0
            val taskEnd = (task["endTime"] as? String)?.let { format.parse(it)?.time } ?: 0
            val filterStart = if (filterStartTime.isNotBlank()) {
                format.parse(filterStartTime)?.time
            } else {
                Long.MIN_VALUE
            } ?: Long.MIN_VALUE

            val filterEnd = if (filterEndTime.isNotBlank()) {
                format.parse(filterEndTime)?.time
            } else {
                Long.MAX_VALUE
            } ?: Long.MAX_VALUE


            taskEnd >= filterStart && taskStart <= filterEnd
        }

        matchKeyword && matchDistance && matchTime
    }

    key(swipeSessionId) {
        val swipeState = rememberSwipeableCardsState { taskCards.size }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            LazySwipeableCards(
                state = swipeState,
                properties = SwipeableCardsProperties(),
                factors = SwipeableCardsFactors(),
                onSwipe = { (task, pet), direction ->
                    // Handle right swipe → send message + navigate
                    if (direction == SwipeableCardDirection.Right) {
                        val ownerId = pet["ownerProfileId"]?.toString()
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LazySwipeableCards

                        if (ownerId.isNullOrEmpty()) {
                            Toast.makeText(context, "Invalid owner ID", Toast.LENGTH_SHORT).show()
                        } else if (ownerId == currentUserId) {
                            Toast.makeText(context, "You can't swipe your own task!", Toast.LENGTH_SHORT).show()
                        } else {
                            val chatId = listOf(currentUserId, ownerId).sorted().joinToString("_")

                            scope.launch {
                                try {
                                    val taskId = task["id"]?.toString() ?: ""
                                    val title = task["title"] as? String ?: "Untitled"
                                    val petName = pet["name"]?.toString() ?: "Unknown"
                                    val location = task["manualLocation"]?.toString() ?: ""
                                    val lat = task["lat"] as? Double
                                    val lng = task["lng"] as? Double

                                    val taskInfoMessage = mapOf(
                                        "type" to "task_info",
                                        "senderId" to currentUserId,
                                        "timestamp" to System.currentTimeMillis(),
                                        "taskTitle" to title,
                                        "taskId" to task["id"],
                                        "petName" to petName,
                                        "manualLocation" to location,
                                        "lat" to lat,
                                        "lng" to lng,
                                        "source" to "swipe"
                                    )

                                    val chatDoc = db.collection("chats").document(chatId)
                                    chatDoc.collection("messages").add(taskInfoMessage).await()
                                    try {
                                        val senderRef = db.collection("users").document(currentUserId)
                                            .collection("chatRefs").document(chatId)
                                        val receiverRef = db.collection("users").document(ownerId)
                                            .collection("chatRefs").document(chatId)

                                        val now = System.currentTimeMillis()

                                        val chatRefDataForSender = mapOf(
                                            "chatId" to chatId,
                                            "with" to ownerId,
                                            "lastMessageTime" to now
                                        )
                                        val chatRefDataForReceiver = mapOf(
                                            "chatId" to chatId,
                                            "with" to currentUserId,
                                            "lastMessageTime" to now
                                        )

                                        senderRef.set(chatRefDataForSender, SetOptions.merge()).await()
                                        receiverRef.set(chatRefDataForReceiver, SetOptions.merge()).await()
                                        chatDoc.update("lastMessageTime", now).await()
                                    } catch (e: Exception) {
                                        Log.e("SwipeFirestore", "Failed to update lastMessageTime: ${e.message}", e)
                                    }
                                    val userDoc = db.collection("users").document(ownerId).get().await()
                                    val username = userDoc.getString("username") ?: "Unknown"
                                    val profileUrl = userDoc.getString("imageUrl") ?: ""

                                    val encodedUsername = URLEncoder.encode(username, "UTF-8")
                                    val encodedProfileUrl = URLEncoder.encode(profileUrl, "UTF-8")

                                    val route = "chat/$chatId/$ownerId/$encodedUsername/$encodedProfileUrl"
                                    withContext(Dispatchers.Main) {
                                        navController.navigate(route)
                                    }

                                } catch (e: Exception) {
                                    Log.e("Swipe", "Navigation failed: ${e.message}")
                                    Toast.makeText(context, "Failed to open chat", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    localIndex++
                    if (localIndex >= taskCards.size) {
                        localIndex = 0
                        taskCards = taskCards.shuffled().toList()
                        swipeSessionId++
                    }

                }
            ) {
                items(filteredTasks) { item, index, offset ->
                    val (task, pet) = item as Pair<Map<String, Any>, Map<String, Any>>

                    val swipeThreshold = 80f  // adjust sensitivity here

                    val swipeDirection = when {
                        offset.x > swipeThreshold -> SwipeableCardDirection.Right
                        offset.x < -swipeThreshold -> SwipeableCardDirection.Left
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SwipeTaskCard(
                            task = task,
                            pet = pet,
                            onClick = { selectedTask = item },
                            navController = navController,
                            direction = swipeDirection,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                        )
                        if (showFilterDialog) {
                            AlertDialog(
                                onDismissRequest = { showFilterDialog = false },
                                title = { Text("Set Filters") },
                                text = {
                                    Column {
                                        OutlinedTextField(
                                            value = keywordInput,
                                            onValueChange = { keywordInput = it },
                                            label = { Text("Keyword") },
                                            singleLine = true
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Distance:")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            OutlinedTextField(
                                                value = distanceInput,
                                                onValueChange = { distanceInput = it },
                                                label = { Text("km") },
                                                modifier = Modifier.width(100.dp),
                                                singleLine = true
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Slider(
                                            value = maxDistanceKm,
                                            onValueChange = {
                                                maxDistanceKm = it
                                                distanceInput = it.toInt().toString()
                                            },
                                            valueRange = 0f..1000f
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(onClick = {
                                            showDateTimePicker(context, Calendar.getInstance()) {
                                                filterStartCal = it
                                            }
                                        }) {
                                            Text("Select Start Time: ${filterStartTime.ifBlank { "Not Set" }}")
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(onClick = {
                                            showDateTimePicker(context, Calendar.getInstance()) {
                                                filterEndCal = it
                                            }
                                        }) {
                                            Text("Select End Time: ${filterEndTime.ifBlank { "Not Set" }}")
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        val parsed = distanceInput.toFloatOrNull()
                                        if (parsed != null) {
                                            maxDistanceKm = parsed.coerceIn(0f, 1000f)
                                        }
                                        keyword = keywordInput
                                        showFilterDialog = false
                                    }) {
                                        Text("Apply")
                                    }
                                }
                            )
                        }
                    }

                }
            }
            FilterFAB(
                onClick = { showFilterDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            )
        }

    }
    selectedTask?.let { (task, pet) ->
        SelectTaskDialog(
            task = task,
            pet = pet,
            onDismiss = { selectedTask = null }
        )
    }
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Location Disabled") },
            text = { Text("Please enable GPS to access this feature.") },
            confirmButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}