package com.kartel.display.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Локальные часы устройства — не тянет ничего с сервера (§4
// DISPLAY_ARCHITECTURE.md: "Локальные часы/таймер... выполняется локально").
@Composable
fun ClockWidget(config: JsonElement?) {
    val format = (config?.jsonObject?.get("format")?.jsonPrimitive?.content) ?: "24h"
    val showDate = (config?.jsonObject?.get("showDate")?.jsonPrimitive?.content) != "false"

    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(1000)
        }
    }

    val timePattern = if (format == "12h") "hh:mm:ss a" else "HH:mm:ss"
    val timeText = SimpleDateFormat(timePattern, Locale("ru")).format(now)
    val dateText = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(now)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = timeText, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        if (showDate) Text(text = dateText, fontSize = 20.sp)
    }
}
