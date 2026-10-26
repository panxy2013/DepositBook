package com.pxy.depositbook.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pxy.depositbook.MainActivity
import com.pxy.depositbook.R
import com.pxy.depositbook.data.db.AppDatabase
import com.pxy.depositbook.util.DateUtils
import com.pxy.depositbook.util.InterestCalculator
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).depositDao()
        val deposits = dao.getAllOnce().filter { it.isActive }
        val today = DateUtils.today()

        val toRemind = deposits.filter { d ->
            val left = InterestCalculator.daysLeft(d, today)
            left in 0..30
        }
        if (toRemind.isNotEmpty()) {
            notify(applicationContext, toRemind.size)
        }
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "deposit_reminder"
        private const val NOTIFICATION_ID = 1001

        fun notify(context: Context, count: Int) {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            val nm = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "存款到期提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "存款到期前 30 天提醒" }
                nm.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("有 $count 笔存款将在 30 天内到期")
                .setContentText("点击查看并安排续存或取出")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pending)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }
}

object ReminderScheduler {
    private const val WORK_NAME = "deposit_reminder_daily"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
