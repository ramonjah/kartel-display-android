// Renderer MVP (Этап 5, DISPLAY_ARCHITECTURE.md §20 roadmap): нет
// Realtime-подписки ещё (Этап 6) — Layout получается один раз при
// старте. Heartbeat отправляется раз в 30с (§8: "обычный REST-вызов, не
// Realtime — устройству не нужно получать события о себе").

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.kartel.display.network.DisplayApi
import com.kartel.display.network.ScreenConfigResponse
import com.kartel.display.registration.PairingScreen
import com.kartel.display.renderer.ScreenRenderer
import com.kartel.display.storage.TokenStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(applicationContext)
        val api = DisplayApi()

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
                        DisplayContent(api = api, token = token!!)
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayContent(api: DisplayApi, token: String) {
    var config by remember { mutableStateOf<ScreenConfigResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Один REST-вызов при старте — не Realtime, это Этап 6 (§8/§12).
    LaunchedEffect(token) {
        val res = api.getScreenConfig(token)
        if (res.ok) config = res else errorMessage = res.reason
        loading = false
    }

    // Heartbeat — статус online/offline на стороне owner UI (уже проверено
    // живьём в Этапе 4: last_heartbeat_at, порог 2 минуты).
    LaunchedEffect(token) {
        while (true) {
            launch { runCatching { api.deviceHeartbeat(token, appVersion = BuildConfigVersion) } }
            delay(30_000)
        }
    }

    when {
        loading -> Text(text = "Загрузка…", color = Color.Gray)
        errorMessage != null -> Text(text = "Ошибка: $errorMessage", color = Color.Red)
        config?.layout == null -> Text(text = "Экран без layout — ждём назначения от владельца", color = Color.Gray)
        else -> ScreenRenderer(layout = config?.layout)
    }
}

private const val BuildConfigVersion = "0.1.0-mvp"
