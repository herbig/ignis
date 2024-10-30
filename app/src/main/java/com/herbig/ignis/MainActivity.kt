package com.herbig.ignis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herbig.ignis.location.LocationLoggingService
import com.herbig.ignis.location.PermissionRequestHandler
import com.herbig.ignis.location.PermissionState
import com.herbig.ignis.ui.theme.IgnisTheme
import com.herbig.ignis.ui.theme.ignisOrange

/**
 * This class should ideally be using a ViewModel class to manage these dependencies and provide
 * state and actions to the UI, rather than embedding everything in here.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionRequestHandler = PermissionRequestHandler(application)

        // register an ActivityResultLauncher for permission requests
        val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // update the permission state
            permissionRequestHandler.updatePermissionsState()
        }

        setContent {
            IgnisTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val permissionsState by permissionRequestHandler.permissionsState

                    // trigger service start when permissionsState becomes BACKGROUND
                    LaunchedEffect(permissionsState) {
                        if (permissionsState == PermissionState.BACKGROUND) {
                            val intent = Intent(this@MainActivity, LocationLoggingService::class.java)
                            startForegroundService(intent)
                        }
                    }

                    val explainerText = when (permissionsState) {
                        PermissionState.NONE -> "This app requires a few sets of permissions: general location tracking and notifications, " +
                                "then background location tracking.\n\nLet's start with the first two!"
                        PermissionState.FOREGROUND -> "Foreground location access is granted.  You'll also need to turn on background " +
                                "location permission (e.g. \"Allow all the time\")."
                        PermissionState.BACKGROUND -> "All permissions granted."
                    }
                    val buttonText = when (permissionsState) {
                        PermissionState.NONE -> "Request Permissions"
                        PermissionState.FOREGROUND -> "Allow Always"
                        PermissionState.BACKGROUND -> ""
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = ignisOrange
                            ),
                        )

                        Text(
                            text = explainerText,
                            modifier = Modifier.padding(16.dp)
                        )

                        if (permissionsState != PermissionState.BACKGROUND) {
                            Button(
                                onClick = {
                                    permissionRequestHandler.requestPermissions(permissionLauncher)
                                },
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(buttonText)
                            }
                        } else {
                            SyncedLocationsList(
                                // TODO this is super janky, state should be provided by a ViewModel
                                (application as IgnisApp).syncManager,
                                (application as IgnisApp).database
                            )
                        }
                    }
                }
            }
        }
    }
}
