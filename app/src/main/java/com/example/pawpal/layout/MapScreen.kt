package com.example.pawpal.layout

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import com.example.pawpal.utils.getCurrentLocation
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import com.example.pawpal.components.OwnerCard
import com.example.pawpal.components.SelectTaskDialog
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.pawpal.components.FilterFAB
import java.util.Calendar
import com.example.pawpal.utils.showDateTimePicker
import com.example.pawpal.utils.isGpsEnabled

@Composable
fun MapScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var taskMarkers by remember { mutableStateOf<List<Pair<Map<String, Any>, Map<String, Any>>>>(emptyList()) }
    var selectedTask by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedPet by remember { mutableStateOf<Map<String, Any>?>(null) }
    var selectedOwnerId by remember { mutableStateOf<String?>(null) }
    var showOwnerCard by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var pendingTaskForOwnerCard by remember { mutableStateOf<Map<String, Any>?>(null) }
    var pendingPetForOwnerCard by remember { mutableStateOf<Map<String, Any>?>(null) }

    val cameraPositionState = rememberCameraPositionState()
    val isGpsEnabled = isGpsEnabled(context)
    var showGpsDialog by remember { mutableStateOf(!isGpsEnabled) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var userLocation by remember { mutableStateOf<Location?>(null) }

    var keyword by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }
    var filterStartCal by remember { mutableStateOf<Calendar?>(null) }
    var filterEndCal by remember { mutableStateOf<Calendar?>(null) }
    var maxDistanceKm by remember { mutableStateOf(10000f) }
    var distanceInput by remember { mutableStateOf("50") }
    var showFilterDialog by remember { mutableStateOf(false) }

    val filterStartTime = filterStartCal?.let { dateFormat.format(it.time) } ?: ""
    val filterEndTime = filterEndCal?.let { dateFormat.format(it.time) } ?: ""

    LaunchedEffect(Unit) {
        val location = getCurrentLocation(context)
        val latLng = LatLng(location?.latitude ?: 23.6978, location?.longitude ?: 120.9605)
        currentLocation = latLng
        userLocation = location
        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
    }

    LaunchedEffect(Unit) {
        try {
            val allUsers = db.collection("users").get().await()
            val allTasks = mutableListOf<Pair<Map<String, Any>, Map<String, Any>>>()
            for (user in allUsers) {
                val userId = user.id
                val petDocs = db.collection("users").document(userId).collection("pets").get().await()
                    .associateBy { it.id }

                val taskDocs = db.collection("users").document(userId).collection("task").get().await()
                for (taskDoc in taskDocs) {
                    val task = taskDoc.data + ("id" to taskDoc.id)
                    val petId = task["petId"]?.toString()
                    val petData = petDocs[petId]?.data?.plus("id" to petId)
                    if (task["lat"] != null && task["lng"] != null && petData != null) {
                        val taskMap = task as? Map<String, Any>
                        val petMap = petData as? Map<String, Any>
                        if (taskMap != null && petMap != null) {
                            allTasks.add(taskMap to petMap)
                        }
                    }
                }
            }
            taskMarkers = allTasks
        } catch (e: Exception) {
            Log.e("MapScreen", "Failed to fetch tasks: ${e.message}")
        }
    }
    val filteredTasks = taskMarkers.filter { (task, pet) ->
        val combinedText = listOf(
            task["title"] as? String,
            task["description"] as? String,
            task["manualLocation"] as? String,
            pet["name"] as? String,
            pet["breed"] as? String,
            pet["personality"] as? String
        ).joinToString(" ") { it?.lowercase() ?: "" }

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
            val taskStart = (task["startTime"] as? String)?.let { dateFormat.parse(it)?.time } ?: 0
            val taskEnd = (task["endTime"] as? String)?.let { dateFormat.parse(it)?.time } ?: 0
            val filterStart = if (filterStartTime.isNotBlank()) {
                dateFormat.parse(filterStartTime)?.time
            } else {
                Long.MIN_VALUE
            } ?: Long.MIN_VALUE

            val filterEnd = if (filterEndTime.isNotBlank()) {
                dateFormat.parse(filterEndTime)?.time
            } else {
                Long.MAX_VALUE
            } ?: Long.MAX_VALUE

            taskEnd >= filterStart && taskStart <= filterEnd
        }

        matchKeyword && matchDistance && matchTime
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                filteredTasks.forEach { (task, pet) ->
                    val lat = task["lat"] as? Double ?: return@forEach
                    val lng = task["lng"] as? Double ?: return@forEach

                    val jitteredLat = lat + ((-0.0001..0.0001).random())
                    val jitteredLng = lng + ((-0.0001..0.0001).random())
                    val latLng = LatLng(jitteredLat, jitteredLng)

                    Marker(
                        state = MarkerState(position = latLng),
                        title = task["title"].toString(),
                        snippet = pet["name"].toString(),
                        onClick = {
                            selectedTask = task
                            selectedPet = pet

                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()

                                // Now show the new one
                                snackbarHostState.showSnackbar(
                                    message = "View task owner",
                                    actionLabel = "Open",
                                    withDismissAction = true
                                ).also { result ->
                                    if (result == SnackbarResult.ActionPerformed) {
                                        val ownerId = pet["ownerProfileId"]?.toString()
                                        if (ownerId != null) {
                                            selectedOwnerId = ownerId
                                            pendingTaskForOwnerCard = task
                                            pendingPetForOwnerCard = pet
                                            showOwnerCard = true
                                        }
                                    }
                                }
                            }
                            false
                        }
                    )
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

    if (selectedTask != null && selectedPet != null) {
        SelectTaskDialog(
            task = selectedTask!!,
            pet = selectedPet!!,
            onDismiss = {
                selectedTask = null
                selectedPet = null
            }
        )
    }

    if (showOwnerCard && selectedOwnerId != null) {
        OwnerCard(
            navController = navController,
            onDismiss = { showOwnerCard = false },
            profileUrl = null,
            backgroundUrl = null,
            username = "Loading...",
            bio = "",
            role = "",
            gender = "",
            location = "",
            ownerId = selectedOwnerId!!,
            preloadTaskInfo = mapOf(
                "taskTitle" to (pendingTaskForOwnerCard?.get("title") ?: ""),
                "taskId" to (pendingTaskForOwnerCard?.get("id") ?: ""),
                "petName" to (pendingPetForOwnerCard?.get("name") ?: ""),
                "manualLocation" to (pendingTaskForOwnerCard?.get("manualLocation") ?: ""),
                "lat" to (pendingTaskForOwnerCard?.get("lat") ?: ""),
                "lng" to (pendingTaskForOwnerCard?.get("lng") ?: "")
            )
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
fun ClosedFloatingPointRange<Double>.random(): Double {
    return start + (endInclusive - start) * Math.random()
}