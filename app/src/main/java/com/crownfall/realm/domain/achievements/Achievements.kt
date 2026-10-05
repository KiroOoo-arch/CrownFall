package com.crownfall.realm.domain.achievements

import com.crownfall.realm.data.entities.GameStatisticsEntity

/** Which lifetime counter feeds an achievement. */
enum class AchievementMetric {
    TOTAL_CAPTURES,
    KNIGHT_CAPTURES,
    WINS_WITHOUT_QUEEN_LOSS,
    CHECKMATES_DELIVERED,
    GAMES_PLAYED,
    EXPERT_WINS,
    MASTER_WINS
}

data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val target: Int,
    val metric: AchievementMetric
)

/**
 * The full achievement catalogue. Progress is always derived from the stored
 * statistics, so unlocking works retroactively and survives reinstalls of data.
 */
object Achievements {

    val ALL: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "first_blood",
            name = "First Blood",
            description = "Capture your first enemy piece.",
            target = 1,
            metric = AchievementMetric.TOTAL_CAPTURES
        ),
        AchievementDefinition(
            id = "knight_commander",
            name = "Knight Commander",
            description = "Capture 10 pieces using Knights.",
            target = 10,
            metric = AchievementMetric.KNIGHT_CAPTURES
        ),
        AchievementDefinition(
            id = "royal_victory",
            name = "Royal Victory",
            description = "Win a game without losing your Queen.",
            target = 1,
            metric = AchievementMetric.WINS_WITHOUT_QUEEN_LOSS
        ),
        AchievementDefinition(
            id = "checkmate",
            name = "Checkmate",
            description = "Deliver your first checkmate.",
            target = 1,
            metric = AchievementMetric.CHECKMATES_DELIVERED
        ),
        AchievementDefinition(
            id = "veteran",
            name = "Veteran",
            description = "Play 50 games.",
            target = 50,
            metric = AchievementMetric.GAMES_PLAYED
        ),
        AchievementDefinition(
            id = "grand_strategist",
            name = "Grand Strategist",
            description = "Win a game on Expert.",
            target = 1,
            metric = AchievementMetric.EXPERT_WINS
        ),
        AchievementDefinition(
            id = "master_of_war",
            name = "Master of War",
            description = "Defeat the Master AI.",
            target = 1,
            metric = AchievementMetric.MASTER_WINS
        )
    )

    fun progress(def: AchievementDefinition, stats: GameStatisticsEntity): Int = when (def.metric) {
        AchievementMetric.TOTAL_CAPTURES -> stats.totalCaptures
        AchievementMetric.KNIGHT_CAPTURES -> stats.knightCaptures
        AchievementMetric.WINS_WITHOUT_QUEEN_LOSS -> stats.winsWithoutQueenLoss
        AchievementMetric.CHECKMATES_DELIVERED -> stats.checkmatesDelivered
        AchievementMetric.GAMES_PLAYED -> stats.gamesPlayed
        AchievementMetric.EXPERT_WINS -> stats.expertWins
        AchievementMetric.MASTER_WINS -> stats.masterWins
    }

    fun definition(id: String): AchievementDefinition? = ALL.firstOrNull { it.id == id }
}
