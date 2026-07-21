// Renderer MVP (Этап 5) + Realtime-подписка/offline-кэш (Этап 6,
// DISPLAY_ARCHITECTURE.md §12/§13) — Layout получается cache-first, затем
// обновляется через ScreenSyncManager (sync/) по diff'у layout_version, не
// перезапросом раз в N секунд. Heartbeat остаётся отдельным REST-циклом раз
// в 30с (§8: "устройству не нужно получать события о себе"). UpdateChecker
// (updater/, Этап 10) — раз в 6 часов, тоже не time-critical.

package com.kartel.display

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        // installSplashScreen() ОБЯЗАН вызываться до super.onCreate() —
        // держит логотип (Theme.KartelDisplay.Starting, themes.xml) на
        // экране, пока идёт первая проверка токена/пейринга, вместо голого
        // чёрного экрана (владелец: «начинает с чёрного экрана просто»).
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Digital signage: экран не должен гаснуть и уходить в системный
        // скринсейвер (Daydream) от бездействия — на витрине никто не
        // трогает пульт часами. FLAG_KEEP_SCREEN_ON держит устройство
        // "активным" с точки зрения PowerManager, чего для kiosk-приложения
        // достаточно (без него — тёмный/спящий экран через несколько минут
        // простоя, обнаружено живьём при установке в точке).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                // contentColor = White ОБЯЗАТЕЛЕН: Surface(color = Color.Black) — не
                // тема-цвет, поэтому material3 наследует LocalContentColor по умолчанию
                // (чёрный) → весь Text без явного цвета (заголовок и 6-значный код на
                // PairingScreen, и любое сообщение об ошибке) рисуется чёрным по чёрному
                // и невидим. Симптом — «тёмный экран» при живой установке на ТВ.
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black, contentColor = Color.White) {
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
        config == null && errorMessage == null -> StatusMessage("Загрузка…", Color.Gray)
        config == null && errorMessage != null -> StatusMessage("Ошибка: $errorMessage", Color.Red)
        config?.layout == null -> StatusMessage("Экран без layout — ждём назначения от владельца", Color.Gray)
        else -> ScreenRenderer(layout = config?.layout, playlist = config?.playlist)
    }
}

// TV-safe area: голый Text без Box/padding садится в (0,0) — левый верхний
// угол — который на реальных телевизорах часто попадает под overscan-обрезку
// (найдено живьём: текст «вылезал» за рамку экрана). 48.dp — тот же отступ,
// что уже используется в PairingScreen; центровка — чтобы сообщение было
// заведомо внутри безопасной зоны на любой диагонали/масштабировании.
@Composable
private fun StatusMessage(text: String, color: Color) {
    Box(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = color, textAlign = TextAlign.Center)
    }
}
