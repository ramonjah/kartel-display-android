package com.kartel.display.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kartel.display.network.Playlist

// "Акции" (§10 MVP Templates) — playlist изображений/видео вперемешку;
// per-элемент решение image/video по расширению url (content_items не
// несёт отдельного media-type флага для kind='url' — сырая ссылка).
@Composable
fun PromoBannerWidget(playlist: Playlist?) {
    val item = rememberPlaylistItem(playlist) { it.url != null }
    if (item?.url == null) {
        UnknownWidget("promo_banner (пустой playlist)")
        return
    }

    if (item.looksLikeVideo()) {
        VideoPlayer(url = item.url, loop = false)
    } else {
        AsyncImage(
            model = item.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}
