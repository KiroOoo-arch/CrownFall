package com.crownfall.realm.domain.animation

import kotlin.math.PI
import kotlin.math.sin

/**
 * Translates an [AnimationKind] plus its progress into a concrete [PiecePose].
 * Pure maths, so a cinematic looks identical on every device and can be tested.
 */
object PoseMapper {

    fun poseFor(kind: AnimationKind, progress: Float, intensity: Float = 1f): PiecePose {
        val t = progress.coerceIn(0f, 1f)
        return when (kind) {
            AnimationKind.DRAW_WEAPON -> PiecePose(armRaise = t * 0.9f)

            AnimationKind.SPIN_ATTACK -> PiecePose(
                tiltDegrees = sin(t * PI.toFloat()) * 12f,
                armRaise = 0.6f + t * 0.4f,
                lunge = 0.6f,
                impactFlash = if (t > 0.6f) intensity else 0f
            )

            AnimationKind.SPEAR_THRUST -> PiecePose(
                armRaise = 0.3f + t * 0.5f,
                lunge = t * 1.1f,
                impactFlash = if (t > 0.55f) intensity * 0.5f else 0f
            )

            AnimationKind.SHIELD_BASH -> PiecePose(
                armRaise = t * 0.35f,
                lunge = t * 0.9f,
                shieldForward = t * 1.4f,
                impactFlash = if (t > 0.6f) intensity else 0f
            )

            AnimationKind.HOLY_STRIKE -> PiecePose(
                armRaise = 0.5f + t * 0.5f,
                lunge = t * 0.25f,
                impactFlash = t * intensity
            )

            AnimationKind.LANCE_IMPACT -> PiecePose(
                armRaise = 0.4f + t * 0.4f,
                lunge = t * 1.2f,
                leap = if (t > 0.25f && t < 0.75f) 0.7f else 0.2f,
                impactFlash = if (t > 0.6f) intensity else 0f
            )

            AnimationKind.EXECUTION -> PiecePose(
                armRaise = if (t < 0.5f) 1f else 0.25f,
                lunge = if (t > 0.5f) 0.55f else 0f,
                impactFlash = if (t > 0.62f) intensity else 0f
            )

            AnimationKind.CHARGING -> PiecePose(lunge = 0.8f * t, armRaise = 0.4f)

            AnimationKind.HIT_REACT -> PiecePose(tiltDegrees = -6f * t)

            AnimationKind.DEATH_FALL -> PiecePose(tiltDegrees = 78f * t)

            AnimationKind.DEATH_KNOCKBACK -> PiecePose(tiltDegrees = 62f * t, lunge = -0.6f * t)

            AnimationKind.DEATH_DISSOLVE -> PiecePose(tiltDegrees = 18f * t, impactFlash = t * 0.4f)

            AnimationKind.MARCH -> PiecePose(lunge = 0.05f)

            AnimationKind.STEP -> PiecePose()

            AnimationKind.GALLOP -> PiecePose(lunge = 0.35f, leap = 0.45f)

            AnimationKind.PROMOTION -> PiecePose(armRaise = 0.7f, impactFlash = t)

            AnimationKind.VICTORY -> PiecePose(armRaise = 0.9f, impactFlash = t * 0.6f)

            AnimationKind.DEFEAT -> PiecePose(tiltDegrees = 22f * t)

            AnimationKind.IDLE -> PiecePose.NEUTRAL
        }
    }
}
