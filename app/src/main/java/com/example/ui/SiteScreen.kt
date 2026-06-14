package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// MAIN ROUTER / ORCHESTRATOR
// ─────────────────────────────────────────────
@Composable
fun SiteScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dark = viewModel.darkThemeEnabled
    val context = LocalContext.current
    val currentProject by viewModel.activeProject.collectAsState()
    val allWorkers by viewModel.workers.collectAsState()
    val allAttendance by viewModel.attendance.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()

    val activeDate = viewModel.attendanceDate
    var selectedWorkerForAttendance by remember { mutableStateOf<Worker?>(null) }
    var inputOvertimeHours by remember { mutableStateOf("0.0") }

    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US) }
    val cFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    var selectedPartyDetail by remember { mutableStateOf<Worker?>(null) }
    var selectedTxDetail by remember { mutableStateOf<Transaction?>(null) }

    val activeSiteTab = viewModel.activeSiteTab
    val tabs = listOf("Party", "Transaction", "Attendance")

    var partySearchQuery by remember { mutableStateOf("") }
    var txSearchQuery by remember { mutableStateOf("") }
    var taskSearchQuery by remember { mutableStateOf("") }

    var txDatePreset by remember { mutableStateOf("All") }
    var txStartDate by remember { mutableStateOf("") }
    var txEndDate by remember { mutableStateOf("") }
    var showTxDateFilterDialog by remember { mutableStateOf(false) }

    var showAddPartyTxDialog by remember { mutableStateOf(false) }
    var partyTxType by remember { mutableStateOf("Money Out") }
    var partyTxAmount by remember { mutableStateOf("") }
    var partyTxCategory by remember { mutableStateOf("Labour") }
    var partyTxDesc by remember { mutableStateOf("") }
    var partyTxMethod by remember { mutableStateOf("Cash") }
    var partyTxDate by remember { mutableStateOf("2026-05-27") }
    var showPdfPreviewDialog by remember { mutableStateOf(false) }

    val activeProjId = currentProject?.id
    val projectTransactions = remember(allTransactions, activeProjId) {
        if (activeProjId == null) emptyList()
        else allTransactions.filter { it.projectId == activeProjId }
    }

    val filteredTransactions = remember(projectTransactions, txSearchQuery, txDatePreset, txStartDate, txEndDate) {
        var list = projectTransactions
        if (txSearchQuery.isNotEmpty()) {
            list = list.filter { tx ->
                (tx.description.contains(txSearchQuery, ignoreCase = true)) ||
                        (tx.category?.contains(txSearchQuery, ignoreCase = true) == true) ||
                        (tx.partyName?.contains(txSearchQuery, ignoreCase = true) == true) ||
                        (tx.reference.contains(txSearchQuery, ignoreCase = true)) ||
                        (tx.paymentMethod.contains(txSearchQuery, ignoreCase = true)) ||
                        tx.amount.toString().contains(txSearchQuery)
            }
        }
        val bounds = if (txDatePreset == "Custom") {
            if (txStartDate.isNotEmpty() && txEndDate.isNotEmpty()) Pair(txStartDate, txEndDate) else null
        } else {
            getPresetDateRange(txDatePreset)
        }
        if (bounds != null) {
            val (start, end) = bounds
            list = list.filter { it.date >= start && it.date <= end }
        }
        list
    }

    val activeDateRangeText = remember(txDatePreset, txStartDate, txEndDate) {
        if (txDatePreset == "Custom") {
            if (txStartDate.isNotEmpty() && txEndDate.isNotEmpty()) {
                "$txStartDate to $txEndDate"
            } else {
                "Custom Range"
            }
        } else {
            txDatePreset
        }
    }

    val navigateDay = { days: Int ->
        val cal = Calendar.getInstance()
        cal.time = formatter.parse(activeDate) ?: Date()
        cal.add(Calendar.DATE, days)
        viewModel.attendanceDate = formatter.format(cal.time)
    }

    val parsedDateString = remember(activeDate) {
        try { displayFormat.format(formatter.parse(activeDate) ?: Date()) }
        catch (e: Exception) { activeDate }
    }

    val activeDateAttendance = remember(allAttendance, activeDate, currentProject) {
        val projId = currentProject?.id ?: return@remember emptyList()
        allAttendance.filter { it.date == activeDate && it.projectId == projId }
    }

    val presentCount  = activeDateAttendance.count { it.status == "Present" }
    val absentCount   = activeDateAttendance.count { it.status == "Absent" }
    val totalOvertime = activeDateAttendance.sumOf { it.overtimeHours }

    val dailyWages = remember(activeDateAttendance, allWorkers) {
        activeDateAttendance.sumOf { att ->
            val w = allWorkers.find { it.id == att.workerId } ?: return@sumOf 0.0
            val base = if (att.status == "Absent") 0.0 else w.wageRate
            val otCost = att.overtimeHours * (w.wageRate / 8.0) * 1.5
            base + otCost
        }
    }

    val bgBrush = if (dark)
        Brush.verticalGradient(listOf(PremiumNavy, Color(0xFF080C18), PremiumDeepBlue))
    else
        Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFFAFBFF), Color(0xFFEEF2FF)))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Ambient backdrop visual blobs
        if (dark) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset((-60).dp, (-40).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AquaGlow.copy(alpha = 0.06f), Color.Transparent)
                        ), CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.TopEnd)
                    .offset(60.dp, 80.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(VioletGlow.copy(alpha = 0.07f), Color.Transparent)
                        ), CircleShape
                    )
            )
        }

        // Animated Screen Transitions
        AnimatedContent(
            targetState = Pair(selectedTxDetail, selectedPartyDetail),
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
            },
            label = "pageRoute"
        ) { (txDetail, partyDetail) ->
            when {
                txDetail != null -> {
                    val freshTx = projectTransactions.find { it.id == txDetail.id } ?: txDetail
                    PremiumPaymentDetailPage(
                        tx = freshTx,
                        dark = dark,
                        currentProject = currentProject,
                        viewModel = viewModel,
                        context = context,
                        onBack = { selectedTxDetail = null },
                        onShowPdf = { showPdfPreviewDialog = true }
                    )
                }

                partyDetail != null -> {
                    PremiumPartyDetailPage(
                        worker = partyDetail,
                        dark = dark,
                        currentProject = currentProject,
                        projectTransactions = projectTransactions,
                        allAttendance = allAttendance,
                        viewModel = viewModel,
                        onBack = { selectedPartyDetail = null },
                        onSelectTx = { selectedTxDetail = it },
                        onIPaid = {
                            partyTxType = "Money Out"
                            partyTxCategory = "Labour"
                            partyTxAmount = ""
                            partyTxDesc = "Crew payment"
                            showAddPartyTxDialog = true
                        },
                        onIReceived = {
                            partyTxType = "Money In"
                            partyTxCategory = "Client Advance"
                            partyTxAmount = ""
                            partyTxDesc = "Received funds"
                            showAddPartyTxDialog = true
                        },
                        onAddTx = { viewModel.showTransactionDialog = true }
                    )
                }

                else -> {
                    PremiumMainPage(
                        dark = dark,
                        viewModel = viewModel,
                        currentProject = currentProject,
                        allWorkers = allWorkers,
                        projectTransactions = projectTransactions,
                        allTasks = allTasks,
                        activeProjId = activeProjId,
                        activeSiteTab = activeSiteTab,
                        tabs = tabs,
                        partySearchQuery = partySearchQuery,
                        onPartySearchChange = { partySearchQuery = it },
                        txSearchQuery = txSearchQuery,
                        onTxSearchQueryChange = { txSearchQuery = it },
                        taskSearchQuery = taskSearchQuery,
                        onTaskSearchQueryChange = { taskSearchQuery = it },
                        activeDate = activeDate,
                        allAttendance = allAttendance,
                        onShowPdf = { showPdfPreviewDialog = true },
                        filteredTransactions = filteredTransactions,
                        txDatePreset = txDatePreset,
                        onOpenDateFilter = { showTxDateFilterDialog = true },
                        onSelectParty = { selectedPartyDetail = it },
                        onSelectTx = { selectedTxDetail = it },
                        onOpenMarkDialog = {
                            selectedWorkerForAttendance = it
                            val rec = activeDateAttendance.find { r -> r.workerId == it.id }
                            inputOvertimeHours = rec?.overtimeHours?.toString() ?: "0.0"
                        },
                        onDayClick = { worker, dateStr ->
                            viewModel.attendanceDate = dateStr
                            val rec = allAttendance.find { r -> r.workerId == worker.id && r.date == dateStr && r.projectId == (currentProject?.id ?: 0) }
                            inputOvertimeHours = rec?.overtimeHours?.toString() ?: "0.0"
                            selectedWorkerForAttendance = worker
                        },
                        navigateDay = navigateDay,
                        onReturnToday = {
                            viewModel.attendanceDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        }
                    )
                }
            }
        }
    }

    // Modal Overlays
    val activeProj = currentProject
    val selectedWork = selectedWorkerForAttendance
    if (selectedWork != null && activeProj != null) {
        val record = activeDateAttendance.find { it.workerId == selectedWork.id }
        PremiumAttendanceDialog(
            worker = selectedWork,
            record = record,
            dark = dark,
            parsedDateString = parsedDateString,
            activeProj = activeProj,
            activeDate = activeDate,
            inputOvertimeHours = inputOvertimeHours,
            onOtChange = { inputOvertimeHours = it },
            onPresent = {
                viewModel.recordAttendance(selectedWork.id, activeProj.id, activeDate, "Present")
                selectedWorkerForAttendance = null
            },
            onAbsent = {
                viewModel.recordAttendance(selectedWork.id, activeProj.id, activeDate, "Absent")
                selectedWorkerForAttendance = null
            },
            onSaveOt = {
                val hrs = inputOvertimeHours.toDoubleOrNull() ?: 0.0
                viewModel.recordAttendance(
                    selectedWork.id, activeProj.id, activeDate,
                    if (hrs > 0) "Overtime" else "Present", hrs
                )
                selectedWorkerForAttendance = null
            },
            onClear = {
                viewModel.recordAttendance(selectedWork.id, activeProj.id, activeDate, "Clear")
                selectedWorkerForAttendance = null
            },
            onDismiss = { selectedWorkerForAttendance = null }
        )
    }

    val activePartyForDialog = selectedPartyDetail
    if (showAddPartyTxDialog && activePartyForDialog != null && activeProj != null) {
        PremiumAddTransactionDialog(
            dark = dark,
            partyTxType = partyTxType,
            partyTxAmount = partyTxAmount,
            partyTxDesc = partyTxDesc,
            partyTxDate = partyTxDate,
            partyTxCategory = partyTxCategory,
            partyTxMethod = partyTxMethod,
            onTypeChange = { partyTxType = it },
            onAmountChange = { partyTxAmount = it },
            onDescChange = { partyTxDesc = it },
            onDateChange = { partyTxDate = it },
            onCategoryChange = { partyTxCategory = it },
            onMethodChange = { partyTxMethod = it },
            onDismiss = { showAddPartyTxDialog = false },
            onSave = {
                val amt = partyTxAmount.toDoubleOrNull() ?: 0.0
                if (amt > 0.0) {
                    viewModel.addTransaction(
                        projectId = activeProj.id, type = partyTxType, amount = amt,
                        category = partyTxCategory, description = partyTxDesc,
                        date = partyTxDate, partyId = activePartyForDialog.id,
                        partyName = activePartyForDialog.name, reference = "SiteScreen",
                        paymentMethod = partyTxMethod
                    )
                    showAddPartyTxDialog = false
                    Toast.makeText(context, "Transaction saved!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please enter a valid amount!", Toast.LENGTH_SHORT).show()
                }
            },
            allWorkers = allWorkers,
            selectedParty = activePartyForDialog,
            viewModel = viewModel
        )
    }

    if (showTxDateFilterDialog) {
        ReportFilterDialog(
            visible = showTxDateFilterDialog,
            onDismiss = { showTxDateFilterDialog = false },
            dark = dark,
            preset = txDatePreset,
            onPresetChange = { txDatePreset = it },
            startDate = txStartDate,
            onStartDateChange = { txStartDate = it },
            endDate = txEndDate,
            onEndDateChange = { txEndDate = it },
            onApply = { showTxDateFilterDialog = false }
        )
    }

    if (showPdfPreviewDialog) {
        val txsForReport = if (selectedPartyDetail != null) {
            projectTransactions.filter { it.partyName == selectedPartyDetail!!.name }
        } else if (viewModel.activeSiteTab == "Transaction") {
            filteredTransactions
        } else {
            projectTransactions
        }

        PremiumReportPreviewDialog(
            dark = dark,
            selectedTxDetail = selectedTxDetail,
            selectedPartyDetail = selectedPartyDetail,
            projectTransactions = txsForReport,
            allWorkers = if (viewModel.activeSiteTab == "Party") allWorkers.filter { it.name.contains(partySearchQuery, ignoreCase = true) } else allWorkers,
            currentProject = currentProject,
            viewModel = viewModel,
            dateRangeText = if (selectedPartyDetail != null) "All Time" else activeDateRangeText,
            onDismiss = { showPdfPreviewDialog = false }
        )
    }
}

// ─────────────────────────────────────────────
// PAGE 1 — MAIN SCROLLABLE MULTI-TAB VIEW
// ─────────────────────────────────────────────
@Composable
private fun PremiumMainPage(
    dark: Boolean,
    viewModel: MainViewModel,
    currentProject: Project?,
    allWorkers: List<Worker>,
    projectTransactions: List<Transaction>,
    allTasks: List<Task>,
    activeProjId: Int?,
    activeSiteTab: String,
    tabs: List<String>,
    partySearchQuery: String,
    onPartySearchChange: (String) -> Unit,
    txSearchQuery: String,
    onTxSearchQueryChange: (String) -> Unit,
    taskSearchQuery: String,
    onTaskSearchQueryChange: (String) -> Unit,
    activeDate: String,
    allAttendance: List<Attendance>,
    onShowPdf: () -> Unit,
    filteredTransactions: List<Transaction>,
    txDatePreset: String,
    onOpenDateFilter: () -> Unit,
    onSelectParty: (Worker) -> Unit,
    onSelectTx: (Transaction) -> Unit,
    onOpenMarkDialog: (Worker) -> Unit,
    onDayClick: (Worker, String) -> Unit,
    navigateDay: (Int) -> Unit,
    onReturnToday: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom Header
        PremiumSiteHeader(
            dark = dark,
            currentProject = currentProject,
            viewModel = viewModel,
            context = context,
            onShowPdf = onShowPdf
        )

        // Custom Tab Bar Strip
        PremiumTabBar(
            dark = dark,
            tabs = tabs,
            activeSiteTab = activeSiteTab,
            onTabSelected = { viewModel.activeSiteTab = it }
        )

        // Dynamic Tab Loader
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = activeSiteTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    "Party" -> {
                        PartyTab(
                            workers = allWorkers,
                            searchText = partySearchQuery,
                            onSearchChange = onPartySearchChange,
                            projectTransactions = projectTransactions,
                            dark = dark,
                            onSelectWorker = onSelectParty,
                            onAddParty = { viewModel.showWorkerDialog = true }
                        )
                    }

                    "Transaction" -> {
                        TransactionTab(
                            txList = filteredTransactions,
                            searchText = txSearchQuery,
                            onSearchChange = onTxSearchQueryChange,
                            dark = dark,
                            onSelectTx = onSelectTx,
                            onAddPayment = { viewModel.showTransactionDialog = true }
                        )
                    }

                    "Site" -> {
                        SiteInfoTab(
                            currentProject = currentProject,
                            allWorkers = allWorkers,
                            projectTransactions = projectTransactions,
                            dark = dark
                        )
                    }

                    "Task" -> {
                        TaskTab(
                            tasks = allTasks,
                            currentProjectId = activeProjId,
                            searchText = taskSearchQuery,
                            onSearchChange = onTaskSearchQueryChange,
                            dark = dark,
                            onAddTask = { viewModel.showTaskDialog = true },
                            onCycleStatus = { task ->
                                viewModel.cycleTaskStatus(task)
                            }
                        )
                    }

                    "Attendance" -> {
                        AttendanceTab(
                            workers = allWorkers,
                            allAttendance = allAttendance,
                            currentProject = currentProject,
                            activeDate = activeDate,
                            dark = dark,
                            onSelectDay = onDayClick,
                            onNavigateDay = navigateDay,
                            onReturnToday = onReturnToday,
                            onOpenMarkDialog = onOpenMarkDialog
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// PREMIUM SITE HEADER
// ─────────────────────────────────────────────
@Composable
private fun PremiumSiteHeader(
    dark: Boolean,
    currentProject: Project?,
    viewModel: MainViewModel,
    context: Context,
    onShowPdf: () -> Unit
) {
    val headerBg = if (dark)
        Brush.horizontalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
    else
        Brush.horizontalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFF)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Bottom subtle visual divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AquaGlow.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { viewModel.currentScreen = AppScreen.Dashboard },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (dark) Color.White else Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsatingDot(color = EmeraldGlow)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE SITE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = EmeraldGlow
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    val projects by viewModel.projects.collectAsState()

                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { dropdownExpanded = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = currentProject?.name ?: "Select Project",
                                style = TextStyle(
                                    brush = GradientAqua,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.3).sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch Project",
                                tint = if (dark) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(if (dark) Color(0xFF0F172A) else Color.White)
                        ) {
                            projects.forEach { proj ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = proj.name,
                                            color = if (dark) Color.White else Color.Black,
                                            fontWeight = if (proj.id == currentProject?.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.selectedProjectId = proj.id
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = "Site Operations",
                        color = if (dark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp,
                        maxLines = 1
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumIconBtn(
                    icon = if (viewModel.darkThemeEnabled) Icons.Default.LightMode else Icons.Default.DarkMode,
                    tint = if (dark) AquaGlow else VioletGlow,
                    dark = dark,
                    onClick = { viewModel.darkThemeEnabled = !viewModel.darkThemeEnabled }
                )
                PremiumIconBtn(
                    icon = Icons.Default.PictureAsPdf,
                    tint = RoseGlow,
                    dark = dark,
                    onClick = onShowPdf
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// PREMIUM TAB BAR
// ─────────────────────────────────────────────
@Composable
private fun PremiumTabBar(
    dark: Boolean,
    tabs: List<String>,
    activeSiteTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabIcons = mapOf(
        "Party"       to Icons.Default.Group,
        "Transaction" to Icons.Default.AccountBalance,
        "Site"        to Icons.Default.LocationOn,
        "Task"        to Icons.Default.Assignment,
        "Attendance"  to Icons.Default.CalendarToday
    )
    val tabColors = mapOf(
        "Party"       to VioletGlow,
        "Transaction" to EmeraldGlow,
        "Site"        to AquaGlow,
        "Task"        to AmberGlow,
        "Attendance"  to IndigoGlow
    )

    val scrollState = rememberScrollState()
    val tabBg = if (dark) Color(0xFF0D1B3E) else Color(0xFFEEF2FF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(tabBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val selected = activeSiteTab == tab
                val accentColor = tabColors[tab] ?: AquaGlow
                val icon = tabIcons[tab] ?: Icons.Default.Circle

                val animAlpha by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = tween(250),
                    label = "tabAlpha"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected)
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = 0.18f), accentColor.copy(alpha = 0.08f))
                                )
                            else
                                Brush.linearGradient(
                                    listOf(
                                        if (dark) Color(0xFF1A2744).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f),
                                        if (dark) Color(0xFF111827).copy(alpha = 0.4f) else Color(0xFFF1F5FF).copy(alpha = 0.5f)
                                    )
                                )
                        )
                        .border(
                            width = if (selected) 1.2.dp else 1.dp,
                            brush = if (selected)
                                Brush.linearGradient(listOf(accentColor, accentColor.copy(alpha = 0.4f)))
                            else
                                Brush.linearGradient(
                                    listOf(
                                        if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0),
                                        if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0)
                                    )
                                ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = tab,
                            tint = if (selected) accentColor else
                                if (dark) Color(0xFF475569) else Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = tab,
                            color = if (selected) accentColor else
                                if (dark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }

        // Bottom gradient border strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    if (dark)
                        Brush.horizontalGradient(listOf(Color(0xFF1E2D4A), Color(0xFF0D1B3E)))
                    else
                        Brush.horizontalGradient(listOf(Color(0xFFDDE4F0), Color(0xFFEEF2FF)))
                )
        )
    }
}
