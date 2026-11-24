package com.seiv.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.seiv.app.data.SessionMessage
import com.seiv.app.data.SeivStore
import com.seiv.app.data.SessionStatus
import com.seiv.app.data.counselorById
import com.seiv.app.data.isSameDay
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.components.*
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ruang chat sesi konseling (metode "Chat"). Mahasiswa menulis dari aplikasi,
 * konselor membalas dari panel web. Pesan baru diambil berkala selama layar terbuka;
 * balasan konselor juga dikirim sebagai push notification.
 */
@Composable
fun ChatScreen(nav: NavHostController, sessionId: String) {
    val s = SeivStore.sessions.firstOrNull { it.id == sessionId }
    if (s == null) {
        LaunchedEffect(Unit) { SeivSync.refreshInBackground() }
        Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding()) {
            ScreenHeader("Ruang Chat", onBack = { nav.popBackStack() })
            Text("Memuat sesi…", color = Seiv.TextSecondary, modifier = Modifier.padding(20.dp))
        }
        return
    }
    val c = counselorById(s.counselorId)
    val messages = remember { mutableStateListOf<SessionMessage>() }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val open = s.status == SessionStatus.CONFIRMED

    suspend fun reload() {
        runCatching { SeivSync.loadChat(sessionId) }
            .onSuccess { list ->
                if (list.map { it.id } != messages.map { it.id }) { messages.clear(); messages.addAll(list) }
                error = null
            }
            .onFailure { if (messages.isEmpty()) error = it.message ?: "Gagal memuat pesan." }
        loaded = true
    }

    // Ambil pesan baru tiap 3 detik selama layar terbuka
    LaunchedEffect(sessionId) {
        while (true) {
            reload()
            delay(3_000)
        }
    }
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    fun send() {
        val body = text.trim()
        if (body.isEmpty() || sending) return
        sending = true
        scope.launch {
            runCatching { SeivSync.sendChat(sessionId, body) }
                .onSuccess { m -> text = ""; if (m != null && messages.none { it.id == m.id }) messages.add(m) }
                .onFailure { error = it.message ?: "Pesan gagal terkirim. Coba lagi." }
            sending = false
        }
    }

    Column(Modifier.fillMaxSize().background(Seiv.Background).statusBarsPadding().navigationBarsPadding().imePadding()) {
        ScreenHeader(c.name, onBack = { nav.popBackStack() }, subtitle = "${c.role} · ${SessionStatus.label(s.status)}") {
            Avatar(c.initials, c.colorIndex, 36.dp)
        }
        Text(
            "🔒 Percakapan ini hanya bisa dibaca olehmu dan konselormu.",
            fontSize = 11.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(Seiv.MintSoft).padding(8.dp),
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(), state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (loaded && messages.isEmpty() && error == null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (open) "Mulai ceritakan apa yang kamu rasakan. ${c.name} akan membalas di sini."
                            else "Belum ada percakapan.",
                            fontSize = 13.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { m ->
                val idx = messages.indexOf(m)
                val showDay = idx == 0 || !isSameDay(messages[idx - 1].createdAt, m.createdAt)
                if (showDay) {
                    Text(
                        formatDate(m.createdAt, "EEEE, d MMMM"), fontSize = 10.sp, color = Seiv.TextHint, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    )
                }
                ChatBubble(m)
            }
        }
        error?.let { Text(it, fontSize = 11.sp, color = Seiv.Red, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        if (open) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text, onValueChange = { if (it.length <= 2000) text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tulis pesan…", fontSize = 13.sp, color = Seiv.TextHint) },
                    maxLines = 4, shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Seiv.Background, focusedContainerColor = Seiv.Background,
                        unfocusedBorderColor = Color.Transparent, focusedBorderColor = Seiv.Primary,
                    ),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(if (text.isBlank() || sending) Seiv.MintLight else Seiv.Primary)
                        .clickable(enabled = text.isNotBlank() && !sending) { send() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Kirim", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        } else {
            Text(
                when (s.status) {
                    SessionStatus.WAITING -> "Ruang chat terbuka setelah pembayaranmu diverifikasi admin."
                    SessionStatus.COMPLETED -> "Sesi sudah selesai. Percakapan ini tersimpan untukmu."
                    else -> "Sesi dibatalkan."
                },
                fontSize = 12.sp, color = Seiv.TextSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            )
        }
    }
}

@Composable
private fun ChatBubble(m: SessionMessage) {
    val mine = !m.fromCounselor
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 290.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp))
                .background(if (mine) Seiv.Primary else Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        ) {
            Text(m.body, fontSize = 14.sp, color = if (mine) Color.White else Seiv.Text, lineHeight = 19.sp)
            Text(
                formatDate(m.createdAt, "HH:mm"), fontSize = 9.sp,
                color = if (mine) Color.White.copy(alpha = 0.7f) else Seiv.TextHint, fontWeight = FontWeight.Medium,
            )
        }
    }
}
