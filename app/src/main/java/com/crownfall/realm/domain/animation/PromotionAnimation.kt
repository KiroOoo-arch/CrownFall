package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.PieceType

/**
 * The foot soldier kneels, is engulfed in light and rises as a new role.
 * The piece type only changes in the final phase, which keeps the board model
 * consistent with what the player sees.
 */
data class PromotionAnimation(
    val label: String = "Promotion",
    val gatherMs: Int = 420,
    val transformMs: Int = 520,
    val flareMs: Int = 260,
    val particles: ParticleStyle = ParticleStyle.GOLD_BURST,
    val sound: SoundEffect = SoundEffect.PROMOTION
) {
    val totalMs: Int get() = gatherMs + transformMs + flareMs

    fun phases(multiplier: Float, promotedTo: PieceType): List<AnimationPhase> = listOf(
        AnimationPhase(
            kind = AnimationKind.PROMOTION,
            label = "$label: $promotedTo gathers",
            durationMs = gatherMs,
            particles = ParticleStyle.HOLY_GLOW
        ),
        AnimationPhase(
            kind = AnimationKind.PROMOTION,
            label = "$label: becomes ${promotedTo.medievalName}",
            durationMs = transformMs,
            particles = particles,
            camera = CameraEffect.PUSH_IN,
            sound = sound
        ),
        AnimationPhase(
            kind = AnimationKind.PROMOTION,
            label = "$label: rises",
            durationMs = flareMs,
            particles = ParticleStyle.GOLD_BURST
        )
    ).map { it.paced(multiplier) }
}
