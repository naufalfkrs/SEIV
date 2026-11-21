package com.seiv.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.FACTS
import com.seiv.app.data.MOODS
import com.seiv.app.data.SeivStore
import com.seiv.app.data.careLevel
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.Routes
import com.seiv.app.ui.components.IconBadge
import com.seiv.app.ui.components.SectionTitle
import com.seiv.app.ui.components.SeivCard
import com.seiv.app.ui.goTab
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.delay
import java.util.Calendar

fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 4..10 -> "Selamat pagi ☀️"
        in 11..14 -> "Selamat siang 🌤️"
        in 15..17 -> "Selamat sore 🌇"
        else -> "Selamat malam 🌙"
    }
}

fun firstName(): String = SeivStore.user?.name?.split(" ")?.firstOrNull().orEmpty().ifBlank { "Sahabat" }

@Composable
fun GreetingHeader(nav: NavHostController, showCompanion: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), fontSize = 12.sp, color = Seiv.TextSecondary)
            Text("Hai, ${firstName()}! 👋", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp))
        }
        if (showCompanion) {
            Box(
                Modifier.size(40.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(Seiv.PrimaryDark)
                    .clickable { nav.navigate(Routes.COMPANION) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.AutoAwesome, "AI Companion", tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(10.dp))
        }
        val unread = unreadNotificationCount()
        Box {
            Box(
                Modifier.size(40.dp).shadow(2.dp, CircleShape).clip(CircleShape).background(Color.White)
                    .clickable { nav.navigate(Routes.NOTIFICATIONS) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.NotificationsNone, "Notifikasi", tint = Seiv.Text, modifier = Modifier.size(20.dp)) }
            if (unread > 0) {
                // Jumlah notifikasi yang belum dibaca
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).defaultMinSize(16.dp, 16.dp).clip(CircleShape)
                        .background(Seiv.Red).border(1.5.dp, Color.White, CircleShape).padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (unread > 9) "9+" else "$unread", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavHostController) {
    val today = SeivStore.todayCheckIn()
    val latestScreening = SeivStore.screenings.firstOrNull()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
    ) {
        GreetingHeader(nav)

        // Kartu check-in harian
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Seiv.PrimaryDark, Seiv.Primary, Color(0xFF6E9A8C))))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Seiv.MintLight, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("CHECK-IN HARIAN", color = Seiv.MintLight, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("🔥 ${SeivStore.streak()} hari", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            if (today == null) {
                Text("Bagaimana perasaanmu hari ini?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MOODS.forEachIndexed { i, (emoji, _) ->
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.18f))
                                .clickable { nav.navigate("${Routes.CHECKIN}?mood=$i") },
                            contentAlignment = Alignment.Center,
                        ) { Text(emoji, fontSize = 22.sp) }
                    }
                }
                Spacer(Modifier.height(14.dp))
                WhitePillButton("Mulai Check-in") { nav.navigate(Routes.CHECKIN) }
            } else {
                Text("Kamu sudah check-in hari ini ${MOODS[today.mood].first}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Perasaanmu: ${MOODS[today.mood].second}" + if (today.feelings.isNotEmpty()) " · ${today.feelings.joinToString(", ")}" else "",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                )
                Spacer(Modifier.height(14.dp))
                WhitePillButton("Lihat insight harianmu") { nav.navigate("${Routes.INSIGHT}/${today.id}") }
            }
        }

        // Rekomendasi berdasar skrining terakhir
        if (latestScreening != null && latestScreening.careLevel() >= 2) {
            Spacer(Modifier.height(16.dp))
            SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), color = Seiv.OrangeSoft, onClick = { nav.navigate(Routes.COUNSELORS) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Outlined.SupportAgent, Seiv.Orange, Color.White)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kamu tidak harus menghadapinya sendiri", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
                        Text("Hasil skriningmu menyarankan berbicara dengan konselor.", fontSize = 11.sp, color = Seiv.TextSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Seiv.Orange, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Tahukah kamu
        Spacer(Modifier.height(16.dp))
        var factIndex by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) { delay(5000); factIndex = (factIndex + 1) % FACTS.size }
        }
        SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), color = Color(0xFFEFF5F1)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Lightbulb, Color.White, Seiv.Primary, size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Text("TAHUKAH KAMU?", fontSize = 11.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, color = Seiv.PrimaryDark)
                Spacer(Modifier.weight(1f))
                Text(
                    "Fakta #${factIndex + 1}", fontSize = 10.sp, color = Seiv.TextSecondary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(factIndex, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "fact") { i ->
                Column {
                    Row {
                        Text("🌿", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(FACTS[i].first, fontSize = 13.sp, color = Seiv.Text, lineHeight = 19.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sumber: ", fontSize = 10.sp, color = Seiv.TextHint)
                        Text(FACTS[i].second, fontSize = 10.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        FACTS.indices.forEach { d ->
                            Box(
                                Modifier.padding(horizontal = 1.5.dp).height(4.dp).width(if (d == i) 14.dp else 4.dp)
                                    .clip(RoundedCornerShape(50)).background(if (d == i) Seiv.Primary else Seiv.Border),
                            )
                        }
                    }
                }
            }
        }

        // Rekomendasi
        Spacer(Modifier.height(22.dp))
        SectionTitle("Rekomendasi untukmu", Modifier.padding(horizontal = 20.dp), action = "Lihat semua") { nav.goTab(Routes.ACTIVITY) }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()).height(IntrinsicSize.Max).padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecommendationCard(Icons.Outlined.Air, Seiv.Primary, Seiv.MintLight, "Latihan Napas", "Tenangkan pikiran dengan teknik pernapasan 4-7-8.", "2 menit") { nav.navigate(Routes.BREATHING) }
            RecommendationCard(Icons.AutoMirrored.Outlined.MenuBook, Seiv.Orange, Seiv.OrangeSoft, "Jurnal Singkat", "Tuangkan isi pikiranmu dalam beberapa kalimat.", "5 menit") { nav.navigate(Routes.JOURNAL_EDIT) }
            RecommendationCard(Icons.Outlined.ChatBubbleOutline, Seiv.Purple, Seiv.PurpleSoft, "Ruang Cerita", "Ceritakan perasaanmu pada AI Companion yang empatik.", "Kapan saja") { nav.navigate(Routes.COMPANION) }
            RecommendationCard(Icons.Outlined.Psychology, Color(0xFF3D7AB8), Color(0xFFE2EEF9), "Skrining Mental", "Kenali kondisimu dengan kuesioner klinis.", "±11 menit") { nav.goTab(Routes.SCREENING) }
        }

        // Ruang cerita banner
        Spacer(Modifier.height(22.dp))
        SeivCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), onClick = { nav.navigate(Routes.COMPANION) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(Seiv.PrimaryDark), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Butuh teman bercerita?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Seiv.Text)
                    Text("SEIV AI siap mendengarkan tanpa menghakimi.", fontSize = 12.sp, color = Seiv.TextSecondary)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Seiv.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun WhitePillButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Seiv.PrimaryDark, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text("›", color = Seiv.PrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecommendationCard(icon: ImageVector, tint: Color, bg: Color, title: String, desc: String, duration: String, onClick: () -> Unit) {
    // Tinggi kartu mengikuti isi (Row memakai IntrinsicSize.Max), jadi teks tidak terpotong
    // meskipun ukuran font sistem diperbesar.
    SeivCard(Modifier.width(156.dp).fillMaxHeight(), padding = PaddingValues(14.dp), onClick = onClick) {
        IconBadge(icon, tint, bg, size = 36.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Seiv.Text)
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 11.sp, color = Seiv.TextSecondary, lineHeight = 15.sp)
        Spacer(Modifier.weight(1f).heightIn(min = 10.dp))
        Text(duration, fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
