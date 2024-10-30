package com.herbig.ignis.location

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.herbig.ignis.IgnisApp
import com.herbig.ignis.MainActivity
import com.herbig.ignis.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A Service that continually polls the user's location and logs it to a local database.
 */
class LocationLoggingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP_SERVICE = "com.herbig.ignis.ACTION_STOP_SERVICE"
        private const val CHANNEL_ID = "location_channel"
    }

    private val database by lazy {
        (application as IgnisApp).database
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest
            .Builder(15000)
            .setMinUpdateIntervalMillis(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach {
                    // save the location result to the database
                    scope.launch {
                        database.saveLocation(it.latitude, it.longitude)
                    }
                }
            }
        }

        startLocationTracking()

        // start the service in the foreground with a persistent notification
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun startLocationTracking() {
        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            // we're in a state that shouldn't occur, if our permissions handling is done properly
            throw IllegalStateException("Attempting to track location without ACCESS_BACKGROUND_LOCATION permission.")
        }
    }

    private fun createNotification(): Notification {

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        // tap the notification to open the app
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // stop the service entirely when the action button is clicked
        val stopIntent = Intent(this, LocationLoggingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking")
            .setContentText("Logging your location.")
            .setSmallIcon(R.drawable.notification)
            .setContentIntent(mainActivityPendingIntent)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_stop,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            scope.cancel()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
