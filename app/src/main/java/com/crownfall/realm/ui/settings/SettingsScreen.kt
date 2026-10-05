package com.crownfall.realm.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownfall.realm.data.repository.AnimationSpeed
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.ui.components.BannerTitle
import com.crownfall.realm.ui.components.MedievalBackground
import com.crownfall.realm.ui.components.MedievalButton
import com.crownfall.realm.ui.components.StonePanel
import com.crownfall.realm.ui.theme.CrownfallPalette

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    var nameDraft by remember(settings.id) { mutableStateOf("") }

    MedievalBackground {
        Row(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ---------------------------------------------------- audio column
            StonePanel(modifier = Modifier.weight(1f).fillMaxHeight(), scrollable = true) {
                BannerTitle("Settings")
                Spacer(Modifier.height(14.dp))
                Text(
                    "Audio",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrownfallPalette.GoldBright
                )
                Spacer(Modifier.height(8.dp))
                VolumeRow(
                    label = "Music Volume",
                    value = settings.musicVolume,
                    onValueChange = viewModel::setMusicVolume
                )
                VolumeRow(
                    label = "Sound Effects",
                    value = settings.soundVolume,
                    onValueChange = viewModel::setSoundVolume
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    label = "Combat Sounds",
                    checked = settings.combatSounds,
                    onCheckedChange = viewModel::setCombatSounds
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedievalButton(
                        text = "Try Steel",
                        onClick = viewModel::previewSword,
                        modifier = Modifier.weight(1f)
                    )
                    MedievalButton(
                        text = "Try Warning",
                        onClick = viewModel::previewCheck,
                        modifier = Modifier.weight(1f)
                    )
                    MedievalButton(
                        text = "Try Fanfare",
                        onClick = viewModel::previewVictory,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "Combat Animation",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrownfallPalette.GoldBright
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    label = "Play capture cinematics",
                    checked = settings.combatAnimation,
                    onCheckedChange = viewModel::setCombatAnimation
                )
                SwitchRow(
                    label = "Screen shake",
                    checked = settings.screenShake,
                    onCheckedChange = viewModel::setScreenShake
                )
                Spacer(Modifier.height(8.dp))
                Text("Animation Speed", style = MaterialTheme.typography.labelMedium, color = CrownfallPalette.ParchmentDim)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimationSpeed.entries.forEach { speed ->
                        MedievalButton(
                            text = speed.displayName,
                            onClick = { viewModel.setAnimationSpeed(speed) },
                            modifier = Modifier.weight(1f),
                            accent = if (settings.animationSpeed == speed.name) {
                                CrownfallPalette.GoldBright
                            } else {
                                CrownfallPalette.Iron
                            }
                        )
                    }
                }
            }

            // ------------------------------------------------- board + profile
            StonePanel(modifier = Modifier.weight(1f).fillMaxHeight(), scrollable = true) {
                Text(
                    "Battlefield",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrownfallPalette.GoldBright
                )
                Spacer(Modifier.height(8.dp))
                SwitchRow(
                    label = "Show legal move hints",
                    checked = settings.showLegalMoveHints,
                    onCheckedChange = viewModel::setHints
                )
                SwitchRow(
                    label = "Highlight last move",
                    checked = settings.showLastMove,
                    onCheckedChange = viewModel::setLastMove
                )
                SwitchRow(
                    label = "Show coordinates",
                    checked = settings.showCoordinates,
                    onCheckedChange = viewModel::setCoordinates
                )
                SwitchRow(
                    label = "Rotate board for the active player (two player)",
                    checked = settings.boardRotation,
                    onCheckedChange = viewModel::setBoardRotation
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "AI Difficulty",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrownfallPalette.GoldBright
                )
                Spacer(Modifier.height(8.dp))
                // Two columns so names like "Beginner" never wrap mid-word.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Difficulty.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pair.forEach { difficulty ->
                                MedievalButton(
                                    text = difficulty.displayName,
                                    onClick = { viewModel.setDefaultDifficulty(difficulty) },
                                    modifier = Modifier.weight(1f),
                                    accent = if (settings.defaultDifficulty == difficulty.name) {
                                        CrownfallPalette.GoldBright
                                    } else {
                                        CrownfallPalette.Iron
                                    }
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Commander Name",
                    style = MaterialTheme.typography.titleLarge,
                    color = CrownfallPalette.GoldBright
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = nameDraft,
                        onValueChange = { nameDraft = it.take(24) },
                        label = { Text("New name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    MedievalButton(
                        text = "Rename",
                        onClick = { if (nameDraft.isNotBlank()) viewModel.setPlayerName(nameDraft.trim()) },
                        enabled = nameDraft.isNotBlank()
                    )
                }

                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(
                        "Danger",
                        style = MaterialTheme.typography.titleLarge,
                        color = CrownfallPalette.RoyalBright
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Resetting wipes match history, statistics and achievement progress. Settings return to their defaults.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrownfallPalette.ParchmentDim
                    )
                    Spacer(Modifier.height(8.dp))
                    MedievalButton(
                        text = "Reset All Progress",
                        onClick = viewModel::resetProgress,
                        accent = CrownfallPalette.RoyalBright
                    )
                }

                Spacer(Modifier.height(10.dp))
                MedievalButton("Back", onBack, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun VolumeRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = CrownfallPalette.Parchment)
            Text(
                "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = CrownfallPalette.Gold
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = CrownfallPalette.Parchment,
                textAlign = TextAlign.Start
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
