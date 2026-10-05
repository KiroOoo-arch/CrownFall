package com.crownfall.realm.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.crownfall.realm.domain.animation.CaptureAnimationController
import com.crownfall.realm.domain.animation.CinematicSampler
import com.crownfall.realm.domain.animation.IdleTransform
import com.crownfall.realm.domain.animation.ParticleStyle
import com.crownfall.realm.domain.animation.PieceAnimationController
import com.crownfall.realm.domain.animation.PiecePose
import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.GameState
import com.crownfall.realm.domain.chess.Move
import com.crownfall.realm.domain.chess.Piece
import com.crownfall.realm.domain.chess.Square
import com.crownfall.realm.ui.theme.CrownfallPalette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Minimum gap between idle-animation samples (about 30fps). */
private const val IDLE_SAMPLE_MS = 33L

/** One piece as it should be drawn this frame. */
data class PieceRender(
    val piece: Piece,
    val unitsFile: Float,
    val unitsRank: Float,
    val pose: PiecePose = PiecePose.NEUTRAL,
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val flip: Boolean = false
)

/** A capture cinematic that the board is responsible for playing out. */
data class BoardCinematic(
    val move: Move,
    val cinematic: CaptureAnimationController.CaptureCinematic,
    val stateBefore: GameState
)

/**
 * The battlefield: stone board inside a carved wooden frame, every piece, the
 * camera and the particle effects.
 *
 * The board owns the cinematic loop. It samples the pure
 * [CinematicSampler] once per frame, drives the camera, fires the phase sounds
 * and only then reports back, so the engine state changes exactly when the
 * player has finished watching the duel.
 */
@Composable
fun BoardSurface(
    state: GameState,
    cinematic: BoardCinematic?,
    highlights: BoardHighlights,
    camera: BoardCamera,
    checkPulse: Float,
    showCoordinates: Boolean,
    onSquareTap: (Square) -> Unit,
    onCinematicSound: (SoundEffect) -> Unit,
    onCinematicFinished: () -> Unit,
    modifier: Modifier = Modifier,
    autoRotateForDusk: Boolean = false,
    screenShakeEnabled: Boolean = true,
    animationController: PieceAnimationController = remember { PieceAnimationController() }
) {
    // The idle loop is sampled at ~30fps rather than every frame: the motion is
    // subtle, and halving the redraw rate of 32 piece canvases leaves far more
    // headroom for the cinematics that actually need 60fps.
    val idleClock by produceState(0L) {
        var lastEmitted = 0L
        while (true) {
            withFrameNanos { nanos ->
                val millis = nanos / 1_000_000L
                if (millis - lastEmitted >= IDLE_SAMPLE_MS) {
                    lastEmitted = millis
                    value = millis
                }
            }
        }
    }
    var frame by remember { mutableStateOf<CinematicSampler.Frame?>(null) }
    val finishedCallback by rememberUpdatedState(onCinematicFinished)
    val soundCallback by rememberUpdatedState(onCinematicSound)

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val side = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val geometry = remember(side) { BoardGeometry(side) }
        val center = Offset(side / 2f, side / 2f)

        // Auto-rotate the battlefield so the active faction always sits at the bottom.
        LaunchedEffect(autoRotateForDusk, state.sideToMove, side) {
            val target = if (autoRotateForDusk && cinematic == null && state.sideToMove == Faction.DUSK) {
                180f
            } else {
                0f
            }
            camera.applyFramingRotation(target)
            if (target != 0f) camera.setPan(Offset.Zero, side)
        }

        // The cinematic driver: one sample per frame, camera included.
        //
        // Keyed only on [cinematic] so a layout change (which shifts [side])
        // can never restart the loop mid-duel and make the camera jump. The
        // current size and the shake preference are read through
        // [rememberUpdatedState] instead.
        val currentSide by rememberUpdatedState(side)
        val shakeAllowed by rememberUpdatedState(screenShakeEnabled)
        LaunchedEffect(cinematic) {
            val playing = cinematic
            if (playing == null) {
                frame = null
                return@LaunchedEffect
            }
            val sampler = CinematicSampler(playing.cinematic, playing.move)
            // Only a true capture cinematic is allowed to move the camera. An
            // ordinary move must leave the player's own zoom and pan untouched,
            // otherwise it snaps the view out and back on every single turn.
            val cinematicZoom = CinematicSampler.cameraZoomFor(playing.cinematic)
                .coerceAtMost(camera.maxZoom)
                .coerceAtLeast(1f)
            val drivesCamera = cinematicZoom > 1.001f
            // Remember the framing the player had before the duel so it can be
            // handed back exactly, instead of snapping to 1x.
            val restoreZoom = camera.zoom
            val restorePan = camera.pan
            camera.markFraming(drivesCamera)
            var startNanos = 0L
            var previous = -1L
            while (true) {
                val nanos = withFrameNanos { it }
                if (startNanos == 0L) startNanos = nanos
                val elapsed = (nanos - startNanos) / 1_000_000L
                val sample = sampler.sample(elapsed, previous)
                if (previous >= 0) sample.sounds.forEach { soundCallback(it) }
                frame = sample
                previous = elapsed

                if (drivesCamera) {
                    val frameSide = currentSide
                    val frameGeometry = BoardGeometry(frameSide)
                    val frameCenter = Offset(frameSide / 2f, frameSide / 2f)
                    // Camera: push in toward the duel, add shake on impact.
                    camera.setZoom(cinematicZoom, frameSide)
                    val duelFile = (playing.move.from.file + playing.move.to.file) / 2f
                    val duelRank = (playing.move.from.rank + playing.move.to.rank) / 2f
                    val duel = frameGeometry.unitCenter(duelFile, duelRank)
                    camera.setPan(
                        Offset(
                            -(duel.x - frameCenter.x) * cinematicZoom,
                            -(duel.y - frameCenter.y) * cinematicZoom
                        ),
                        frameSide
                    )
                    val shake = if (shakeAllowed) {
                        val (shakeX, shakeY) = animationController.shakeOffset(sample.cameraShake, elapsed)
                        Offset(shakeX * frameSide, shakeY * frameSide)
                    } else {
                        Offset.Zero
                    }
                    camera.setShake(shake)
                }

                if (elapsed >= sampler.totalDurationMs) break
            }
            camera.setShake(Offset.Zero)
            if (drivesCamera) {
                // Restore the player's framing rather than forcing 1x.
                camera.setZoom(restoreZoom, currentSide)
                camera.setPan(restorePan, currentSide)
            }
            camera.markFraming(false)
            frame = null
            finishedCallback()
        }

        val pieces = remember(state, cinematic, frame, autoRotateForDusk) {
            buildPieces(state, cinematic, frame, autoRotateForDusk)
        }
        val particles = frame?.particles ?: emptyList()

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(with(density) { side.toDp() })
                .clipToBounds()
                .boardGestures(camera, { side }, { offset ->
                    val boardPoint = camera.viewToBoard(offset, side)
                    geometry.squareAt(boardPoint)?.let(onSquareTap)
                })
        ) {
            Canvas(Modifier.fillMaxSize()) {
                withTransform({
                    translate(camera.pan.x, camera.pan.y)
                    scale(camera.zoom, camera.zoom, pivot = center)
                    rotate(camera.rotation, pivot = center)
                }) {
                    with(BoardRenderer) {
                        drawBoard(
                            geometry = geometry,
                            highlights = highlights,
                            checkPulse = checkPulse,
                            showCoordinates = showCoordinates
                        )
                    }
                }
            }

            pieces.forEach { render ->
                val pieceCenter = camera.boardToView(
                    geometry.unitCenter(render.unitsFile, render.unitsRank),
                    side
                )
                val piecePx = geometry.squareSize * camera.zoom * render.scale
                val idle = animationController.idleTransform(render.piece.type, idleClock)
                Box(
                    modifier = Modifier
                        .offsetPx(pieceCenter, piecePx)
                        .size(with(density) { piecePx.toDp() })
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val area = Size(size.width, size.height)
                        val effectiveIdle = if (cinematic == null) idle else IdleTransform.NONE
                        if (render.flip) {
                            withTransform({
                                scale(-1f, 1f, pivot = Offset(area.width / 2f, area.height / 2f))
                            }) {
                                with(PieceArt) {
                                    drawPiece(
                                        render.piece.type,
                                        render.piece.faction,
                                        area,
                                        effectiveIdle,
                                        render.pose,
                                        render.alpha
                                    )
                                }
                            }
                        } else {
                            with(PieceArt) {
                                drawPiece(
                                    render.piece.type,
                                    render.piece.faction,
                                    area,
                                    effectiveIdle,
                                    render.pose,
                                    render.alpha
                                )
                            }
                        }
                    }
                }
            }

            if (particles.isNotEmpty()) {
                Canvas(Modifier.fillMaxSize()) {
                    withTransform({
                        translate(camera.pan.x, camera.pan.y)
                        scale(camera.zoom, camera.zoom, pivot = center)
                        rotate(camera.rotation, pivot = center)
                    }) {
                        particles.forEach { sample -> drawParticle(geometry, sample) }
                    }
                }
            }
        }
    }
}

/**
 * Builds the draw list. While a cinematic plays the attacker follows the
 * sampler and the target is drawn with its defeat pose; the rest of the army
 * stays put.
 */
private fun buildPieces(
    state: GameState,
    cinematic: BoardCinematic?,
    frame: CinematicSampler.Frame?,
    autoRotateForDusk: Boolean
): List<PieceRender> {
    val flipFor = { faction: Faction -> faction == Faction.DUSK }

    if (cinematic == null || frame == null) {
        return state.board.pieces().map { (square, piece) ->
            PieceRender(
                piece = piece,
                unitsFile = square.file.toFloat(),
                unitsRank = square.rank.toFloat(),
                flip = flipFor(piece.faction)
            )
        }
    }

    val move = cinematic.move
    val board = cinematic.stateBefore.board
    val captured = move.captured
    val defenderSquare = if (move.isEnPassant) Square(move.to.file, move.from.rank) else move.to
    val out = ArrayList<PieceRender>(32)

    for ((square, piece) in board.pieces()) {
        if (square == move.from || square == defenderSquare) continue
        out.add(
            PieceRender(
                piece = piece,
                unitsFile = square.file.toFloat(),
                unitsRank = square.rank.toFloat(),
                flip = flipFor(piece.faction)
            )
        )
    }

    out.add(
        PieceRender(
            piece = move.piece,
            unitsFile = frame.attackerFile,
            unitsRank = frame.attackerRank,
            pose = frame.attackerPose,
            alpha = frame.attackerAlpha,
            flip = flipFor(move.piece.faction)
        )
    )

    if (captured != null && frame.defenderPresent) {
        // Keep the knock-back offset relative to the real captured square.
        val knockFile = frame.defenderFile - move.to.file.toFloat()
        val knockRank = frame.defenderRank - move.to.rank.toFloat()
        out.add(
            PieceRender(
                piece = captured,
                unitsFile = defenderSquare.file + knockFile,
                unitsRank = defenderSquare.rank + knockRank,
                pose = frame.defenderPose,
                alpha = frame.defenderAlpha,
                flip = flipFor(captured.faction)
            )
        )
    }

    return out
}

private fun Modifier.offsetPx(center: Offset, sizePx: Float): Modifier = this.then(
    Modifier.offset {
        IntOffset(
            (center.x - sizePx / 2f).roundToInt(),
            (center.y - sizePx / 2f).roundToInt()
        )
    }
)

/** Draws one particle burst. Deterministic so a phase always looks the same. */
private fun DrawScope.drawParticle(
    geometry: BoardGeometry,
    sample: com.crownfall.realm.domain.animation.ParticleSample
) {
    if (sample.style == ParticleStyle.NONE) return
    val center = geometry.unitCenter(sample.file, sample.rank)
    val square = geometry.squareSize
    val t = sample.progress.coerceIn(0f, 1f)
    val fade = (1f - t).coerceIn(0f, 1f) * sample.intensity

    val count = when (sample.style) {
        ParticleStyle.ROYAL_SPARKS -> 12
        ParticleStyle.HOLY_GLOW -> 8
        ParticleStyle.DUST_CLOUD -> 10
        ParticleStyle.IRON_SPARKS -> 14
        ParticleStyle.GOLD_BURST -> 18
        ParticleStyle.DARK_MIST -> 9
        ParticleStyle.NONE -> 0
    }
    val color = when (sample.style) {
        ParticleStyle.ROYAL_SPARKS -> CrownfallPalette.RoyalBright
        ParticleStyle.HOLY_GLOW -> CrownfallPalette.MagicGlow
        ParticleStyle.DUST_CLOUD -> CrownfallPalette.StoneLight
        ParticleStyle.IRON_SPARKS -> CrownfallPalette.GoldBright
        ParticleStyle.GOLD_BURST -> CrownfallPalette.Gold
        ParticleStyle.DARK_MIST -> CrownfallPalette.DuskAccent
        ParticleStyle.NONE -> Color.Transparent
    }

    if (sample.style == ParticleStyle.HOLY_GLOW || sample.style == ParticleStyle.DARK_MIST) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.35f * fade), Color.Transparent),
                center = center,
                radius = square * (0.5f + t * 0.9f)
            ),
            radius = square * (0.5f + t * 0.9f),
            center = center
        )
        return
    }

    for (i in 0 until count) {
        val angle = (i.toFloat() / count) * (2f * Math.PI.toFloat()) + i * 0.37f
        val speed = 0.25f + (i % 5) * 0.12f
        val distance = t * speed * square * 2f
        val position = Offset(
            center.x + cos(angle) * distance,
            center.y + sin(angle) * distance + t * t * square * 0.5f
        )
        val radius = square * (0.045f + (i % 3) * 0.012f) * (1f - t * 0.5f)
        drawCircle(color = color.copy(alpha = 0.85f * fade), radius = radius, center = position)
    }
}

/**
 * The wooden frame around the battlefield, with banners and iron studs.
 * Drawn outside the camera transform so it always frames the view.
 */
@Composable
fun BoardFrame(
    modifier: Modifier = Modifier,
    dawnGlow: Float = 1f,
    duskGlow: Float = 1f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .background(
                Brush.verticalGradient(
                    listOf(CrownfallPalette.WoodLight, CrownfallPalette.Wood, CrownfallPalette.WoodDark)
                )
            )
            .border(3.dp, CrownfallPalette.IronDark, RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val plank = size.height / 12f
            var y = plank
            while (y < size.height) {
                drawLine(
                    color = CrownfallPalette.WoodDark.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.6f
                )
                y += plank
            }
            val studSpacing = size.width / 16f
            var x = studSpacing
            while (x < size.width) {
                drawCircle(
                    color = CrownfallPalette.IronBright.copy(alpha = 0.35f),
                    radius = 2.4f,
                    center = Offset(x, size.height * 0.03f)
                )
                drawCircle(
                    color = CrownfallPalette.IronBright.copy(alpha = 0.35f),
                    radius = 2.4f,
                    center = Offset(x, size.height * 0.97f)
                )
                x += studSpacing
            }
            drawBanner(Offset(size.width * 0.04f, 0f), size.height * 0.28f, CrownfallPalette.DawnAccent, dawnGlow)
            drawBanner(Offset(size.width * 0.96f, 0f), size.height * 0.28f, CrownfallPalette.DuskAccent, duskGlow)
            drawBanner(Offset(size.width * 0.04f, size.height), size.height * 0.28f, CrownfallPalette.DawnAccent, dawnGlow)
            drawBanner(Offset(size.width * 0.96f, size.height), size.height * 0.28f, CrownfallPalette.DuskAccent, duskGlow)
        }
        Box(Modifier.fillMaxSize().padding(4.dp)) { content() }
    }
}

private fun DrawScope.drawBanner(origin: Offset, length: Float, color: Color, glow: Float) {
    val width = length * 0.34f
    val direction = if (origin.y <= size.height * 0.5f) 1f else -1f
    val path = Path().apply {
        moveTo(origin.x - width / 2f, origin.y)
        lineTo(origin.x + width / 2f, origin.y)
        lineTo(origin.x + width / 2f, origin.y + direction * length * 0.78f)
        lineTo(origin.x, origin.y + direction * length)
        lineTo(origin.x - width / 2f, origin.y + direction * length * 0.78f)
        close()
    }
    val alpha = glow.coerceIn(0.4f, 1f)
    drawPath(path, color = color.copy(alpha = 0.75f * alpha))
    drawPath(path, color = CrownfallPalette.Gold.copy(alpha = 0.5f * alpha), style = Stroke(width = 2f))
    drawCircle(
        color = CrownfallPalette.Gold.copy(alpha = 0.8f),
        radius = width * 0.20f,
        center = Offset(origin.x, origin.y + direction * length * 0.42f)
    )
}
