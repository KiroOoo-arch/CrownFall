package com.crownfall.realm.ui.game

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.GameStatus
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.models.GameMode
import com.crownfall.realm.ui.board.BoardCinematic
import com.crownfall.realm.ui.board.BoardFrame
import com.crownfall.realm.ui.board.BoardHighlights
import com.crownfall.realm.ui.board.BoardSurface
import com.crownfall.realm.ui.board.BoardCamera
import com.crownfall.realm.ui.components.BannerTitle
import com.crownfall.realm.ui.components.MedievalBackground
import com.crownfall.realm.ui.components.MedievalButton
import com.crownfall.realm.ui.components.StatChip
import com.crownfall.realm.ui.components.StonePanel
import com.crownfall.realm.ui.theme.CrownfallPalette
import java.util.Locale

/**
 * The battlefield screen: board in the middle, both commanders on the flanks,
 * plus the promotion menu, pause menu and the end-of-battle report.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExitToMenu: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val camera = remember { BoardCamera() }
    var showPause by remember { mutableStateOf(false) }

    // Re-read settings whenever we come back to the board.
    LaunchedEffect(showPause) {
        if (!showPause) viewModel.refreshSettings()
    }

    val checkTransition = rememberInfiniteTransition(label = "check")
    val checkPulse by checkTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "checkPulse"
    )

    val cinematic = ui.animation?.let {
        BoardCinematic(it.move, it.cinematic, it.stateBefore)
    }
    val capturedByDawn = remember(ui.state) {
        ui.state.moveLog.toList().filter { it.piece.faction == Faction.DAWN && it.captured != null }
            .mapNotNull { it.captured?.type }
    }
    val capturedByDusk = remember(ui.state) {
        ui.state.moveLog.toList().filter { it.piece.faction == Faction.DUSK && it.captured != null }
            .mapNotNull { it.captured?.type }
    }

    MedievalBackground {
        BoxWithConstraints(Modifier.fillMaxSize()) {
        // Narrower rails on phones hand the extra width to the battlefield.
        val railWidth = when {
            maxWidth < 520.dp -> 104.dp
            maxWidth < 760.dp -> 138.dp
            else -> 168.dp
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ---------------------------------------------------- Dusk flank
            CommanderPanel(
                modifier = Modifier.width(railWidth).fillMaxHeight(),
                title = ui.opponentName,
                subtitle = if (ui.mode == GameMode.SINGLE_PLAYER) "Difficulty: ${ui.difficulty.displayName}"
                else "Empire of Dusk",
                timerMs = ui.duskTimeMs,
                active = ui.state.sideToMove == Faction.DUSK && !ui.state.isFinished,
                accent = CrownfallPalette.DuskAccent,
                captured = capturedByDawn,
                capturedLabel = "Pieces taken",
                footer = if (ui.mode == GameMode.SINGLE_PLAYER && ui.aiThinking) "Planning..." else null
            )

            // --------------------------------------------------- Battlefield
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                TopRail(ui = ui, onPause = { showPause = true }, onExit = onExitToMenu)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Square board hugging the largest square that fits, so the
                        // frame no longer sits around empty space.
                        val boardSide = minOf(maxWidth, maxHeight)
                        BoardFrame(
                            modifier = Modifier.size(boardSide),
                            dawnGlow = if (ui.state.sideToMove == Faction.DAWN) 1f else 0.55f,
                            duskGlow = if (ui.state.sideToMove == Faction.DUSK) 1f else 0.55f
                        ) {
                            BoardSurface(
                                state = ui.state,
                                cinematic = cinematic,
                                highlights = BoardHighlights(
                                    selected = ui.selected,
                                    legalMoves = ui.legalMoves,
                                    captureTargets = ui.captureTargets,
                                    lastMoveFrom = if (ui.showLastMove) ui.lastMoveFrom else null,
                                    lastMoveTo = if (ui.showLastMove) ui.lastMoveTo else null,
                                    checkSquare = ui.checkSquare,
                                    focusSquare = if (cinematic != null) cinematic.move.to else null
                                ),
                                camera = camera,
                                checkPulse = if (ui.checkSquare != null) checkPulse else 0f,
                                showCoordinates = ui.showCoordinates,
                                onSquareTap = viewModel::onSquareTap,
                                onCinematicSound = viewModel::playSound,
                                onCinematicFinished = viewModel::onAnimationComplete,
                                autoRotateForDusk = ui.boardRotation && ui.mode == GameMode.TWO_PLAYER,
                                screenShakeEnabled = ui.screenShake,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                // Fixed-height slot so the AI readout never resizes the board - the
                // old toggling line made the whole view look like it zoomed out and in.
                Box(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (ui.aiInfo.isNotEmpty() && ui.mode == GameMode.SINGLE_PLAYER) {
                        Text(
                            text = ui.aiInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrownfallPalette.ParchmentDim
                        )
                    }
                }
            }

            // --------------------------------------------------- Dawn flank
            CommanderPanel(
                modifier = Modifier.width(railWidth).fillMaxHeight(),
                title = ui.playerName,
                subtitle = if (ui.mode == GameMode.TWO_PLAYER) "Kingdom of Dawn"
                else "Move ${ui.state.fullmoveNumber} · ${ui.state.moveLog.size} plies",
                timerMs = ui.dawnTimeMs,
                active = ui.state.sideToMove == Faction.DAWN && !ui.state.isFinished,
                accent = CrownfallPalette.DawnAccent,
                captured = capturedByDusk,
                capturedLabel = "Pieces taken",
                footer = statusLine(ui)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MedievalButton(
                        text = "Undo Move",
                        onClick = viewModel::undo,
                        enabled = ui.state.moveLog.size > 0 && ui.animation == null && !ui.aiThinking,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                    if (ui.animation != null) {
                        MedievalButton(
                            text = "Skip Attack",
                            onClick = viewModel::skipAnimation,
                            accent = CrownfallPalette.RoyalBright,
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        )
                    }
                    MedievalButton(
                        text = "Offer Truce",
                        onClick = viewModel::offerDraw,
                        enabled = !ui.state.isFinished,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                    MedievalButton(
                        text = "Resign",
                        onClick = viewModel::requestResign,
                        enabled = !ui.state.isFinished,
                        accent = CrownfallPalette.RoyalBright,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                    MedievalButton(
                        text = "Settings",
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        compact = true
                    )
                }
            }
        }
        }

        // ------------------------------------------------------------ overlays
        ui.banner?.let { banner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                BannerChip(banner)
            }
        }

        ui.promotion?.let { prompt ->
            PromotionMenu(
                prompt = prompt,
                onChoose = viewModel::choosePromotion,
                onCancel = viewModel::cancelPromotion
            )
        }

        if (ui.showResignConfirm) {
            ConfirmOverlay(
                headline = "Resign the field?",
                body = "Your banner will be struck and the match recorded as a defeat.",
                confirmText = "Resign",
                onConfirm = viewModel::confirmResign,
                onDismiss = viewModel::cancelResign
            )
        }

        ui.gameOver?.let { info ->
            GameOverOverlay(
                info = info,
                mode = ui.mode,
                moveCount = ui.state.moveLog.size,
                onRematch = {
                    viewModel.dismissGameOver()
                    viewModel.restart()
                },
                onMenu = onExitToMenu
            )
        }

        if (showPause) {
            PauseOverlay(
                ui = ui,
                onResume = { showPause = false },
                onUndo = { showPause = false; viewModel.undo() },
                onRestart = { showPause = false; viewModel.restart() },
                onResign = { showPause = false; viewModel.requestResign() },
                onDraw = { showPause = false; viewModel.offerDraw() },
                onSettings = onOpenSettings,
                onMenu = onExitToMenu
            )
        }
    }
}

private fun statusLine(ui: GameUiState): String = when {
    ui.state.isFinished -> ui.gameOver?.headline ?: "Battle complete"
    ui.aiThinking -> "Empire is planning..."
    ui.animation != null -> "Assault in progress"
    ui.state.status == GameStatus.CHECK -> "Check! Defend the king"
    ui.state.sideToMove == Faction.DAWN -> "Your move"
    else -> "Empire to move"
}

@Composable
private fun TopRail(ui: GameUiState, onPause: () -> Unit, onExit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("Turn", if (ui.state.sideToMove == Faction.DAWN) "Dawn" else "Dusk")
            StatChip("Ply", ui.state.moveLog.size.toString())
            if (ui.state.isCheck) StatChip("WARNING", "CHECK", CrownfallPalette.HighlightCheck)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MedievalButton(text = "Pause", onClick = onPause, modifier = Modifier.widthIn(min = 96.dp))
            MedievalButton(text = "Menu", onClick = onExit, modifier = Modifier.widthIn(min = 96.dp))
        }
    }
}

@Composable
private fun CommanderPanel(
    modifier: Modifier,
    title: String,
    subtitle: String,
    timerMs: Long,
    active: Boolean,
    accent: Color,
    captured: List<PieceType>,
    capturedLabel: String,
    footer: String? = null,
    extra: (@Composable () -> Unit)? = null
) {
    StonePanel(
        modifier = modifier,
        borderColor = if (active) accent else CrownfallPalette.IronDark,
        borderWidth = if (active) 3.dp else 2.dp,
        contentPadding = PaddingValues(12.dp),
        scrollable = true
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (active) accent else CrownfallPalette.Parchment,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = CrownfallPalette.ParchmentDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = formatClock(timerMs),
            style = MaterialTheme.typography.headlineMedium,
            color = if (active) CrownfallPalette.GoldBright else CrownfallPalette.ParchmentDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CrownfallPalette.Void.copy(alpha = 0.55f))
                .padding(vertical = 6.dp)
        )
        if (active) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "TO MOVE",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(capturedLabel, style = MaterialTheme.typography.labelSmall, color = CrownfallPalette.ParchmentDim)
        if (captured.isEmpty()) {
            Text("—", style = MaterialTheme.typography.bodySmall, color = CrownfallPalette.ParchmentDim)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                items(captured) { piece ->
                    Text(
                        text = pieceGlyph(piece),
                        style = MaterialTheme.typography.titleMedium,
                        color = CrownfallPalette.Parchment
                    )
                }
            }
        }
        footer?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = CrownfallPalette.ParchmentDim)
        }
        extra?.let {
            Spacer(Modifier.height(12.dp))
            it.invoke()
        }
    }
}

@Composable
private fun BannerChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CrownfallPalette.RoyalDark.copy(alpha = 0.92f))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = CrownfallPalette.GoldBright
        )
    }
}

@Composable
private fun PromotionMenu(
    prompt: PromotionPrompt,
    onChoose: (PieceType) -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrownfallPalette.Void.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        StonePanel(modifier = Modifier.width(460.dp)) {
            BannerTitle("Promotion", "The soldier has reached the far rank")
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Choose the new role for your soldier ${prompt.from.algebraic} → ${prompt.to.algebraic}",
                style = MaterialTheme.typography.bodyMedium,
                color = CrownfallPalette.Parchment,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            // A 2x2 grid keeps the medieval names on a single line; a single row
            // of four was too narrow and wrapped words like "Warri or".
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                prompt.choices.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { type ->
                            MedievalButton(
                                text = type.medievalName,
                                supportingText = valueHint(type),
                                onClick = { onChoose(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            MedievalButton(
                text = "Cancel",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                accent = CrownfallPalette.Iron
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    info: GameOverInfo,
    mode: GameMode,
    moveCount: Int,
    onRematch: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrownfallPalette.Void.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        StonePanel(modifier = Modifier.width(520.dp)) {
            BannerTitle(info.headline, info.detail)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip("Recorded", if (mode == GameMode.SINGLE_PLAYER) "vs AI" else "Local duel")
                StatChip("Ply", moveCount.toString())
                StatChip("Result", info.winner?.shortName ?: "Draw")
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MedievalButton("New Battle", onRematch, modifier = Modifier.weight(1f))
                MedievalButton("Main Menu", onMenu, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ConfirmOverlay(
    headline: String,
    body: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrownfallPalette.Void.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        StonePanel(modifier = Modifier.width(440.dp)) {
            BannerTitle(headline)
            Spacer(Modifier.height(12.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = CrownfallPalette.Parchment,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MedievalButton(confirmText, onConfirm, modifier = Modifier.weight(1f), accent = CrownfallPalette.RoyalBright)
                MedievalButton("Keep fighting", onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    ui: GameUiState,
    onResume: () -> Unit,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onResign: () -> Unit,
    onDraw: () -> Unit,
    onSettings: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrownfallPalette.Void.copy(alpha = 0.80f)),
        contentAlignment = Alignment.Center
    ) {
        StonePanel(modifier = Modifier.width(420.dp)) {
            BannerTitle("Battle Paused", ui.state.activeFactionName)
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MedievalButton("Resume", onResume, modifier = Modifier.fillMaxWidth())
                MedievalButton(
                    "Undo Move",
                    onUndo,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ui.state.moveLog.size > 0 && ui.animation == null
                )
                MedievalButton("Restart Match", onRestart, modifier = Modifier.fillMaxWidth())
                MedievalButton("Offer Truce", onDraw, modifier = Modifier.fillMaxWidth(), enabled = !ui.state.isFinished)
                MedievalButton(
                    "Resign",
                    onResign,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !ui.state.isFinished,
                    accent = CrownfallPalette.RoyalBright
                )
                MedievalButton("Settings", onSettings, modifier = Modifier.fillMaxWidth())
                MedievalButton("Abandon to Menu", onMenu, modifier = Modifier.fillMaxWidth(), accent = CrownfallPalette.Iron)
            }
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun pieceGlyph(type: PieceType): String = when (type) {
    PieceType.KING -> "♔"
    PieceType.QUEEN -> "♕"
    PieceType.ROOK -> "♖"
    PieceType.BISHOP -> "♗"
    PieceType.KNIGHT -> "♘"
    PieceType.PAWN -> "♙"
}

private fun valueHint(type: PieceType): String =
    "Worth ${type.baseValue} ${if (type.baseValue == 1) "point" else "points"}"
