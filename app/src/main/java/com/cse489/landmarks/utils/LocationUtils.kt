package com.cse489.landmarks.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtils {

    // Default Bangladesh Coordinates (Dhaka)
    const val DEFAULT_LAT = 23.8103
    const val DEFAULT_LON = 90.4125

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location {
        return suspendCancellableCoroutine { continuation ->
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(location)
                    } else {
                        // Fallback to LocationManager
                        val locationManager =
                            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val gpsLocation =
                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        val netLocation =
                            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                        val bestLocation = gpsLocation ?: netLocation
                        if (bestLocation != null) {
                            continuation.resume(bestLocation)
                        } else {
                            val defaultLoc = Location("default").apply {
                                latitude = DEFAULT_LAT
                                longitude = DEFAULT_LON
                            }
                            continuation.resume(defaultLoc)
                        }
                    }
                }.addOnFailureListener {
                    val defaultLoc = Location("default").apply {
                        latitude = DEFAULT_LAT
                        longitude = DEFAULT_LON
                    }
                    continuation.resume(defaultLoc)
                }
            } catch (e: Exception) {
                val defaultLoc = Location("default").apply {
                    latitude = DEFAULT_LAT
                    longitude = DEFAULT_LON
                }
                continuation.resume(defaultLoc)
            }
        }
    }
}
