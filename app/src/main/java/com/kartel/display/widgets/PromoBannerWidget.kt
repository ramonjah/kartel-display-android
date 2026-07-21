package com.kartel.display.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kartel.display.network.Playlist
import com.kartel.display.network.ZoneContentItem

// "Акции" (§10 MVP Templates). Два режима на один и тот же виджет:
// (1) kind='promo' (миграция 063, PromotionsPanel в rstore-dashboard) — фото
//     + slogan + description → шаблонная карточка (тот же CSS-дизайн, что
//     владелец видел в живом предпросмотре при создании акции: фото фоном,
//     тёмный градиент снизу, слоган крупно, описание мельче), без HTML;
// (2) kind='media'/'url' без slogan (старые голые картинки/видео в плейлисте
//     Акций) — прежнее поведение, просто изображение/видео на весь виджет.
// Источник контента: zoneItems (владелец назначил ИМЕННО этой зоне в
// VisualLayoutEditor) приоритетнее ambient screen-level playlist — обратная
// совместимость с экранами, настроенными до 063.
@Composable
fun PromoBannerWidget(zoneItems: List<ZoneContentItem> = emptyList(), playlist: Playlist? = null) {
    if (zoneItems.isNotEmpty()) {
        val item = rememberZoneItem(zoneItems.filter { it.url != null })
        if (item?.url == null) { UnknownWidget("promo_banner (нет назначенного контента)"); return }
        PromoContent(url = item.url, isVideo = item.looksLikeVideo(), slogan = item.slogan, description = item.description, alignment = item.focusAlignment())
        return
    }
    val item = rememberPlaylistItem(playlist) { it.url != null }
    if (item?.url == null) { UnknownWidget("promo_banner (пустой playlist)"); return }
    PromoContent(url = item.url, isVideo = item.looksLikeVideo(), slogan = null, description = null, alignment = Alignment.Center)
}

@Composable
private fun PromoContent(url: String, isVideo: Boolean, slogan: String?, description: String?, alignment: Alignment) {
    if (slogan == null && description == null) {
        // Голая картинка/видео без структурированного текста — как раньше.
        if (isVideo) VideoPlayer(url = url, loop = false)
        else AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = alignment)
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) VideoPlayer(url = url, loop = false)
        else AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = alignment)

        // Тот же градиент, что PromoCardPreview в Displays.jsx: прозрачно сверху
        // → чёрный снизу, чтобы текст был читаем поверх любого фото.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Black.copy(alpha = 0.15f), 1f to Color.Black.copy(alpha = 0.9f))),
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(16.dp),
        ) {
            if (!slogan.isNullOrBlank()) {
                Text(text = slogan, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp)
            }
            if (!description.isNullOrBlank()) {
                Text(text = description, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            }
        }
    }
}
