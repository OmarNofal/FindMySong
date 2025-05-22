package com.omar.findmysong.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.ui.permission.rememberMicrophoneRationaleDialog
import com.omar.findmysong.ui.permission.rememberPermissionHandler
import com.omar.findmysong.ui.theme.ManropeFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow


@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: HomeScreenViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState()
    val beatsFlow = viewModel.beatsFlow

    HomeScreen(
        modifier,
        state,
        beatsFlow,
        start = viewModel::start,
        stop = viewModel::stop
    )
}

@Composable
fun HomeScreen(
    modifier: Modifier,
    state: HomeScreenViewModel.State,
    beatsFlow: SharedFlow<Unit>,
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


    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(state) {
        if (state is HomeScreenViewModel.State.NotFound)
            snackbarState.showSnackbar("No results found!")
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
                text = "Find songs around you..."
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
    VibrateEffect(state)

    BackHandler(state == HomeScreenViewModel.State.Identifying, stop)
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
}

@Composable
fun RecordButton(
    modifier: Modifier,
    beatsFlow: SharedFlow<Unit>,
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

    // Beat animation scale state
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
        if (!hide)
            RipplesVisualizer(Modifier.matchParentSize(), beatsFlow)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    modifier: Modifier,
    onCancel: () -> Unit
) {

    var textState by remember { mutableStateOf(RecordingScreenTextState.LISTENING) }

    LaunchedEffect(Unit) {
        delay(6000)
        textState = RecordingScreenTextState.FINDING_MATCH
        delay(7000)
        textState = RecordingScreenTextState.TAKING_TIME
    }

    Scaffold(
        modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onCancel) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White
                )
            )

        },
        containerColor = Color.Transparent,
    ) { it ->
        it
        Box(modifier = Modifier.fillMaxSize()) {
            CompositionLocalProvider(
                LocalContentColor provides Color.White
            ) {

                AnimatedContent(
                    targetState = textState, modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 200.dp)
                ) { it ->
                    val text = when (it) {
                        RecordingScreenTextState.LISTENING -> "Listening..."
                        RecordingScreenTextState.FINDING_MATCH -> "Finding a match..."
                        RecordingScreenTextState.TAKING_TIME -> "This is taking some time..."
                    }
                    TitleText(
                        modifier = Modifier,
                        text = text,
                    )
                }
            }
        }
    }
}

enum class RecordingScreenTextState {
    LISTENING, FINDING_MATCH, TAKING_TIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchFoundScreen(
    modifier: Modifier,
    songInfo: SongInfo,
    onNavigateBack: () -> Unit
) {

    BackHandler(true, onNavigateBack)

    var hasPlayedAnimation by rememberSaveable { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasPlayedAnimation) {
            visible = true
            delay(3000)
            hasPlayedAnimation = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White
                )
            )

        }) { it ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            var showImage by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(2000)
                showImage = true
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize(),
                    model = if (showImage) ImageRequest.Builder(LocalContext.current)
                        .data("http://192.168.1.77:8000/get_albumart?song_id=${songInfo.id}")
                        .crossfade(1000)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build() else null,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(0.2f),
                                    Color.Black.copy(0.7f),
                                    Color.Black.copy(alpha = 1.0f)
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {


                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(visible, enter = fadeIn()) {
                    Text(
                        songInfo.title, style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ManropeFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(durationMillis = 600, delayMillis = 500))
                ) {
                    Text(
                        songInfo.artist,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Normal,
                        fontFamily = ManropeFontFamily,
                        color = Color(0xF0F0F0F0),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}


data class Ripple(var alpha: Float, var scale: Float)

@Composable
fun RipplesVisualizer(
    modifier: Modifier = Modifier,
    beats: Flow<Unit>
) {
    val ripples = remember { mutableStateListOf<Ripple>() }
    var lastFrameTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Dummy state to trigger recomposition
    var frameTick by remember { mutableIntStateOf(0) }

    val alphaDeltaPerSecond = -0.4f
    val scaleDeltaPerSecond = 3.4f

    // Listen to beat events
    LaunchedEffect(Unit) {
        beats.collect {
            ripples.add(Ripple(alpha = 0.8f, scale = 1.0f))
        }
    }

    // Animate ripples every frame
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                val currentTime = frameTimeNanos / 1_000_000
                val diffSeconds = (currentTime - lastFrameTime) / 1000f
                lastFrameTime = currentTime

                val iterator = ripples.iterator()
                while (iterator.hasNext()) {
                    val ripple = iterator.next()
                    ripple.alpha += alphaDeltaPerSecond * diffSeconds
                    ripple.scale += scaleDeltaPerSecond * diffSeconds
                    if (ripple.alpha <= 0f) iterator.remove()
                }

                // Trigger recomposition by incrementing the state
                frameTick++
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            for (ripple in ripples) {
                withTransform({
                    frameTick
                    val scalePx = with(this@Canvas) {
                        ripple.scale.dp.toPx()
                    }
                    scale(scalePx)
                }) {
                    drawCircle(
                        color = Color(1.0f, 0.63f, 0.63f, alpha = ripple.alpha),
                        radius = 40f
                    )
                }
            }
        }
    }
}


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
fun VibrateEffect(state: HomeScreenViewModel.State) {

    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is HomeScreenViewModel.State.Found) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(600L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else if (state is HomeScreenViewModel.State.NotFound) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 70, 80, 70) // delay, vibrate, pause, vibrate
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1)) // -1 means no repeat
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