package com.crownfall.realm.audio

import com.crownfall.realm.domain.audio.SoundEffect

/**
 * The contract every audio backend must satisfy. Keeping this small means the
 * synthesised engine used today can be replaced by a recorded-assets engine
 * (or muted entirely in previews) without touching any gameplay code.
 */
interface AudioEngine {

    fun play(effect: SoundEffect)

    fun setMusicVolume(volume: Float)

    fun setSoundVolume(volume: Float)

    fun setCombatSoundsEnabled(enabled: Boolean)

    fun startMusic()

    fun stopMusic()

    fun release()

    val isMusicPlaying: Boolean
}

/** Silent engine used by previews and tests. */
object NoOpAudioEngine : AudioEngine {
    override fun play(effect: SoundEffect) = Unit
    override fun setMusicVolume(volume: Float) = Unit
    override fun setSoundVolume(volume: Float) = Unit
    override fun setCombatSoundsEnabled(enabled: Boolean) = Unit
    override fun startMusic() = Unit
    override fun stopMusic() = Unit
    override fun release() = Unit
    override val isMusicPlaying: Boolean = false
}
