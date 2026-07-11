package com.formuladock

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.formuladock.core.i18n.AppLanguage
import java.util.Locale

object QuickCalcNotification {
    private const val ChannelId = "quick_calc"
    private const val NotificationId = 1001

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun show(context: Context, language: AppLanguage) {
        createChannel(context, language)

        val intent = Intent(context, QuickCalcActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = getLocalizedString(language, "公式坞", "FormulaDock")
        val text = getLocalizedString(language, "点击打开快捷计算面板", "Click to open the Quick Calculator panel")

        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        NotificationManagerCompat.from(context).notify(NotificationId, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NotificationId)
    }

    private fun createChannel(context: Context, language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val name = getLocalizedString(language, "快捷计算", "Quick Calculator")
        val desc = getLocalizedString(language, "公式坞快捷计算入口", "FormulaDock Quick Calculator entry")

        val channel = NotificationChannel(
            ChannelId,
            name,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = desc
            setShowBadge(false)
        }

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun getLocalizedString(language: AppLanguage, zh: String, en: String): String {
        return when (language) {
            AppLanguage.Chinese -> zh
            AppLanguage.English -> en
            AppLanguage.System -> {
                if (Locale.getDefault().language == "zh") zh else en
            }
        }
    }
}
