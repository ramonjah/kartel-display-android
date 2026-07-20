// Системный install-prompt через ACTION_VIEW + FileProvider content://
// URI (§15 DISPLAY_ARCHITECTURE.md) — НЕ silent install. Это приложение
// не system-signed/device-owner, поэтому PackageInstaller-сессии без
// участия пользователя недоступны на обычном Android TV сайдлоаде; тот
// же путь, которым сегодня обновляется любой сторонний launcher/
// signage-клиент — один раз включить "install unknown apps" для этого
// приложения (системная настройка, не наш экран), дальше — обычный
// системный диалог подтверждения на каждое обновление.

package com.kartel.display.updater

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    fun promptInstall(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
