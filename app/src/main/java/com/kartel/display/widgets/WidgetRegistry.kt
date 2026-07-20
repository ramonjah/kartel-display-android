package com.kartel.display.widgets

import androidx.compose.runtime.Composable
import com.kartel.display.network.Zone

// Реестр типов виджетов (аналог Capability-реестра ADR-000 §5, но для
// рендер-виджетов, не бизнес-глаголов) — сервер не может прислать клиенту
// тип, которого клиент не знает: неизвестный `widget` рендерится
// UnknownWidget, не роняет приложение (§7 DISPLAY_ARCHITECTURE.md).
val MVP_IMPLEMENTED_WIDGETS = setOf("clock", "image", "qr")

@Composable
fun RenderWidget(zone: Zone) {
    when (zone.widget) {
        "clock" -> ClockWidget(zone.config)
        "image" -> ImageWidget(zone.config)
        "qr" -> QrWidget(zone.config)
        else -> UnknownWidget(zone.widget)
    }
}
