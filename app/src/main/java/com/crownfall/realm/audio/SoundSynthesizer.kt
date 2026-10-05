package com.crownfall.realm.audio

import com.crownfall.realm.domain.audio.SoundEffect
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates every medieval sound effect as raw PCM on the fly.
 *
 * Synthesising instead of shipping audio files keeps the repository free of
 * binary assets while still giving the game real, distinct sounds: steel rings
 * with metallic partials, hooves are low thumps, holy magic is a rising sweep.
 * Swapping in recorded assets later only means replacing the [AudioEngine].
 */
object SoundSynthesizer {

    const val SAMPLE_RATE = 44_100

    fun samples(effect: SoundEffect): ShortArray {
        val durationMs = effect.defaultDurationMs
        val count = SAMPLE_RATE * durationMs / 1000
        val buffer = FloatArray(count)
        val random = Random(effect.name.hashCode())

        when (effect) {
            SoundEffect.SWORD_SWING -> swordSwing(buffer, random)
            SoundEffect.SWORD_IMPACT -> swordImpact(buffer, random)
            SoundEffect.ARMOR_MOVE -> armorMove(buffer, random)
            SoundEffect.HORSE_GALLOP -> gallop(buffer, random)
            SoundEffect.SHIELD_BASH -> shieldBash(buffer, random)
            SoundEffect.MAGIC_CAST -> magicCast(buffer)
            SoundEffect.PIECE_MOVE -> pieceMove(buffer, random)
            SoundEffect.CHECK_WARNING -> checkWarning(buffer)
            SoundEffect.CHECKMATE -> fanfare(buffer, ascending = false, dramatic = true)
            SoundEffect.BUTTON_CLICK -> buttonClick(buffer, random)
            SoundEffect.VICTORY_MUSIC -> fanfare(buffer, ascending = true, dramatic = false)
            SoundEffect.DEFEAT_MUSIC -> lament(buffer)
            SoundEffect.PROMOTION -> promotion(buffer)
            SoundEffect.DRAW -> truce(buffer)
        }

        return toShorts(buffer, effect.gain)
    }

    // ------------------------------------------------------------- textures

    private fun swordSwing(buffer: FloatArray, random: Random) {
        val n = buffer.size
        var lowPass = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val envelope = sin(PI * t).toFloat()
            val noise = random.nextFloat() * 2f - 1f
            val cutoff = 0.35f - 0.28f * t
            lowPass += cutoff * (noise - lowPass)
            buffer[i] = lowPass * envelope
        }
    }

    private fun swordImpact(buffer: FloatArray, random: Random) {
        val n = buffer.size
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val decay = exp(-t * 18f)
            val thud = sin(2f * PI.toFloat() * 95f * t) * 0.7f
            val ring = (sin(2f * PI.toFloat() * 780f * t) +
                sin(2f * PI.toFloat() * 1240f * t) * 0.6f +
                sin(2f * PI.toFloat() * 1970f * t) * 0.35f) * 0.25f
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t * 55f) * 0.6f
            buffer[i] = (thud + ring) * decay + noise * 0.4f
        }
    }

    private fun armorMove(buffer: FloatArray, random: Random) {
        val n = buffer.size
        var lowPass = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val clank = when {
                t < 0.06f -> exp(-t * 60f)
                t in 0.18f..0.26f -> exp(-(t - 0.18f) * 60f) * 0.7f
                t > 0.48f -> exp(-(t - 0.48f) * 50f) * 0.9f
                else -> 0.05f
            }
            val noise = random.nextFloat() * 2f - 1f
            lowPass += 0.25f * (noise - lowPass)
            val metal = sin(2f * PI.toFloat() * 430f * i / SAMPLE_RATE) * 0.18f
            buffer[i] = (lowPass * 0.6f + metal) * clank
        }
    }

    private fun gallop(buffer: FloatArray, random: Random) {
        val n = buffer.size
        val hoofCount = 4
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            var sample = 0f
            for (h in 0 until hoofCount) {
                val start = h * 0.11f
                val dt = t - start
                if (dt >= 0f && dt < 0.09f) {
                    val env = exp(-dt * 42f)
                    val thump = sin(2f * PI.toFloat() * 130f * dt) * 0.8f
                    val click = (random.nextFloat() * 2f - 1f) * exp(-dt * 90f) * 0.4f
                    sample += (thump + click) * env * 0.6f
                }
            }
            buffer[i] = sample
        }
    }

    private fun shieldBash(buffer: FloatArray, random: Random) {
        val n = buffer.size
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val decay = exp(-t * 9f)
            val boom = sin(2f * PI.toFloat() * 62f * t) * 0.9f
            val metal = (sin(2f * PI.toFloat() * 240f * t) * 0.3f +
                sin(2f * PI.toFloat() * 372f * t) * 0.22f +
                sin(2f * PI.toFloat() * 611f * t) * 0.15f)
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t * 30f) * 0.5f
            buffer[i] = boom * decay + metal * exp(-t * 5f) + noise * 0.35f
        }
    }

    private fun magicCast(buffer: FloatArray) {
        val n = buffer.size
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val time = i.toFloat() / SAMPLE_RATE
            val freq = 190f + 1_100f * t * t
            val envelope = sin(PI * t).toFloat() * 0.85f
            val shimmer = sin(2f * PI.toFloat() * freq * time) * 0.7f +
                sin(2f * PI.toFloat() * (freq * 1.5f) * time) * 0.3f +
                sin(2f * PI.toFloat() * (freq * 2.02f) * time) * 0.2f
            val tremolo = 0.85f + 0.15f * sin(2f * PI.toFloat() * 9f * time)
            buffer[i] = shimmer * envelope * tremolo
        }
    }

    private fun pieceMove(buffer: FloatArray, random: Random) {
        val n = buffer.size
        var lowPass = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val noise = random.nextFloat() * 2f - 1f
            lowPass += 0.2f * (noise - lowPass)
            buffer[i] = lowPass * exp(-t * 9f) * 0.7f +
                sin(2f * PI.toFloat() * 210f * i / SAMPLE_RATE) * exp(-t * 12f) * 0.25f
        }
    }

    private fun checkWarning(buffer: FloatArray) {
        val n = buffer.size
        for (i in 0 until n) {
            val time = i.toFloat() / SAMPLE_RATE
            val t = i.toFloat() / n
            val second = if (t < 0.5f) 0f else 1f
            val freq = 660f + second * 220f
            val envelope = (1f - t) * 0.8f
            val tremolo = 0.6f + 0.4f * sin(2f * PI.toFloat() * 14f * time)
            buffer[i] = sin(2f * PI.toFloat() * freq * time) * envelope * tremolo
        }
    }

    private fun fanfare(buffer: FloatArray, ascending: Boolean, dramatic: Boolean) {
        val n = buffer.size
        val notes = if (ascending) listOf(392f, 523f, 659f, 784f) else listOf(587f, 494f, 392f, 294f)
        val noteLength = n / notes.size
        for (i in 0 until n) {
            val noteIndex = (i / noteLength).coerceAtMost(notes.size - 1)
            val local = i - noteIndex * noteLength
            val t = local.toFloat() / SAMPLE_RATE
            val decay = if (dramatic) exp(-t * 3.2f) else exp(-t * 2.0f)
            val freq = notes[noteIndex]
            val tone = sin(2f * PI.toFloat() * freq * t) * 0.6f +
                sin(2f * PI.toFloat() * freq * 2f * t) * 0.22f +
                sin(2f * PI.toFloat() * freq * 3f * t) * 0.1f
            val attack = (local.toFloat() / (SAMPLE_RATE * 0.01f)).coerceAtMost(1f)
            buffer[i] = tone * decay * attack
        }
    }

    private fun lament(buffer: FloatArray) {
        val n = buffer.size
        val notes = listOf(330f, 311f, 294f, 220f)
        val noteLength = n / notes.size
        for (i in 0 until n) {
            val noteIndex = (i / noteLength).coerceAtMost(notes.size - 1)
            val local = i - noteIndex * noteLength
            val t = local.toFloat() / SAMPLE_RATE
            val freq = notes[noteIndex]
            val vibrato = 1f + 0.006f * sin(2f * PI.toFloat() * 5f * t)
            val tone = sin(2f * PI.toFloat() * freq * vibrato * t) * 0.55f +
                sin(2f * PI.toFloat() * freq * 2f * vibrato * t) * 0.18f
            buffer[i] = tone * exp(-t * 1.6f)
        }
    }

    private fun promotion(buffer: FloatArray) {
        val n = buffer.size
        val notes = listOf(523f, 659f, 784f, 1046f)
        val noteLength = n / notes.size
        for (i in 0 until n) {
            val noteIndex = (i / noteLength).coerceAtMost(notes.size - 1)
            val local = i - noteIndex * noteLength
            val t = local.toFloat() / SAMPLE_RATE
            val freq = notes[noteIndex]
            val tone = sin(2f * PI.toFloat() * freq * t) * 0.5f +
                sin(2f * PI.toFloat() * freq * 1.5f * t) * 0.25f
            buffer[i] = tone * exp(-t * 2.4f)
        }
    }

    private fun truce(buffer: FloatArray) {
        val n = buffer.size
        for (i in 0 until n) {
            val t = i.toFloat() / SAMPLE_RATE
            val envelope = sin(PI * (i.toFloat() / n)).toFloat()
            buffer[i] = (sin(2f * PI.toFloat() * 349f * t) * 0.4f +
                sin(2f * PI.toFloat() * 440f * t) * 0.4f) * envelope
        }
    }

    private fun buttonClick(buffer: FloatArray, random: Random) {
        val n = buffer.size
        for (i in 0 until n) {
            val t = i.toFloat() / n
            val time = i.toFloat() / SAMPLE_RATE
            val click = (random.nextFloat() * 2f - 1f) * exp(-t * 26f) * 0.5f
            val blip = sin(2f * PI.toFloat() * 1_100f * time) * exp(-t * 14f) * 0.4f
            buffer[i] = click + blip
        }
    }

    // ------------------------------------------------------------ utilities

    private fun toShorts(buffer: FloatArray, gain: Float): ShortArray {
        val out = ShortArray(buffer.size)
        var peak = 0f
        for (value in buffer) peak = maxOf(peak, kotlin.math.abs(value))
        val normalise = if (peak > 0.001f) (0.95f / peak) else 0f
        for (i in buffer.indices) {
            val scaled = (buffer[i] * normalise * gain).coerceIn(-1f, 1f)
            out[i] = (scaled * Short.MAX_VALUE).toInt().toShort()
        }
        // Short fade out to avoid a click at the end of the buffer.
        val fade = minOf(256, out.size)
        for (i in 0 until fade) {
            val factor = 1f - i.toFloat() / fade
            val index = out.size - 1 - i
            out[index] = (out[index] * factor).toInt().toShort()
        }
        return out
    }
}
