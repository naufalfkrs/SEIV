package com.seiv.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Seiv {
    val Primary = Color(0xFF4F7A6F)
    val PrimaryDark = Color(0xFF3E5F56)
    val PrimaryDeep = Color(0xFF2F4A43)
    val Mint = Color(0xFF8BBDAA)
    val MintLight = Color(0xFFDDEBE5)
    val MintSoft = Color(0xFFEAF3EF)
    val Cream = Color(0xFFF5F2EC)
    val Background = Color(0xFFF2F4EF)
    val LoginBg = Color(0xFFE9EDF7)
    val Card = Color(0xFFFFFFFF)
    val Text = Color(0xFF1E2B28)
    val TextSecondary = Color(0xFF7B8784)
    val TextHint = Color(0xFFA9B2AF)
    val Border = Color(0xFFE4E8E4)
    val Orange = Color(0xFFE07A55)
    val OrangeSoft = Color(0xFFFCEBE3)
    val Purple = Color(0xFF7C6FB0)
    val PurpleSoft = Color(0xFFEDEAF7)
    val Yellow = Color(0xFFF2C14E)
    val YellowSoft = Color(0xFFFFF4DC)
    val Red = Color(0xFFD9534F)
    val RedSoft = Color(0xFFFDECEA)
    val Warning = Color(0xFFFFF4E5)
}

private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, color = Seiv.Text),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, color = Seiv.Text),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, color = Seiv.Text),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, color = Seiv.Text),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, color = Seiv.Text),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp, color = Seiv.Text),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 19.sp, color = Seiv.TextSecondary),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 15.sp, color = Seiv.TextSecondary),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.8.sp),
)

@Composable
fun SeivTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Seiv.Primary,
            onPrimary = Color.White,
            secondary = Seiv.Mint,
            background = Seiv.Background,
            surface = Seiv.Card,
            onSurface = Seiv.Text,
            onBackground = Seiv.Text,
            outline = Seiv.Border,
        ),
        typography = AppTypography,
    ) {
        // Font tetap ikut pengaturan sistem, tapi dibatasi maksimal 1.15x supaya
        // layout (kartu, chip, navbar) tetap sesuai desain Figma dan tidak bertabrakan.
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(MAX_FONT_SCALE)),
            content = content,
        )
    }
}

private const val MAX_FONT_SCALE = 1.15f
