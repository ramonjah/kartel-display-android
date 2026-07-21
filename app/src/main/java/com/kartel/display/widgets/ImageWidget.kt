package com.kartel.display.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kartel.display.network.Playlist
import com.kartel.display.network.ZoneContentItem
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Coil кэширует на диск сам (§13 offline: изображение остаётся видно без
// сети после первой успешной загрузки) — не пишем свой кэш-слой поверх.
// Приоритет источника url: (1) config.url — фиксированная одна картинка;
// (2) zoneItems (миграция 063) — то, что владелец явно назначил ЭТОЙ зоне в
// VisualLayoutEditor, ротация по порядку/duration_sec; (3) ambient
// screen-level Playlist — обратная совместимость с уже настроенными
// экранами до 063, где per-зонного назначения не было.
@Composable
fun ImageWidget(config: JsonElement?, zoneItems: List<ZoneContentItem> = emptyList(), playlist: Playlist? = null) {
    val directUrl = config?.jsonObject?.get("url")?.jsonPrimitive?.content
    val zoneItem = if (directUrl == null && zoneItems.isNotEmpty())
        rememberZoneItem(zoneItems.filter { it.url != null && !it.looksLikeVideo() }) else null
    val playlistItem = if (directUrl == null && zoneItems.isEmpty())
        rememberPlaylistItem(playlist) { it.url != null && !it.looksLikeVideo() } else null
    val url = directUrl ?: zoneItem?.url ?: playlistItem?.url
    val alignment = zoneItem?.focusAlignment() ?: Alignment.Center

    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = alignment,
        )
    } else {
        UnknownWidget("image (нет url в config и нет изображений в playlist)")
    }
}
