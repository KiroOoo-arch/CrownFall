package com.crownfall.realm.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownfall.realm.audio.AudioEngine
import com.crownfall.realm.data.repository.AnimationSpeed
import com.crownfall.realm.data.repository.GameRepository
import com.crownfall.realm.data.repository.SettingsRepository
import com.crownfall.realm.domain.ai.AIEngine
import com.crownfall.realm.domain.ai.Difficulty
import com.crownfall.realm.domain.animation.CaptureAnimationController
import com.crownfall.realm.domain.animation.PieceAnimationController
import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.ChessRules
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.Fen
import com.crownfall.realm.domain.chess.GameEngine
import com.crownfall.realm.domain.chess.GameState
import com.crownfall.realm.domain.chess.GameStatus
import com.crownfall.realm.domain.chess.Move
import com.crownfall.realm.domain.chess.MoveCodec
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.chess.Square
import com.crownfall.realm.domain.models.FinishedGame
import com.crownfall.realm.domain.models.GameMode
import com.crownfall.realm.domain.models.GameResult
import com.crownfall.realm.domain.models.PlayedMove
import com.crownfall.realm.domain.models.SavedGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

/** A capture cinematic currently playing on the board. */
data class AnimationInFlight(
    val move: Move,
    val cinematic: CaptureAnimationController.CaptureCinematic,
    val stateBefore: GameState,
    val combatEnabled: Boolean
)

/** The promotion menu awaiting the player's choice. */
data class PromotionPrompt(
    val from: Square,
    val to: Square,
    val choices: List<PieceType> = ChessRules.PROMOTION_CHOICES
)

data class GameOverInfo(
    val status: GameStatus,
    val winner: Faction?,
    val headline: String,
    val detail: String
)

data class GameUiState(
    val state: GameState = GameState.initial(),
    val mode: GameMode = GameMode.SINGLE_PLAYER,
    val difficulty: Difficulty = Difficulty.HARD,
    val playerName: String = "You",
    val opponentName: String = "AI Commander",
    val selected: Square? = null,
    val legalMoves: List<Square> = emptyList(),
    val captureTargets: List<Square> = emptyList(),
    val promotion: PromotionPrompt? = null,
    val animation: AnimationInFlight? = null,
    val aiThinking: Boolean = false,
    val aiInfo: String = "",
    val dawnTimeMs: Long = 0L,
    val duskTimeMs: Long = 0L,
    val startedAt: Long = System.currentTimeMillis(),
    val gameOver: GameOverInfo? = null,
    val banner: String? = null,
    val showResignConfirm: Boolean = false,
    val combatAnimation: Boolean = true,
    val screenShake: Boolean = true,
    val animationSpeed: Float = 1f,
    val showHints: Boolean = true,
    val showLastMove: Boolean = true,
    val showCoordinates: Boolean = true,
    val boardRotation: Boolean = false,
    val loaded: Boolean = false
) {
    val humanCanMove: Boolean
        get() = !state.isFinished && animation == null && promotion == null &&
            (mode == GameMode.TWO_PLAYER || state.sideToMove == Faction.DAWN)

    val lastMoveFrom: Square? get() = state.lastMove?.from
    val lastMoveTo: Square? get() = state.lastMove?.to

    val checkSquare: Square?
        get() = if (state.isCheck || state.status == GameStatus.CHECKMATE) {
            state.board.kingSquare(state.sideToMove)
        } else {
            null
        }
}

/**
 * Owns one match: selection, AI turns, cinematics, timers, persistence and the
 * end-of-game pipeline. All heavy work (AI search, database writes) happens off
 * the main thread, and nothing is written per animation frame.
 */
class GameViewModel(
    private val chess: GameEngine,
    private val ai: AIEngine,
    private val animations: PieceAnimationController,
    private val games: GameRepository,
    private val settings: SettingsRepository,
    private val audio: AudioEngine
) : ViewModel() {

    private val _ui = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _ui.asStateFlow()

    private var turnStartedAt = System.currentTimeMillis()
    private val moveTimes = ArrayList<Long>()
    private var turnCounter = 0

    init {
        refreshSettings()
    }

    fun refreshSettings() {
        viewModelScope.launch {
            val current = withContext(Dispatchers.IO) { settings.current() }
            _ui.update {
                it.copy(
                    combatAnimation = current.combatAnimation,
                    screenShake = current.screenShake,
                    animationSpeed = AnimationSpeed.fromName(current.animationSpeed).multiplier,
                    showHints = current.showLegalMoveHints,
                    showLastMove = current.showLastMove,
                    showCoordinates = current.showCoordinates,
                    boardRotation = current.boardRotation
                )
            }
        }
    }

    // ------------------------------------------------------------ lifecycle

    fun startNewGame(
        mode: GameMode,
        difficulty: Difficulty,
        playerName: String = "Realm Champion",
        opponentName: String? = null
    ) {
        val opponent = opponentName ?: when (mode) {
            GameMode.SINGLE_PLAYER -> "${difficulty.displayName} AI"
            GameMode.TWO_PLAYER -> "Second Player"
            GameMode.CONTINUED -> "AI Commander"
        }
        turnStartedAt = System.currentTimeMillis()
        moveTimes.clear()
        turnCounter = 0
        _ui.update {
            it.copy(
                state = chess.newGame(),
                mode = mode,
                difficulty = difficulty,
                playerName = if (mode == GameMode.TWO_PLAYER) "Kingdom of Dawn" else playerName,
                opponentName = if (mode == GameMode.TWO_PLAYER) "Empire of Dusk" else opponent,
                selected = null,
                legalMoves = emptyList(),
                captureTargets = emptyList(),
                promotion = null,
                animation = null,
                aiThinking = false,
                aiInfo = "",
                dawnTimeMs = 0L,
                duskTimeMs = 0L,
                startedAt = System.currentTimeMillis(),
                gameOver = null,
                banner = null,
                showResignConfirm = false,
                loaded = true
            )
        }
        if (mode == GameMode.SINGLE_PLAYER) maybeRunAi()
    }

    /** Restores the single resumable match, if one exists. */
    fun resumeSavedGame() {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { games.loadInProgress() } ?: return@launch
            val state = MoveCodec.replay(saved.movesEncoded, chess)
            moveTimes.clear()
            turnStartedAt = System.currentTimeMillis()
            _ui.update {
                it.copy(
                    state = state,
                    mode = saved.mode,
                    difficulty = saved.difficulty ?: Difficulty.HARD,
                    playerName = saved.playerName,
                    opponentName = saved.opponentName,
                    dawnTimeMs = saved.dawnTimeMs,
                    duskTimeMs = saved.duskTimeMs,
                    startedAt = saved.startedAt,
                    selected = null,
                    legalMoves = emptyList(),
                    captureTargets = emptyList(),
                    gameOver = null,
                    loaded = true
                )
            }
            showBanner("Match resumed")
            if (!state.isFinished) {
                _ui.update { it.copy(state = chess.resolve(state)) }
                maybeRunAi()
            }
        }
    }

    // -------------------------------------------------------------- input

    fun onSquareTap(square: Square) {
        val s = _ui.value
        if (s.animation != null || s.promotion != null || s.gameOver != null) return
        if (!s.humanCanMove) return
        if (s.state.isFinished) return

        val piece = s.state.pieceAt(square)
        val selected = s.selected

        if (selected == null) {
            if (piece != null && piece.faction == s.state.sideToMove) select(square)
            return
        }
        if (selected == square) {
            clearSelection()
            return
        }
        if (s.legalMoves.contains(square)) {
            val move = chess.findMove(s.state, selected, square)
            if (move == null) {
                clearSelection()
                return
            }
            if (move.promotion != null) {
                _ui.update { it.copy(promotion = PromotionPrompt(move.from, move.to)) }
            } else {
                startMove(move)
            }
            return
        }
        if (piece != null && piece.faction == s.state.sideToMove) {
            select(square)
        } else {
            clearSelection()
        }
    }

    private fun select(square: Square) {
        val s = _ui.value
        val moves = ChessRules.legalMoves(s.state, square)
        audio.play(SoundEffect.PIECE_MOVE)
        _ui.update {
            it.copy(
                selected = square,
                legalMoves = if (s.showHints) moves.map { move -> move.to } else emptyList(),
                captureTargets = if (s.showHints) moves.filter { m -> m.isCapture }.map { m -> m.to } else emptyList()
            )
        }
    }

    private fun clearSelection() {
        _ui.update { it.copy(selected = null, legalMoves = emptyList(), captureTargets = emptyList()) }
    }

    fun choosePromotion(type: PieceType) {
        val s = _ui.value
        val prompt = s.promotion ?: return
        val move = chess.findMove(s.state, prompt.from, prompt.to, type) ?: run {
            _ui.update { it.copy(promotion = null) }
            return
        }
        _ui.update { it.copy(promotion = null) }
        startMove(move)
    }

    fun cancelPromotion() {
        _ui.update { it.copy(promotion = null) }
        clearSelection()
    }

    // ---------------------------------------------------------- animation

    private fun startMove(move: Move) {
        val s = _ui.value
        val cinematic = animations.captures.planCapture(move, s.animationSpeed, s.combatAnimation)
        audio.play(if (move.isCapture && s.combatAnimation) SoundEffect.SWORD_SWING else SoundEffect.PIECE_MOVE)
        _ui.update {
            it.copy(
                animation = AnimationInFlight(move, cinematic, s.state, s.combatAnimation),
                selected = null,
                legalMoves = emptyList(),
                captureTargets = emptyList(),
                promotion = null
            )
        }
    }

    fun playSound(effect: SoundEffect) {
        audio.play(effect)
    }

    /** Called by the board when the cinematic has finished (or was skipped). */
    fun onAnimationComplete() {
        val s = _ui.value
        val inFlight = s.animation ?: return

        val now = System.currentTimeMillis()
        val spent = (now - turnStartedAt).coerceAtLeast(0L)
        turnStartedAt = now
        val next = chess.play(inFlight.stateBefore, inFlight.move)
        moveTimes.add(now)

        val dawnTime = if (inFlight.stateBefore.sideToMove == Faction.DAWN) s.dawnTimeMs + spent else s.dawnTimeMs
        val duskTime = if (inFlight.stateBefore.sideToMove == Faction.DUSK) s.duskTimeMs + spent else s.duskTimeMs

        _ui.update {
            it.copy(
                state = next,
                animation = null,
                dawnTimeMs = dawnTime,
                duskTimeMs = duskTime,
                aiInfo = ""
            )
        }
        turnCounter++

        if (next.status == GameStatus.CHECK) {
            audio.play(SoundEffect.CHECK_WARNING)
            showBanner("Check!")
        }
        if (next.isFinished) {
            finishMatch(next)
        } else {
            autosave(next)
            maybeRunAi()
        }
    }

    /** Skipping fast-forwards to the end of the cinematic. */
    fun skipAnimation() {
        if (_ui.value.animation != null) onAnimationComplete()
    }

    // ---------------------------------------------------------------- AI

    private fun maybeRunAi() {
        val s = _ui.value
        if (s.mode != GameMode.SINGLE_PLAYER) return
        if (s.state.isFinished || s.animation != null || s.promotion != null) return
        if (s.state.sideToMove != Faction.DUSK) return

        val key = s.state.positionKey
        _ui.update { it.copy(aiThinking = true) }
        viewModelScope.launch {
            val decision = withContext(Dispatchers.Default) {
                ai.chooseMove(s.state, s.difficulty, Random(key))
            }
            // The player may have undone or restarted while the AI was thinking.
            if (_ui.value.state.positionKey != key) {
                _ui.update { it.copy(aiThinking = false) }
                return@launch
            }
            _ui.update {
                it.copy(
                    aiThinking = false,
                    aiInfo = decision?.let { d ->
                        "${s.difficulty.displayName}: depth ${d.depth}, ${d.nodes} nodes, ${d.elapsedMs}ms"
                    } ?: ""
                )
            }
            val move = decision?.move ?: return@launch
            startMove(move)
        }
    }

    // -------------------------------------------------------- match control

    fun requestResign() = _ui.update { it.copy(showResignConfirm = true) }

    fun cancelResign() = _ui.update { it.copy(showResignConfirm = false) }

    fun confirmResign() {
        val s = _ui.value
        if (s.state.isFinished) return
        val faction = if (s.mode == GameMode.SINGLE_PLAYER) Faction.DAWN else s.state.sideToMove
        _ui.update { it.copy(showResignConfirm = false, state = chess.resign(s.state, faction), aiThinking = false) }
        finishMatch(_ui.value.state)
    }

    fun offerDraw() {
        val s = _ui.value
        if (s.state.isFinished) return
        _ui.update { it.copy(state = chess.agreeDraw(s.state), aiThinking = false) }
        finishMatch(_ui.value.state)
    }

    fun undo() {
        val s = _ui.value
        if (s.animation != null || s.aiThinking) return
        if (s.state.moveLog.size == 0) return

        var target: GameState? = chess.undo(s.state)
        if (s.mode == GameMode.SINGLE_PLAYER) {
            while (target != null && target.sideToMove != Faction.DAWN && target.moveLog.size > 0) {
                target = chess.undo(target)
            }
        }
        val restored = target ?: return
        if (moveTimes.isNotEmpty()) moveTimes.removeAt(moveTimes.size - 1)
        _ui.update {
            it.copy(
                state = restored,
                selected = null,
                legalMoves = emptyList(),
                captureTargets = emptyList(),
                gameOver = null,
                aiThinking = false
            )
        }
        showBanner("Move taken back")
        audio.play(SoundEffect.BUTTON_CLICK)
        autosave(restored)
    }

    fun restart() {
        val s = _ui.value
        startNewGame(s.mode, s.difficulty, s.playerName, s.opponentName)
        viewModelScope.launch { withContext(Dispatchers.IO) { games.clearInProgress() } }
    }

    fun dismissGameOver() = _ui.update { it.copy(gameOver = null) }

    fun clearBanner() {
        if (_ui.value.banner != null) _ui.update { it.copy(banner = null) }
    }

    private fun showBanner(text: String) {
        _ui.update { it.copy(banner = text) }
        viewModelScope.launch {
            delay(2200)
            if (_ui.value.banner == text) _ui.update { it.copy(banner = null) }
        }
    }

    // ---------------------------------------------------------- persistence

    private fun autosave(state: GameState) {
        if (state.isFinished || state.moveLog.size == 0) return
        val s = _ui.value
        val save = SavedGame(
            fen = Fen.toFen(state),
            movesEncoded = MoveCodec.encodeAll(state.moveLog.toList()),
            sideToMove = state.sideToMove.name,
            mode = s.mode,
            difficulty = if (s.mode == GameMode.SINGLE_PLAYER) s.difficulty else null,
            playerName = s.playerName,
            opponentName = s.opponentName,
            dawnTimeMs = s.dawnTimeMs,
            duskTimeMs = s.duskTimeMs,
            startedAt = s.startedAt,
            savedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) { games.saveInProgress(save) }
        }
    }

    private fun finishMatch(state: GameState) {
        val s = _ui.value
        val info = buildGameOver(state, s)
        _ui.update { it.copy(gameOver = info, aiThinking = false, selected = null, legalMoves = emptyList(), captureTargets = emptyList()) }

        when {
            state.status == GameStatus.CHECKMATE -> audio.play(SoundEffect.CHECKMATE)
            state.status.isDraw -> audio.play(SoundEffect.DRAW)
            state.winner == Faction.DAWN && s.mode == GameMode.SINGLE_PLAYER -> audio.play(SoundEffect.VICTORY_MUSIC)
            state.winner == Faction.DUSK && s.mode == GameMode.SINGLE_PLAYER -> audio.play(SoundEffect.DEFEAT_MUSIC)
            state.winner != null -> audio.play(SoundEffect.VICTORY_MUSIC)
            else -> audio.play(SoundEffect.DRAW)
        }

        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { games.recordFinishedGame(buildFinishedGame(state, s)) }
            val unlocked = outcome.newlyUnlocked
            if (unlocked.isNotEmpty()) {
                showBanner(
                    if (unlocked.size == 1) "Achievement: ${unlocked.first().name}"
                    else "${unlocked.size} achievements unlocked"
                )
            }
        }
    }

    private fun buildGameOver(state: GameState, s: GameUiState): GameOverInfo {
        val headline = when {
            state.status == GameStatus.CHECKMATE -> "Checkmate"
            state.status == GameStatus.STALEMATE -> "Stalemate"
            state.status == GameStatus.RESIGNATION -> "Resignation"
            state.status == GameStatus.DRAW_AGREEMENT -> "Truce"
            state.status == GameStatus.DRAW_FIFTY_MOVE -> "Fifty-move draw"
            state.status == GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Insufficient force"
            state.status == GameStatus.DRAW_REPETITION -> "Threefold repetition"
            state.status == GameStatus.ABANDONED -> "Abandoned"
            else -> "Battle ended"
        }
        val detail = when {
            state.winner == Faction.DAWN -> "${s.playerName.ifBlank { "Kingdom of Dawn" }} takes the crown"
            state.winner == Faction.DUSK -> "${s.opponentName.ifBlank { "Empire of Dusk" }} takes the crown"
            else -> "The field is left to the crows"
        }
        return GameOverInfo(state.status, state.winner, headline, detail)
    }

    private fun buildFinishedGame(state: GameState, s: GameUiState): FinishedGame {
        val moves = historyOf(state)
        val dawnLostQueen = moves.any { it.side == Faction.DUSK && it.captured == PieceType.QUEEN }
        val result = when {
            state.winner == Faction.DAWN -> GameResult.VICTORY
            state.winner == Faction.DUSK -> GameResult.DEFEAT
            state.status.isDraw -> GameResult.DRAW
            else -> GameResult.IN_PROGRESS
        }
        return FinishedGame(
            mode = s.mode,
            difficulty = if (s.mode == GameMode.SINGLE_PLAYER) s.difficulty else null,
            playerName = if (s.mode == GameMode.TWO_PLAYER) "Kingdom of Dawn" else s.playerName,
            opponentName = s.opponentName,
            startTime = s.startedAt,
            endTime = System.currentTimeMillis(),
            winnerName = state.winner?.factionName,
            result = result,
            moves = moves,
            dawnLostQueen = dawnLostQueen,
            checkmateDelivered = state.status == GameStatus.CHECKMATE && state.winner == Faction.DAWN
        )
    }

    /** Converts the engine's move log into the persisted, displayable form. */
    fun historyOf(state: GameState = _ui.value.state): List<PlayedMove> {
        val log = state.moveLog.toList()
        val now = System.currentTimeMillis()
        return log.mapIndexed { index, move ->
            PlayedMove(
                moveNumber = index + 1,
                side = move.piece.faction,
                piece = move.piece.type,
                from = move.from.algebraic,
                to = move.to.algebraic,
                captured = move.captured?.type,
                promotion = move.promotion,
                notation = move.notation(),
                timestamp = moveTimes.getOrElse(index) { now }
            )
        }
    }

    override fun onCleared() {
        // Best-effort save so the player can continue after leaving.
        val s = _ui.value
        if (!s.state.isFinished && s.state.moveLog.size > 0) {
            autosave(s.state)
        }
        super.onCleared()
    }
}
