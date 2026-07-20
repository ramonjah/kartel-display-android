// Этап 6 (DISPLAY_ARCHITECTURE.md §12/§13): orchestration поверх DisplayApi —
// cache-first холодный старт, затем сеть, затем Realtime broadcast как
// триггер "перечитай конфиг" (сами данные всегда идут через
// display_get_screen_config, канал только сигнализирует — миграция 058).
// Diff по layout_version: полная перерисовка происходит только если версия
// реально изменилась, а не на каждый broadcast-евент.
//
// Периодический fallback-refresh (раз в 60с) — не просто перестраховка:
// найден живой edge case при верификации на эмуляторе — устройство может
// быть спарено РАНЬШЕ, чем экран ему назначен (display_get_screen_config
// тогда возвращает {ok:true, screen:null}, без screen_id), и в этом случае
// подписаться на broadcast-канал нечего — screen_id ещё не существует.
// Реальный BUSINESS UI (Displays.jsx) обычно назначает устройство на уже
// существующий экран (это UPDATE, триггер срабатывает), но полагаться на
// порядок действий владельца — хрупко; периодический refresh самолечит
// эту и любую другую пропущенную/отключившуюся WebSocket-подписку.
package com.kartel.display.sync

import com.kartel.display.network.DisplayApi
import com.kartel.display.network.ScreenConfigResponse
import com.kartel.display.network.ScreenUpdatedEvent
import com.kartel.display.network.SupabaseConfig
import com.kartel.display.storage.ConfigCache
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScreenSyncManager(
    private val api: DisplayApi,
    private val cache: ConfigCache,
    private val client: io.github.jan.supabase.SupabaseClient = SupabaseConfig.client,
) {
    private val _config = MutableStateFlow<ScreenConfigResponse?>(null)
    val config: StateFlow<ScreenConfigResponse?> = _config.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var subscribedChannel: RealtimeChannel? = null

    fun start(scope: CoroutineScope, deviceToken: String) {
        // 1. Cache-first: что-то на экране до первого сетевого ответа.
        cache.load()?.let { _config.value = it }

        // 2. Живой fetch раз в 60с — основной путь для первого получения
        // screen_id и safety-net на случай пропущенного/оборванного broadcast.
        scope.launch {
            while (true) {
                refresh(deviceToken)
                ensureSubscribed(scope, deviceToken)
                delay(60_000)
            }
        }
    }

    private fun ensureSubscribed(scope: CoroutineScope, deviceToken: String) {
        if (subscribedChannel != null) return
        val screenId = _config.value?.screen_id ?: return
        scope.launch {
            val channel = client.channel("screen:$screenId")
            subscribedChannel = channel
            // subscribe() должен быть вызван ДО collect — broadcastFlow сам не
            // присоединяется к каналу, он лишь подписывается на уже идущий
            // поток событий канала (подтверждено декомпиляцией realtime-kt).
            channel.subscribe(blockUntilSubscribed = true)
            channel.broadcastFlow<ScreenUpdatedEvent>("screen_updated")
                .collect { event ->
                    // layout_version отсутствует в событиях от notify_screen_direct/
                    // notify_screen_via_playlist — тогда просто перечитываем и
                    // сверяем версию после fetch, а не доверяем событию вслепую.
                    if (event.layout_version == null || event.layout_version != _config.value?.layout_version) {
                        refresh(deviceToken)
                    }
                }
        }
    }

    private suspend fun refresh(deviceToken: String) {
        val res = api.getScreenConfig(deviceToken)
        if (res.ok) {
            if (res.layout_version != _config.value?.layout_version || _config.value == null) {
                _config.value = res
            }
            cache.save(res)
            _errorMessage.value = null
        } else {
            // Сеть/сервер недоступны — не затираем уже отрендеренный
            // (кэшированный или предыдущий живой) конфиг ошибкой.
            if (_config.value == null) _errorMessage.value = res.reason
        }
    }
}
