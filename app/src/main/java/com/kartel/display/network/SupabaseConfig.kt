package com.kartel.display.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// Тот же production-проект и тот же публичный anon-ключ, что уже встроен в
// веб-бандл rstore-dashboard (CLAUDE.md → Supabase MCP OOM Fallback) — anon
// key публичен по дизайну (ADR-2, E1 baseline): все display_* RPC сами
// проверяют device token/pairing-код внутри функции, роль Postgres не несёт
// авторизационной нагрузки.
private const val SUPABASE_URL = "https://aaenoxwooahexostuzwe.supabase.co"
private const val SUPABASE_ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFhZW5veHdvb2FoZXhvc3R1endlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI2NzQwMDMsImV4cCI6MjA5ODI1MDAwM30.iOniuxvgR2J0LY93hQ8Hx7Af6Mon5_3o7f7EmJUGMEs"

object SupabaseConfig {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
        }
    }
}
