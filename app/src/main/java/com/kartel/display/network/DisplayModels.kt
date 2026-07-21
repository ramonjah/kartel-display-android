// DTOs matching the JSON shapes returned by display_* RPCs (migration 056,
// rstore-dashboard). Kept structurally identical to the JSON Layout DSL
// from DISPLAY_ARCHITECTURE.md §7 — zones carry relative (0..1) coordinates
// and an opaque `config` object, so a new widget type on the server needs
// no schema change here (unknown `widget` values fall back to UnknownWidget,
// not a crash — see widgets/WidgetRegistry.kt).

package com.kartel.display.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RequestPairingResponse(
    val ok: Boolean,
    val reason: String? = null,
    val pairing_session_id: String? = null,
    val code: String? = null,
    val expires_at: String? = null,
)

@Serializable
data class ClaimPairingResponse(
    val ok: Boolean,
    val reason: String? = null,
    val status: String? = null, // pending | confirmed | claimed | expired | not_found
    val token: String? = null,
)

// Контент, явно назначенный ЭТОЙ зоне владельцем в редакторе (rstore-dashboard
// VisualLayoutEditor/ZoneInspector, миграция 063) — не общий screen-level
// playlist. Сервер (display_get_screen_config) резолвит zone.config.items[].
// content_item_id → эту структуру, org-scoped; устройство content_items
// напрямую не читает (RLS), тот же принцип, что и PlaylistItem ниже.
@Serializable
data class ZoneContentItem(
    val content_item_id: String,
    val duration_sec: Int = 10,
    val kind: String? = null, // 'media' | 'url' | 'live' | 'promo'
    val url: String? = null,
    val live_widget: String? = null,
    val name: String? = null,
    val slogan: String? = null,
    val description: String? = null,
)

@Serializable
data class Zone(
    val id: String,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val widget: String,
    val config: JsonElement? = null,
    // Пусто, если владелец не назначил зоне конкретный контент — тогда
    // виджет-рендерер (ImageWidget/PromoBannerWidget) падает обратно на
    // ambient screen-level playlist (обратная совместимость, ничего не
    // ломает для уже настроенных экранов).
    val items: List<ZoneContentItem> = emptyList(),
)

@Serializable
data class Layout(
    val id: String,
    val zones: List<Zone> = emptyList(),
)

// Разрешено на сервере (миграция 059, display_get_screen_config) против
// content_items — приходят готовые kind/url/live_widget/name, не голая
// content_item_id-ссылка, которую клиенту было бы не с чем сопоставить
// (RLS запрещает устройству читать content_items напрямую).
@Serializable
data class PlaylistItem(
    val content_item_id: String,
    val duration_sec: Int = 10,
    val order: Int = 0,
    val kind: String? = null, // 'media' | 'url' | 'live'
    val url: String? = null,
    val live_widget: String? = null,
    val name: String? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val items: List<PlaylistItem> = emptyList(),
)

@Serializable
data class ScreenConfigResponse(
    val ok: Boolean,
    val reason: String? = null,
    val screen_id: String? = null,
    val layout_version: Int = 0,
    val layout: Layout? = null,
    val playlist: Playlist? = null,
)

@Serializable
data class SimpleOkResponse(
    val ok: Boolean,
    val reason: String? = null,
)

// Payload миграции 058 (realtime.send на topic screen:<screen_id>) — сам
// сигнал "что-то поменялось", не данные; layout_version — best-effort diff
// hint (приходит только из notify_screen_via_layout, у остальных двух
// триггеров его нет), финальное решение "обновляться или нет" всё равно за
// сверкой с уже полученным ScreenConfigResponse.layout_version.
@Serializable
data class ScreenUpdatedEvent(
    val screen_id: String,
    val layout_version: Int? = null,
)

// Этап 10 (§15 DISPLAY_ARCHITECTURE.md) — self-hosted OTA. available=false
// значит "уже последняя версия", не ошибка; url ведёт на GitHub Releases
// asset, не на сам KARTEL Supabase (APK-файл не хранится в БД).
@Serializable
data class CheckUpdateResponse(
    val ok: Boolean,
    val reason: String? = null,
    val available: Boolean = false,
    val version: String? = null,
    val version_code: Int? = null,
    val url: String? = null,
    val mandatory: Boolean = false,
)
