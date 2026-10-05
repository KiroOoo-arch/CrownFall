package com.crownfall.realm.data.repository

import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.models.FinishedGame
import com.crownfall.realm.domain.models.GameMode
import com.crownfall.realm.domain.models.GameResult
import com.crownfall.realm.domain.models.PlayedMove
import com.crownfall.realm.domain.models.SavedGame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RepositoryTest {

    private lateinit var playerDao: FakePlayerDao
    private lateinit var gameDao: FakeGameDao
    private lateinit var moveDao: FakeMoveDao
    private lateinit var pieceDao: FakePieceStatisticsDao
    private lateinit var statsDao: FakeGameStatisticsDao
    private lateinit var achievementDao: FakeAchievementDao
    private lateinit var savedDao: FakeSavedGameDao

    private lateinit var statistics: StatisticsRepository
    private lateinit var achievements: AchievementRepository
    private lateinit var games: GameRepository

    @Before
    fun setUp() = runBlocking {
        playerDao = FakePlayerDao()
        gameDao = FakeGameDao()
        moveDao = FakeMoveDao()
        pieceDao = FakePieceStatisticsDao()
        statsDao = FakeGameStatisticsDao()
        achievementDao = FakeAchievementDao()
        savedDao = FakeSavedGameDao()

        statistics = StatisticsRepository(playerDao, statsDao, pieceDao)
        achievements = AchievementRepository(achievementDao, statistics)
        games = GameRepository(gameDao, moveDao, savedDao, statistics, achievements)

        statistics.ensureSeeded("Tester", Difficulty.HARD)
        achievements.ensureSeeded()
    }

    private fun move(
        number: Int,
        side: Faction,
        piece: PieceType,
        from: String,
        to: String,
        captured: PieceType? = null
    ) = PlayedMove(
        moveNumber = number,
        side = side,
        piece = piece,
        from = from,
        to = to,
        captured = captured,
        promotion = null,
        notation = "${piece.symbol.uppercaseChar()}$from-$to",
        timestamp = 1_000L + number
    )

    private fun game(
        result: GameResult,
        difficulty: Difficulty? = Difficulty.HARD,
        moves: List<PlayedMove> = emptyList(),
        dawnLostQueen: Boolean = false,
        checkmate: Boolean = false,
        mode: GameMode = GameMode.SINGLE_PLAYER,
        start: Long = 0L,
        end: Long = 60_000L,
        playerName: String = "Tester"
    ) = FinishedGame(
        mode = mode,
        difficulty = difficulty,
        playerName = playerName,
        opponentName = if (mode == GameMode.TWO_PLAYER) "Second Player" else "Master AI",
        startTime = start,
        endTime = end,
        winnerName = when (result) {
            GameResult.VICTORY -> "Kingdom of Dawn"
            GameResult.DEFEAT -> "Empire of Dusk"
            else -> null
        },
        result = result,
        moves = moves,
        dawnLostQueen = dawnLostQueen,
        checkmateDelivered = checkmate
    )

    @Test
    fun finishedGameIsPersistedWithItsMoves() = runBlocking {
        val moves = listOf(
            move(1, Faction.DAWN, PieceType.KNIGHT, "g1", "f3"),
            move(2, Faction.DUSK, PieceType.PAWN, "e7", "e5"),
            move(3, Faction.DAWN, PieceType.KNIGHT, "f3", "e5", captured = PieceType.PAWN)
        )
        val outcome = games.recordFinishedGame(game(GameResult.VICTORY, moves = moves, checkmate = true))

        assertTrue(outcome.gameId > 0)
        assertEquals(1, gameDao.count())
        assertEquals(3, moveDao.countFor(outcome.gameId))
        assertEquals(1, statsDao.stored!!.gamesPlayed)
        assertEquals(1, statsDao.stored!!.wins)
        assertEquals(1, statsDao.stored!!.totalCaptures)
        assertEquals(1, statsDao.stored!!.knightCaptures)
        assertEquals(1, statsDao.stored!!.checkmatesDelivered)
        assertEquals(1, statsDao.stored!!.currentWinStreak)
        assertTrue(outcome.newlyUnlocked.any { it.achievementId == "first_blood" })
        assertFalse(outcome.newlyUnlocked.any { it.achievementId == "knight_commander" })
    }

    @Test
    fun matchHistoryListsNewestFirstAndKeepsMovesOrdered() = runBlocking {
        val first = games.recordFinishedGame(game(GameResult.DEFEAT, start = 0, end = 1_000))
        val second = games.recordFinishedGame(
            game(
                GameResult.VICTORY, start = 2_000, end = 3_000,
                moves = listOf(
                    move(1, Faction.DAWN, PieceType.PAWN, "e2", "e4"),
                    move(2, Faction.DUSK, PieceType.PAWN, "e7", "e5")
                )
            )
        )

        val history = games.observeMatches().first()
        assertEquals(2, history.size)
        assertEquals(second.gameId, history.first().gameId)
        assertEquals(GameResult.VICTORY, history.first().result)

        val detail = games.matchDetail(second.gameId)
        assertNotNull(detail)
        assertEquals(2, detail!!.moves.size)
        assertEquals("e2", detail.moves.first().from)
        assertEquals("e5", detail.moves.last().to)
        assertEquals(Faction.DUSK, detail.moves.last().side)
        assertEquals(0, detail.capturedByDawn + detail.capturedByDusk)
        assertNotNull(games.matchDetail(first.gameId))
        assertNull(games.matchDetail(9_999L))
    }

    @Test
    fun profileAggregatesWinsLossesDrawsAndFavouritePiece() = runBlocking {
        games.recordFinishedGame(
            game(
                GameResult.VICTORY,
                moves = listOf(
                    move(1, Faction.DAWN, PieceType.KNIGHT, "g1", "f3"),
                    move(2, Faction.DUSK, PieceType.PAWN, "e7", "e5"),
                    move(3, Faction.DAWN, PieceType.KNIGHT, "f3", "g5"),
                    move(4, Faction.DUSK, PieceType.PAWN, "d7", "d5"),
                    move(5, Faction.DAWN, PieceType.KNIGHT, "g5", "f7", captured = PieceType.PAWN)
                )
            )
        )
        games.recordFinishedGame(game(GameResult.DEFEAT))
        games.recordFinishedGame(game(GameResult.DRAW))

        val profile = statistics.profile()
        assertEquals(3, profile.gamesPlayed)
        assertEquals(1, profile.wins)
        assertEquals(1, profile.losses)
        assertEquals(1, profile.draws)
        assertEquals("Warhorse Knight", profile.favouritePiece)
        assertEquals(1, profile.totalCaptures)
        assertEquals(33.3f, profile.winRate, 0.05f)
        assertEquals(5, profile.pieceStats.sumOf { it.moves })

        val knight = profile.pieceStats.first { it.pieceType == "KNIGHT" }
        assertEquals(3, knight.moves)
        assertEquals(1, knight.captures)
        assertEquals(0, knight.deaths)
        assertEquals(1, knight.gamesUsed)
        val pawn = profile.pieceStats.first { it.pieceType == "PAWN" }
        assertEquals(1, pawn.deaths)
    }

    @Test
    fun localDuelKeepsTheSoloCommanderName() = runBlocking {
        games.recordFinishedGame(
            game(GameResult.DEFEAT, mode = GameMode.TWO_PLAYER, playerName = "Kingdom of Dawn")
        )

        // A local duel has two commanders, so the solo profile must not be renamed.
        assertEquals("Tester", statistics.profile().name)
        assertEquals(1, statistics.profile().losses)
    }

    @Test
    fun soloBattleAdoptsTheCommanderName() = runBlocking {
        games.recordFinishedGame(game(GameResult.VICTORY, playerName = "Champion"))

        assertEquals("Champion", statistics.profile().name)
    }

    @Test
    fun winStreakIsTrackedAndResetsOnLoss() = runBlocking {
        games.recordFinishedGame(game(GameResult.VICTORY))
        games.recordFinishedGame(game(GameResult.VICTORY))
        games.recordFinishedGame(game(GameResult.VICTORY))
        assertEquals(3, statsDao.stored!!.currentWinStreak)
        assertEquals(3, statsDao.stored!!.bestWinStreak)

        games.recordFinishedGame(game(GameResult.DEFEAT))
        assertEquals(0, statsDao.stored!!.currentWinStreak)
        assertEquals(3, statsDao.stored!!.bestWinStreak)
    }

    @Test
    fun veteranUnlocksAfterFiftyGames() = runBlocking {
        repeat(49) { games.recordFinishedGame(game(GameResult.DRAW)) }
        assertFalse(achievements.all().first { it.achievementId == "veteran" }.unlocked)

        val outcome = games.recordFinishedGame(game(GameResult.DRAW))
        val veteran = outcome.achievements.first { it.achievementId == "veteran" }
        assertTrue(veteran.unlocked)
        assertEquals(50, veteran.progress)
        assertTrue(outcome.newlyUnlocked.any { it.achievementId == "veteran" })
    }

    @Test
    fun difficultySpecificAchievementsUnlock() = runBlocking {
        games.recordFinishedGame(game(GameResult.VICTORY, difficulty = Difficulty.EXPERT))
        assertTrue(achievements.all().first { it.achievementId == "grand_strategist" }.unlocked)
        assertFalse(achievements.all().first { it.achievementId == "master_of_war" }.unlocked)

        games.recordFinishedGame(game(GameResult.VICTORY, difficulty = Difficulty.MASTER))
        assertTrue(achievements.all().first { it.achievementId == "master_of_war" }.unlocked)

        games.recordFinishedGame(game(GameResult.VICTORY, difficulty = Difficulty.MASTER, dawnLostQueen = true))
        // royal_victory was already earned by the earlier lossless win.
        assertTrue(achievements.all().first { it.achievementId == "royal_victory" }.unlocked)
    }

    @Test
    fun achievementsNeverRevertOnceUnlocked() = runBlocking {
        games.recordFinishedGame(
            game(
                GameResult.VICTORY,
                moves = listOf(move(1, Faction.DAWN, PieceType.PAWN, "e2", "e4", captured = PieceType.KNIGHT))
            )
        )
        val unlocked = achievements.all().first { it.achievementId == "first_blood" }
        assertTrue(unlocked.unlocked)
        assertNotNull(unlocked.unlockedAt)

        // A later game with no captures must not erase the earlier unlock.
        games.recordFinishedGame(game(GameResult.DEFEAT))
        assertTrue(achievements.all().first { it.achievementId == "first_blood" }.unlocked)
    }

    @Test
    fun saveResumeRoundTripsAndClearsWhenTheMatchEnds() = runBlocking {
        val saved = SavedGame(
            fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            movesEncoded = "e2e4",
            sideToMove = "DUSK",
            mode = GameMode.SINGLE_PLAYER,
            difficulty = Difficulty.EXPERT,
            playerName = "Tester",
            opponentName = "Expert AI",
            dawnTimeMs = 12_000L,
            duskTimeMs = 9_000L,
            startedAt = 500L,
            savedAt = 900L
        )
        games.saveInProgress(saved)

        val loaded = games.loadInProgress()
        assertNotNull(loaded)
        assertEquals(saved.fen, loaded!!.fen)
        assertEquals("e2e4", loaded.movesEncoded)
        assertEquals(GameMode.SINGLE_PLAYER, loaded.mode)
        assertEquals(Difficulty.EXPERT, loaded.difficulty)
        assertEquals(12_000L, loaded.dawnTimeMs)
        assertNotNull(games.observeSavedGame().first())

        games.recordFinishedGame(game(GameResult.VICTORY))
        assertNull("finishing a match clears the resume slot", games.loadInProgress())
        assertNull(games.observeSavedGame().first())
    }

    @Test
    fun resetClearsHistoryAndProfile() = runBlocking {
        games.recordFinishedGame(game(GameResult.VICTORY, moves = listOf(move(1, Faction.DAWN, PieceType.PAWN, "e2", "e4"))))
        games.saveInProgress(
            SavedGame(
                fen = "8/8/8/8/8/8/8/8 w - - 0 1", movesEncoded = "", sideToMove = "DAWN",
                mode = GameMode.TWO_PLAYER, difficulty = null, playerName = "Tester",
                opponentName = "Second Player", dawnTimeMs = 0, duskTimeMs = 0,
                startedAt = 0, savedAt = 0
            )
        )

        games.reset()
        assertEquals(0, gameDao.count())
        assertEquals(0, moveDao.countFor(1L))
        assertNull(games.loadInProgress())

        statistics.reset()
        val profile = statistics.profile()
        assertEquals(0, profile.gamesPlayed)
        assertEquals(0, profile.totalCaptures)
    }

    @Test
    fun twoPlayerGamesDoNotTakeTheAiDifficultyWins() = runBlocking {
        // A two player victory is a Dawn victory but should not count as an AI-mode win.
        games.recordFinishedGame(game(GameResult.VICTORY, difficulty = Difficulty.MASTER, mode = GameMode.TWO_PLAYER))
        // masterWins counts "defeat master AI", and in two player mode there is no AI: keep it false.
        assertFalse(achievements.all().first { it.achievementId == "master_of_war" }.unlocked)
    }
}
