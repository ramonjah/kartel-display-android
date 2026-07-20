// Offline-кэш последнего успешно полученного ScreenConfigResponse (§13
// DISPLAY_ARCHITECTURE.md: устройство должно рендерить что-то на холодном
// старте без сети, не пустой экран). Обычный SharedPreferences, не
// EncryptedSharedPreferences — это не секрет (layout/playlist уже публично
// видны любому, кто стоит перед экраном), только device token в TokenStore
// требует шифрования.

package com.kartel.display.storage

import android.content.Context
import com.kartel.display.network.ScreenConfigResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ConfigCache(context: Context) {
    private val prefs = context.getSharedPreferences("kartel_display_config_cache", Context.MODE_PRIVATE)

    fun save(config: ScreenConfigResponse) {
        prefs.edit().putString(KEY_CONFIG, json.encodeToString(config)).apply()
    }

    fun load(): ScreenConfigResponse? =
        prefs.getString(KEY_CONFIG, null)?.let { raw ->
            runCatching { json.decodeFromString<ScreenConfigResponse>(raw) }.getOrNull()
        }

    companion object {
        private const val KEY_CONFIG = "last_screen_config"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
