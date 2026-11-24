package com.seiv.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.COUNSELORS
import com.seiv.app.data.Counselor
import com.seiv.app.data.PLATFORM_FEE
import com.seiv.app.data.SeivStore
import com.seiv.app.data.Review
import com.seiv.app.data.Session
import com.seiv.app.data.SessionStatus
import com.seiv.app.data.counselorById
import com.seiv.app.data.isSameDay
import com.seiv.app.data.rupiah
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.Routes
import com.seiv.app.ui.components.*
import com.seiv.app.ui.goTab
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

/** Draft pemesanan yang dibawa lintas layar pada alur konseling. */
object BookingDraft {
    var counselorId by mutableStateOf(COUNSELORS.first().id)
    var dateMillis by mutableLongStateOf(System.currentTimeMillis())
    var time by mutableStateOf("15:00")
    var method by mutableStateOf("Voice Call")
    var note by mutableStateOf("")
    var shareCheckIn by mutableStateOf(false)
    var paymentMethod by mutableStateOf("QRIS")
    val counselor get() = counselorById(counselorId)
    val total get() = counselor.price + PLATFORM_FEE
}

const val SESSION_MINUTES = 45

/** Kelonggaran setelah durasi sesi habis sebelum ruang sesi ditutup (sama dengan server). */
const val ROOM_GRACE_MINUTES = 15

fun sessionStartMillis(dateMillis: Long, time: String): Long {
    val (h, m) = time.split(":").map { it.toInt() }
    return Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun endTime(time: String): String {
    val (h, m) = time.split(":").map { it.toInt() }
    val total = h * 60 + m + SESSION_MINUTES
    return "%02d:%02d".format(total / 60 % 24, total % 60)
}

fun methodIcon(method: String): ImageVector = when (method) {
    "Chat" -> Icons.AutoMirrored.Outlined.Chat
    "Video Call" -> Icons.Outlined.Videocam
    else -> Icons.Outlined.Call
}

// ---------------------------------------------------------------------------
// Beranda konseling
// ---------------------------------------------------------------------------

@Composable
fun CounselingScreen(nav: NavHostController) {
    LaunchedEffect(Unit) { SeivSync.refreshInBackground() } // ambil status sesi terbaru dari server
    val now = System.currentTimeMillis()
    val active = SeivStore.sessions.filter { it.status != SessionStatus.CANCELLED }
    val upcoming = active.filter { it.status != SessionStatus.COMPLETED && sessionStartMillis(it.dateMillis, it.time) + SESSION_MINUTES * 60_000 > now }
        .sortedBy { sessionStartMillis(it.dateMillis, it.time) }
    val done = active.size - upcoming.size

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        GreetingHeader(nav, showCompanion = false)

        Box(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Seiv.PrimaryDeep, Seiv.PrimaryDark, Seiv.Primary))),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.06f), radius = size.width * 0.35f, center = Offset(size.width * 0.95f, size.height * 0.1f))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.2f, center = Offset(size.width * 0.15f, size.height * 1.05f))
            }
            Text("🧑‍⚕️", fontSize = 64.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 50.dp))
            Column(Modifier.padding(18.dp)) {
                Text(
                    "LAYANAN KONSELING", color = Seiv.MintLight, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("Bantuan\nProfesional", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Kamu tidak harus menghadapinya sendiri. Konselor kami siap membantu kamu merasa lebih baik.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth(0.68f),
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).clickable { nav.navigate(Routes.COUNSELORS) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Jadwalkan sekarang", color = Seiv.PrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Seiv.PrimaryDark, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ServiceTile(Icons.Outlined.HealthAndSafety, Seiv.Primary, Seiv.MintLight, "Nurse", "Konselor\nkesehatan", Modifier.weight(1f)) { nav.navigate("${Routes.COUNSELORS}?role=Nurse") }
            ServiceTile(Icons.Outlined.Psychology, Seiv.Orange, Seiv.OrangeSoft, "Psikolog", "Konselor\nprofesional", Modifier.weight(1f)) { nav.navigate("${Routes.COUNSELORS}?role=Psikolog") }
            ServiceTile(Icons.Outlined.History, Seiv.Purple, Seiv.PurpleSoft, "Riwayat", "Sesi\nkonseling", Modifier.weight(1f)) { nav.navigate(Routes.SESSIONS) }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("$done", "Sesi Selesai", Seiv.Primary, Modifier.weight(1f))
            StatTile("${upcoming.size}", "Jadwal Aktif", Seiv.Orange, Modifier.weight(1f))
            // Rata-rata rating konselor SEIV (data konselor dari server)
            // Rata-rata tertimbang jumlah ulasan; "-" bila belum ada ulasan sama sekali
            val totalReviews = SeivStore.counselors.sumOf { it.reviews }
            val avgRating = if (totalReviews == 0) null
            else SeivStore.counselors.sumOf { it.rating * it.reviews } / totalReviews
            StatTile(avgRating?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "-", "Rating Konselor", Seiv.Purple, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Sesi akan datang", Modifier.padding(horizontal = 20.dp), action = "Lihat semua") { nav.navigate(Routes.SESSIONS) }
        Spacer(Modifier.height(10.dp))
        if (upcoming.isEmpty()) {
            SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text("Belum ada sesi terjadwal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Seiv.Text)
                Text("Jadwalkan sesi pertamamu dengan konselor pilihanmu.", fontSize = 11.sp, color = Seiv.TextSecondary)
            }
        } else {
            upcoming.take(3).forEach { s -> SessionRow(s, Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) { nav.navigate("${Routes.SESSION}/${s.id}") } }
        }

        Spacer(Modifier.height(16.dp))
        SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), color = Seiv.RedSoft, onClick = { nav.navigate(Routes.REFERRAL) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Emergency, Seiv.Red, Color.White)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Butuh bantuan segera?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                    Text("Lihat layanan darurat & hotline kesehatan jiwa 24 jam.", fontSize = 11.sp, color = Seiv.TextSecondary)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Seiv.Red, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ServiceTile(icon: ImageVector, tint: Color, bg: Color, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    SeivCard(modifier, padding = PaddingValues(vertical = 14.dp, horizontal = 8.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, tint, bg, size = 38.dp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Seiv.Text)
            Text(sub, fontSize = 9.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier) {
    SeivCard(modifier, padding = PaddingValues(vertical = 12.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 9.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun SessionRow(s: Session, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = counselorById(s.counselorId)
    SeivCard(modifier.fillMaxWidth(), padding = PaddingValues(12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.initials, c.colorIndex, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                Text("${formatDate(s.dateMillis, "EEE, d MMM")} · ${s.time} WIB", fontSize = 11.sp, color = Seiv.TextSecondary)
                Spacer(Modifier.height(4.dp))
                StatusTag(s.status)
            }
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Seiv.MintLight).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(methodIcon(s.method), null, tint = Seiv.PrimaryDark, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(s.method, fontSize = 10.sp, color = Seiv.PrimaryDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun StatusTag(status: String) {
    val (fg, bg) = when (status) {
        SessionStatus.WAITING -> Seiv.Orange to Seiv.OrangeSoft
        SessionStatus.CANCELLED -> Seiv.Red to Seiv.RedSoft
        SessionStatus.COMPLETED -> Seiv.Purple to Seiv.PurpleSoft
        else -> Seiv.PrimaryDark to Seiv.MintLight
    }
    Tag(SessionStatus.label(status), fg, bg)
}

@Composable
fun SessionListScreen(nav: NavHostController) {
    LaunchedEffect(Unit) { SeivSync.refreshInBackground() }
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Riwayat Sesi", onBack = { nav.popBackStack() }, subtitle = "Semua sesi konselingmu")
        RefreshBox(Modifier.weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                if (SeivStore.sessions.isEmpty()) {
                    EmptyState("🗓️", "Belum ada sesi", "Sesi konseling yang kamu jadwalkan akan muncul di sini.")
                }
                SeivStore.sessions.forEach { s -> SessionRow(s, Modifier.padding(vertical = 5.dp)) { nav.navigate("${Routes.SESSION}/${s.id}") } }
            }
        }
        PrimaryButton("Jadwalkan Sesi Baru", onClick = { nav.navigate(Routes.COUNSELORS) }, modifier = Modifier.padding(20.dp))
    }
}

@Composable
fun EmptyState(emoji: String, title: String, desc: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Seiv.Text)
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 12.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------
// Pilih konselor
// ---------------------------------------------------------------------------

@Composable
fun CounselorListScreen(nav: NavHostController, role: String?) {
    LaunchedEffect(Unit) { SeivSync.refreshInBackground() }
    val list = SeivStore.counselors.filter { role == null || it.role.contains(role, ignoreCase = true) }
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Pilih Konselor", onBack = { nav.popBackStack() }, subtitle = "Pilih yang paling sesuai untukmu") {
            Text("🌿", fontSize = 22.sp)
        }
        RefreshBox(Modifier.weight(1f)) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            list.forEach { c ->
                CounselorCard(c) {
                    BookingDraft.counselorId = c.id
                    nav.navigate("${Routes.SCHEDULE}/${c.id}")
                }
                Spacer(Modifier.height(12.dp))
            }
            SeivCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.Shield, Seiv.Primary, Seiv.MintLight, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Privasimu aman", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Seiv.Text)
                        Text("Semua sesi bersifat rahasia dan hanya digunakan untuk membantumu.", fontSize = 10.sp, color = Seiv.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        } }
    }
}

private val tagPalette = listOf(Seiv.MintLight to Seiv.PrimaryDark, Seiv.OrangeSoft to Seiv.Orange, Seiv.PurpleSoft to Seiv.Purple)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CounselorCard(c: Counselor, onClick: () -> Unit) {
    SeivCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row {
            Avatar(c.initials, c.colorIndex, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Seiv.Text)
                Text(c.role, fontSize = 11.sp, color = Seiv.TextSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (c.reviews > 0) {
                        Icon(Icons.Filled.Star, null, tint = Seiv.Yellow, modifier = Modifier.size(13.dp))
                        Text(" ${c.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Seiv.Text)
                        Text(" (${c.reviews} ulasan)", fontSize = 11.sp, color = Seiv.TextHint)
                    } else {
                        Text("Belum ada ulasan", fontSize = 11.sp, color = Seiv.TextHint)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(rupiah(c.price), fontSize = 11.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold)
                }
            }
            val (bg, fg) = tagPalette[c.colorIndex % 3]
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(bg), contentAlignment = Alignment.Center) {
                Text("›", color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val (bg, fg) = tagPalette[c.colorIndex % 3]
            c.tags.forEach { Tag(it, fg, bg) }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Seiv.Border))
        Spacer(Modifier.height(10.dp))
        Text(c.bio, fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 16.sp)
    }
}

// ---------------------------------------------------------------------------
// Jadwalkan sesi
// ---------------------------------------------------------------------------

private val TIME_SLOTS = listOf("09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "19:00")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleScreen(nav: NavHostController, counselorId: String) {
    val c = counselorById(counselorId)
    val days = remember {
        (0 until 10).map { offset ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }.timeInMillis
        }
    }
    var date by remember { mutableLongStateOf(days[1]) }
    var time by remember { mutableStateOf("15:00") }
    var method by remember { mutableStateOf("Voice Call") }
    var note by remember { mutableStateOf("") }
    var share by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    // Online: slot terisi diambil dari server. Offline (demo): beberapa slot dibuat "terisi" agar terasa realistis.
    var booked by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(date, counselorId) {
        booked = if (SeivSync.online) SeivSync.bookedSlots(counselorId, date)
        else TIME_SLOTS.filterIndexed { i, _ -> (i + date / 86_400_000L + counselorId.length) % 4 == 0L }.toSet()
    }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeader("Jadwalkan Sesi", onBack = { nav.popBackStack() }, subtitle = "bersama ${c.name}")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            LabelWithIcon(Icons.Outlined.CalendarMonth, "Pilih tanggal", Modifier.padding(horizontal = 20.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEach { d ->
                    val sel = isSameDay(d, date)
                    Column(
                        Modifier.width(52.dp).shadow(if (sel) 4.dp else 1.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                            .background(if (sel) Seiv.PrimaryDark else Color.White).clickable { date = d }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(formatDate(d, "EEE"), fontSize = 10.sp, color = if (sel) Color.White.copy(alpha = 0.8f) else Seiv.TextSecondary)
                        Text(formatDate(d, "d"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Seiv.Text)
                        Text(formatDate(d, "MMM"), fontSize = 10.sp, color = if (sel) Color.White.copy(alpha = 0.8f) else Seiv.TextSecondary)
                    }
                }
            }

            LabelWithIcon(Icons.Outlined.Schedule, "Pilih waktu", Modifier.padding(horizontal = 20.dp))
            FlowRow(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 4,
            ) {
                TIME_SLOTS.forEach { t ->
                    val past = sessionStartMillis(date, t) < now + 30 * 60_000
                    val disabled = past || t in booked
                    val sel = t == time && !disabled
                    Text(
                        t, textAlign = TextAlign.Center, fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = when { sel -> Color.White; disabled -> Seiv.TextHint; else -> Seiv.Text },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) Seiv.Primary else if (disabled) Color(0xFFEDEFEC) else Color.White)
                            .border(1.dp, if (sel) Seiv.Primary else Seiv.Border, RoundedCornerShape(12.dp))
                            .clickable(enabled = !disabled) { time = t }.padding(vertical = 10.dp),
                    )
                }
            }

            LabelWithIcon(Icons.AutoMirrored.Outlined.Chat, "Metode sesi", Modifier.padding(horizontal = 20.dp))
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Chat", "Voice Call", "Video Call").forEach { m ->
                    val sel = m == method
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (sel) Seiv.PrimaryDark else Color.White)
                            .border(1.dp, if (sel) Seiv.PrimaryDark else Seiv.Border, RoundedCornerShape(12.dp))
                            .clickable { method = m }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(methodIcon(m), null, tint = if (sel) Color.White else Seiv.TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(m, fontSize = 11.sp, color = if (sel) Color.White else Seiv.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }

            Row(Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)) {
                Text("Apa yang ingin kamu ceritakan?", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Seiv.Text)
                Text(" (Opsional)", fontSize = 10.sp, color = Seiv.TextHint)
            }
            OutlinedTextField(
                value = note, onValueChange = { if (it.length <= 300) note = it },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 90.dp),
                placeholder = { Text("Ceritakan secara singkat agar konselor dapat lebih siap...", fontSize = 12.sp, color = Seiv.TextHint) },
                shape = RoundedCornerShape(14.dp),
                supportingText = { Text("${note.length}/300", fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                    unfocusedBorderColor = Seiv.Border, focusedBorderColor = Seiv.Primary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
            )
            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
                    .clickable { share = !share }.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(share, { share = it }, colors = CheckboxDefaults.colors(checkedColor = Seiv.Primary))
                Text("Bagikan ringkasan check-in & skrining", fontSize = 12.sp, color = Seiv.Text)
            }
            Spacer(Modifier.height(16.dp))
        }
        val valid = sessionStartMillis(date, time) >= now + 30 * 60_000 && time !in booked
        PrimaryButton(
            "Konfirmasi jadwal",
            enabled = valid,
            onClick = {
                BookingDraft.counselorId = c.id
                BookingDraft.dateMillis = date
                BookingDraft.time = time
                BookingDraft.method = method
                BookingDraft.note = note.trim()
                BookingDraft.shareCheckIn = share
                nav.navigate(Routes.PAYMENT)
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun LabelWithIcon(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(top = 18.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Seiv.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Seiv.Text)
    }
}

// ---------------------------------------------------------------------------
// Pembayaran
// ---------------------------------------------------------------------------

private data class PayOption(val id: String, val label: String, val group: String, val emoji: String)

private val PAY_OPTIONS = listOf(
    PayOption("QRIS", "QRIS — Scan QR Code", "QRIS & E-WALLET", "▦"),
    PayOption("BCA", "BCA Virtual Account", "TRANSFER BANK", "🏦"),
    PayOption("Mandiri", "Mandiri Virtual Account", "TRANSFER BANK", "🟡"),
)

@Composable
private fun StepHeader(step: Int, title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BackCircle(onBack)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Langkah $step dari 2", fontSize = 10.sp, color = Seiv.TextSecondary)
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        StepProgress(step - 1, 2, Seiv.Primary)
    }
}

@Composable
fun PaymentScreen(nav: NavHostController) {
    val c = BookingDraft.counselor
    var selected by remember { mutableStateOf(BookingDraft.paymentMethod) }
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        StepHeader(1, "Pembayaran") { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SeivCard(Modifier.fillMaxWidth()) {
                Text("RINGKASAN SESI", fontSize = 10.sp, letterSpacing = 1.sp, color = Seiv.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.initials, c.colorIndex, 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                        Text(c.role, fontSize = 11.sp, color = Seiv.TextSecondary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Seiv.Border))
                Spacer(Modifier.height(6.dp))
                KeyValueRow("Tanggal", formatDate(BookingDraft.dateMillis, "EEE, d MMM yyyy"))
                KeyValueRow("Waktu", "${BookingDraft.time} WIB")
                KeyValueRow("Metode", BookingDraft.method)
                KeyValueRow("Durasi", "$SESSION_MINUTES menit")
            }
            Spacer(Modifier.height(12.dp))
            SeivCard(Modifier.fillMaxWidth()) {
                Text("RINCIAN BIAYA", fontSize = 10.sp, letterSpacing = 1.sp, color = Seiv.TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                KeyValueRow("Sesi konseling ($SESSION_MINUTES mnt)", rupiah(c.price))
                KeyValueRow("Biaya platform", rupiah(PLATFORM_FEE))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Seiv.Border))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                    Spacer(Modifier.weight(1f))
                    Text(rupiah(BookingDraft.total), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Seiv.PrimaryDark)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Pilih Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
            PAY_OPTIONS.groupBy { it.group }.forEach { (group, opts) ->
                Row(Modifier.padding(top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (group.startsWith("QRIS")) Icons.Outlined.QrCode2 else Icons.Outlined.AccountBalance, null, tint = Seiv.TextHint, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(group, fontSize = 10.sp, letterSpacing = 1.sp, color = Seiv.TextHint, fontWeight = FontWeight.SemiBold)
                }
                opts.forEach { o ->
                    val sel = o.id == selected
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).shadow(1.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                            .background(Color.White).border(1.5.dp, if (sel) Seiv.Primary else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { selected = o.id }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Seiv.Background), contentAlignment = Alignment.Center) {
                            Text(o.emoji, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(o.label, fontSize = 13.sp, color = Seiv.Text, modifier = Modifier.weight(1f))
                        RadioDot(sel)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(
            "Lanjutkan Pembayaran", trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = { BookingDraft.paymentMethod = selected; nav.navigate(Routes.QRIS) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), container = Seiv.PrimaryDark,
        )
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier.size(20.dp).clip(CircleShape).border(1.5.dp, if (selected) Seiv.Primary else Seiv.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(Seiv.Primary)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentDetailScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val method = BookingDraft.paymentMethod
    val total = BookingDraft.total
    val orderSeed = remember { Random.nextLong() }
    var remaining by remember { mutableIntStateOf(15 * 60) }
    var checking by remember { mutableStateOf(false) }
    var payError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { while (remaining > 0) { delay(1000); remaining-- } }
    LaunchedEffect(checking) {
        if (checking) {
            delay(800)
            val draft = Session(
                counselorId = BookingDraft.counselorId, dateMillis = BookingDraft.dateMillis, time = BookingDraft.time,
                method = BookingDraft.method, note = BookingDraft.note, shareCheckIn = BookingDraft.shareCheckIn,
                paymentMethod = if (method == "QRIS") "QRIS" else "$method VA", total = total,
            )
            try {
                val s = SeivSync.bookSession(draft)
                nav.navigate("${Routes.PAY_SUCCESS}/${s.id}") { popUpTo(Routes.COUNSELING) }
            } catch (e: Exception) {
                payError = e.message ?: "Gagal menyimpan pesanan."
                checking = false
            }
        }
    }
    payError?.let { msg ->
        AlertDialog(
            onDismissRequest = { payError = null },
            confirmButton = { TextButton({ payError = null }) { Text("OK", color = Seiv.Primary) } },
            title = { Text("Pesanan belum tersimpan") },
            text = { Text(msg, fontSize = 13.sp) },
            containerColor = Color.White,
        )
    }
    val vaNumber = remember { (if (method == "BCA") "39012" else "88908") + (100000000L + (orderSeed and 0x7FFFFFF) % 900000000L) }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        StepHeader(2, "Detail Pembayaran") { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFE7EEEA)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Seiv.Text), contentAlignment = Alignment.Center) {
                    Icon(if (method == "QRIS") Icons.Outlined.QrCode2 else Icons.Outlined.AccountBalance, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(PAY_OPTIONS.first { it.id == method }.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                    Text("Metode pembayaran dipilih", fontSize = 10.sp, color = Seiv.TextSecondary)
                }
                Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, null, tint = Seiv.Primary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            SeivCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (method == "QRIS") {
                        Text("SCAN QR CODE DI BAWAH INI", fontSize = 10.sp, letterSpacing = 1.sp, color = Seiv.TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Seiv.Border, RoundedCornerShape(12.dp)).padding(10.dp)) {
                            FakeQr(orderSeed)
                        }
                    } else {
                        Text("NOMOR VIRTUAL ACCOUNT", fontSize = 10.sp, letterSpacing = 1.sp, color = Seiv.TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(vaNumber.chunked(4).joinToString(" "), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Seiv.Text, letterSpacing = 1.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Outlined.ContentCopy, "Salin", tint = Seiv.Primary,
                                modifier = Modifier.size(18.dp).clickable {
                                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("VA", vaNumber))
                                    Toast.makeText(ctx, "Nomor VA disalin", Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                        Text("a.n. SEIV Indonesia", fontSize = 11.sp, color = Seiv.TextSecondary)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(rupiah(total), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Seiv.Text)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Timer, null, tint = Seiv.Red, modifier = Modifier.size(12.dp))
                        Text(" Berlaku %02d:%02d".format(remaining / 60, remaining % 60), fontSize = 11.sp, color = Seiv.Red)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SeivCard(Modifier.fillMaxWidth()) {
                if (method == "QRIS") {
                    Text("Gunakan aplikasi:", fontSize = 11.sp, color = Seiv.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("GoPay", "OVO", "Dana", "ShopeePay", "LinkAja", "m-BCA").forEach {
                            Text(it, fontSize = 10.sp, color = Seiv.Text, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Seiv.Background).padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                } else {
                    Text("Cara pembayaran", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Seiv.Text)
                    Spacer(Modifier.height(6.dp))
                    listOf(
                        "Buka aplikasi mobile banking $method.",
                        "Pilih menu Transfer → Virtual Account.",
                        "Masukkan nomor VA di atas lalu periksa nominal.",
                        "Konfirmasi dan simpan bukti pembayaran.",
                    ).forEachIndexed { i, s -> Text("${i + 1}. $s", fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 17.sp) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton(
            if (checking) "Memverifikasi pembayaran..." else "Saya Sudah Bayar", icon = if (checking) null else Icons.Filled.Check,
            enabled = !checking && remaining > 0,
            onClick = { checking = true },
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp), container = Seiv.PrimaryDark,
        )
        Text(
            "Pembayaran diproses dengan aman 🔒", fontSize = 10.sp, color = Seiv.TextHint,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FakeQr(seed: Long) {
    val n = 25
    val rnd = remember(seed) { Random(seed) }
    val cells = remember(seed) { List(n * n) { rnd.nextInt(100) < 48 } }
    Canvas(Modifier.fillMaxSize()) {
        val cs = size.width / n
        fun finder(x: Int, y: Int) {
            drawRect(Color.Black, Offset(x * cs, y * cs), Size(cs * 7, cs * 7))
            drawRect(Color.White, Offset((x + 1) * cs, (y + 1) * cs), Size(cs * 5, cs * 5))
            drawRect(Color.Black, Offset((x + 2) * cs, (y + 2) * cs), Size(cs * 3, cs * 3))
        }
        for (y in 0 until n) for (x in 0 until n) {
            val inFinder = (x < 8 && y < 8) || (x >= n - 8 && y < 8) || (x < 8 && y >= n - 8)
            if (!inFinder && cells[y * n + x]) drawRect(Color.Black, Offset(x * cs, y * cs), Size(cs + 0.5f, cs + 0.5f))
        }
        finder(0, 0); finder(n - 7, 0); finder(0, n - 7)
    }
}

// ---------------------------------------------------------------------------
// Pembayaran berhasil & detail sesi
// ---------------------------------------------------------------------------

@Composable
fun PaymentSuccessScreen(nav: NavHostController, id: String) {
    val s = SeivStore.sessions.firstOrNull { it.id == id } ?: return
    val c = counselorById(s.counselorId)
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val dots = listOf(
                        Triple(0.2f, 0.3f, Seiv.Yellow), Triple(0.32f, 0.12f, Seiv.Mint), Triple(0.75f, 0.2f, Seiv.Purple),
                        Triple(0.85f, 0.5f, Seiv.Orange), Triple(0.15f, 0.65f, Seiv.Orange), Triple(0.68f, 0.08f, Seiv.Primary),
                        Triple(0.9f, 0.15f, Seiv.Mint), Triple(0.28f, 0.85f, Seiv.Purple),
                    )
                    dots.forEach { (x, y, col) -> drawCircle(col, radius = 5f, center = Offset(size.width * x, size.height * y)) }
                }
                Box(
                    Modifier.align(Alignment.Center).padding(top = 20.dp).size(72.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Seiv.PrimaryDark),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
            }
            val waiting = s.status == SessionStatus.WAITING
            Text(if (waiting) "Pembayaran Terkirim! 🎉" else "Pembayaran Berhasil! 🎉", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
            Spacer(Modifier.height(6.dp))
            Text(if (waiting) "Admin SEIV akan memverifikasi pembayaranmu. Status sesi bisa dipantau di menu Konseling." else "Sesi konseling kamu sudah terkonfirmasi. Sampai jumpa!", fontSize = 12.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            SeivCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ID TRANSAKSI", fontSize = 9.sp, letterSpacing = 1.sp, color = Seiv.TextHint)
                        Text("#${s.id}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Seiv.Text)
                    }
                    if (s.status == SessionStatus.WAITING) StatusTag(s.status) else Tag("✓ Lunas")
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Seiv.Border))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.initials, c.colorIndex, 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                        Text(c.role, fontSize = 11.sp, color = Seiv.TextSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Tanggal", formatDate(s.dateMillis, "EEE, d MMM"))
                KeyValueRow("Waktu", "${s.time} WIB ($SESSION_MINUTES mnt)")
                KeyValueRow("Metode sesi", s.method)
                KeyValueRow("Pembayaran via", s.paymentMethod)
                KeyValueRow("Total dibayar", rupiah(s.total), bold = true)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Seiv.Background).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, null, tint = Seiv.Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Dilindungi enkripsi SEIV · Bukti tersimpan otomatis", fontSize = 10.sp, color = Seiv.TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("Lihat Detail Sesi", trailingIcon = Icons.AutoMirrored.Filled.ArrowForward, container = Seiv.PrimaryDark, onClick = {
                nav.navigate("${Routes.SESSION}/${s.id}") { popUpTo(Routes.COUNSELING) }
            })
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Kembali ke Beranda", icon = Icons.Outlined.Home, onClick = { nav.goTab(Routes.HOME) })
        }
    }
}

/** Form ulasan (1–5 bintang + komentar) untuk sesi yang sudah selesai; bisa diubah setelah dikirim. */
@Composable
private fun ReviewSection(s: Session, c: Counselor) {
    val existing = SeivStore.reviewFor(s.id)
    var editing by remember(existing) { mutableStateOf(existing == null) }
    var rating by remember(existing) { mutableIntStateOf(existing?.rating ?: 0) }
    var comment by remember(existing) { mutableStateOf(existing?.comment.orEmpty()) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val words = listOf("", "Kurang membantu", "Cukup", "Baik", "Sangat baik", "Luar biasa")

    SeivCard(Modifier.fillMaxWidth(), color = if (editing) Color.White else Seiv.YellowSoft) {
        Text(
            if (existing == null) "Bagaimana sesimu dengan ${c.name}?" else "Ulasanmu",
            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text,
        )
        if (existing == null) {
            Text("Ulasanmu membantu mahasiswa lain memilih konselor. Identitasmu tidak ditampilkan.", fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            (1..5).forEach { i ->
                Icon(
                    if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline, "Bintang $i",
                    tint = if (i <= rating) Seiv.Yellow else Seiv.TextHint,
                    modifier = Modifier.size(34.dp).clip(CircleShape).clickable(enabled = editing) { rating = i }.padding(2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(words[rating], fontSize = 12.sp, color = Seiv.TextSecondary)
        }
        if (editing) {
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = comment, onValueChange = { if (it.length <= 500) comment = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ceritakan pengalamanmu (opsional)", fontSize = 12.sp) }, minLines = 2,
                shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
            )
            error?.let { Text(it, fontSize = 11.sp, color = Seiv.Red, modifier = Modifier.padding(top = 6.dp)) }
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                if (sending) "Mengirim…" else if (existing == null) "Kirim Ulasan" else "Simpan Perubahan",
                enabled = rating > 0 && !sending,
                onClick = {
                    sending = true; error = null
                    scope.launch {
                        runCatching { SeivSync.submitReview(Review(s.id, s.counselorId, rating, comment.trim())) }
                            .onSuccess { editing = false }
                            .onFailure { error = it.message ?: "Gagal mengirim ulasan. Coba lagi." }
                        sending = false
                    }
                },
            )
        } else {
            if (comment.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("“$comment”", fontSize = 12.sp, color = Seiv.Text, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Ubah ulasan", fontSize = 12.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { editing = true }.padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
fun SessionDetailScreen(nav: NavHostController, id: String) {
    val s = SeivStore.sessions.firstOrNull { it.id == id } ?: return
    val c = counselorById(s.counselorId)
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    // Selama sesi masih menunggu/berjalan, cek server berkala (cadangan bila push terlambat).
    // Layar membaca SeivStore, jadi status & pesan konselor langsung berubah saat data baru masuk.
    LaunchedEffect(s.id, s.status) {
        SeivSync.refreshInBackground()
        while (s.status == SessionStatus.WAITING || s.status == SessionStatus.CONFIRMED) {
            delay(15_000)
            SeivSync.refreshInBackground()
        }
    }
    val start = sessionStartMillis(s.dateMillis, s.time)
    val end = start + SESSION_MINUTES * 60_000
    var share by remember { mutableStateOf(s.shareCheckIn) }
    var note by remember { mutableStateOf(s.note) }
    var editing by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }

    val status = when {
        now < start -> {
            val diff = (start - now) / 1000
            val d = diff / 86400; val h = diff % 86400 / 3600; val m = diff % 3600 / 60; val sec = diff % 60
            if (d > 0) "$d hari $h jam $m menit" else "%02d:%02d:%02d".format(h, m, sec)
        }
        now < end -> "Sesi sedang berlangsung"
        else -> "Sesi telah selesai"
    }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Detail Sesi", onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            SeivCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (now < start) "SESI DIMULAI DALAM" else "STATUS SESI", fontSize = 9.sp, letterSpacing = 1.sp, color = Seiv.TextHint)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (now in start until end) Text("✦ ", color = Seiv.Primary, fontSize = 12.sp)
                        Text(status, fontWeight = FontWeight.Bold, fontSize = if (now < start) 20.sp else 14.sp, color = Seiv.PrimaryDark)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SeivCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.initials, c.colorIndex, 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Seiv.Text)
                        Text(c.role, fontSize = 11.sp, color = Seiv.TextSecondary)
                    }
                    Text("#${s.id}", fontSize = 10.sp, color = Seiv.TextHint)
                }
                Spacer(Modifier.height(8.dp))
                DetailRow(Icons.Outlined.CalendarMonth, "Tanggal", formatDate(s.dateMillis, "EEEE, d MMMM yyyy"))
                DetailRow(Icons.Outlined.Schedule, "Waktu & Durasi", "${s.time} – ${endTime(s.time)} WIB ($SESSION_MINUTES menit)")
                DetailRow(methodIcon(s.method), "Metode Sesi", s.method, s.method)
                DetailRow(Icons.Outlined.Payments, "Biaya Sesi", rupiah(s.total), when (s.status) {
                    SessionStatus.WAITING -> "Menunggu"
                    SessionStatus.CANCELLED -> "Dibatalkan"
                    else -> "Lunas"
                })
                DetailRow(Icons.Outlined.Info, "Status", SessionStatus.label(s.status))
                DetailRow(
                    Icons.Outlined.Language, "Ruang sesi",
                    when {
                        s.method == "Chat" -> "Chat di aplikasi SEIV"
                        s.meetingUrl.isNotBlank() -> "Tautan dari konselor sudah tersedia"
                        else -> "Tautan ${s.method.lowercase()} akan dikirim konselor"
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            SeivCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Catatan untuk Konselor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text, modifier = Modifier.weight(1f))
                    Text(if (editing) "Simpan" else "Edit ›", fontSize = 11.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            if (editing) SeivSync.updateSessionPrefs(s.copy(note = note.trim()))
                            editing = !editing
                        })
                }
                Spacer(Modifier.height(8.dp))
                if (editing) {
                    OutlinedTextField(
                        value = note, onValueChange = { if (it.length <= 300) note = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tulis catatan untuk konselor...", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                    )
                } else {
                    Text(
                        note.ifBlank { "Belum ada catatan. Tambahkan untuk membantu konselor mempersiapkan diri." },
                        fontSize = 12.sp, color = if (note.isBlank()) Seiv.TextHint else Seiv.Text,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Seiv.Background).padding(12.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Insights, null, tint = Seiv.Orange, modifier = Modifier.size(14.dp))
                    Text(" Data check-in dibagikan", fontSize = 11.sp, color = Seiv.TextSecondary, modifier = Modifier.weight(1f))
                    Switch(share, { share = it; SeivSync.updateSessionPrefs(s.copy(shareCheckIn = it, note = note)) }, colors = SwitchDefaults.colors(checkedTrackColor = Seiv.Primary))
                }
            }
            if (s.counselorNote.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                SeivCard(Modifier.fillMaxWidth(), color = Seiv.MintSoft) {
                    Text("Pesan dari konselor", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.PrimaryDark)
                    Spacer(Modifier.height(6.dp))
                    Text(s.counselorNote, fontSize = 12.sp, color = Seiv.Text, lineHeight = 18.sp)
                }
            }
            if (s.status == SessionStatus.COMPLETED) {
                Spacer(Modifier.height(12.dp))
                ReviewSection(s, c)
            }
            Spacer(Modifier.height(16.dp))
        }
        // Ruang sesi (chat maupun voice/video) hanya terbuka pada jam sesi:
        // tepat jam mulai sampai akhir durasi + 15 menit (sama dengan aturan di server)
        val roomCloses = end + ROOM_GRACE_MINUTES * 60_000
        val confirmed = s.status == SessionStatus.CONFIRMED
        val beforeStart = now < start
        val roomOpen = confirmed && !beforeStart && now <= roomCloses
        val isChat = s.method == "Chat"
        if (isChat) {
            // Sesi Chat: ruang chat di dalam aplikasi (riwayat tetap bisa dibaca setelah sesi)
            PrimaryButton(
                when {
                    s.status == SessionStatus.WAITING -> "Ruang chat terbuka setelah verifikasi"
                    s.status == SessionStatus.CANCELLED -> "Sesi dibatalkan"
                    confirmed && beforeStart -> "Ruang chat dibuka pukul ${s.time} WIB"
                    roomOpen -> "Buka Ruang Chat"
                    else -> "Lihat Riwayat Chat"
                },
                icon = methodIcon(s.method), container = Seiv.PrimaryDark,
                enabled = roomOpen || s.status == SessionStatus.COMPLETED || (confirmed && now > roomCloses),
                onClick = { nav.navigate("${Routes.CHAT}/${s.id}") },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        } else {
            PrimaryButton(
                when {
                    s.status == SessionStatus.COMPLETED || now > roomCloses -> "Sesi telah selesai"
                    confirmed && beforeStart -> "Ruang sesi dibuka pukul ${s.time} WIB"
                    else -> "Masuk ke Sesi"
                },
                icon = methodIcon(s.method), container = Seiv.PrimaryDark,
                enabled = now <= roomCloses && s.status != SessionStatus.CANCELLED && s.status != SessionStatus.COMPLETED,
                onClick = {
                    if (roomOpen && s.meetingUrl.isNotBlank()) {
                        runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(s.meetingUrl))) }
                            .onFailure { showJoin = true }
                    } else showJoin = true
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
    if (showJoin) {
        val verified = s.status == SessionStatus.CONFIRMED
        val timeOk = now >= start
        val hasLink = s.meetingUrl.isNotBlank()
        AlertDialog(
            onDismissRequest = { showJoin = false },
            confirmButton = { TextButton({ showJoin = false }) { Text("Mengerti", color = Seiv.Primary) } },
            title = {
                Text(
                    when {
                        !verified -> "Menunggu verifikasi"
                        !hasLink -> "Tautan belum dikirim"
                        !timeOk -> "Ruang sesi belum dibuka"
                        else -> "Tidak bisa membuka tautan"
                    },
                )
            },
            text = {
                Text(
                    when {
                        !verified -> "Pembayaranmu sedang diverifikasi admin SEIV. Ruang sesi akan terbuka setelah sesi terkonfirmasi."
                        !hasLink -> "${c.name} belum mengirim tautan ${s.method.lowercase()}. Kamu akan mendapat notifikasi begitu tautannya tersedia."
                        !timeOk -> "Ruang sesi bisa dimasuki saat jam sesi dimulai (${s.time} WIB). Tautan dari ${c.name} sudah siap."
                        else -> "Pastikan aplikasi Google Meet atau browser terpasang, lalu coba lagi."
                    },
                    fontSize = 13.sp,
                )
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, chip: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Seiv.Background), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Seiv.TextSecondary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = Seiv.TextHint)
            Text(value, fontSize = 12.sp, color = Seiv.Text, fontWeight = FontWeight.SemiBold)
        }
        if (chip != null) Tag(chip)
    }
}
