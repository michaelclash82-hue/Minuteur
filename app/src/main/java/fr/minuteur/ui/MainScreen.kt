package fr.minuteur.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.minuteur.KitchenTimer
import fr.minuteur.SetupItem
import fr.minuteur.TimerRepository
import fr.minuteur.TimerState
import fr.minuteur.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MinuteurApp(missingSetup: List<SetupItem>, onFixSetup: (SetupItem) -> Unit) {
    val timers by TimerRepository.timers.collectAsState()
    val recent by TimerRepository.recentLabels.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }

    // Terminés en haut, puis ceux qui vont sonner en premier, puis les pauses.
    val sorted = remember(timers) {
        timers.sortedWith(
            compareBy<KitchenTimer>(
                { if (it.state == TimerState.FINISHED) -1 else it.state.ordinal },
                { if (it.state == TimerState.RUNNING) it.endAt else it.remainingMs },
            )
        )
    }
    val running = timers.count { it.state == TimerState.RUNNING }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
            ) {
                Text("Minuteurs", style = MaterialTheme.typography.headlineLarge, color = Ink)
                if (timers.isNotEmpty()) {
                    Text(
                        "$running en cours",
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSoft,
                    )
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                containerColor = Coral,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nouveau", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (missingSetup.isNotEmpty()) {
                item(key = "setup") { SetupCard(missingSetup, onFixSetup) }
            }
            if (sorted.isEmpty()) {
                item(key = "empty") { EmptyState() }
            }
            items(sorted, key = { it.id }) { t ->
                TimerCard(
                    timer = t,
                    now = now,
                    onToggle = {
                        if (t.state == TimerState.RUNNING) TimerRepository.pause(t.id)
                        else TimerRepository.resume(t.id)
                    },
                    onAddMinute = { TimerRepository.addMinute(t.id) },
                    onDelete = { TimerRepository.delete(t.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showSheet) {
        AddTimerSheet(
            recent = recent,
            onDismiss = { showSheet = false },
            onStart = { label, ms -> TimerRepository.add(label, ms) },
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🍲", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Aucun minuteur",
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Appuie sur « Nouveau » pour lancer un minuteur et lui donner le nom de ta recette.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SetupCard(missing: List<SetupItem>, onFix: (SetupItem) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, Color(0xFFFFD166)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFE09B00))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Pour que ça sonne même téléphone verrouillé",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
            }
            Spacer(Modifier.height(8.dp))
            missing.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (item) {
                            SetupItem.NOTIFICATIONS -> "Autoriser les notifications"
                            SetupItem.EXACT_ALARMS -> "Autoriser les alarmes"
                            SetupItem.BATTERY -> "Ne pas optimiser la batterie"
                            SetupItem.FULL_SCREEN -> "Afficher en plein écran à la sonnerie"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSoft,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onFix(item) }) {
                        Text("Régler", fontWeight = FontWeight.Bold, color = Coral)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: KitchenTimer,
    now: Long,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TimerPalette[timer.colorIndex.mod(TimerPalette.size)]
    val finished = timer.state == TimerState.FINISHED
    val paused = timer.state == TimerState.PAUSED

    val container = if (finished) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val pulse by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
            label = "pulse",
        )
        lerp(colors.container, colors.accent, 0.15f + pulse * 0.55f)
    } else {
        colors.container
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (finished) Color.White else colors.accent)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    timer.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Close, contentDescription = "Supprimer", tint = InkSoft)
                }
            }

            Column(Modifier.padding(end = 12.dp)) {
                if (finished) {
                    Text(
                        "C'est prêt !",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onDelete,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Ink,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Arrêter", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onAddMinute,
                            modifier = Modifier.height(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(2.dp, Ink),
                        ) {
                            Text("+1 min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                    }
                } else {
                    Text(
                        formatDuration(timer.remaining(now)),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (paused) Ink.copy(alpha = 0.45f) else Ink,
                        style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.75f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(timer.progress(now))
                                .clip(RoundedCornerShape(5.dp))
                                .background(colors.accent)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(56.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.accent,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (paused) "Reprendre" else "Pause",
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = onAddMinute,
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(2.dp, colors.accent),
                        ) {
                            Text("+1 min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        if (paused) {
                            Spacer(Modifier.width(12.dp))
                            Text("En pause", color = InkSoft, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

private val QuickMinutes = listOf(1, 2, 3, 5, 8, 10, 12, 15, 20, 25, 30, 40, 45, 60, 90)

private fun quickLabel(m: Int): String = when {
    m < 60 -> "$m min"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60} h ${m % 60}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTimerSheet(
    recent: List<String>,
    onDismiss: () -> Unit,
    onStart: (String, Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var label by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableIntStateOf(10) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    val totalMs = (minutes * 60L + seconds) * 1000L

    fun close(then: () -> Unit = {}) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            then()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Cream,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text("Nouveau minuteur", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text("Recette") },
                placeholder = { Text("ex : Lasagnes") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (recent.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recent.forEach { r ->
                        FilledTonalButton(
                            onClick = { label = r },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) { Text(r) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                formatDuration(totalMs),
                fontSize = 60.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Stepper("−1 min", Modifier.weight(1f)) { minutes = (minutes - 1).coerceAtLeast(0) }
                Stepper("+1 min", Modifier.weight(1f)) { minutes = (minutes + 1).coerceAtMost(599) }
                Stepper("−10 s", Modifier.weight(1f)) {
                    val total = (minutes * 60 + seconds - 10).coerceAtLeast(0)
                    minutes = total / 60; seconds = total % 60
                }
                Stepper("+10 s", Modifier.weight(1f)) {
                    val total = (minutes * 60 + seconds + 10).coerceAtMost(599 * 60)
                    minutes = total / 60; seconds = total % 60
                }
            }

            Spacer(Modifier.height(16.dp))
            QuickMinutes.chunked(5).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { m ->
                        val selected = minutes == m && seconds == 0
                        Button(
                            onClick = { minutes = m; seconds = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Coral else Color.White,
                                contentColor = if (selected) Color.White else Ink,
                            ),
                        ) {
                            Text(quickLabel(m), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val l = label
                    val ms = totalMs
                    close { onStart(l, ms) }
                },
                enabled = totalMs > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Lancer", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Stepper(text: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
