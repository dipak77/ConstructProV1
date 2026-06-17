package com.example

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.ConstructionRepository
import com.example.data.Worker
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import com.example.ui.components.LowerMainMenu
import com.example.ui.components.DrawerContent
import com.example.ui.components.SidebarNavRow

// Build trace token: v1.0.1 - ConstructPro Compiled Build
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ConstructionRepository(database.constructionDao())
        MainViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("constructpro_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("drive_auto_backup_enabled", false)
        if (enabled) {
            AutoBackupWorker.schedule(applicationContext)
        }
        enableEdgeToEdge()
        setContent {
            val dark = viewModel.darkThemeEnabled
            LaunchedEffect(Unit) {
                viewModel.loadUserSessionFromPrefs(applicationContext)
                viewModel.initDeviceAndRestrictions(applicationContext)
                viewModel.trackDeviceRegistration(applicationContext)
            }
            LaunchedEffect(key1 = true) {
                viewModel.uiEvents.collectLatest { event ->
                    when (event) {
                        is UiEvent.ShowToast -> {
                            android.widget.Toast.makeText(applicationContext, event.message, if (event.long) android.widget.Toast.LENGTH_LONG else android.widget.Toast.LENGTH_SHORT).show()
                        }
                        is UiEvent.ShowError -> {
                            android.widget.Toast.makeText(applicationContext, "Error: ${event.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                        is UiEvent.ShareFile -> {
                            try {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    applicationContext,
                                    "${packageName}.fileprovider",
                                    event.file
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = event.mimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_TITLE, event.title)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                startActivity(Intent.createChooser(intent, event.title))
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(applicationContext, "Failed to share file: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                        UiEvent.ImportSuccess -> {
                            android.widget.Toast.makeText(applicationContext, "Import completed successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        UiEvent.ExportSuccess -> {
                            android.widget.Toast.makeText(applicationContext, "Export completed successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            MyApplicationTheme(darkTheme = dark) {
                GlassAtmosphereBox(darkTheme = dark) {
                    val userSession by viewModel.userSession.collectAsState()
                    val isBlocked by viewModel.isUserBlocked.collectAsState()

                    // PIN verification state — resets on every cold app open (not persisted)
                    var pinVerified by remember { mutableStateOf(false) }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP) {
                                pinVerified = false
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    val pinCredential = remember(userSession) { readPinCredential(applicationContext) }
                    // Bypass PIN checks if PIN is disabled.
                    val pinVerifiedState = pinVerified || pinCredential.disabled

                    when {
                        isBlocked -> {
                            BlockedScreen(
                                dark = dark,
                                email = userSession?.email ?: "",
                                deviceId = viewModel.deviceId,
                                onRefreshCheck = { viewModel.checkRemoteRestrictions(applicationContext) }
                            )
                        }
                        userSession == null -> {
                            // Step 1: not signed in → Google login
                            GoogleLoginScreen(viewModel = viewModel)
                        }
                        userSession != null && !pinVerifiedState -> {
                            // Step 2: signed in with real Google session but PIN not yet verified this session (skip for demo sandbox)
                            PinScreen(
                                dark = dark,
                                userName = userSession?.displayName ?: "User",
                                onPinVerified = { pinVerified = true },
                                onResetSignOut = {
                                    val prefs = getSharedPreferences("constructpro_prefs", MODE_PRIVATE)
                                    prefs.edit()
                                        .remove("app_security_pin")
                                        .remove("app_security_pin_hash")
                                        .remove("app_security_pin_salt")
                                        .putBoolean("app_security_pin_disabled", false)
                                        .apply()
                                    viewModel.handleGoogleSignOut(applicationContext)
                                },
                                onBackToLogin = {
                                    viewModel.handleGoogleSignOut(applicationContext)
                                }
                            )
                        }
                        else -> {
                            // Step 3: fully authenticated → app
                            ScaffoldFrame(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScaffoldFrame(viewModel: MainViewModel) {
    val dark = viewModel.darkThemeEnabled
    val currentTab = viewModel.currentScreen

    var showQuickDialog by remember { mutableStateOf(false) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showWorkerDialog by remember { mutableStateOf(false) }
    var txType by remember { mutableStateOf("Money Out") }

    val currentProject by viewModel.activeProject.collectAsState()
    val allWorkers by viewModel.workers.collectAsState()

    LaunchedEffect(viewModel.showQuickDialog) {
        if (viewModel.showQuickDialog) {
            showQuickDialog = true
            viewModel.showQuickDialog = false
        }
    }
    LaunchedEffect(viewModel.showProjectDialog) {
        if (viewModel.showProjectDialog) {
            showProjectDialog = true
            viewModel.showProjectDialog = false
        }
    }
    LaunchedEffect(viewModel.showTransactionDialog) {
        if (viewModel.showTransactionDialog) {
            showTransactionDialog = true
            txType = viewModel.transactionTypePreset
            viewModel.showTransactionDialog = false
        }
    }
    LaunchedEffect(viewModel.showTaskDialog) {
        if (viewModel.showTaskDialog) {
            showTaskDialog = true
            viewModel.showTaskDialog = false
        }
    }
    LaunchedEffect(viewModel.showWorkerDialog) {
        if (viewModel.showWorkerDialog) {
            showWorkerDialog = true
            viewModel.showWorkerDialog = false
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(viewModel = viewModel) {
                    coroutineScope.launch { drawerState.close() }
                }
            }
        },
        gesturesEnabled = true
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
        val isDesktopWidth = maxWidth >= 1024.dp

        if (isDesktopWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .padding(16.dp)
                        .background(if (dark) Color(0x35111827) else Color(0xDFFFFFFF), RoundedCornerShape(24.dp))
                        .border(BorderStroke(1.dp, if (dark) GlassBorderDark else GlassBorderLight), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BuildOnSiteLogo(modifier = Modifier.size(36.dp), darkTheme = dark)
                        Text(
                            text = "ConstructPro",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (dark) NeonCyan else Color(0xFF0284C7)
                        )
                    }
                    HorizontalDivider(color = if (dark) GlassBorderDark else GlassBorderLight)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SidebarNavRow("Dashboard", Icons.Default.Dashboard, currentTab == AppScreen.Dashboard, dark) { viewModel.currentScreen = AppScreen.Dashboard }
                        SidebarNavRow("Leads & Bids", Icons.Default.BusinessCenter, currentTab == AppScreen.Money, dark) { viewModel.currentScreen = AppScreen.Money }
                        SidebarNavRow("Site Tasks", Icons.Default.TaskAlt, currentTab == AppScreen.Tasks, dark) { viewModel.currentScreen = AppScreen.Tasks }
                        SidebarNavRow("Attendance Logs", Icons.Default.Face, currentTab == AppScreen.Site, dark) { viewModel.currentScreen = AppScreen.Site }
                        SidebarNavRow("More Options", Icons.Default.MoreHoriz, currentTab == AppScreen.More, dark) { viewModel.currentScreen = AppScreen.More }
                    }
                    GlassButton(onClick = { showProjectDialog = true }, glowColor = NeonPurple, darkTheme = dark, modifier = Modifier.fillMaxWidth()) {
                        Text("+ NEW SITE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = { fadeIn() + slideInVertically { it / 6 } togetherWith fadeOut() + slideOutVertically { it / 6 } },
                        label = "desktopTabsTransition"
                    ) { tab ->
                        when (tab) {
                            AppScreen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onMenuClick = { coroutineScope.launch { drawerState.open() } }
                            )
                            AppScreen.Money -> LeadQuoteScreen(
                                viewModel = viewModel,
                                onMenuClick = { coroutineScope.launch { drawerState.open() } }
                            )
                            AppScreen.Tasks -> TasksScreen(viewModel = viewModel)
                            AppScreen.Site -> SiteScreen(viewModel = viewModel)
                            AppScreen.More -> MoreScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() + slideInVertically { it / 8 } togetherWith fadeOut() + slideOutVertically { it / 8 } },
                            label = "mobileTabsTransition"
                        ) { tab ->
                            when (tab) {
                                AppScreen.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } }
                                )
                                AppScreen.Money -> LeadQuoteScreen(
                                    viewModel = viewModel,
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } }
                                )
                                AppScreen.Tasks -> TasksScreen(viewModel = viewModel)
                                AppScreen.Site -> SiteScreen(viewModel = viewModel)
                                AppScreen.More -> MoreScreen(viewModel = viewModel)
                            }
                        }
                    }

                    // Premium Glassmorphic Bottom Navigation Dock
                    if (currentTab != AppScreen.Site) {
                        LowerMainMenu(
                            currentTab = currentTab,
                            dark = dark,
                            onNavigate = { viewModel.currentScreen = it }
                        )
                    }
                }
            }
        }

        // Premium Neo-AI Fluent Action Button - Dynamic contextual entry on Screens (except Commercial/Money and Dashboard)
        if (currentTab != AppScreen.Money && currentTab != AppScreen.Dashboard) {
            NeoAiFloatingActionButton(
                onClick = {
                    when (currentTab) {
                        AppScreen.Dashboard -> showQuickDialog = true
                        AppScreen.Money -> showTransactionDialog = true
                        AppScreen.Tasks -> showTaskDialog = true
                        AppScreen.Site -> {
                            when (viewModel.activeSiteTab) {
                                "Party" -> showWorkerDialog = true
                                "Transaction" -> showTransactionDialog = true
                                "Task" -> showTaskDialog = true
                                "Attendance" -> showWorkerDialog = true
                                "Site" -> showProjectDialog = true
                                else -> showWorkerDialog = true
                            }
                        }
                        AppScreen.More -> showProjectDialog = true
                    }
                },
                darkTheme = dark,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = if (isDesktopWidth || currentTab == AppScreen.Site) 24.dp else 102.dp, end = 20.dp)
            )
        }
    }

    }

    QuickAddDialog(
        visible = showQuickDialog,
        onDismiss = { showQuickDialog = false },
        darkTheme = dark,
        onAddTransaction = { showTransactionDialog = true },
        onAddTask = { showTaskDialog = true },
        onAddWorker = { showWorkerDialog = true }
    )

    ProjectFormDialog(
        visible = showProjectDialog,
        onDismiss = { showProjectDialog = false },
        darkTheme = dark,
        onSave = { name, location, budget, startDate, endDate ->
            viewModel.addProject(name = name, location = location, budget = budget, startDate = startDate, endDate = endDate)
        }
    )

    TransactionFormDialog(
        visible = showTransactionDialog,
        onDismiss = {
            showTransactionDialog = false
            viewModel.transactionToEdit = null
        },
        darkTheme = dark,
        presetType = txType,
        allWorkers = allWorkers,
        viewModel = viewModel,
        transactionToEdit = viewModel.transactionToEdit,
        onSave = { type, amount, category, description, party, reference, paymentMethod, date ->
            val proj = currentProject
            if (proj != null) {
                val toEdit = viewModel.transactionToEdit
                if (toEdit != null) {
                    viewModel.updateTransaction(
                        toEdit.copy(
                            type = type,
                            amount = amount,
                            category = category,
                            description = description,
                            date = date,
                            partyId = party?.id,
                            partyName = party?.name,
                            reference = reference,
                            paymentMethod = paymentMethod
                        )
                    )
                } else {
                    viewModel.addTransaction(
                        projectId = proj.id,
                        type = type,
                        amount = amount,
                        category = category,
                        description = description,
                        date = date,
                        partyId = party?.id,
                        partyName = party?.name,
                        reference = reference,
                        paymentMethod = paymentMethod
                    )
                }
            }
            viewModel.transactionToEdit = null
        }
    )

    TaskFormDialog(
        visible = showTaskDialog,
        onDismiss = { showTaskDialog = false },
        darkTheme = dark,
        onSave = { title, priority, assignee, dueDate ->
            val proj = currentProject
            if (proj != null) {
                viewModel.addTask(proj.id, title, priority, assignee, dueDate)
            }
        }
    )

    WorkerFormDialog(
        visible = showWorkerDialog,
        onDismiss = { showWorkerDialog = false },
        darkTheme = dark,
        workerCount = allWorkers.size,
        onSave = { name, role, shift, wageRate, phone, email, partyType, address, partyId, dateOfJoining, aadhaar, pan, reference ->
            viewModel.addWorker(
                name = name,
                role = role,
                shift = shift,
                wageRate = wageRate,
                color = listOf(0xFF3B82F6.toInt(), 0xFFEC4899.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFF8B5CF6.toInt()).random(),
                phone = phone,
                email = email,
                partyType = partyType,
                address = address,
                partyId = partyId,
                dateOfJoining = dateOfJoining,
                aadhaar = aadhaar,
                pan = pan,
                reference = reference
            )
        }
    )
}

@Composable
private fun TogglePill(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    onClick: () -> Unit
) {
    val resolvedColor = if (!darkTheme) {
        when (selectedColor) {
            NeonCyan -> Color(0xFF0284C7)
            NeonPurple -> Color(0xFF6D28D9)
            NeonGreen -> Color(0xFF047857)
            NeonPink -> Color(0xFFBE123C)
            else -> selectedColor
        }
    } else selectedColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) resolvedColor.copy(alpha = 0.20f) else Color.Transparent)
            .border(1.dp, if (selected) resolvedColor else if (darkTheme) GlassBorderLight.copy(alpha = 0.20f) else GlassBorderLight, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) resolvedColor else if (darkTheme) TextSecondary else TextSecondaryLight,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun NeoAiFloatingActionButton(
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neo_fab")
    
    // Constant pulsing scale breath
    val scaleBreath by infiniteTransition.animateFloat(
        initialValue = 0.96f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_breath"
    )

    // Rotating dial angle
    val dialRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fab_rotation"
    )
    
    // Pulsing glowing background radius
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_aura"
    )

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val clickScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "fab_click_scale"
    )

    val coreColor = if (darkTheme) NeonPurple else Color(0xFF6366F1)
    val accentColor = if (darkTheme) NeonCyan else Color(0xFF06B6D4)

    Box(
        modifier = modifier
            .size(72.dp) // Generous 48dp+ tap size
            .graphicsLayer {
                scaleX = scaleBreath * clickScale
                scaleY = scaleBreath * clickScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Radar Glowing Aura behind the FAB
        Box(
            modifier = Modifier
                .size(68.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(coreColor.copy(alpha = auraAlpha), Color.Transparent),
                            radius = this.size.width * 0.48f
                        )
                    )
                }
        )

        // 2. Rotating holographic mechanical border dial
        Canvas(modifier = Modifier.size(60.dp)) {
            rotate(dialRotation) {
                // Draw twin neon crescent arcs orbiting each other
                drawArc(
                    color = accentColor.copy(alpha = 0.65f),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = coreColor.copy(alpha = 0.65f),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // Fine blueprint guide circle
            drawCircle(
                color = coreColor.copy(alpha = 0.15f),
                style = Stroke(width = 0.8f.dp.toPx())
            )
        }

        // 3. Main Glass FAB Capsule
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            coreColor,
                            coreColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .drawBehind {
                    // Premium gloss highlight
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(this.size.width * 0.35f, this.size.height * 0.35f),
                            radius = this.size.width * 0.45f
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Record action",
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        // Micro rotating feedback inside icon when pressed
                        rotationZ = if (isPressed) 45f else 0f
                    }
            )
        }
    }
}

