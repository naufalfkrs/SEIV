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

data class User(val name: String, val email: String)

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
)

data class Session(
    val id: String = "SEIV" + (10000..99999).random(),
    val counselorId: String,
    val dateMillis: Long,
    val time: String,
    val method: String,
    val note: String,
    val shareCheckIn: Boolean,
    val paymentMethod: String,
    val total: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Penyimpanan lokal sederhana (SharedPreferences + JSON).
 * Semua data pengguna tersimpan di perangkat saja.
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

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences("seiv", Context.MODE_PRIVATE)
        onboarded = prefs.getBoolean("onboarded", false)
        prefs.getString("user", null)?.let {
            val o = JSONObject(it)
            user = User(o.getString("name"), o.getString("email"))
        }
        checkIns.addAll(readArray("checkins") { o ->
            CheckIn(
                o.getString("id"), o.getLong("ts"), o.getInt("mood"),
                o.getJSONArray("factors").toStrings(), o.getJSONArray("feelings").toStrings(),
                o.optString("note"),
            )
        })
        journals.addAll(readArray("journals") { o ->
            JournalEntry(o.getString("id"), o.getLong("ts"), o.optString("prompt"), o.getString("text"))
        })
        screenings.addAll(readArray("screenings") { o ->
            ScreeningResult(o.getString("id"), o.getLong("ts"), o.getInt("srq"), o.getInt("gad"), o.getInt("phq"), o.optInt("phq9"))
        })
        sessions.addAll(readArray("sessions") { o ->
            Session(
                o.getString("id"), o.getString("counselor"), o.getLong("date"), o.getString("time"),
                o.getString("method"), o.optString("note"), o.optBoolean("share"),
                o.getString("pay"), o.getInt("total"), o.getLong("created"),
            )
        })
    }

    fun finishOnboarding() {
        onboarded = true
        prefs.edit().putBoolean("onboarded", true).apply()
    }

    fun login(name: String, email: String) {
        user = User(name, email)
        prefs.edit().putString("user", JSONObject().put("name", name).put("email", email).toString()).apply()
    }

    fun logout() {
        user = null
        prefs.edit().remove("user").apply()
    }

    fun updateName(name: String) {
        val u = user ?: return
        login(name, u.email)
    }

    fun addCheckIn(c: CheckIn) {
        checkIns.add(0, c); saveCheckIns()
    }

    fun addJournal(j: JournalEntry) {
        journals.add(0, j); saveJournals()
    }

    fun deleteJournal(id: String) {
        journals.removeAll { it.id == id }; saveJournals()
    }

    fun addScreening(s: ScreeningResult) {
        screenings.add(0, s)
        writeArray("screenings", screenings) {
            JSONObject().put("id", it.id).put("ts", it.timestamp).put("srq", it.srq)
                .put("gad", it.gad).put("phq", it.phq).put("phq9", it.phqItem9)
        }
    }

    fun addSession(s: Session) {
        sessions.add(0, s)
        writeArray("sessions", sessions) {
            JSONObject().put("id", it.id).put("counselor", it.counselorId).put("date", it.dateMillis)
                .put("time", it.time).put("method", it.method).put("note", it.note)
                .put("share", it.shareCheckIn).put("pay", it.paymentMethod).put("total", it.total)
                .put("created", it.createdAt)
        }
    }

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

    private fun saveCheckIns() = writeArray("checkins", checkIns) {
        JSONObject().put("id", it.id).put("ts", it.timestamp).put("mood", it.mood)
            .put("factors", JSONArray(it.factors)).put("feelings", JSONArray(it.feelings)).put("note", it.note)
    }

    private fun saveJournals() = writeArray("journals", journals) {
        JSONObject().put("id", it.id).put("ts", it.timestamp).put("prompt", it.prompt).put("text", it.text)
    }

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

    private fun JSONArray.toStrings() = (0 until length()).map { getString(it) }
}

fun dayKey(ts: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

fun isSameDay(a: Long, b: Long) = dayKey(a) == dayKey(b)
