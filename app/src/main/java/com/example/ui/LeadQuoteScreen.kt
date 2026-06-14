package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Estimate
import com.example.data.Lead
import com.example.data.Project
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LeadQuoteScreen(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val context = LocalContext.current
    val isDark = viewModel.darkThemeEnabled
    val currencyFormatter = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

    // State flows
    val leadsState by viewModel.leads.collectAsState()
    val estimateState by viewModel.estimates.collectAsState()
    val projectsState by viewModel.projects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()

    // Tab state
    var selectedTab by remember { mutableStateOf("Leads") } // "Leads" or "Quotes"

    // Search & Filter
    var leadSearch by remember { mutableStateOf("") }
    var leadStatusFilter by remember { mutableStateOf("All") } // "All", "New", "Contacted", "Quoted", "Converted", "Lost"
    var quoteSearch by remember { mutableStateOf("") }

    // Dialog state targets
    var showAddLeadDialog by remember { mutableStateOf(false) }
    var leadToEdit by remember { mutableStateOf<Lead?>(null) }
    var showAddEstimateDialog by remember { mutableStateOf(false) }
    var estimateToEdit by remember { mutableStateOf<Estimate?>(null) }

    // Background gradients matching modern light/dark themes
    val surfaceBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF1E1E2C), Color(0xFF121218))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF6F8FA), Color(0xFFEEF1F6))
        )
    }

    Scaffold(
        topBar = {
            Column {
                // Top Bar with premium design
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Commercial Desk",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Drawer"
                            )
                        }
                    },
                    actions = {
                        if (selectedTab == "Leads") {
                            IconButton(onClick = { showAddLeadDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "New Lead",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            if (activeProject != null) {
                                IconButton(onClick = { showAddEstimateDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Estimate Item"
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )

                // High Contrast Custom Glassmatic Navigation Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(
                            color = if (isDark) Color(0xFF282836) else Color(0xFFE2E6EC),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabModifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))

                    // Tab 1: Leads
                    Box(
                        modifier = tabModifier
                            .background(
                                color = if (selectedTab == "Leads") {
                                    if (isDark) Color(0xFF38384C) else Color.White
                                } else Color.Transparent
                            )
                            .clickable { selectedTab = "Leads" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedTab == "Leads") MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LEADS & PIPELINE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selectedTab == "Leads") {
                                    MaterialTheme.colorScheme.onSurface
                                } else Color.Gray
                            )
                        }
                    }

                    // Tab 2: Estimates
                    Box(
                        modifier = tabModifier
                            .background(
                                color = if (selectedTab == "Quotes") {
                                    if (isDark) Color(0xFF38384C) else Color.White
                                } else Color.Transparent
                            )
                            .clickable { selectedTab = "Quotes" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = null,
                                tint = if (selectedTab == "Quotes") MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PROJECT QUOTES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selectedTab == "Quotes") {
                                    MaterialTheme.colorScheme.onSurface
                                } else Color.Gray
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceBrush)
                .padding(innerPadding)
        ) {
            if (selectedTab == "Leads") {
                // Leads Tab rendering
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Pipeline Metrics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val activeLeads = leadsState.filter { it.status != "Lost" && it.status != "Converted" }
                        val potentialValue = activeLeads.sumOf { it.budget }

                        // Stat 1: Pipe Value
                        Card(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF242433) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ACTIVE PIPELINE VALUE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "₹${currencyFormatter.format(potentialValue)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                            }
                        }

                        // Stat 2: Pipeline volume
                        Card(
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF242433) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ACTIVE LEADS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${activeLeads.size} Clients",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Search & Level Filters
                    OutlinedTextField(
                        value = leadSearch,
                        onValueChange = { leadSearch = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        placeholder = { Text("Search prospective clients...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        singleLine = true
                    )

                    // Pipeline Stages Filter Scroll Tab
                    val stages = listOf("All", "New", "Contacted", "Quoted", "Converted", "Lost")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stages.forEach { stage ->
                            val isSelected = leadStatusFilter == stage
                            FilterChip(
                                selected = isSelected,
                                onClick = { leadStatusFilter = stage },
                                label = { Text(stage, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // Prospective leads list
                    val filteredLeads = leadsState.filter { lead ->
                        (leadStatusFilter == "All" || lead.status == leadStatusFilter) &&
                        (lead.clientName.contains(leadSearch, ignoreCase = true) ||
                         lead.projectType.contains(leadSearch, ignoreCase = true) ||
                         lead.notes.contains(leadSearch, ignoreCase = true))
                    }

                    if (filteredLeads.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No prospective leads found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Create leads to track potential jobs & quotes",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredLeads) { lead ->
                                LeadItemCard(
                                    lead = lead,
                                    isDark = isDark,
                                    currencyFmt = currencyFormatter,
                                    onClick = { leadToEdit = lead },
                                    onConvert = { viewModel.convertLeadToProject(lead) },
                                    onDelete = { viewModel.deleteLead(lead) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Project Quotations & Estimate Items
                Column(modifier = Modifier.fillMaxSize()) {
                    // Project Picker Header Area
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF242433) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "ACTIVE ESTIMATION PROJECT PROFILE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom Dropdown Box selector for active target projects
                            var expandedProjSelector by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isDark) Color(0xFF1E1E28) else Color(0xFFEEF1F6),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { expandedProjSelector = true }
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = activeProject?.name ?: "Select a project...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (activeProject != null) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown"
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandedProjSelector,
                                    onDismissRequest = { expandedProjSelector = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    projectsState.forEach { project ->
                                        DropdownMenuItem(
                                            text = { Text(project.name, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                viewModel.selectedProjectId = project.id
                                                expandedProjSelector = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (activeProject != null) {
                                val proj = activeProject!!
                                val itemsForProject = estimateState.filter { it.projectId == proj.id }
                                val totalCost = itemsForProject.sumOf { it.totalCost }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "ESTIMATION TOTAL COST",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "₹${currencyFormatter.format(totalCost)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                        )
                                    }

                                    // Print / Export Quote Button
                                    Button(
                                        onClick = {
                                            try {
                                                val file = PdfUtils.generateQuotationReportPdfFile(
                                                    context = context,
                                                    projectName = proj.name,
                                                    siteAddress = proj.location,
                                                    generatedBy = viewModel.userSession.value?.displayName ?: "Commercial Desk",
                                                    estimates = itemsForProject
                                                )
                                                PdfUtils.sharePdfFile(context, file, "Share Project Quotation")
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PDF BID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Custom Ceiling comparison warning
                                if (proj.budget > 0.0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val pct = (totalCost / proj.budget).coerceAtMost(1.0)
                                    LinearProgressIndicator(
                                        progress = pct.toFloat(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape),
                                        color = if (totalCost > proj.budget) Color.Red else MaterialTheme.colorScheme.primary,
                                        trackColor = if (isDark) Color(0xFF2E2E38) else Color(0xFFEEF1F6)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Estimates consume ${(pct * 100).toInt()}% of limits",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Cap limit: ₹${currencyFormatter.format(proj.budget)}",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quotations line items
                    if (activeProject == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Create a project first",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Estimations must be organized under a project profile",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    } else {
                        val proj = activeProject!!
                        val itemsForProject = estimateState.filter { it.projectId == proj.id }

                        if (itemsForProject.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No quotation items listed",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap actions or add estimate items to build quotation proposals",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddEstimateDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Estimate Item")
                                }
                            }
                        } else {
                            // Listing of estimate quotation items
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(itemsForProject) { item ->
                                    EstimateItemCard(
                                        item = item,
                                        isDark = isDark,
                                        currencyFmt = currencyFormatter,
                                        onClick = { estimateToEdit = item },
                                        onDelete = { viewModel.deleteEstimate(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── DIALOG 1: ADD LEAD DIALOG ──
    if (showAddLeadDialog) {
        AddEditLeadDialog(
            lead = null,
            isDark = isDark,
            onDismiss = { showAddLeadDialog = false },
            onConfirm = { client, phone, email, pType, budgetVal, notes ->
                viewModel.addLead(client, phone, email, pType, budgetVal, notes)
                showAddLeadDialog = false
            }
        )
    }

    // ── DIALOG 2: EDIT LEAD DIALOG ──
    if (leadToEdit != null) {
        AddEditLeadDialog(
            lead = leadToEdit,
            isDark = isDark,
            onDismiss = { leadToEdit = null },
            onConfirm = { client, phone, email, pType, budgetVal, notes ->
                val prev = leadToEdit!!
                viewModel.updateLead(
                    prev.copy(
                        clientName = client,
                        clientPhone = phone,
                        clientEmail = email,
                        projectType = pType,
                        budget = budgetVal,
                        notes = notes
                    )
                )
                leadToEdit = null
            }
        )
    }

    // ── DIALOG 3: ADD ESTIMATE LINE ITEM ──
    if (showAddEstimateDialog) {
        AddEditEstimateDialog(
            estimate = null,
            isDark = isDark,
            onDismiss = { showAddEstimateDialog = false },
            onConfirm = { name, qty, unit, rate ->
                activeProject?.id?.let { pId ->
                    viewModel.addEstimate(pId, name, qty, unit, rate)
                }
                showAddEstimateDialog = false
            }
        )
    }

    // ── DIALOG 4: EDIT ESTIMATE LINE ITEM ──
    if (estimateToEdit != null) {
        AddEditEstimateDialog(
            estimate = estimateToEdit,
            isDark = isDark,
            onDismiss = { estimateToEdit = null },
            onConfirm = { name, qty, unit, rate ->
                val prev = estimateToEdit!!
                viewModel.updateEstimate(
                    prev.copy(
                        itemName = name,
                        quantity = qty,
                        unit = unit,
                        rate = rate,
                        totalCost = qty * rate
                    )
                )
                estimateToEdit = null
            }
        )
    }
}

@Composable
fun LeadItemCard(
    lead: Lead,
    isDark: Boolean,
    currencyFmt: NumberFormat,
    onClick: () -> Unit,
    onConvert: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (lead.status) {
        "New" -> Color(0xFF2196F3)
        "Contacted" -> Color(0xFFFF9800)
        "Quoted" -> Color(0xFF9C27B0)
        "Converted" -> Color(0xFF4CAF50)
        "Lost" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF222230) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pipeline Status Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lead.projectType.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Render Badge
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lead.status.uppercase(),
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Client Name & Details
            Text(
                text = lead.clientName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Phone
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = lead.clientPhone,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (lead.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lead.notes,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = if (isDark) Color(0xFF323242) else Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            // Footing block: budget + Conversion action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "CLIENT ESTIMATION BUDGET", fontSize = 9.sp, color = Color.Gray)
                    Text(
                        text = "₹${currencyFmt.format(lead.budget)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Show conversion action if applicable
                if (lead.status != "Converted") {
                    Button(
                        onClick = onConvert,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Convert Project",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Active Project", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EstimateItemCard(
    item: Estimate,
    isDark: Boolean,
    currencyFmt: NumberFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF222230) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.quantity} ${item.unit} × ₹${currencyFmt.format(item.rate)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${currencyFmt.format(item.totalCost)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLeadDialog(
    lead: Lead?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (client: String, phone: String, email: String, projectType: String, budget: Double, notes: String) -> Unit
) {
    var clientName by remember { mutableStateOf(lead?.clientName ?: "") }
    var phone by remember { mutableStateOf(lead?.clientPhone ?: "") }
    var email by remember { mutableStateOf(lead?.clientEmail ?: "") }
    var projectType by remember { mutableStateOf(lead?.projectType ?: "") }
    var budgetStr by remember { mutableStateOf(lead?.budget?.toString() ?: "") }
    var notes by remember { mutableStateOf(lead?.notes ?: "") }

    val contentBg = if (isDark) Color(0xFF242434) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(contentBg, RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF38384C) else Color(0xFFE2E6EC),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (lead == null) "Insert Prospection Lead" else "Update Lead Profile",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Client Handphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Client Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                // Selectable type of execution project
                OutlinedTextField(
                    value = projectType,
                    onValueChange = { projectType = it },
                    label = { Text("Execution Type (e.g. Commercial, Villa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Budget Cap Estimate (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Prospection Notes") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DIMISS")
                    }

                    Button(
                        onClick = {
                            val budgetVal = budgetStr.toDoubleOrNull() ?: 0.0
                            onConfirm(clientName, phone, email, projectType, budgetVal, notes)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEstimateDialog(
    estimate: Estimate?,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, qty: Double, unit: String, rate: Double) -> Unit
) {
    var itemName by remember { mutableStateOf(estimate?.itemName ?: "") }
    var qtyStr by remember { mutableStateOf(estimate?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(estimate?.unit ?: "") }
    var rateStr by remember { mutableStateOf(estimate?.rate?.toString() ?: "") }

    val contentBg = if (isDark) Color(0xFF242434) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(contentBg, RoundedCornerShape(16.dp))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0xFF38384C) else Color(0xFFE2E6EC),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (estimate == null) "New Estimate Line" else "Modify Estimate Item",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Line Item Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        placeholder = { Text("Bags, SqFt") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = { rateStr = it },
                    label = { Text("Unit Rate Pricing (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DIMISS")
                    }

                    Button(
                        onClick = {
                            val qtyVal = qtyStr.toDoubleOrNull() ?: 0.0
                            val rateVal = rateStr.toDoubleOrNull() ?: 0.0
                            onConfirm(itemName, qtyVal, unit, rateVal)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}
