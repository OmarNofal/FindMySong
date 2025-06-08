package com.omar.findmysong.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omar.findmysong.R
import kotlinx.coroutines.delay

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
                        RecordingScreenTextState.LISTENING -> stringResource(R.string.listening)
                        RecordingScreenTextState.FINDING_MATCH -> stringResource(R.string.finding_a_match)
                        RecordingScreenTextState.TAKING_TIME -> stringResource(R.string.taking_some_time)
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