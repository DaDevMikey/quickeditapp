/**
 * QuickPanelTweaks - A specialized utility for Samsung devices to unlock hidden Quick Panel features.
 * Developed by: DaDevMikey
 *
 * This application leverages Shizuku (a bridge to system-level ADB) to grant itself
 * the WRITE_SECURE_SETTINGS permission. This allows the app to toggle system-level
 * flags that Samsung uses for Quick Panel customization, even if they aren't exposed in standard menus.
 */

package com.example.quickeditapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.OutputStream

// One UI inspired color palette for a native look and feel on Samsung devices
private val OneUIBlue = Color(0xFF3E91FF)
private val OneUIGray = Color(0xFF1C1C1E)
private val OneUISurface = Color(0xFF2C2C2E)
private val OneUITextSecondary = Color(0xFFA1A1A1)

class MainActivity : ComponentActivity() {
    // Listener to monitor Shizuku permission grant events
    private val REQUEST_PERMISSION_RESULT_LISTENER = Shizuku.OnRequestPermissionResultListener { _, _ ->
        // Status refresh is handled by the UI polling logic
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Add Shizuku listener on startup
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
        // Clean up the listener to prevent memory leaks
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER)
    }
}

/**
 * Manages the top-level navigation state.
 * It checks for the required WRITE_SECURE_SETTINGS permission.
 * If granted, it goes to the Main Screen; otherwise, it shows the Onboarding.
 */
@Composable
fun RootNavigation() {
    val context = LocalContext.current
    var isSetupComplete by remember { mutableStateOf(false) }
    
    // Check if the critical system permission is granted.
    // Once this is true, Shizuku is no longer strictly necessary for basic operation.
    fun checkPermissions(): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == 
                PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        // Repeatedly poll for permission status while on setup screen
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

/**
 * Onboarding flow to guide the user through Shizuku and System Permission setup.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    var isShizukuAuthorized by remember { mutableStateOf(false) }
    var isSecureSettingsGranted by remember { mutableStateOf(false) }

    // Refreshes the internal status of both required permissions
    fun refreshStatus() {
        isShizukuAuthorized = Shizuku.pingBinder() && 
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        isSecureSettingsGranted = context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == 
                PackageManager.PERMISSION_GRANTED
        
        // Auto-complete onboarding if permission is detected
        if (isSecureSettingsGranted) {
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
            userScrollEnabled = false // Forced step-by-step navigation
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Welcome to QuickEdit",
                    description = "Unlock the full potential of your Quick Panel.\n\nCreated by DaDevMikey",
                    imageResId = R.drawable.ic_launcher_foreground
                )
                1 -> OnboardingPage(
                    title = "Secure System Access",
                    description = "We use Shizuku to securely communicate with system settings without requiring Root access.",
                    icon = Icons.Default.Info
                )
                2 -> OnboardingPage(
                    title = "Persistent Tweaks",
                    description = "Your settings are applied instantly and saved automatically, even after you reboot your device.",
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

        // Navigation Footer
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Page indicators (Dots)
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

            // Next button logic
            if (pagerState.currentPage < 3) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                ) {
                    Text("Next")
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}

/**
 * Individual page template for onboarding.
 */
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

/**
 * Screen where permissions are actually requested and granted.
 */
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
            text = "Grant these permissions to allow modification of system settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = OneUITextSecondary
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        PermissionStep(
            title = "1. Authorize Shizuku",
            isGranted = isShizukuAuthorized,
            onClick = onAuthorizeShizuku
        )

        Spacer(modifier = Modifier.height(16.dp))

        PermissionStep(
            title = "2. Grant Secure Settings",
            isGranted = isSecureSettingsGranted,
            enabled = isShizukuAuthorized,
            onClick = onGrantSecureSettings
        )
        
        if (isShizukuAuthorized && !isSecureSettingsGranted) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Tap 'Setup' to run the grant command automatically via Shizuku.",
                style = MaterialTheme.typography.bodySmall,
                color = OneUIBlue,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Row representing a single permission requirement.
 */
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

/**
 * Standard One UI themed wrapper.
 */
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

/**
 * Main dashboard where tweaks are toggled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var editMoreEnabled by remember { mutableStateOf(false) }
    var landscapeEditEnabled by remember { mutableStateOf(false) }
    var percentTextEnabled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var showInfoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sync UI state with current device settings on load
    LaunchedEffect(Unit) {
        editMoreEnabled = getSetting(context, QuickPanelTweaks.KEY_EDIT_MORE)
        landscapeEditEnabled = getSetting(context, QuickPanelTweaks.KEY_LANDSCAPE)
        percentTextEnabled = getSetting(context, QuickPanelTweaks.KEY_PERCENT)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
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
                text = "Changes take effect instantly. Shizuku is only needed for the initial setup.",
                icon = Icons.Default.Info,
                backgroundColor = OneUIBlue.copy(alpha = 0.15f),
                contentColor = OneUIBlue
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("Visual Customization")
            
            TweakCard {
                QuickSettingToggle(
                    title = "Enhanced Editing",
                    description = "Unlocks additional layout options in the editor",
                    checked = editMoreEnabled,
                    onCheckedChange = {
                        editMoreEnabled = it
                        QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_EDIT_MORE, it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.05f))
                QuickSettingToggle(
                    title = "Landscape Editor",
                    description = "Enable full editing support in landscape mode",
                    checked = landscapeEditEnabled,
                    onCheckedChange = {
                        landscapeEditEnabled = it
                        QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_LANDSCAPE, it)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.05f))
                QuickSettingToggle(
                    title = "Percentage Labels",
                    description = "Display values on Brightness and Volume sliders",
                    checked = percentTextEnabled,
                    onCheckedChange = {
                        percentTextEnabled = it
                        QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_PERCENT, it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "App Developed by DaDevMikey",
                style = MaterialTheme.typography.bodySmall,
                color = OneUITextSecondary.copy(alpha = 0.4f)
            )
            
            Spacer(modifier = Modifier.height(80.dp).navigationBarsPadding())
        }

        // Info Button at bottom right
        FloatingActionButton(
            onClick = { showInfoSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding(),
            containerColor = OneUISurface,
            contentColor = OneUIBlue
        ) {
            Icon(Icons.Default.Info, contentDescription = "App Information")
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = OneUISurface,
            contentColor = Color.White,
            sheetState = sheetState
        ) {
            InfoSheetContent()
        }
    }
}

@Composable
fun InfoSheetContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    val currentVersion = "1.0.1" // Bumped version

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quick Panel Tweaks",
            style = MaterialTheme.typography.headlineMedium,
            color = OneUIBlue
        )
        Text(
            text = "Version $currentVersion",
            style = MaterialTheme.typography.bodySmall,
            color = OneUITextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Developed with ❤️ by DaDevMikey",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Update Section
        Surface(
            color = OneUIGray,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (latestRelease != null) {
                    Text("New update available: ${latestRelease!!.tag_name}", color = OneUIBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestRelease!!.html_url))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OneUIBlue)
                    ) {
                        Text("Download Update")
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (!isChecking) {
                                    isChecking = true
                                    UpdateChecker.checkForUpdates(currentVersion) { release ->
                                        scope.launch {
                                            isChecking = false
                                            if (release != null) {
                                                latestRelease = release
                                            } else {
                                                Toast.makeText(context, "You are on the latest version!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            .padding(8.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = OneUIBlue)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = OneUIBlue)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isChecking) "Checking..." else "Check for Updates", color = OneUIBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        InfoLink("Source Code", "https://github.com/DaDevMikey/quickeditapp")
        InfoLink("Telegram Channel", "https://t.me/thecipherproject")
        InfoLink("Official Website", "https://damanmikey.me")
    }
}

@Composable
fun InfoLink(label: String, url: String) {
    val context = LocalContext.current
    Surface(
        onClick = { 
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        },
        color = OneUIGray,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = OneUIBlue)
        }
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

/**
 * Developed by: DaDevMikey
 * Fixed the permission grant logic using a more robust reflection approach.
 * This runs the shell command 'pm grant <pkg> <perm>' through the Shizuku bridge.
 */
fun grantSecureSettingsPermission(context: Context) {
    val packageName = context.packageName
    val command = "pm grant $packageName android.permission.WRITE_SECURE_SETTINGS\n"
    
    try {
        // Find the newProcess method via reflection to bypass 'private' access restrictions
        val methods = Shizuku::class.java.declaredMethods
        val newProcessMethod = methods.find { 
            it.name == "newProcess" && it.parameterCount == 3 
        }
        
        if (newProcessMethod != null) {
            newProcessMethod.isAccessible = true
            // Execute 'sh -c' to run the grant command as the shell user
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as rikka.shizuku.ShizukuRemoteProcess
            val os: OutputStream = process.outputStream
            os.write("exit\n".toByteArray())
            os.flush()
            process.waitFor()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Utility for reading and writing Samsung-specific secure settings.
 */
object QuickPanelTweaks {
    // Internal system keys used by Samsung's Quick Panel
    const val KEY_EDIT_MORE = "quick_panel_edit_more"
    const val KEY_LANDSCAPE = "quick_panel_landscape_edit"
    const val KEY_PERCENT = "quick_panel_percent_text"

    /**
     * Updates the system setting and saves a copy locally for boot persistence.
     */
    fun setSetting(context: Context, key: String, enabled: Boolean) {
        val value = if (enabled) "1" else "0"
        try {
            // Write to the device's Secure Settings table
            Settings.Secure.putString(context.contentResolver, key, value)
            
            // Mirror to SharedPreferences so we can restore it after a reboot
            val sharedPrefs = context.getSharedPreferences("tweak_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean(key, enabled).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Fetches the current value of a secure setting key.
 */
fun getSetting(context: Context, key: String): Boolean {
    return try {
        Settings.Secure.getInt(context.contentResolver, key, 0) == 1
    } catch (e: Exception) {
        false
    }
}
