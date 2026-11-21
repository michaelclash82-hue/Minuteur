package fr.minuteur

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat

/** Fait sonner et vibrer en boucle tant qu'il reste des minuteurs terminés. */
class AlarmService : Service() {

    companion object {
        const val ACTION_STOP = "fr.minuteur.STOP"

        fun start(ctx: Context) {
            try {
                ContextCompat.startForegroundService(ctx, Intent(ctx, AlarmService::class.java))
            } catch (e: Exception) {
                Notifications.showFallback(ctx, TimerRepository.finishedLabels())
            }
        }

        /** Met à jour la notification si le service tourne déjà (appelé depuis l'app). */
        fun refresh(ctx: Context) {
            try {
                ctx.startService(Intent(ctx, AlarmService::class.java))
            } catch (e: Exception) {
                // App en arrière-plan : la notification sera mise à jour plus tard.
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, AlarmService::class.java))
        }
    }

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TimerRepository.init(this)
        if (intent?.action == ACTION_STOP) {
            TimerRepository.dismissAllFinished()
            stopSelf()
            return START_NOT_STICKY
        }

        val labels = TimerRepository.finishedLabels()
        val notification = Notifications.ringing(this, labels)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifications.RINGING_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(Notifications.RINGING_ID, notification)
        }

        if (labels.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startSound()
        startVibration()
        return START_NOT_STICKY
    }

    private fun startSound() {
        if (player != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val candidates = listOf(
            RingtoneManager.TYPE_ALARM,
            RingtoneManager.TYPE_RINGTONE,
            RingtoneManager.TYPE_NOTIFICATION,
        ).mapNotNull { RingtoneManager.getDefaultUri(it) }

        for (uri in candidates) {
            val p = MediaPlayer()
            try {
                p.setAudioAttributes(attrs)
                p.setDataSource(this, uri)
                p.isLooping = true
                p.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                p.prepare()
                p.start()
                player = p
                return
            } catch (e: Exception) {
                p.release()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun startVibration() {
        if (vibrator != null) return
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            v.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
        vibrator = v
    }

    override fun onDestroy() {
        player?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                // Lecteur déjà arrêté.
            }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
        super.onDestroy()
    }
}
