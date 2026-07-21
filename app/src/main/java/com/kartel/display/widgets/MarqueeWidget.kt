package com.kartel.display.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Бегущая строка (zone.config: {text, bgColor, textColor, speedPxPerSec}).
//
// ПЕРВАЯ версия (v1.1.0) использовала Modifier.basicMarquee() — на реальном
// ТВ строка стояла на месте (владелец, живая проверка). Причина: basicMarquee
// по документированному поведению анимирует ТОЛЬКО когда контент физически
// не помещается в отведённую ширину — короткий текст/широкая зона просто не
// запускают анимацию, это не баг API, а его дизайн ("marquee только при
// overflow"). «Бегущая строка» по смыслу должна двигаться ВСЕГДА, независимо
// от того, влезает текст или нет — поэтому здесь своя анимация, не условная:
// текст рисуется дважды подряд с зазором, вся строка бесконечно едет влево на
// ширину одного цикла (текст+зазор), зацикливаясь без видимого шва.
@Composable
fun MarqueeWidget(config: JsonElement?) {
    val obj = config?.jsonObject
    val text = obj?.get("text")?.jsonPrimitive?.content
    val bg = parseHexColor(obj?.get("bgColor")?.jsonPrimitive?.content, fallback = Color(0xFFC2410C))
    val fg = parseHexColor(obj?.get("textColor")?.jsonPrimitive?.content, fallback = Color.White)
    val speedPx = obj?.get("speedPxPerSec")?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(10, 500) ?: 80

    if (text.isNullOrBlank()) { UnknownWidget("marquee (нет текста в config)"); return }

    val fontSizeSp = 24.sp
    val gapDp = 80.dp
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val style = remember(fontSizeSp) { TextStyle(fontSize = fontSizeSp, fontWeight = FontWeight.Bold) }
    val textLayout = remember(text, style) { textMeasurer.measure(text = text, style = style) }
    val textWidthPx = textLayout.size.width
    val gapPx = with(density) { gapDp.roundToPx() }
    val cyclePx = (textWidthPx + gapPx).coerceAtLeast(1)
    // Длительность одного цикла из скорости (px/сек) — не фиксированная длина
    // анимации, чтобы разный текст ехал с одинаковой ВИЗУАЛЬНОЙ скоростью, не
    // с одинаковым временем оборота. Пол на 300мс — очень короткий текст на
    // максимальной скорости не должен мигать неразличимо быстро.
    val durationMs = ((cyclePx / speedPx.toFloat()) * 1000).toInt().coerceAtLeast(300)

    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val offsetPx by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = cyclePx.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "marqueeOffset",
    )

    val offsetDp = with(density) { offsetPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(bg).clipToBounds(), contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.offset(x = -offsetDp, y = 0.dp),
        ) {
            Text(text = text, color = fg, style = style, maxLines = 1, softWrap = false)
            Spacer(modifier = Modifier.width(gapDp))
            Text(text = text, color = fg, style = style, maxLines = 1, softWrap = false)
        }
    }
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try { Color(android.graphics.Color.parseColor(hex)) } catch (e: IllegalArgumentException) { fallback }
}
