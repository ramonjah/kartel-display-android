// Скачивает APK во внутренний кэш (не Downloads/внешнее хранилище — не
// нужен пользователю, только установщику через FileProvider). Обычный
// HttpURLConnection, не Ktor — это единственный не-JSON, потоковый вызов
// во всём клиенте, городить отдельный Ktor-стрим ради одного файла того
// не стоило (§13: сеть уже проверена рабочей для JSON RPC).

package com.kartel.display.updater

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object ApkDownloader {
    // withContext(Dispatchers.IO) обязателен — блокирующий HttpURLConnection
    // с главного/UI-диспетчера (LaunchedEffect в Compose исполняется на
    // AndroidUiDispatcher) кидает NetworkOnMainThreadException, найдено
    // живой проверкой на эмуляторе.
    suspend fun download(context: Context, url: String, versionCode: Int): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val dest = File(dir, "kartel-display-$versionCode.apk")

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true // GitHub Releases assets 302 в S3
        try {
            connection.connect()
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("HTTP ${connection.responseCode} downloading $url")
            }
            connection.inputStream.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        dest
    }
}
