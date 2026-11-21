package fr.minuteur

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import fr.minuteur.ui.MinuteurApp
import fr.minuteur.ui.MinuteurTheme

enum class SetupItem { NOTIFICATIONS, EXACT_ALARMS, BATTERY, FULL_SCREEN }

class MainActivity : ComponentActivity() {

    private val missingSetup = mutableStateOf<List<SetupItem>>(emptyList())

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            missingSetup.value = computeMissingSetup()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Affiche l'app par-dessus l'écran verrouillé quand un minuteur sonne.
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        TimerRepository.init(this)

        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasNotificationPermission() && !prefs.getBoolean("askedNotif", false)
        ) {
            prefs.edit().putBoolean("askedNotif", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MinuteurTheme {
                MinuteurApp(missingSetup = missingSetup.value, onFixSetup = ::fixSetup)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        missingSetup.value = computeMissingSetup()
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun computeMissingSetup(): List<SetupItem> = buildList {
        if (!hasNotificationPermission()) add(SetupItem.NOTIFICATIONS)
        if (!AlarmScheduler.canScheduleExact(this@MainActivity)) add(SetupItem.EXACT_ALARMS)
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) add(SetupItem.BATTERY)
        if (Build.VERSION.SDK_INT >= 34 &&
            !getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        ) add(SetupItem.FULL_SCREEN)
    }

    private fun fixSetup(item: SetupItem) {
        val pkg = Uri.parse("package:$packageName")
        when (item) {
            SetupItem.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    open(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    )
                }
            }
            SetupItem.EXACT_ALARMS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
            }
            SetupItem.BATTERY -> {
                if (!open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))) {
                    open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            SetupItem.FULL_SCREEN -> if (Build.VERSION.SDK_INT >= 34) {
                open(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
            }
        }
    }

    private fun open(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
