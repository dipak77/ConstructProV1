package com.example.ui

import android.content.Context
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TransactionTab(
    txList: List<Transaction>,
    searchText: String,
    onSearchChange: (String) -> Unit,
    dark: Boolean,
    txDatePreset: String,
    onOpenDateFilter: () -> Unit,
    onSelectTx: (Transaction) -> Unit,
    onAddPayment: () -> Unit
) {
    val totalIn  = txList.filter { it.type == "Money In" }.sumOf { it.amount }
    val totalOut = txList.filter { it.type == "Money Out" }.sumOf { it.amount }
    val net      = totalIn - totalOut

    val filteredTx = remember(txList, searchText) {
        if (searchText.isBlank()) txList
        else txList.filter {
            it.partyName?.contains(searchText, ignoreCase = true) == true ||
                    it.description.contains(searchText, ignoreCase = true) ||
                    it.reference.contains(searchText, ignoreCase = true) ||
                    it.paymentMethod.contains(searchText, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Summary Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (dark)
                        Brush.horizontalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
                    else
                        Brush.horizontalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFF)))
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniFinanceStat(
                    dark = dark, label = "IN",
                    value = formatIndianRupees(totalIn),
                    color = EmeraldGlow, modifier = Modifier.weight(1f)
                )
                MiniFinanceStat(
                    dark = dark, label = "OUT",
                    value = formatIndianRupees(totalOut),
                    color = RoseGlow, modifier = Modifier.weight(1f)
                )
                MiniFinanceStat(
                    dark = dark, label = "NET",
                    value = formatIndianRupees(kotlin.math.abs(net)),
                    color = if (net >= 0) EmeraldGlow else RoseGlow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search and filter row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PremiumSearchBar(
                    value = searchText,
                    onValueChange = onSearchChange,
                    dark = dark,
                    placeholder = "Search description, party..."
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (dark) Color(0xFF1E2D4A).copy(alpha = 0.6f) else Color(0xFFE2E8F4))
                    .border(1.dp, if (dark) Color(0xFF2D3F5E) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    .clickable { onOpenDateFilter() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Date Filter",
                    tint = EmeraldGlow,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (txDatePreset == "All") "All Dates" else txDatePreset,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dark) Color.White else Color(0xFF1E293B)
                )
            }
        }

        val txRunningBalances = remember(filteredTx) {
            val map = mutableMapOf<Int, Double>()
            var running = 0.0
            val chronological = filteredTx.sortedWith(compareBy({ it.date }, { it.id }))
            for (tx in chronological) {
                running += if (tx.type == "Money In") tx.amount else -tx.amount
                map[tx.id] = running
            }
            map
        }

        if (filteredTx.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val emptyMessage = if (searchText.isNotEmpty() || txDatePreset != "All") {
                    "No matching transactions found"
                } else {
                    "No transactions logged yet"
                }
                PremiumEmptyState(dark = dark, message = emptyMessage)
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "All Receipts  ·  ${filteredTx.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            letterSpacing = 0.3.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(filteredTx, key = { it.id }) { tx ->
                        PremiumTransactionCard(
                            tx = tx,
                            dark = dark,
                            runningBalance = txRunningBalances[tx.id],
                            onClick = { onSelectTx(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTransactionCard(
    tx: Transaction,
    dark: Boolean,
    workerName: String = "",
    runningBalance: Double? = null,
    onClick: () -> Unit
) {
    TransactionCardLayout(tx = tx, dark = dark, workerName = workerName, balanceAfter = runningBalance, onClick = onClick)
}

@Composable
fun TransactionCardLayout(
    tx: Transaction,
    dark: Boolean,
    workerName: String = "",
    balanceAfter: Double? = null,
    onClick: () -> Unit
) {
    val isIn = tx.type == "Money In"
    val dateParts = tx.date.split("-")
    val months = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    val dayStr = dateParts.getOrNull(2) ?: "27"
    val monIndex = (dateParts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
    val monStr = months.getOrElse(monIndex) { "MAY" }
    val yearStr = dateParts.getOrNull(0) ?: "2024"

    val cardBg = if (dark) Color(0xFF1E293B) else Color.White
    val cardBorder = if (dark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textPrimary = if (dark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
    val textSecondary = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val themeGreen = if (dark) NeonGreen else Color(0xFF047857)
    val themePink = if (dark) NeonPink else Color(0xFFBE123C)
    val amountColor = if (isIn) themeGreen else themePink

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Left Date Badge
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (dark) Color(0x285D53EA) else Color(0xFFF1F0FF)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayStr,
                        fontSize = 18.sp,
                        color = Color(0xFF5D53EA),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = monStr,
                        fontSize = 10.sp,
                        color = Color(0xFF5D53EA),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = yearStr,
                        fontSize = 9.sp,
                        color = Color(0xFF5D53EA).copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Middle Content Column (weight = 1f)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val partyText = tx.partyName ?: workerName.ifBlank { "Party" }
                val line1 = if (isIn) partyText else "Company"
                val line2 = if (isIn) "To: Company" else "To: $partyText"

                Text(
                    text = line1,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = line2,
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Chips Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type tag chip
                    val typeBg = if (isIn) {
                        if (dark) Color(0x2410B981) else Color(0xFFE6F4EA)
                    } else {
                        if (dark) Color(0x24EF4444) else Color(0xFFFCE8E6)
                    }
                    val typeTxtColor = if (isIn) {
                        if (dark) Color(0xFF34D399) else Color(0xFF137333)
                    } else {
                        if (dark) Color(0xFFF87171) else Color(0xFFC5221F)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isIn) "RECEIPT" else "PAYMENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeTxtColor
                        )
                    }

                    // Payment Method Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (dark) Color(0xFF334155) else Color(0xFFE8F0FE))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tx.paymentMethod,
                            fontSize = 9.sp,
                            color = if (dark) Color(0xFF90CDF4) else Color(0xFF1A73E8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (tx.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tx.description,
                        fontSize = 12.sp,
                        color = textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                val refStr = if (tx.reference.isNotBlank()) tx.reference else "TXN${tx.id}"
                Text(
                    text = "Ref No: $refStr  |  Remarks: ${tx.description.ifBlank { "None" }}",
                    fontSize = 10.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Right Content Column (horizontalAlignment = Alignment.End)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = (if (isIn) "+" else "-") + formatIndianRupees(tx.amount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                if (balanceAfter != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Balance After",
                            fontSize = 9.sp,
                            color = textSecondary
                        )
                        Text(
                            text = formatIndianRupees(balanceAfter),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Btn
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(onClick = onClick)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "View",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "View",
                            fontSize = 8.sp,
                            color = textSecondary
                        )
                    }

                    // More Btn
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(onClick = onClick)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "More",
                            fontSize = 8.sp,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumPaymentDetailPage(
    tx: Transaction,
    dark: Boolean,
    currentProject: Project?,
    viewModel: MainViewModel,
    context: Context,
    onBack: () -> Unit,
    onShowPdf: () -> Unit
) {
    val amountStr  = formatIndianRupees(tx.amount)
    val isMoneyIn  = tx.type == "Money In"
    val accentColor = if (isMoneyIn) EmeraldGlow else RoseGlow
    val accentGrad  = if (isMoneyIn) GradientEmerald else GradientRose

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
            title = "Payment Receipt",
            subtitle = "by ${viewModel.userSession.value?.displayName ?: "Tejas Harane"}",
            onBack = onBack,
            actions = {
                PremiumIconBtn(
                    icon = Icons.Default.PictureAsPdf, tint = accentColor, dark = dark,
                    onClick = onShowPdf
                )
                PremiumIconBtn(
                    icon = Icons.Default.Share,
                    tint = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B), dark = dark,
                    onClick = {
                        val amountStr = formatIndianRupees(tx.amount)
                        val pdfFile = PdfUtils.generateReceiptPdfFile(
                            context = context,
                            txId = tx.id,
                            name = tx.partyName ?: "Company",
                            amount = amountStr,
                            date = tx.date,
                            paymentMethod = tx.paymentMethod,
                            remark = tx.description,
                            isMoneyIn = tx.type == "Money In",
                            projectName = currentProject?.name ?: "Treasure garden",
                            siteAddress = currentProject?.location ?: "Treasure Garden Site, India"
                        )
                        PdfUtils.sharePdfFile(context, pdfFile, "Share Receipt")
                    }
                )
                PremiumIconBtn(
                    icon = Icons.Default.Edit,
                    tint = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B), dark = dark,
                    onClick = {
                        viewModel.transactionToEdit = tx
                        viewModel.transactionTypePreset = tx.type
                        viewModel.showTransactionDialog = true
                    }
                )
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Hero
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor.copy(alpha = 0.15f), accentColor.copy(alpha = 0.04f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(accentColor.copy(alpha = 0.7f), accentColor.copy(alpha = 0.1f))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isMoneyIn) "MONEY RECEIVED" else "MONEY PAID OUT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = accentColor.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = amountStr,
                                style = TextStyle(
                                    brush = accentGrad,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(EmeraldGlow, CircleShape)
                                )
                                Text(
                                    "Authenticated & Sealed",
                                    fontSize = 10.sp,
                                    color = EmeraldGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                                .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMoneyIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Detail Rows
            item {
                PremiumDetailCard(
                    dark = dark,
                    items = listOf(
                        Triple(Icons.Default.Person, "To",
                            if (!isMoneyIn) (tx.partyName ?: "Staff") else "Company"),
                        Triple(Icons.Default.PersonOutline, "From",
                            if (isMoneyIn) (tx.partyName ?: "Client") else "Company"),
                        Triple(Icons.Default.CalendarToday, "Date", tx.date),
                        Triple(Icons.Default.LocationOn, "Project",
                            currentProject?.name ?: "Treasure Garden"),
                        Triple(Icons.Default.AccountBalance, "Method", tx.paymentMethod),
                        Triple(Icons.Default.Notes, "Description", tx.description)
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Bottom CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GradientAqua)
                    .clickable(onClick = onShowPdf)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Text("VIEW PDF", fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = Color.White, letterSpacing = 0.5.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(VioletGlow, VioletGlow.copy(alpha = 0.3f))),
                        RoundedCornerShape(14.dp)
                    )
                    .background(VioletGlow.copy(alpha = 0.1f))
                    .clickable {
                        val pdfFile = PdfUtils.generateReceiptPdfFile(
                            context = context,
                            txId = tx.id,
                            name = tx.partyName ?: "Company",
                            amount = amountStr,
                            date = tx.date,
                            paymentMethod = tx.paymentMethod,
                            remark = tx.description,
                            isMoneyIn = tx.type == "Money In",
                            projectName = currentProject?.name ?: "Treasure garden",
                            siteAddress = currentProject?.location ?: "Treasure Garden Site, India"
                        )
                        PdfUtils.sharePdfFile(context, pdfFile, "Share Receipt PDF")
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Share, null, tint = VioletGlow, modifier = Modifier.size(15.dp))
                    Text("SHARE PDF", fontSize = 11.sp, fontWeight = FontWeight.Black,
                        color = VioletGlow, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
fun PremiumAddTransactionDialog(
    dark: Boolean,
    partyTxType: String,
    partyTxAmount: String,
    partyTxDesc: String,
    partyTxDate: String,
    partyTxCategory: String,
    partyTxMethod: String,
    onTypeChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onMethodChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    allWorkers: List<Worker>,
    selectedParty: Worker?,
    viewModel: MainViewModel
) {
    var reference by remember { mutableStateOf("") }
    GlassModalDialog(
        visible = true, onDismiss = onDismiss,
        title = "Record Payment",
        darkTheme = dark,
        glowColor = if (partyTxType == "Money Out") RoseGlow else EmeraldGlow,
        scrollable = true
    ) {
        UnifiedTransactionFormContent(
            dark = dark,
            type = partyTxType,
            onTypeChange = onTypeChange,
            allWorkers = allWorkers,
            selectedParty = selectedParty,
            onPartySelected = {},
            amountStr = partyTxAmount,
            onAmountChange = onAmountChange,
            category = partyTxCategory,
            onCategoryChange = onCategoryChange,
            description = partyTxDesc,
            onDescriptionChange = onDescChange,
            reference = reference,
            onReferenceChange = { reference = it },
            paymentMethod = partyTxMethod,
            onPaymentMethodChange = onMethodChange,
            date = partyTxDate,
            onDateChange = onDateChange,
            onSave = onSave,
            onCancel = onDismiss,
            isPartyLocked = true,
            viewModel = viewModel
        )
    }
}
