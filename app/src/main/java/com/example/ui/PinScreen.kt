package com.example.ui

import android.content.Context
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.cos
import kotlin.math.sin

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  SECURITY LAYER  (SHA-256 + salt, legacy migration)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private const val PREFS_NAME       = "constructpro_prefs"
private const val KEY_PIN          = "app_security_pin"
private const val KEY_PIN_HASH     = "app_security_pin_hash"
private const val KEY_PIN_SALT     = "app_security_pin_salt"
private const val KEY_PIN_DISABLED = "app_security_pin_disabled"

data class PinCredential(val hash: String?, val salt: String?, val disabled: Boolean)

fun hashPin(pin: String, salt: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

fun newSalt(): String {
    val b = ByteArray(16); SecureRandom().nextBytes(b)
    return Base64.encodeToString(b, Base64.NO_WRAP)
}

fun readPinCredential(ctx: Context): PinCredential {
    val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val disabled = p.getBoolean(KEY_PIN_DISABLED, false)
    val hash = p.getString(KEY_PIN_HASH, null)
    val salt = p.getString(KEY_PIN_SALT, null)
    if (disabled || (hash != null && salt != null)) {
        return PinCredential(hash, salt, disabled)
    }

    val legacy = p.getString(KEY_PIN, null)
    if (legacy == "SKIP") {
        p.edit().remove(KEY_PIN).putBoolean(KEY_PIN_DISABLED, true).apply()
        return PinCredential(null, null, true)
    } else if (legacy != null && legacy.length == 4 && legacy.all { it.isDigit() }) {
        val s = newSalt()
        val h = hashPin(legacy, s)
        p.edit().remove(KEY_PIN).putString(KEY_PIN_HASH, h)
            .putString(KEY_PIN_SALT, s).putBoolean(KEY_PIN_DISABLED, false).apply()
        return PinCredential(h, s, false)
    }
    return PinCredential(null, null, false)
}

fun savePin(context: Context, pin: String) {
    val s = newSalt()
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .remove(KEY_PIN).putString(KEY_PIN_HASH, hashPin(pin, s))
        .putString(KEY_PIN_SALT, s).putBoolean(KEY_PIN_DISABLED, false).apply()
}

fun disablePin(ctx: Context) {
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        .remove(KEY_PIN).remove(KEY_PIN_HASH).remove(KEY_PIN_SALT)
        .putBoolean(KEY_PIN_DISABLED, true).apply()
}

private fun verifyPin(pin: String, c: PinCredential): Boolean {
    val h = c.hash ?: return false; val s = c.salt ?: return false
    return hashPin(pin, s) == h
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  DESIGN TOKENS & COLOR PALETTE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private object PinTokens {
    val keySize       = 64.dp
    val keySpacing    = 12.dp
    val pinLength     = 4
}

private object PremiumPinColors {
    val bgroundBase     = Color(0xFF020817) // Layer 0 - Deep void base
    val bgroundBaseSec  = Color(0xFF0B1224) // Layer 1 - Deep blue galaxy hub
    val neonCyan        = Color(0xFF00D4FF) // Primary neon sky action
    val neonPurple      = Color(0xFFA78BFA) // Aesthetic secondary violet
    val neonGreen       = Color(0xFF00FF87) // Success active green
    val neonPink        = Color(0xFFFF4D7D) // Error warning danger pink
    val textPrimary     = Color(0xFFF8FAFC) // Ice white headers
    val textSecondary   = Color(0xFF94A3B8) // Slate secondary
}

// Keyboards letter pairings
private val KeyLetters = mapOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "0" to "+"
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  ANIMATED SPACE BLUEPRINT BACKGROUND
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun AnimatedSpaceBlueprintBackground(
    pinLength: Int,
    isError: Boolean,
    isSuccess: Boolean
) {
    val scaleBreath = 1.0f
    val gridOffset = 30f

    // Vibrant interactive backing color shifting dynamically with user interaction state
    val glowColorTarget = when {
        isSuccess -> PremiumPinColors.neonGreen.copy(alpha = 0.16f)
        isError -> PremiumPinColors.neonPink.copy(alpha = 0.18f)
        pinLength > 0 -> PremiumPinColors.neonCyan.copy(alpha = 0.08f + (pinLength * 0.02f))
        else -> PremiumPinColors.neonPurple.copy(alpha = 0.06f)
    }
    val ambientGlowColor by animateColorAsState(
        targetValue = glowColorTarget,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "bg_pulse_glow"
    )

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PremiumPinColors.bgroundBase,
                            PremiumPinColors.bgroundBaseSec,
                            PremiumPinColors.bgroundBase
                        )
                    )
                )
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@drawBehind

                    // A. Vibrant expanding background light bloom orbs
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ambientGlowColor, Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.35f),
                            radius = w * 0.90f * scaleBreath
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PremiumPinColors.neonCyan.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(w * 0.15f, h * 0.12f),
                            radius = w * 0.55f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PremiumPinColors.neonPurple.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(w * 0.85f, h * 0.85f),
                            radius = w * 0.55f
                        )
                    )

                    // B. Blueprint Grid Layout: ConstructPro visual language
                    val gridSize = 45.dp.toPx()
                    val gridC = Color(0x0900D4FF)
                    val startX = (gridOffset % gridSize) - gridSize
                    val startY = (gridOffset % gridSize) - gridSize

                    var currX = startX
                    while (currX < w + gridSize) {
                        drawLine(
                            color = gridC,
                            start = Offset(currX, 0f),
                            end = Offset(currX, h),
                            strokeWidth = 0.8f
                        )
                        currX += gridSize
                    }

                    var currY = startY
                    while (currY < h + gridSize) {
                        drawLine(
                            color = gridC,
                            start = Offset(0f, currY),
                            end = Offset(w, currY),
                            strokeWidth = 0.8f
                        )
                        currY += gridSize
                    }

                    // C. Orbiting tiny architectural anchors (Blueprint crosses)
                    val anchors = listOf(
                        Offset(w * 0.20f, h * 0.22f),
                        Offset(w * 0.80f, h * 0.18f),
                        Offset(w * 0.15f, h * 0.76f),
                        Offset(w * 0.85f, h * 0.68f),
                        Offset(w * 0.50f, h * 0.54f)
                    )
                    anchors.forEachIndexed { idx, center ->
                        val speedFactor = 0.8f + (idx * 0.25f)
                        val angle = gridOffset * 0.012f * speedFactor
                        val activeX = center.x + cos(angle) * 15.dp.toPx()
                        val activeY = center.y + sin(angle) * 15.dp.toPx()
                        
                        val opacity = (0.22f + 0.14f * sin(gridOffset * 0.015f + idx)).coerceIn(0.06f, 0.44f)
                        val crossC = PremiumPinColors.neonCyan.copy(alpha = opacity)
                        val crossSize = 5.dp.toPx()

                        // Smooth cross intersection drawing
                        drawLine(crossC, Offset(activeX - crossSize, activeY), Offset(activeX + crossSize, activeY), strokeWidth = 1f)
                        drawLine(crossC, Offset(activeX, activeY - crossSize), Offset(activeX, activeY + crossSize), strokeWidth = 1f)
                        drawCircle(crossC, radius = 0.8f.dp.toPx(), center = Offset(activeX, activeY))
                    }
                }
        )
        // Vignette Shadow Mask
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.66f)),
                        radius = 1200f
                    )
                )
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  BIOMETRIC VAULT ROTATING DIAL LOCK (The center mechanical locked wheel)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun BiometricVaultLock(
    unlocked: Boolean,
    progressPercent: Float,
    isError: Boolean,
    scaleFactor: Float
) {
    val ringRotation = progressPercent * 90f
    val pulsingGlow = 0.45f

    val scaleBounce by animateFloatAsState(
        targetValue = if (unlocked) 1.22f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioHighBouncy),
        label = "vault_scale_bounce"
    )

    val activeStateC = when {
        unlocked -> PremiumPinColors.neonGreen
        isError  -> PremiumPinColors.neonPink
        else     -> PremiumPinColors.neonCyan
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(114.dp)
            .graphicsLayer {
                scaleX = scaleFactor * scaleBounce
                scaleY = scaleFactor * scaleBounce
            }
    ) {
        // Back-surface physical glow bloom
        Box(
            Modifier
                .size(110.dp)
                .background(
                    Brush.radialGradient(
                        listOf(activeStateC.copy(alpha = if (unlocked) 0.38f else pulsingGlow * 0.16f), Color.Transparent)
                    ), CircleShape
                )
        )

        // Multiple concentric rotating mechanical rings
        Canvas(Modifier.size(100.dp)) {
            val strokeW = 1.25.dp.toPx()
            val fineStrokeW = 0.8.dp.toPx()

            // Outer Dashed blueprint tick ring (slow rotating clockwise)
            rotate(ringRotation) {
                for (deg in 0 until 360 step 12) {
                    drawArc(
                        color = activeStateC.copy(alpha = 0.25f),
                        startAngle = deg.toFloat(),
                        sweepAngle = 4f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner counter-rotating ring segments
            rotate(-ringRotation * 1.5f) {
                for (deg in 0 until 360 step 36) {
                    drawArc(
                        color = PremiumPinColors.neonPurple.copy(alpha = 0.22f),
                        startAngle = deg.toFloat(),
                        sweepAngle = 14f,
                        useCenter = false,
                        style = Stroke(width = fineStrokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Solid inner border skeleton
            drawCircle(
                color = activeStateC.copy(alpha = 0.10f),
                radius = size.width * 0.38f,
                style = Stroke(width = fineStrokeW)
            )
        }

        // Circular sweep progress arc matching entered PIN percentage
        val progressSweepTarget = progressPercent * 360f
        val animatedSweepAngle by animateFloatAsState(
            targetValue = progressSweepTarget,
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "sweep_progress"
        )
        Canvas(Modifier.size(84.dp)) {
            drawArc(
                color = activeStateC,
                startAngle = -90f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Frosted central glass capsule core
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.01f))
                    )
                )
                .border(1.2.dp, activeStateC.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = unlocked,
                animationSpec = tween(380, easing = EaseOutBack),
                label = "vault_core_icon"
            ) { isUnlocked ->
                if (isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = PremiumPinColors.neonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = activeStateC,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREMIUM BIOMETRIC TERMINAL DOTS  (Glowing interface cells)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun PremiumBiometricTerminalDots(
    count: Int,
    filledCount: Int,
    isUnlocked: Boolean,
    isError: Boolean,
    shakeOffsetX: Float
) {
    val activeStateC = when {
        isUnlocked -> PremiumPinColors.neonGreen
        isError    -> PremiumPinColors.neonPink
        else       -> PremiumPinColors.neonCyan
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.offset(x = shakeOffsetX.dp)
    ) {
        repeat(count) { idx ->
            val isFilled = idx < filledCount
            
            val coreScale by animateFloatAsState(
                targetValue = if (isFilled) 1.0f else 0f,
                animationSpec = if (isFilled) {
                    spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
                } else {
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                },
                label = "terminal_dot_scale_$idx"
            )

            val cellOuterScale by animateFloatAsState(
                targetValue = if (isFilled) 1.22f else 1.0f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "terminal_outer_scale_$idx"
            )

            val borderAlpha by animateFloatAsState(
                targetValue = if (isFilled) 0.85f else 0.22f,
                animationSpec = tween(140),
                label = "terminal_dot_border_alpha_$idx"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp)
            ) {
                // Background radial flare glow
                androidx.compose.animation.AnimatedVisibility(
                    visible = isFilled,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(activeStateC.copy(alpha = 0.22f), Color.Transparent)
                                ), CircleShape
                            )
                    )
                }

                // Outer circular scanner ring
                Box(
                    Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            scaleX = cellOuterScale
                            scaleY = cellOuterScale
                        }
                        .border(
                            1.5.dp,
                            activeStateC.copy(alpha = borderAlpha),
                            CircleShape
                        )
                )

                // Springy glowing inner diamond core asset
                Box(
                    Modifier
                        .size(10.dp)
                        .graphicsLayer {
                            scaleX = coreScale; scaleY = coreScale
                        }
                        .clip(CircleShape)
                        .background(activeStateC)
                        .drawBehind {
                            drawCircle(activeStateC.copy(alpha = 0.35f), radius = size.width * 1.5f)
                        }
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREMIUM INTERACTIVE NUM_KEY  (Tactile feedback micro-spring clicks)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun PremiumInteractiveKey(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    if (label.isEmpty()) {
        Box(modifier = Modifier.size(PinTokens.keySize))
        return
    }

    val isBack  = label == "⌫"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val glowColor = if (isPressed) PremiumPinColors.neonCyan else PremiumPinColors.neonPurple
    val borderAlpha = if (isPressed) 0.65f else 0.12f
    val activeBorderColor by animateColorAsState(
        targetValue = if (isPressed) PremiumPinColors.neonCyan else Color.White.copy(alpha = borderAlpha),
        animationSpec = tween(120),
        label = "active_border_color"
    )

    // Sleek physical key inner body
    val bodyBrush = if (isPressed) {
        Brush.verticalGradient(
            listOf(PremiumPinColors.neonCyan.copy(alpha = 0.14f), PremiumPinColors.neonPurple.copy(alpha = 0.04f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.01f))
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PinTokens.keySize)
            .clip(CircleShape)
            .background(bodyBrush)
            .border(1.dp, activeBorderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        if (isBack) {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = if (isPressed) PremiumPinColors.neonCyan else PremiumPinColors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPressed) PremiumPinColors.neonCyan else PremiumPinColors.textPrimary,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                val sub = KeyLetters[label]
                if (!sub.isNullOrEmpty()) {
                    Text(
                        text = sub,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPressed) PremiumPinColors.neonCyan.copy(alpha = 0.8f) else PremiumPinColors.textSecondary,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  MAIN PIN SCREEN LAYER
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun PinScreen(
    dark: Boolean,
    userName: String,
    onPinVerified: () -> Unit,
    onResetSignOut: () -> Unit = {},
    onBackToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val credential = remember { readPinCredential(context) }
    val isSetup = credential.hash == null && !credential.disabled
    val PIN_LEN = PinTokens.pinLength

    LaunchedEffect(credential.disabled) { if (credential.disabled) onPinVerified() }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var inConfirm by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var shakeState by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }

    val dotShiftX = remember { Animatable(0f) }
    LaunchedEffect(shakeState) {
        if (shakeState) {
            // High-fidelity decayed oscillatory spring wave physics sequence
            dotShiftX.animateTo(16f, spring(dampingRatio = 0.20f, stiffness = 850f))
            dotShiftX.animateTo(-12f, spring(dampingRatio = 0.22f, stiffness = 850f))
            dotShiftX.animateTo(8f, spring(dampingRatio = 0.30f, stiffness = 650f))
            dotShiftX.animateTo(-4f, spring(dampingRatio = 0.40f, stiffness = 650f))
            dotShiftX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            delay(100)
            shakeState = false
        }
    }

    val successScale by animateFloatAsState(
        if (unlocked) 1.15f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "unlocked_scale"
    )
    val containerAlpha by animateFloatAsState(
        if (unlocked) 0f else 1f,
        tween(durationMillis = 380, easing = LinearOutSlowInEasing), label = "container_alpha"
    )
    val numpadTranslationY by animateDpAsState(
        if (unlocked) 65.dp else 0.dp,
        spring(stiffness = Spring.StiffnessLow), label = "translation_y"
    )

    LaunchedEffect(unlocked) {
        if (unlocked) {
            delay(850)
            onPinVerified()
        }
    }

    val currentPin = if (isSetup && inConfirm) confirmPin else pin

    fun onKey(d: String) {
        if (unlocked) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMsg = null
        if (isSetup) {
            if (!inConfirm) {
                if (pin.length < PIN_LEN) {
                    pin += d
                    if (pin.length == PIN_LEN) {
                        inConfirm = true
                    }
                }
            } else {
                if (confirmPin.length < PIN_LEN) {
                    confirmPin += d
                    if (confirmPin.length == PIN_LEN) {
                        if (confirmPin == pin) {
                            savePin(context, pin)
                            unlocked = true
                        } else {
                            shakeState = true
                            errorMsg = "PINs don't match. Try again."
                            confirmPin = ""
                        }
                    }
                }
            }
        } else {
            if (pin.length < PIN_LEN) {
                pin += d
                if (pin.length == PIN_LEN) {
                    if (verifyPin(pin, credential)) {
                        unlocked = true
                    } else {
                        shakeState = true
                        errorMsg = "Incorrect PIN. Try again."
                        pin = ""
                    }
                }
            }
        }
    }

    fun onBack() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMsg = null
        if (isSetup && inConfirm) {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                inConfirm = false
                pin = ""
            }
        } else {
            if (pin.isNotEmpty()) pin = pin.dropLast(1)
        }
    }

    BackHandler {
        if (isSetup && inConfirm) {
            inConfirm = false
            confirmPin = ""
            pin = ""
        } else {
            onBackToLogin()
        }
    }

    // Breathing label animation loop
    val subtitleBreatheAlpha = 0.85f

    // ── SCENE TREE LAYOUT ───────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {
        AnimatedSpaceBlueprintBackground(
            pinLength = currentPin.length,
            isError = errorMsg != null,
            isSuccess = unlocked
        )

        val scrollState = rememberScrollState()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .systemBarsPadding()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(8.dp))

            // Upper secure padlock badge
            if (!unlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PremiumPinColors.neonPurple.copy(alpha = 0.08f))
                        .border(0.5.dp, PremiumPinColors.neonPurple.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Secured",
                        tint = PremiumPinColors.neonPurple.copy(alpha = 0.75f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Secured",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumPinColors.neonPurple.copy(alpha = 0.85f),
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(42.dp))
            }

            // Central rotating dialing vault lock
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dotShiftX.value * 1.05f
                        rotationZ = dotShiftX.value * 0.65f
                    }
            ) {
                BiometricVaultLock(
                    unlocked = unlocked,
                    progressPercent = currentPin.length.toFloat() / PIN_LEN.toFloat(),
                    isError = errorMsg != null,
                    scaleFactor = successScale
                )
            }
            Spacer(Modifier.height(24.dp))

            // Text description headers
            Text(
                text = when {
                    unlocked -> "Access Granted"
                    isSetup  -> "Create PIN"
                    else     -> "Welcome back"
                },
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                color = if (unlocked) PremiumPinColors.neonGreen else PremiumPinColors.textPrimary,
                letterSpacing = (-0.3).sp,
            )
            if (!isSetup && !unlocked) {
                Text(
                    text = userName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PremiumPinColors.neonCyan,
                    letterSpacing = 0.3.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    unlocked              -> "Entering ConstructPro…"
                    isSetup && !inConfirm -> "Choose a 4-digit PIN"
                    isSetup               -> "Confirm your PIN"
                    else                  -> "Enter your 4-digit PIN"
                },
                fontSize = 13.sp,
                color = PremiumPinColors.textSecondary.copy(alpha = subtitleBreatheAlpha),
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp,
            )
            Spacer(Modifier.height(30.dp))

            // Glowing indicator terminal capsules
            PremiumBiometricTerminalDots(
                count = PIN_LEN,
                filledCount = currentPin.length,
                isUnlocked = unlocked,
                isError = errorMsg != null,
                shakeOffsetX = dotShiftX.value
            )
            Spacer(Modifier.height(18.dp))

            // Fluid error sliding box chip
            AnimatedVisibility(
                visible = errorMsg != null,
                enter = fadeIn(tween(200)) + slideInVertically { -it / 2 },
                exit = fadeOut(tween(150))
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PremiumPinColors.neonPink.copy(alpha = 0.10f))
                        .border(0.5.dp, PremiumPinColors.neonPink.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = errorMsg ?: "",
                        color = PremiumPinColors.neonPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Soft glowing layout divider
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, PremiumPinColors.neonCyan.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(28.dp))

            // Numpad keypad matrix
            Column(
                verticalArrangement = Arrangement.spacedBy(PinTokens.keySpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = containerAlpha
                    translationY = numpadTranslationY.toPx()
                }
            ) {
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫"),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PinTokens.keySpacing)) {
                        row.forEach { key ->
                            PremiumInteractiveKey(
                                label = key,
                                enabled = !unlocked,
                                onClick = {
                                    when (key) {
                                        "⌫" -> onBack()
                                        else -> onKey(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))

            // Action labels (Skip or Sign-out reset)
            if (isSetup && !inConfirm) {
                Text(
                    text = "Skip for now",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PremiumPinColors.textSecondary,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            println("!!! CLICKED SKIP FOR NOW !!!")
                            disablePin(context)
                            println("!!! DISABLE PIN COMPLETED !!!")
                            onPinVerified()
                            println("!!! ON PIN VERIFIED COMPLETED !!!")
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else if (!isSetup && !unlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onResetSignOut() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Sign out",
                        tint = PremiumPinColors.neonPink.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Reset PIN & Sign Out",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumPinColors.neonPink.copy(alpha = 0.90f),
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREVIEW DESIGNERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Preview(
    name = "🌙 Premium Void - Verify",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF020817,
    group = "PinScreen",
)
@Composable
private fun PreviewPinDark() {
    PinScreen(dark = true, userName = "Alexandra", onPinVerified = {})
}
