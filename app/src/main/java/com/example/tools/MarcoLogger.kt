package com.example.tools

import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom Timber Tree that outputs debug logs to Logcat and persists WARN/ERROR logs
 * to a local log file for execution error tracking and intent debugging.
 */
class FileLoggingTree(private val context: Context) : Timber.DebugTree() {

    private val logFile: File by lazy {
        File(context.filesDir, LOG_FILE_NAME)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)

        // Capture and persist execution errors and warnings to local log file
        if (priority >= Log.WARN) {
            val timeStr = dateFormat.format(Date())
            val priorityStr = when (priority) {
                Log.WARN -> "WARN"
                Log.ERROR -> "ERROR"
                Log.ASSERT -> "ASSERT"
                else -> "INFO"
            }
            val tagStr = tag ?: "MarcoLogger"
            val logEntry = StringBuilder()
                .append("[$timeStr] [$priorityStr] [$tagStr] $message\n")

            t?.let {
                logEntry.append(Log.getStackTraceString(it)).append("\n")
            }

            writeLogToFile(logEntry.toString())
        }
    }

    @Synchronized
    private fun writeLogToFile(text: String) {
        try {
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
                logFile.writeText("--- Log file rotated due to size limit (${MAX_LOG_SIZE_BYTES / 1024} KB) ---\n")
            }
            FileWriter(logFile, true).use { writer ->
                writer.append(text)
            }
        } catch (e: Exception) {
            Log.e("FileLoggingTree", "Failed to append log entry to file: ${e.message}")
        }
    }

    companion object {
        const val LOG_FILE_NAME = "marco_intent_errors.log"
        const val MAX_LOG_SIZE_BYTES = 500 * 1024L // 500 KB limit
    }
}

/**
 * Centralized Timber-based logging utility to log, store, and inspect execution errors
 * from intent handlers and voice commands.
 */
object MarcoLogger {

    private var isInitialized = false

    @Synchronized
    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        val loggingTree = FileLoggingTree(appContext)
        Timber.plant(loggingTree)
        isInitialized = true
        Timber.tag("MarcoLogger").i("Centralized Timber logging utility initialized.")
    }

    /**
     * Logs an execution error from an intent handler or voice command.
     */
    fun logIntentError(intentAction: String, message: String, throwable: Throwable? = null) {
        Timber.tag("IntentHandler").e(throwable, "Intent execution failed [$intentAction]: $message")
    }

    /**
     * Logs a warning during intent execution.
     */
    fun logIntentWarning(intentAction: String, message: String) {
        Timber.tag("IntentHandler").w("Intent warning [$intentAction]: $message")
    }

    /**
     * Logs a successful intent execution.
     */
    fun logIntentSuccess(intentAction: String, message: String) {
        Timber.tag("IntentHandler").i("Intent executed successfully [$intentAction]: $message")
    }

    /**
     * Retrieves stored log contents from the local error log file.
     */
    fun getLogs(context: Context): String {
        return try {
            val logFile = File(context.applicationContext.filesDir, FileLoggingTree.LOG_FILE_NAME)
            if (logFile.exists() && logFile.length() > 0) {
                logFile.readText()
            } else {
                "No execution errors logged in local storage."
            }
        } catch (e: Exception) {
            "Failed to read local log file: ${e.message}"
        }
    }

    /**
     * Clears local error logs.
     */
    fun clearLogs(context: Context): Boolean {
        return try {
            val logFile = File(context.applicationContext.filesDir, FileLoggingTree.LOG_FILE_NAME)
            if (logFile.exists()) {
                logFile.delete()
            } else true
        } catch (e: Exception) {
            false
        }
    }
}
