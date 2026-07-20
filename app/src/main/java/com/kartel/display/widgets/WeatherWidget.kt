package com.kartel.display.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kartel.display.network.CurrentWeather
import com.kartel.display.network.WeatherApi
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull

// weather_code (WMO) → русская подпись, та же таблица, что weatherKind.js
// (rstore-dashboard) — без иконки-эмодзи (правило проекта: иконки только
// вектором через Icon/I, здесь — просто текст, эмодзи не заводим вовсе).
private fun weatherLabel(code: Int): String = when {
    code <= 1  -> "ясно"
    code <= 3  -> "облачно"
    code <= 48 -> "туман"
    code <= 67 || code in 80..82 -> "дождь"
    code <= 77 || code == 85 || code == 86 -> "снег"
    else       -> "гроза"
}

@Composable
fun WeatherWidget(config: JsonElement?) {
    val lat = config?.jsonObject?.get("lat")?.jsonPrimitive?.doubleOrNull
    val lon = config?.jsonObject?.get("lon")?.jsonPrimitive?.doubleOrNull
    if (lat == null || lon == null) {
        UnknownWidget("weather (нет lat/lon в config)")
        return
    }

    var weather by remember { mutableStateOf<CurrentWeather?>(null) }

    // Погода не меняется быстро — раз в 20 минут достаточно, не нагружаем
    // Open-Meteo лишними запросами с парка устройств.
    LaunchedEffect(lat, lon) {
        while (true) {
            WeatherApi.fetchCurrent(lat, lon)?.let { weather = it }
            delay(20 * 60_000L)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        val w = weather
        if (w == null) {
            Text(text = "…", color = Color.Gray, fontSize = 20.sp)
        } else {
            Text(text = "${w.temperature.toInt()}°C", color = Color.White, fontSize = 32.sp)
            Text(text = weatherLabel(w.weathercode), color = Color.LightGray, fontSize = 16.sp)
        }
    }
}
