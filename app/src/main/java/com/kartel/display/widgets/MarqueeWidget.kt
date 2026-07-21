package com.kartel.display.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Бегущая строка (zone.config: {text, bgColor, textColor, speedPxPerSec}),
// заведена по запросу владельца рядом с редактором зон в rstore-dashboard
// (Displays.jsx MarqueeFields). Скроллинг — Modifier.basicMarquee() из
// Compose Foundation (стабилен с 1.6, есть в текущем BOM 2024.06.00): не
// пишем свою анимацию текста, foundation уже делает ровно это (P10).
@Composable
fun MarqueeWidget(config: JsonElement?) {
    val obj = config?.jsonObject
    val text = obj?.get("text")?.jsonPrimitive?.content
    val bg = parseHexColor(obj?.get("bgColor")?.jsonPrimitive?.content, fallback = Color(0xFFC2410C))
    val fg = parseHexColor(obj?.get("textColor")?.jsonPrimitive?.content, fallback = Color.White)
    val speedPx = obj?.get("speedPxPerSec")?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(10, 500) ?: 80

    if (text.isNullOrBlank()) { UnknownWidget("marquee (нет текста в config)"); return }

    Box(modifier = Modifier.fillMaxSize().background(bg), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = fg,
            fontSize = 20.sp,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .basicMarquee(iterations = Int.MAX_VALUE, velocity = speedPx.dp),
        )
    }
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try { Color(android.graphics.Color.parseColor(hex)) } catch (e: IllegalArgumentException) { fallback }
}
