package com.omar.findmysong.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omar.findmysong.model.SongInfo
import com.omar.findmysong.network.URL
import com.omar.findmysong.ui.theme.ManropeFontFamily
import kotlinx.coroutines.delay

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
                        .data("http://$URL/get_albumart?song_id=${songInfo.id}")
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
                                    Color.Black.copy(0.8f),
                                    Color.Black.copy(0.9f),
                                    Color.Black.copy(1.0f)
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
                AnimatedVisibility(visible, enter = fadeIn(tween(durationMillis = 400))) {
                    Text(
                        songInfo.title, style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ManropeFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedVisibility(
                    visible,
                    enter = fadeIn(tween(durationMillis = 600, delayMillis = 600))
                ) {
                    Text(
                        songInfo.artist,
                        style = MaterialTheme.typography.titleMedium,
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
