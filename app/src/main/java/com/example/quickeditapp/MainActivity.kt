package com.example.quickeditapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.OutputStream

// One UI inspired colors
private val OneUIBlue = Color(0xFF3E91FF)
private val OneUIGray = Color(0xFF1C1C1E)
private val OneUISurface = Color(0xFF2C2C2E)
private val OneUITextSecondary = Color(0xFFA1A1A1)

class MainActivity : ComponentActivity() {
    private val REQUEST_PERMISSION_RESULT_LISTENER = Shizuku.OnRequestPermissionResultListener { _, _ ->
        // Status refresh is handled by the UI polling or manual refresh
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
        enableEdgeToEdge()
        setContent {
            OneUITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }
}

@Composable
fun RootNavigation() {
    val context = LocalContext.current
    var isSetupComplete by remember { mutableStateOf(false) }
    
    // Check if both Shizuku and Secure Settings are ready
    fun checkPermissions(): Boolean {
        val hasShizuku = Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val hasSecureSettings = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == 
                PackageManager.PERMISSION_GRANTED
        return hasShizuku && hasSecureSettings
    }

    LaunchedEffect(Unit) {
        // Repeatedly check permissions while on onboarding
        while(!isSetupComplete) {
            isSetupComplete = checkPermissions()
            kotlinx.coroutines.delay(1000)
        }
    }

    if (isSetupComplete) {
        MainScreen()
    } else {
        OnboardingScreen(onComplete = { isSetupComplete = true })
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    var isShizukuAuthorized by remember { mutableStateOf(false) }
    var isSecureSettingsGranted by remember { mutableStateOf(false) }

    fun refreshStatus() {
        isShizukuAuthorized = Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        isSecureSettingsGranted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == 
                PackageManager.PERMISSION_GRANTED
        
        if (isShizukuAuthorized && isSecureSettingsGranted) {
            onComplete()
        }
    }

    LaunchedEffect(Unit) {
        refreshStatus()
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false // We guide them with buttons
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Welcome to QuickEdit",
                    description = "Take full control of your Samsung Quick Panel with advanced visual tweaks.",
                    imageResId = R.drawable.ic_launcher_foreground
                )
                1 -> OnboardingPage(
                    title = "Powerful Integration",
                    description = "We use Shizuku to interact with system settings securely without needing Root access on most devices.",
                    icon = Icons.Default.Info
                )
                2 -> OnboardingPage(
                    title = "Instant Changes",
                    description = "No reboots, no waiting. Every toggle you flip takes effect the moment you touch it.",
                    icon = Icons.Default.CheckCircle
                )
                3 -> PermissionSetupPage(
                    isShizukuAuthorized = isShizukuAuthorized,
                    isSecureSettingsGranted = isSecureSettingsGranted,
                    onAuthorizeShizuku = { 
                        if (Shizuku.pingBinder()) {
                            Shizuku.requestPermission(0)
                        }
                        refreshStatus()
                    },
                    onGrantSecureSettings = {
                        grantSecureSettingsPermission(context)
                        refreshStatus()
                    }
                )
            }
        }

        // Bottom Navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page Indicator
            Row {
                repeat(4) { index ->
                    val color = if (pagerState.currentPage == index) OneUIBlue else OneUISurface
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            if (pagerState.currentPage < 3) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                ) {
                    Text("Next")
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(
    title: String, 
    description: String, 
    icon: ImageVector? = null,
    imageResId: Int? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            color = OneUIBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(24.dp)
        ) {
            if (imageResId != null) {
                Icon(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    tint = Color.Unspecified
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp).fillMaxSize(),
                    tint = OneUIBlue
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = OneUITextSecondary,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun PermissionSetupPage(
    isShizukuAuthorized: Boolean,
    isSecureSettingsGranted: Boolean,
    onAuthorizeShizuku: () -> Unit,
    onGrantSecureSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Final Setup",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We need these permissions to modify your Quick Panel settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = OneUITextSecondary
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        // Shizuku Step
        PermissionStep(
            title = "1. Authorize Shizuku",
            isGranted = isShizukuAuthorized,
            onClick = onAuthorizeShizuku
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Secure Settings Step
        PermissionStep(
            title = "2. Grant Secure Settings",
            isGranted = isSecureSettingsGranted,
            enabled = isShizukuAuthorized,
            onClick = onGrantSecureSettings
        )
        
        if (isShizukuAuthorized && !isSecureSettingsGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap 'Grant' to automatically run the ADB command via Shizuku.",
                style = MaterialTheme.typography.bodySmall,
                color = OneUIBlue,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PermissionStep(title: String, isGranted: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = { if (!isGranted && enabled) onClick() },
        color = if (isGranted) Color(0xFF1E3A1E) else OneUIGray,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        enabled = !isGranted && enabled
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF4CAF50) else if (enabled) OneUIBlue else OneUITextSecondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = if (enabled || isGranted) Color.White else OneUITextSecondary,
                fontWeight = FontWeight.SemiBold
            )
            if (!isGranted && enabled) {
                Text("Setup", color = OneUIBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OneUITheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = OneUIBlue,
        background = Color.Black,
        surface = OneUIGray,
        surfaceVariant = OneUISurface,
        onSurface = Color.White,
        onSurfaceVariant = OneUITextSecondary
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(
            headlineMedium = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            bodySmall = TextStyle(
                fontSize = 14.sp,
                color = OneUITextSecondary
            )
        ),
        content = content
    )
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var editMoreEnabled by remember { mutableStateOf(false) }
    var landscapeEditEnabled by remember { mutableStateOf(false) }
    var percentTextEnabled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        editMoreEnabled = getSetting(context, QuickPanelTweaks.KEY_EDIT_MORE)
        landscapeEditEnabled = getSetting(context, QuickPanelTweaks.KEY_LANDSCAPE)
        percentTextEnabled = getSetting(context, QuickPanelTweaks.KEY_PERCENT)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // App Logo in Header
        Surface(
            modifier = Modifier.size(80.dp),
            color = OneUIBlue.copy(alpha = 0.1f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                tint = Color.Unspecified
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Quick Panel ", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(text = "Tweaks", style = MaterialTheme.typography.headlineMedium, color = OneUIBlue)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        InfoCard(
            text = "Settings take effect instantly. No reboot required.",
            icon = Icons.Default.Info,
            backgroundColor = OneUIBlue.copy(alpha = 0.15f),
            contentColor = OneUIBlue
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader("Visual Tweaks")
        
        TweakCard {
            QuickSettingToggle(
                title = "Edit More",
                description = "Extra editing options in panel",
                checked = editMoreEnabled,
                onCheckedChange = {
                    editMoreEnabled = it
                    QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_EDIT_MORE, it)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.05f))
            QuickSettingToggle(
                title = "Landscape Edit",
                description = "Enable editing in landscape",
                checked = landscapeEditEnabled,
                onCheckedChange = {
                    landscapeEditEnabled = it
                    QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_LANDSCAPE, it)
                }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.05f))
            QuickSettingToggle(
                title = "Volume & Brightness %",
                description = "Show percentage indicators",
                checked = percentTextEnabled,
                onCheckedChange = {
                    percentTextEnabled = it
                    QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_PERCENT, it)
                }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Reset Button
        TextButton(
            onClick = {
                editMoreEnabled = false
                landscapeEditEnabled = false
                percentTextEnabled = false
                QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_EDIT_MORE, false)
                QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_LANDSCAPE, false)
                QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_PERCENT, false)
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.7f))
        ) {
            Text("Reset all tweaks", fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(32.dp).navigationBarsPadding())
    }
}

@Composable
fun InfoCard(text: String, icon: ImageVector, backgroundColor: Color, contentColor: Color) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = OneUITextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 12.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun TweakCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = OneUIGray,
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun QuickSettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OneUIBlue,
                uncheckedThumbColor = OneUITextSecondary,
                uncheckedTrackColor = OneUISurface
            )
        )
    }
}

fun checkShizuku(context: Context): Boolean {
    return try {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return true
            } else {
                Shizuku.requestPermission(0)
            }
        }
        false
    } catch (e: Exception) {
        false
    }
}

fun grantSecureSettingsPermission(context: Context) {
    val packageName = context.packageName
    val command = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS\n"
    
    try {
        val newProcessMethod = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
            "newProcess", 
            Array<String>::class.java, 
            Array<String>::class.java, 
            String::class.java
        ).apply { isAccessible = true }
        
        val process = newProcessMethod.invoke(null, arrayOf("sh"), null, null) as rikka.shizuku.ShizukuRemoteProcess
        val os: OutputStream = process.outputStream
        os.write(command.toByteArray())
        os.write("exit\n".toByteArray())
        os.flush()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

object QuickPanelTweaks {
    const val KEY_EDIT_MORE = "quick_panel_edit_more"
    const val KEY_LANDSCAPE = "quick_panel_landscape_edit"
    const val KEY_PERCENT = "quick_panel_percent_text"

    fun setSetting(context: Context, key: String, enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        try {
            Settings.Secure.putString(context.contentResolver, key, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun getSetting(context: Context, key: String): Boolean {
    return try {
        Settings.Secure.getInt(context.contentResolver, key, 0) == 1
    } catch (e: Exception) {
        false
    }
}
