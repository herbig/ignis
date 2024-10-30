package com.herbig.ignis.network

import android.util.Log
import com.herbig.ignis.database.LocationEntity
import kotlinx.coroutines.delay

interface NetworkHandler {
    suspend fun syncLocationData(locations: List<LocationEntity>): Boolean
}

class RetrofitNetworkHandler: NetworkHandler {

    companion object {
        private const val TAG = "RetrofitNetworkHandler"
    }

    /**
     * Mock network request, here would be where it hits a backend API with the latest
     * location data.
     */
    override suspend fun syncLocationData(locations: List<LocationEntity>): Boolean {
        return try {
            delay(500)
            locations.forEach {
                Log.d(TAG, "Synced: ${it.latitude}, ${it.longitude}")
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}