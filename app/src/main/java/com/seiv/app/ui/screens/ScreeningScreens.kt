package com.seiv.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.*
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.Routes
import com.seiv.app.ui.components.*
import com.seiv.app.ui.goTab
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.delay

fun sectionColor(section: Int) = when (section) { 1 -> Seiv.Primary; 2 -> Seiv.Orange; else -> Seiv.Purple }
fun sectionSoft(section: Int) = when (section) { 1 -> Seiv.MintLight; 2 -> Seiv.OrangeSoft; else -> Seiv.PurpleSoft }
private fun sectionEmoji(section: Int) = when (section) { 1 -> "🌿"; 2 -> "🧘"; else -> "💜" }

// ---------------------------------------------------------------------------
// Pengantar skrining (tab)
// ---------------------------------------------------------------------------

@Composable
fun ScreeningIntroScreen(nav: NavHostController) {
    val totalQ = QUESTIONNAIRES.sumOf { it.questions.size }
    val totalMin = QUESTIONNAIRES.sumOf { it.minutes }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text("Kuesioner Skrining", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))
            Column(
                Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Seiv.PrimaryDeep, Seiv.PrimaryDark, Seiv.Primary))).padding(20.dp),
            ) {
                Text("🧠", fontSize = 32.sp)
                Spacer(Modifier.height(10.dp))
                Text("Kenali kondisi mentalmu hari ini", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Skrining ini terdiri dari 3 kuesioner klinis yang membantu mengidentifikasi kondisi kesehatan mentalmu.",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp,
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Schedule, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Text("  Total ±$totalMin menit · $totalQ pertanyaan", color = Color.White, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("3 Bagian Kuesioner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
            Spacer(Modifier.height(10.dp))
            QUESTIONNAIRES.forEach { q ->
                SeivCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(sectionSoft(q.section)), contentAlignment = Alignment.Center) {
                            Text(sectionEmoji(q.section), fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Tag("Bagian ${q.section}", sectionColor(q.section), sectionSoft(q.section))
                                Text("  ±${q.minutes} menit · ${q.questions.size} pertanyaan", fontSize = 10.sp, color = Seiv.TextHint)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(q.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                            Text(q.description, fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 15.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Seiv.Warning).padding(12.dp)) {
                Text("ℹ️", fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Skrining bukan diagnosis. Hasilnya membantu kamu memahami kondisi dan menentukan langkah selanjutnya. Jawablah dengan jujur — jawabanmu bersifat rahasia.",
                    fontSize = 11.sp, color = Seiv.Text.copy(alpha = 0.75f), lineHeight = 16.sp,
                )
            }
            if (SeivStore.screenings.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Riwayat Skrining", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                Spacer(Modifier.height(10.dp))
                SeivStore.screenings.take(5).forEach { r ->
                    SeivCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), padding = PaddingValues(12.dp), onClick = { nav.navigate("${Routes.SCREENING_RESULT}/${r.id}") }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(formatDate(r.timestamp, "d MMMM yyyy, HH:mm"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Seiv.Text)
                                Text("SRQ ${r.srq} · GAD ${r.gad} · PHQ ${r.phq}", fontSize = 11.sp, color = Seiv.TextSecondary)
                            }
                            val lvl = r.careLevel()
                            Tag(
                                listOf("Baik", "Perlu perhatian", "Disarankan konseling", "Butuh bantuan segera")[lvl],
                                if (lvl >= 2) Seiv.Orange else Seiv.Primary, if (lvl >= 2) Seiv.OrangeSoft else Seiv.MintLight,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton("Mulai Skrining", onClick = { nav.navigate(Routes.SCREENING_RUN) }, container = Seiv.PrimaryDark, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
    }
}

// ---------------------------------------------------------------------------
// Pengisian kuesioner
// ---------------------------------------------------------------------------

@Composable
fun ScreeningRunScreen(nav: NavHostController) {
    val answers = remember { QUESTIONNAIRES.map { q -> mutableStateListOf<Int?>().apply { repeat(q.questions.size) { add(null) } } } }
    var section by remember { mutableIntStateOf(0) }
    var index by remember { mutableIntStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }
    var pendingAdvance by remember { mutableIntStateOf(0) }
    val q = QUESTIONNAIRES[section]
    val color = sectionColor(q.section)
    val soft = sectionSoft(q.section)

    fun back() {
        when {
            index > 0 -> index--
            section > 0 -> { section--; index = QUESTIONNAIRES[section].questions.lastIndex }
            else -> confirmExit = true
        }
    }
    BackHandler { back() }

    fun finish() {
        val srq = answers[0].count { it == 1 }
        val gad = answers[1].sumOf { it ?: 0 }
        val phq = answers[2].sumOf { it ?: 0 }
        val r = ScreeningResult(srq = srq, gad = gad, phq = phq, phqItem9 = answers[2].last() ?: 0)
        SeivSync.addScreening(r)
        nav.navigate("${Routes.SCREENING_RESULT}/${r.id}") { popUpTo(Routes.SCREENING) }
    }

    LaunchedEffect(pendingAdvance) {
        if (pendingAdvance == 0) return@LaunchedEffect
        delay(260)
        when {
            index < q.questions.lastIndex -> index++
            section < QUESTIONNAIRES.lastIndex -> { section++; index = 0 }
            else -> finish()
        }
    }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackCircle({ back() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    QUESTIONNAIRES.forEachIndexed { i, qq ->
                        val progress = when {
                            i < section -> 1f
                            i == section -> (index + 1f) / qq.questions.size
                            else -> 0f
                        }
                        Box(Modifier.weight(qq.questions.size.toFloat()).height(5.dp).clip(RoundedCornerShape(50)).background(Seiv.Border)) {
                            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(sectionColor(qq.section)))
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("${index + 1}/${q.questions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }

        AnimatedContent(
            targetState = section to index,
            transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "question",
        ) { (s, i) ->
            val qq = QUESTIONNAIRES[s]
            val c = sectionColor(qq.section)
            val sf = sectionSoft(qq.section)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Tag("BAGIAN ${qq.section} · ${qq.code}", c, sf)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag("Pertanyaan ${i + 1}", c, sf)
                    Spacer(Modifier.weight(1f))
                    Text(qq.period, fontSize = 10.sp, color = Seiv.TextHint)
                }
                Spacer(Modifier.height(16.dp))
                if (qq.stem != null) {
                    Text(qq.stem, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Seiv.Text, lineHeight = 24.sp)
                    Text(qq.questions[i], fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = c, lineHeight = 24.sp)
                } else {
                    Text(qq.questions[i], fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Seiv.Text, lineHeight = 26.sp)
                }
                Spacer(Modifier.height(24.dp))
                val current = answers[s][i]
                if (qq.type == QuestionType.YES_NO) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(Triple(1, "👍", "Ya"), Triple(0, "👎", "Tidak")).forEach { (v, e, label) ->
                            val sel = current == v
                            Column(
                                Modifier.weight(1f).shadow(if (sel) 4.dp else 1.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) sf else Color.White).border(1.5.dp, if (sel) c else Color.Transparent, RoundedCornerShape(16.dp))
                                    .clickable { answers[s][i] = v; pendingAdvance++ }.padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(e, fontSize = 26.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (sel) c else Seiv.Text)
                            }
                        }
                    }
                } else {
                    FREQUENCY_OPTIONS.forEachIndexed { v, label ->
                        val sel = current == v
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp).shadow(if (sel) 3.dp else 1.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp)).background(if (sel) sf else Color.White)
                                .border(1.5.dp, if (sel) c else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { answers[s][i] = v; pendingAdvance++ }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (sel) c else sf), contentAlignment = Alignment.Center) {
                                Text("$v", color = if (sel) Color.White else c, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = 13.sp, color = Seiv.Text, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        Text(
            "Jawablah sesuai yang kamu rasakan selama ${q.period}.",
            fontSize = 10.sp, color = Seiv.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Keluar dari skrining?") },
            text = { Text("Jawaban yang sudah kamu isi tidak akan disimpan.", fontSize = 13.sp) },
            confirmButton = { TextButton({ confirmExit = false; nav.popBackStack() }) { Text("Keluar", color = Seiv.Red) } },
            dismissButton = { TextButton({ confirmExit = false }) { Text("Lanjutkan", color = Seiv.Primary) } },
            containerColor = Color.White,
        )
    }
}

// ---------------------------------------------------------------------------
// Hasil & rekomendasi
// ---------------------------------------------------------------------------

@Composable
fun ScreeningResultScreen(nav: NavHostController, id: String) {
    val r = SeivStore.screenings.firstOrNull { it.id == id } ?: return
    val level = r.careLevel()
    val summary = when (level) {
        0 -> "Kondisi mentalmu secara keseluruhan tampak baik. Terus jaga dirimu!"
        1 -> "Ada beberapa hal yang perlu kamu perhatikan. Yuk, rawat dirimu lebih baik."
        2 -> "Hasilmu menunjukkan kamu sedang melalui masa yang berat. Bicara dengan konselor bisa sangat membantu."
        else -> "Keselamatanmu adalah yang terpenting. Kami sangat menyarankan kamu segera mencari bantuan."
    }
    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Selesai ✓ · ${formatDate(r.timestamp, "d MMM yyyy")}", fontSize = 10.sp, color = Seiv.TextSecondary)
                Text("Hasil Skrining", style = MaterialTheme.typography.titleLarge)
            }
            Box(
                Modifier.size(36.dp).shadow(1.dp, CircleShape).clip(CircleShape).background(Color.White)
                    .clickable { nav.navigate(Routes.SCREENING_RUN) { popUpTo(Routes.SCREENING) } },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Refresh, "Ulangi", tint = Seiv.Text, modifier = Modifier.size(18.dp)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (level == 3) {
                CrisisCard { nav.navigate(Routes.REFERRAL) }
                Spacer(Modifier.height(12.dp))
            }
            Column(
                Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Seiv.PrimaryDeep, Seiv.Primary))).padding(16.dp),
            ) {
                Row {
                    Text(if (level == 0) "🌱" else if (level == 1) "🌤️" else "🤝", fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("RINGKASAN HASIL", color = Seiv.MintLight, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(summary, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreBox("${r.srq}", "SRQ-20", srqSeverity(r.srq).label, Modifier.weight(1f))
                    ScoreBox("${r.gad}", "GAD-7", gadSeverity(r.gad).label, Modifier.weight(1f))
                    ScoreBox("${r.phq}", "PHQ-9", phqSeverity(r.phq).label, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Detail Per Kuesioner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
            Spacer(Modifier.height(10.dp))
            DetailCard(SRQ20, r.srq, 20, srqSeverity(r.srq), if (r.srq >= 6) "Ada indikasi gangguan mental emosional" else "Tidak ada indikasi gangguan", srqDescription(r.srq))
            DetailCard(GAD7, r.gad, 21, gadSeverity(r.gad), "Kecemasan ${gadSeverity(r.gad).label.lowercase()}", gadDescription(r.gad))
            DetailCard(PHQ9, r.phq, 27, phqSeverity(r.phq), "Gejala depresi ${phqSeverity(r.phq).label.lowercase()}", phqDescription(r.phq))

            Spacer(Modifier.height(10.dp))
            Text("Rekomendasi untukmu", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
            Text(
                when (level) {
                    0 -> "Promotif — pertahankan kesejahteraan mentalmu"
                    1 -> "Preventif — cegah kondisi agar tidak memburuk"
                    else -> "Kuratif — dapatkan pendampingan profesional"
                },
                fontSize = 11.sp, color = Seiv.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            if (level >= 2) {
                NextStep(Icons.Outlined.SupportAgent, Seiv.Orange, Seiv.OrangeSoft, "Jadwalkan Konseling", "Bicara dengan nurse counselor atau psikolog") { nav.navigate(Routes.COUNSELORS) }
            }
            NextStep(Icons.Outlined.AutoAwesome, Seiv.Purple, Seiv.PurpleSoft, "Ceritakan ke AI Companion", "Ruang aman untuk mengekspresikan perasaan") { nav.navigate(Routes.COMPANION) }
            NextStep(Icons.Outlined.Air, Seiv.Primary, Seiv.MintLight, "Latihan Napas", "Redakan ketegangan dalam 2 menit") { nav.navigate(Routes.BREATHING) }
            NextStep(Icons.AutoMirrored.Outlined.MenuBook, Color(0xFF3D7AB8), Color(0xFFE2EEF9), "Jurnal Refleksi", "Kenali pola pikiran dan perasaanmu") { nav.navigate(Routes.JOURNAL_EDIT) }
            if (level < 3) {
                NextStep(Icons.Outlined.Emergency, Seiv.Red, Seiv.RedSoft, "Bantuan Profesional & Darurat", "Daftar layanan yang bisa kamu hubungi") { nav.navigate(Routes.REFERRAL) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("Kembali ke Beranda", icon = Icons.Outlined.Home, container = Seiv.PrimaryDark, onClick = { nav.goTab(Routes.HOME) })
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Ulangi Skrining", icon = Icons.Outlined.Refresh, onClick = { nav.navigate(Routes.SCREENING_RUN) { popUpTo(Routes.SCREENING) } })
        }
    }
}

@Composable
private fun ScoreBox(score: String, label: String, severity: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(score, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            severity, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun DetailCard(q: Questionnaire, score: Int, max: Int, severity: Severity, headline: String, desc: String) {
    val c = sectionColor(q.section)
    val sf = sectionSoft(q.section)
    val sevColor = when (severity.level) { 0 -> Seiv.Primary; 1 -> Seiv.Yellow; 2 -> Seiv.Orange; else -> Seiv.Red }
    SeivCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), color = sf.copy(alpha = 0.55f)) {
        Row {
            Column(Modifier.weight(1f)) {
                Tag("Bagian ${q.section}", c, Color.White)
                Spacer(Modifier.height(6.dp))
                Text(q.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Seiv.Text)
                Text(q.fullName, fontSize = 10.sp, color = Seiv.TextSecondary)
            }
            Text(sectionEmoji(q.section), fontSize = 24.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Seiv.Text, lineHeight = 16.sp)
                    Text("/$max", fontSize = 8.sp, color = Seiv.TextHint, lineHeight = 9.sp)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    severity.label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(sevColor).padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.height(3.dp))
                Text(headline, fontSize = 11.sp, color = Seiv.Text)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color.White)) {
            Box(Modifier.fillMaxWidth((score.toFloat() / max).coerceIn(0.03f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(c))
        }
        Spacer(Modifier.height(8.dp))
        Text(desc, fontSize = 11.sp, color = Seiv.Text.copy(alpha = 0.75f), lineHeight = 16.sp)
    }
}

@Composable
fun CrisisCard(onClick: () -> Unit) {
    SeivCard(Modifier.fillMaxWidth(), color = Seiv.RedSoft, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🆘", fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Kamu tidak sendirian", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Seiv.Red)
                Text(
                    "Jawabanmu menunjukkan adanya pikiran menyakiti diri. Hubungi SEJIWA 119 ext. 8 atau 112 sekarang, atau ketuk untuk melihat bantuan.",
                    fontSize = 11.sp, color = Seiv.Text, lineHeight = 16.sp,
                )
            }
        }
    }
}
