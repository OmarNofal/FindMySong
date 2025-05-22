package com.omar.findmysong.ui.permission

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class PermissionHandler(
    val askForPermission: () -> Unit,
    val permissionGranted: Boolean
)

@Composable
fun rememberPermissionHandler(
    permission: String,
    onAccepted: () -> Unit,
    onDeclined: () -> Unit,
    onShowRationale: () -> Unit,
): PermissionHandler {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (isGranted) {
            onAccepted()
        } else {
            onDeclined()
        }
    }

    val askForPermission = {
        launcher.launch(permission)
    }

    return PermissionHandler(
        askForPermission = {
            val shouldShowRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, permission)
            if (shouldShowRationale)
                onShowRationale()
            else
                askForPermission()
        },
        permissionGranted = permissionGranted
    )
}
