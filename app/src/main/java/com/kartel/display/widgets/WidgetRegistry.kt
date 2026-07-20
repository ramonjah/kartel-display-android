package com.kartel.display.widgets

import androidx.compose.runtime.Composable
import com.kartel.display.network.Playlist
import com.kartel.display.network.Zone

// Реестр типов виджетов (аналог Capability-реестра ADR-000 §5, но для
// рендер-виджетов, не бизнес-глаголов) — сервер не может прислать клиенту
// тип, которого клиент не знает: неизвестный `widget` рендерится
// UnknownWidget, не роняет приложение (§7 DISPLAY_ARCHITECTURE.md).
// product_grid, top_products, daily_brief — намеренно не в этом списке:
// product_grid отложен (нужна projection-таблица products/stock, ещё не
// построена), top_products/daily_brief — Этап 8 (Live-виджеты) по roadmap.
val MVP_IMPLEMENTED_WIDGETS = setOf(
    "clock", "image", "qr", "weather", "promo_banner", "video", "html", "youtube",
)

@Composable
fun RenderWidget(zone: Zone, playlist: Playlist?) {
    when (zone.widget) {
        "clock" -> ClockWidget(zone.config)
        "image" -> ImageWidget(zone.config, playlist)
        "qr" -> QrWidget(zone.config)
        "weather" -> WeatherWidget(zone.config)
        "promo_banner" -> PromoBannerWidget(playlist)
        "video" -> VideoWidget(zone.config, playlist)
        "html" -> HtmlWidget(zone.config)
        "youtube" -> YoutubeWidget(zone.config)
        else -> UnknownWidget(zone.widget)
    }
}
