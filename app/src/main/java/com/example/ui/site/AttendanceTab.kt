package com.example.ui

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

@Composable
fun AttendanceTab(
    workers: List<Worker>,
    allAttendance: List<Attendance>,
    currentProject: Project?,
    activeDate: String,
    dark: Boolean,
    onSelectDay: (Worker, String) -> Unit,
    onNavigateDay: (Int) -> Unit,
    onReturnToday: () -> Unit,
    onOpenMarkDialog: (Worker) -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dayFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US) }
    val parsedDateString = remember(activeDate) {
        try {
            val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(activeDate)
            dayFormatter.format(dateObj ?: Date())
        } catch (e: Exception) {
            activeDate
        }
    }

    // Attendance stats
    val siteAttendance = remember(allAttendance, activeDate, currentProject) {
        allAttendance.filter { it.date == activeDate && it.projectId == (currentProject?.id ?: 0) }
    }

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Present", "Absent", "Overtime", "Unmarked"

    val filteredWorkers = remember(workers, siteAttendance, searchQuery, statusFilter) {
        workers.filter { worker ->
            val matchesSearch = worker.name.contains(searchQuery, ignoreCase = true) ||
                    worker.role.contains(searchQuery, ignoreCase = true)
            
            val record = siteAttendance.find { it.workerId == worker.id }
            val matchesFilter = when (statusFilter) {
                "All" -> true
                "Present" -> record?.status == "Present"
                "Absent" -> record?.status == "Absent"
                "Overtime" -> record?.status == "Overtime"
                "Unmarked" -> record == null
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val projectName = currentProject?.name ?: "Treasure Garden"
    val siteAddress = currentProject?.location ?: "Treasure Garden Road Site, India"

    fun handleExport() {
        try {
            val pdfFile = PdfUtils.generateAttendanceReportPdfFile(
                context = context,
                projectName = projectName,
                siteAddress = siteAddress,
                generatedBy = "Site Manager",
                attendanceDate = activeDate,
                workers = filteredWorkers,
                attendanceList = siteAttendance
            )
            
            val fileName = pdfFile.name
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS + "/ConstructPro")
                }
                val uri = context.contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        pdfFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    android.widget.Toast.makeText(context, "Saved to Downloads/ConstructPro/$fileName", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                val dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val folder = java.io.File(dir, "ConstructPro").also { it.mkdirs() }
                val dest = java.io.File(folder, fileName)
                pdfFile.copyTo(dest, overwrite = true)
                android.widget.Toast.makeText(context, "Saved to Downloads/ConstructPro/$fileName", android.widget.Toast.LENGTH_LONG).show()
            }
            
            PdfUtils.sharePdfFile(context, pdfFile, "Share Attendance Sheet")
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val totalWorkersCount = workers.size
    val presentCount = remember(siteAttendance) { siteAttendance.count { it.status == "Present" } }
    val absentCount  = remember(siteAttendance) { siteAttendance.count { it.status == "Absent" } }
    val overtimeCount = remember(siteAttendance) { siteAttendance.count { it.status == "Overtime" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search Bar & Export Button at the top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                PremiumSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    dark = dark,
                    placeholder = "Search name or role..."
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (dark) Color(0xFF1E2D4A) else Color(0xFFEEF2FF)
                    )
                    .border(
                        1.dp,
                        if (dark) Color(0xFF2D3F5E) else Color(0xFFCBD5E1),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { handleExport() }
                    .padding(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export Attendance",
                        tint = if (dark) AquaGlow else Color(0xFF4F46E5),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Export",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) AquaGlow else Color(0xFF4F46E5)
                    )
                }
            }
        }

        // Date Navigator
        PremiumDateNavigator(
            dark = dark,
            parsedDateString = parsedDateString,
            activeDate = activeDate,
            navigateDay = onNavigateDay,
            onReturnToday = onReturnToday
        )

        // Statistics Badges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AttendanceStatBadge(
                dark = dark,
                label = "PRESENT",
                value = presentCount.toString(),
                color = EmeraldGlow,
                modifier = Modifier.weight(1f)
            )
            AttendanceStatBadge(
                dark = dark,
                label = "ABSENT",
                value = absentCount.toString(),
                color = RoseGlow,
                modifier = Modifier.weight(1f)
            )
            AttendanceStatBadge(
                dark = dark,
                label = "OVERTIME",
                value = overtimeCount.toString(),
                color = VioletGlow,
                modifier = Modifier.weight(1f)
            )
        }

        // Scrollable filter chips list
        val filters = listOf("All", "Present", "Absent", "Overtime", "Unmarked")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                val isSelected = statusFilter == filter
                val chipColor = when (filter) {
                    "Present" -> EmeraldGlow
                    "Absent" -> RoseGlow
                    "Overtime" -> VioletGlow
                    else -> if (dark) AquaGlow else Color(0xFF4F46E5)
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) chipColor.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isSelected) chipColor else {
                                if (dark) Color(0xFF1E2D4A) else Color(0xFFE2E8F0)
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { statusFilter = filter }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                        color = if (isSelected) chipColor else {
                            if (dark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        }
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

        Text(
            text = "ALL PARTIES, WORKERS & LABOURS (${filteredWorkers.size} of $totalWorkersCount)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
        )

        if (filteredWorkers.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PremiumEmptyState(
                    dark = dark, 
                    message = if (workers.isEmpty()) "No workers or sub-contractors on payroll currently"
                              else "No workers match your search or filter criteria"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWorkers, key = { it.id }) { worker ->
                    val record = siteAttendance.find { it.workerId == worker.id }
                    PremiumAttendanceCard(
                        worker = worker,
                        record = record,
                        dark = dark,
                        cFormatter = currencyFormatter,
                        allAttendance = allAttendance,
                        activeDate = activeDate,
                        projectId = currentProject?.id ?: 0,
                        onClick = { onOpenMarkDialog(worker) },
                        onDayClick = onSelectDay
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumAttendanceCard(
    worker: Worker,
    record: Attendance?,
    dark: Boolean,
    cFormatter: NumberFormat,
    allAttendance: List<Attendance>,
    activeDate: String,
    projectId: Int,
    onClick: () -> Unit,
    onDayClick: (Worker, String) -> Unit
) {
    val statusText = when (record?.status) {
        "Present"  -> "Present"
        "Absent"   -> "Absent"
        "Overtime" -> "OT ${record.overtimeHours}h"
        else       -> "Unmarked"
    }
    val statusColor = when (record?.status) {
        "Present"  -> EmeraldGlow
        "Absent"   -> RoseGlow
        "Overtime" -> VioletGlow
        else       -> if (dark) Color(0xFF2D3F5E) else Color(0xFFCBD5E1)
    }

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
                    listOf(statusColor.copy(alpha = 0.35f), if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0))
                ),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(worker.avatarColor), Color(worker.avatarColor).copy(alpha = 0.5f))
                                ), CircleShape
                            )
                            .border(2.dp, statusColor.copy(alpha = 0.5f), CircleShape),
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
                            "${worker.role} · ${worker.shift} · ${cFormatter.format(worker.wageRate)}/day",
                            fontSize = 10.sp,
                            color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (dark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            )
            Spacer(modifier = Modifier.height(12.dp))

            WeeklyAttendanceStrip(
                worker = worker,
                allAttendance = allAttendance,
                activeDate = activeDate,
                projectId = projectId,
                dark = dark,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
fun PremiumDateNavigator(
    dark: Boolean,
    parsedDateString: String,
    activeDate: String,
    navigateDay: (Int) -> Unit,
    onReturnToday: () -> Unit
) {
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
                    listOf(AquaGlow.copy(alpha = 0.4f), if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0))
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AquaGlow.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, AquaGlow.copy(alpha = 0.3f), CircleShape)
                    .clickable { navigateDay(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = AquaGlow, modifier = Modifier.size(16.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    parsedDateString, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                if (activeDate == today) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AquaGlow.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("TODAY", fontSize = 8.sp, fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp, color = AquaGlow)
                    }
                } else {
                    Text(
                        "TAP TO RETURN TODAY", fontSize = 8.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp, color = VioletGlow,
                        modifier = Modifier.clickable { onReturnToday() }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(AquaGlow.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, AquaGlow.copy(alpha = 0.3f), CircleShape)
                    .clickable { navigateDay(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowForward, null, tint = AquaGlow, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AttendanceStatBadge(
    dark: Boolean,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.14f), color.copy(alpha = 0.04f))))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 8.sp, fontWeight = FontWeight.Black,
                letterSpacing = 1.sp, color = color.copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun PremiumAttendanceDialog(
    worker: Worker,
    record: Attendance?,
    dark: Boolean,
    parsedDateString: String,
    activeProj: Project,
    activeDate: String,
    inputOvertimeHours: String,
    onOtChange: (String) -> Unit,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onSaveOt: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassModalDialog(
        visible = true,
        onDismiss = onDismiss,
        title = "Mark Attendance",
        darkTheme = dark,
        glowColor = VioletGlow,
        scrollable = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Worker Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(worker.avatarColor), Color(worker.avatarColor).copy(alpha = 0.6f))),
                            CircleShape
                        )
                        .border(2.dp, VioletGlow.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(worker.name.take(2).uppercase(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
                Column {
                    Text(worker.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B))
                    Text(parsedDateString, fontSize = 10.sp,
                        color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8))
                }
            }

            // Status Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (record?.status == "Present")
                                Brush.linearGradient(listOf(EmeraldGlow.copy(0.25f), EmeraldGlow.copy(0.1f)))
                            else Brush.linearGradient(listOf(Color(0x0A10B981), Color.Transparent))
                        )
                        .border(
                            1.5.dp,
                            if (record?.status == "Present") EmeraldGlow else EmeraldGlow.copy(0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onPresent)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Check, null, tint = EmeraldGlow, modifier = Modifier.size(16.dp))
                        Text("PRESENT", color = EmeraldGlow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (record?.status == "Absent")
                                Brush.linearGradient(listOf(RoseGlow.copy(0.25f), RoseGlow.copy(0.1f)))
                            else Brush.linearGradient(listOf(Color(0x0AF43F5E), Color.Transparent))
                        )
                        .border(
                            1.5.dp,
                            if (record?.status == "Absent") RoseGlow else RoseGlow.copy(0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onAbsent)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Close, null, tint = RoseGlow, modifier = Modifier.size(16.dp))
                        Text("ABSENT", color = RoseGlow, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }

            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, VioletGlow.copy(0.3f), Color.Transparent))))

            // Overtime
            Text(
                "Log Overtime Hours",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    GlassTextField(
                        value = inputOvertimeHours,
                        onValueChange = onOtChange,
                        label = "Hours",
                        isNumeric = true,
                        darkTheme = dark
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GradientViolet)
                        .clickable(onClick = onSaveOt)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAVE OT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }

            if (record != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RoseGlow.copy(alpha = 0.08f))
                        .border(1.dp, RoseGlow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onClear)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Delete, null, tint = RoseGlow, modifier = Modifier.size(14.dp))
                        Text("CLEAR RECORD", color = RoseGlow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun getWeekDaysForDate(activeDateStr: String): List<java.util.Date> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance(Locale.US)
    try {
        cal.time = formatter.parse(activeDateStr) ?: Date()
    } catch (e: Exception) {
        cal.time = Date()
    }

    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysToSubtract = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
    cal.add(Calendar.DATE, -daysToSubtract)

    val weekDates = mutableListOf<java.util.Date>()
    for (i in 0 until 7) {
        weekDates.add(cal.time)
        cal.add(Calendar.DATE, 1)
    }
    return weekDates
}

@Composable
fun WeeklyAttendanceStrip(
    worker: Worker,
    allAttendance: List<Attendance>,
    activeDate: String,
    projectId: Int,
    dark: Boolean,
    onDayClick: (Worker, String) -> Unit
) {
    val weekDays = remember(activeDate) {
        getWeekDaysForDate(activeDate)
    }

    val dbFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayNameFormatter = remember { SimpleDateFormat("EEE", Locale.US) }
    val dayNumberFormatter = remember { SimpleDateFormat("d", Locale.US) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "WEEKLY SUMMARY (MON - SUN)",
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { date ->
                val dateStr = dbFormatter.format(date)
                val dayName = dayNameFormatter.format(date).uppercase()
                val dayNum = dayNumberFormatter.format(date)
                val isSelectedDay = dateStr == activeDate

                val record = allAttendance.find {
                    it.workerId == worker.id && it.date == dateStr && it.projectId == projectId
                }

                val (statusColor, icon, bgOpacity) = when (record?.status) {
                    "Present" -> Triple(EmeraldGlow, Icons.Default.Check, 0.15f)
                    "Absent" -> Triple(RoseGlow, Icons.Default.Close, 0.15f)
                    "Overtime" -> Triple(VioletGlow, Icons.Default.TrendingUp, 0.15f)
                    else -> Triple(if (dark) Color(0xFF334155) else Color(0xFFCBD5E1), null, 0.0f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelectedDay) {
                                if (dark) Color(0xFF1E293B) else Color(0xFFEEF2FF)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onDayClick(worker, dateStr) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = dayName,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelectedDay) {
                            if (dark) Color.White else Color(0xFF4F46E5)
                        } else {
                            if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (icon != null) statusColor.copy(alpha = bgOpacity) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (icon != null) statusColor.copy(alpha = 0.5f) else {
                                    if (dark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                },
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = record?.status,
                                tint = statusColor,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        if (dark) Color(0xFF475569) else Color(0xFF475569),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = dayNum,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelectedDay) {
                            if (dark) Color.White else Color(0xFF4F46E5)
                        } else {
                            if (dark) Color(0xFF475569) else Color(0xFF64748B)
                        }
                    )
                }
            }
        }
    }
}
