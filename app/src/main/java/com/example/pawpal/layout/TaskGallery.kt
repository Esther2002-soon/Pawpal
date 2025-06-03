package com.example.pawpal.layout

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.pawpal.components.TaskCard
import com.example.pawpal.utils.showDateTimePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.pawpal.components.SelectTaskDialog
import kotlinx.coroutines.delay
import com.google.firebase.firestore.SetOptions

@Composable
fun TaskGallery(userId: String, pets: List<Map<String, Any>>) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    var tasks by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedTask by remember { mutableStateOf<Map<String, Any>?>(null) }
    val scope = rememberCoroutineScope()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTaskToEdit by remember { mutableStateOf<Map<String, Any>?>(null) }
    var pets by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(userId) {
        db.collection("users").document(userId).collection("pets").get()
            .addOnSuccessListener { result ->
                pets = result.documents.mapNotNull { doc -> doc.data?.plus("id" to doc.id) }
            }

        db.collection("users").document(userId).collection("task").get().addOnSuccessListener { snapshot ->
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            val validTasks = mutableListOf<Map<String, Any>>()

            snapshot.documents.forEach { doc ->
                val data = doc.data ?: return@forEach
                val endTimeStr = data["endTime"]?.toString()
                val id = doc.id

                val endTime = try {
                    endTimeStr?.let { dateFormat.parse(it)?.time }
                } catch (e: Exception) {
                    null
                }

                if (endTime != null && endTime < now) {
                    // ❌ Task has expired → delete from Firebase
                    db.collection("users").document(userId).collection("task").document(id).delete()
                } else {
                    // ✅ Keep it
                    validTasks.add(data + ("id" to id))
                }
            }

            tasks = validTasks
        }
    }

    fun refreshAndCleanTasks() {
        db.collection("users").document(userId).collection("task").get()
            .addOnSuccessListener { snapshot ->
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val validTasks = mutableListOf<Map<String, Any>>()

                snapshot.documents.forEach { doc ->
                    val data = doc.data ?: return@forEach
                    val endTimeStr = data["endTime"]?.toString()
                    val id = doc.id

                    val endTime = try {
                        endTimeStr?.let { dateFormat.parse(it)?.time }
                    } catch (e: Exception) {
                        null
                    }

                    if (endTime != null && endTime < now) {
                        db.collection("users").document(userId).collection("task").document(id).delete()
                    } else {
                        validTasks.add(data + ("id" to id))
                    }
                }

                tasks = validTasks
            }
    }
    LaunchedEffect(Unit) {
        while (true) {
            refreshAndCleanTasks()
            delay(5 * 60 * 1000) // every 5 minutes
        }
    }
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        // Task cards
        tasks.forEach { task ->
            TaskCard(
                task = task,
                onClick = { selectedTask = task },
                onEdit = { selectedTaskToEdit = task },
                onDelete = {
                    scope.launch {
                        db.collection("users").document(userId).collection("task")
                            .document(task["id"].toString()).delete().await()
                        tasks = tasks.filterNot { it["id"] == task["id"] }
                    }
                }
            )
        }

        // Add New Task Card
        Box(
            modifier = Modifier
                .size(140.dp)
                .padding(8.dp)
                .clickable {
                    showAddTaskDialog = true
                }
                .border(1.dp, Color.Gray, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text("+", style = MaterialTheme.typography.headlineMedium)
        }
    }

    selectedTask?.let { task ->
        val petInfo = (task["petInfo"] as? Map<*, *>)?.mapKeys { it.key.toString() } ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        SelectTaskDialog(
            task = task,
            pet = petInfo as Map<String, Any>,
            onDismiss = { selectedTask = null }
        )
    }

    if (showAddTaskDialog || selectedTaskToEdit != null) {
        var selectedPetId by remember { mutableStateOf("") }
        var taskTitle by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var latLng by remember { mutableStateOf<LatLng?>(null) }
        var isSaving by remember { mutableStateOf(false) }
        var taskDescription by remember { mutableStateOf("") }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        var startCalendar by remember { mutableStateOf(Calendar.getInstance()) }
        var endCalendar by remember { mutableStateOf(Calendar.getInstance()) }
        val editing = selectedTaskToEdit != null
        var showMapDialog by remember { mutableStateOf(false) }
        var customLocationDescription by remember { mutableStateOf("") }

        LaunchedEffect(selectedTaskToEdit) {
            selectedTaskToEdit?.let { task ->
                location = task["location"]?.toString() ?: ""
                taskTitle = task["title"]?.toString() ?: ""
                taskDescription = task["description"]?.toString() ?: ""
                customLocationDescription = task["manualLocation"]?.toString() ?: ""
                selectedPetId = task["petId"]?.toString() ?: ""

                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                try {
                    task["startTime"]?.toString()?.let {
                        startCalendar.time = format.parse(it) ?: startCalendar.time
                    }
                    task["endTime"]?.toString()?.let {
                        endCalendar.time = format.parse(it) ?: endCalendar.time
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        AlertDialog(
            onDismissRequest = {
                showAddTaskDialog = false
                selectedTaskToEdit = null
            },
            title = { Text(if (editing) "Edit Task" else "Add Task") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 500.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(value = taskTitle, onValueChange = { taskTitle = it }, label = { Text("Task Title") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = taskDescription, onValueChange = { taskDescription = it }, label = { Text("Description") })
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Start: ${dateFormat.format(startCalendar.time)}")
                    Button(onClick = {
                        showDateTimePicker(context, startCalendar) { selected ->
                            startCalendar = selected
                        }
                    }) {
                        Text("Pick Start Time")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("End: ${dateFormat.format(endCalendar.time)}")
                    Button(onClick = {
                        showDateTimePicker(context, endCalendar) { selected ->
                            endCalendar = selected
                        }
                    }) {
                        Text("Pick End Time")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customLocationDescription,
                        onValueChange = { customLocationDescription = it },
                        label = { Text("Location Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location (Enter or auto-detect)") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    val cameraPositionState = rememberCameraPositionState()
                    var markerPosition by remember { mutableStateOf(latLng ?: LatLng(0.0, 0.0)) }
                    val markerState = rememberMarkerState(position = markerPosition)

                    LaunchedEffect(latLng) {
                        latLng?.let {
                            markerPosition = it
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
                        }
                    }

                    Button(
                        onClick = { showMapDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Location")
                    }

                    LaunchedEffect(markerState.position) {
                        if (!editing) {
                            latLng = markerState.position
                            location = "Lat: %.5f, Lng: %.5f".format(
                                markerState.position.latitude,
                                markerState.position.longitude
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    var petDropdownExpanded by remember { mutableStateOf(false) }

                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                                .clickable { petDropdownExpanded = true }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = pets.find { it["id"].toString() == selectedPetId }?.get("name")?.toString()
                                    ?: "Select a pet",
                                color = if (selectedPetId.isBlank()) Color.Gray else Color.Black
                            )
                        }

                        DropdownMenu(
                            expanded = petDropdownExpanded,
                            onDismissRequest = { petDropdownExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp) // This enables scrolling natively
                        ) {
                            pets.forEach { pet ->
                                DropdownMenuItem(
                                    text = { Text(pet["name"].toString()) },
                                    onClick = {
                                        selectedPetId = pet["id"].toString()
                                        petDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (isSaving) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedPetId.isBlank()) {
                        Toast.makeText(context, "Please select a pet", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    isSaving = true
                    scope.launch {
                        try {
                            val pet = pets.find { it["id"].toString() == selectedPetId } ?: return@launch
                            val taskCollection = db.collection("users").document(userId).collection("task")
                            val docRef = selectedTaskToEdit?.get("id")?.let {
                                taskCollection.document(it.toString())
                            } ?: taskCollection.document()
                            val taskId = docRef.id

                            val taskData = mapOf(
                                "title" to taskTitle,
                                "description" to taskDescription,
                                "startTime" to dateFormat.format(startCalendar.time),
                                "endTime" to dateFormat.format(endCalendar.time),
                                "location" to location,
                                "petId" to selectedPetId,
                                "petInfo" to pet,
                                "ownerProfileId" to userId,
                                "lat" to latLng?.latitude,
                                "lng" to latLng?.longitude,
                                "manualLocation" to customLocationDescription,
                            )

                            val cleanedTaskData = taskData.filterValues { it != null }
                            docRef.set(cleanedTaskData, SetOptions.merge()).await()


                            val snapshot = db.collection("users").document(userId).collection("task").get().await()
                            tasks = snapshot.documents.mapNotNull { it.data?.plus("id" to it.id) }

                            showAddTaskDialog = false
                            selectedTaskToEdit = null

                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        } finally {
                            isSaving = false
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddTaskDialog = false
                    selectedTaskToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
        if (showMapDialog) {
            Dialog(onDismissRequest = { showMapDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val placesClient = remember { Places.createClient(context) }
                    val mapUiSettings = remember { MapUiSettings(zoomControlsEnabled = true) }
                    val mapProperties = remember { MapProperties(isMyLocationEnabled = true) }
                    val cameraPositionState = rememberCameraPositionState()

                    var searchQuery by remember { mutableStateOf("") }
                    val markerState = rememberMarkerState(position = latLng ?: LatLng(0.0, 0.0))

                    LaunchedEffect(latLng) {
                        latLng?.let {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 17f)
                        }
                    }

                    val taiwanBounds = LatLngBounds(
                        LatLng(21.8, 119.3),  // Southwest corner
                        LatLng(25.4, 122.1)   // Northeast corner
                    )
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = mapUiSettings,
                        properties = mapProperties,
                        onMapLoaded = {
                            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(taiwanBounds, 50))
                        },
                        onMapClick = {
                            latLng = it
                            markerState.position = it
                            location = "Lat: ${it.latitude}, Lng: ${it.longitude}" // optional display only
                        }
                    ) {
                        Marker(
                            state = markerState,
                            draggable = true
                        )
                    }
                    LaunchedEffect(markerState.position) {
                        latLng = markerState.position
                        location = "Lat: ${latLng!!.latitude}, Lng: ${latLng!!.longitude}"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 52.dp, start = 16.dp, end = 16.dp)
                            .background(Color.White, shape = MaterialTheme.shapes.small)
                            .padding(8.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search address or place") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    val placesClient = Places.createClient(context)

                                    val request = FindAutocompletePredictionsRequest.builder()
                                        .setQuery(searchQuery)
                                        .setCountries(listOf("TW"))
                                        .build()

                                    placesClient.findAutocompletePredictions(request)
                                        .addOnSuccessListener { response ->
                                            if (response.autocompletePredictions.isNotEmpty()) {
                                                val placeId = response.autocompletePredictions[0].placeId
                                                val fetchRequest = FetchPlaceRequest.builder(
                                                    placeId,
                                                    listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)
                                                ).build()

                                                placesClient.fetchPlace(fetchRequest)
                                                    .addOnSuccessListener { placeResponse ->
                                                        val place = placeResponse.place
                                                        val coords = place.latLng
                                                        if (coords != null) {
                                                            latLng = coords
                                                            markerState.position = coords
                                                            location = place.address ?: searchQuery
                                                        }
                                                    }
                                            } else {
                                                Toast.makeText(context, "No results found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Place search failed", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        ) {
                            Text("Search")
                        }
                    }

                    IconButton(
                        onClick = { showMapDialog = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.White, shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close map")
                    }
                }
            }
        }
    }
}