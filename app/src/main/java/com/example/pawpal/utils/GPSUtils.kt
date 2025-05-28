package com.example.pawpal.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import android.location.LocationManager
import android.graphics.Bitmap

import android.provider.Settings@SuppressLint("MissingPermission")

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
fun isGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}