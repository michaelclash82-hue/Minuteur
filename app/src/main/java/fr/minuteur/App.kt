package fr.minuteur

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        TimerRepository.init(this)
        TimerRepository.rescheduleAll()
    }
}
