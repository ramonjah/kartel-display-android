package com.kartel.display.widgets

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// WebView внутри Compose — единственный движок для html/web-виджета
// (§10 MVP Templates: "HTML/Web" = произвольный URL/встроенный HTML).
// Namespace: устройство рендерит только то, что владелец явно ввёл в
// config зоны — не выполняет серверный код (ADR-000 P9).
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HtmlWidget(config: JsonElement?) {
    val url = config?.jsonObject?.get("url")?.jsonPrimitive?.content
    val html = config?.jsonObject?.get("html")?.jsonPrimitive?.content

    if (url == null && html == null) {
        UnknownWidget("html (нет url/html в config)")
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                if (url != null) loadUrl(url) else loadDataWithBaseURL(null, html!!, "text/html", "utf-8", null)
            }
        },
    )
}
