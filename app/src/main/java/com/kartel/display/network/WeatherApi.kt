// Погода напрямую с Open-Meteo — та же схема, что WeatherAnalyticsService.js
// в rstore-dashboard: бесплатный провайдер без API-ключа, CORS/аутентификация
// не нужны, поэтому отдельный KARTEL RPC не оправдан (§10 DISPLAY_ARCHITECTURE.md
// MVP Templates) — устройство обращается к Open-Meteo так же, как обращался бы
// браузер.

package com.kartel.display.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val weathercode: Int,
    val windspeed: Double,
)

@Serializable
data class WeatherForecastResponse(
    val current_weather: CurrentWeather? = null,
)

object WeatherApi {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchCurrent(lat: Double, lon: Double): CurrentWeather? =
        runCatching {
            client.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current_weather", true)
                parameter("timezone", "auto")
            }.body<WeatherForecastResponse>().current_weather
        }.getOrNull()
}
