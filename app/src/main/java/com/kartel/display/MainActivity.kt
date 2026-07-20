// Renderer MVP (Этап 5) + Realtime-подписка/offline-кэш (Этап 6,
// DISPLAY_ARCHITECTURE.md §12/§13) — Layout получается cache-first, затем
// обновляется через ScreenSyncManager (sync/) по diff'у layout_version, не
// перезапросом раз в N секунд. Heartbeat остаётся отдельным REST-циклом раз
// в 30с (§8: "устройству не нужно получать события о себе"). UpdateChecker
// (updater/, Этап 10) — раз в 6 часов, тоже не time-critical.

package com.kartel.display

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kartel.display.network.DisplayApi
import com.kartel.display.registration.PairingScreen
import com.kartel.display.renderer.ScreenRenderer
import com.kartel.display.storage.ConfigCache
import com.kartel.display.storage.TokenStore
import com.kartel.display.sync.ScreenSyncManager
import com.kartel.display.updater.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(applicationContext)
        val configCache = ConfigCache(applicationContext)
        val api = DisplayApi()
        val updateChecker = UpdateChecker(api, applicationContext)

        setContent {
            var token by remember { mutableStateOf(tokenStore.deviceToken) }

            // MaterialTheme обязателен: без него material3-компоненты берут
            // LocalContentColor по умолчанию (чёрный) поверх чёрного Surface
            // — текст невидим, хотя рендер технически "работает" (найдено
            // живой проверкой на эмуляторе: экран был чёрным без ошибок).
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    if (token == null) {
                        PairingScreen(onPaired = { newToken ->
                            tokenStore.deviceToken = newToken
                            token = newToken
                        })
                    } else {
                        DisplayContent(api = api, token = token!!, configCache = configCache, updateChecker = updateChecker)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayContent(api: DisplayApi, token: String, configCache: ConfigCache, updateChecker: UpdateChecker) {
    val scope = rememberCoroutineScope()
    val syncManager = remember(token) { ScreenSyncManager(api = api, cache = configCache) }
    val config by syncManager.config.collectAsState()
    val errorMessage by syncManager.errorMessage.collectAsState()

    // Cache-first + fetch + Realtime broadcast-подписка (Этап 6) — вся
    // orchestration в ScreenSyncManager, здесь только запуск/сбор состояния.
    LaunchedEffect(token) {
        syncManager.start(scope = scope, deviceToken = token)
    }

    // Heartbeat — статус online/offline на стороне owner UI (уже проверено
    // живьём в Этапе 4: last_heartbeat_at, порог 2 минуты).
    LaunchedEffect(token) {
        while (true) {
            launch { runCatching { api.deviceHeartbeat(token, appVersion = BuildConfig.VERSION_NAME) } }
            delay(30_000)
        }
    }

    // APK-обновления (Этап 10) — раз в 6 часов, см. UpdateChecker.
    LaunchedEffect(token) {
        updateChecker.start(scope = scope, deviceToken = token)
    }

    when {
        config == null && errorMessage == null -> Text(text = "Загрузка…", color = Color.Gray)
        config == null && errorMessage != null -> Text(text = "Ошибка: $errorMessage", color = Color.Red)
        config?.layout == null -> Text(text = "Экран без layout — ждём назначения от владельца", color = Color.Gray)
        else -> ScreenRenderer(layout = config?.layout, playlist = config?.playlist)
    }
}
