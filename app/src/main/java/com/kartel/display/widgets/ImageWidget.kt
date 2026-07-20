package com.kartel.display.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kartel.display.network.Playlist
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Coil кэширует на диск сам (§13 offline: изображение остаётся видно без
// сети после первой успешной загрузки) — не пишем свой кэш-слой поверх.
// "Слайдер изображений" (§10 MVP Templates) — тот же виджет, но без своего
// url в config: тогда ротирует по screen-level Playlist (PlaylistRotation.kt).
@Composable
fun ImageWidget(config: JsonElement?, playlist: Playlist? = null) {
    val directUrl = config?.jsonObject?.get("url")?.jsonPrimitive?.content
    val item = if (directUrl == null) rememberPlaylistItem(playlist) { it.url != null && !it.looksLikeVideo() } else null
    val url = directUrl ?: item?.url

    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        UnknownWidget("image (нет url в config и нет изображений в playlist)")
    }
}
