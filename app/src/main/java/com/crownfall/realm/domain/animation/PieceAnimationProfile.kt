package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.PieceType

/**
 * The complete animated identity of one medieval role. Every piece has an
 * idle loop, a travel style, an attack and a death - all of them distinct, so
 * the board reads like a battle rather than six recoloured symbols.
 */
data class PieceAnimationProfile(
    val type: PieceType,
    /** Short flavour text shown in the "Codex" tooltips. */
    val identity: String,
    val idle: IdleProfile,
    val movement: MovementAnimation,
    val attack: AttackAnimation,
    val death: DeathAnimation,
    /** Extra camera shake this unit adds on top of the phase value, 0..1. */
    val cameraShakeBias: Float
)

object PieceAnimationProfiles {

    private val KING = PieceAnimationProfile(
        type = PieceType.KING,
        identity = "Armored king with crown, sword and cape. Walks slowly, executes with a single heavy blow.",
        idle = IdleProfile(0.030f, 0.020f, 0.008f, 0.055f, 0.045f, 0.45f, "Royal breathing"),
        movement = MovementAnimation(
            kind = AnimationKind.MARCH,
            label = "King advances",
            baseTravelMs = 260,
            perSquareMs = 150,
            bobAmplitude = 0.02f,
            sound = SoundEffect.ARMOR_MOVE,
            footstepSound = SoundEffect.PIECE_MOVE
        ),
        attack = AttackAnimation(
            kind = AnimationKind.EXECUTION,
            label = "Royal execution",
            windUpMs = 340,
            strikeMs = 300,
            recoverMs = 400,
            particles = ParticleStyle.ROYAL_SPARKS,
            camera = CameraEffect.SHAKE_HEAVY,
            sound = SoundEffect.SWORD_SWING,
            impactSound = SoundEffect.SWORD_IMPACT,
            impactAtPercent = 52f
        ),
        death = DeathAnimation(
            kind = AnimationKind.DEATH_FALL,
            label = "King falls",
            reactMs = 260,
            defeatMs = 420,
            fadeMs = 320,
            particles = ParticleStyle.ROYAL_SPARKS,
            camera = CameraEffect.SHAKE_LIGHT,
            sound = SoundEffect.SWORD_IMPACT,
            knockbackDistance = 0.30f
        ),
        cameraShakeBias = 0.25f
    )

    private val QUEEN = PieceAnimationProfile(
        type = PieceType.QUEEN,
        identity = "Warrior queen with long cloak and an ornate sword. Fast, elegant spinning strike.",
        idle = IdleProfile(0.028f, 0.014f, 0.012f, 0.075f, 0.065f, 0.60f, "Cloak flourish"),
        movement = MovementAnimation(
            kind = AnimationKind.STEP,
            label = "Queen strides",
            baseTravelMs = 180,
            perSquareMs = 100,
            bobAmplitude = 0.018f,
            sound = SoundEffect.PIECE_MOVE,
            footstepSound = SoundEffect.PIECE_MOVE
        ),
        attack = AttackAnimation(
            kind = AnimationKind.SPIN_ATTACK,
            label = "Spinning strike",
            windUpMs = 220,
            strikeMs = 280,
            recoverMs = 300,
            particles = ParticleStyle.ROYAL_SPARKS,
            camera = CameraEffect.ORBIT,
            sound = SoundEffect.SWORD_SWING,
            impactSound = SoundEffect.SWORD_IMPACT,
            impactAtPercent = 48f
        ),
        death = DeathAnimation(
            kind = AnimationKind.DEATH_FALL,
            label = "Queen falls",
            reactMs = 220,
            defeatMs = 380,
            fadeMs = 300,
            particles = ParticleStyle.ROYAL_SPARKS,
            camera = CameraEffect.SHAKE_LIGHT,
            sound = SoundEffect.ARMOR_MOVE,
            knockbackDistance = 0.45f
        ),
        cameraShakeBias = 0.15f
    )

    private val ROOK = PieceAnimationProfile(
        type = PieceType.ROOK,
        identity = "Castle Guardian / armored tower knight. Charges and shield-bashes with a heavy impact.",
        idle = IdleProfile(0.040f, 0.024f, 0.005f, 0.030f, 0.010f, 0.35f, "Heavy armor breathing"),
        movement = MovementAnimation(
            kind = AnimationKind.MARCH,
            label = "Guardian marches",
            baseTravelMs = 250,
            perSquareMs = 150,
            bobAmplitude = 0.028f,
            sound = SoundEffect.ARMOR_MOVE,
            footstepSound = SoundEffect.ARMOR_MOVE
        ),
        attack = AttackAnimation(
            kind = AnimationKind.SHIELD_BASH,
            label = "Shield bash charge",
            windUpMs = 320,
            strikeMs = 380,
            recoverMs = 440,
            particles = ParticleStyle.DUST_CLOUD,
            camera = CameraEffect.SHAKE_HEAVY,
            sound = SoundEffect.ARMOR_MOVE,
            impactSound = SoundEffect.SHIELD_BASH,
            impactAtPercent = 58f
        ),
        death = DeathAnimation(
            kind = AnimationKind.DEATH_KNOCKBACK,
            label = "Guardian knocked down",
            reactMs = 240,
            defeatMs = 460,
            fadeMs = 320,
            particles = ParticleStyle.DUST_CLOUD,
            camera = CameraEffect.SHAKE_HEAVY,
            sound = SoundEffect.SHIELD_BASH,
            knockbackDistance = 0.60f
        ),
        cameraShakeBias = 0.55f
    )

    private val BISHOP = PieceAnimationProfile(
        type = PieceType.BISHOP,
        identity = "Battle cleric with staff. Channels a holy strike from a distance.",
        idle = IdleProfile(0.022f, 0.010f, 0.010f, 0.050f, 0.035f, 0.50f, "Staff hum"),
        movement = MovementAnimation(
            kind = AnimationKind.STEP,
            label = "Cleric glides",
            baseTravelMs = 190,
            perSquareMs = 105,
            bobAmplitude = 0.012f,
            sound = SoundEffect.PIECE_MOVE,
            footstepSound = SoundEffect.PIECE_MOVE
        ),
        attack = AttackAnimation(
            kind = AnimationKind.HOLY_STRIKE,
            label = "Holy strike",
            windUpMs = 420,
            strikeMs = 440,
            recoverMs = 360,
            particles = ParticleStyle.HOLY_GLOW,
            camera = CameraEffect.PUSH_IN,
            sound = SoundEffect.MAGIC_CAST,
            impactSound = SoundEffect.MAGIC_CAST,
            impactAtPercent = 62f
        ),
        death = DeathAnimation(
            kind = AnimationKind.DEATH_DISSOLVE,
            label = "Cleric banished",
            reactMs = 260,
            defeatMs = 440,
            fadeMs = 380,
            particles = ParticleStyle.HOLY_GLOW,
            camera = CameraEffect.SHAKE_LIGHT,
            sound = SoundEffect.MAGIC_CAST,
            knockbackDistance = 0.22f
        ),
        cameraShakeBias = 0.10f
    )

    private val KNIGHT = PieceAnimationProfile(
        type = PieceType.KNIGHT,
        identity = "Armored knight on a warhorse. Gallops, leaps and batters the enemy backward.",
        idle = IdleProfile(0.034f, 0.030f, 0.014f, 0.085f, 0.020f, 0.70f, "Horse breathing"),
        movement = MovementAnimation(
            kind = AnimationKind.GALLOP,
            label = "Warhorse gallop",
            baseTravelMs = 150,
            perSquareMs = 72,
            bobAmplitude = 0.055f,
            sound = SoundEffect.HORSE_GALLOP,
            footstepSound = SoundEffect.ARMOR_MOVE
        ),
        attack = AttackAnimation(
            kind = AnimationKind.LANCE_IMPACT,
            label = "Lance charge",
            windUpMs = 300,
            strikeMs = 440,
            recoverMs = 470,
            particles = ParticleStyle.DUST_CLOUD,
            camera = CameraEffect.SHAKE_HEAVY,
            sound = SoundEffect.HORSE_GALLOP,
            impactSound = SoundEffect.SWORD_IMPACT,
            impactAtPercent = 64f
        ),
        death = DeathAnimation(
            kind = AnimationKind.DEATH_KNOCKBACK,
            label = "Rider unhorsed",
            reactMs = 260,
            defeatMs = 520,
            fadeMs = 340,
            particles = ParticleStyle.DUST_CLOUD,
            camera = CameraEffect.SHAKE_HEAVY,
            sound = SoundEffect.SHIELD_BASH,
            knockbackDistance = 0.85f
        ),
        cameraShakeBias = 0.70f
    )

    private val PAWN = PieceAnimationProfile(
        type = PieceType.PAWN,
        identity = "Foot soldier with helmet, shield and spear. Steps forward and thrusts.",
        idle = IdleProfile(0.018f, 0.012f, 0.006f, 0.025f, 0.008f, 0.80f, "Soldier breathing"),
        movement = MovementAnimation(
            kind = AnimationKind.STEP,
            label = "Soldier steps",
            baseTravelMs = 150,
            perSquareMs = 90,
            bobAmplitude = 0.016f,
            sound = SoundEffect.PIECE_MOVE,
            footstepSound = SoundEffect.PIECE_MOVE
        ),
        attack = AttackAnimation(
            kind = AnimationKind.SPEAR_THRUST,
            label = "Spear thrust",
            windUpMs = 140,
            strikeMs = 170,
            recoverMs = 200,
            particles = ParticleStyle.IRON_SPARKS,
            camera = CameraEffect.SHAKE_LIGHT,
            sound = SoundEffect.SWORD_SWING,
            impactSound = SoundEffect.SWORD_IMPACT,
            impactAtPercent = 45f
        ),
        death = DeathAnimation(
            kind = AnimationKind.DEATH_FALL,
            label = "Soldier falls",
            reactMs = 160,
            defeatMs = 280,
            fadeMs = 240,
            particles = ParticleStyle.DUST_CLOUD,
            camera = CameraEffect.SHAKE_LIGHT,
            sound = SoundEffect.ARMOR_MOVE,
            knockbackDistance = 0.40f
        ),
        cameraShakeBias = 0.05f
    )

    val ALL: List<PieceAnimationProfile> = listOf(KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN)

    private val byType: Map<PieceType, PieceAnimationProfile> = ALL.associateBy { it.type }

    fun of(type: PieceType): PieceAnimationProfile =
        byType[type] ?: error("No animation profile for $type")

    /** The most cinematic unit, used by the "highlight the best capture" feature. */
    val mostCinematic: PieceAnimationProfile = KNIGHT
}
