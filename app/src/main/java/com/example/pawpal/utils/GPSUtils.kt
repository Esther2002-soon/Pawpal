package com.example.pawpal.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.model.RectangularBounds

@SuppressLint("MissingPermission")
suspend fun getCurrentLocation(context: Context): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    return suspendCancellableCoroutine { cont ->
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                cont.resume(location)
            } else {
                val request = LocationRequest.create().apply {
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    interval = 0
                    numUpdates = 1
                }
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        cont.resume(result.lastLocation)
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
        }
    }
}

fun getAddressFromCoordinates(context: Context, latitude: Double, longitude: Double): String? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addressList = geocoder.getFromLocation(latitude, longitude, 1)
    return addressList?.firstOrNull()?.getAddressLine(0)
}