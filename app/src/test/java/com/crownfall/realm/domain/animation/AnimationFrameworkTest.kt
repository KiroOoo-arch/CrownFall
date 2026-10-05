package com.crownfall.realm.domain.animation

import com.crownfall.realm.domain.audio.SoundEffect
import com.crownfall.realm.domain.chess.Faction
import com.crownfall.realm.domain.chess.Move
import com.crownfall.realm.domain.chess.Piece
import com.crownfall.realm.domain.chess.PieceType
import com.crownfall.realm.domain.chess.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationFrameworkTest {

    private val controller = CaptureAnimationController()
    private val pieceController = PieceAnimationController(controller)

    private fun square(name: String): Square = requireNotNull(Square.fromAlgebraic(name))

    private fun captureMove(
        from: String = "e2",
        to: String = "e4",
        attacker: PieceType = PieceType.PAWN,
        victim: PieceType = PieceType.KNIGHT
    ) = Move(
        from = square(from),
        to = square(to),
        piece = Piece(attacker, Faction.DAWN),
        captured = Piece(victim, Faction.DUSK)
    )

    // ------------------------------------------------------- combat enabled

    @Test
    fun captureCinematicHasBothFightersAndAPlannedCamera() {
        val cinematic = controller.planCapture(captureMove(), speedMultiplier = 1f, combatEnabled = true)

        assertFalse(cinematic.abbreviated)
        assertEquals(3, cinematic.attackerPhases.size)
        assertEquals(3, cinematic.defenderPhases.size)
        assertTrue("camera should push in", cinematic.cameraZoom > 1f)
        assertTrue("a duel should shake the camera", cinematic.cameraShake > 0f)
        assertTrue(cinematic.skippable)
        assertTrue(
            "the target must react after the strike begins",
            cinematic.defenderStartOffsetMs > 0
        )
        assertTrue(
            "the whole cinematic must last long enough to read",
            cinematic.totalDurationMs >= 500
        )
        assertEquals(PieceType.KNIGHT, cinematic.defenderType)
    }

    @Test
    fun cameraShakeBiasRankingsMatchThePieceIdentities() {
        val knight = PieceAnimationProfiles.of(PieceType.KNIGHT)
        val rook = PieceAnimationProfiles.of(PieceType.ROOK)
        val pawn = PieceAnimationProfiles.of(PieceType.PAWN)

        assertTrue(knight.cameraShakeBias > pawn.cameraShakeBias)
        assertTrue(rook.cameraShakeBias > pawn.cameraShakeBias)
        assertTrue(knight.movement.perSquareMs < pawn.movement.perSquareMs)
        assertEquals(PieceAnimationProfiles.mostCinematic.type, PieceType.KNIGHT)
    }

    @Test
    fun everyPieceHasItsOwnAttackAndDeathStyle() {
        val attackKinds = PieceType.entries.map { type -> pieceController.profile(type).attack.kind }
        assertEquals("each role attacks differently", attackKinds.size, attackKinds.distinct().size)

        val deathKinds = PieceType.entries.map { type -> pieceController.profile(type).death.kind }
        assertTrue("at least three distinct death styles", deathKinds.distinct().size >= 3)

        val identities = PieceType.entries.map { type -> pieceController.profile(type).identity }
        assertEquals(identities.size, identities.distinct().size)
    }

    @Test
    fun animationSpeedChangesTheDuration() {
        val slowCinematic = controller.planCapture(captureMove(), speedMultiplier = 1.6f, combatEnabled = true)
        val fastCinematic = controller.planCapture(captureMove(), speedMultiplier = 0.55f, combatEnabled = true)

        assertTrue(fastCinematic.totalDurationMs < slowCinematic.totalDurationMs)

        val slowMove = controller.planMovement(captureMove(), speedMultiplier = 1.6f, combatEnabled = true)
        val fastMove = controller.planMovement(captureMove(), speedMultiplier = 0.55f, combatEnabled = true)
        val slowTotal = slowMove.sumOf { it.durationMs }
        val fastTotal = fastMove.sumOf { it.durationMs }
        assertTrue(fastTotal < slowTotal)
    }

    // ------------------------------------------------------ combat disabled

    @Test
    fun disablingCombatAnimationGivesAShortAbbreviatedSlide() {
        val cinematic = controller.planCapture(captureMove(), speedMultiplier = 1f, combatEnabled = false)

        assertTrue(cinematic.abbreviated)
        assertEquals(ParticleStyle.NONE, cinematic.particleStyle)
        assertEquals(0f, cinematic.cameraShake, 0.0001f)
        assertTrue(
            "an abbreviated capture must finish fast",
            cinematic.totalDurationMs <= 400
        )
        assertTrue(cinematic.attackerPhases.any { it.travelToX != null })
    }

    // ---------------------------------------------------------- movement

    @Test
    fun movementPacingGrowsWithDistance() {
        val animation = pieceController.movement(PieceType.PAWN)
        assertTrue(animation.durationMs(4, 1f) > animation.durationMs(1, 1f))
        assertEquals(
            "non-combat pacing still scales with speed",
            true,
            animation.durationMs(2, 0.55f) < animation.durationMs(2, 1.6f)
        )
    }

    @Test
    fun promotionProducesAThreeBeatTransform() {
        val phases = controller.planPromotion(PieceType.QUEEN)
        assertEquals(3, phases.size)
        assertTrue(phases.all { it.kind == AnimationKind.PROMOTION })
        assertTrue(phases[1].label.contains(PieceType.QUEEN.medievalName))
        assertTrue(phases.any { it.sound == SoundEffect.PROMOTION })
    }

    @Test
    fun movementWithPromotionAppendsTheFlourish() {
        val move = Move(
            from = square("a7"),
            to = square("a8"),
            piece = Piece(PieceType.PAWN, Faction.DAWN),
            promotion = PieceType.QUEEN
        )
        val phases = controller.planMovement(move, 1f, combatEnabled = true)
        assertTrue(phases.any { it.kind == AnimationKind.PROMOTION })
    }

    // ------------------------------------------------------------ sampler

    @Test
    fun samplerMovesTheAttackerFromItsSquareToTheTarget() {
        val move = captureMove("e2", "e4")
        val cinematic = controller.planCapture(move, 1f, true)
        val sampler = CinematicSampler(cinematic, move)

        val first = sampler.sample(0L, -1L)
        assertEquals(4f, first.attackerFile, 0.001f)
        assertEquals(1f, first.attackerRank, 0.001f)

        val last = sampler.sample(sampler.totalDurationMs.toLong(), 0L)
        assertEquals(4f, last.attackerFile, 0.001f)
        assertEquals(3f, last.attackerRank, 0.001f)
    }

    @Test
    fun samplerRemovesTheDefeatedPieceOnlyAtTheEnd() {
        val move = captureMove()
        val cinematic = controller.planCapture(move, 1f, true)
        val sampler = CinematicSampler(cinematic, move)

        val start = sampler.sample(0L, -1L)
        assertTrue("the target is alive at the start", start.defenderPresent)
        assertEquals(1f, start.defenderAlpha, 0.001f)

        val mid = sampler.sample(cinematic.defenderStartOffsetMs.toLong() + 40L, -1L)
        assertTrue("the target is still reacting mid-cinematic", mid.defenderPresent)
        assertTrue("the target is still visible mid-cinematic", mid.defenderAlpha > 0f)

        val end = sampler.sample(sampler.totalDurationMs.toLong(), -1L)
        assertFalse("the target is gone once the cinematic ends", end.defenderPresent)
        assertEquals(0f, end.defenderAlpha, 0.001f)
    }

    @Test
    fun samplerFiresEachSoundOnceInItsWindow() {
        // Pawn attacker (steel) against a cleric defender (holy magic): three
        // distinct sounds that must each fire exactly once.
        val move = captureMove("e2", "e4", PieceType.PAWN, PieceType.BISHOP)
        val cinematic = controller.planCapture(move, 1f, true)
        val sampler = CinematicSampler(cinematic, move)

        val collected = ArrayList<SoundEffect>()
        var previous = -1L
        var elapsed = 0L
        while (elapsed <= sampler.totalDurationMs) {
            collected.addAll(sampler.sample(elapsed, previous).sounds)
            previous = elapsed
            elapsed += 16L
        }
        assertTrue(
            "the swing and the impact must both be heard",
            collected.contains(SoundEffect.SWORD_SWING)
        )
        assertTrue(collected.contains(SoundEffect.SWORD_IMPACT))
        assertEquals(
            "each sound is triggered exactly once",
            collected.size,
            collected.distinct().size
        )
    }

    @Test
    fun firstSampleNeverReplaysEarlierSounds() {
        val move = captureMove()
        val cinematic = controller.planCapture(move, 1f, true)
        val sampler = CinematicSampler(cinematic, move)
        assertTrue(sampler.sample(400L, -1L).sounds.isEmpty())
    }

    @Test
    fun samplerClampsCameraZoomToSomethingUsableOnAPhone() {
        val cinematic = controller.planCapture(captureMove(), 1f, true)
        val zoom = CinematicSampler.cameraZoomFor(cinematic)
        assertTrue(zoom in 1f..1.6f)
        assertTrue(samplerProgress(cinematic) in 0f..1f)
    }

    private fun samplerProgress(cinematic: CaptureAnimationController.CaptureCinematic): Float {
        val move = captureMove()
        val sampler = CinematicSampler(cinematic, move)
        return sampler.progressAt(sampler.totalDurationMs.toLong() / 2)
    }

    // --------------------------------------------------------- idle motion

    @Test
    fun idleMotionStaysWithinItsProfileBounds() {
        for (type in PieceType.entries) {
            val profile = pieceController.idleProfile(type)
            for (t in 0L..2_000L step 137L) {
                val transform = pieceController.idleTransform(type, t)
                assertTrue(transform.breatheScale in 0.9f..1.1f)
                assertTrue(kotlin.math.abs(transform.bobOffset) <= profile.bobAmplitude + 0.0001f)
                assertTrue(kotlin.math.abs(transform.swayOffset) <= profile.swayAmplitude + 0.0001f)
                assertTrue(transform.weaponRotation.isFinite())
            }
            assertTrue(pieceController.idleLoopMs(type) >= 400)
        }
    }

    @Test
    fun idleCyclesArePhaseShiftedPerPiece() {
        val atZero = PieceType.entries.map { pieceController.idleTransform(it, 0L).bobOffset }
        assertNotEquals(
            "the army should not breathe in unison",
            atZero.first(),
            atZero.last()
        )
    }

    @Test
    fun shakeOffsetIsZeroWithoutStrength() {
        val (x, y) = pieceController.shakeOffset(0f, 250L)
        assertEquals(0f, x, 0.0001f)
        assertEquals(0f, y, 0.0001f)
        val (sx, sy) = pieceController.shakeOffset(1f, 250L)
        assertTrue(kotlin.math.abs(sx) > 0f || kotlin.math.abs(sy) > 0f)
    }

    // -------------------------------------------------------------- poses

    @Test
    fun posesProgressMonotonicallyWithTheirPhase() {
        val early = PoseMapper.poseFor(AnimationKind.DRAW_WEAPON, 0.1f)
        val late = PoseMapper.poseFor(AnimationKind.DRAW_WEAPON, 0.9f)
        assertTrue(late.armRaise > early.armRaise)

        val falling = PoseMapper.poseFor(AnimationKind.DEATH_FALL, 1f)
        assertTrue(falling.tiltDegrees > 60f)

        val neutral = PoseMapper.poseFor(AnimationKind.IDLE, 0.5f)
        assertEquals(0f, neutral.tiltDegrees, 0.0001f)
        assertEquals(0f, neutral.armRaise, 0.0001f)
    }

    @Test
    fun everyAnimationKindHasAMappingWithoutThrowing() {
        for (kind in AnimationKind.entries) {
            val pose = PoseMapper.poseFor(kind, 0.5f)
            assertTrue(pose.tiltDegrees.isFinite())
            assertTrue(pose.armRaise.isFinite())
            assertTrue(pose.impactFlash.isFinite())
        }
    }
}
