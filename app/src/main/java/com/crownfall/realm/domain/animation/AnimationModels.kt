package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.PieceType

/** Every distinct animated action a soldier can perform. */
enum class AnimationKind {
    IDLE,
    MARCH,
    STEP,
    GALLOP,
    CHARGING,
    SPIN_ATTACK,
    SPEAR_THRUST,
    SHIELD_BASH,
    HOLY_STRIKE,
    LANCE_IMPACT,
    EXECUTION,
    DRAW_WEAPON,
    HIT_REACT,
    DEATH_FALL,
    DEATH_KNOCKBACK,
    DEATH_DISSOLVE,
    PROMOTION,
    VICTORY,
    DEFEAT
}

/** Fantasy particle treatments, kept deliberately cheap to render. */
enum class ParticleStyle {
    NONE,
    ROYAL_SPARKS,
    HOLY_GLOW,
    DUST_CLOUD,
    IRON_SPARKS,
    GOLD_BURST,
    DARK_MIST
}

/** Cinematic camera treatments. */
enum class CameraEffect {
    NONE,

    /** Smooth push toward the action. */
    PUSH_IN,

    /** Small rumble for normal strikes. */
    SHAKE_LIGHT,

    /** Heavy rumble for charges and executions. */
    SHAKE_HEAVY,

    /** Slight orbit for elegant attacks. */
    ORBIT
}

/**
 * One step of a cinematic. Coordinates are normalised board units (0..7 across
 * the board) so the renderer can map them onto whatever size the board is.
 */
data class AnimationPhase(
    val kind: AnimationKind,
    val label: String,
    val durationMs: Int,
    val travelToX: Float? = null,
    val travelToY: Float? = null,
    val particles: ParticleStyle = ParticleStyle.NONE,
    val camera: CameraEffect = CameraEffect.NONE,
    val sound: SoundEffect? = null
) {
    /**
     * Returns a copy paced by [multiplier], which is a *time scale*:
     * 1.0 is normal, 1.6 is deliberately slow, 0.55 is fast.
     */
    fun paced(multiplier: Float): AnimationPhase {
        val safe = if (multiplier <= 0f) 1f else multiplier
        return copy(durationMs = (durationMs * safe).toInt().coerceAtLeast(40))
    }
}

/**
 * Per-pose deformation for a piece. All values are normalised so the animation
 * layer can drive them directly from its cinematic phases.
 */
data class PiecePose(
    /** Body rotation for knock-back and defeat (degrees). */
    val tiltDegrees: Float = 0f,
    /** 0 = weapon at rest, 1 = weapon fully raised for a strike. */
    val armRaise: Float = 0f,
    /** Extra forward drive for charges and shield bashes. */
    val lunge: Float = 0f,
    /** Vertical lift used by the warhorse leap. */
    val leap: Float = 0f,
    /** How far the shield is pushed forward. */
    val shieldForward: Float = 0f,
    /** Extra glow applied on impact frames. */
    val impactFlash: Float = 0f
) {
    companion object {
        val NEUTRAL = PiecePose()
    }
}

/** Small idle motion driven by a monotonically increasing clock. */
data class IdleTransform(
    val breatheScale: Float,
    val bobOffset: Float,
    val swayOffset: Float,
    val weaponRotation: Float,
    val cloakSway: Float
) {
    companion object {
        val NONE = IdleTransform(1f, 0f, 0f, 0f, 0f)
    }
}

/** Per-piece flavour used to shape the idle loop and the movement pacing. */
data class IdleProfile(
    val breatheAmplitude: Float,
    val bobAmplitude: Float,
    val swayAmplitude: Float,
    val weaponAmplitude: Float,
    val cloakAmplitude: Float,
    val cyclesPerSecond: Float,
    val displayName: String
)

/** Shared helper for building phase lists from piece profiles. */
internal fun PieceType.isMounted(): Boolean = this == PieceType.KNIGHT

/** A particle burst tied to a cinematic phase. */
data class ParticleSample(
    val file: Float,
    val rank: Float,
    val style: ParticleStyle,
    val progress: Float,
    val intensity: Float
)
