package com.seiv.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.CheckIn
import com.seiv.app.data.FACTORS
import com.seiv.app.data.FEELINGS
import com.seiv.app.data.MOODS
import com.seiv.app.data.SeivStore
import com.seiv.app.data.buildInsight
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.Routes
import com.seiv.app.ui.components.BackCircle
import com.seiv.app.ui.components.Chip
import com.seiv.app.ui.components.IconBadge
import com.seiv.app.ui.components.PrimaryButton
import com.seiv.app.ui.components.ScreenHeader
import com.seiv.app.ui.components.SectionTitle
import com.seiv.app.ui.components.SeivCard
import com.seiv.app.ui.goTab
import com.seiv.app.ui.theme.Seiv
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ID = Locale.forLanguageTag("id-ID")
fun formatDate(ts: Long, pattern: String) = SimpleDateFormat(pattern, ID).format(Date(ts))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInScreen(nav: NavHostController, initialMood: Int?) {
    var mood by remember { mutableStateOf(initialMood) }
    val factors = remember { mutableStateListOf<String>() }
    val feelings = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackCircle({ nav.popBackStack() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(formatDate(System.currentTimeMillis(), "EEEE, d MMMM").uppercase(), fontSize = 10.sp, color = Seiv.TextSecondary, letterSpacing = 1.sp)
                Text("Check-in Harian", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "🔥 ${SeivStore.streak()} hari", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Seiv.Orange,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Seiv.OrangeSoft).padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(6.dp))
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    append("Bagaimana perasaanmu ")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Seiv.Primary)); append("hari ini?"); pop()
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOODS.forEachIndexed { i, (emoji, label) ->
                    val selected = mood == i
                    val s by animateFloatAsState(if (selected) 1.06f else 1f, label = "mood")
                    Column(
                        Modifier
                            .weight(1f)
                            .scale(s)
                            .shadow(if (selected) 4.dp else 1.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Color(0xFFFBE3E0).takeIf { i <= 1 } ?: Seiv.MintLight else Color.White)
                            .border(1.dp, if (selected) Seiv.Primary.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { mood = i }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 8.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center, lineHeight = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("Apa yang mempengaruhi harimu?", hint = "(opsional)")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FACTORS.forEach { (icon, label) ->
                    Chip(label, label in factors, { if (label in factors) factors.remove(label) else factors.add(label) }, leading = icon)
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("Lebih tepatnya kamu merasa...", hint = "(opsional)")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FEELINGS.forEach { f ->
                    Chip(f, f in feelings, { if (f in feelings) feelings.remove(f) else feelings.add(f) })
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle("Ceritakan sedikit", hint = "(opsional)")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                placeholder = { Text("Tulis apa yang ada di pikiranmu hari ini...", fontSize = 13.sp, color = Seiv.TextHint) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                    unfocusedBorderColor = Seiv.Border, focusedBorderColor = Seiv.Primary,
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
            )
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            "Simpan Check-in",
            enabled = mood != null,
            onClick = {
                val c = CheckIn(mood = mood!!, factors = factors.toList(), feelings = feelings.toList(), note = note.trim())
                SeivSync.addCheckIn(c)
                nav.navigate("${Routes.INSIGHT}/${c.id}") { popUpTo(Routes.HOME) }
            },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightScreen(nav: NavHostController, id: String) {
    val checkIn = SeivStore.checkIns.firstOrNull { it.id == id } ?: SeivStore.checkIns.firstOrNull()
    if (checkIn == null) { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    val insight = remember(checkIn) { buildInsight(checkIn, SeivStore.checkIns.toList()) }
    val lowMood = checkIn.mood <= 1

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Insight Harian", onBack = { nav.popBackStack() }, overline = formatDate(checkIn.timestamp, "EEEE, d MMMM yyyy"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Seiv.PrimaryDark, Seiv.Primary)))
                    .padding(20.dp),
            ) {
                Text(MOODS[checkIn.mood].first, fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text(insight.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text(insight.message, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, lineHeight = 19.sp)
                if (checkIn.feelings.isNotEmpty() || checkIn.factors.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (checkIn.factors + checkIn.feelings).forEach {
                            Text(it, color = Color.White, fontSize = 10.sp,
                                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SeivCard(Modifier.fillMaxWidth(), color = Seiv.YellowSoft) {
                Row {
                    Text("💡", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Saran kecil untukmu", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                        Spacer(Modifier.height(4.dp))
                        Text(insight.tip, fontSize = 12.sp, color = Seiv.Text.copy(alpha = 0.8f), lineHeight = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SectionTitle("Langkah selanjutnya")
            Spacer(Modifier.height(10.dp))
            NextStep(Icons.Outlined.Air, Seiv.Primary, Seiv.MintLight, "Latihan Napas 4-7-8", "Tenangkan tubuh dan pikiran dalam 2 menit") { nav.navigate(Routes.BREATHING) }
            NextStep(Icons.AutoMirrored.Outlined.MenuBook, Seiv.Orange, Seiv.OrangeSoft, "Tulis Jurnal Refleksi", "Uraikan perasaanmu lewat tulisan") { nav.navigate(Routes.JOURNAL_EDIT) }
            NextStep(Icons.Outlined.AutoAwesome, Seiv.Purple, Seiv.PurpleSoft, "Ceritakan ke AI Companion", "Ruang cerita yang empatik dan tidak menghakimi") { nav.navigate(Routes.COMPANION) }
            NextStep(Icons.Outlined.Psychology, Color(0xFF3D7AB8), Color(0xFFE2EEF9), "Ikuti Skrining", "Pahami kondisi mentalmu lebih dalam") { nav.goTab(Routes.SCREENING) }
            if (lowMood) {
                NextStep(Icons.Outlined.SupportAgent, Seiv.Red, Seiv.RedSoft, "Bicara dengan Profesional", "Konselor kami siap mendampingimu") { nav.navigate(Routes.COUNSELORS) }
            }
            Spacer(Modifier.height(12.dp))
        }
        PrimaryButton("Kembali ke Beranda", onClick = { nav.goTab(Routes.HOME) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
    }
}

@Composable
fun NextStep(icon: ImageVector, tint: Color, bg: Color, title: String, desc: String, onClick: () -> Unit) {
    SeivCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), padding = PaddingValues(14.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, bg)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Seiv.Text)
                Text(desc, fontSize = 11.sp, color = Seiv.TextSecondary)
            }
            Text("›", fontSize = 20.sp, color = Seiv.TextHint)
        }
    }
}
