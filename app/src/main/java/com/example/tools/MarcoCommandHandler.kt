package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.example.data.ToolResult

/**
 * Handles mapping voice intents (e.g. "Open WhatsApp", "Open Camera", "Open Settings")
 * to actual Android Intent actions to launch installed applications on the device.
 */
class MarcoCommandHandler(private val context: Context) {

    private val knownAppPackages = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "spotify" to "com.spotify.music",
        "gmail" to "com.google.android.gm",
        "email" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "photos" to "com.google.android.apps.photos",
        "drive" to "com.google.android.apps.docs",
        "files" to "com.google.android.apps.nbu.files",
        "clock" to "com.google.android.deskclock",
        "calendar" to "com.google.android.calendar",
        "snapchat" to "com.snapchat.android",
        "telegram" to "org.telegram.messenger",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "linkedin" to "com.linkedin.android"
    )

    fun launchInstalledAppByVoiceCommand(voiceCommand: String): ToolResult {
        val cleanedName = cleanVoiceCommandForAppName(voiceCommand)
        return handleOpenAppIntent(if (cleanedName.isNotBlank()) cleanedName else voiceCommand)
    }

    private fun cleanVoiceCommandForAppName(command: String): String {
        var text = command.trim()
        val removePrefixes = listOf(
            "open app", "open", "launch app", "launch", "start app", "start",
            "kholo", "chalo", "thira", "திற", "ஓபன்"
        )
        for (prefix in removePrefixes) {
            if (text.lowercase().startsWith(prefix)) {
                text = text.substring(prefix.length).trim()
                break
            }
        }
        val removeSuffixes = listOf(
            "open பண்ணு", "open pannu", "kholo", "app", "open", "திற"
        )
        for (suffix in removeSuffixes) {
            if (text.lowercase().endsWith(suffix)) {
                text = text.substring(0, text.length - suffix.length).trim()
                break
            }
        }
        return text
    }

    fun findInstalledPackageName(appName: String): String? {
        val lower = appName.trim().lowercase()
        val mapped = knownAppPackages[lower] ?: knownAppPackages.entries.firstOrNull { lower.contains(it.key) }?.value
        if (mapped != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(mapped)
            if (launchIntent != null) return mapped
        }

        val pm = context.packageManager
        return try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            installedApps.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label.contains(lower) || lower.contains(label)
            }?.packageName
        } catch (e: Exception) {
            null
        }
    }

    fun handleOpenAppIntent(appName: String): ToolResult {
        val lower = appName.trim().lowercase()

        // 1. Check special system category intents
        if (lower.contains("camera")) {
            return launchCameraIntent()
        }
        if (lower.contains("calculator") || lower.contains("calc")) {
            return launchCalculatorIntent()
        }
        if (lower.contains("setting")) {
            return launchSettingsIntent()
        }
        if (lower.contains("gallery") || lower.contains("photos")) {
            return launchGalleryIntent()
        }
        if (lower.contains("clock") || lower.contains("alarm")) {
            return launchClockIntent()
        }
        if (lower.contains("calendar")) {
            return launchCalendarIntent()
        }
        if (lower.contains("phone") || lower.contains("dialer")) {
            return launchDialerIntent()
        }

        // 2. Check direct package name mapping
        val mappedPackage = knownAppPackages[lower] ?: knownAppPackages.entries.firstOrNull { lower.contains(it.key) }?.value
        if (mappedPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(mappedPackage)
            if (launchIntent != null) {
                return executeLaunchIntent(launchIntent, appName, mappedPackage)
            }
        }

        // 3. Scan installed packages on device via PackageManager
        val pm = context.packageManager
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(lower) || lower.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        return executeLaunchIntent(launchIntent, pm.getApplicationLabel(app).toString(), app.packageName)
                    }
                }
            }
        } catch (e: Exception) {
            // Log fallback
        }

        // 4. Category / Generic Intent Fallback
        return launchGenericCategoryIntent(lower, appName)
    }

    private fun launchCameraIntent(): ToolResult {
        return try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolResult(true, "Camera opened successfully.", "Launch Camera")
        } catch (e: Exception) {
            val altIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage("com.android.camera")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            executeLaunchIntent(altIntent, "Camera", "com.android.camera")
        }
    }

    private fun launchCalculatorIntent(): ToolResult {
        val calcIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALCULATOR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(calcIntent)
            ToolResult(true, "Calculator launched.", "Launch Calculator")
        } catch (e: Exception) {
            ToolResult(false, "Calculator app not found.", "App Not Found")
        }
    }

    private fun launchSettingsIntent(): ToolResult {
        return try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            ToolResult(true, "Device Settings opened.", "Launch Settings")
        } catch (e: Exception) {
            ToolResult(false, "Settings app could not be launched.", "Error")
        }
    }

    private fun launchGalleryIntent(): ToolResult {
        return try {
            val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(galleryIntent)
            ToolResult(true, "Gallery photos opened.", "Launch Gallery")
        } catch (e: Exception) {
            ToolResult(false, "Gallery could not be opened.", "Error")
        }
    }

    private fun launchClockIntent(): ToolResult {
        return try {
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(clockIntent)
            ToolResult(true, "Clock & Alarms opened.", "Launch Clock")
        } catch (e: Exception) {
            ToolResult(false, "Clock application not found.", "Error")
        }
    }

    private fun launchCalendarIntent(): ToolResult {
        return try {
            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calendarIntent)
            ToolResult(true, "Calendar opened.", "Launch Calendar")
        } catch (e: Exception) {
            ToolResult(false, "Calendar application not found.", "Error")
        }
    }

    private fun launchDialerIntent(): ToolResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            ToolResult(true, "Phone dialer opened.", "Launch Dialer")
        } catch (e: Exception) {
            ToolResult(false, "Dialer app not found.", "Error")
        }
    }

    private fun launchGenericCategoryIntent(lowerName: String, originalName: String): ToolResult {
        val categoryIntent = when {
            lowerName.contains("browser") || lowerName.contains("web") -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_BROWSER) }
            lowerName.contains("music") || lowerName.contains("song") -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MUSIC) }
            lowerName.contains("message") || lowerName.contains("sms") -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
            lowerName.contains("email") || lowerName.contains("mail") -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_EMAIL) }
            else -> null
        }

        if (categoryIntent != null) {
            categoryIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(categoryIntent)
                return ToolResult(true, "Launched default app for $originalName.", "Launch Category App")
            } catch (e: Exception) {
                // fall through
            }
        }

        return ToolResult(
            success = false,
            message = "Application '$originalName' was not found on this device.",
            actionExecuted = "App Not Found"
        )
    }

    private fun executeLaunchIntent(intent: Intent, appName: String, packageName: String): ToolResult {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult(
                success = true,
                message = "Opened application $appName.",
                actionExecuted = "Launch App",
                details = mapOf("package" to packageName, "appName" to appName)
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                message = "Could not launch $appName: ${e.localizedMessage}",
                actionExecuted = "Launch Failed"
            )
        }
    }
}
