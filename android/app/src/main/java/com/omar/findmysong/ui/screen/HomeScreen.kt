package com.omar.findmysong.ui.screen

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.omar.findmysong.R
import com.omar.findmysong.ui.effects.DarkStatusBarEffect
import com.omar.findmysong.ui.effects.LockScreenOrientation
import com.omar.findmysong.ui.effects.VibrateEffect
import com.omar.findmysong.ui.permission.rememberMicrophoneRationaleDialog
import com.omar.findmysong.ui.permission.rememberPermissionHandler
import com.omar.findmysong.ui.theme.ManropeFontFamily
import com.omar.findmysong.ui.visualizer.RipplesVisualizer
import com.omar.findmysong.visualizer.BeatWithData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow


@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState()
    val beatsFlow = viewModel.beatsFlow
    val events = viewModel.events

    HomeScreen(
        state,
        events,
        beatsFlow,
        start = viewModel::start,
        stop = viewModel::stop
    )
}

@Composable
fun HomeScreen(
    state: HomeScreenViewModel.State,
    events: Flow<HomeScreenViewModel.Event>,
    beatsFlow: SharedFlow<BeatWithData>,
    start: () -> Unit,
    stop: () -> Unit
) {

    val rationaleDialog = rememberMicrophoneRationaleDialog()
    val micPermissionHandler = rememberPermissionHandler(
        Manifest.permission.RECORD_AUDIO,
        onAccepted = start,
        onDeclined = {},
        onShowRationale = { rationaleDialog.show() }
    )

    val context = LocalContext.current
    val snackbarState = remember { SnackbarHostState() }

    val notificationsPermissionHandler =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionHandler(
                Manifest.permission.POST_NOTIFICATIONS,
                onAccepted = {},
                onDeclined = {},
                onShowRationale = {}
            )
        } else null

    LaunchedEffect(Unit) {
        events.collect {
            if (it is HomeScreenViewModel.Event.ScheduledForOfflineRecognition) {
                if (notificationsPermissionHandler?.permissionGranted == false) {
                    notificationsPermissionHandler.askForPermission()
                }
            }
            when (it) {
                HomeScreenViewModel.Event.ScheduledForOfflineRecognition -> snackbarState.showSnackbar(
                    context.getString(R.string.offline_recognition)
                )

                HomeScreenViewModel.Event.SongNotFound -> snackbarState.showSnackbar(
                    context.getString(
                        R.string.no_results
                    )
                )
            }
        }

    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarState) },
        containerColor = Color.Transparent,
    ) { padding ->
        Box(Modifier.fillMaxSize()) {

            TitleText(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 200.dp),
                text = stringResource(R.string.find_songs)
            )
            RecordButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(0.40f),
                beatsFlow = beatsFlow,
                isRecording = state == HomeScreenViewModel.State.Identifying || state is HomeScreenViewModel.State.Found,
                hide = state is HomeScreenViewModel.State.Found,
                onClick = {
                    if (micPermissionHandler.permissionGranted)
                        start()
                    else micPermissionHandler.askForPermission()
                },
            )
            AnimatedVisibility(
                visible = state == HomeScreenViewModel.State.Identifying,
                enter = fadeIn(tween(delayMillis = 100)),
                exit = fadeOut()
            ) {
                RecordingScreen(
                    modifier = Modifier.fillMaxSize(),
                    onCancel = stop
                )
            }
            if (state is HomeScreenViewModel.State.Found) {
                MatchFoundScreen(
                    modifier = Modifier.fillMaxSize(),
                    songInfo = state.songInfo,
                    onNavigateBack = stop
                )
            }
        }
    }

    if (state == HomeScreenViewModel.State.Identifying || state is HomeScreenViewModel.State.Found) {
        DarkStatusBarEffect()
    }
    VibrateEffect(state, events)

    BackHandler(state == HomeScreenViewModel.State.Identifying, stop)
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
}

@Composable
fun RecordButton(
    modifier: Modifier,
    beatsFlow: SharedFlow<BeatWithData>,
    isRecording: Boolean,
    hide: Boolean,
    onClick: () -> Unit,
) {

    val transition = rememberInfiniteTransition()
    val scale by transition.animateFloat(
        1.0f,
        1.15f,
        infiniteRepeatable(
            tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val iconRotation by transition.animateFloat(
        0.0f,
        360f,
        infiniteRepeatable(
            tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val circleRecordingScale by animateFloatAsState(
        if (isRecording) 10.0f else 1.0f,
        tween(400, easing = FastOutSlowInEasing)
    )

    var pressScaleDown by remember { mutableFloatStateOf(0.0f) }

    // BeatWithData animation scale state
    var beatScale by remember { mutableFloatStateOf(1.0f) }

    // Animate beat bounce
    val animatedBeatScale by animateFloatAsState(
        targetValue = beatScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    // Launch beat detection listener
    LaunchedEffect(Unit) {
        beatsFlow.collect {
            beatScale = 1.4f
            delay(50)
            beatScale = 1.0f
        }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onPress = { pressScaleDown = -0.1f }) {
                    onClick()
                    pressScaleDown = 0.0f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .zIndex(-2f)
                .graphicsLayer {
                    scaleX = scale * circleRecordingScale + pressScaleDown
                    scaleY = scale * circleRecordingScale + pressScaleDown
                }
                .aspectRatio(1.0f)
                .background(Color(0xFFE70000), CircleShape)
        )
        AnimatedVisibility(
            modifier = Modifier.matchParentSize(),
            visible = isRecording,
            enter = EnterTransition.None,
            exit = fadeOut(tween(200))
        ) {
            RipplesVisualizer(Modifier.matchParentSize(), beatsFlow)
        }
        AnimatedVisibility(!hide, exit = fadeOut(), enter = fadeIn()) {
            Icon(
                modifier = Modifier
                    .graphicsLayer { rotationZ = iconRotation }
                    .size(56.dp)
                    .scale(animatedBeatScale),
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun TitleText(
    modifier: Modifier,
    text: String,
) {
    var hasPlayedAnimation by rememberSaveable { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPlayedAnimation) {
            visible = true
            delay(3000)
            hasPlayedAnimation = true
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible || hasPlayedAnimation,
        enter = fadeIn(tween(700)) + slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    ) {
        Text(
            text,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontFamily = ManropeFontFamily
        )
    }
}







