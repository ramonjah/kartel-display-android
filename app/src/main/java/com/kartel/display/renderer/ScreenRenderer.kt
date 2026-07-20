// Рендер Layout: координаты зон относительные (0..1), не пиксели (§7
// DISPLAY_ARCHITECTURE.md) — один Layout работает на любой диагонали/
// разрешении TV без пересчёта на сервере. Recomposition Compose сама даёт
// diff на уровне UI-дерева (§12: "не нужно изобретать свой diff") — при
// смене Layout меняются только зоны, чьи данные реально изменились.

package com.kartel.display.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kartel.display.network.Layout
import com.kartel.display.network.Playlist
import com.kartel.display.widgets.RenderWidget

@Composable
fun ScreenRenderer(layout: Layout?, playlist: Playlist? = null) {
    BoxWithConstraints(modifier = Modifier.background(Color.Black)) {
        if (layout == null) {
            // Экран создан, но layout ещё не назначен владельцем — не
            // ошибка, законное состояние (Screen.isPaired без Layout).
            return@BoxWithConstraints
        }
        val fullWidth = maxWidth
        val fullHeight = maxHeight

        layout.zones.forEach { zone ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .absoluteOffset(x = fullWidth * zone.x.toFloat(), y = fullHeight * zone.y.toFloat())
                    .size(width = fullWidth * zone.w.toFloat(), height = fullHeight * zone.h.toFloat()),
            ) {
                RenderWidget(zone, playlist)
            }
        }
    }
}
