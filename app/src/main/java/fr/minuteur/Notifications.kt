package fr.minuteur

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

object Notifications {
    private const val CH_TIMERS = "timers"
    private const val CH_RINGING = "ringing"
    private const val CH_DONE = "done"
    const val RINGING_ID = 1
    private const val FALLBACK_ID = 2
    private const val TIMER_BASE_ID = 1000

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    fun createChannels(ctx: Context) {
        val nm = nm(ctx)
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMERS, "Minuteurs en cours", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_RINGING, "Sonnerie", NotificationManager.IMPORTANCE_HIGH).apply {
                // Le son est joué par AlarmService, en boucle.
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Minuteur terminé (secours)", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    private fun openApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, 1,
            Intent(ctx, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun safeNotify(ctx: Context, id: Int, n: Notification) {
        try {
            nm(ctx).notify(id, n)
        } catch (e: SecurityException) {
            // Notifications refusées : l'app reste utilisable.
        }
    }

    fun showTimer(ctx: Context, t: KitchenTimer) {
        val b = NotificationCompat.Builder(ctx, CH_TIMERS)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(t.label)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(ctx))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (t.state == TimerState.RUNNING) {
            b.setWhen(t.endAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setContentText("En cours")
        } else {
            b.setShowWhen(false).setContentText("En pause · ${formatDuration(t.remainingMs)}")
        }
        safeNotify(ctx, TIMER_BASE_ID + t.id, b.build())
    }

    fun cancelTimer(ctx: Context, id: Int) {
        nm(ctx).cancel(TIMER_BASE_ID + id)
    }

    private fun title(labels: List<String>) =
        if (labels.size <= 1) "${labels.firstOrNull() ?: "Minuteur"} : c'est prêt !"
        else "${labels.size} minuteurs terminés"

    fun ringing(ctx: Context, labels: List<String>): Notification {
        val stop = PendingIntent.getService(
            ctx, 2,
            Intent(ctx, AlarmService::class.java).setAction(AlarmService.ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(ctx, CH_RINGING)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title(labels))
            .setContentText(labels.joinToString(" · "))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(openApp(ctx))
            .setFullScreenIntent(openApp(ctx), true)
            .addAction(0, "Arrêter", stop)
            .build()
    }

    /** Si le service de sonnerie ne peut pas démarrer : notification sonore simple. */
    fun showFallback(ctx: Context, labels: List<String>) {
        val n = NotificationCompat.Builder(ctx, CH_DONE)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title(labels))
            .setContentText(labels.joinToString(" · "))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .setFullScreenIntent(openApp(ctx), true)
            .build()
        safeNotify(ctx, FALLBACK_ID, n)
    }
}
