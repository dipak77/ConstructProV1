package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSecondaryLight

/* ═══════════════════════════════════════════════════════════════════════════════
   LowerMainMenu — Obsidian Luxury · Premium Floating Navigation Bar
   ═══════════════════════════════════════════════════════════════════════════════

   Layer stack (bottom → top):
     ① Multi-pass neon aura glow    – bleeds outside container edges (dark only)
     ② Deep frosted-glass fill      – dense gradient, darker than surface
     ③ 5-stop gradient border       – cyan → indigo shimmer (dark) / blue (light)
     ④ Inner specular top edge      – top-edge glass reflection
     ⑤ Inner ambient tint           – subtle NeonCyan wash (dark only)
     ⑥ Navigation items             – animated pill + glow + lift
   ══════════════════════════════════════════════════════════════════════════════ */

@Composable
fun LowerMainMenu(
    currentTab: AppScreen,
    dark: Boolean,
    onNavigate: (AppScreen) -> Unit
) {
    if (currentTab == AppScreen.Site) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 20.dp, top = 8.dp)
            .height(76.dp)

            // ── ① Layered neon aura (dark only) ──────────────────────────────
            // Three expanding semi-transparent rounds simulate a soft bloom glow
            // that radiates outward from behind the bar without BlurMaskFilter.
            .drawBehind {
                if (dark) {
                    listOf(22f to 0.055f, 13f to 0.105f, 6f to 0.075f)
                        .forEach { (expand, alpha) ->
                            drawRoundRect(
                                color = NeonCyan.copy(alpha = alpha),
                                topLeft = Offset(-expand, -expand),
                                size = Size(
                                    width  = size.width  + expand * 2f,
                                    height = size.height + expand * 2f
                                ),
                                cornerRadius = CornerRadius(24.dp.toPx() + expand)
                            )
                        }
                }
            }

            // ── ② Deep frosted-glass fill ─────────────────────────────────────
            .background(
                brush = Brush.linearGradient(
                    colors = if (dark) {
                        listOf(Color(0xF2050A16), Color(0xF20E1428), Color(0xF2050A16))
                    } else {
                        listOf(Color(0xFCFFFFFF), Color(0xFCF4F8FF), Color(0xFCFFFFFF))
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // ── ③ Vivid 5-stop gradient border ────────────────────────────────
            .border(
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = if (dark) listOf(
                            GlassBorderDark,
                            NeonCyan.copy(alpha = 0.62f),
                            Color(0xFF818CF8).copy(alpha = 0.52f),
                            NeonCyan.copy(alpha = 0.28f),
                            GlassBorderDark
                        ) else listOf(
                            Color(0x226366F1),
                            Color(0xAA06B6D4),
                            Color(0x996366F1),
                            Color(0x226366F1)
                        )
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // ── ④ + ⑤ Inner surface details ──────────────────────────────────
            // Drawn after the border so these sit on the inner glass surface.
            .drawBehind {
                // Top-edge specular shine (glass reflection)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (dark) Color.White.copy(alpha = 0.09f)
                            else      Color.White.copy(alpha = 0.88f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY   = size.height * 0.30f
                    ),
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
                // Subtle cyan ambient wash (dark only)
                if (dark) {
                    drawRoundRect(
                        color        = NeonCyan.copy(alpha = 0.032f),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
            },
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        BottomBarNavItem(
            icon   = Icons.Default.Dashboard,
            active = currentTab == AppScreen.Dashboard,
            darkTheme = dark, label = "Dashboard"
        ) { onNavigate(AppScreen.Dashboard) }

        BottomBarNavItem(
            icon   = Icons.Default.BusinessCenter,
            active = currentTab == AppScreen.Money,
            darkTheme = dark, label = "Commercial"
        ) { onNavigate(AppScreen.Money) }

        BottomBarNavItem(
            icon   = Icons.Default.TaskAlt,
            active = currentTab == AppScreen.Tasks,
            darkTheme = dark, label = "Tasks"
        ) { onNavigate(AppScreen.Tasks) }

        BottomBarNavItem(
            icon   = Icons.Default.EventAvailable,
            active = currentTab == AppScreen.Site,
            darkTheme = dark, label = "Site"
        ) { onNavigate(AppScreen.Site) }

        BottomBarNavItem(
            icon   = Icons.Default.Menu,
            active = currentTab == AppScreen.More,
            darkTheme = dark, label = "More"
        ) { onNavigate(AppScreen.More) }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════════
   BottomBarNavItem — Individual navigation pill with premium micro-interactions
   ═══════════════════════════════════════════════════════════════════════════════

   Animation layers per item:
     · Scale spring     – item gently pops to 112% with medium-bouncy spring
     · Lift offset      – item floats −3 dp upward when active
     · Activation %     – 0→1 fraction drives pill opacity, dot width, glow alpha
     · Tint animation   – icon/label color cross-fades 240ms
     · Pulse ring       – infinite scale+fade ring behind icon (dark mode only)
     · Pill backdrop    – radial gradient capsule; fades in with activeFraction
     · Icon glow halo   – two concentric radial circles drawn behind icon (dark)
     · Indicator dot    – horizontal gradient pill; width = 14dp × activeFraction
   ══════════════════════════════════════════════════════════════════════════════ */

@Composable
fun BottomBarNavItem(
    icon: ImageVector,
    active: Boolean,
    darkTheme: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val activeColor   = if (darkTheme) NeonCyan       else Color(0xFF0284C7)
    val inactiveColor = if (darkTheme) TextSecondary  else TextSecondaryLight

    // ── Core state animations ─────────────────────────────────────────────────

    // Bouncy scale — item pops when selected
    val scale by animateFloatAsState(
        targetValue  = if (active) 1.12f else 1.00f,
        animationSpec = spring(
            stiffness    = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "nav_scale"
    )

    // Lift — active item gently floats above its siblings
    val liftOffset by animateDpAsState(
        targetValue  = if (active) (-3).dp else 0.dp,
        animationSpec = spring(
            stiffness    = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "nav_lift"
    )

    // 0→1 activation fraction — master driver for all opacity/size effects
    val activeFraction by animateFloatAsState(
        targetValue  = if (active) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label        = "nav_fraction"
    )

    // Color cross-fade for icon tint and label
    val tintColor by animateColorAsState(
        targetValue  = if (active) activeColor else inactiveColor,
        animationSpec = tween(240),
        label        = "nav_tint"
    )

    // ── Infinite pulse ring (runs always; alpha gated by activeFraction) ──────
    val pulse = rememberInfiniteTransition(label = "pulse_$label")

    val pulseScale by pulse.animateFloat(
        initialValue = 0.78f, targetValue = 1.85f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.52f, targetValue = 0.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1050, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .offset(y = liftOffset)                                     // float up
            .graphicsLayer {                                            // spring pop
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = activeColor.copy(alpha = 0.16f)),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {

        // ── Pill icon container (46 × 28 dp capsule) ─────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(46.dp)
                .height(28.dp)
        ) {

            // Radial gradient pill backdrop — fades in smoothly with activeFraction
            if (activeFraction > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)  // CircleShape on non-square = pill/capsule
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = 0.30f * activeFraction),
                                    activeColor.copy(alpha = 0.11f * activeFraction),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Pulsing halo ring — dark mode only, scaled and faded infinitely
            if (darkTheme) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha  = (pulseAlpha * activeFraction).coerceIn(0f, 1f)
                        }
                        .clip(CircleShape)
                        .background(activeColor.copy(alpha = 0.22f))
                )
            }

            // Icon with soft radial glow halo drawn behind it (dark + active)
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = tintColor,
                modifier           = Modifier
                    .size(22.dp)
                    .drawBehind {
                        if (darkTheme && activeFraction > 0.02f) {
                            // Inner tight glow
                            drawCircle(
                                color  = activeColor.copy(alpha = 0.24f * activeFraction),
                                radius = 15.dp.toPx()
                            )
                            // Outer soft halo
                            drawCircle(
                                color  = activeColor.copy(alpha = 0.09f * activeFraction),
                                radius = 23.dp.toPx()
                            )
                        }
                    }
            )
        }

        Spacer(Modifier.height(2.dp))

        // ── Label — consistent size avoids layout reflow; weight carries state ─
        Text(
            text       = label,
            color      = tintColor,
            fontSize   = 10.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines   = 1
        )

        // ── Gradient indicator pill — width animates from 0 → 14 dp ──────────
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .width((14f * activeFraction).dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.42f),
                            activeColor,
                            activeColor.copy(alpha = 0.42f)
                        )
                    )
                )
        )
    }
}