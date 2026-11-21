package fr.minuteur

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {
    const val EXTRA_ID = "timer_id"

    private fun manager(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    private fun pending(ctx: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            id,
            Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun canScheduleExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager(ctx).canScheduleExactAlarms()

    fun schedule(ctx: Context, t: KitchenTimer) {
        val am = manager(ctx)
        val pi = pending(ctx, t.id)
        try {
            if (canScheduleExact(ctx)) {
                // setAlarmClock : le type d'alarme le plus fiable, ignoré par
                // les économies de batterie (comme l'app Horloge).
                val show = PendingIntent.getActivity(
                    ctx, 0, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                am.setAlarmClock(AlarmManager.AlarmClockInfo(t.endAt, show), pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.endAt, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.endAt, pi)
        }
    }

    fun cancel(ctx: Context, id: Int) {
        manager(ctx).cancel(pending(ctx, id))
    }
}
