/**
 * BootReceiver - Ensures persistence of tweaks after a device restart.
 * Developed by: DaDevMikey
 * 
 * This receiver listens for the system 'BOOT_COMPLETED' event. Since the 
 * WRITE_SECURE_SETTINGS permission persists across reboots, this class
 * can re-apply the user's chosen tweaks without needing manual intervention
 * or Shizuku to be actively running at boot time.
 */

package com.example.quickeditapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle both standard Android and Samsung-specific quick boot actions
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Reboot detected, reapplying Quick Panel tweaks...")
            
            // Retrieve the saved states from SharedPreferences (the app's local storage)
            val sharedPrefs = context.getSharedPreferences("tweak_prefs", Context.MODE_PRIVATE)
            val editMore = sharedPrefs.getBoolean(QuickPanelTweaks.KEY_EDIT_MORE, false)
            val landscape = sharedPrefs.getBoolean(QuickPanelTweaks.KEY_LANDSCAPE, false)
            val percent = sharedPrefs.getBoolean(QuickPanelTweaks.KEY_PERCENT, false)

            // Re-apply each setting to the system's Secure Settings table.
            // This is possible because the permission was granted permanently during setup.
            QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_EDIT_MORE, editMore)
            QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_LANDSCAPE, landscape)
            QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_PERCENT, percent)
        }
    }
}
