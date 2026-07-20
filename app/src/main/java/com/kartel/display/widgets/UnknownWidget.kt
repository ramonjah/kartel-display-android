package com.kartel.display.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// Пустая зона + видимая подпись вместо краша (DISPLAY_ARCHITECTURE.md §7:
// "неизвестный widget → пустая зона + лог ошибки, не краш"). Используется и
// для зарегистрированных, но ещё не реализованных в MVP типов (weather,
// product_grid, promo_banner, video, html, youtube, top_products,
// daily_brief) — честно показывает "не готово", не притворяется рабочим.
@Composable
fun UnknownWidget(label: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "[$label]", color = Color(0xFF555555))
    }
}
