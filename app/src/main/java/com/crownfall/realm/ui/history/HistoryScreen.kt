package com.crownfall.realm.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownfall.realm.domain.models.GameResult
import com.crownfall.realm.domain.models.MatchDetail
import com.crownfall.realm.domain.models.MatchSummary
import com.crownfall.realm.ui.components.BannerTitle
import com.crownfall.realm.ui.components.MedievalBackground
import com.crownfall.realm.ui.components.MedievalButton
import com.crownfall.realm.ui.components.ProgressTrack
import com.crownfall.realm.ui.components.StatChip
import com.crownfall.realm.ui.components.StonePanel
import com.crownfall.realm.ui.theme.CrownfallPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    MedievalBackground {
        Row(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StonePanel(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                BannerTitle("Match History", "${matches.size} recorded battles")
                Spacer(Modifier.height(12.dp))
                if (matches.isEmpty()) {
                    Text(
                        "No battles have been fought yet. Finish a match and it will be recorded here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrownfallPalette.ParchmentDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(matches, key = { it.gameId }) { match ->
                            MatchCard(
                                match = match,
                                selected = detail.selected?.summary?.gameId == match.gameId,
                                onClick = { viewModel.open(match.gameId) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedievalButton("Back", onBack, modifier = Modifier.weight(1f))
                    MedievalButton(
                        "Clear History",
                        onClick = viewModel::clearHistory,
                        modifier = Modifier.weight(1f),
                        accent = CrownfallPalette.RoyalBright,
                        enabled = matches.isNotEmpty()
                    )
                }
            }

            StonePanel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val selected = detail.selected
                if (selected == null) {
                    Text(
                        "Select a match to inspect the full move list, duration and captures.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrownfallPalette.ParchmentDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                    )
                } else {
                    DetailBody(selected)
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: MatchSummary, selected: Boolean, onClick: () -> Unit) {
    val accent = when (match.result) {
        GameResult.VICTORY -> CrownfallPalette.GoldBright
        GameResult.DEFEAT -> CrownfallPalette.RoyalBright
        GameResult.DRAW -> CrownfallPalette.IronBright
        GameResult.IN_PROGRESS -> CrownfallPalette.ParchmentDim
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) CrownfallPalette.Stone
                else CrownfallPalette.StoneDeep.copy(alpha = 0.7f)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                match.result.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                formatDate(match.endTime),
                style = MaterialTheme.typography.bodySmall,
                color = CrownfallPalette.ParchmentDim
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${match.player1}  vs  ${match.player2}",
            style = MaterialTheme.typography.bodyMedium,
            color = CrownfallPalette.Parchment
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatChip("Difficulty", match.difficulty?.displayName ?: "Local")
            StatChip("Ply", match.totalMoves.toString())
            StatChip("Mode", match.mode.displayName)
        }
        if (match.winner != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Winner: ${match.winner}",
                style = MaterialTheme.typography.bodySmall,
                color = CrownfallPalette.ParchmentDim
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                "No victor — the field was left even",
                style = MaterialTheme.typography.bodySmall,
                color = CrownfallPalette.ParchmentDim
            )
        }
    }
}

@Composable
private fun ColumnScope.DetailBody(detail: MatchDetail) {
    val summary = detail.summary
    BannerTitle(
        title = summary.result.displayName,
        subtitle = "${summary.player1} vs ${summary.player2}"
    )
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatChip("Difficulty", summary.difficulty?.displayName ?: "Local")
        StatChip("Moves", summary.totalMoves.toString())
        StatChip("Duration", formatDuration(summary.durationMs))
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatChip("Dawn took", detail.capturedByDawn.toString())
        StatChip("Dusk took", detail.capturedByDusk.toString())
        StatChip("Winner", summary.winner ?: "Draw")
    }
    Spacer(Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Move list", style = MaterialTheme.typography.labelMedium, color = CrownfallPalette.Gold)
        Text(
            "${detail.moves.size} plies",
            style = MaterialTheme.typography.bodySmall,
            color = CrownfallPalette.ParchmentDim
        )
    }
    Spacer(Modifier.height(6.dp))
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
    ) {
        detail.moves.forEach { move ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = "${move.moveNumber}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrownfallPalette.ParchmentDim,
                    modifier = Modifier.width(34.dp)
                )
                Column {
                    Text(
                        text = move.displayLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrownfallPalette.Parchment
                    )
                    Text(
                        text = move.notation,
                        style = MaterialTheme.typography.bodySmall,
                        color = CrownfallPalette.ParchmentDim
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    ProgressTrack(
        progress = if (summary.totalMoves == 0) 0f else detail.moves.size.toFloat() / summary.totalMoves,
        modifier = Modifier.fillMaxWidth().height(6.dp)
    )
}

private fun formatDate(epochMillis: Long): String {
    val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)
    return formatter.format(Date(epochMillis))
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
