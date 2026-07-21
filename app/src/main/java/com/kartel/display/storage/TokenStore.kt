// Device token — хранится в Android Keystore-backed EncryptedSharedPreferences,
// не plaintext (DISPLAY_ARCHITECTURE.md §16: физическое устройство стоит в
// публичном месте, кража токена оттуда реалистичнее, чем из браузера).

package com.kartel.display.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    // EncryptedSharedPreferences (security-crypto alpha) кидает исключение на
    // части дешёвых Android TV боксов со сломанным Keystore/StrongBox — это роняет
    // onCreate ещё до setContent и даёт чёрный экран. Фолбэк на обычные
    // SharedPreferences: устройство остаётся рабочим; device-токен и так узкоскоупный
    // и отзываемый сервером (ADR-011 §5.1), это приемлемая деградация против
    // неработающего устройства. Целевое хранилище — по-прежнему шифрованное.
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "kartel_display_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        context.getSharedPreferences("kartel_display_prefs", Context.MODE_PRIVATE)
    }

    var deviceToken: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    fun clear() = prefs.edit().remove(KEY_TOKEN).apply()

    companion object {
        private const val KEY_TOKEN = "device_token"
    }
}
