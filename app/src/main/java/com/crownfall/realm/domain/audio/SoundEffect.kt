package com.crownfall.realm.domain.audio

/**
 * Every sound the realm can make. The animation layer references these
 * symbolically so the audio backend can be swapped (synthesised PCM today,
 * recorded assets later) without touching gameplay code.
 */
enum class SoundEffect(
    val displayName: String,
    val defaultDurationMs: Int,
    /** Relative loudness used by the synthesiser. */
    val gain: Float,
    val isCombatSound: Boolean
) {
    SWORD_SWING("Sword swing", 260, 0.7f, true),
    SWORD_IMPACT("Sword impact", 320, 0.95f, true),
    ARMOR_MOVE("Armor movement", 380, 0.45f, true),
    HORSE_GALLOP("Horse gallop", 520, 0.7f, true),
    SHIELD_BASH("Shield impact", 420, 1.0f, true),
    MAGIC_CAST("Holy magic", 700, 0.6f, true),
    PIECE_MOVE("Piece movement", 180, 0.4f, false),
    CHECK_WARNING("Check warning", 600, 0.8f, false),
    CHECKMATE("Checkmate", 900, 0.95f, false),
    BUTTON_CLICK("Button click", 90, 0.5f, false),
    VICTORY_MUSIC("Victory fanfare", 2200, 0.8f, false),
    DEFEAT_MUSIC("Defeat lament", 2400, 0.7f, false),
    PROMOTION("Promotion", 1100, 0.85f, false),
    DRAW("Truce", 1400, 0.6f, false);
}
