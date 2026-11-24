package com.seiv.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.seiv.app.data.SeivStore
import com.seiv.app.data.remote.SeivSync
import com.seiv.app.ui.components.RefreshBox
import com.seiv.app.data.remote.SupabaseApi
import com.seiv.app.ui.screens.*
import com.seiv.app.ui.theme.Seiv

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val COUNSELING = "counseling"
    const val SCREENING = "screening"
    const val PROFILE = "profile"
    const val CHECKIN = "checkin"
    const val INSIGHT = "insight"
    const val COMPANION = "companion"
    const val COUNSELORS = "counselors"
    const val SCHEDULE = "schedule"
    const val PAYMENT = "payment"
    const val QRIS = "qris"
    const val PAY_SUCCESS = "pay_success"
    const val SESSION = "session"
    const val SESSIONS = "sessions"
    const val SCREENING_RUN = "screening_run"
    const val SCREENING_RESULT = "screening_result"
    const val JOURNAL_EDIT = "journal_edit"
    const val BREATHING = "breathing"
    const val REFERRAL = "referral"
    const val HISTORY = "history"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE_EDIT = "profile_edit"
}

/** Rute tujuan dari notifikasi yang diketuk; diproses oleh [SeivApp]. */
object PushRouter {
    var pending by androidx.compose.runtime.mutableStateOf<String?>(null)
}

val TAB_ROUTES = listOf(Routes.HOME, Routes.ACTIVITY, Routes.COUNSELING, Routes.SCREENING, Routes.PROFILE)

/**
 * Pindah ke salah satu tab utama dan buang layar-layar sekunder di atasnya.
 * Sengaja tanpa saveState/restoreState: kombinasi itu memulihkan kembali tumpukan
 * layar yang baru dibuang (mis. tombol "Kembali ke Beranda" malah kembali ke Insight).
 */
fun NavHostController.goTab(route: String) {
    if (route == Routes.HOME) {
        // Kembali ke Beranda yang sudah ada di tumpukan; bila tidak ada, buat ulang.
        if (!popBackStack(Routes.HOME, inclusive = false)) resetTo(Routes.HOME)
        return
    }
    navigate(route) {
        popUpTo(Routes.HOME)
        launchSingleTop = true
    }
}

private fun optionalArg(name: String) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun SeivApp() {
    val nav = rememberNavController()
    // Buka layar tujuan dari push notification setelah pengguna sampai di Beranda
    val pendingRoute = PushRouter.pending
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(pendingRoute, currentRoute) {
        val r = pendingRoute ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute == Routes.SPLASH || currentRoute == Routes.LOGIN ||
            currentRoute == Routes.ONBOARDING || currentRoute == Routes.REGISTER
        ) return@LaunchedEffect
        PushRouter.pending = null
        runCatching { nav.navigate(r) }
    }
    NavHost(
        navController = nav,
        startDestination = Routes.SPLASH,
        modifier = Modifier.fillMaxSize().background(Seiv.Background),
        enterTransition = { slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { slideOutHorizontally(tween(240)) { it / 6 } + fadeOut(tween(200)) },
    ) {
        composable(Routes.SPLASH) {
            SplashScreen {
                val next = when {
                    !SeivStore.onboarded -> Routes.ONBOARDING
                    SeivStore.user == null -> Routes.LOGIN
                    // Backend aktif tapi sesi login sudah tidak ada (mis. baru dikonfigurasi)
                    SeivSync.online && !SupabaseApi.hasSession -> Routes.LOGIN
                    else -> Routes.HOME
                }
                nav.resetTo(next)
            }
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen { SeivStore.finishOnboarding(); nav.resetTo(Routes.LOGIN) }
        }
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = { nav.resetTo(Routes.HOME) }, onRegister = { nav.navigate(Routes.REGISTER) })
        }
        composable(Routes.REGISTER) {
            // Setelah daftar, minta pengguna melengkapi data diri
            RegisterScreen(onBack = { nav.popBackStack() }, onRegistered = {
                nav.resetTo(Routes.HOME)
                nav.navigate("${Routes.PROFILE_EDIT}?first=true")
            })
        }

        // --- Tab utama ---
        composable(Routes.HOME) { MainScaffold(nav, Routes.HOME) { HomeScreen(nav) } }
        composable(Routes.ACTIVITY) { MainScaffold(nav, Routes.ACTIVITY) { ActivityScreen(nav) } }
        composable(Routes.COUNSELING) { MainScaffold(nav, Routes.COUNSELING) { CounselingScreen(nav) } }
        composable(Routes.SCREENING) { MainScaffold(nav, Routes.SCREENING) { ScreeningIntroScreen(nav) } }
        composable(Routes.PROFILE) { MainScaffold(nav, Routes.PROFILE) { ProfileScreen(nav) } }

        // --- Alur sekunder ---
        composable("${Routes.CHECKIN}?mood={mood}", arguments = listOf(optionalArg("mood"))) {
            CheckInScreen(nav, it.arguments?.getString("mood")?.toIntOrNull())
        }
        composable("${Routes.INSIGHT}/{id}") { InsightScreen(nav, it.arguments?.getString("id").orEmpty()) }
        composable(Routes.COMPANION) { CompanionScreen(nav) }
        composable("${Routes.COUNSELORS}?role={role}", arguments = listOf(optionalArg("role"))) { CounselorListScreen(nav, it.arguments?.getString("role")) }
        composable(Routes.SESSIONS) { SessionListScreen(nav) }
        composable("${Routes.SCHEDULE}/{id}") { ScheduleScreen(nav, it.arguments?.getString("id").orEmpty()) }
        composable(Routes.PAYMENT) { PaymentScreen(nav) }
        composable(Routes.QRIS) { PaymentDetailScreen(nav) }
        composable("${Routes.PAY_SUCCESS}/{id}") { PaymentSuccessScreen(nav, it.arguments?.getString("id").orEmpty()) }
        composable("${Routes.SESSION}/{id}") { SessionDetailScreen(nav, it.arguments?.getString("id").orEmpty()) }
        composable(Routes.SCREENING_RUN) { ScreeningRunScreen(nav) }
        composable("${Routes.SCREENING_RESULT}/{id}") { ScreeningResultScreen(nav, it.arguments?.getString("id").orEmpty()) }
        composable("${Routes.JOURNAL_EDIT}?prompt={prompt}", arguments = listOf(optionalArg("prompt"))) {
            JournalEditorScreen(nav, it.arguments?.getString("prompt"))
        }
        composable(Routes.BREATHING) { BreathingScreen(nav) }
        composable(Routes.REFERRAL) { ReferralScreen(nav) }
        composable(Routes.HISTORY) { CheckInHistoryScreen(nav) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
        composable("${Routes.PROFILE_EDIT}?first={first}", arguments = listOf(optionalArg("first"))) {
            ProfileEditScreen(nav, firstTime = it.arguments?.getString("first") == "true")
        }
    }
}

@Composable
fun MainScaffold(nav: NavHostController, current: String, content: @Composable () -> Unit) {
    // Setiap tab dibuka: ambil data terbaru dari server (layar ikut berubah otomatis)
    LaunchedEffect(current) { SeivSync.refreshInBackground() }
    Box(Modifier.fillMaxSize().background(Seiv.Background)) {
        // Sisakan ruang untuk tinggi navbar (64dp), inset navigasi sistem, dan
        // tombol Konseling di tengah yang menonjol 22dp di atas navbar.
        // Tarik layar ke bawah untuk memuat ulang.
        RefreshBox(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 64.dp + 30.dp)) { content() }
        BottomBar(
            current = current,
            onSelect = { if (it != current) nav.goTab(it) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Outlined.Home, "Beranda", current == Routes.HOME) { onSelect(Routes.HOME) }
            NavItem(Icons.Outlined.Bolt, "Aktivitas", current == Routes.ACTIVITY) { onSelect(Routes.ACTIVITY) }
            // ruang untuk tombol tengah
            Column(Modifier.size(64.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(
                    "Konseling", fontSize = 10.sp,
                    fontWeight = if (current == Routes.COUNSELING) FontWeight.Bold else FontWeight.Medium,
                    color = if (current == Routes.COUNSELING) Seiv.PrimaryDark else Seiv.TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            NavItem(Icons.Outlined.CenterFocusWeak, "Skrining", current == Routes.SCREENING) { onSelect(Routes.SCREENING) }
            NavItem(Icons.Outlined.Person, "Profil", current == Routes.PROFILE) { onSelect(Routes.PROFILE) }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Seiv.PrimaryDark)
                .clickable { onSelect(Routes.COUNSELING) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Forum, "Konseling", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (selected) Seiv.PrimaryDark else Seiv.TextHint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label, fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Seiv.PrimaryDark else Seiv.TextSecondary,
        )
        Box(
            Modifier.padding(top = 2.dp).size(4.dp).clip(CircleShape)
                .background(if (selected) Seiv.PrimaryDark else Color.Transparent),
        )
    }
}

