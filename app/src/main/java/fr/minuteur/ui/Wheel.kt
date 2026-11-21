package fr.minuteur.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

const val MAX_SECONDS = 23 * 3600 + 59 * 60 + 59

/**
 * Trois roulettes heures : minutes : secondes, comme l'horloge du téléphone.
 * [totalSeconds] est la valeur affichée ; [onChange] reçoit la nouvelle valeur.
 */
@Composable
fun DurationWheels(
    totalSeconds: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 52.dp,
    fontSize: TextUnit = 40.sp,
) {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val current by rememberUpdatedState(Triple(h, m, s))

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight * 3),
            contentAlignment = Alignment.Center,
        ) {
            // Bandeau qui met en valeur la ligne sélectionnée.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(Color.White, RoundedCornerShape(18.dp))
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                NumberWheel(h, 24, { v -> onChange(v * 3600 + current.second * 60 + current.third) },
                    Modifier.weight(1f), itemHeight, fontSize)
                Separator(fontSize)
                NumberWheel(m, 60, { v -> onChange(current.first * 3600 + v * 60 + current.third) },
                    Modifier.weight(1f), itemHeight, fontSize)
                Separator(fontSize)
                NumberWheel(s, 60, { v -> onChange(current.first * 3600 + current.second * 60 + v) },
                    Modifier.weight(1f), itemHeight, fontSize)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            listOf("heures", "minutes", "secondes").forEachIndexed { i, unit ->
                if (i > 0) Text(":", fontSize = fontSize, color = Color.Transparent, modifier = Modifier.height(1.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSoft,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Separator(fontSize: TextUnit) {
    Text(":", fontSize = fontSize, fontWeight = FontWeight.Bold, color = Ink)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    value: Int,
    range: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier,
    itemHeight: Dp,
    fontSize: TextUnit,
) {
    // Liste très longue qui boucle (… 58, 59, 00, 01 …), démarrée au milieu.
    val loops = 1000
    val count = range * loops
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (loops / 2) * range + value - 1)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val haptic = LocalHapticFeedback.current

    // Index de l'élément au centre (3 éléments visibles : le 2e).
    val centered by remember(itemPx) {
        derivedStateOf {
            state.firstVisibleItemIndex + 1 +
                if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)

    // Quand la roulette s'arrête, on transmet la valeur centrée.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val v = centered.mod(range)
                if (v != latestValue) latestOnChange(v)
            }
        }
    }

    // Petit « clic » de vibration à chaque cran pendant le défilement.
    LaunchedEffect(state) {
        snapshotFlow { centered }.collect {
            if (state.isScrollInProgress) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Valeur changée de l'extérieur (durée rapide, favori…) : on fait tourner la roulette.
    LaunchedEffect(value) {
        if (state.isScrollInProgress) return@LaunchedEffect
        val shown = centered.mod(range)
        if (shown != value) {
            var delta = (value - shown).mod(range)
            if (delta > range / 2) delta -= range
            state.animateScrollToItem(centered + delta - 1)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        modifier = modifier.height(itemHeight * 3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count) { index ->
            val selected = index == centered
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    String.format(Locale.ROOT, "%02d", index % range),
                    fontSize = fontSize,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (selected) Ink else Ink.copy(alpha = 0.22f),
                    style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}
