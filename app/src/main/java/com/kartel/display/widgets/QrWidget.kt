package com.kartel.display.widgets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// zxing — та же независимая от платформы библиотека, что использовалась бы
// и на вебе; чистый JVM-код, без сетевого сервиса генерации QR (§7: экран
// не должен зависеть от внешнего провайдера ради статичной картинки).
@Composable
fun QrWidget(config: JsonElement?) {
    val url = config?.jsonObject?.get("url")?.jsonPrimitive?.content
    if (url == null) {
        UnknownWidget("qr (нет url в config)")
        return
    }
    val bitmap = remember(url) { generateQrBitmap(url, 512) }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.padding(16.dp))
        }
        Text(text = url)
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? = try {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) for (y in 0 until size) {
            setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
} catch (_: Exception) {
    null
}
