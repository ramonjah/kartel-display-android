// Оркестрация self-hosted OTA (Этап 10, §15 DISPLAY_ARCHITECTURE.md):
// проверка раз в 6 часов — обновление APK НЕ time-critical, в отличие
// от Layout (Realtime, миграция 058), поэтому обычный REST-поллинг с
// большим интервалом, не отдельный broadcast-канал. checkOnce() публичен
// отдельно от start() — живая проверка может вызвать его напрямую, не
// дожидаясь полного интервала.

package com.kartel.display.updater

import android.content.Context
import android.util.Log
import com.kartel.display.BuildConfig
import com.kartel.display.network.DisplayApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "UpdateChecker"

class UpdateChecker(private val api: DisplayApi, private val context: Context) {

    fun start(scope: CoroutineScope, deviceToken: String) {
        scope.launch {
            while (true) {
                // checkOnce возвращает true, если обновление было найдено и
                // предложено (независимо от того, нажал ли владелец
                // "установить" — promptInstall не умеет отследить результат,
                // §updater/ApkInstaller.kt). Живой случай: владелец случайно
                // закрыл диалог установки — следующая попытка не должна
                // ждать полный 6-часовой цикл, иначе обновление зависает на
                // старой версии на полдня. "Уже последняя версия" — обычный
                // случай, там долгий интервал оправдан (не дёргать сервер).
                val wasOffered = runCatching { checkOnce(deviceToken) }
                    .onFailure { e ->
                        // Раньше здесь была немая runCatching-обёртка — реальная
                        // ошибка (например, скачивание оборвалось) не оставляла
                        // никакого следа, что подрывает саму идею "Диагностика"
                        // в названии этапа. Теперь падение видно и в logcat, и
                        // в device_events — оно же читается владельцем в
                        // DiagnosticsSection (BUSINESS UI).
                        Log.e(TAG, "update check/download/install failed", e)
                        runCatching {
                            api.logDeviceEvent(deviceToken, "apk_update", buildJsonObject {
                                put("stage", "error"); put("message", e.message ?: e.toString())
                            })
                        }
                    }
                    .getOrDefault(false)
                delay(if (wasOffered) RETRY_INTERVAL_MS else CHECK_INTERVAL_MS)
            }
        }
    }

    suspend fun checkOnce(deviceToken: String): Boolean {
        val res = api.checkUpdate(deviceToken, BuildConfig.VERSION_CODE)
        if (!res.ok || !res.available) return false
        val url = res.url ?: return false
        val versionCode = res.version_code ?: return false

        api.logDeviceEvent(deviceToken, "apk_update", buildJsonObject {
            put("stage", "downloading"); put("version", res.version)
        })
        val apkFile = ApkDownloader.download(context, url, versionCode)
        api.logDeviceEvent(deviceToken, "apk_update", buildJsonObject {
            put("stage", "install_prompt"); put("version", res.version)
        })
        ApkInstaller.promptInstall(context, apkFile)
        return true
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val RETRY_INTERVAL_MS = 5 * 60 * 1000L
    }
}
