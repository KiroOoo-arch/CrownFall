package com.crownfall.realm.ui.mainmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.ui.components.BannerTitle
import com.crownfall.realm.ui.components.MedievalBackground
import com.crownfall.realm.ui.components.MedievalButton
import com.crownfall.realm.ui.components.StatChip
import com.crownfall.realm.ui.components.StonePanel
import com.crownfall.realm.ui.components.SwordDivider
import com.crownfall.realm.ui.theme.CrownfallPalette

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onStartSinglePlayer: (Difficulty) -> Unit,
    onStartTwoPlayer: () -> Unit,
    onContinue: () -> Unit,
    onHistory: () -> Unit,
    onStatistics: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MedievalBackground {
        Row(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ------------------------------------------------------ heraldry
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CROWNFALL",
                    style = MaterialTheme.typography.displayLarge,
                    color = CrownfallPalette.GoldBright,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "LEGENDS OF THE REALM",
                    style = MaterialTheme.typography.labelLarge,
                    color = CrownfallPalette.ParchmentDim,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                SwordDivider(Modifier.fillMaxWidth(0.7f))
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Command the Kingdom of Dawn against the Empire of Dusk " +
                        "on the ancient stone battlefield. Every capture is a duel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrownfallPalette.Parchment,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Commander", state.playerName)
                    StatChip("Rating", state.rating.toString())
                    StatChip("Battles", state.gamesPlayed.toString())
                    StatChip("Wins", state.wins.toString())
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Achievements unlocked are tracked across every battle you fight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrownfallPalette.ParchmentDim
                )
            }

            // ---------------------------------------------------------- menu
            StonePanel(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                BannerTitle("The War Table", "Choose your path")
                Spacer(Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // The war table already holds up to eight commands, which
                        // overflows a phone in landscape; keep them all reachable.
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.hasSavedGame) {
                        MedievalButton(
                            text = "CONTINUE GAME",
                            supportingText = state.savedGameLabel,
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            accent = CrownfallPalette.RoyalBright
                        )
                        MedievalButton(
                            text = "Discard Saved Match",
                            onClick = viewModel::discardSavedGame,
                            modifier = Modifier.fillMaxWidth(),
                            accent = CrownfallPalette.Iron
                        )
                    }
                    MedievalButton(
                        text = "PLAY",
                        supportingText = "Single player vs ${state.difficulty.displayName} AI",
                        onClick = {
                            viewModel.click()
                            viewModel.openDifficultyPicker()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MedievalButton(
                        text = "TWO PLAYER",
                        supportingText = "Local duel on this device",
                        onClick = {
                            viewModel.click()
                            onStartTwoPlayer()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        accent = CrownfallPalette.IronBright
                    )
                    MedievalButton(
                        text = "MATCH HISTORY",
                        onClick = { viewModel.click(); onHistory() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MedievalButton(
                        text = "STATISTICS",
                        onClick = { viewModel.click(); onStatistics() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MedievalButton(
                        text = "ACHIEVEMENTS",
                        onClick = { viewModel.click(); onAchievements() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    MedievalButton(
                        text = "SETTINGS",
                        onClick = { viewModel.click(); onSettings() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (state.showDifficultyPicker) {
            DifficultyPicker(
                selected = state.difficulty,
                onSelect = { difficulty ->
                    viewModel.selectDifficulty(difficulty)
                    onStartSinglePlayer(difficulty)
                },
                onDismiss = viewModel::dismissDifficultyPicker
            )
        }
    }
}

@Composable
private fun DifficultyPicker(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrownfallPalette.Void.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        StonePanel(modifier = Modifier.width(560.dp)) {
            BannerTitle("AI Difficulty", "Each commander fights differently")
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Difficulty.entries.forEach { difficulty ->
                    DifficultyRow(
                        difficulty = difficulty,
                        selected = difficulty == selected,
                        onClick = { onSelect(difficulty) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            MedievalButton("Back", onDismiss, modifier = Modifier.fillMaxWidth(), accent = CrownfallPalette.Iron)
        }
    }
}

@Composable
private fun DifficultyRow(difficulty: Difficulty, selected: Boolean, onClick: () -> Unit) {
    val accent = if (selected) CrownfallPalette.GoldBright else CrownfallPalette.Iron
    Column(modifier = Modifier.fillMaxWidth()) {
        MedievalButton(
            text = difficulty.displayName,
            supportingText = difficulty.blurb,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            accent = accent
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Depth ${difficulty.maxDepth} · ${"%.1f".format(difficulty.timeLimitMs / 1000f)}s think time" +
                if (difficulty.useTranspositionTable) " · transposition table" else "",
            style = MaterialTheme.typography.bodySmall,
            color = CrownfallPalette.ParchmentDim,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

