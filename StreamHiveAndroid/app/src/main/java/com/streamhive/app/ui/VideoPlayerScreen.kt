package com.streamhive.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import androidx.compose.ui.viewinterop.AndroidView
import com.streamhive.app.net.ApiClient

@Composable
fun VideoPlayerScreen(key: String) {
    val urlState = remember { mutableStateOf("") }

    LaunchedEffect(key) {
        val presign = ApiClient.api.presignGet(key)
        urlState.value = presign.url
    }

    if (urlState.value.isNotEmpty()) {
        val context = ApiClient.appContext
        val player = remember {
            ExoPlayer.Builder(context).build()
        }
        LaunchedEffect(urlState.value) {
            player.setMediaItem(MediaItem.fromUri(Uri.parse(urlState.value)))
            player.prepare()
            player.playWhenReady = true
        }
        AndroidView(
            factory = { ctx ->
                StyledPlayerView(ctx).apply { this.player = player }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) { }
    }
}

