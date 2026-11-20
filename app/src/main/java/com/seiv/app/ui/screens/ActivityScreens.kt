package com.seiv.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.*
import com.seiv.app.ui.Routes
import com.seiv.app.ui.components.*
import com.seiv.app.ui.resetTo
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

// ---------------------------------------------------------------------------
// Tab Aktivitas
// ---------------------------------------------------------------------------

@Composable
fun ActivityScreen(nav: NavHostController) {
    val promptOfDay = JOURNAL_PROMPTS[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % JOURNAL_PROMPTS.size]
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Aktivitas", style = MaterialTheme.typography.titleLarge)
            Text("Refleksi diri dan latihan kecil untuk harimu", fontSize = 12.sp, color = Seiv.TextSecondary)
        }

        // Tren mood minggu ini
        SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            SectionTitle("Mood 7 hari terakhir", action = "Riwayat") { nav.navigate(Routes.HISTORY) }
            Spacer(Modifier.height(14.dp))
            MoodWeek()
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Latihan & ruang aman", Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolTile(Icons.Outlined.Air, Seiv.Primary, Seiv.MintLight, "Latihan\nNapas", Modifier.weight(1f)) { nav.navigate(Routes.BREATHING) }
            ToolTile(Icons.Outlined.EditNote, Seiv.Orange, Seiv.OrangeSoft, "Tulis\nJurnal", Modifier.weight(1f)) { nav.navigate(Routes.JOURNAL_EDIT) }
            ToolTile(Icons.Outlined.AutoAwesome, Seiv.Purple, Seiv.PurpleSoft, "Ruang\nCerita", Modifier.weight(1f)) { nav.navigate(Routes.COMPANION) }
        }

        Spacer(Modifier.height(20.dp))
        Column(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFF1E6), Color(0xFFFDE5D8)))).padding(16.dp),
        ) {
            Text("✍️ PROMPT HARI INI", fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = Seiv.Orange)
            Spacer(Modifier.height(8.dp))
            Text(promptOfDay, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Seiv.Text, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Mulai menulis →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Seiv.Orange)
                    .clickable { nav.navigate("${Routes.JOURNAL_EDIT}?prompt=${Uri.encode(promptOfDay)}") }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Jurnal Refleksi", Modifier.padding(horizontal = 20.dp), action = "+ Tulis") { nav.navigate(Routes.JOURNAL_EDIT) }
        Spacer(Modifier.height(10.dp))
        if (SeivStore.journals.isEmpty()) {
            SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                Text("Belum ada jurnal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Seiv.Text)
                Text("Tulisanmu hanya tersimpan di perangkat ini dan bersifat pribadi.", fontSize = 11.sp, color = Seiv.TextSecondary)
            }
        }
        var toDelete by remember { mutableStateOf<JournalEntry?>(null) }
        SeivStore.journals.forEach { j ->
            var expanded by remember(j.id) { mutableStateOf(false) }
            SeivCard(Modifier.padding(horizontal = 20.dp, vertical = 5.dp).fillMaxWidth(), onClick = { expanded = !expanded }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(j.timestamp, "EEEE, d MMM · HH:mm"), fontSize = 10.sp, color = Seiv.TextHint, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.DeleteOutline, "Hapus", tint = Seiv.TextHint, modifier = Modifier.size(16.dp).clickable { toDelete = j })
                }
                if (j.prompt.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(j.prompt, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = Seiv.Primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    j.text, fontSize = 13.sp, color = Seiv.Text, lineHeight = 19.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 3, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        toDelete?.let { j ->
            AlertDialog(
                onDismissRequest = { toDelete = null },
                title = { Text("Hapus jurnal?") },
                text = { Text("Tulisan ini akan dihapus permanen dari perangkatmu.", fontSize = 13.sp) },
                confirmButton = { TextButton({ SeivStore.deleteJournal(j.id); toDelete = null }) { Text("Hapus", color = Seiv.Red) } },
                dismissButton = { TextButton({ toDelete = null }) { Text("Batal", color = Seiv.Primary) } },
                containerColor = Color.White,
            )
        }
    }
}

@Composable
private fun MoodWeek() {
    val days = (6 downTo 0).map { off -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -off) }.timeInMillis }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            val c = SeivStore.checkIns.firstOrNull { isSameDay(it.timestamp, d) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val h = if (c == null) 10.dp else (24 + c.mood * 14).dp
                Box(
                    Modifier.width(24.dp).height(h).clip(RoundedCornerShape(8.dp))
                        .background(if (c == null) Seiv.Border else Seiv.Primary.copy(alpha = 0.35f + c.mood * 0.16f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(if (c == null) "·" else MOODS[c.mood].first, fontSize = 14.sp)
                Text(formatDate(d, "EEE"), fontSize = 9.sp, color = if (isSameDay(d, System.currentTimeMillis())) Seiv.Primary else Seiv.TextHint)
            }
        }
    }
}

@Composable
private fun ToolTile(icon: ImageVector, tint: Color, bg: Color, label: String, modifier: Modifier, onClick: () -> Unit) {
    SeivCard(modifier, padding = PaddingValues(vertical = 14.dp), onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, tint, bg, 40.dp)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Seiv.Text, textAlign = TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Editor jurnal
// ---------------------------------------------------------------------------

@Composable
fun JournalEditorScreen(nav: NavHostController, initialPrompt: String?) {
    val ctx = LocalContext.current
    var prompt by remember { mutableStateOf(initialPrompt.orEmpty()) }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeader("Jurnal Refleksi", onBack = { nav.popBackStack() }, overline = formatDate(System.currentTimeMillis(), "EEEE, d MMMM yyyy"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("Pilih prompt (opsional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Seiv.Text, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("✏️ Tulis bebas", prompt.isEmpty(), { prompt = "" })
                JOURNAL_PROMPTS.forEach { p -> Chip(p.take(34) + if (p.length > 34) "…" else "", prompt == p, { prompt = p }) }
            }
            Spacer(Modifier.height(16.dp))
            if (prompt.isNotEmpty()) {
                Text(
                    prompt, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Seiv.PrimaryDark, lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Seiv.MintSoft).padding(14.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 260.dp),
                placeholder = { Text("Tuliskan apa pun yang kamu rasakan. Tidak ada jawaban benar atau salah...", fontSize = 13.sp, color = Seiv.TextHint) },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                    unfocusedBorderColor = Seiv.Border, focusedBorderColor = Seiv.Primary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 22.sp),
            )
            Text(
                "🔒 Jurnalmu hanya tersimpan di perangkat ini.", fontSize = 10.sp, color = Seiv.TextHint,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        PrimaryButton(
            "Simpan Jurnal", enabled = text.isNotBlank(),
            onClick = {
                SeivStore.addJournal(JournalEntry(prompt = prompt, text = text.trim()))
                Toast.makeText(ctx, "Jurnal tersimpan 🌿", Toast.LENGTH_SHORT).show()
                nav.popBackStack()
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Latihan napas 4-7-8
// ---------------------------------------------------------------------------

@Composable
fun BreathingScreen(nav: NavHostController) {
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Siap?") }
    var count by remember { mutableIntStateOf(0) }
    var cycle by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.6f) }
    val totalCycles = 4

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        cycle = 0
        while (cycle < totalCycles) {
            cycle++
            for ((label, secs, target) in listOf(Triple("Tarik napas", 4, 1f), Triple("Tahan", 7, 1f), Triple("Hembuskan", 8, 0.6f))) {
                phase = label
                count = secs
                coroutineScope {
                    if (target != scale.value) launch { scale.animateTo(target, tween(secs * 1000, easing = LinearEasing)) }
                    repeat(secs) { delay(1000); count-- }
                }
            }
        }
        phase = "Selesai 🌿"
        running = false
    }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE3EFEA), Seiv.Cream))).statusBarsPadding().navigationBarsPadding(),
    ) {
        ScreenHeader("Latihan Napas", onBack = { nav.popBackStack() }, subtitle = "Teknik 4-7-8 · $totalCycles siklus")
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(280.dp).scale(scale.value).clip(CircleShape).background(Seiv.Mint.copy(alpha = 0.25f)))
            Box(Modifier.size(220.dp).scale(scale.value).clip(CircleShape).background(Seiv.Mint.copy(alpha = 0.45f)))
            Box(
                Modifier.size(160.dp).scale(scale.value).shadow(8.dp, CircleShape).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Seiv.Primary, Seiv.PrimaryDark))),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phase, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (running) Text("$count", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
            }
        }
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (running) "Siklus $cycle dari $totalCycles" else "Tarik napas 4 detik, tahan 7 detik, lalu hembuskan perlahan 8 detik. Latihan ini membantu menenangkan sistem saraf.",
                fontSize = 13.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            if (running) "Berhenti" else if (phase.startsWith("Selesai")) "Ulangi Latihan" else "Mulai",
            onClick = {
                if (running) { running = false; phase = "Siap?" } else running = true
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Riwayat check-in
// ---------------------------------------------------------------------------

@Composable
fun CheckInHistoryScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Riwayat Check-in", onBack = { nav.popBackStack() }, subtitle = "🔥 Streak ${SeivStore.streak()} hari · ${SeivStore.checkIns.size} catatan")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (SeivStore.checkIns.isEmpty()) EmptyState("🌤️", "Belum ada check-in", "Catat perasaanmu setiap hari untuk melihat polanya di sini.")
            SeivStore.checkIns.forEach { c ->
                SeivCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), padding = PaddingValues(14.dp), onClick = { nav.navigate("${Routes.INSIGHT}/${c.id}") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(MOODS[c.mood].first, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(MOODS[c.mood].second, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                            Text(formatDate(c.timestamp, "EEEE, d MMM yyyy · HH:mm"), fontSize = 10.sp, color = Seiv.TextHint)
                            if (c.factors.isNotEmpty() || c.feelings.isNotEmpty()) {
                                Text((c.factors + c.feelings).joinToString(" · "), fontSize = 11.sp, color = Seiv.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (c.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("“${c.note}”", fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Seiv.Text.copy(alpha = 0.8f))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Bantuan profesional / referral
// ---------------------------------------------------------------------------

@Composable
fun ReferralScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Bantuan Profesional", onBack = { nav.popBackStack() }, subtitle = "Kamu tidak harus menghadapinya sendiri")
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Seiv.RedSoft).border(1.dp, Seiv.Red.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(16.dp),
            ) {
                Text("🆘 Dalam keadaan darurat?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Seiv.Red)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Jika kamu memiliki pikiran untuk menyakiti diri sendiri atau merasa tidak aman, segera hubungi layanan di bawah ini atau orang terdekat yang kamu percaya.",
                    fontSize = 12.sp, color = Seiv.Text, lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            HOTLINES.forEach { h ->
                SeivCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = {
                    val intent = if (h.number.all { it.isDigit() }) Intent(Intent.ACTION_DIAL, Uri.parse("tel:${h.number}"))
                    else Intent(Intent.ACTION_VIEW, Uri.parse("https://${h.number}"))
                    runCatching { ctx.startActivity(intent) }.onFailure { Toast.makeText(ctx, h.number, Toast.LENGTH_SHORT).show() }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(if (h.number.all { it.isDigit() }) Icons.Outlined.Call else Icons.Outlined.Language, Seiv.Red, Seiv.RedSoft)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                            Text(h.desc, fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 15.sp)
                        }
                        Text(h.number, fontWeight = FontWeight.Bold, fontSize = if (h.number.length < 5) 18.sp else 10.sp, color = Seiv.Red)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Pendampingan lanjutan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
            Spacer(Modifier.height(10.dp))
            NextStep(Icons.Outlined.SupportAgent, Seiv.Primary, Seiv.MintLight, "Konseling di SEIV", "Nurse counselor & psikolog berlisensi") { nav.navigate(Routes.COUNSELORS) }
            NextStep(Icons.Outlined.School, Seiv.Orange, Seiv.OrangeSoft, "Layanan Konseling Kampus", "Hubungi unit konseling / BK di kampusmu") {
                Toast.makeText(ctx, "Hubungi unit layanan konseling di fakultas atau universitasmu.", Toast.LENGTH_LONG).show()
            }
            NextStep(Icons.Outlined.LocalHospital, Seiv.Purple, Seiv.PurpleSoft, "Puskesmas / Rumah Sakit", "Layanan psikolog & psikiater dengan BPJS") {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=puskesmas"))) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Profil
// ---------------------------------------------------------------------------

@Composable
fun ProfileScreen(nav: NavHostController) {
    val user = SeivStore.user
    var editName by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Text("Profil", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(20.dp))
        SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Seiv.Mint, Seiv.Primary))), contentAlignment = Alignment.Center) {
                    Text(user?.name?.take(1)?.uppercase() ?: "S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.name ?: "-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Seiv.Text)
                    Text(user?.email ?: "-", fontSize = 12.sp, color = Seiv.TextSecondary)
                }
                Icon(Icons.Outlined.Edit, "Ubah nama", tint = Seiv.Primary, modifier = Modifier.size(20.dp).clickable { editName = true })
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStat("${SeivStore.streak()}", "Streak", Modifier.weight(1f))
                ProfileStat("${SeivStore.checkIns.size}", "Check-in", Modifier.weight(1f))
                ProfileStat("${SeivStore.journals.size}", "Jurnal", Modifier.weight(1f))
                ProfileStat("${SeivStore.screenings.size}", "Skrining", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))
        MenuItem(Icons.Outlined.History, "Riwayat Check-in") { nav.navigate(Routes.HISTORY) }
        MenuItem(Icons.Outlined.Psychology, "Riwayat Skrining") {
            val last = SeivStore.screenings.firstOrNull()
            if (last != null) nav.navigate("${Routes.SCREENING_RESULT}/${last.id}") else nav.navigate(Routes.SCREENING_RUN)
        }
        MenuItem(Icons.AutoMirrored.Outlined.EventNote, "Sesi Konseling") { nav.navigate(Routes.SESSIONS) }
        MenuItem(Icons.AutoMirrored.Outlined.MenuBook, "Jurnal Refleksi") { nav.navigate(Routes.JOURNAL_EDIT) }
        MenuItem(Icons.Outlined.Emergency, "Bantuan Profesional & Darurat", Seiv.Red) { nav.navigate(Routes.REFERRAL) }
        MenuItem(Icons.Outlined.Shield, "Privasi & Keamanan Data") {}
        MenuItem(Icons.AutoMirrored.Outlined.Logout, "Keluar", Seiv.Red) { confirmLogout = true }
        Spacer(Modifier.height(16.dp))
        Text(
            "SEIV v1.0 · Ruang aman untuk kesehatan mental mahasiswa\nMendukung SDGs 3: Kehidupan Sehat dan Sejahtera",
            fontSize = 10.sp, color = Seiv.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
    if (editName) {
        var name by remember { mutableStateOf(user?.name.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Ubah nama panggilan") },
            text = { OutlinedTextField(name, { name = it }, singleLine = true, shape = RoundedCornerShape(12.dp)) },
            confirmButton = { TextButton({ if (name.isNotBlank()) SeivStore.updateName(name.trim()); editName = false }) { Text("Simpan", color = Seiv.Primary) } },
            dismissButton = { TextButton({ editName = false }) { Text("Batal", color = Seiv.TextSecondary) } },
            containerColor = Color.White,
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar dari SEIV?") },
            text = { Text("Data check-in dan jurnalmu tetap tersimpan di perangkat ini.", fontSize = 13.sp) },
            confirmButton = { TextButton({ confirmLogout = false; SeivStore.logout(); nav.resetTo(Routes.LOGIN) }) { Text("Keluar", color = Seiv.Red) } },
            dismissButton = { TextButton({ confirmLogout = false }) { Text("Batal", color = Seiv.Primary) } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Seiv.Background).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Seiv.PrimaryDark)
        Text(label, fontSize = 9.sp, color = Seiv.TextSecondary)
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, tint: Color = Seiv.Text, onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 13.sp, color = tint, modifier = Modifier.weight(1f))
        Text("›", fontSize = 18.sp, color = Seiv.TextHint)
    }
}
