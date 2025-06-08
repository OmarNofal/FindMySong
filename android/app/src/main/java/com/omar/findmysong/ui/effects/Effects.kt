package com.omar.findmysong.ui.effects

import android.app.Activity
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.omar.findmysong.ui.screen.HomeScreenViewModel
import kotlinx.coroutines.flow.Flow

@Composable
fun DarkStatusBarEffect() {
    val view = LocalView.current
    DisposableEffect(Unit) {

        val window = (view.context as Activity).window


        val windowsInsetsController = WindowCompat.getInsetsController(window, view)
        val previous = windowsInsetsController.isAppearanceLightStatusBars


        windowsInsetsController.isAppearanceLightStatusBars = false
        windowsInsetsController.isAppearanceLightNavigationBars = false

        onDispose {
            windowsInsetsController.isAppearanceLightStatusBars = previous
            windowsInsetsController.isAppearanceLightNavigationBars = previous
        }
    }
}


@Composable
fun VibrateEffect(state: HomeScreenViewModel.State, events: Flow<HomeScreenViewModel.Event>) {

    val context = LocalContext.current


    LaunchedEffect(Unit) {
        events.collect {
            val shouldVibrate =
                it is HomeScreenViewModel.Event.SongNotFound || it is HomeScreenViewModel.Event.ScheduledForOfflineRecognition
            if (shouldVibrate) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(0, 70, 80, 70) // delay, vibrate, pause, vibrate
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 means no repeat
            }
        }
    }

    LaunchedEffect(state) {
        if (state is HomeScreenViewModel.State.Found) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(600L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(Unit) {
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation
        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}