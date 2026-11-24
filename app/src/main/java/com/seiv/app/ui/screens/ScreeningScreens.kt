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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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

private val Blue = Color(0xFF3D7AB8)
private val BlueSoft = Color(0xFFE2EEF9)
private val Gold = Color(0xFFB7860B)

fun sectionColor(section: Int) = when (section) {
    1 -> Seiv.Primary; 2 -> Seiv.Purple; 3 -> Seiv.Orange; 4 -> Seiv.Red; 5 -> Blue; else -> Gold
}
fun sectionSoft(section: Int) = when (section) {
    1 -> Seiv.MintLight; 2 -> Seiv.PurpleSoft; 3 -> Seiv.OrangeSoft; 4 -> Seiv.RedSoft; 5 -> BlueSoft; else -> Seiv.YellowSoft
}
private fun sectionEmoji(section: Int) = when (section) {
    1 -> "🌿"; 2 -> "💜"; 3 -> "🧘"; 4 -> "🛟"; 5 -> "🔍"; else -> "💪"
}

// ---------------------------------------------------------------------------
// Pengantar skrining (tab)
// ---------------------------------------------------------------------------

@Composable
fun ScreeningIntroScreen(nav: NavHostController) {
    val totalQ = QUESTIONNAIRES.sumOf { it.mainCount }
    val totalMin = QUESTIONNAIRES.sumOf { it.minutes }
    var consent by remember { mutableStateOf(false) }
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
                    "Skrining kesehatan jiwa ini terdiri dari ${QUESTIONNAIRES.size} instrumen: gejala mental emosional, depresi, kecemasan, risiko bunuh diri, pengalaman psikologis tertentu, dan resiliensi.",
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
            Text("${QUESTIONNAIRES.size} Bagian Kuesioner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
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
                                Text("  ±${q.minutes} menit · ${q.mainCount} pertanyaan", fontSize = 10.sp, color = Seiv.TextHint)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(q.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                            Text(q.description, fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 15.sp)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Seiv.Warning).padding(12.dp)) {
                Text("ℹ️  Hal yang perlu diketahui", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Seiv.Text)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Skrining ini bukan diagnosis gangguan jiwa — hasilnya merupakan gambaran awal.",
                    "Beberapa pertanyaan bersifat pribadi dan sensitif. Jawablah sesuai kondisi dan pengalamanmu.",
                    "Data dan hasil skrining bersifat rahasia dan hanya dapat dilihat olehmu (serta konselor bila kamu memilih membagikannya).",
                    "Bila ditemukan risiko keselamatan yang mendesak, aplikasi akan langsung menampilkan layanan darurat yang bisa kamu hubungi.",
                ).forEach { Text("• $it", fontSize = 11.sp, color = Seiv.Text.copy(alpha = 0.75f), lineHeight = 16.sp) }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).clickable { consent = !consent }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (consent) Seiv.Primary else Color.White)
                        .border(1.5.dp, if (consent) Seiv.Primary else Seiv.TextHint, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (consent) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Saya telah membaca, memahami, dan menyetujui pelaksanaan skrining kesehatan jiwa ini.",
                    fontSize = 12.sp, color = Seiv.Text, lineHeight = 17.sp,
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
                                Text(
                                    "SRQ ${r.srq} · PHQ ${r.phq} · GAD ${r.gad}" + if (r.isV2) " · CD-RISC ${r.cdrisc}" else "",
                                    fontSize = 11.sp, color = Seiv.TextSecondary,
                                )
                            }
                            CareTag(r.careLevel())
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        PrimaryButton(
            if (consent) "Mulai Skrining" else "Centang persetujuan untuk mulai",
            onClick = { nav.navigate(Routes.SCREENING_RUN) }, enabled = consent, container = Seiv.PrimaryDark,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CareTag(level: Int) {
    Tag(
        listOf("Baik", "Perlu perhatian", "Disarankan konseling", "Butuh bantuan segera")[level],
        when (level) { 3 -> Seiv.Red; 2 -> Seiv.Orange; else -> Seiv.Primary },
        when (level) { 3 -> Seiv.RedSoft; 2 -> Seiv.OrangeSoft; else -> Seiv.MintLight },
    )
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

    fun visible(s: Int, i: Int) = QUESTIONNAIRES[s].questions[i].visibleIf?.invoke(answers[s]) ?: true

    /** Posisi butir terlihat berikutnya/sebelumnya; null bila sudah di ujung. */
    fun nextPos(s: Int, i: Int): Pair<Int, Int>? {
        var ss = s; var ii = i + 1
        while (ss < QUESTIONNAIRES.size) {
            while (ii < QUESTIONNAIRES[ss].questions.size) { if (visible(ss, ii)) return ss to ii; ii++ }
            ss++; ii = 0
        }
        return null
    }
    fun prevPos(s: Int, i: Int): Pair<Int, Int>? {
        var ss = s; var ii = i - 1
        while (ss >= 0) {
            while (ii >= 0) { if (visible(ss, ii)) return ss to ii; ii-- }
            ss--; if (ss >= 0) ii = QUESTIONNAIRES[ss].questions.lastIndex
        }
        return null
    }

    fun back() {
        val p = prevPos(section, index)
        if (p == null) confirmExit = true else { section = p.first; index = p.second }
    }
    BackHandler { back() }

    fun finish() {
        // Buang jawaban butir bersyarat yang kini tersembunyi (mis. jawaban awal diubah)
        QUESTIONNAIRES.forEachIndexed { s, qq -> qq.questions.indices.forEach { i -> if (!visible(s, i)) answers[s][i] = null } }
        val r = scoreScreening(answers.map { it.toList() })
        SeivSync.addScreening(r)
        nav.navigate("${Routes.SCREENING_RESULT}/${r.id}") { popUpTo(Routes.SCREENING) }
    }

    LaunchedEffect(pendingAdvance) {
        if (pendingAdvance == 0) return@LaunchedEffect
        delay(260)
        val n = nextPos(section, index)
        if (n == null) finish() else { section = n.first; index = n.second }
    }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackCircle({ back() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    QUESTIONNAIRES.forEachIndexed { i, qq ->
                        val progress = when {
                            i < section -> 1f
                            i == section -> ((index + 1f) / qq.mainCount).coerceAtMost(1f)
                            else -> 0f
                        }
                        Box(Modifier.weight(qq.mainCount.toFloat()).height(5.dp).clip(RoundedCornerShape(50)).background(Seiv.Border)) {
                            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(50)).background(sectionColor(qq.section)))
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (index < q.mainCount) "${index + 1}/${q.mainCount}" else "+",
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color,
            )
            Spacer(Modifier.width(10.dp))
            // Keluar langsung ke halaman Skrining tanpa mundur satu per satu
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(Color.White).clickable { confirmExit = true },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Close, "Keluar dari skrining", tint = Seiv.TextSecondary, modifier = Modifier.size(18.dp)) }
        }

        AnimatedContent(
            targetState = section to index,
            transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "question",
        ) { (s, i) ->
            val qq = QUESTIONNAIRES[s]
            val question = qq.questions[i]
            val c = sectionColor(qq.section)
            val sf = sectionSoft(qq.section)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Tag("BAGIAN ${qq.section} · ${qq.code}", c, sf)
                if (i == 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(qq.instruction, fontSize = 12.sp, color = Seiv.TextSecondary, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tag(if (i < qq.mainCount) "Pertanyaan ${i + 1}" else "Pertanyaan lanjutan", c, sf)
                    Spacer(Modifier.weight(1f))
                    Text(qq.period, fontSize = 10.sp, color = Seiv.TextHint)
                }
                Spacer(Modifier.height(16.dp))
                Text(question.text, fontWeight = FontWeight.Bold, fontSize = if (question.text.length > 110) 16.sp else 19.sp, color = Seiv.Text, lineHeight = 25.sp)
                Spacer(Modifier.height(22.dp))
                val current = answers[s][i]
                val opts = qq.optionsFor(question)
                if (qq.type == QuestionType.YES_NO && question.options == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        opts.forEach { (v, label) ->
                            val sel = current == v
                            Column(
                                Modifier.weight(1f).shadow(if (sel) 4.dp else 1.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))
                                    .background(if (sel) sf else Color.White).border(1.5.dp, if (sel) c else Color.Transparent, RoundedCornerShape(16.dp))
                                    .clickable { answers[s][i] = v; pendingAdvance++ }.padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(if (v == 1) "👍" else "👎", fontSize = 26.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (sel) c else Seiv.Text)
                            }
                        }
                    }
                } else {
                    val showScore = question.scored && question.options == null
                    opts.forEach { (v, label) ->
                        val sel = current == v
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 10.dp).shadow(if (sel) 3.dp else 1.dp, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp)).background(if (sel) sf else Color.White)
                                .border(1.5.dp, if (sel) c else Color.Transparent, RoundedCornerShape(14.dp))
                                .clickable { answers[s][i] = v; pendingAdvance++ }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(if (sel) c else sf), contentAlignment = Alignment.Center) {
                                Text(if (showScore) "${v + qq.scoreOffset}" else "${'A' + v}", color = if (sel) Color.White else c, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = 13.sp, color = Seiv.Text, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                if (question.optional) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Lewati pertanyaan ini ›", fontSize = 13.sp, color = Seiv.TextSecondary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(8.dp))
                            .clickable { answers[s][i] = null; pendingAdvance++ }.padding(8.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (q.code == ASQ.code) {
            Text(
                "Jika kamu merasa tidak aman saat ini, hubungi SEJIWA 119 ext. 8 atau 112.",
                fontSize = 11.sp, color = Seiv.Red, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { nav.navigate(Routes.REFERRAL) }.padding(16.dp),
            )
        } else {
            Text(
                "Jawablah sesuai yang kamu rasakan selama ${q.period}.",
                fontSize = 10.sp, color = Seiv.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Keluar dari skrining?") },
            text = { Text("Jawaban yang sudah kamu isi tidak akan disimpan.", fontSize = 13.sp) },
            confirmButton = { TextButton({ confirmExit = false; nav.goTab(Routes.SCREENING) }) { Text("Keluar", color = Seiv.Red) } },
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
                    .clickable { nav.goTab(Routes.SCREENING) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Refresh, "Ulangi", tint = Seiv.Text, modifier = Modifier.size(18.dp)) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (level == 3) {
                CrisisCard(acute = r.asqAcute == true) { nav.navigate(Routes.REFERRAL) }
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
                    ScoreBox("${r.phq}", "PHQ-9", phqSeverity(r.phq).label, Modifier.weight(1f))
                    ScoreBox("${r.gad}", "GAD-7", gadSeverity(r.gad).label, Modifier.weight(1f))
                }
                if (r.isV2) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoreBox("${r.asq}", "ASQ", asqSeverity(r).label, Modifier.weight(1f))
                        ScoreBox("%.2f".format(capeMean(r.cape!!)), "CAPE-15", capeSeverity(r.cape).label, Modifier.weight(1f))
                        ScoreBox("${r.cdrisc}", "CD-RISC", "Resiliensi ${cdriscSeverity(r.cdrisc!!).label.lowercase()}", Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Detail Per Kuesioner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
            Spacer(Modifier.height(10.dp))
            DetailCard(SRQ20, r.srq, 20, srqSeverity(r.srq), if (r.srq >= 6) "Ada indikasi gangguan mental emosional" else "Tidak ada indikasi gangguan", srqDescription(r.srq))
            DetailCard(
                PHQ9, r.phq, 27, phqSeverity(r.phq),
                "Gejala depresi ${phqSeverity(r.phq).label.lowercase()}" + if (r.phqItem9 > 0) " · butir 9 = ${r.phqItem9}" else "",
                phqDescription(r.phq, r.phqItem9),
            )
            DetailCard(GAD7, r.gad, 21, gadSeverity(r.gad), "Kecemasan ${gadSeverity(r.gad).label.lowercase()}", gadDescription(r.gad))
            if (r.isV2) {
                val asqSev = asqSeverity(r)
                DetailCard(ASQ, r.asq!!, 4, asqSev, "Skrining ${asqSev.label.lowercase()}", asqDescription(r))
                val capeSev = capeSeverity(r.cape!!)
                val domains = if (r.capePi != null && r.capeBe != null && r.capePa != null)
                    "\n\nPer domain (rata-rata): ide paranoid ${"%.2f".format(r.capePi / 5.0)} · pengalaman aneh ${"%.2f".format(r.capeBe / 7.0)} · anomali persepsi ${"%.2f".format(r.capePa / 3.0)}"
                else ""
                DetailCard(
                    CAPE15, r.cape, 75, capeSev,
                    "Rata-rata ${"%.2f".format(capeMean(r.cape))} · cut-off ${"%.2f".format(CAPE_CUTOFF)}", capeDescription(r.cape) + domains,
                )
                DetailCard(CDRISC10, r.cdrisc!!, 40, cdriscSeverity(r.cdrisc), "Resiliensi ${cdriscSeverity(r.cdrisc).label.lowercase()}", cdriscDescription(r.cdrisc))
            } else {
                Text(
                    "Skrining ini memakai versi lama (3 instrumen). Ulangi skrining untuk mendapatkan hasil lengkap 6 instrumen.",
                    fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 16.sp, modifier = Modifier.padding(bottom = 12.dp),
                )
            }

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
            NextStep(Icons.AutoMirrored.Outlined.MenuBook, Blue, BlueSoft, "Jurnal Refleksi", "Kenali pola pikiran dan perasaanmu") { nav.navigate(Routes.JOURNAL_EDIT) }
            if (level < 3) {
                NextStep(Icons.Outlined.Emergency, Seiv.Red, Seiv.RedSoft, "Bantuan Profesional & Darurat", "Daftar layanan yang bisa kamu hubungi") { nav.navigate(Routes.REFERRAL) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("Kembali ke Beranda", icon = Icons.Outlined.Home, container = Seiv.PrimaryDark, onClick = { nav.goTab(Routes.HOME) })
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Ulangi Skrining", icon = Icons.Outlined.Refresh, onClick = { nav.goTab(Routes.SCREENING) })
        }
    }
}

@Composable
private fun ScoreBox(score: String, label: String, severity: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(score, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            severity, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 11.sp,
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
            Column(Modifier.weight(1f)) {
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
fun CrisisCard(acute: Boolean = true, onClick: () -> Unit) {
    SeivCard(Modifier.fillMaxWidth(), color = Seiv.RedSoft, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🆘", fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Kamu tidak sendirian", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Seiv.Red)
                Text(
                    if (acute) "Jawabanmu menunjukkan pikiran untuk mengakhiri hidup saat ini. Keselamatanmu yang utama — hubungi SEJIWA 119 ext. 8 atau 112 sekarang, atau ketuk untuk melihat bantuan."
                    else "Jawabanmu menunjukkan adanya potensi risiko menyakiti diri. Segera bicarakan dengan konselor atau tenaga profesional. Bila merasa tidak aman, hubungi SEJIWA 119 ext. 8 atau 112.",
                    fontSize = 11.sp, color = Seiv.Text, lineHeight = 16.sp,
                )
            }
        }
    }
}
