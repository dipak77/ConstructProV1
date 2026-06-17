package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun GoogleLoginScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val dark = viewModel.darkThemeEnabled
    
    // UI controller states
    var isConnecting by remember { mutableStateOf(false) }

    // Authentic GMS Google Sign In options config
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("970298420983-bin5cqqcqgdoi9r256p7a78bvpi6c0hs.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember {
        try {
            GoogleSignIn.getClient(context, gso)
        } catch (e: Throwable) {
            null
        }
    }

    // Launcher for standard Google Sign In Intent activity
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isConnecting = false
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
                if (account != null) {
                    val user = GoogleUser(
                        displayName = account.displayName ?: "Google User",
                        email = account.email ?: "developer@gmail.com",
                        photoUrl = account.photoUrl?.toString(),
                        idToken = account.idToken,
                        isGuest = false
                    )
                    viewModel.handleGoogleSignIn(user, context)
                    Toast.makeText(context, "Welcome, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to retrieve Google Account details.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                val errorMsg = "Google Sign In Failed. Code: ${e.statusCode}. Please verify device accounts/fingerprint."
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Sign In Canceled (Code: ${result.resultCode})", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. App logo branding logo
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .widthIn(max = 480.dp),
                darkTheme = dark,
                glowColor = NeonCyan
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BuildOnSiteLogo(modifier = Modifier.size(72.dp), darkTheme = dark)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CONSTRUCTPRO",
                            color = if (dark) NeonCyan else Color(0xFF0284C7),
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Unified Workspace Client",
                            color = if (dark) TextSecondary else TextSecondaryLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Welcome & Introductory Message
                    Text(
                        text = "Build On Site App empowers contractors, site engineers, and project developers with advanced offline-first ledger tracking, attendance, customized cards and real-time summaries.",
                        color = if (dark) TextSecondary else TextSecondaryLight,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // 2. Interactive Sign In Panel
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                darkTheme = dark,
                glowColor = NeonPurple
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SECURE WORKSPACE PORTAL",
                        color = if (dark) NeonPurple else Color(0xFF7C3AED),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    if (isConnecting) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NeonPurple,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Connecting to Google Services...",
                                    color = if (dark) TextSecondary else TextSecondaryLight,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { isConnecting = false }) {
                                Text("Cancel", color = NeonCyan, fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Google Sign-In button (Real authentication flow Only)
                        GlassButton(
                            onClick = {
                                isConnecting = true
                                val client = googleSignInClient
                                if (client != null) {
                                    try {
                                        val intent = client.signInIntent
                                        signInLauncher.launch(intent)
                                    } catch (e: Throwable) {
                                        try {
                                            client.signOut()
                                            val intent = client.signInIntent
                                            signInLauncher.launch(intent)
                                        } catch (ex: Throwable) {
                                            isConnecting = false
                                            Toast.makeText(context, "Google Sign-In launch failed: ${ex.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    isConnecting = false
                                    Toast.makeText(context, "Google Play Services unavailable on this device", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            outlineMode = true,
                            glowColor = if (dark) Color.White.copy(alpha = 0.3f) else Color.LightGray,
                            darkTheme = dark
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Draw a miniature color Google logo
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color.White, CircleShape)
                                        .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), CircleShape)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFEA4335), // Google Red
                                        textAlign = TextAlign.Center
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = "Sign in with Google",
                                    color = if (dark) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
