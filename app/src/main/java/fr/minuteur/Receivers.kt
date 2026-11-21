package fr.minuteur

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerRepository.init(context)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        if (id >= 0 && TimerRepository.markFinished(id)) {
            AlarmService.start(context)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerRepository.init(context)
        TimerRepository.rescheduleAll()
    }
}
