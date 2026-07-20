// Pairing (§9 DISPLAY_ARCHITECTURE.md): запросить код → показать на
// экране → поллить, пока владелец не подтвердит в KARTEL BUSINESS →
// забрать токен РОВНО ОДИН РАЗ (сервер обнуляет его после первого
// чтения — второй claim того же pairing_session_id токен уже не увидит,
// это уже проверено живьём на бэкенде). Клиент не решает, кто он —
// только показывает код и ждёт человека.

package com.kartel.display.registration

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kartel.display.network.DisplayApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PairingState {
    data object Requesting : PairingState
    data class ShowingCode(val code: String, val pairingSessionId: String) : PairingState
    data object Expired : PairingState
    data class Paired(val token: String) : PairingState
    data class Error(val message: String) : PairingState
}

class PairingViewModel(private val api: DisplayApi = DisplayApi()) : ViewModel() {
    private val _state = MutableStateFlow<PairingState>(PairingState.Requesting)
    val state: StateFlow<PairingState> = _state.asStateFlow()

    fun start() {
        viewModelScope.launch {
            requestNewCode()
        }
    }

    private suspend fun requestNewCode() {
        _state.value = PairingState.Requesting
        val res = api.requestPairing(deviceHint = "${Build.MANUFACTURER} ${Build.MODEL}")
        if (!res.ok || res.pairing_session_id == null || res.code == null) {
            _state.value = PairingState.Error(res.reason ?: "pairing_request_failed")
            return
        }
        _state.value = PairingState.ShowingCode(res.code, res.pairing_session_id)
        pollForConfirmation(res.pairing_session_id)
    }

    private suspend fun pollForConfirmation(pairingSessionId: String) {
        while (true) {
            delay(2000)
            val res = api.claimPairing(pairingSessionId)
            when (res.status) {
                "confirmed" -> {
                    if (res.token != null) {
                        _state.value = PairingState.Paired(res.token)
                        return
                    }
                }
                "expired" -> {
                    _state.value = PairingState.Expired
                    delay(1500)
                    requestNewCode()
                    return
                }
                "pending" -> { /* keep polling */ }
                else -> { /* claimed by a previous attempt, or not_found — request fresh code */
                    requestNewCode()
                    return
                }
            }
        }
    }
}
