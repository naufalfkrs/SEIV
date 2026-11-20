package com.seiv.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seiv.app.data.SeivStore
import com.seiv.app.data.remote.GoogleAuth
import com.seiv.app.data.remote.SeivSync
import kotlinx.coroutines.launch
import com.seiv.app.ui.components.BackCircle
import com.seiv.app.ui.components.PrimaryButton
import com.seiv.app.ui.theme.Seiv
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Splash
// ---------------------------------------------------------------------------

@Composable
fun SeivLogo(fontSize: Int = 44) {
    Text(
        "SEIV",
        style = MaterialTheme.typography.headlineLarge.copy(
            brush = Brush.linearGradient(listOf(Seiv.PrimaryDark, Seiv.Mint)),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (fontSize / 4).sp,
        ),
    )
}

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(900))
        delay(900)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Seiv.Cream)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Seiv.Primary, radius = size.width * 0.17f, center = Offset(size.width * 1.0f, size.height * 0.27f))
            drawCircle(Seiv.Mint, radius = size.width * 0.15f, center = Offset(size.width * 1.04f, size.height * 0.30f))
            drawCircle(Seiv.Primary, radius = size.width * 0.15f, center = Offset(size.width * 0.02f, size.height * 0.70f))
            drawCircle(Seiv.Mint, radius = size.width * 0.14f, center = Offset(-size.width * 0.03f, size.height * 0.665f))
        }
        Box(Modifier.align(Alignment.Center).alpha(alpha.value)) { SeivLogo(52) }
        Text(
            "\"A safe space for you.\"",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp).alpha(alpha.value),
            color = Seiv.PrimaryDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ---------------------------------------------------------------------------
// Onboarding
// ---------------------------------------------------------------------------

private data class OnboardPage(val emoji: String, val badge: String, val title: String, val highlight: String, val body: String)

private val ONBOARD_PAGES = listOf(
    OnboardPage("🧘‍♀️", "👋 Halo!", "Selamat datang ", "di SEIV", "Ruang aman untuk memahami dan menjaga kesehatan mentalmu."),
    OnboardPage("📝", "🌤️ Check-in", "Kenali perasaanmu ", "setiap hari", "Catat mood harianmu secara ringan dan dapatkan insight kecil yang membantumu lebih sadar diri."),
    OnboardPage("🧠", "🔍 Skrining", "Pahami kondisimu ", "lebih dalam", "Skrining dengan instrumen klinis tervalidasi (SRQ-20, GAD-7, PHQ-9) lengkap dengan rekomendasi langkah selanjutnya."),
    OnboardPage("🤝", "💬 Dukungan", "Kamu tidak ", "sendirian", "Bercerita di Ruang Cerita bersama AI Companion, menulis jurnal, hingga terhubung dengan konselor profesional."),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val last = page == ONBOARD_PAGES.lastIndex
    Column(Modifier.fillMaxSize().background(Seiv.Cream)) {
        // Area ilustrasi
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFDDEAE4), Color(0xFFE9F1EC)))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFFB8D3C9), radius = size.width * 0.09f, center = Offset(size.width * 0.17f, size.height * 0.28f))
                drawCircle(Color(0xFFC8DDD5), radius = size.width * 0.05f, center = Offset(size.width * 0.88f, size.height * 0.82f))
                drawCircle(Color(0xFFCFE3DB), radius = size.width * 0.33f, center = Offset(size.width * 0.5f, size.height * 0.58f))
            }
            Text(
                "Lewati",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.6f))
                    .clickable { onFinish() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 12.sp, color = Seiv.PrimaryDark, fontWeight = FontWeight.Medium,
            )
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (fadeIn(tween(350, delayMillis = 90)) + scaleIn(tween(350), initialScale = 0.9f))
                        .togetherWith(fadeOut(tween(150)))
                        .using(SizeTransform(clip = false))
                },
                modifier = Modifier.align(Alignment.Center).padding(top = 40.dp),
                label = "illus",
            ) { p -> Text(ONBOARD_PAGES[p].emoji, fontSize = 120.sp) }
        }

        AnimatedContent(
            targetState = page,
            // clip = false: badge yang menonjol ke atas (offset negatif) tidak terpotong saat transisi.
            transitionSpec = {
                (fadeIn(tween(350, delayMillis = 90)) + slideInHorizontally(tween(350)) { it / 8 })
                    .togetherWith(fadeOut(tween(150)))
                    .using(SizeTransform(clip = false))
            },
            modifier = Modifier.weight(0.9f),
            label = "text",
        ) { p ->
            val data = ONBOARD_PAGES[p]
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 0.dp)) {
                Box(
                    Modifier.offset(y = (-16).dp).clip(RoundedCornerShape(12.dp)).background(Seiv.Primary)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) { Text(data.badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                Text(
                    buildAnnotatedString {
                        append(data.title)
                        withStyle(SpanStyle(color = Seiv.Primary)) { append(data.highlight) }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(10.dp))
                Text(data.body, style = MaterialTheme.typography.bodyLarge.copy(color = Seiv.TextSecondary, fontSize = 14.sp))
            }
        }

        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ONBOARD_PAGES.indices.forEach { i ->
                    val w by animateDpAsState(if (i == page) 20.dp else 6.dp, label = "dot")
                    Box(
                        Modifier.padding(horizontal = 3.dp).height(6.dp).width(w).clip(RoundedCornerShape(50))
                            .background(if (i == page) Seiv.Primary else Seiv.Border),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                if (last) "Mulai Sekarang" else "Lanjut",
                onClick = { if (last) onFinish() else page++ },
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Login / Daftar
// ---------------------------------------------------------------------------

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingLabel: String? = null,
    onTrailing: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold, color = Seiv.TextSecondary)
            Spacer(Modifier.weight(1f))
            if (trailingLabel != null) Text(trailingLabel, fontSize = 11.sp, color = Color(0xFF4F6EA8), modifier = Modifier.clickable(onClick = onTrailing))
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Seiv.TextHint) },
            leadingIcon = { Icon(icon, null, tint = Seiv.TextHint, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (password) {
                {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null,
                        tint = Seiv.TextHint, modifier = Modifier.size(18.dp).clickable { visible = !visible },
                    )
                }
            } else null,
            singleLine = true,
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFE3E8F6),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Seiv.Primary,
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
        )
    }
}

@Composable
private fun AuthBackground(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE4E9F7), Color(0xFFEFF2FA), Color(0xFFE6EBF7))))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun SerifTitle(first: String, second: String) {
    Text(first, fontFamily = FontFamily.Serif, fontSize = 30.sp, color = Seiv.Text, textAlign = TextAlign.Center)
    Text(second, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 30.sp, color = Seiv.Text, textAlign = TextAlign.Center)
}

@Composable
private fun LogoBox() {
    Box(
        Modifier.size(64.dp).shadow(3.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(Color(0xFFF5F2EC)),
        contentAlignment = Alignment.Center,
    ) { SeivLogo(15) }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(2.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F4FB))
            .border(1.dp, Color(0xFFD8DEEE), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("G") }
            },
            fontWeight = FontWeight.Bold, fontSize = 18.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text("Masuk dengan Google", fontSize = 14.sp, color = Seiv.Text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PrivacyFooter() {
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Shield, null, tint = Seiv.TextHint, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("YOUR PRIVACY IS OUR PRIORITY", fontSize = 9.sp, letterSpacing = 1.sp, color = Seiv.TextHint)
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Dengan masuk, kamu menyetujui Ketentuan Layanan dan Kebijakan Privasi kami.",
        fontSize = 10.sp, color = Seiv.TextHint, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
}

private fun nameFromEmail(email: String): String =
    email.substringBefore("@").split('.', '_', '-').filter { it.isNotBlank() }
        .joinToString(" ") { s -> s.replaceFirstChar { it.uppercase() } }.ifBlank { "Sahabat" }


private fun isEmail(s: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(s.trim()).matches()

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onRegister: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun run(block: suspend () -> Unit) {
        loading = true; error = null
        scope.launch {
            try { block() } catch (e: Exception) { error = e.message ?: "Terjadi kesalahan." } finally { loading = false }
        }
    }

    AuthBackground {
        Spacer(Modifier.height(40.dp))
        LogoBox()
        Spacer(Modifier.height(24.dp))
        SerifTitle("Selamat datang", "di SEIV")
        Spacer(Modifier.height(8.dp))
        Text("Ruang aman untukmu bercerita.", fontSize = 13.sp, color = Seiv.TextSecondary)
        Spacer(Modifier.height(28.dp))
        GoogleButton {
            when {
                loading -> Unit
                GoogleAuth.isConfigured -> run {
                    val token = GoogleAuth.requestIdToken(ctx) ?: return@run
                    SeivSync.signInWithGoogle(token)
                    onLoggedIn()
                }
                SeivSync.online -> error = "Login Google belum diaktifkan. Gunakan email untuk sementara."
                else -> { SeivStore.login("Sahabat SEIV", "pengguna@gmail.com"); onLoggedIn() } // mode offline/demo
            }
        }
        Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD5DBEA)))
            Text("ATAU", fontSize = 10.sp, color = Seiv.TextHint, modifier = Modifier.padding(horizontal = 12.dp), letterSpacing = 1.sp)
            Box(Modifier.weight(1f).height(1.dp).background(Color(0xFFD5DBEA)))
        }
        LabeledField("Email", email, { email = it; error = null }, "halo@kamu.com", Icons.Outlined.Email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        LabeledField(
            "Kata sandi", pass, { pass = it; error = null }, "••••••••", Icons.Outlined.Lock, password = true,
            trailingLabel = "Lupa?", onTrailing = { Toast.makeText(ctx, "Hubungi admin SEIV untuk mengatur ulang kata sandi.", Toast.LENGTH_SHORT).show() },
        )
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = Seiv.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(if (loading) "Memproses..." else "Masuk", enabled = !loading, onClick = {
            when {
                !isEmail(email) -> error = "Masukkan email yang valid."
                pass.length < 6 -> error = "Kata sandi minimal 6 karakter."
                SeivSync.online -> run { SeivSync.signIn(email.trim(), pass); onLoggedIn() }
                else -> {
                    val existing = SeivStore.user
                    val name = if (existing != null && existing.email == email.trim()) existing.name else nameFromEmail(email.trim())
                    SeivStore.login(name, email.trim()); onLoggedIn()
                }
            }
        })
        Spacer(Modifier.height(18.dp))
        Row {
            Text("Belum punya akun? ", fontSize = 12.sp, color = Seiv.TextSecondary)
            Text("Daftar sekarang", fontSize = 12.sp, color = Seiv.Text, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onRegister))
        }
        if (!SeivSync.online) {
            Spacer(Modifier.height(8.dp))
            Text("Mode offline · data tersimpan di perangkat ini", fontSize = 10.sp, color = Seiv.TextHint)
        }
        PrivacyFooter()
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onRegistered: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    AuthBackground {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) { BackCircle(onBack) }
        Spacer(Modifier.height(8.dp))
        LogoBox()
        Spacer(Modifier.height(20.dp))
        SerifTitle("Buat akun", "SEIV-mu")
        Spacer(Modifier.height(8.dp))
        Text("Mulai perjalanan mengenal dirimu.", fontSize = 13.sp, color = Seiv.TextSecondary)
        Spacer(Modifier.height(24.dp))
        LabeledField("Nama panggilan", name, { name = it; error = null }, "Aulia", Icons.Outlined.Person)
        Spacer(Modifier.height(14.dp))
        LabeledField("Email", email, { email = it; error = null }, "halo@kamu.com", Icons.Outlined.Email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        LabeledField("Kata sandi", pass, { pass = it; error = null }, "Minimal 6 karakter", Icons.Outlined.Lock, password = true)
        Spacer(Modifier.height(14.dp))
        LabeledField("Ulangi kata sandi", confirm, { confirm = it; error = null }, "••••••••", Icons.Outlined.Lock, password = true)
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = Seiv.Red, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        if (info != null) {
            Spacer(Modifier.height(8.dp))
            Text(info!!, color = Seiv.PrimaryDark, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(if (loading) "Memproses..." else "Daftar", enabled = !loading, onClick = {
            info = null
            when {
                name.isBlank() -> error = "Nama tidak boleh kosong."
                !isEmail(email) -> error = "Masukkan email yang valid."
                pass.length < 6 -> error = "Kata sandi minimal 6 karakter."
                pass != confirm -> error = "Kata sandi tidak sama."
                SeivSync.online -> {
                    loading = true; error = null
                    scope.launch {
                        try {
                            if (SeivSync.signUp(name.trim(), email.trim(), pass)) onRegistered()
                            else info = "Akun dibuat! Cek emailmu untuk verifikasi, lalu masuk."
                        } catch (e: Exception) {
                            error = e.message ?: "Terjadi kesalahan."
                        } finally { loading = false }
                    }
                }
                else -> { SeivStore.login(name.trim(), email.trim()); onRegistered() }
            }
        })
        Spacer(Modifier.height(18.dp))
        Row {
            Text("Sudah punya akun? ", fontSize = 12.sp, color = Seiv.TextSecondary)
            Text("Masuk", fontSize = 12.sp, color = Seiv.Text, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onBack))
        }
        PrivacyFooter()
    }
}
