package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextSecondaryLight

@Composable
fun DrawerContent(
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val dark = viewModel.darkThemeEnabled
    val currentProject by viewModel.activeProject.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val currentTab = viewModel.currentScreen
    val activeSiteTab = viewModel.activeSiteTab

    val projectName = currentProject?.name ?: ""
    val userEmail = userSession?.email ?: ""
    val userName = userSession?.displayName ?: "Guest"

    // Compute initials dynamically from user name as fallback for profile picture placeholder
    val userInitials = userName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)
        .ifEmpty { "U" }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (dark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        if (dark) Color(0xFF1E1B4B) else Color(0xFFEEF2F6),
                        if (dark) Color(0xFF090D1A) else Color(0xFFE2E8F0)
                    )
                )
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (dark) GlassBorderDark else Color(0x330284C7)
                ),
                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Profile & Project Badge Block (arranged vertically exactly as in the image)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Circular Profile Photo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF4F46E5),
                                    Color(0xFF312E81)
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val photo = userSession?.photoUrl
                    if (!photo.isNullOrEmpty()) {
                        AsyncImage(
                            model = photo,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = userInitials,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Workspace & Email Names
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = userName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = if (dark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Channels Header
            Text(
                text = "WORKSPACE CHANNELS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (dark) Color.White.copy(alpha = 0.4f) else Color(0xFF64748B),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Channels Navigation List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. Site Dashboard
                DrawerNavItem(
                    label = "Site Dashboard",
                    icon = Icons.Default.Dashboard,
                    active = currentTab == AppScreen.Dashboard,
                    darkTheme = dark
                ) {
                    viewModel.currentScreen = AppScreen.Dashboard
                    onClose()
                }

                // 2. Affiliates & Crew
                DrawerNavItem(
                    label = "Affiliates & Crew",
                    icon = Icons.Default.People,
                    active = currentTab == AppScreen.Site && activeSiteTab == "Party",
                    darkTheme = dark
                ) {
                    viewModel.currentScreen = AppScreen.Site
                    viewModel.activeSiteTab = "Party"
                    onClose()
                }

                // 3. Chronicle Ledger
                DrawerNavItem(
                    label = "Chronicle Ledger",
                    icon = Icons.Default.AccountBalanceWallet,
                    active = currentTab == AppScreen.Money,
                    darkTheme = dark
                ) {
                    viewModel.currentScreen = AppScreen.Money
                    onClose()
                }

                // 4. Velocity Tasks
                DrawerNavItem(
                    label = "Velocity Tasks",
                    icon = Icons.Default.TaskAlt,
                    active = currentTab == AppScreen.Tasks,
                    darkTheme = dark
                ) {
                    viewModel.currentScreen = AppScreen.Tasks
                    onClose()
                }

                // 5. Staff Attendance
                DrawerNavItem(
                    label = "Staff Attendance",
                    icon = Icons.Default.CheckCircle,
                    active = currentTab == AppScreen.Site && activeSiteTab == "Attendance",
                    darkTheme = dark
                ) {
                    viewModel.currentScreen = AppScreen.Site
                    viewModel.activeSiteTab = "Attendance"
                    onClose()
                }

                // 6. Site Control Hub
                DrawerNavItem(
                    label = "Site Control Hub",
                    icon = Icons.Default.Settings,
                    active = currentTab == AppScreen.More,
                    darkTheme = dark
                ) {
                    viewModel.currentScreen = AppScreen.More
                    onClose()
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // Dark Mode switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Theme",
                        tint = if (dark) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Night Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (dark) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)
                    )
                }
                Switch(
                    checked = viewModel.darkThemeEnabled,
                    onCheckedChange = { viewModel.darkThemeEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2DD4BF),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1) // Lighter track for day mode
                    ),
                    modifier = Modifier.size(36.dp)
                )
            }

            // Sync Status Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (dark) Color.Black.copy(alpha = 0.3f) else Color(0xFFF1F5F9))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (dark) Color(0xFF1E2D4A) else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (dark) NeonCyan else Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Cloud Backup",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Synced Just Now",
                        fontSize = 10.sp,
                        color = if (dark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerSubItem(label: String, active: Boolean, darkTheme: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (active) {
            if (darkTheme) NeonCyan else Color(0xFF0284C7)
        } else {
            if (darkTheme) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
        },
        fontSize = 13.sp,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

@Composable
fun DrawerNavItem(
    label: String,
    icon: ImageVector,
    active: Boolean,
    darkTheme: Boolean,
    onClick: () -> Unit
) {
    val bg = if (active) {
        if (darkTheme) Color(0x332DD4BF) else Color(0x1A0284C7)
    } else Color.Transparent

    val tc = if (active) {
        if (darkTheme) Color(0xFF2DD4BF) else Color(0xFF0284C7)
    } else {
        if (darkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF334155)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                1.dp,
                if (active) (if (darkTheme) Color(0x4D2DD4BF) else Color(0x4D0284C7)) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tc,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            color = tc,
            fontSize = 15.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
fun SidebarNavRow(label: String, icon: ImageVector, active: Boolean, darkTheme: Boolean, onClick: () -> Unit) {
    val bg = if (active) {
        if (darkTheme) NeonCyan.copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.10f)
    } else Color.Transparent
    val tc = if (active) {
        if (darkTheme) NeonCyan else Color(0xFF0284C7)
    } else if (darkTheme) TextSecondary else TextSecondaryLight

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tc, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = tc, fontSize = 14.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}
