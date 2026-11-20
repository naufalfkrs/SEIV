package com.seiv.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seiv.app.ui.theme.Seiv

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    container: Color = Seiv.Primary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            disabledContainerColor = container.copy(alpha = 0.4f),
            contentColor = Color.White,
            disabledContentColor = Color.White,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        }
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        if (trailingIcon != null) {
            Spacer(Modifier.width(6.dp)); Icon(trailingIcon, null, Modifier.size(18.dp))
        }
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Seiv.Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Seiv.PrimaryDark),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun SeivCard(
    modifier: Modifier = Modifier,
    color: Color = Seiv.Card,
    radius: Dp = 18.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .shadow(2.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(shape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun BackCircle(onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Seiv.Text, bg: Color = Color.White) {
    Box(
        modifier
            .size(36.dp)
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    overline: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackCircle(onBack)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (overline != null) Text(overline, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            if (subtitle != null) Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: String? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .clip(shape)
            .background(if (selected) Seiv.MintLight else Color.White)
            .border(1.dp, if (selected) Seiv.Primary else Seiv.Border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(leading, fontSize = 13.sp); Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Seiv.PrimaryDark else Seiv.Text,
        )
    }
}

@Composable
fun Tag(text: String, color: Color = Seiv.Primary, bg: Color = Seiv.MintLight, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, hint: String? = null, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
        if (hint != null) {
            Spacer(Modifier.width(4.dp))
            Text(hint, fontSize = 10.sp, color = Seiv.TextHint)
        }
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(action, fontSize = 12.sp, color = Seiv.Primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun Avatar(initials: String, colorIndex: Int, size: Dp = 48.dp) {
    val palettes = listOf(
        Color(0xFF1F4E8C) to Color(0xFFDCE7F7),
        Color(0xFFB4533A) to Color(0xFFF9E3DB),
        Color(0xFF5B4C9A) to Color(0xFFE8E4F6),
    )
    val (fg, bg) = palettes[colorIndex % palettes.size]
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value / 3).sp)
    }
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, bg: Color, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun KeyValueRow(key: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(key, fontSize = 12.sp, color = Seiv.TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            value, fontSize = if (bold) 15.sp else 12.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = Seiv.Text,
            modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun StepProgress(current: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .height(4.dp)
                    .width(if (i == current) 26.dp else 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (i <= current) color else Seiv.Border),
            )
        }
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, bg: Color = Seiv.MintLight, fg: Color = Seiv.PrimaryDark) {
    Box(modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
