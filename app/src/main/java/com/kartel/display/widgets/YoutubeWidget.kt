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

// YouTube без нативного SDK/API-ключа — публичный embed-плеер через
// WebView (autoplay+mute+loop), тот же трюк, что используют витринные
// экраны повсеместно без договора с YouTube Data API (§10 MVP Templates:
// "video ID/ссылка", не требует авторизации).
private val YT_ID_REGEX = Regex("""(?:v=|youtu\.be/|embed/)([A-Za-z0-9_-]{6,})""")

private fun extractVideoId(config: JsonElement?): String? {
    val obj = config?.jsonObject ?: return null
    obj["video_id"]?.jsonPrimitive?.content?.let { return it }
    val url = obj["url"]?.jsonPrimitive?.content ?: return null
    return YT_ID_REGEX.find(url)?.groupValues?.get(1) ?: url.substringAfterLast('/')
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeWidget(config: JsonElement?) {
    val videoId = extractVideoId(config)
    if (videoId == null) {
        UnknownWidget("youtube (нет video_id/url в config)")
        return
    }

    // Прямая навигация WebView на youtube.com/embed/... даёт Error 153
    // ("configuration error") на части видео — живьём воспроизведено на
    // эмуляторе. Обёртка iframe в HTML-страницу, загруженную через
    // loadDataWithBaseURL с baseUrl=https://www.youtube.com — стандартный
    // обход: страница получает корректный origin/referrer для проверки
    // embeddability, которого нет при loadUrl напрямую на embed-ссылку.
    val html = """
        <html><body style="margin:0;padding:0;background:#000;">
        <iframe width="100%" height="100%" frameborder="0"
          src="https://www.youtube.com/embed/$videoId?autoplay=1&mute=1&loop=1&playlist=$videoId&controls=0&modestbranding=1&playsinline=1"
          allow="autoplay; encrypted-media"></iframe>
        </body></html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            }
        },
    )
}
