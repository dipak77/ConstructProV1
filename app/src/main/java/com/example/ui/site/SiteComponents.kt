package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Compatibility layers
val PremiumNavy        = DarkBg0
val PremiumDeepBlue    = DarkBg2
val PremiumCard        = DarkBg1
val PremiumCardLight   = LightBg2
val PremiumBorder      = GlassBorderDark
val PremiumBorderLight = GlassBorderLight

val AquaGlow @Composable get() = MaterialTheme.colorScheme.ext.accentPrimary
val VioletGlow @Composable get() = MaterialTheme.colorScheme.ext.accentSecondary
val EmeraldGlow @Composable get() = MaterialTheme.colorScheme.ext.accentSuccess
val RoseGlow @Composable get() = MaterialTheme.colorScheme.ext.accentDanger
val AmberGlow @Composable get() = MaterialTheme.colorScheme.ext.accentWarning
val IndigoGlow @Composable get() = if (MaterialTheme.colorScheme.ext.isDark) NeonBlue else LightBlue

val GradientAqua @Composable get() = Brush.linearGradient(
    if (MaterialTheme.colorScheme.ext.isDark) listOf(NeonCyan, NeonCyanDim)
    else listOf(LightCyan, Color(0xFF0284C7))
)
val GradientViolet @Composable get() = Brush.linearGradient(
    if (MaterialTheme.colorScheme.ext.isDark) listOf(NeonPurple, NeonPurpleDim)
    else listOf(LightPurple, Color(0xFF6D28D9))
)
val GradientEmerald @Composable get() = Brush.linearGradient(
    if (MaterialTheme.colorScheme.ext.isDark) listOf(NeonGreen, NeonGreenDim)
    else listOf(LightGreen, Color(0xFF047857))
)
val GradientRose @Composable get() = Brush.linearGradient(
    if (MaterialTheme.colorScheme.ext.isDark) listOf(NeonPink, NeonPinkDim)
    else listOf(LightPink, Color(0xFFBE185D))
)
val GradientAmber @Composable get() = Brush.linearGradient(
    if (MaterialTheme.colorScheme.ext.isDark) listOf(NeonAmber, NeonAmberDim)
    else listOf(LightAmber, Color(0xFFB45309))
)
val GradientPremium @Composable get() = Brush.linearGradient(
    if (MaterialTheme.colorScheme.ext.isDark) listOf(NeonCyan, NeonPurple, NeonPink)
    else listOf(LightCyan, LightPurple, LightPink)
)

@Composable
fun PremiumPageHeader(
    dark: Boolean,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val bg = if (dark)
        Brush.horizontalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
    else
        Brush.horizontalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFF)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AquaGlow.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, AquaGlow.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ArrowBack, null,
                        tint = AquaGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B)
                    )
                    Text(
                        subtitle, fontSize = 10.sp,
                        color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }

        // Bottom border glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, AquaGlow.copy(0.3f), Color.Transparent))
                )
        )
    }
}

@Composable
fun PremiumIconBtn(
    icon: ImageVector,
    tint: Color,
    dark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
    }
}

@Composable
fun PremiumStatCard(
    modifier: Modifier = Modifier,
    dark: Boolean,
    label: String,
    value: String,
    valueColor: Color,
    icon: ImageVector,
    gradient: Brush,
    borderColor: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    label, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                )
                Icon(icon, null, tint = valueColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = valueColor,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HeaderActionButton(
    icon: ImageVector,
    dark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (dark) Color(0xFF1E2D4A) else Color.White)
            .border(
                1.dp,
                if (dark) Color(0xFF2D3F5E) else Color(0xFFE2E8F0),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (dark) Color.White else Color(0xFF1E293B),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MiniFinanceStat(
    dark: Boolean,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Black,
                letterSpacing = 1.sp, color = color.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = color,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PremiumInfoCard(
    dark: Boolean,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    valueColor: Color? = null,
    gradient: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(iconColor.copy(alpha = 0.3f), if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0))
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, iconColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    label, fontSize = 9.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    value, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = valueColor ?: (if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B))
                )
            }
        }
    }
}

@Composable
fun PremiumDetailCard(
    dark: Boolean,
    items: List<Triple<ImageVector, String, String>>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (dark)
                    Brush.verticalGradient(listOf(Color(0xFF111827), Color(0xFF0D1B3E)))
                else
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )
            .border(
                1.dp,
                if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0),
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            items.forEachIndexed { idx, (icon, label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            icon, null,
                            tint = AquaGlow.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = if (dark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        value, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B),
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(max = 180.dp),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (idx < items.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                if (dark) Color(0xFF1A2744) else Color(0xFFEEF2FF)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    dark: Boolean,
    placeholder: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (dark) Color(0xFF111827) else Color.White
            )
            .border(
                1.dp,
                if (value.isNotEmpty()) AquaGlow.copy(alpha = 0.5f)
                else if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0),
                RoundedCornerShape(14.dp)
            )
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    placeholder, fontSize = 13.sp,
                    color = if (dark) Color(0xFF2D3F5E) else Color(0xFFCBD5E1)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, null,
                    tint = if (value.isNotEmpty()) AquaGlow else
                        if (dark) Color(0xFF2D3F5E) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = if (value.isNotEmpty()) ({
                Icon(
                    Icons.Default.Clear, null,
                    tint = if (dark) Color(0xFF475569) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp).clickable { onValueChange("") }
                )
            }) else null,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B),
                unfocusedTextColor = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B),
                cursorColor = AquaGlow
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PremiumEmptyState(dark: Boolean, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    Brush.radialGradient(listOf(AquaGlow.copy(0.1f), Color.Transparent)),
                    CircleShape
                )
                .border(1.dp, AquaGlow.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Inbox, null,
                tint = if (dark) Color(0xFF2D3F5E) else Color(0xFFCBD5E1),
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            message, fontSize = 13.sp, textAlign = TextAlign.Center,
            color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PulsatingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size((8 * scale).dp)
            .background(color.copy(alpha = alpha), CircleShape)
    )
}

@Composable
fun DetailTextRow(label: String, value: String, darkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp,
            color = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        Text(value, fontSize = 13.sp,
            color = if (darkTheme) Color(0xFFE2E8F4) else Color(0xFF1E293B), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LegendItem(
    dark: Boolean,
    icon: ImageVector,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(color.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (dark) Color(0xFFCBD5E1) else Color(0xFF475569)
        )
    }
}
