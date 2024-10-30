package com.herbig.ignis.location

import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.core.content.ContextCompat
import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Application
import android.os.Build
import androidx.activity.result.ActivityResultLauncher

enum class PermissionState {
    NONE,
    FOREGROUND,
    BACKGROUND
}

/**
 * A simple class that handles permissions state and requests to grant permissions.
 */
class PermissionRequestHandler(private val context: Application) {

    private val _permissionsState = mutableStateOf(PermissionState.NONE)
    val permissionsState: State<PermissionState> = _permissionsState

    init {
        updatePermissionsState()
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun foregroundGranted(): Boolean {
        // either permission allows for requesting background location, even though coarse will be less accurate
        return checkPermission(ACCESS_FINE_LOCATION) || checkPermission(ACCESS_COARSE_LOCATION)
    }

    fun requestPermissions(permissionLauncher: ActivityResultLauncher<Array<String>>) {

        updatePermissionsState()

        when (_permissionsState.value) {
            PermissionState.NONE -> {
                // TODO POST_NOTIFICATIONS was thrown in here last minute when I realized I need it
                // for the persistent notification on API > 33.  This class should be updated to
                // account for users denying that permission.
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS)
                } else {
                    arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
                }
                permissionLauncher.launch(permissions)
            }
            PermissionState.FOREGROUND -> {
                // request background permission if foreground is already granted
                permissionLauncher.launch(arrayOf(ACCESS_BACKGROUND_LOCATION))
            }
            PermissionState.BACKGROUND -> {
                // background permission already granted, all good
            }
        }
    }

    fun updatePermissionsState() {
        _permissionsState.value = when {
            checkPermission(ACCESS_BACKGROUND_LOCATION) -> PermissionState.BACKGROUND
            foregroundGranted() -> PermissionState.FOREGROUND
            else -> PermissionState.NONE
        }
    }
}