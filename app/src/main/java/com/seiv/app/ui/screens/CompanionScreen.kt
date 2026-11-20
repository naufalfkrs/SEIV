package com.seiv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.ChatMessage
import com.seiv.app.data.CompanionEngine
import com.seiv.app.ui.Routes
import com.seiv.app.ui.goTab
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Riwayat chat hanya disimpan di memori (tidak ditulis ke penyimpanan) demi privasi. */
object CompanionSession {
    val messages = mutableStateListOf<ChatMessage>()
    var userTurns = 0
    fun ensureGreeting(name: String) {
        if (messages.isEmpty()) messages.addAll(CompanionEngine.greeting(name))
    }
    fun reset(name: String) {
        messages.clear(); userTurns = 0; ensureGreeting(name)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreen(nav: NavHostController) {
    val name = firstName()
    LaunchedEffect(Unit) { CompanionSession.ensureGreeting(name) }
    val messages = CompanionSession.messages
    var input by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || typing) return
        messages.add(ChatMessage(true, t))
        input = ""
        typing = true
        val turn = CompanionSession.userTurns++
        scope.launch {
            delay(900L + (t.length * 8L).coerceAtMost(900L))
            messages.add(CompanionEngine.reply(t, turn))
            typing = false
        }
    }

    LaunchedEffect(messages.size, typing) {
        val count = messages.size + (if (typing) 1 else 0) + 1
        listState.animateScrollToItem(count - 1)
    }

    Column(Modifier.fillMaxSize().background(Color(0xFFF1F3EE))) {
        // Header
        Row(
            Modifier.fillMaxWidth().background(Seiv.PrimaryDark).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).clickable { nav.popBackStack() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = Color.White, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Box {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.align(Alignment.BottomEnd).size(11.dp).clip(CircleShape).background(Color(0xFF5CD68A)).border(2.dp, Seiv.PrimaryDark, CircleShape))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("AI Companion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(if (typing) "Sedang mengetik..." else "● Online · Siap menemanimu", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
            }
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f))
                    .clickable { CompanionSession.reset(name) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Refresh, "Mulai ulang", tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
        // Peringatan
        Text(
            buildAnnotatedString {
                append("⚠️ AI ini bukan pengganti profesional — untuk bantuan klinis, gunakan fitur ")
                withStyle(SpanStyle(color = Seiv.Orange, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) { append("Konseling") }
            },
            fontSize = 11.sp, color = Seiv.Text.copy(alpha = 0.75f), lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth().background(Seiv.Warning).clickable { nav.goTab(Routes.COUNSELING) }.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { m -> Bubble(m, onHelp = { nav.navigate(Routes.REFERRAL) }) }
            if (typing) item { TypingBubble() }
            item {
                if (CompanionSession.userTurns == 0 && !typing) {
                    Column(Modifier.padding(start = 30.dp, top = 4.dp)) {
                        Text("Mulai dari sini", fontSize = 10.sp, color = Seiv.TextHint)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompanionEngine.quickStarters.forEach { q ->
                                Text(
                                    q, fontSize = 12.sp, color = Seiv.Text,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)
                                        .border(1.dp, Seiv.Border, RoundedCornerShape(12.dp))
                                        .clickable { send(q) }.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                } else Spacer(Modifier.height(1.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ceritakan sesuatu...", fontSize = 13.sp, color = Seiv.TextHint) },
                shape = RoundedCornerShape(22.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F3EE), unfocusedContainerColor = Color(0xFFF1F3EE),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Seiv.Text),
            )
            Spacer(Modifier.width(8.dp))
            val canSend = input.isNotBlank() && !typing
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(if (canSend) Seiv.Primary else Seiv.MintLight)
                    .clickable(enabled = canSend) { send(input) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = Color.White, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, onHelp: () -> Unit) {
    val time = formatDate(m.timestamp, "HH:mm")
    if (m.fromUser) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Text(
                m.text, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.widthIn(max = 270.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(Seiv.Primary).padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Text(time, fontSize = 9.sp, color = Seiv.TextHint, modifier = Modifier.padding(top = 3.dp, end = 4.dp))
        }
    } else {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.padding(bottom = 16.dp).size(22.dp).clip(CircleShape).background(Seiv.PrimaryDark), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Column(
                    Modifier.widthIn(max = 270.dp).shadow(1.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                        .background(if (m.isCrisis) Seiv.RedSoft else Color.White).padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(m.text, color = Seiv.Text, fontSize = 13.sp, lineHeight = 19.sp)
                    if (m.isCrisis) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lihat bantuan darurat →", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Seiv.Red).clickable(onClick = onHelp)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
                Text(time, fontSize = 9.sp, color = Seiv.TextHint, modifier = Modifier.padding(top = 3.dp, start = 4.dp))
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    var dots by remember { mutableIntStateOf(1) }
    LaunchedEffect(Unit) { while (true) { delay(350); dots = dots % 3 + 1 } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(30.dp))
        Text(
            "●".repeat(dots).padEnd(3, '○'), color = Seiv.Primary, fontSize = 10.sp, letterSpacing = 3.sp,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
