package fr.minuteur.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import fr.minuteur.Preset
import fr.minuteur.SetupItem
import fr.minuteur.TimerRepository
import fr.minuteur.TimerState
import fr.minuteur.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MinuteurApp(missingSetup: List<SetupItem>, onFixSetup: (SetupItem) -> Unit) {
    val timers by TimerRepository.timers.collectAsState()
    val recents by TimerRepository.recents.collectAsState()
    val favorites by TimerRepository.favorites.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<KitchenTimer?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(message: String, undo: (() -> Unit)? = null) {
        snackbar.currentSnackbarData?.dismiss()
        scope.launch {
            val result = snackbar.showSnackbar(message, actionLabel = if (undo != null) "Annuler" else null)
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbar) },
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
                val isFavorite = favorites.any { it.sameAs(t.label, t.baseMs) }
                TimerCard(
                    timer = t,
                    now = now,
                    isFavorite = isFavorite,
                    onToggle = {
                        if (t.state == TimerState.RUNNING) TimerRepository.pause(t.id)
                        else TimerRepository.resume(t.id)
                    },
                    onAddMinute = { TimerRepository.addMinute(t.id) },
                    onStop = { TimerRepository.delete(t.id) },
                    onDelete = {
                        TimerRepository.delete(t.id)
                        // Un minuteur terminé qu'on supprime = sonnerie arrêtée, rien à annuler.
                        if (t.state != TimerState.FINISHED) {
                            notify("« ${t.label} » supprimé") { TimerRepository.restore(t) }
                        }
                    },
                    onRename = { renaming = t },
                    onToggleFavorite = {
                        val added = TimerRepository.toggleFavorite(t.label, t.baseMs)
                        notify(
                            if (added) "« ${t.label} » ajouté aux favoris (${formatDuration(t.baseMs)})"
                            else "« ${t.label} » retiré des favoris"
                        )
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (showSheet) {
        AddTimerSheet(
            favorites = favorites,
            recents = recents.filter { r -> favorites.none { it.sameAs(r.label, r.durationMs) } }.take(3),
            onDismiss = { showSheet = false },
            onStart = { label, ms, keep ->
                val t = TimerRepository.add(label, ms)
                if (keep && t != null) TimerRepository.addFavorite(t.label, ms)
            },
        )
    }

    renaming?.let { t ->
        RenameDialog(
            initial = t.label,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                TimerRepository.rename(t.id, name)
                renaming = null
            },
        )
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cream,
        title = { Text("Renommer") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("OK", fontWeight = FontWeight.Bold, color = Coral)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler", color = InkSoft) }
        },
    )
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
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onAddMinute: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onToggleFavorite: () -> Unit,
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
                // Appui sur le nom : renommer.
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onRename)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        timer.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Renommer",
                        tint = InkSoft.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                        tint = if (isFavorite) Gold else InkSoft.copy(alpha = 0.35f),
                    )
                }
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
                            onClick = onStop,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AddTimerSheet(
    favorites: List<Preset>,
    recents: List<Preset>,
    onDismiss: () -> Unit,
    onStart: (label: String, durationMs: Long, keepAsFavorite: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var label by rememberSaveable { mutableStateOf("") }
    var seconds by rememberSaveable { mutableIntStateOf(10 * 60) }
    var keep by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Preset?>(null) }
    val totalMs = seconds * 1000L

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

            Spacer(Modifier.height(14.dp))
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

            Spacer(Modifier.height(16.dp))
            DurationWheels(totalSeconds = seconds, onChange = { seconds = it })

            // Favoris : un appui lance directement, appui long pour modifier.
            if (favorites.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionTitle("Mes favoris")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    favorites.forEach { f ->
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White)
                                .combinedClickable(
                                    onClick = { close { onStart(f.label, f.durationMs, false) } },
                                    onLongClick = { editing = f },
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, null, tint = Gold, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(f.label, fontWeight = FontWeight.SemiBold, color = Ink, maxLines = 1)
                            }
                            Text(formatDuration(f.durationMs), color = InkSoft, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text(
                    "Appui : lancer · appui long : modifier",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Récents : un appui remplit le nom et la durée.
            if (recents.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Récents")
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recents.forEach { r ->
                        FilledTonalButton(
                            onClick = {
                                label = r.label
                                seconds = (r.durationMs / 1000L).toInt().coerceIn(1, MAX_SECONDS)
                            },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text("${r.label} · ${formatDuration(r.durationMs)}", maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { keep = !keep }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Star, null, tint = if (keep) Gold else InkSoft.copy(alpha = 0.35f))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Garder en favori",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = keep,
                    onCheckedChange = { keep = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Coral),
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val l = label
                    val ms = totalMs
                    val k = keep
                    close { onStart(l, ms, k) }
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

    editing?.let { f ->
        FavoriteDialog(
            preset = f,
            onDismiss = { editing = null },
            onSave = { updated ->
                TimerRepository.updateFavorite(f, updated)
                editing = null
            },
            onDelete = {
                TimerRepository.removeFavorite(f)
                editing = null
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = InkSoft,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun FavoriteDialog(
    preset: Preset,
    onDismiss: () -> Unit,
    onSave: (Preset) -> Unit,
    onDelete: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(preset.label) }
    var seconds by rememberSaveable { mutableIntStateOf((preset.durationMs / 1000L).toInt().coerceIn(1, MAX_SECONDS)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Cream,
        title = { Text("Modifier le favori") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Recette") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                DurationWheels(
                    totalSeconds = seconds,
                    onChange = { seconds = it },
                    itemHeight = 44.dp,
                    fontSize = 30.sp,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Preset(name, seconds * 1000L)) },
                enabled = name.isNotBlank() && seconds > 0,
            ) { Text("Enregistrer", fontWeight = FontWeight.Bold, color = Coral) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Supprimer", color = Color(0xFFD93025)) }
                TextButton(onClick = onDismiss) { Text("Annuler", color = InkSoft) }
            }
        },
    )
}
