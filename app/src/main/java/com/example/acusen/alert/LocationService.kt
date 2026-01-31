package com.example.acusen.alert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Služba pro získání GPS lokace pro alert
 */
class LocationService(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Získá aktuální GPS lokaci s timeoutem
     */
    suspend fun getCurrentLocation(timeoutMs: Long = 10000): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        try {
            // Zkusí získat poslední známou lokaci
            val lastKnownLocation = getLastKnownLocation()
            if (lastKnownLocation != null && isLocationFresh(lastKnownLocation)) {
                return lastKnownLocation
            }

            // Požádá o novou lokaci
            return withTimeout(timeoutMs) {
                requestNewLocation()
            }

        } catch (e: TimeoutCancellationException) {
            // Timeout - vrátí poslední známou lokaci pokud existuje
            return getLastKnownLocation()
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun requestNewLocation(): Location? {
        val locationDeferred = CompletableDeferred<Location?>()

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                locationDeferred.complete(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {
                locationDeferred.complete(null)
            }
        }

        try {
            // Preferuje GPS, záložně síť
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            ).filter { provider ->
                locationManager.isProviderEnabled(provider)
            }

            if (providers.isEmpty()) {
                return null
            }

            // Požádá o lokaci z prvního dostupného poskytovatele
            locationManager.requestLocationUpdates(
                providers.first(),
                0L,
                0f,
                locationListener
            )

            return locationDeferred.await()

        } catch (e: SecurityException) {
            return null
        }
    }

    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var bestLocation: Location? = null

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val location = locationManager.getLastKnownLocation(provider)

                    if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                        bestLocation = location
                    }
                }
            } catch (e: SecurityException) {
                continue
            }
        }

        return bestLocation
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationFresh(location: Location): Boolean {
        val maxAge = 5 * 60 * 1000 // 5 minut
        return System.currentTimeMillis() - location.time < maxAge
    }
}
