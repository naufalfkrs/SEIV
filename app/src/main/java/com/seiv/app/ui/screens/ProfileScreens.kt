package com.seiv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.*
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.Routes
import com.seiv.app.ui.components.*
import com.seiv.app.ui.goTab
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.launch
import java.util.Calendar

// ---------------------------------------------------------------------------
// Data diri
// ---------------------------------------------------------------------------

private val PHONE = Regex("^\\+?[0-9]{8,15}$")

/** Hapus spasi/strip agar "0812-3456 789" tetap valid. */
private fun cleanPhone(s: String) = s.filter { it.isDigit() || it == '+' }

@Composable
fun ProfileEditScreen(nav: NavHostController, firstTime: Boolean) {
    val start = SeivStore.user?.profile ?: Profile()
    var fullName by remember { mutableStateOf(start.fullName.ifBlank { SeivStore.user?.name.orEmpty() }) }
    var address by remember { mutableStateOf(start.address) }
    var occupation by remember { mutableStateOf(start.occupation) }
    var institution by remember { mutableStateOf(start.institution) }
    var faculty by remember { mutableStateOf(start.faculty) }
    var phone by remember { mutableStateOf(start.phone) }
    var guardianPhone by remember { mutableStateOf(start.guardianPhone) }
    var submitted by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val isStudent = occupation == "mahasiswa"
    fun err(cond: Boolean, msg: String) = if (submitted && cond) msg else null
    val errors = mapOf(
        "name" to err(fullName.isBlank(), "Nama lengkap wajib diisi"),
        "address" to err(address.isBlank(), "Alamat domisili wajib diisi"),
        "occupation" to err(occupation.isBlank(), "Pilih status"),
        "institution" to err(isStudent && institution.isBlank(), "Asal instansi wajib diisi"),
        "faculty" to err(isStudent && faculty.isBlank(), "Fakultas wajib diisi"),
        "phone" to err(!PHONE.matches(cleanPhone(phone)), "Nomor HP tidak valid (8–15 angka)"),
        "guardian" to when {
            submitted && !PHONE.matches(cleanPhone(guardianPhone)) -> "Nomor HP tidak valid (8–15 angka)"
            submitted && cleanPhone(guardianPhone) == cleanPhone(phone) -> "Gunakan nomor orang lain, bukan nomormu sendiri"
            else -> null
        },
    )

    fun leave() = if (firstTime) nav.goTab(Routes.HOME) else nav.popBackStack()

    fun save() {
        submitted = true
        error = null
        val p = Profile(
            fullName.trim(), address.trim(), occupation, institution.trim(), faculty.trim(),
            cleanPhone(phone), cleanPhone(guardianPhone),
        )
        if (!p.isComplete || errors.values.any { it != null } ||
            !PHONE.matches(p.phone) || !PHONE.matches(p.guardianPhone) || p.phone == p.guardianPhone
        ) {
            error = "Periksa kembali isian yang ditandai merah."
            return
        }
        saving = true
        scope.launch {
            runCatching { SeivSync.saveProfile(p) }
                .onSuccess { leave() }
                .onFailure { error = it.message ?: "Gagal menyimpan. Coba lagi." }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeader(
            if (firstTime) "Lengkapi Data Diri" else "Data Diri", onBack = { leave() },
            subtitle = "Membantu konselor mengenal dan menghubungimu",
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Seiv.MintSoft).padding(12.dp)) {
                Text("🔒", fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Data diri bersifat rahasia — hanya kamu, admin SEIV, dan konselor yang menangani sesimu yang dapat melihatnya. Nomor wali hanya dihubungi dalam keadaan darurat.",
                    fontSize = 11.sp, color = Seiv.Text.copy(alpha = 0.8f), lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            FormField("Nama lengkap", fullName, { fullName = it }, "Sesuai KTP/KTM", errors["name"], caps = KeyboardCapitalization.Words)
            FormField("Alamat domisili", address, { address = it }, "Alamat tempat tinggalmu saat ini", errors["address"], singleLine = false)

            FieldLabel("Status")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("mahasiswa" to "Mahasiswa", "bekerja" to "Bekerja", "lainnya" to "Lainnya").forEach { (v, label) ->
                    Chip(label, occupation == v, { occupation = v }, Modifier.weight(1f))
                }
            }
            errors["occupation"]?.let { ErrorText(it) }
            Spacer(Modifier.height(14.dp))

            if (isStudent) {
                FormField("Asal instansi", institution, { institution = it }, "Contoh: Universitas Brawijaya", errors["institution"], caps = KeyboardCapitalization.Words)
                FormField("Fakultas", faculty, { faculty = it }, "Contoh: Fakultas Ilmu Kesehatan", errors["faculty"], caps = KeyboardCapitalization.Words)
            }
            FormField("Nomor HP", phone, { phone = it }, "08xxxxxxxxxx", errors["phone"], keyboard = KeyboardType.Phone)
            FormField(
                "Nomor HP wali / kontak darurat", guardianPhone, { guardianPhone = it }, "Orang tua, saudara, atau orang terdekat",
                errors["guardian"], keyboard = KeyboardType.Phone,
            )
            error?.let { ErrorText(it) }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton(if (saving) "Menyimpan…" else "Simpan Data Diri", onClick = { save() }, enabled = !saving)
            if (firstTime) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nanti saja", fontSize = 13.sp, color = Seiv.TextSecondary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp)).clickable { leave() }.padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = Seiv.TextSecondary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ErrorText(text: String) {
    Text(text, fontSize = 11.sp, color = Seiv.Red, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    keyboard: KeyboardType = KeyboardType.Text,
    caps: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    singleLine: Boolean = true,
) {
    FieldLabel(label)
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(if (singleLine) 150 else 300)) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 13.sp, color = Seiv.TextHint) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = caps),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
            unfocusedBorderColor = Seiv.Border, focusedBorderColor = Seiv.Primary,
        ),
    )
    error?.let { ErrorText(it) }
    Spacer(Modifier.height(14.dp))
}

// ---------------------------------------------------------------------------
// Notifikasi
// ---------------------------------------------------------------------------

data class Notif(
    val id: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color,
    val title: String,
    val body: String,
    val time: Long,
    val route: String?,
)

private fun sessionStart(s: Session): Long {
    val c = Calendar.getInstance().apply { timeInMillis = s.dateMillis }
    val (h, m) = s.time.split(":").map { it.toInt() }
    c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, m)
    return c.timeInMillis
}

/** Notifikasi dibangun dari data pengguna (sesi, check-in, skrining, data diri). */
fun buildNotifications(): List<Notif> {
    val now = System.currentTimeMillis()
    val list = mutableListOf<Notif>()
    val profile = SeivStore.user?.profile
    if (profile != null && !profile.isComplete) {
        list += Notif(
            "profile-incomplete", Icons.Outlined.Badge, Seiv.Primary, Seiv.MintLight,
            "Lengkapi data dirimu", "Nama lengkap, domisili, status, dan kontak darurat membantu konselor mendampingimu.",
            now, "${Routes.PROFILE_EDIT}?first=false",
        )
    }
    SeivStore.sessions.forEach { s ->
        val c = SeivStore.counselorById(s.counselorId)
        val start = sessionStart(s)
        val whenText = "${formatDate(s.dateMillis, "EEE, d MMM")} · ${s.time} WIB"
        val route = "${Routes.SESSION}/${s.id}"
        when (s.status) {
            SessionStatus.WAITING -> list += Notif(
                "s-${s.id}-waiting", Icons.Outlined.HourglassTop, Seiv.Orange, Seiv.OrangeSoft,
                "Pembayaran sedang diverifikasi", "Sesi dengan ${c.name} ($whenText) menunggu konfirmasi admin.", s.createdAt, route,
            )
            SessionStatus.CONFIRMED -> {
                list += Notif(
                    "s-${s.id}-confirmed", Icons.Outlined.CheckCircle, Seiv.Primary, Seiv.MintLight,
                    "Sesi terkonfirmasi 🎉", "Pembayaranmu sudah diverifikasi. Sesi dengan ${c.name} pada $whenText.", s.updatedAt, route,
                )
                if (start in now..(now + 24 * 3600_000L)) list += Notif(
                    "s-${s.id}-soon", Icons.Outlined.CalendarMonth, Seiv.Purple, Seiv.PurpleSoft,
                    "Sesimu segera dimulai", "Jangan lupa, sesi dengan ${c.name} dimulai $whenText.", start - 24 * 3600_000L, route,
                )
            }
            SessionStatus.COMPLETED -> {
                list += Notif(
                    "s-${s.id}-completed", Icons.Outlined.TaskAlt, Seiv.Purple, Seiv.PurpleSoft,
                    "Sesi selesai", "Terima kasih sudah hadir di sesi dengan ${c.name}. Jaga dirimu, ya!", s.updatedAt, route,
                )
                if (SeivStore.reviewFor(s.id) == null) list += Notif(
                    "s-${s.id}-review", Icons.Outlined.StarOutline, Seiv.Orange, Seiv.OrangeSoft,
                    "Bagaimana sesimu?", "Beri ulasan untuk ${c.name} agar mahasiswa lain terbantu memilih konselor.", s.updatedAt + 1, route,
                )
            }
            SessionStatus.CANCELLED -> list += Notif(
                "s-${s.id}-cancelled", Icons.Outlined.Cancel, Seiv.Red, Seiv.RedSoft,
                "Sesi dibatalkan", "Sesi dengan ${c.name} ($whenText) dibatalkan. Hubungi admin SEIV bila ada pertanyaan.", s.updatedAt, route,
            )
        }
        if (s.counselorNote.isNotBlank()) list += Notif(
            "s-${s.id}-note-${s.counselorNote.hashCode()}", Icons.AutoMirrored.Outlined.Chat, Seiv.Primary, Seiv.MintLight,
            "Pesan dari ${c.name}", s.counselorNote, s.updatedAt + 2, route,
        )
    }
    SeivStore.screenings.firstOrNull()?.let { r ->
        if (r.careLevel() >= 2) list += Notif(
            "scr-${r.id}", Icons.Outlined.Psychology, Seiv.Orange, Seiv.OrangeSoft,
            "Hasil skrining menyarankan konseling", "Bicara dengan konselor bisa membantumu melewati masa ini.", r.timestamp,
            "${Routes.SCREENING_RESULT}/${r.id}",
        )
    }
    val last = SeivStore.screenings.firstOrNull()?.timestamp
    if (last == null || now - last > 30L * 24 * 3600_000) {
        val month = Calendar.getInstance().let { it.get(Calendar.YEAR) * 100 + it.get(Calendar.MONTH) }
        list += Notif(
            "scr-reminder-$month", Icons.Outlined.Psychology, Seiv.Purple, Seiv.PurpleSoft,
            if (last == null) "Coba skrining pertamamu" else "Saatnya skrining ulang",
            "Kenali kondisi mentalmu lewat skrining kesehatan jiwa (±20 menit).", now - 1, Routes.SCREENING,
        )
    }
    if (SeivStore.todayCheckIn() == null) {
        list += Notif(
            "checkin-${dayKey(now)}", Icons.Outlined.SentimentSatisfied, Seiv.Primary, Seiv.MintLight,
            "Bagaimana perasaanmu hari ini?", "Luangkan 1 menit untuk check-in harian.", now - 2, Routes.CHECKIN,
        )
    }
    // Waktu stabil per notifikasi, sembunyikan yang sudah dihapus, terbaru di atas
    return list.map { it.copy(time = SeivStore.notificationTime(it.id, it.time)) }
        .filterNot { it.id in SeivStore.dismissedNotifications }
        .sortedByDescending { it.time }
}

@Composable
fun unreadNotificationCount(): Int {
    val seen = SeivStore.seenNotifications
    return buildNotifications().count { it.id !in seen }
}

/** "Baru saja", "5 mnt lalu", "2 jam lalu", "Kemarin", atau tanggal. */
private fun relativeTime(t: Long): String {
    val diff = (System.currentTimeMillis() - t).coerceAtLeast(0)
    val min = diff / 60_000
    return when {
        min < 1 -> "Baru saja"
        min < 60 -> "$min mnt lalu"
        min < 24 * 60 -> "${min / 60} jam lalu"
        min < 48 * 60 -> "Kemarin"
        else -> formatDate(t, "d MMM")
    }
}

private const val VISIBLE_NOTIFICATIONS = 5

@Composable
fun NotificationsScreen(nav: NavHostController) {
    // Dibaca langsung dari state (sesi, skrining, ulasan, notifikasi yang dihapus) agar selalu terbaru
    val items = buildNotifications()
    // Status "belum dibaca" dihitung saat layar dibuka, lalu semua ditandai dibaca
    val unreadIds = remember { items.filter { it.id !in SeivStore.seenNotifications }.map { it.id }.toSet() }
    var showAll by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { SeivSync.refreshInBackground() }
    LaunchedEffect(items.map { it.id }) { SeivStore.markNotificationsSeen(items.map { it.id }) }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Notifikasi", onBack = { nav.popBackStack() }, subtitle = "Kabar terbaru untukmu") {
            if (items.isNotEmpty()) {
                Text(
                    "Hapus semua", fontSize = 12.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { confirmClear = true }.padding(8.dp),
                )
            }
        }
        RefreshBox(Modifier.weight(1f)) {
            if (items.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.NotificationsNone, null, tint = Seiv.TextHint, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Belum ada notifikasi", color = Seiv.TextSecondary, fontSize = 13.sp)
                }
                return@RefreshBox
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                val shown = if (showAll) items else items.take(VISIBLE_NOTIFICATIONS)
                shown.forEach { n -> NotificationCard(n, n.id in unreadIds, nav) }
                val hidden = items.size - VISIBLE_NOTIFICATIONS
                if (hidden > 0) {
                    Text(
                        if (showAll) "Sembunyikan notifikasi lama ▲" else "Lihat $hidden notifikasi lebih lama ▼",
                        fontSize = 13.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp))
                            .clickable { showAll = !showAll }.padding(12.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Hapus semua notifikasi?") },
            text = { Text("Notifikasi yang dihapus tidak ditampilkan lagi. Kabar baru tetap akan muncul.", fontSize = 13.sp) },
            confirmButton = {
                androidx.compose.material3.TextButton({ SeivStore.dismissNotifications(items.map { it.id }); confirmClear = false }) {
                    Text("Hapus", color = Seiv.Red)
                }
            },
            dismissButton = { androidx.compose.material3.TextButton({ confirmClear = false }) { Text("Batal", color = Seiv.Primary) } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun NotificationCard(n: Notif, unread: Boolean, nav: NavHostController) {
    SeivCard(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        color = if (unread) Color.White else Color.White.copy(alpha = 0.6f),
        padding = PaddingValues(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
        onClick = n.route?.let { r ->
            {
                when (r) {
                    Routes.SCREENING -> nav.goTab(Routes.SCREENING)
                    Routes.CHECKIN -> nav.navigate(Routes.CHECKIN)
                    else -> nav.navigate(r)
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(n.icon, n.tint, n.bg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (unread) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(Seiv.Red))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(n.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(2.dp))
                Text(n.body, fontSize = 12.sp, color = Seiv.TextSecondary, lineHeight = 17.sp)
                Spacer(Modifier.height(4.dp))
                Text(relativeTime(n.time), fontSize = 10.sp, color = Seiv.TextHint)
            }
            // Hapus satu notifikasi
            Icon(
                Icons.Outlined.Close, "Hapus notifikasi", tint = Seiv.TextHint,
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable { SeivStore.dismissNotifications(listOf(n.id)) }.padding(7.dp),
            )
        }
    }
}
