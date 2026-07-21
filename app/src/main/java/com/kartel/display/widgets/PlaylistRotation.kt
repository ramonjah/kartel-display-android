// Общая ротация по Playlist для playlist-driven виджетов (слайдер/video/
// promo_banner) — один экран несёт ОДИН Playlist (screens.playlist_id),
// каждый playlist-виджет фильтрует его под себя (например, video-зона
// показывает только видео-элементы плейлиста). Ротация по duration_sec
// каждого элемента, не по фиксированному интервалу (§10 MVP Templates).

package com.kartel.display.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kartel.display.network.Playlist
import com.kartel.display.network.PlaylistItem
import com.kartel.display.network.ZoneContentItem
import kotlinx.coroutines.delay

@Composable
fun rememberPlaylistItem(playlist: Playlist?, filter: (PlaylistItem) -> Boolean): PlaylistItem? {
    val items = playlist?.items?.filter(filter) ?: emptyList()
    var index by remember(items) { mutableIntStateOf(0) }

    LaunchedEffect(items) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            val current = items[index % items.size]
            delay(current.duration_sec.coerceAtLeast(1) * 1000L)
            index++
        }
    }

    return items.getOrNull(index % items.size.coerceAtLeast(1))
}

// То же самое, но над контентом, назначенным конкретной зоне (zone.items,
// миграция 063) — владелец сам выбрал состав и порядок в редакторе, поэтому
// без фильтра: список уже ровно то, что должно ротироваться в этой зоне.
@Composable
fun rememberZoneItem(items: List<ZoneContentItem>): ZoneContentItem? {
    var index by remember(items) { mutableIntStateOf(0) }

    LaunchedEffect(items) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            val current = items[index % items.size]
            delay(current.duration_sec.coerceAtLeast(1) * 1000L)
            index++
        }
    }

    return items.getOrNull(index % items.size.coerceAtLeast(1))
}

private val VIDEO_EXTENSIONS = setOf(".mp4", ".webm", ".mov", ".mkv")

fun PlaylistItem.looksLikeVideo(): Boolean =
    url?.let { u -> VIDEO_EXTENSIONS.any { u.substringBefore('?').endsWith(it, ignoreCase = true) } } ?: false

fun ZoneContentItem.looksLikeVideo(): Boolean =
    url?.let { u -> VIDEO_EXTENSIONS.any { u.substringBefore('?').endsWith(it, ignoreCase = true) } } ?: false

// (0..1, центр 0.5) → Coil/Compose Alignment (-1..1, центр 0) — та же точка
// фокуса, что владелец выставил drag'ом в веб-предпросмотре (PromotionsPanel,
// CSS object-position), здесь управляет обрезкой при ContentScale.Crop.
fun ZoneContentItem.focusAlignment(): Alignment =
    BiasAlignment((focus_x * 2 - 1).toFloat(), (focus_y * 2 - 1).toFloat())
