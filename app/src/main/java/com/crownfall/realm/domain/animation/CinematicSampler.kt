package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.Move
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Samples a [CaptureAnimationController.CaptureCinematic] at a given elapsed
 * time and returns exactly what the renderer needs for that frame: both
 * fighters' positions and poses, the camera framing, particles and the sounds
 * that should fire on this frame.
 *
 * Because the sampler is a pure function of time, the animation can be paused,
 * skipped, replayed or unit tested without a single Android dependency.
 */
class CinematicSampler(
    private val cinematic: CaptureAnimationController.CaptureCinematic,
    private val move: Move
) {

    data class Frame(
        val attackerFile: Float,
        val attackerRank: Float,
        val attackerPose: PiecePose,
        val attackerAlpha: Float,
        val defenderFile: Float,
        val defenderRank: Float,
        val defenderPose: PiecePose,
        val defenderAlpha: Float,
        val defenderPresent: Boolean,
        val cameraZoom: Float,
        val cameraShake: Float,
        val particles: List<ParticleSample>,
        val currentKind: AnimationKind,
        val label: String,
        val sounds: List<SoundEffect>
    ) {
        companion object {
            fun idle(file: Float, rank: Float) = Frame(
                attackerFile = file,
                attackerRank = rank,
                attackerPose = PiecePose.NEUTRAL,
                attackerAlpha = 1f,
                defenderFile = file,
                defenderRank = rank,
                defenderPose = PiecePose.NEUTRAL,
                defenderAlpha = 0f,
                defenderPresent = false,
                cameraZoom = 1f,
                cameraShake = 0f,
                particles = emptyList(),
                currentKind = AnimationKind.IDLE,
                label = "",
                sounds = emptyList()
            )
        }
    }

    val totalDurationMs: Int get() = cinematic.totalDurationMs

    /** True when the frames at [elapsedMs] should still draw the defender. */
    fun defenderPresentAt(elapsedMs: Long): Boolean =
        elapsedMs < cinematic.defenderStartOffsetMs + cinematic.defenderPhases.sumOf { it.durationMs }

    fun sample(elapsedMs: Long, previousElapsedMs: Long = -1L): Frame {
        val elapsed = elapsedMs.coerceAtLeast(0L)

        val attacker = sampleAttacker(elapsed)
        val defender = sampleDefender(elapsed)

        val impactWindow = cinematic.defenderStartOffsetMs.toLong()
        val shakeStrength = cinematic.cameraShake
        val shake = if (abs(elapsed - impactWindow) < 260L && shakeStrength > 0f) {
            shakeStrength * (1f - abs(elapsed - impactWindow) / 260f)
        } else {
            0f
        }

        val rampIn = 300f
        val rampOut = 320f
        val zoom = when {
            elapsed < rampIn -> 1f + (cinematic.cameraZoom - 1f) * (elapsed / rampIn)
            elapsed > totalDurationMs - rampOut && totalDurationMs > rampOut ->
                1f + (cinematic.cameraZoom - 1f) *
                    ((totalDurationMs - elapsed) / rampOut).coerceIn(0f, 1f)
            else -> cinematic.cameraZoom
        }

        val particles = buildParticles(elapsed)

        return Frame(
            attackerFile = attacker.file,
            attackerRank = attacker.rank,
            attackerPose = attacker.pose,
            attackerAlpha = 1f,
            defenderFile = defender.file,
            defenderRank = defender.rank,
            defenderPose = defender.pose,
            defenderAlpha = defender.alpha,
            defenderPresent = defender.present,
            cameraZoom = zoom,
            cameraShake = shake,
            particles = particles,
            currentKind = attacker.kind,
            label = attacker.label,
            sounds = soundsBetween(previousElapsedMs, elapsed)
        )
    }

    private data class Fighter(
        val file: Float,
        val rank: Float,
        val pose: PiecePose,
        val alpha: Float = 1f,
        val present: Boolean = true,
        val kind: AnimationKind = AnimationKind.IDLE,
        val label: String = ""
    )

    private fun sampleAttacker(elapsed: Long): Fighter {
        var cursor = 0L
        var file = move.from.file.toFloat()
        var rank = move.from.rank.toFloat()
        var pose = PiecePose.NEUTRAL
        var kind = AnimationKind.IDLE
        var label = ""
        for (phase in cinematic.attackerPhases) {
            val start = cursor
            val end = cursor + phase.durationMs
            if (elapsed >= end) {
                // Completed: apply its travel and keep the final pose.
                phase.travelToX?.let { file = it }
                phase.travelToY?.let { rank = it }
                pose = PoseMapper.poseFor(phase.kind, 1f)
                kind = phase.kind
                label = phase.label
                cursor = end
                continue
            }
            if (elapsed >= start) {
                val t = if (phase.durationMs <= 0) 1f
                else ((elapsed - start).toFloat() / phase.durationMs).coerceIn(0f, 1f)
                val eased = easeInOut(t)
                val startFile = file
                val startRank = rank
                phase.travelToX?.let { target ->
                    file = startFile + (target - startFile) * eased
                }
                phase.travelToY?.let { target ->
                    rank = startRank + (target - startRank) * eased
                }
                pose = PoseMapper.poseFor(phase.kind, t, 1f)
                kind = phase.kind
                label = phase.label
            }
            break
        }
        // If all phases are done, snap to the destination.
        if (elapsed >= cinematic.attackerPhases.sumOf { it.durationMs }) {
            file = move.to.file.toFloat()
            rank = move.to.rank.toFloat()
        }
        return Fighter(file, rank, pose, 1f, true, kind, label)
    }

    private fun sampleDefender(elapsed: Long): Fighter {
        val start = cinematic.defenderStartOffsetMs.toLong()
        val file = move.to.file.toFloat()
        val rank = move.to.rank.toFloat()
        if (cinematic.defenderPhases.isEmpty() || elapsed < start) {
            return Fighter(file, rank, PiecePose.NEUTRAL, 1f, present = true)
        }
        val total = cinematic.defenderPhases.sumOf { it.durationMs }
        if (elapsed >= start + total) {
            // Fully defeated and removed.
            return Fighter(file, rank, PiecePose(tiltDegrees = 80f), 0f, present = false)
        }
        var cursor = start
        var pose = PiecePose.NEUTRAL
        var alpha = 1f
        // Knock-back direction: away from the attacker.
        val dfx = (file - move.from.file).let { if (it == 0f) 0f else it / abs(it) }
        val dfy = (rank - move.from.rank).let { if (it == 0f) 1f else it / abs(it) }
        var offset = 0f
        var kind = AnimationKind.HIT_REACT
        for (phase in cinematic.defenderPhases) {
            val end = cursor + phase.durationMs
            if (elapsed >= end) {
                pose = PoseMapper.poseFor(phase.kind, 1f)
                if (phase.kind == AnimationKind.DEATH_DISSOLVE) alpha = 0f
                kind = phase.kind
                cursor = end
                continue
            }
            val t = if (phase.durationMs <= 0) 1f
            else ((elapsed - cursor).toFloat() / phase.durationMs).coerceIn(0f, 1f)
            pose = PoseMapper.poseFor(phase.kind, t)
            kind = phase.kind
            when (phase.kind) {
                AnimationKind.HIT_REACT -> Unit
                AnimationKind.DEATH_FALL, AnimationKind.DEATH_KNOCKBACK -> {
                    val kb = cinematic.defenderPhases.firstOrNull { it.travelToX != null }
                    val distance = kb?.travelToX ?: 0.35f
                    offset = distance * t
                }
                AnimationKind.DEATH_DISSOLVE -> alpha = 1f - t
                else -> Unit
            }
            break
        }
        return Fighter(
            file = file + dfx * offset,
            rank = rank + dfy * offset,
            pose = pose,
            alpha = alpha.coerceIn(0f, 1f),
            present = true,
            kind = kind
        )
    }

    private fun buildParticles(elapsed: Long): List<ParticleSample> {
        val out = ArrayList<ParticleSample>(2)
        // Attacker burst during its strike phase.
        phaseAt(cinematic.attackerPhases, elapsed)?.let { (phase, t) ->
            if (phase.particles != ParticleStyle.NONE && t > 0.25f) {
                out.add(
                    ParticleSample(
                        file = cinematic.defenderSquare.file.toFloat(),
                        rank = cinematic.defenderSquare.rank.toFloat(),
                        style = phase.particles,
                        progress = ((t - 0.25f) / 0.75f).coerceIn(0f, 1f),
                        intensity = 1f
                    )
                )
            }
        }
        // Defender's own effect during defeat.
        val defenderElapsed = elapsed - cinematic.defenderStartOffsetMs
        if (defenderElapsed >= 0) {
            phaseAt(cinematic.defenderPhases, defenderElapsed)?.let { (phase, t) ->
                if (phase.particles != ParticleStyle.NONE) {
                    out.add(
                        ParticleSample(
                            file = cinematic.defenderSquare.file.toFloat(),
                            rank = cinematic.defenderSquare.rank.toFloat(),
                            style = phase.particles,
                            progress = t,
                            intensity = 0.85f
                        )
                    )
                }
            }
        }
        return out
    }

    /** Which phase is active at [elapsed] plus its normalised progress. */
    private fun phaseAt(phases: List<AnimationPhase>, elapsed: Long): Pair<AnimationPhase, Float>? {
        var cursor = 0L
        for (phase in phases) {
            val end = cursor + phase.durationMs
            if (elapsed < end) {
                val t = if (phase.durationMs <= 0) 1f
                else ((elapsed - cursor).toFloat() / phase.durationMs).coerceIn(0f, 1f)
                return phase to t
            }
            cursor = end
        }
        return null
    }

    private fun soundsBetween(previous: Long, current: Long): List<SoundEffect> {
        if (previous < 0) return emptyList()
        val out = ArrayList<SoundEffect>(2)
        fun scan(phases: List<AnimationPhase>, offset: Long) {
            var cursor = offset
            for (phase in phases) {
                val end = cursor + phase.durationMs
                phase.sound?.let { sound ->
                    if (cursor >= previous && cursor < current) out.add(sound)
                }
                cursor = end
            }
        }
        scan(cinematic.attackerPhases, 0L)
        scan(cinematic.defenderPhases, cinematic.defenderStartOffsetMs.toLong())
        return out
    }

    private fun easeInOut(t: Float): Float =
        if (t < 0.5f) 2f * t * t else 1f - 2f * (1f - t) * (1f - t)

    /** Progress helper used by the HUD progress rail. */
    fun progressAt(elapsedMs: Long): Float =
        (elapsedMs.toFloat() / max(1, totalDurationMs)).coerceIn(0f, 1f)

    companion object {
        /** Chooses a sensible zoom for the given cinematic, clamped for phones. */
        fun cameraZoomFor(cinematic: CaptureAnimationController.CaptureCinematic): Float =
            min(1.6f, max(1f, cinematic.cameraZoom))
    }
}
