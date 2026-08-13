package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.voice.LocalKeywordSpotter

class MarcoForegroundService : Service() {

    private var keywordSpotter: LocalKeywordSpotter? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }

        startBackgroundHotwordSpotting()

        return START_STICKY
    }

    private fun startBackgroundHotwordSpotting() {
        if (keywordSpotter == null) {
            keywordSpotter = LocalKeywordSpotter(this).apply {
                onKeywordDetectedListener = { keyword ->
                    playWakeChime()
                    launchHandsFreeAssistant()
                }
                startSpotting()
            }
        }
    }

    private fun playWakeChime() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (e: Exception) {
            // ignore sound tone error
        }
    }

    private fun launchHandsFreeAssistant() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(EXTRA_START_VOICE_LISTENING, true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        keywordSpotter?.stopSpotting()
        keywordSpotter = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MARCO Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps MARCO background hotword detection active hands-free"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_VOICE_LISTENING, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MARCO Hands-Free Voice Match")
            .setContentText("Listening in background for 'Hey MARCO'...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "marco_service_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_START_VOICE_LISTENING = "extra_start_voice_listening"

        fun start(context: Context) {
            val intent = Intent(context, MarcoForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MarcoForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
