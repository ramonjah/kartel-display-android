# KARTEL Display — Android TV Client

Digital Signage-клиент для Android TV. Реализует L5-клиент из
`DISPLAY_ARCHITECTURE.md` (см. `rstore-dashboard` репозиторий) —
рендерит Layout/Playlist, которые владелец собрал в KARTEL BUSINESS →
«Экраны», на телевизоре в точке.

Backend (миграция 056/057, RPC-каталог, BUSINESS UI, pairing) —
в `rstore-dashboard`, уже в production. Этот репозиторий — только
Android-клиент; отдельный язык (Kotlin), отдельный тулчейн (Gradle),
отдельный релизный цикл (§2 DISPLAY_ARCHITECTURE.md — не тянется в
`npm run build`).

## Текущий статус — Этап 10 (APK Updater + Diagnostics)

- ✅ Pairing (код с экрана → owner подтверждает в BUSINESS UI → токен single-read)
- ✅ Получение Layout/Playlist через `display_get_screen_config` (playlist.items резолвится против content_items — миграция 059)
- ✅ Рендер зон: `clock`, `image` (+ слайдер по Playlist), `qr`, `weather` (Open-Meteo напрямую, без нового RPC), `promo_banner` (Playlist изображений/видео), `video` (Media3 ExoPlayer, url или Playlist), `html`/`web` (WebView), `youtube` (WebView embed) — остальные типы (`product_grid`, `top_products`, `daily_brief`) — честный `UnknownWidget`-плейсхолдер, не притворяется
- ✅ Heartbeat раз в 30с
- ✅ Realtime-подписка (diff по `layout_version`, broadcast-канал `screen:<id>`, миграция 058 в `rstore-dashboard`) + периодический fallback-refresh раз в 60с
- ✅ Offline-кэш последнего успешного `ScreenConfigResponse` — рендер сразу на холодном старте без сети
- ✅ Self-hosted OTA (`updater/`) — проверка раз в 6 часов (`display_check_update`), скачивание, системный install-prompt через FileProvider (не silent — приложение не system-signed). Публикация релизов — вручную из KARTEL BUSINESS «Экраны» → «Обновления»
- ✅ Диагностика — `device_events` (boot/error/apk_update/paired/revoked) видны владельцу в BUSINESS UI
- ❌ `product_grid` (Меню/Товары) — отложен: требует новой projection-таблицы products/stock в Postgres (сейчас остатки МойСклад считаются только живьём в браузере), отдельная задача
- ❌ Полноценный offline-кэш медиа-ассетов (download-ahead картинок/видео) — пока только то, что Coil/ExoPlayer кэшируют сами по умолчанию

**Репозиторий публичный** (с Этапа 10) — self-hosted OTA раздаёт APK через
GitHub Releases asset URL, а приватный репозиторий отдаёт такие ссылки
с 404 без авторизации (найдено живой проверкой) — устройства в поле не
смогли бы скачать обновление. Секретов в исходниках нет по дизайну (anon
Supabase key публичен намеренно, device token не хранится в репозитории
— выдаётся динамически через pairing).

## Модули (`app/src/main/java/com/kartel/display/`)

| Модуль | Назначение |
|---|---|
| `registration/` | Pairing flow — код, поллинг, токен |
| `network/` | Supabase Postgrest RPC-клиент + DTO |
| `storage/` | `TokenStore` — Keystore-backed (не plaintext); `ConfigCache` — offline-кэш последнего Layout/Playlist |
| `sync/` | `ScreenSyncManager` — cache-first + `display_get_screen_config` + Realtime broadcast-подписка, diff по `layout_version` |
| `renderer/` | `ScreenRenderer` — относительные координаты зон → Compose |
| `widgets/` | Реестр типов виджетов, каждый — свой Composable |
| `updater/` | `ApkDownloader`/`ApkInstaller`/`UpdateChecker` — self-hosted OTA (§15) |

Полный список будущих модулей (`settings/`, `crash/`) — см.
DISPLAY_ARCHITECTURE.md §12, добавляются по мере продвижения по roadmap.

## Технология

- **Kotlin + Jetpack Compose** (не WebView/Canvas — обоснование в §5/6 DISPLAY_ARCHITECTURE.md)
- **Supabase Kotlin SDK** (Postgrest) — те же RPC, что уже проверены живьём против production backend
- **Coil** — изображения (image-виджет), диск-кэш из коробки
- **ZXing** — QR-генерация, чистый JVM, без внешнего сервиса

## Сборка

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.19/libexec/openjdk.jdk/Contents/Home
./gradlew assembleDebug
```

## Локальная проверка на эмуляторе Android TV

```bash
avdmanager create avd -n kartel_tv_test -k "system-images;android-33;android-tv;arm64-v8a" -d "tv_1080p"
emulator -avd kartel_tv_test -no-audio -no-boot-anim &
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kartel.display/.MainActivity
```

Pairing-код для теста можно сгенерировать вручную с сервера (пока нет
второго устройства для параллельного теста):

```sql
select display_request_pairing('manual test');
```
