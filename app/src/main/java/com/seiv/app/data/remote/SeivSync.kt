package com.seiv.app.data.remote

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.seiv.app.data.ChatMessage
import com.seiv.app.data.CheckIn
import com.seiv.app.data.Counselor
import com.seiv.app.data.JournalEntry
import com.seiv.app.data.ScreeningResult
import com.seiv.app.data.SeivStore
import com.seiv.app.data.Session
import com.seiv.app.data.SessionStatus
import com.seiv.app.data.toStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repositori: semua penulisan data lewat sini.
 * Data selalu disimpan lokal dulu (aplikasi tetap jalan tanpa internet),
 * lalu dikirim ke Supabase. Yang gagal masuk antrean dan dicoba lagi saat [refresh].
 *
 * Bila Supabase belum dikonfigurasi, aplikasi berjalan penuh dalam mode offline.
 */
object SeivSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    val online: Boolean get() = SupabaseApi.isConfigured

    /** true saat [refresh] sedang berjalan. */
    var syncing by mutableStateOf(false)
        private set

    // ------------------------------------------------------------------ akun

    suspend fun signIn(email: String, password: String) {
        val u = SupabaseApi.signIn(email, password)
        afterAuth(u, email)
    }

    /** @return false bila pengguna harus memverifikasi email terlebih dulu. */
    suspend fun signUp(name: String, email: String, password: String): Boolean {
        val u = SupabaseApi.signUp(email, password, name) ?: return false
        afterAuth(u.copy(name = u.name.ifBlank { name }), email)
        return true
    }

    suspend fun signInWithGoogle(idToken: String) {
        val u = SupabaseApi.signInWithGoogleIdToken(idToken)
        afterAuth(u, u.email)
    }

    private suspend fun afterAuth(u: AuthUser, email: String) {
        // Data lokal milik akun lain tidak boleh tercampur
        val previous = SeivStore.user
        if (previous != null && previous.email != email) SeivStore.logout(clearData = true)
        val profile = runCatching {
            SupabaseApi.select("profiles", "select=name&id=eq.${u.id}").optJSONObject(0)
        }.getOrNull()
        val name = profile?.optString("name").orEmpty().ifBlank { u.name }.ifBlank { email.substringBefore("@") }
        SeivStore.login(name, email)
        refresh()
    }

    fun signOut() {
        scope.launch { if (online) SupabaseApi.signOut() }
        // Bila tersinkron ke cloud, data lokal dihapus demi privasi (mis. HP dipakai bergantian)
        SeivStore.logout(clearData = online)
    }

    fun updateName(name: String) {
        SeivStore.updateName(name)
        val id = SupabaseApi.userId ?: return
        if (online) scope.launch { runCatching { SupabaseApi.update("profiles", "id=eq.$id", JSONObject().put("name", name)) } }
    }

    // ------------------------------------------------------------------ tulis

    fun addCheckIn(c: CheckIn) { SeivStore.addCheckIn(c); push("check_ins", c.id) }
    fun addJournal(j: JournalEntry) { SeivStore.addJournal(j); push("journals", j.id) }
    fun addScreening(s: ScreeningResult) { SeivStore.addScreening(s); push("screenings", s.id) }

    fun deleteJournal(id: String) {
        SeivStore.deleteJournal(id)
        SeivStore.clearPending("journals:$id")
        if (online && SupabaseApi.hasSession) scope.launch { runCatching { SupabaseApi.delete("journals", "id=eq.$id") } }
    }

    /**
     * Pemesanan sesi harus langsung tercatat di server (agar slot tidak bentrok),
     * jadi fungsi ini menunggu respons dan melempar error bila gagal.
     */
    suspend fun bookSession(s: Session): Session {
        if (!online) {
            SeivStore.addSession(s); return s
        }
        val saved = s.copy(status = SessionStatus.WAITING)
        SupabaseApi.insert("sessions", sessionToJson(saved))
        SeivStore.addSession(saved)
        return saved
    }

    fun updateSessionPrefs(s: Session) {
        SeivStore.updateSession(s)
        if (online && SupabaseApi.hasSession) scope.launch {
            runCatching {
                SupabaseApi.update("sessions", "id=eq.${s.id}", JSONObject().put("note", s.note).put("share_check_in", s.shareCheckIn))
            }
        }
    }

    /** Jam yang sudah dipesan orang lain untuk konselor & tanggal tsb (format "HH:mm"). */
    suspend fun bookedSlots(counselorId: String, dateMillis: Long): Set<String> {
        if (!online || !SupabaseApi.hasSession) return emptySet()
        val day = Instant.ofEpochMilli(dateMillis).atZone(zone).toLocalDate().toString()
        val res = runCatching {
            SupabaseApi.rpc("booked_slots", JSONObject().put("p_counselor", counselorId).put("p_day", day)) as? JSONArray
        }.getOrNull() ?: return emptySet()
        return (0 until res.length()).map { res.getJSONObject(it).getString("slot") }.toSet()
    }

    // ------------------------------------------------------------------ AI Companion

    /** Balasan dari Claude lewat Edge Function; null bila offline/gagal (pakai mesin lokal). */
    suspend fun companionReply(history: List<ChatMessage>, name: String): String? {
        if (!online || !SupabaseApi.hasSession) return null
        val msgs = JSONArray()
        history.filterNot { it.isCrisis }.forEach {
            msgs.put(JSONObject().put("role", if (it.fromUser) "user" else "assistant").put("content", it.text))
        }
        return runCatching {
            SupabaseApi.invokeFunction("companion-chat", JSONObject().put("messages", msgs).put("name", name))
                .optString("reply").ifBlank { null }
        }.getOrNull()
    }

    // ------------------------------------------------------------------ sinkronisasi

    /** Kirim antrean yang tertunda lalu ambil ulang semua data dari server. */
    fun refreshInBackground() {
        if (online && SupabaseApi.hasSession && !syncing) scope.launch { runCatching { refresh() } }
    }

    suspend fun refresh() {
        if (!online || !SupabaseApi.hasSession) return
        syncing = true
        try {
            SeivStore.pendingKeys().forEach { key -> runCatching { pushNow(key) } }
            runCatching {
                val rows = SupabaseApi.select("counselors", "select=*&active=eq.true&order=created_at")
                SeivStore.setCounselors((0 until rows.length()).map { counselorFromJson(rows.getJSONObject(it)) })
            }
            val uid = SupabaseApi.userId ?: return
            val ci = SupabaseApi.select("check_ins", "select=*&user_id=eq.$uid&order=created_at.desc&limit=365")
            val jo = SupabaseApi.select("journals", "select=*&user_id=eq.$uid&order=created_at.desc")
            val sc = SupabaseApi.select("screenings", "select=*&user_id=eq.$uid&order=created_at.desc")
            val se = SupabaseApi.select("sessions", "select=*&user_id=eq.$uid&order=created_at.desc")
            SeivStore.replaceRemote(
                ci.map(::checkInFromJson), jo.map(::journalFromJson),
                sc.map(::screeningFromJson), se.map(::sessionFromJson),
            )
        } finally {
            syncing = false
        }
    }

    private fun push(table: String, id: String) {
        if (!online) return
        val key = "$table:$id"
        SeivStore.markPending(key)
        if (SupabaseApi.hasSession) scope.launch { runCatching { pushNow(key) } }
    }

    private suspend fun pushNow(key: String) {
        val (table, id) = key.split(":", limit = 2)
        val row = when (table) {
            "check_ins" -> SeivStore.checkIns.firstOrNull { it.id == id }?.let(::checkInToJson)
            "journals" -> SeivStore.journals.firstOrNull { it.id == id }?.let(::journalToJson)
            "screenings" -> SeivStore.screenings.firstOrNull { it.id == id }?.let(::screeningToJson)
            else -> null
        }
        if (row != null) SupabaseApi.insert(table, row, upsert = true)
        SeivStore.clearPending(key)
    }

    // ------------------------------------------------------------------ pemetaan JSON

    private fun iso(ms: Long) = Instant.ofEpochMilli(ms).toString()
    private fun ms(iso: String) = Instant.parse(normalizeIso(iso)).toEpochMilli()

    /** PostgREST mengirim "2026-11-21T10:00:00+00:00"; ubah agar terbaca Instant.parse. */
    private fun normalizeIso(s: String) = java.time.OffsetDateTime.parse(s).toInstant().toString()

    private fun <T> JSONArray.map(f: (JSONObject) -> T) = (0 until length()).map { f(getJSONObject(it)) }

    private fun checkInToJson(c: CheckIn) = JSONObject().put("id", c.id).put("created_at", iso(c.timestamp))
        .put("mood", c.mood).put("factors", JSONArray(c.factors)).put("feelings", JSONArray(c.feelings)).put("note", c.note)

    private fun checkInFromJson(o: JSONObject) = CheckIn(
        o.getString("id"), ms(o.getString("created_at")), o.getInt("mood"),
        o.optJSONArray("factors")?.toStrings().orEmpty(), o.optJSONArray("feelings")?.toStrings().orEmpty(), o.optString("note"),
    )

    private fun journalToJson(j: JournalEntry) = JSONObject().put("id", j.id).put("created_at", iso(j.timestamp))
        .put("prompt", j.prompt).put("body", j.text)

    private fun journalFromJson(o: JSONObject) =
        JournalEntry(o.getString("id"), ms(o.getString("created_at")), o.optString("prompt"), o.getString("body"))

    private fun screeningToJson(s: ScreeningResult) = JSONObject().put("id", s.id).put("created_at", iso(s.timestamp))
        .put("srq", s.srq).put("gad", s.gad).put("phq", s.phq).put("phq_item9", s.phqItem9)

    private fun screeningFromJson(o: JSONObject) = ScreeningResult(
        o.getString("id"), ms(o.getString("created_at")), o.getInt("srq"), o.getInt("gad"), o.getInt("phq"), o.optInt("phq_item9"),
    )

    private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

    private fun sessionToJson(s: Session): JSONObject {
        val date = Instant.ofEpochMilli(s.dateMillis).atZone(zone).toLocalDate()
        val start = LocalDateTime.of(date, LocalTime.parse(s.time, hhmm)).atZone(zone).toInstant()
        return JSONObject().put("id", s.id).put("counselor_id", s.counselorId).put("starts_at", start.toString())
            .put("method", s.method).put("note", s.note).put("share_check_in", s.shareCheckIn)
            .put("payment_method", s.paymentMethod).put("total", s.total).put("status", s.status)
            .put("created_at", iso(s.createdAt))
    }

    private fun sessionFromJson(o: JSONObject): Session {
        val start = java.time.OffsetDateTime.parse(o.getString("starts_at")).atZoneSameInstant(zone)
        val dayStart = LocalDate.from(start).atStartOfDay(zone).toInstant().toEpochMilli()
        return Session(
            id = o.getString("id"), counselorId = o.getString("counselor_id"), dateMillis = dayStart,
            time = start.toLocalTime().format(hhmm), method = o.getString("method"), note = o.optString("note"),
            shareCheckIn = o.optBoolean("share_check_in"), paymentMethod = o.getString("payment_method"),
            total = o.getInt("total"), createdAt = ms(o.getString("created_at")),
            status = o.optString("status", SessionStatus.WAITING), counselorNote = o.optString("counselor_note"),
        )
    }

    private fun counselorFromJson(o: JSONObject) = Counselor(
        id = o.getString("id"), name = o.getString("name"), role = o.getString("title"),
        rating = o.optDouble("rating", 5.0), reviews = o.optInt("reviews"),
        tags = o.optJSONArray("tags")?.toStrings().orEmpty(), bio = o.optString("bio"),
        price = o.getInt("price"), initials = o.optString("initials").ifBlank { o.getString("name").take(2).uppercase() },
        colorIndex = o.optInt("color_index"),
    )
}
