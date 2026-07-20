package com.kartel.display.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kartel.display.network.Playlist
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Media3 ExoPlayer — единственный video-плеер, зашитый в KARTEL Display
// (§10 MVP Templates: "video" template = media_assets/URL-загрузка).
// Плейлист-режим (когда в config нет своего url) обслуживает и "video"-
// зону со множеством роликов подряд, и video-часть promo_banner — общая
// ротация в PlaylistRotation.kt, здесь только собственно плеер.
@Composable
fun VideoWidget(config: JsonElement?, playlist: Playlist?) {
    val directUrl = config?.jsonObject?.get("url")?.jsonPrimitive?.content
    val item = if (directUrl == null) rememberPlaylistItem(playlist) { it.url != null } else null
    val url = directUrl ?: item?.url

    if (url == null) {
        UnknownWidget("video (нет url в config и нет видео в playlist)")
        return
    }

    VideoPlayer(url = url, loop = directUrl != null)
}

@Composable
fun VideoPlayer(url: String, loop: Boolean) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = if (loop) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
            volume = 0f // витрина — звук не нужен, часто рядом с покупателями
            prepare()
            play()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.player = player
            }
        },
    )
}
