package com.kartel.display.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Coil кэширует на диск сам (§13 offline: изображение остаётся видно без
// сети после первой успешной загрузки) — не пишем свой кэш-слой поверх.
@Composable
fun ImageWidget(config: JsonElement?) {
    val url = config?.jsonObject?.get("url")?.jsonPrimitive?.content
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        UnknownWidget("image (нет url в config)")
    }
}
