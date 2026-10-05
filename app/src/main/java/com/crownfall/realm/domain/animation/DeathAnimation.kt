package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect

/**
 * The defensive half of a capture: the target reacts, is defeated and is
 * removed from the board. The animation deliberately does not delete the piece
 * model - the caller removes it only after the last phase completes.
 */
data class DeathAnimation(
    val kind: AnimationKind,
    val label: String,
    val reactMs: Int,
    val defeatMs: Int,
    val fadeMs: Int,
    val particles: ParticleStyle,
    val camera: CameraEffect,
    val sound: SoundEffect?,
    /** How far the body is knocked back, in board units. */
    val knockbackDistance: Float = 0.35f
) {
    val totalMs: Int get() = reactMs + defeatMs + fadeMs

    fun phases(multiplier: Float): List<AnimationPhase> = listOf(
        AnimationPhase(
            kind = AnimationKind.HIT_REACT,
            label = "$label: reacts",
            durationMs = reactMs
        ),
        AnimationPhase(
            kind = kind,
            label = label,
            durationMs = defeatMs,
            travelToX = knockbackDistance,
            travelToY = knockbackDistance,
            particles = particles,
            camera = camera,
            sound = sound
        ),
        AnimationPhase(
            kind = AnimationKind.DEATH_DISSOLVE,
            label = "$label: removed",
            durationMs = fadeMs,
            particles = particles
        )
    ).map { it.paced(multiplier) }
}
