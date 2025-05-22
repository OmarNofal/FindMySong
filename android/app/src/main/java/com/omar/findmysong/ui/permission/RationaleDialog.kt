package com.omar.findmysong.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext


data class RationaleDialog(
    val show: () -> Unit
)

@Composable
fun rememberMicrophoneRationaleDialog(): RationaleDialog {

    var isShown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val goToSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    if (isShown)
        AlertDialog(
            onDismissRequest = { isShown = false },
            confirmButton = {
                TextButton(goToSettings) {
                    Text("Grant Permission")
                }
            },
            title = {
                Text("Permission Required")
            },
            text = {
                Text("Microphone permission is required. Please grant it in the settings")
            },
            dismissButton = {
                TextButton(onClick = { isShown = false }) {
                    Text("Cancel")
                }
            },
            icon = {
                Icon(imageVector = Icons.Rounded.Mic, contentDescription = null)
            }
        )

    return RationaleDialog { isShown = true }
}