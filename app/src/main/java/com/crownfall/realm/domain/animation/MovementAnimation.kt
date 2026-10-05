package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect

/**
 * A non-capturing move. Pawns step, the king walks, and the mounted knight
 * gallops with a longer travel so it reads as the fastest unit on the field.
 */
data class MovementAnimation(
    val kind: AnimationKind,
    val label: String,
    val baseTravelMs: Int,
    val perSquareMs: Int,
    /** Bob amplitude of the walk cycle, in board units. */
    val bobAmplitude: Float,
    val sound: SoundEffect,
    val footstepSound: SoundEffect?
) {
    /** [multiplier] is a time scale: 1.6 slow, 0.55 fast. */
    fun durationMs(distanceInSquares: Int, multiplier: Float): Int {
        val raw = baseTravelMs + perSquareMs * distanceInSquares.coerceAtLeast(1)
        val safe = if (multiplier <= 0f) 1f else multiplier
        return (raw * safe).toInt().coerceAtLeast(90)
    }

    fun phases(
        distanceInSquares: Int,
        multiplier: Float,
        toX: Float,
        toY: Float
    ): List<AnimationPhase> = listOf(
        AnimationPhase(
            kind = AnimationKind.IDLE,
            label = "$label: brace",
            durationMs = (120 * if (multiplier <= 0f) 1f else multiplier).toInt().coerceAtLeast(40),
            sound = footstepSound
        ),
        AnimationPhase(
            kind = kind,
            label = label,
            durationMs = durationMs(distanceInSquares, multiplier),
            travelToX = toX,
            travelToY = toY,
            particles = ParticleStyle.DUST_CLOUD,
            sound = sound
        )
    )
}
