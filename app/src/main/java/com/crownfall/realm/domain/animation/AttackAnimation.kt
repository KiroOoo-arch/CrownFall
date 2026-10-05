package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect

/**
 * The offensive half of a capture. Three beats: wind up, strike, recover.
 * [impactAtPercent] tells the renderer when the defender should react so the
 * two halves stay in sync.
 */
data class AttackAnimation(
    val kind: AnimationKind,
    val label: String,
    val windUpMs: Int,
    val strikeMs: Int,
    val recoverMs: Int,
    val particles: ParticleStyle,
    val camera: CameraEffect,
    val sound: SoundEffect,
    val impactSound: SoundEffect,
    val impactAtPercent: Float = 55f
) {
    val totalMs: Int get() = windUpMs + strikeMs + recoverMs

    fun phases(multiplier: Float, targetX: Float, targetY: Float): List<AnimationPhase> = listOf(
        AnimationPhase(
            kind = AnimationKind.DRAW_WEAPON,
            label = "$label: ready",
            durationMs = windUpMs
        ),
        AnimationPhase(
            kind = kind,
            label = label,
            durationMs = strikeMs,
            travelToX = targetX,
            travelToY = targetY,
            particles = particles,
            camera = camera,
            sound = sound
        ),
        AnimationPhase(
            kind = kind,
            label = "$label: impact",
            durationMs = recoverMs,
            particles = particles,
            sound = impactSound
        )
    ).map { it.paced(multiplier) }
}
