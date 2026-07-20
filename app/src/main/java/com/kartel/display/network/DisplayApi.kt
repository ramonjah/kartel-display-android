// Тонкая обёртка над device-facing RPC (миграция 056, rstore-dashboard) —
// та же самая грань, что уже живьём проверена в задаче Backend API:
// request_pairing → claim_pairing → get_screen_config/heartbeat/log_event.
// Ни один вызов здесь не решает бизнес-логику — только вызывает
// способность и возвращает то, что сказал сервер (ADR-000 §8: клиент
// отображает и вызывает, никогда не решает).

package com.kartel.display.network

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DisplayApi(private val client: io.github.jan.supabase.SupabaseClient = SupabaseConfig.client) {

    suspend fun requestPairing(deviceHint: String? = null): RequestPairingResponse =
        client.postgrest.rpc(
            "display_request_pairing",
            buildJsonObject { put("p_device_hint", deviceHint) },
        ).decodeAs()

    suspend fun claimPairing(pairingSessionId: String): ClaimPairingResponse =
        client.postgrest.rpc(
            "display_claim_pairing",
            buildJsonObject { put("p_pairing_session_id", pairingSessionId) },
        ).decodeAs()

    suspend fun deviceHeartbeat(deviceToken: String, appVersion: String? = null): SimpleOkResponse =
        client.postgrest.rpc(
            "display_device_heartbeat",
            buildJsonObject {
                put("p_device_token", deviceToken)
                put("p_app_version", appVersion)
            },
        ).decodeAs()

    suspend fun getScreenConfig(deviceToken: String): ScreenConfigResponse =
        client.postgrest.rpc(
            "display_get_screen_config",
            buildJsonObject { put("p_device_token", deviceToken) },
        ).decodeAs()

    suspend fun logDeviceEvent(deviceToken: String, kind: String, payload: JsonObject = buildJsonObject {}): SimpleOkResponse =
        client.postgrest.rpc(
            "display_log_device_event",
            buildJsonObject {
                put("p_device_token", deviceToken)
                put("p_kind", kind)
                put("p_payload", payload)
            },
        ).decodeAs()
}
