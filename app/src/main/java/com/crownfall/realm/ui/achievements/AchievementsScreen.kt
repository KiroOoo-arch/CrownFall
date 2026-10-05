package com.crownfall.realm.ui.achievements

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownfall.realm.data.entities.AchievementEntity
import com.crownfall.realm.ui.components.BannerTitle
import com.crownfall.realm.ui.components.MedievalBackground
import com.crownfall.realm.ui.components.MedievalButton
import com.crownfall.realm.ui.components.ProgressTrack
import com.crownfall.realm.ui.components.StatChip
import com.crownfall.realm.ui.components.StonePanel
import com.crownfall.realm.ui.theme.CrownfallPalette

@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    MedievalBackground {
        Row(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StonePanel(modifier = Modifier.weight(1f).fillMaxHeight()) {
                BannerTitle("Achievements", "Deeds remembered in the realm")
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("Unlocked", "${summary.unlocked} / ${summary.total.coerceAtLeast(items.size)}")
                    StatChip("Remaining", "${(summary.total.coerceAtLeast(items.size) - summary.unlocked).coerceAtLeast(0)}")
                }
                Spacer(Modifier.height(10.dp))
                ProgressTrack(
                    progress = if (items.isEmpty()) 0f else summary.unlocked.toFloat() / items.size,
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.achievementId }) { achievement ->
                        AchievementRow(achievement)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedievalButton("Refresh Progress", viewModel::refresh, modifier = Modifier.weight(1f))
                    MedievalButton("Back", onBack, modifier = Modifier.weight(1f), accent = CrownfallPalette.Iron)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: AchievementEntity) {
    val accent = if (achievement.unlocked) CrownfallPalette.GoldBright else CrownfallPalette.Iron
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CrownfallPalette.StoneDeep.copy(alpha = 0.7f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                achievement.name,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
            Text(
                if (achievement.unlocked) "UNLOCKED" else "${achievement.progress}/${achievement.target}",
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            achievement.description,
            style = MaterialTheme.typography.bodySmall,
            color = CrownfallPalette.ParchmentDim
        )
        Spacer(Modifier.height(6.dp))
        ProgressTrack(
            progress = achievement.ratio,
            modifier = Modifier.fillMaxWidth().height(6.dp),
            accent = accent
        )
    }
}
