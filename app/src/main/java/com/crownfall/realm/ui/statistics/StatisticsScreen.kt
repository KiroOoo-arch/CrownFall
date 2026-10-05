package com.crownfall.realm.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownfall.realm.domain.models.PieceStat
import com.crownfall.realm.ui.components.BannerTitle
import com.crownfall.realm.ui.components.MedievalBackground
import com.crownfall.realm.ui.components.MedievalButton
import com.crownfall.realm.ui.components.ProgressTrack
import com.crownfall.realm.ui.components.StatChip
import com.crownfall.realm.ui.components.StonePanel
import com.crownfall.realm.ui.components.SwordDivider
import com.crownfall.realm.ui.theme.CrownfallPalette

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val pieces by viewModel.pieceStats.collectAsStateWithLifecycle()

    MedievalBackground {
        Row(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StonePanel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                BannerTitle("Player Profile", profile.name)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Rating", profile.rating.toString())
                    StatChip("Games", profile.gamesPlayed.toString())
                    StatChip("Wins", profile.wins.toString())
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Losses", profile.losses.toString())
                    StatChip("Draws", profile.draws.toString())
                    StatChip("Win Rate", profile.winRateText)
                }
                Spacer(Modifier.height(16.dp))
                Text("Win rate", style = MaterialTheme.typography.labelMedium, color = CrownfallPalette.ParchmentDim)
                Spacer(Modifier.height(6.dp))
                ProgressTrack(
                    progress = profile.winRate / 100f,
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
                Spacer(Modifier.height(16.dp))
                SwordDivider(Modifier.fillMaxWidth(0.8f))
                Spacer(Modifier.height(16.dp))
                StatRow("Favourite Piece", profile.favouritePiece)
                StatRow("Total Captures", profile.totalCaptures.toString())
                StatRow("Best Win Streak", profile.bestWinStreak.toString())
                StatRow("Current Streak", profile.currentWinStreak.toString())
                Spacer(Modifier.height(16.dp))
                Text(
                    if (profile.gamesPlayed == 0) {
                        "Fight your first battle to begin recording a legend."
                    } else {
                        "Every capture, promotion and defeat is recorded per soldier."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = CrownfallPalette.ParchmentDim
                )
                Spacer(Modifier.weight(1f))
                MedievalButton("Back", onBack, modifier = Modifier.fillMaxWidth())
            }

            StonePanel(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                BannerTitle("Piece Statistics")
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Soldier", style = MaterialTheme.typography.labelSmall, color = CrownfallPalette.Gold)
                    Text("Moves  Captures  Fallen  Battles", style = MaterialTheme.typography.labelSmall, color = CrownfallPalette.Gold)
                }
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pieces, key = { it.pieceType }) { piece -> PieceStatRow(piece) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Times Captured counts how often each role has fallen in battle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrownfallPalette.ParchmentDim
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CrownfallPalette.ParchmentDim)
        Text(value, style = MaterialTheme.typography.titleMedium, color = CrownfallPalette.GoldBright)
    }
}

@Composable
private fun PieceStatRow(piece: PieceStat) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                piece.medievalName,
                style = MaterialTheme.typography.bodyMedium,
                color = CrownfallPalette.Parchment
            )
            Text(
                "${piece.moves}   ${piece.captures}   ${piece.deaths}   ${piece.gamesUsed}",
                style = MaterialTheme.typography.bodyMedium,
                color = CrownfallPalette.GoldBright,
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(3.dp))
        ProgressTrack(
            progress = if (piece.moves == 0) 0f else piece.captures.toFloat() / piece.moves.coerceAtLeast(1),
            modifier = Modifier.fillMaxWidth().height(4.dp),
            accent = CrownfallPalette.RoyalBright
        )
    }
}
