package com.herbig.ignis.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.herbig.ignis.database.LocationDB
import com.herbig.ignis.database.LocationEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

interface LocationSyncManager {
    suspend fun syncPendingEntries()
    fun getSyncedLocationsFlow(): StateFlow<List<LocationEntity>>
}

/**
 * Handles syncing location data to a mock "backend".
 */
class LocationSyncer(
    context: Application,
    private val database: LocationDB,
    private val networkHandler: NetworkHandler,
): LocationSyncManager {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val syncedLocations = MutableStateFlow<List<LocationEntity>>(emptyList())
    private val syncedLocationsFlow: StateFlow<List<LocationEntity>> = syncedLocations.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // start sync if there are pending entries when the network becomes available
            scope.launch { syncPendingEntries() }
        }
    }

    init {

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            networkRequest,
            networkCallback
        )

        // observe database entries and sync if online
        scope.launch {
            database.getAllLocationsFlow()
                .filter { it.isNotEmpty() }
                .collect { entries ->
                    if (isOnline()) {
                        syncEntries(entries)
                    }
                }
        }
    }

    private suspend fun syncEntries(entries: List<LocationEntity>) {

        val success = networkHandler.syncLocationData(entries)

        // remove synced entries from the database and update synced locations list
        if (success) {
            database.clearLocations(entries.map { it.id })
            syncedLocations.value += entries
        }
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun syncPendingEntries() {
        val entries = database.getAllLocationsFlow().first()
        if (entries.isNotEmpty()) {
            syncEntries(entries)
        }
    }

    override fun getSyncedLocationsFlow(): StateFlow<List<LocationEntity>> {
        return syncedLocationsFlow
    }
}
