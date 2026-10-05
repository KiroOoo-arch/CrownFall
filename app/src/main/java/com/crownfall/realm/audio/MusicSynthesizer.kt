package com.crownfall.realm.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * A short, seamless medieval-sounding loop (A minor: Am - F - C - G) built from
 * plucked-string notes over a soft drone. Generated at runtime so the project
 * ships without any audio binaries.
 */
object MusicSynthesizer {

    const val SAMPLE_RATE = 44_100
    private const val CHORD_SECONDS = 2.0f

    // MIDI note numbers for each chord: root, third, fifth, plus a bass note.
    private val PROGRESSION = listOf(
        intArrayOf(57, 60, 64, 45), // A minor
        intArrayOf(53, 57, 60, 41), // F major
        intArrayOf(48, 52, 55, 36), // C major
        intArrayOf(55, 59, 62, 43)  // G major
    )

    private fun frequency(midi: Int): Float = 440f * Math.pow(2.0, (midi - 69) / 12.0).toFloat()

    fun loop(gain: Float = 0.5f): ShortArray {
        val samplesPerChord = (SAMPLE_RATE * CHORD_SECONDS).toInt()
        val total = samplesPerChord * PROGRESSION.size
        val buffer = FloatArray(total)

        for ((index, chord) in PROGRESSION.withIndex()) {
            val offset = index * samplesPerChord
            // Bass drone for the whole chord.
            val bass = frequency(chord[3])
            for (i in 0 until samplesPerChord) {
                val t = i.toFloat() / SAMPLE_RATE
                val envelope = 0.35f * (exp(-t * 0.7f) + 0.35f)
                buffer[offset + i] += sin(2f * PI.toFloat() * bass * t) * envelope * 0.5f
                buffer[offset + i] += sin(2f * PI.toFloat() * bass * 2f * t) * envelope * 0.12f
            }
            // Arpeggiated plucks.
            val pluckNotes = listOf(chord[0], chord[1], chord[2], chord[1])
            for ((pluckIndex, note) in pluckNotes.withIndex()) {
                val start = (pluckIndex * (samplesPerChord / pluckNotes.size).toFloat()).toInt()
                val freq = frequency(note)
                val length = samplesPerChord - start
                for (i in 0 until length) {
                    val t = i.toFloat() / SAMPLE_RATE
                    val decay = exp(-t * 3.4f)
                    val tone = sin(2f * PI.toFloat() * freq * t) * 0.55f +
                        sin(2f * PI.toFloat() * freq * 2f * t) * 0.20f +
                        sin(2f * PI.toFloat() * freq * 3f * t) * 0.08f
                    buffer[start + i] += tone * decay * 0.28f
                }
            }
        }

        // Normalise and cross-fade the loop seam so it does not click.
        var peak = 0f
        for (value in buffer) peak = maxOf(peak, kotlin.math.abs(value))
        val scale = if (peak > 0.001f) 0.92f / peak else 0f

        val out = ShortArray(total)
        val seam = SAMPLE_RATE / 12
        for (i in buffer.indices) {
            var value = buffer[i] * scale * gain
            if (i < seam) {
                val head = (buffer[total - seam + i] * scale * gain) * (1f - i.toFloat() / seam)
                value = value * (i.toFloat() / seam) + head
            }
            out[i] = (value.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
