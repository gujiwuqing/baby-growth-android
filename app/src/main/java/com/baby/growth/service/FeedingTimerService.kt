package com.baby.growth.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.baby.growth.MainActivity
import com.baby.growth.R
import com.baby.growth.utils.BreastfeedingTimer
import kotlinx.coroutines.*

/**
 * 前台服务：母乳喂养计时器
 * 在通知栏显示计时状态，防止 APP 被系统杀掉
 */
class FeedingTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "feeding_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_SIDE = "extra_side"

        fun start(context: Context, side: String) {
            val intent = Intent(context, FeedingTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SIDE, side)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FeedingTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTimerAndService()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                val side = intent.getStringExtra(EXTRA_SIDE) ?: "left"
                BreastfeedingTimer.start(this, side)
                startForeground(NOTIFICATION_ID, buildNotification())
                startNotificationUpdater()
            }
        }

        // 如果计时器正在运行，确保前台服务和通知更新
        if (BreastfeedingTimer.state.value.isRunning) {
            startForeground(NOTIFICATION_ID, buildNotification())
            startNotificationUpdater()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    private fun stopTimerAndService() {
        BreastfeedingTimer.stop(this)
        updateJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startNotificationUpdater() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!BreastfeedingTimer.state.value.isRunning) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    cancel()
                } else {
                    updateNotification()
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val state = BreastfeedingTimer.state.value
        val sideLabel = if (state.side == "left") "左侧" else "右侧"
        val totalSeconds = if (state.side == "left") {
            BreastfeedingTimer.getLeftTotalSeconds()
        } else {
            BreastfeedingTimer.getRightTotalSeconds()
        }
        val timeText = formatSeconds(totalSeconds)

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                putExtra("route", "record/feeding")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FeedingTimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🤱 哺乳计时中")
            .setContentText("$sideLabel $timeText")
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentIntent(contentIntent)
            .addAction(0, "停止", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "哺乳计时器",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示哺乳计时状态"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun formatSeconds(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h${m}m${s}s"
            m > 0 -> "${m}m${s}s"
            else -> "${s}s"
        }
    }
}