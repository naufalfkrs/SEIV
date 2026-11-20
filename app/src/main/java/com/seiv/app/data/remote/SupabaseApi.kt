package com.seiv.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.seiv.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseException(message: String, val status: Int = 0) : Exception(message)

data class AuthUser(val id: String, val email: String, val name: String)

/**
 * Klien minimal untuk Supabase (GoTrue Auth, PostgREST, RPC, Edge Functions)
 * memakai OkHttp. Token sesi disimpan di SharedPreferences privat aplikasi.
 */
object SupabaseApi {
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS) // balasan AI bisa memakan waktu
        .build()
    private val refreshLock = Mutex()
    private lateinit var prefs: SharedPreferences

    private val baseUrl get() = BuildConfig.SUPABASE_URL
    private val anonKey get() = BuildConfig.SUPABASE_ANON_KEY

    /** true bila URL & anon key Supabase sudah diisi di local.properties saat build. */
    val isConfigured: Boolean get() = baseUrl.startsWith("http") && anonKey.isNotBlank()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("seiv_auth", Context.MODE_PRIVATE)
    }

    val hasSession: Boolean get() = isConfigured && prefs.getString("refresh_token", null) != null
    val userId: String? get() = prefs.getString("user_id", null)

    // ------------------------------------------------------------------ Auth

    /** @return user bila sesi langsung aktif, null bila perlu verifikasi email dulu. */
    suspend fun signUp(email: String, password: String, name: String): AuthUser? {
        val body = JSONObject().put("email", email).put("password", password)
            .put("data", JSONObject().put("name", name))
        val res = call("POST", "/auth/v1/signup", body, auth = false) as JSONObject
        return if (res.has("access_token")) saveSession(res) else null
    }

    suspend fun signIn(email: String, password: String): AuthUser {
        val body = JSONObject().put("email", email).put("password", password)
        return saveSession(call("POST", "/auth/v1/token?grant_type=password", body, auth = false) as JSONObject)
    }

    suspend fun signInWithGoogleIdToken(idToken: String): AuthUser {
        val body = JSONObject().put("provider", "google").put("id_token", idToken)
        return saveSession(call("POST", "/auth/v1/token?grant_type=id_token", body, auth = false) as JSONObject)
    }

    suspend fun signOut() {
        runCatching { call("POST", "/auth/v1/logout", JSONObject()) }
        prefs.edit().clear().apply()
    }

    private fun saveSession(o: JSONObject): AuthUser {
        val user = o.getJSONObject("user")
        val meta = user.optJSONObject("user_metadata") ?: JSONObject()
        val expiresIn = o.optLong("expires_in", 3600)
        prefs.edit()
            .putString("access_token", o.getString("access_token"))
            .putString("refresh_token", o.getString("refresh_token"))
            .putLong("expires_at", System.currentTimeMillis() + (expiresIn - 60) * 1000)
            .putString("user_id", user.getString("id"))
            .apply()
        return AuthUser(
            id = user.getString("id"),
            email = user.optString("email"),
            name = meta.optString("name").ifBlank { meta.optString("full_name") },
        )
    }

    private suspend fun accessToken(): String = refreshLock.withLock {
        val token = prefs.getString("access_token", null) ?: throw SupabaseException("Belum login", 401)
        if (System.currentTimeMillis() < prefs.getLong("expires_at", 0)) return token
        val refresh = prefs.getString("refresh_token", null) ?: throw SupabaseException("Sesi berakhir", 401)
        val res = call("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh), auth = false) as JSONObject
        saveSession(res)
        res.getString("access_token")
    }

    // ------------------------------------------------------------------ Data

    suspend fun select(table: String, query: String = "select=*"): JSONArray =
        call("GET", "/rest/v1/$table?$query", null) as JSONArray

    suspend fun insert(table: String, row: JSONObject, upsert: Boolean = false): JSONObject? {
        val res = call(
            "POST", "/rest/v1/$table", row,
            prefer = if (upsert) "return=representation,resolution=ignore-duplicates" else "return=representation",
        )
        // Kosong bila baris sudah ada (ignore-duplicates) — dianggap berhasil
        return (res as? JSONArray)?.optJSONObject(0)
    }

    suspend fun update(table: String, filter: String, values: JSONObject) {
        call("PATCH", "/rest/v1/$table?$filter", values, prefer = "return=minimal")
    }

    suspend fun delete(table: String, filter: String) {
        call("DELETE", "/rest/v1/$table?$filter", null, prefer = "return=minimal")
    }

    suspend fun rpc(fn: String, args: JSONObject): Any? = call("POST", "/rest/v1/rpc/$fn", args)

    suspend fun invokeFunction(name: String, body: JSONObject): JSONObject =
        call("POST", "/functions/v1/$name", body) as JSONObject

    // ------------------------------------------------------------------ HTTP

    private suspend fun call(
        method: String,
        path: String,
        body: JSONObject?,
        auth: Boolean = true,
        prefer: String? = null,
    ): Any? {
        if (!isConfigured) throw SupabaseException("Backend belum dikonfigurasi")
        val token = if (auth) accessToken() else anonKey
        return withContext(Dispatchers.IO) {
            val url = (baseUrl + path).toHttpUrl()
            val builder = Request.Builder().url(url)
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $token")
            if (prefer != null) builder.header("Prefer", prefer)
            val reqBody = (body?.toString() ?: if (method == "GET" || method == "DELETE") null else "{}")?.toRequestBody(JSON)
            builder.method(method, reqBody)
            val response = try {
                http.newCall(builder.build()).execute()
            } catch (e: IOException) {
                throw SupabaseException("Tidak ada koneksi internet")
            }
            response.use { r ->
                val text = r.body?.string().orEmpty()
                if (!r.isSuccessful) throw SupabaseException(errorMessage(text, r.code), r.code)
                when {
                    text.isBlank() -> null
                    text.trimStart().startsWith("[") -> JSONArray(text)
                    text.trimStart().startsWith("{") -> JSONObject(text)
                    else -> text
                }
            }
        }
    }

    private fun errorMessage(text: String, code: Int): String {
        val o = runCatching { JSONObject(text) }.getOrNull()
        val raw = o?.let {
            it.optString("error_description").ifBlank { it.optString("msg") }
                .ifBlank { it.optString("message") }.ifBlank { it.optString("error") }
        }.orEmpty()
        return when {
            raw.contains("Invalid login credentials", true) -> "Email atau kata sandi salah."
            raw.contains("already registered", true) -> "Email ini sudah terdaftar. Silakan masuk."
            raw.contains("Email not confirmed", true) -> "Email belum diverifikasi. Cek kotak masukmu."
            raw.contains("Password should be", true) -> "Kata sandi terlalu lemah (minimal 6 karakter)."
            raw.contains("duplicate key", true) && raw.contains("slot", true) -> "Jadwal ini baru saja dipesan orang lain. Pilih jam lain."
            code == 429 -> "Terlalu banyak percobaan. Coba lagi sebentar."
            raw.isNotBlank() -> raw
            else -> "Terjadi kesalahan server ($code)."
        }
    }
}
