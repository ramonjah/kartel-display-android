// KARTEL Display — Android TV client, root build file.
// Реализация DISPLAY_ARCHITECTURE.md (rstore-dashboard) — плагины
// объявлены здесь с apply false, реально применяются в app/build.gradle.kts.

plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
}
