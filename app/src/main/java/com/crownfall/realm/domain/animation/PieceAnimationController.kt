package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.chess.PieceType
import kotlin.math.PI
import kotlin.math.sin

/**
 * Drives the always-on idle animation and exposes each piece's animated
 * identity. Idle motion is a pure function of elapsed time, so the renderer can
 * sample it every frame without allocating or storing per-piece state.
 */
class PieceAnimationController(
    val captures: CaptureAnimationController = CaptureAnimationController()
) {

    fun profile(type: PieceType): PieceAnimationProfile = PieceAnimationProfiles.of(type)

    fun idleProfile(type: PieceType): IdleProfile = profile(type).idle

    fun attack(type: PieceType): AttackAnimation = profile(type).attack

    fun movement(type: PieceType): MovementAnimation = profile(type).movement

    fun death(type: PieceType): DeathAnimation = profile(type).death

    /** Loop length of the idle cycle in milliseconds. */
    fun idleLoopMs(type: PieceType): Int =
        (1000f / idleProfile(type).cyclesPerSecond).toInt().coerceAtLeast(400)

    /**
     * Samples the idle loop. [elapsedMs] is a monotonically increasing clock;
     * each piece uses its own phase offset so the army never breathes in unison.
     */
    fun idleTransform(type: PieceType, elapsedMs: Long): IdleTransform {
        val profile = idleProfile(type)
        val loop = idleLoopMs(type).toFloat()
        val offset = (type.ordinal * 137) % 360
        val phase = ((elapsedMs % loop.toLong()).toFloat() / loop) * 2f * PI.toFloat() +
            offset * PI.toFloat() / 180f

        val breathe = sin(phase)
        val bob = sin(phase * 2f)
        val sway = sin(phase * 0.5f + 0.7f)

        return IdleTransform(
            breatheScale = 1f + profile.breatheAmplitude * breathe,
            bobOffset = profile.bobAmplitude * bob,
            swayOffset = profile.swayAmplitude * sway,
            weaponRotation = profile.weaponAmplitude * sin(phase * 1.5f) * 12f,
            cloakSway = profile.cloakAmplitude * sway * 8f
        )
    }

    /** Direction a unit is facing, used to mirror horse and sword poses. */
    fun facingDegrees(factionDawn: Boolean, towardHigherRank: Boolean): Float = when {
        factionDawn && towardHigherRank -> 0f
        factionDawn -> 180f
        towardHigherRank -> 180f
        else -> 0f
    }

    /** Camera shake offset for the current phase, in board units. */
    fun shakeOffset(strength: Float, elapsedMs: Long): Pair<Float, Float> {
        if (strength <= 0f) return 0f to 0f
        val t = elapsedMs / 42.0
        val x = (sin(t * 1.7) * 0.055 * strength).toFloat()
        val y = (sin(t * 2.3 + 1.1) * 0.045 * strength).toFloat()
        return x to y
    }
}
