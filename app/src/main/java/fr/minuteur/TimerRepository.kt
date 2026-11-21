package fr.minuteur

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class TimerState { RUNNING, PAUSED, FINISHED }

data class KitchenTimer(
    val id: Int,
    val label: String,
    val totalMs: Long,
    /** Heure de fin (horloge murale) quand le minuteur tourne. */
    val endAt: Long,
    /** Temps restant figé quand le minuteur est en pause. */
    val remainingMs: Long,
    val state: TimerState,
    val colorIndex: Int,
) {
    fun remaining(now: Long): Long = when (state) {
        TimerState.RUNNING -> (endAt - now).coerceAtLeast(0L)
        TimerState.PAUSED -> remainingMs
        TimerState.FINISHED -> 0L
    }

    fun progress(now: Long): Float =
        if (totalMs <= 0L) 0f else (remaining(now).toFloat() / totalMs).coerceIn(0f, 1f)
}

const val COLOR_COUNT = 7

/**
 * Source unique de vérité pour les minuteurs. Sauvegarde dans les préférences
 * et synchronise les alarmes système et les notifications à chaque changement.
 */
object TimerRepository {
    private const val PREFS = "minuteur"
    private const val KEY_TIMERS = "timers"
    private const val KEY_RECENT = "recent"
    private const val KEY_NEXT_ID = "nextId"

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences

    private val _timers = MutableStateFlow<List<KitchenTimer>>(emptyList())
    val timers: StateFlow<List<KitchenTimer>> = _timers.asStateFlow()

    private val _recent = MutableStateFlow<List<String>>(emptyList())
    val recentLabels: StateFlow<List<String>> = _recent.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _timers.value = loadTimers()
        _recent.value = loadRecent()
    }

    @Synchronized
    fun add(label: String, durationMs: Long) {
        if (durationMs <= 0L) return
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).commit()

        val current = _timers.value
        val color = (0 until COLOR_COUNT).firstOrNull { c -> current.none { it.colorIndex == c } }
            ?: (id % COLOR_COUNT)
        val clean = label.trim()
        val timer = KitchenTimer(
            id = id,
            label = clean.ifEmpty { "Minuteur $id" },
            totalMs = durationMs,
            endAt = System.currentTimeMillis() + durationMs,
            remainingMs = durationMs,
            state = TimerState.RUNNING,
            colorIndex = color,
        )
        commit(current + timer)
        applySideEffects(id, timer)

        if (clean.isNotEmpty()) {
            val recent = (listOf(clean) + _recent.value.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
            _recent.value = recent
            prefs.edit().putString(KEY_RECENT, JSONArray(recent).toString()).commit()
        }
    }

    fun pause(id: Int) = update(id) { t ->
        if (t.state != TimerState.RUNNING) t
        else t.copy(state = TimerState.PAUSED, remainingMs = t.remaining(System.currentTimeMillis()))
    }

    fun resume(id: Int) = update(id) { t ->
        if (t.state != TimerState.PAUSED) t
        else t.copy(state = TimerState.RUNNING, endAt = System.currentTimeMillis() + t.remainingMs)
    }

    fun addMinute(id: Int) {
        update(id) { t ->
            when (t.state) {
                TimerState.RUNNING -> t.copy(endAt = t.endAt + 60_000L, totalMs = t.totalMs + 60_000L)
                TimerState.PAUSED -> t.copy(remainingMs = t.remainingMs + 60_000L, totalMs = t.totalMs + 60_000L)
                TimerState.FINISHED -> t.copy(
                    state = TimerState.RUNNING,
                    endAt = System.currentTimeMillis() + 60_000L,
                    totalMs = 60_000L,
                    remainingMs = 60_000L,
                )
            }
        }
        refreshAlarm()
    }

    fun delete(id: Int) {
        update(id) { null }
        refreshAlarm()
    }

    /** Appelé par l'alarme système. Renvoie true s'il faut faire sonner. */
    @Synchronized
    fun markFinished(id: Int): Boolean {
        val t = _timers.value.firstOrNull { it.id == id } ?: return false
        if (t.state != TimerState.RUNNING) return false
        if (t.endAt > System.currentTimeMillis() + 2_000L) {
            // Alarme en avance (minuteur prolongé entre-temps) : on reprogramme.
            applySideEffects(id, t)
            return false
        }
        update(id) { it.copy(state = TimerState.FINISHED, remainingMs = 0L) }
        return true
    }

    /** Arrête la sonnerie : retire tous les minuteurs terminés. */
    @Synchronized
    fun dismissAllFinished() {
        val finished = _timers.value.filter { it.state == TimerState.FINISHED }
        if (finished.isEmpty()) return
        commit(_timers.value.filterNot { it.state == TimerState.FINISHED })
        finished.forEach { applySideEffects(it.id, null) }
    }

    fun finishedLabels(): List<String> =
        _timers.value.filter { it.state == TimerState.FINISHED }.map { it.label }

    /** Reprogramme les alarmes (démarrage de l'app, redémarrage du téléphone, mise à jour). */
    @Synchronized
    fun rescheduleAll() {
        _timers.value.forEach { applySideEffects(it.id, it) }
    }

    @Synchronized
    private fun update(id: Int, transform: (KitchenTimer) -> KitchenTimer?) {
        val list = _timers.value
        val old = list.firstOrNull { it.id == id } ?: return
        val new = transform(old)
        if (new == old) return
        commit(if (new == null) list.filterNot { it.id == id } else list.map { if (it.id == id) new else it })
        applySideEffects(id, new)
    }

    private fun refreshAlarm() {
        if (_timers.value.none { it.state == TimerState.FINISHED }) AlarmService.stop(app)
        else AlarmService.refresh(app)
    }

    private fun applySideEffects(id: Int, t: KitchenTimer?) {
        when (t?.state) {
            null -> {
                AlarmScheduler.cancel(app, id)
                Notifications.cancelTimer(app, id)
            }
            TimerState.RUNNING -> {
                AlarmScheduler.schedule(app, t)
                Notifications.showTimer(app, t)
            }
            TimerState.PAUSED -> {
                AlarmScheduler.cancel(app, id)
                Notifications.showTimer(app, t)
            }
            TimerState.FINISHED -> {
                AlarmScheduler.cancel(app, id)
                Notifications.cancelTimer(app, id)
            }
        }
    }

    private fun commit(list: List<KitchenTimer>) {
        _timers.value = list
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("label", it.label)
                    .put("total", it.totalMs)
                    .put("endAt", it.endAt)
                    .put("remaining", it.remainingMs)
                    .put("state", it.state.name)
                    .put("color", it.colorIndex)
            )
        }
        prefs.edit().putString(KEY_TIMERS, arr.toString()).commit()
    }

    private fun loadTimers(): List<KitchenTimer> = try {
        val arr = JSONArray(prefs.getString(KEY_TIMERS, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            KitchenTimer(
                id = o.getInt("id"),
                label = o.getString("label"),
                totalMs = o.getLong("total"),
                endAt = o.getLong("endAt"),
                remainingMs = o.getLong("remaining"),
                state = TimerState.valueOf(o.getString("state")),
                colorIndex = o.optInt("color", 0),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun loadRecent(): List<String> = try {
        val arr = JSONArray(prefs.getString(KEY_RECENT, "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}
