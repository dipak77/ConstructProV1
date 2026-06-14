package com.example.ui

import android.content.Context
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

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
//  DESIGN TOKENS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private object PinTokens {
    val accentViolet  = Color(0xFF7C3AED)
    val accentBlue    = Color(0xFF00D4FF)
    val accentGreen   = Color(0xFF00E68A)
    val errorRed      = Color(0xFFFF5C5C)
    val keySize       = 76.dp
    val keySpacing    = 18.dp
    val dotSizeFilled = 16.dp
    val dotSizeEmpty  = 12.dp
    val pinLength     = 4
}

private data class PinColors(
    val bgGrad: Brush,
    val cardBg: Color,
    val text: Color,
    val subText: Color,
    val border: Color,
    val keyTopHighlight: Color,
    val keyBg: Brush,
    val orbAlpha: Float,
    val vignetteAlpha: Float,
)

private fun pinColors(dark: Boolean): PinColors {
    return if (dark) PinColors(
        bgGrad = Brush.verticalGradient(listOf(Color(0xFF020611), Color(0xFF0A1628), Color(0xFF060E1F))),
        cardBg = Color(0xFF0C1322),
        text = Color(0xFFF1F5F9),
        subText = Color(0xFF64748B),
        border = Color(0xFF1E293B),
        keyTopHighlight = Color.White.copy(alpha = 0.07f),
        keyBg = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.02f))
        ),
        orbAlpha = 1f,
        vignetteAlpha = 0.55f,
    ) else PinColors(
        bgGrad = Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFE8EEFB), Color(0xFFF8FAFF))),
        cardBg = Color.White,
        text = Color(0xFF0F172A),
        subText = Color(0xFF64748B),
        border = Color(0xFFE2E8F0),
        keyTopHighlight = Color.White.copy(alpha = 0.9f),
        keyBg = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color(0xFFF1F5F9).copy(alpha = 0.8f))
        ),
        orbAlpha = 0.45f,
        vignetteAlpha = 0.08f,
    )
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  ANIMATED ORB BACKGROUND
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun AnimatedOrbBackground(colors: PinColors) {
    val t = rememberInfiniteTransition(label = "orbs")
    
    @Composable
    fun orbit(base: Float, amp: Float, dur: Int) = t.animateFloat(
        initialValue = base - amp, targetValue = base + amp,
        animationSpec = infiniteRepeatable(tween(dur, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb"
    )
    
    val o1x by orbit(0.25f, 0.12f, 9000); val o1y by orbit(0.20f, 0.10f, 11000)
    val o2x by orbit(0.72f, 0.10f, 13000); val o2y by orbit(0.65f, 0.12f, 10000)
    val o3x by orbit(0.50f, 0.15f, 15000); val o3y by orbit(0.82f, 0.08f, 12000)
    val a = colors.orbAlpha

    Box(Modifier.fillMaxSize()) {
        listOf(
            Triple(o1x to o1y, PinTokens.accentViolet.copy(alpha = 0.10f * a), 380.dp),
            Triple(o2x to o2y, PinTokens.accentBlue.copy(alpha = 0.08f * a), 320.dp),
            Triple(o3x to o3y, PinTokens.accentGreen.copy(alpha = 0.05f * a), 280.dp),
        ).forEach { (pos, color, sizeDp) ->
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val cx = size.width * pos.first
                        val cy = size.height * pos.second
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(color, Color.Transparent),
                                center = Offset(cx, cy),
                                radius = sizeDp.toPx() * 0.5f
                            ),
                            radius = sizeDp.toPx() * 0.5f,
                            center = Offset(cx, cy)
                        )
                    }
            )
        }
        // vignette
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = colors.vignetteAlpha)),
                        radius = 900f
                    )
                )
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREMIUM LOCK ICON  (concentric animated rings + glow)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun PremiumLockIcon(
    unlocked: Boolean,
    successScale: Float,
    accentColor: Color,
    successColor: Color,
) {
    val ringRotation by rememberInfiniteTransition(label = "ring_rot").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "ring"
    )
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.25f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    val iconColor = if (unlocked) successColor else accentColor

    // expanding success ring
    val ringScale by animateFloatAsState(
        if (unlocked) 2.8f else 1f,
        spring(dampingRatio = Spring.DampingRatioLowBouncy), label = "ring_scale"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(successScale)) {
        // outer glow
        Box(
            Modifier
                .size(110.dp)
                .background(
                    Brush.radialGradient(
                        listOf(iconColor.copy(alpha = if (unlocked) 0.25f else pulseAlpha * 0.4f), Color.Transparent)
                    ), CircleShape
                )
        )
        // expanding success ring
        if (unlocked) {
            Box(
                Modifier
                    .size(80.dp)
                    .scale(ringScale)
                    .border(1.5.dp, successColor.copy(alpha = 0.5f * (1f - (ringScale - 1f) / 1.8f).coerceAtLeast(0f)), CircleShape)
            )
        }
        // rotating arcs (Canvas)
        androidx.compose.foundation.Canvas(Modifier.size(96.dp)) {
            val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(iconColor.copy(alpha = 0.35f), ringRotation, 100f, false, style = stroke)
            drawArc(iconColor.copy(alpha = 0.18f), -ringRotation * 0.7f, 60f, false, style = stroke)
        }
        // glass circle
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))
                    )
                )
                .border(1.dp, iconColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREMIUM PIN DOTS  (glow halo + spring)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
private fun PremiumPinDots(
    count: Int,
    filledCount: Int,
    unlocked: Boolean,
    hasError: Boolean,
    shakeOffsetX: Float,
) {
    val dotColor = when {
        unlocked  -> PinTokens.accentGreen
        hasError  -> PinTokens.errorRed
        else      -> PinTokens.accentViolet
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.offset(x = shakeOffsetX.dp)
    ) {
        repeat(count) { idx ->
            val filled = idx < filledCount
            val scaleFactor by animateFloatAsState(
                if (filled) 1.28f else 1.0f,
                spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                ), label = "dotScale_$idx"
            )
            val size by animateDpAsState(
                if (filled) PinTokens.dotSizeFilled else PinTokens.dotSizeEmpty,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dot$idx"
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.scale(scaleFactor)
            ) {
                // glow halo
                if (filled) {
                    Box(
                        Modifier
                            .size(size + 14.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(dotColor.copy(alpha = 0.30f), Color.Transparent)
                                ), CircleShape
                            )
                    )
                }
                Box(
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(if (filled) dotColor else Color.Transparent)
                        .border(
                            1.5.dp,
                            if (filled) dotColor else dotColor.copy(alpha = 0.30f),
                            CircleShape
                        )
                )
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREMIUM PIN KEY  (glass + top-highlight + press glow)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private val KeyLetters = mapOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "0" to "+"
)

@Composable
private fun PremiumPinKey(
    label: String,
    colors: PinColors,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isEmpty = label.isEmpty()
    if (isEmpty) {
        Box(modifier = Modifier.size(PinTokens.keySize))
        return
    }

    val isBack  = label == "⌫"
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth physical spring compression under press - stabilized to 1f for web emulator compatibility
    val scale = 1.0f

    // Border glow color transition
    val targetBorderColor = if (isPressed) accentColor else colors.border.copy(alpha = 0.5f)
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(120),
        label = "keyBorder_$label"
    )

    // Text and icon color illumination
    val textAndIconColor by animateColorAsState(
        targetValue = if (isPressed) accentColor else colors.text,
        animationSpec = tween(100),
        label = "keyText_$label"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PinTokens.keySize)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // LAYER A: Halo glow bloom on press
        if (isPressed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale * 1.35f
                        scaleY = scale * 1.35f
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                                center = center,
                                radius = size.minDimension / 2f
                            ),
                            radius = size.minDimension / 2f
                        )
                    }
            )
        }

        // LAYER B: Clipped glassmorphic interactive button body
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(colors.keyBg, CircleShape)
                .drawWithContent {
                    drawContent()
                    // Glass top highlight crescent
                    drawArc(
                        color = colors.keyTopHighlight,
                        startAngle = 200f, sweepAngle = 140f,
                        useCenter = false,
                        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                .border(if (isPressed) 1.5.dp else 1.dp, borderColor, CircleShape)
        ) {
            if (isBack) {
                Icon(
                    Icons.Default.Backspace, "Backspace",
                    tint = textAndIconColor, modifier = Modifier.size(24.dp)
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textAndIconColor,
                        letterSpacing = 1.sp,
                    )
                    val sub = KeyLetters[label]
                    if (!sub.isNullOrEmpty()) {
                        Text(
                            text = sub,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPressed) accentColor.copy(alpha = 0.75f) else colors.subText,
                            letterSpacing = 1.5.sp,
                        )
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  MAIN PIN SCREEN
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
    val colors  = remember(dark) { pinColors(dark) }
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

    // shake
    val dotShiftX = remember { Animatable(0f) }
    LaunchedEffect(shakeState) {
        if (shakeState) {
            repeat(4) {
                dotShiftX.animateTo(12f, tween(50))
                dotShiftX.animateTo(-12f, tween(50))
            }
            dotShiftX.animateTo(0f, tween(50))
            delay(180); shakeState = false
        }
    }
    // success
    val successScale by animateFloatAsState(
        if (unlocked) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "us"
    )
    val numpadAlpha by animateFloatAsState(
        if (unlocked) 0f else 1f,
        tween(durationMillis = 350, easing = LinearOutSlowInEasing), label = "na"
    )
    val numpadTranslationY by animateDpAsState(
        if (unlocked) 50.dp else 0.dp,
        spring(stiffness = Spring.StiffnessLow), label = "nt"
    )
    LaunchedEffect(unlocked) { if (unlocked) { delay(700); onPinVerified() } }

    val currentPin = if (isSetup && inConfirm) confirmPin else pin

    fun onKey(d: String) {
        if (unlocked) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        errorMsg = null
        if (isSetup) {
            if (!inConfirm) {
                if (pin.length < PIN_LEN) { pin += d; if (pin.length == PIN_LEN) inConfirm = true }
            } else {
                if (confirmPin.length < PIN_LEN) {
                    confirmPin += d
                    if (confirmPin.length == PIN_LEN) {
                        if (confirmPin == pin) { savePin(context, pin); unlocked = true }
                        else { shakeState = true; errorMsg = "PINs don't match. Try again."; confirmPin = "" }
                    }
                }
            }
        } else {
            if (pin.length < PIN_LEN) {
                pin += d
                if (pin.length == PIN_LEN) {
                    if (verifyPin(pin, credential)) unlocked = true
                    else { shakeState = true; errorMsg = "Incorrect PIN. Try again."; pin = "" }
                }
            }
        }
    }
    fun onBack() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); errorMsg = null
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

    // Capture device system back gesture and back button clicks safely
    BackHandler {
        if (isSetup && inConfirm) {
            inConfirm = false
            confirmPin = ""
            pin = ""
        } else {
            onBackToLogin()
        }
    }

    // subtitle breathing
    val breatheAlpha by rememberInfiniteTransition(label = "breathe").animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "breathe"
    )

    // ── LAYOUT ──────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize().background(colors.bgGrad)) {
        AnimatedOrbBackground(colors)

        val scrollState = rememberScrollState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .systemBarsPadding()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Shield badge ────────────────────────────────────────────────
            if (!unlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PinTokens.accentViolet.copy(alpha = 0.08f))
                        .border(0.5.dp, PinTokens.accentViolet.copy(alpha = 0.18f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        Icons.Outlined.Shield, null,
                        tint = PinTokens.accentViolet.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        "Secured", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        color = PinTokens.accentViolet.copy(alpha = 0.8f),
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Lock icon ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = dotShiftX.value * 1.1f
                        rotationZ = dotShiftX.value * 0.7f
                    }
            ) {
                PremiumLockIcon(
                    unlocked = unlocked,
                    successScale = successScale,
                    accentColor = PinTokens.accentViolet,
                    successColor = PinTokens.accentGreen,
                )
            }
            Spacer(Modifier.height(24.dp))

            // ── Title ───────────────────────────────────────────────────────
            Text(
                text = when {
                    unlocked  -> "Access Granted"
                    isSetup   -> "Create PIN"
                    else      -> "Welcome back"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (unlocked) PinTokens.accentGreen else colors.text,
                letterSpacing = (-0.3).sp,
            )
            if (!isSetup && !unlocked) {
                Text(
                    text = userName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PinTokens.accentBlue,
                    letterSpacing = 0.3.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = when {
                    unlocked              -> "Entering app…"
                    isSetup && !inConfirm -> "Choose a 4-digit PIN"
                    isSetup               -> "Confirm your PIN"
                    else                  -> "Enter your 4-digit PIN"
                },
                fontSize = 13.sp,
                color = colors.subText.copy(alpha = breatheAlpha),
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp,
            )
            Spacer(Modifier.height(32.dp))

            // ── PIN dots ────────────────────────────────────────────────────
            PremiumPinDots(
                count = PIN_LEN,
                filledCount = currentPin.length,
                unlocked = unlocked,
                hasError = errorMsg != null,
                shakeOffsetX = dotShiftX.value,
            )
            Spacer(Modifier.height(16.dp))

            // ── Error chip ──────────────────────────────────────────────────
            AnimatedVisibility(
                visible = errorMsg != null,
                enter = fadeIn(tween(200)) + slideInVertically { -it / 2 },
                exit = fadeOut(tween(150))
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(PinTokens.errorRed.copy(alpha = 0.10f))
                        .border(0.5.dp, PinTokens.errorRed.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        errorMsg ?: "", color = PinTokens.errorRed,
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // ── Gradient divider ────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth(0.35f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, PinTokens.accentViolet.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(24.dp))

            // ── Numpad ──────────────────────────────────────────────────────
            Column(
                verticalArrangement = Arrangement.spacedBy(PinTokens.keySpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = numpadAlpha
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
                            PremiumPinKey(
                                label = key,
                                colors = colors,
                                accentColor = PinTokens.accentViolet,
                                enabled = !unlocked,
                                onClick = {
                                    when (key) {
                                        "⌫" -> onBack()
                                        ""  -> {}
                                        else -> onKey(key)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Skip or Reset/Logout Actions ────────────────────────────────
            if (isSetup && !inConfirm) {
                Text(
                    "Skip for now",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.subText,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { disablePin(context); onPinVerified() }
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
                        Icons.Default.Logout,
                        contentDescription = "Sign out",
                        modifier = Modifier.size(16.dp),
                        tint = PinTokens.errorRed.copy(alpha = 0.8f)
                    )
                    Text(
                        "Reset PIN & Sign Out",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PinTokens.errorRed.copy(alpha = 0.9f),
                        letterSpacing = 0.2.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  PREVIEW COMPOSABLES  – visible in Android Studio "Split / Design" pane
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Preview(
    name = "🌙  Dark – Verify",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF020611,
    group = "PinScreen",
)
@Composable
private fun PreviewPinDark() {
    PinScreen(dark = true, userName = "Alexandra", onPinVerified = {})
}

@Preview(
    name = "☀️  Light – Verify",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFFF0F4FF,
    group = "PinScreen",
)
@Composable
private fun PreviewPinLight() {
    PinScreen(dark = false, userName = "Alexandra", onPinVerified = {})
}
