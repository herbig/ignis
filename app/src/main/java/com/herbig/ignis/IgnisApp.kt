package com.herbig.ignis

import android.app.Application
import com.herbig.ignis.database.LocationDB
import com.herbig.ignis.database.RoomLocationDB
import com.herbig.ignis.network.LocationSyncManager
import com.herbig.ignis.network.LocationSyncer
import com.herbig.ignis.network.NetworkHandler
import com.herbig.ignis.network.RetrofitNetworkHandler

class IgnisApp : Application() {

    val database: LocationDB by lazy {
        RoomLocationDB(this)
    }

    val networkHandler: NetworkHandler by lazy {
        RetrofitNetworkHandler()
    }

    val syncManager: LocationSyncManager by lazy {
        LocationSyncer(this, database, networkHandler)
    }
}
