package com.seiv.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class User(
    val name: String,
    val email: String,
    val profile: Profile = Profile(),
)

/** Data diri pengguna. occupation: "mahasiswa" | "bekerja" | "lainnya" | "" (belum diisi). */
data class Profile(
    val fullName: String = "",
    val address: String = "",
    val occupation: String = "",
    val institution: String = "",
    val faculty: String = "",
    val phone: String = "",
    val guardianPhone: String = "",
) {
    val isStudent get() = occupation == "mahasiswa"

    /** Wajib diisi: nama lengkap, domisili, status, no HP, no HP wali (+ instansi & fakultas bila mahasiswa). */
    val isComplete get() = fullName.isNotBlank() && address.isNotBlank() && occupation.isNotBlank() &&
        phone.isNotBlank() && guardianPhone.isNotBlank() &&
        (!isStudent || (institution.isNotBlank() && faculty.isNotBlank()))

    fun toJson(): JSONObject = JSONObject().put("full_name", fullName).put("address", address)
        .put("occupation", occupation).put("institution", if (isStudent) institution else "")
        .put("faculty", if (isStudent) faculty else "").put("phone", phone).put("guardian_phone", guardianPhone)

    companion object {
        fun fromJson(o: JSONObject?) = if (o == null) Profile() else Profile(
            o.optString("full_name"), o.optString("address"), o.optString("occupation"),
            o.optString("institution"), o.optString("faculty"), o.optString("phone"), o.optString("guardian_phone"),
        )
    }
}

data class CheckIn(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val mood: Int, // 0 = sangat buruk .. 4 = sangat baik
    val factors: List<String>,
    val feelings: List<String>,
    val note: String,
)

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val prompt: String,
    val text: String,
)

data class ScreeningResult(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val srq: Int,
    val gad: Int,
    val phq: Int,
    val phqItem9: Int,
    // Instrumen tambahan v2 — null pada hasil skrining versi lama (3 instrumen)
    val phqDifficulty: Int? = null,
    val gadDifficulty: Int? = null,
    val asq: Int? = null,
    val asqAttempt: Int? = null,
    val asqAcute: Boolean? = null,
    val cape: Int? = null,
    val capeFrequent: Int? = null,
    val cdrisc: Int? = null,
    // Total per domain CAPE-P15 (null pada hasil lama)
    val capePi: Int? = null,
    val capeBe: Int? = null,
    val capePa: Int? = null,
    /** Bitmask jawaban SRQ-20: bit (n-1) = butir n dijawab "Ya" (null pada hasil lama). */
    val srqItems: Int? = null,
) {
    val isV2 get() = asq != null && cape != null && cdrisc != null

    fun toJson(): JSONObject = JSONObject().put("srq", srq).put("gad", gad).put("phq", phq).put("phq_item9", phqItem9)
        .put("phq_difficulty", phqDifficulty ?: JSONObject.NULL).put("gad_difficulty", gadDifficulty ?: JSONObject.NULL)
        .put("asq", asq ?: JSONObject.NULL).put("asq_attempt", asqAttempt ?: JSONObject.NULL)
        .put("asq_acute", asqAcute ?: JSONObject.NULL).put("cape", cape ?: JSONObject.NULL)
        .put("cape_frequent", capeFrequent ?: JSONObject.NULL).put("cdrisc", cdrisc ?: JSONObject.NULL)
        .put("cape_pi", capePi ?: JSONObject.NULL).put("cape_be", capeBe ?: JSONObject.NULL).put("cape_pa", capePa ?: JSONObject.NULL)
        .put("srq_items", srqItems ?: JSONObject.NULL)

    companion object {
        private fun JSONObject.intOrNull(k: String) = if (has(k) && !isNull(k)) getInt(k) else null
        private fun JSONObject.boolOrNull(k: String) = if (has(k) && !isNull(k)) getBoolean(k) else null

        /** Baca dari JSON server / lokal (kolom sama dengan tabel screenings). */
        fun fromJson(id: String, ts: Long, o: JSONObject) = ScreeningResult(
            id, ts, o.getInt("srq"), o.getInt("gad"), o.getInt("phq"), o.optInt("phq_item9", o.optInt("phq9")),
            o.intOrNull("phq_difficulty"), o.intOrNull("gad_difficulty"), o.intOrNull("asq"), o.intOrNull("asq_attempt"),
            o.boolOrNull("asq_acute"), o.intOrNull("cape"), o.intOrNull("cape_frequent"), o.intOrNull("cdrisc"),
            o.intOrNull("cape_pi"), o.intOrNull("cape_be"), o.intOrNull("cape_pa"), o.intOrNull("srq_items"),
        )
    }
}

/** Ulasan mahasiswa untuk sesi yang sudah selesai (1–5 bintang + komentar). */
data class Review(
    val sessionId: String,
    val counselorId: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis(),
)

object SessionStatus {
    const val WAITING = "waiting_verification"
    const val CONFIRMED = "confirmed"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"

    fun label(status: String) = when (status) {
        WAITING -> "Menunggu verifikasi"
        CONFIRMED -> "Terkonfirmasi"
        COMPLETED -> "Selesai"
        CANCELLED -> "Dibatalkan"
        else -> status
    }
}

data class Session(
    val id: String = "SEIV" + (100000..999999).random(),
    val counselorId: String,
    val dateMillis: Long,
    val time: String,
    val method: String,
    val note: String,
    val shareCheckIn: Boolean,
    val paymentMethod: String,
    val total: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = SessionStatus.CONFIRMED,
    val counselorNote: String = "",
    /** Waktu terakhir status/pesan konselor berubah di server (untuk urutan notifikasi). */
    val updatedAt: Long = createdAt,
)

/**
 * Penyimpanan lokal (SharedPreferences + JSON) sekaligus cache untuk data Supabase.
 * UI selalu membaca dari sini; [com.seiv.app.data.remote.SeivSync] menyinkronkannya dengan server.
 */
object SeivStore {
    private lateinit var prefs: SharedPreferences

    var onboarded by mutableStateOf(false)
        private set
    var user by mutableStateOf<User?>(null)
        private set
    val checkIns = mutableStateListOf<CheckIn>()
    val journals = mutableStateListOf<JournalEntry>()
    val screenings = mutableStateListOf<ScreeningResult>()
    val sessions = mutableStateListOf<Session>()
    val counselors = mutableStateListOf<Counselor>().apply { addAll(COUNSELORS) }
    val reviews = mutableStateListOf<Review>()

    fun reviewFor(sessionId: String) = reviews.firstOrNull { it.sessionId == sessionId }

    fun upsertReview(r: Review) {
        reviews.removeAll { it.sessionId == r.sessionId }
        reviews.add(0, r)
        saveReviews()
    }

    fun setReviews(list: List<Review>) {
        reviews.clear(); reviews.addAll(list); saveReviews()
    }

    private fun saveReviews() = writeArray("reviews", reviews) {
        JSONObject().put("session", it.sessionId).put("counselor", it.counselorId).put("rating", it.rating)
            .put("comment", it.comment).put("created", it.createdAt)
    }

    /** Item yang belum berhasil dikirim ke server, format "tabel:id". */
    private val pending = mutableSetOf<String>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("seiv", Context.MODE_PRIVATE)
        onboarded = prefs.getBoolean("onboarded", false)
        prefs.getString("user", null)?.let {
            val o = JSONObject(it)
            user = User(o.getString("name"), o.getString("email"), Profile.fromJson(o.optJSONObject("profile")))
        }
        pending.addAll(prefs.getStringSet("pending", emptySet()).orEmpty())
        checkIns.addAll(readArray("checkins", ::checkInFromJson))
        journals.addAll(readArray("journals") { o ->
            JournalEntry(o.getString("id"), o.getLong("ts"), o.optString("prompt"), o.getString("text"))
        })
        screenings.addAll(readArray("screenings") { o -> ScreeningResult.fromJson(o.getString("id"), o.getLong("ts"), o) })
        seenNotifications.addAll(prefs.getStringSet("seen_notif", emptySet()).orEmpty())
        dismissedNotifications.addAll(prefs.getStringSet("dismissed_notif", emptySet()).orEmpty())
        runCatching {
            val o = JSONObject(prefs.getString("notif_times", "{}")!!)
            o.keys().forEach { notifTimes[it] = o.getLong(it) }
        }
        sessions.addAll(readArray("sessions") { o ->
            Session(
                o.getString("id"), o.getString("counselor"), o.getLong("date"), o.getString("time"),
                o.getString("method"), o.optString("note"), o.optBoolean("share"),
                o.getString("pay"), o.getInt("total"), o.getLong("created"),
                o.optString("status", SessionStatus.CONFIRMED), o.optString("cnote"), o.optLong("updated", o.getLong("created")),
            )
        })
        readArray("counselors", ::counselorFromJson).takeIf { it.isNotEmpty() }?.let { setCounselors(it, persist = false) }
        reviews.addAll(readArray("reviews") { o ->
            Review(o.getString("session"), o.getString("counselor"), o.getInt("rating"), o.optString("comment"), o.optLong("created"))
        })
    }

    // ------------------------------------------------------------ akun

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    fun login(name: String, email: String, profile: Profile = user?.takeIf { it.email == email }?.profile ?: Profile()) {
        user = User(name, email, profile)
        prefs.edit().putString(
            "user", JSONObject().put("name", name).put("email", email).put("profile", profile.toJson()).toString(),
        ).apply()
    }

    fun updateProfile(p: Profile) {
        val u = user ?: return
        login(u.name, u.email, p)
    }

    // ------------------------------------------------------------ notifikasi (sudah dibaca)

    val seenNotifications = androidx.compose.runtime.mutableStateListOf<String>()

    fun markNotificationsSeen(ids: Collection<String>) {
        val new = ids.filterNot { it in seenNotifications }
        if (new.isEmpty()) return
        seenNotifications.addAll(new)
        prefs.edit().putStringSet("seen_notif", seenNotifications.toSet()).apply()
    }

    /** Notifikasi yang disembunyikan/dihapus pengguna. */
    val dismissedNotifications = androidx.compose.runtime.mutableStateListOf<String>()

    fun dismissNotifications(ids: Collection<String>) {
        val new = ids.filterNot { it in dismissedNotifications }
        if (new.isEmpty()) return
        dismissedNotifications.addAll(new)
        prefs.edit().putStringSet("dismissed_notif", dismissedNotifications.toSet()).apply()
    }

    /**
     * Waktu notifikasi pertama kali muncul. Dipakai agar urutan stabil dan kronologis:
     * kejadian memakai waktu aslinya, pengingat memakai saat pertama kali tampil.
     */
    private val notifTimes = mutableMapOf<String, Long>()

    fun notificationTime(id: String, eventTime: Long): Long {
        if (!::prefs.isInitialized) return eventTime
        notifTimes[id]?.let { return it }
        val t = minOf(eventTime, System.currentTimeMillis())
        notifTimes[id] = t
        // Simpan; buang catatan lama (> 300 entri) agar tidak membengkak
        val keep = notifTimes.entries.sortedByDescending { it.value }.take(300)
        prefs.edit().putString("notif_times", JSONObject().apply { keep.forEach { put(it.key, it.value) } }.toString()).apply()
        return t
    }

    fun logout(clearData: Boolean) {
        user = null
        prefs.edit().remove("user").apply()
        if (clearData) {
            checkIns.clear(); journals.clear(); screenings.clear(); sessions.clear(); pending.clear(); reviews.clear()
            prefs.edit().remove("checkins").remove("journals").remove("screenings").remove("sessions").remove("pending").remove("reviews").apply()
        }
    }

    fun updateName(name: String) {
        val u = user ?: return
        login(name, u.email)
    }

    // ------------------------------------------------------------ tulis lokal

    fun addCheckIn(c: CheckIn) { checkIns.add(0, c); saveCheckIns() }
    fun addJournal(j: JournalEntry) { journals.add(0, j); saveJournals() }
    fun deleteJournal(id: String) { journals.removeAll { it.id == id }; saveJournals() }
    fun addScreening(s: ScreeningResult) { screenings.add(0, s); saveScreenings() }
    fun addSession(s: Session) { sessions.add(0, s); saveSessions() }

    fun updateSession(s: Session) {
        val i = sessions.indexOfFirst { it.id == s.id }
        if (i >= 0) { sessions[i] = s; saveSessions() }
    }

    fun counselorById(id: String): Counselor = counselors.firstOrNull { it.id == id }
        ?: COUNSELORS.firstOrNull { it.id == id } ?: counselors.first()

    fun setCounselors(list: List<Counselor>, persist: Boolean = true) {
        if (list.isEmpty()) return
        counselors.clear(); counselors.addAll(list)
        if (persist) writeArray("counselors", list, ::counselorToJson)
    }

    // ------------------------------------------------------------ data dari server

    fun replaceRemote(
        remoteCheckIns: List<CheckIn>, remoteJournals: List<JournalEntry>,
        remoteScreenings: List<ScreeningResult>, remoteSessions: List<Session>,
    ) {
        fun <T> merge(local: List<T>, remote: List<T>, table: String, id: (T) -> String, ts: (T) -> Long): List<T> {
            val keepLocal = local.filter { "$table:${id(it)}" in pending && remote.none { r -> id(r) == id(it) } }
            return (remote + keepLocal).sortedByDescending(ts)
        }
        val ci = merge(checkIns, remoteCheckIns, "check_ins", { it.id }, { it.timestamp })
        val jo = merge(journals, remoteJournals, "journals", { it.id }, { it.timestamp })
        val sc = merge(screenings, remoteScreenings, "screenings", { it.id }, { it.timestamp })
        val se = merge(sessions, remoteSessions, "sessions", { it.id }, { it.createdAt })
        checkIns.clear(); checkIns.addAll(ci); saveCheckIns()
        journals.clear(); journals.addAll(jo); saveJournals()
        screenings.clear(); screenings.addAll(sc); saveScreenings()
        sessions.clear(); sessions.addAll(se); saveSessions()
    }

    // ------------------------------------------------------------ antrean sinkron

    fun markPending(key: String) { pending.add(key); savePending() }
    fun clearPending(key: String) { pending.remove(key); savePending() }
    fun pendingKeys(): List<String> = pending.toList()
    private fun savePending() = prefs.edit().putStringSet("pending", pending.toSet()).apply()

    // ------------------------------------------------------------ kueri

    fun todayCheckIn(): CheckIn? = checkIns.firstOrNull { isSameDay(it.timestamp, System.currentTimeMillis()) }

    /** Jumlah hari berturut-turut (sampai hari ini / kemarin) pengguna melakukan check-in. */
    fun streak(): Int {
        if (checkIns.isEmpty()) return 0
        val days = checkIns.map { dayKey(it.timestamp) }.toSet()
        val cal = Calendar.getInstance()
        if (!days.contains(dayKey(cal.timeInMillis))) cal.add(Calendar.DAY_OF_YEAR, -1)
        var count = 0
        while (days.contains(dayKey(cal.timeInMillis))) {
            count++; cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return count
    }

    // ------------------------------------------------------------ serialisasi

    private fun saveCheckIns() = writeArray("checkins", checkIns) {
        JSONObject().put("id", it.id).put("ts", it.timestamp).put("mood", it.mood)
            .put("factors", JSONArray(it.factors)).put("feelings", JSONArray(it.feelings)).put("note", it.note)
    }

    private fun checkInFromJson(o: JSONObject) = CheckIn(
        o.getString("id"), o.getLong("ts"), o.getInt("mood"),
        o.getJSONArray("factors").toStrings(), o.getJSONArray("feelings").toStrings(), o.optString("note"),
    )

    private fun saveJournals() = writeArray("journals", journals) {
        JSONObject().put("id", it.id).put("ts", it.timestamp).put("prompt", it.prompt).put("text", it.text)
    }

    private fun saveScreenings() = writeArray("screenings", screenings) {
        it.toJson().put("id", it.id).put("ts", it.timestamp)
    }

    private fun saveSessions() = writeArray("sessions", sessions) {
        JSONObject().put("id", it.id).put("counselor", it.counselorId).put("date", it.dateMillis)
            .put("time", it.time).put("method", it.method).put("note", it.note)
            .put("share", it.shareCheckIn).put("pay", it.paymentMethod).put("total", it.total)
            .put("created", it.createdAt).put("status", it.status).put("cnote", it.counselorNote).put("updated", it.updatedAt)
    }

    private fun counselorToJson(c: Counselor) = JSONObject().put("id", c.id).put("name", c.name).put("role", c.role)
        .put("rating", c.rating).put("reviews", c.reviews).put("tags", JSONArray(c.tags)).put("bio", c.bio)
        .put("price", c.price).put("initials", c.initials).put("color", c.colorIndex)

    private fun counselorFromJson(o: JSONObject) = Counselor(
        o.getString("id"), o.getString("name"), o.getString("role"), o.getDouble("rating"), o.getInt("reviews"),
        o.getJSONArray("tags").toStrings(), o.optString("bio"), o.getInt("price"), o.optString("initials"), o.optInt("color"),
    )

    private fun <T> writeArray(key: String, list: List<T>, map: (T) -> JSONObject) {
        val arr = JSONArray()
        list.forEach { arr.put(map(it)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun <T> readArray(key: String, map: (JSONObject) -> T): List<T> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { map(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}

fun JSONArray.toStrings() = (0 until length()).map { getString(it) }

fun dayKey(ts: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

fun isSameDay(a: Long, b: Long) = dayKey(a) == dayKey(b)
