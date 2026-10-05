package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.chess.Move
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.chess.Square
import kotlin.math.abs
import kotlin.math.max

/**
 * Builds the cinematic that plays when a piece captures.
 *
 * The controller is pure: it returns a plan of timed phases, camera movements
 * and particle styles. The renderer decides how each phase is drawn, which
 * keeps the "what happens" and the "how it looks" cleanly separated and makes
 * the whole animation layer unit testable.
 */
class CaptureAnimationController {

    /**
     * A capture split into the attacker's beats and the defender's beats.
     * The defender starts [defenderStartOffsetMs] after the beginning, so the
     * reaction lands exactly on the impact frame.
     */
    data class CaptureCinematic(
        val attackerSquare: Square,
        val defenderSquare: Square,
        val attackerType: PieceType,
        val defenderType: PieceType?,
        val attackerPhases: List<AnimationPhase>,
        val defenderPhases: List<AnimationPhase>,
        val defenderStartOffsetMs: Int,
        val totalDurationMs: Int,
        val cameraZoom: Float,
        val cameraShake: Float,
        val particleStyle: ParticleStyle,
        val skippable: Boolean,
        /** True when "Combat Animation" is off: only a short slide remains. */
        val abbreviated: Boolean
    ) {
        fun allPhases(): List<AnimationPhase> = attackerPhases + defenderPhases
    }

    fun planCapture(
        move: Move,
        speedMultiplier: Float = 1f,
        combatEnabled: Boolean = true
    ): CaptureCinematic {
        val attacker = PieceAnimationProfiles.of(move.piece.type)
        val defender = move.captured?.let { PieceAnimationProfiles.of(it.type) }

        val targetX = move.to.file.toFloat()
        val targetY = move.to.rank.toFloat()

        if (!combatEnabled || defender == null) {
            val slide = AnimationPhase(
                kind = if (move.piece.type == PieceType.KNIGHT) AnimationKind.GALLOP else AnimationKind.STEP,
                label = "Advance",
                durationMs = 150,
                travelToX = targetX,
                travelToY = targetY
            ).paced(speedMultiplier)
            val removal = if (defender != null) {
                listOf(
                    AnimationPhase(
                        kind = AnimationKind.DEATH_DISSOLVE,
                        label = "Removed",
                        durationMs = 120
                    ).paced(speedMultiplier)
                )
            } else {
                emptyList()
            }
            return CaptureCinematic(
                attackerSquare = move.from,
                defenderSquare = move.to,
                attackerType = move.piece.type,
                defenderType = move.captured?.type,
                attackerPhases = listOf(slide),
                defenderPhases = removal,
                defenderStartOffsetMs = slide.durationMs,
                totalDurationMs = slide.durationMs + removal.sumOf { it.durationMs },
                cameraZoom = 1f,
                cameraShake = 0f,
                particleStyle = ParticleStyle.NONE,
                skippable = true,
                abbreviated = true
            )
        }

        val attackPhases = attacker.attack.phases(speedMultiplier, targetX, targetY)
        val deathPhases = defender.death.phases(speedMultiplier)

        val windUp = attackPhases.firstOrNull()?.durationMs ?: 0
        val strike = attackPhases.getOrNull(1)?.durationMs ?: 0
        val offset = (windUp + strike * (attacker.attack.impactAtPercent / 100f)).toInt()

        val attackTotal = attackPhases.sumOf { it.durationMs }
        val deathTotal = deathPhases.sumOf { it.durationMs }

        val shake = (attacker.cameraShakeBias + defender.cameraShakeBias) / 2f
        val zoom = when (attacker.attack.camera) {
            CameraEffect.PUSH_IN -> 1.35f
            CameraEffect.ORBIT -> 1.25f
            CameraEffect.SHAKE_HEAVY -> 1.20f
            CameraEffect.SHAKE_LIGHT -> 1.12f
            CameraEffect.NONE -> 1.05f
        }

        return CaptureCinematic(
            attackerSquare = move.from,
            defenderSquare = move.to,
            attackerType = move.piece.type,
            defenderType = move.captured?.type,
            attackerPhases = attackPhases,
            defenderPhases = deathPhases,
            defenderStartOffsetMs = offset,
            totalDurationMs = max(attackTotal, offset + deathTotal),
            cameraZoom = zoom,
            cameraShake = shake.coerceIn(0f, 1f),
            particleStyle = attacker.attack.particles,
            skippable = true,
            abbreviated = false
        )
    }

    /** Non-capturing move phases (plus the promotion flourish when relevant). */
    fun planMovement(
        move: Move,
        speedMultiplier: Float = 1f,
        combatEnabled: Boolean = true
    ): List<AnimationPhase> {
        val profile = PieceAnimationProfiles.of(move.piece.type)
        val distance = distance(move.from, move.to)
        if (!combatEnabled) {
            return listOf(
                AnimationPhase(
                    kind = AnimationKind.STEP,
                    label = "Advance",
                    durationMs = 150,
                    travelToX = move.to.file.toFloat(),
                    travelToY = move.to.rank.toFloat()
                ).paced(speedMultiplier)
            )
        }
        val phases = ArrayList<AnimationPhase>(
            profile.movement.phases(distance, speedMultiplier, move.to.file.toFloat(), move.to.rank.toFloat())
        )
        if (move.promotion != null) {
            phases += PromotionAnimation().phases(speedMultiplier, move.promotion!!)
        }
        return phases
    }

    fun planPromotion(promotedTo: PieceType, speedMultiplier: Float = 1f): List<AnimationPhase> =
        PromotionAnimation().phases(speedMultiplier, promotedTo)

    /** Chebyshev distance in squares; used to pace gallops and marches. */
    fun distance(from: Square, to: Square): Int =
        max(abs(to.file - from.file), abs(to.rank - from.rank))
}
