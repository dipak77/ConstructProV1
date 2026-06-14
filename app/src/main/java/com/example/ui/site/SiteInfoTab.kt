package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*

@Composable
fun SiteInfoTab(
    currentProject: Project?,
    allWorkers: List<Worker>,
    projectTransactions: List<Transaction>,
    dark: Boolean
) {
    if (currentProject == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PremiumEmptyState(dark = dark, message = "No project selected")
        }
    } else {
        val totalAdvance = remember(allWorkers, projectTransactions) {
            var sum = 0.0
            allWorkers.forEach { wrk ->
                val rx = projectTransactions.filter { it.partyName == wrk.name && it.type == "Money In" }.sumOf { it.amount }
                val tx = projectTransactions.filter { it.partyName == wrk.name && it.type == "Money Out" }.sumOf { it.amount }
                val diff = rx - tx
                if (diff < 0) {
                    sum += diff.absoluteValue
                }
            }
            sum
        }

        val totalPending = remember(allWorkers, projectTransactions) {
            var sum = 0.0
            allWorkers.forEach { wrk ->
                val rx = projectTransactions.filter { it.partyName == wrk.name && it.type == "Money In" }.sumOf { it.amount }
                val tx = projectTransactions.filter { it.partyName == wrk.name && it.type == "Money Out" }.sumOf { it.amount }
                val diff = rx - tx
                if (diff > 0) {
                    sum += diff
                }
            }
            sum
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "FINANCIAL SUMMARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumStatCard(
                    dark = dark,
                    label = "TOTAL ADVANCED",
                    value = formatIndianRupees(totalAdvance),
                    valueColor = if (dark) NeonGreen else Color(0xFF047857),
                    icon = Icons.Default.TrendingUp,
                    gradient = if (dark)
                        Brush.verticalGradient(listOf(Color(0xFF0D1B3E).copy(alpha = 0.5f), Color(0xFF111827)))
                    else
                        Brush.verticalGradient(listOf(Color(0xFFF1F0FF), Color.White)),
                    borderColor = if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0),
                    modifier = Modifier.weight(1f)
                )

                PremiumStatCard(
                    dark = dark,
                    label = "TOTAL PENDING",
                    value = formatIndianRupees(totalPending),
                    valueColor = RoseGlow,
                    icon = Icons.Default.TrendingDown,
                    gradient = if (dark)
                        Brush.verticalGradient(listOf(Color(0xFF0D1B3E).copy(alpha = 0.5f), Color(0xFF111827)))
                    else
                        Brush.verticalGradient(listOf(Color(0xFFF1F0FF), Color.White)),
                    borderColor = if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0),
                    modifier = Modifier.weight(1f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (dark) Color(0xFF1E2D4A) else Color(0xFFE2E8F0))
            )

            Text(
                text = "SITE METADATA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
            )

            PremiumInfoCard(
                dark = dark,
                icon = Icons.Default.LocationOn,
                iconColor = AquaGlow,
                label = "SITE LOCATION",
                value = currentProject.location.ifBlank { "Location Unspecified" },
                gradient = if (dark)
                    Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
                else
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )

            PremiumInfoCard(
                dark = dark,
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = AmberGlow,
                label = "PROJECT BUDGET",
                value = formatIndianRupees(currentProject.budget),
                gradient = if (dark)
                    Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
                else
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )

            PremiumInfoCard(
                dark = dark,
                icon = Icons.Default.Construction,
                iconColor = VioletGlow,
                label = "ESTIMATED TOTAL FORCE",
                value = "${allWorkers.size} registered workers/contractors",
                gradient = if (dark)
                    Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
                else
                    Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private val Double.absoluteValue: Double
    get() = if (this < 0) -this else this
