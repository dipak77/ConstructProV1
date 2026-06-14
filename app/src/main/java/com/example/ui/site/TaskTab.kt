package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
fun TaskTab(
    tasks: List<Task>,
    currentProjectId: Int?,
    searchText: String,
    onSearchChange: (String) -> Unit,
    dark: Boolean,
    onAddTask: () -> Unit,
    onCycleStatus: (Task) -> Unit
) {
    val projTasks = remember(tasks, currentProjectId) {
        if (currentProjectId == null) emptyList()
        else tasks.filter { it.projectId == currentProjectId }
    }

    val doneCount    = remember(projTasks) { projTasks.count { it.status == "Done" } }
    val pendingCount = remember(projTasks) { projTasks.count { it.status != "Done" } }

    val filteredTasks = remember(projTasks, searchText) {
        if (searchText.isBlank()) projTasks
        else projTasks.filter {
            it.title.contains(searchText, ignoreCase = true) ||
                    it.assignee.contains(searchText, ignoreCase = true) ||
                    it.status.contains(searchText, ignoreCase = true) ||
                    it.priority.contains(searchText, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Task Progress Card Header (Visual richness)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (dark)
                        Brush.horizontalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF111827)))
                    else
                        Brush.horizontalGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF8FAFF)))
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Task Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B)
                        )
                        Text(
                            "$doneCount completed · $pendingCount pending",
                            fontSize = 11.sp,
                            color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(AmberGlow.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, AmberGlow.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (projTasks.isEmpty()) "0%" else "${((doneCount.toFloat() / projTasks.size) * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = AmberGlow
                        )
                    }
                }

                if (projTasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(doneCount.toFloat() / projTasks.size)
                                .fillMaxHeight()
                                .background(GradientAmber)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PremiumSearchBar(
                value = searchText,
                onValueChange = onSearchChange,
                dark = dark,
                placeholder = "Search tasks, status or assignees..."
            )

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    PremiumEmptyState(dark = dark, message = "No tasks or site assignments loaded")
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            PremiumTaskCard(
                                task = task,
                                dark = dark,
                                onCycle = { onCycleStatus(task) }
                            )
                        }
                    }

                    // Add Task Floating CTA
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 4.dp)
                    ) {
                        Button(
                            onClick = onAddTask,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D53EA)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Text(
                                    "ADD TASK",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTaskCard(task: Task, dark: Boolean, onCycle: () -> Unit) {
    val statusColor = when (task.status) {
        "Done"        -> EmeraldGlow
        "In Progress" -> AmberGlow
        else          -> if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
    }
    val priorityColor = when (task.priority) {
        "High"   -> RoseGlow
        "Medium" -> AmberGlow
        else     -> AquaGlow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (dark)
                    Brush.horizontalGradient(listOf(Color(0xFF111827), Color(0xFF0D1B3E)))
                else
                    Brush.horizontalGradient(listOf(Color.White, Color(0xFFF8FAFF)))
            )
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(statusColor.copy(alpha = 0.3f), if (dark) Color(0xFF1E2D4A) else Color(0xFFDDE4F0))
                ),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onCycle)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        task.title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (dark) Color(0xFFE2E8F4) else Color(0xFF1E293B)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            task.assignee, fontSize = 10.sp,
                            color = if (dark) Color(0xFF475569) else Color(0xFF94A3B8)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(task.priority, fontSize = 8.sp, color = priorityColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    task.status, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, color = statusColor
                )
            }
        }
    }
}
