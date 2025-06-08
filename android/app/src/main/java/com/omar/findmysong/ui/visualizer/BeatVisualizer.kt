package com.omar.findmysong.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.omar.findmysong.visualizer.BeatWithData
import kotlinx.coroutines.flow.Flow


const val BASE_ALPHA_DELTA = -0.4f
const val BASE_SCALE_DELTA = 3.4f

data class Ripple(var alpha: Float, var scale: Float, val color: Color)

@Composable
fun RipplesVisualizer(
    modifier: Modifier = Modifier,
    beats: Flow<BeatWithData>
) {
    val ripples = remember { mutableStateListOf<Ripple>() }
    var lastFrameTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Dummy state to trigger recomposition
    var frameTick by remember { mutableIntStateOf(0) }

    var alphaDeltaPerSecond by remember { mutableFloatStateOf(BASE_ALPHA_DELTA) }
    var scaleDeltaPerSecond by remember { mutableFloatStateOf(BASE_SCALE_DELTA) }

    // Listen to beat events
    LaunchedEffect(Unit) {
        beats.collect { beat ->
            if (beat.isSnare)
                ripples.add(Ripple(alpha = 0.5f, scale = 4.0f, color = Color(1.0f, 0.63f, 0.63f)))
            if (beat.isKick)
                ripples.add(
                    Ripple(
                        alpha = 0.8f, scale = 1.0f, color = Color(
                            0.98f,
                            0.467f,
                            0.467f,
                            1.0f
                        )
                    )
                )
            if (beat.isBass)
                ripples.add(
                    Ripple(
                        alpha = 0.7f, scale = 2.0f, color = Color(
                            1.0f,
                            0.353f,
                            0.353f,
                            1.0f
                        )
                    )
                )

            alphaDeltaPerSecond = BASE_ALPHA_DELTA * (beat.tempo / 140f)
            scaleDeltaPerSecond = BASE_SCALE_DELTA * (beat.tempo / 140f)
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
                        color = ripple.color.copy(alpha = ripple.alpha),
                        radius = 40f
                    )
                }
            }
        }
    }
}
