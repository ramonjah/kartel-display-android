// KARTEL Display — Android TV client, app module.
// Renderer MVP (Этап 5, DISPLAY_ARCHITECTURE.md): Compose for TV rendering
// одного Layout, полученного один раз через display_get_screen_config —
// без Realtime-подписки ещё (Этап 6, sync-модуль).

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.kartel.display"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kartel.display"
        minSdk = 28 // Android TV boxes в реальном парке редко ниже Android 9
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-mvp"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    // Compose for TV — официальный TV-SDK (§5 DISPLAY_ARCHITECTURE.md:
    // "Рекомендация: основной движок рендера"), не WebView/Canvas.
    implementation("androidx.tv:tv-foundation:1.0.0-alpha12")
    implementation("androidx.tv:tv-material:1.0.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.1")

    // Сеть: те же RPC, что уже проверены живьём против production Supabase
    // (display_request_pairing/claim/get_screen_config/heartbeat/log_event).
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    // Broadcast, не postgres_changes — RLS на screens/layouts/playlists
    // блокирует anon SELECT (device token — единственная граница доверия),
    // сервер шлёт realtime.send() на topic screen:<id> (миграция 058).
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-okhttp:2.3.11")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Изображения (image-виджет) и локальный кэш ассетов (§13 offline).
    implementation("io.coil-kt:coil-compose:2.6.0")

    // QR-виджет — та же независимая от Android библиотека, что и везде в вебе.
    implementation("com.google.zxing:core:3.5.3")

    // Device token — Android Keystore-backed, не plaintext (§16 security).
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
