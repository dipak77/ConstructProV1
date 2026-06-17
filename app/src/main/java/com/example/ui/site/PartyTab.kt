package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

@Composable
fun PartyTab(
    workers: List<Worker>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    projectTransactions: List<Transaction>,
    dark: Boolean,
    onSelectWorker: (Worker) -> Unit,
    onAddParty: () -> Unit
) {
    val totalAdvance = workers.sumOf { w ->
        val txs = projectTransactions.filter { it.partyId == w.id || it.partyName == w.name }
        val d = txs.filter { it.type == "Money Out" }.sumOf { it.amount } -
                txs.filter { it.type == "Money In" }.sumOf { it.amount }
        if (d > 0) d else 0.0
    }
    val totalPending = workers.sumOf { w ->
        val txs = projectTransactions.filter { it.partyId == w.id || it.partyName == w.name }
        val d = txs.filter { it.type == "Money Out" }.sumOf { it.amount } -
                txs.filter { it.type == "Money In" }.sumOf { it.amount }
        if (d < 0) -d else 0.0
    }

    val filteredWorkers = remember(workers, searchText) {
        if (searchText.isBlank()) workers
        else workers.filter { it.name.contains(searchText, ignoreCase = true) || it.role.contains(searchText, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumStatCard(
                    modifier = Modifier.weight(1f),
                    dark = dark,
                    label = "PAID",
                    value = formatIndianRupees(totalAdvance),
                    valueColor = RoseGlow,
                    icon = Icons.Default.TrendingUp,
                    gradient = Brush.linearGradient(
                        listOf(RoseGlow.copy(alpha = 0.15f), RoseGlow.copy(alpha = 0.05f))
                    ),
                    borderColor = RoseGlow.copy(alpha = 0.3f)
                )
                PremiumStatCard(
                    modifier = Modifier.weight(1f),
                    dark = dark,
                    label = "RECEIVED",
                    value = formatIndianRupees(totalPending),
                    valueColor = EmeraldGlow,
                    icon = Icons.Default.TrendingDown,
                    gradient = Brush.linearGradient(
                        listOf(EmeraldGlow.copy(alpha = 0.15f), EmeraldGlow.copy(alpha = 0.05f))
                    ),
                    borderColor = EmeraldGlow.copy(alpha = 0.3f)
                )
            }
        }

        // Team Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${workers.size} Team Members",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B)
                    )
                    Text(
                        text = "Tap member for balance details",
                        fontSize = 11.sp,
                        color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(VioletGlow.copy(alpha = 0.12f))
                        .border(1.dp, VioletGlow.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onAddParty)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Add Party →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VioletGlow
                    )
                }
            }
        }

        // Search Bar
        item {
            PremiumSearchBar(
                value = searchText,
                onValueChange = onSearchChange,
                dark = dark,
                placeholder = "Search worker or role..."
            )
        }

        // Filter Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AquaGlow.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = AquaGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Filter & Sort",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaGlow
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = AquaGlow,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = "${filteredWorkers.size} results",
                    fontSize = 11.sp,
                    color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                )
            }
        }

        val partyDiffs = filteredWorkers.associateWith { wrk ->
            val rx = projectTransactions.filter { it.partyName == wrk.name && it.type == "Money In" }.sumOf { it.amount }
            val tx = projectTransactions.filter { it.partyName == wrk.name && it.type == "Money Out" }.sumOf { it.amount }
            rx - tx
        }

        if (filteredWorkers.isEmpty()) {
            item { PremiumEmptyState(dark = dark, message = "No matching workers found") }
        } else {
            items(filteredWorkers, key = { it.id }) { worker ->
                val diff = partyDiffs[worker] ?: 0.0
                PremiumPartyCard(
                    worker = worker,
                    diff = diff,
                    dark = dark,
                    onClick = { onSelectWorker(worker) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun PremiumPartyCard(
    worker: Worker,
    diff: Double,
    dark: Boolean,
    onClick: () -> Unit
) {
    val isPositive = diff >= 0
    val labelText = if (isPositive) "Paid" else "Received"
    val accentColor = if (isPositive) RoseGlow else EmeraldGlow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (dark)
                    Brush.horizontalGradient(listOf(Color(0xFF111827), Color(0xFF0D1B3E)))
                else
                    Brush.horizontalGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(accentColor.copy(alpha = 0.2f), if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0))
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(worker.avatarColor), Color(worker.avatarColor).copy(alpha = 0.6f))
                            ), CircleShape
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        worker.name.take(2).uppercase(),
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        worker.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B)
                    )
                    Text(
                        worker.role, fontSize = 11.sp,
                        color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatIndianRupees(diff.absoluteValue),
                    fontSize = 15.sp, fontWeight = FontWeight.Black, color = accentColor
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = labelText,
                        fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumPartyDetailPage(
    worker: Worker,
    dark: Boolean,
    currentProject: Project?,
    projectTransactions: List<Transaction>,
    allAttendance: List<Attendance>,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onAddTx: () -> Unit,
    onIPaid: () -> Unit,
    onIReceived: () -> Unit,
    onSelectTx: (Transaction) -> Unit
) {
    val matchedTxs = remember(projectTransactions, worker.name) {
        projectTransactions.filter { it.partyName == worker.name }
    }

    val totalReceived = remember(matchedTxs) {
        matchedTxs.filter { it.type == "Money In" }.sumOf { it.amount }
    }
    val totalPaid = remember(matchedTxs) {
        matchedTxs.filter { it.type == "Money Out" }.sumOf { it.amount }
    }
    val totalBalance = totalReceived - totalPaid
    val balColor = if (totalBalance >= 0) EmeraldGlow else RoseGlow

    var selectedHistoryTab by remember { mutableStateOf("All") }
    var selectedTimeFilter by remember { mutableStateOf("All Time") }
    var showTimeFilterMenu by remember { mutableStateOf(false) }

    // Toggle between Ledger & Calendar View
    var detailSectionTab by remember { mutableStateOf("Ledger") }

    // Attendance Calendar Month State
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance(Locale.US)) }

    val historyTxs = remember(matchedTxs, selectedHistoryTab, selectedTimeFilter) {
        var base = when (selectedHistoryTab) {
            "Received" -> matchedTxs.filter { it.type == "Money In" }
            "Paid" -> matchedTxs.filter { it.type == "Money Out" }
            else -> matchedTxs
        }

        if (selectedTimeFilter != "All Time") {
            val filterRange = getPresetDateRange(selectedTimeFilter)
            if (filterRange != null) {
                base = base.filter { it.date >= filterRange.first && it.date <= filterRange.second }
            }
        }

        base.sortedByDescending { it.date }
    }

    val receivedCount = remember(matchedTxs) { matchedTxs.count { it.type == "Money In" } }
    val paidCount     = remember(matchedTxs) { matchedTxs.count { it.type == "Money Out" } }

    val bgBrush = if (dark)
        Brush.verticalGradient(listOf(PremiumNavy, Color(0xFF080C18)))
    else
        Brush.verticalGradient(listOf(Color(0xFFF0F4FF), Color(0xFFFAFBFF)))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        PremiumPageHeader(
            dark = dark,
            title = worker.name,
            subtitle = worker.role,
            onBack = onBack,
            actions = {
                HeaderActionButton(icon = Icons.Default.Call, dark = dark, onClick = {})
                HeaderActionButton(icon = Icons.Default.Share, dark = dark, onClick = {})
            }
        )

        // Hero Card (Party Balance)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (dark)
                        Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B)))
                    else
                        Brush.linearGradient(listOf(Color.White, Color(0xFFF1F5F9)))
                )
                .border(
                    1.dp,
                    if (dark) Color(0xFF2D3F5E) else Color(0xFFE2E8F0),
                    RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (totalBalance >= 0) "YOU WILL RECEIVE FROM PARTY" else "YOU OWE TO PARTY",
                    fontSize = 10.sp,
                    color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatIndianRupees(totalBalance.absoluteValue),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = balColor
                )
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(if (dark) Color(0xFF2D3F5E) else Color(0xFFE2E8F0))
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Party Received
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Party Received",
                            fontSize = 12.sp,
                            color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatIndianRupees(totalReceived),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(if (dark) Color(0xFF2D3F5E) else Color(0xFFE2E8F0))
                    )

                    // Right Column: Party Paid
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Party Paid",
                            fontSize = 12.sp,
                            color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatIndianRupees(totalPaid),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }

        // Switching Tabs: "Ledger Ledger" VS "Attendance Calendar"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (dark) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("Ledger" to "Transactions Ledger", "Attendance" to "Attendance Calendar").forEach { (tabKey, tabLabel) ->
                val isSelected = detailSectionTab == tabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                if (dark) Color(0xFF1E293B) else Color.White
                            } else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) {
                                if (dark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            } else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { detailSectionTab = tabKey }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            if (dark) Color.White else Color(0xFF4F46E5)
                        } else {
                            if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (detailSectionTab == "Ledger") {
            // LEDGER VIEW
            // 3-way Horizontal Tab Bar Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    "All" to "All Transactions",
                    "Received" to "Received ($receivedCount)",
                    "Paid" to "Paid ($paidCount)"
                )

                tabs.forEach { (tabKey, tabLabel) ->
                    val isActive = selectedHistoryTab == tabKey
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedHistoryTab = tabKey }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tabLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) {
                                if (dark) Color.White else Color(0xFF5D53EA)
                            } else {
                                if (dark) Color(0xFF64748B) else Color(0xFF94A3B8)
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    if (isActive) {
                                        if (dark) Color(0xFF818CF8) else Color(0xFF5D53EA)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (dark) Color(0xFF1E2D4A) else Color(0xFFE2E8F0))
            )

            // Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { showTimeFilterMenu = true }.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Filter: $selectedTimeFilter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                    DropdownMenu(
                        expanded = showTimeFilterMenu,
                        onDismissRequest = { showTimeFilterMenu = false },
                        modifier = Modifier.background(if (dark) Color(0xFF1E293B) else Color.White)
                    ) {
                        val opts = listOf("All Time", "Today", "Weekly", "2 Weeks", "3 Weeks", "Monthly")
                        opts.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt, color = if (dark) Color.White else Color.Black) },
                                onClick = {
                                    selectedTimeFilter = opt
                                    showTimeFilterMenu = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Amount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val partyTxRunningBalances = remember(matchedTxs) {
                    val map = mutableMapOf<Int, Double>()
                    var running = 0.0
                    val chronological = matchedTxs.sortedWith(compareBy({ it.date }, { it.id }))
                    for (tx in chronological) {
                        running += if (tx.type == "Money In") tx.amount else -tx.amount
                        map[tx.id] = running
                    }
                    map
                }

                if (historyTxs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PremiumEmptyState(
                            dark = dark,
                            message = if (selectedHistoryTab == "Received") "No content received" else "No paid transactions"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(historyTxs, key = { it.id }) { tx ->
                            PartyTransactionCard(
                                tx = tx,
                                dark = dark,
                                workerName = worker.name,
                                runningBalance = partyTxRunningBalances[tx.id],
                                onClick = { onSelectTx(tx) }
                            )
                        }
                    }
                }

                // Bottom Action Bar overlaid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    if (dark) PremiumNavy.copy(alpha = 0.95f) else Color(0xFAF0F4FF)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD11A5B))
                                .clickable(onClick = onIPaid),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "I Paid",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5D53EA))
                                .clickable(onClick = onAddTx),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00897B))
                                .clickable(onClick = onIReceived),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "I Received",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // GORGEOUS CALENDAR ATTENDANCE GRID VIEW
            val dbFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
            val yearMonthHeader = remember(calendarMonth) {
                SimpleDateFormat("MMMM yyyy", Locale.US).format(calendarMonth.time)
            }

            // Calculation of Days for Calendar Grid
            val calendarCells = remember(calendarMonth) {
                val cal = calendarMonth.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // SUNDAY = 1, MONDAY = 2...
                // Align so MONDAY = index 0:
                val offset = if (startDayOfWeek == Calendar.SUNDAY) 6 else startDayOfWeek - Calendar.MONDAY

                val cellsList = mutableListOf<CalendarDayCell>()
                // Add blank/offset cells
                for (i in 0 until offset) {
                    cellsList.add(CalendarDayCell(dayNumber = 0, dateString = "", isCurrentMonth = false))
                }
                // Add actual days
                for (day in 1..totalDays) {
                    val dayCal = calendarMonth.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                    val dateStr = dbFormatter.format(dayCal.time)
                    cellsList.add(CalendarDayCell(dayNumber = day, dateString = dateStr, isCurrentMonth = true))
                }
                cellsList
            }

            // Filter Attendance Records of this specific worker inside this month
            val monthlyStats = remember(allAttendance, worker.id, calendarMonth, currentProject) {
                val cal = calendarMonth.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startStr = dbFormatter.format(cal.time)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endStr = dbFormatter.format(cal.time)

                val workerAttr = allAttendance.filter {
                    it.workerId == worker.id &&
                            it.projectId == (currentProject?.id ?: 0) &&
                            it.date >= startStr &&
                            it.date <= endStr
                }

                val present = workerAttr.count { it.status == "Present" }
                val absent = workerAttr.count { it.status == "Absent" }
                val overtimeDays = workerAttr.count { it.status == "Overtime" }
                val overtimeHours = workerAttr.filter { it.status == "Overtime" }.sumOf {
                    it.overtimeHours
                }

                MonthlySummaryStats(presentCount = present, absentCount = absent, overtimesCount = overtimeDays, overtimeHours = overtimeHours)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calendar Controller (Month Selector Card)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (dark) Color(0xFF111827) else Color.White)
                        .border(1.dp, if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val nextConfig = calendarMonth.clone() as Calendar
                                nextConfig.add(Calendar.MONTH, -1)
                                calendarMonth = nextConfig
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Prev Month", tint = AquaGlow)
                        }

                        Text(
                            text = yearMonthHeader.uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (dark) Color.White else Color(0xFF0F172A),
                            letterSpacing = 1.sp
                        )

                        IconButton(
                            onClick = {
                                val nextConfig = calendarMonth.clone() as Calendar
                                nextConfig.add(Calendar.MONTH, 1)
                                calendarMonth = nextConfig
                            }
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Month", tint = AquaGlow)
                        }
                    }
                }

                // Grid Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (dark) Color(0xFF111827) else Color.White)
                        .border(1.dp, if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Weekday headers: Mon - Sun
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(if (dark) Color(0xFF1E2D4A) else Color(0xFFE2E8F0))
                        )

                        // Render Rows of 7
                        val totalCells = calendarCells.size
                        val rowsCount = (totalCells + 6) / 7

                        for (r in 0 until rowsCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val cell = calendarCells.getOrNull(cellIndex)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cell != null && cell.isCurrentMonth) {
                                            // Search record
                                            val record = allAttendance.find {
                                                it.workerId == worker.id &&
                                                        it.date == cell.dateString &&
                                                        it.projectId == (currentProject?.id ?: 0)
                                            }

                                            val (statusColor, badgeIcon, bgAlpha) = when (record?.status) {
                                                "Present" -> Triple(EmeraldGlow, Icons.Default.Check, 0.16f)
                                                "Absent" -> Triple(RoseGlow, Icons.Default.Close, 0.16f)
                                                "Overtime" -> Triple(VioletGlow, Icons.Default.TrendingUp, 0.16f)
                                                else -> Triple(Color.Transparent, null, 0.0f)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                                    .background(if (badgeIcon != null) statusColor.copy(alpha = bgAlpha) else Color.Transparent)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (badgeIcon != null) statusColor.copy(alpha = 0.5f) else {
                                                            if (dark) Color(0xFF1A2744) else Color(0xFFEEF2FF)
                                                        },
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = cell.dayNumber.toString(),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (badgeIcon != null) {
                                                            if (dark) Color.White else statusColor
                                                        } else {
                                                            if (dark) Color(0xFF94A3B8) else Color(0xFF1E293B)
                                                        }
                                                    )
                                                    if (badgeIcon != null) {
                                                        Icon(
                                                            imageVector = badgeIcon,
                                                            contentDescription = null,
                                                            tint = statusColor,
                                                            modifier = Modifier.size(8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Legend Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (dark) Color(0xFF111827) else Color.White)
                        .border(1.dp, if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(dark = dark, icon = Icons.Default.Check, label = "Present", color = EmeraldGlow)
                        LegendItem(dark = dark, icon = Icons.Default.Close, label = "Absent", color = RoseGlow)
                        LegendItem(dark = dark, icon = Icons.Default.TrendingUp, label = "Overtime", color = VioletGlow)
                    }
                }

                // statistics list summaries for selected month
                Text(
                    text = "${yearMonthHeader.uppercase()} MONTHLY STATISTICS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttendanceStatBadge(
                        dark = dark,
                        label = "PRESENT DAYS",
                        value = "${monthlyStats.presentCount} Days",
                        color = EmeraldGlow,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceStatBadge(
                        dark = dark,
                        label = "ABSENT DAYS",
                        value = "${monthlyStats.absentCount} Days",
                        color = RoseGlow,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AttendanceStatBadge(
                        dark = dark,
                        label = "OVERTIME COMPLETED",
                        value = "${monthlyStats.overtimesCount} Days",
                        color = VioletGlow,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceStatBadge(
                        dark = dark,
                        label = "OVERTIME HOURS",
                        value = "${monthlyStats.overtimeHours} Hrs",
                        color = VioletGlow,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun PartyTransactionCard(
    tx: Transaction,
    dark: Boolean,
    workerName: String,
    runningBalance: Double? = null,
    onClick: () -> Unit
) {
    TransactionCardLayout(tx = tx, dark = dark, workerName = workerName, balanceAfter = runningBalance, onClick = onClick)
}

data class CalendarDayCell(
    val dayNumber: Int,
    val dateString: String,
    val isCurrentMonth: Boolean
)

data class MonthlySummaryStats(
    val presentCount: Int,
    val absentCount: Int,
    val overtimesCount: Int,
    val overtimeHours: Double
)
