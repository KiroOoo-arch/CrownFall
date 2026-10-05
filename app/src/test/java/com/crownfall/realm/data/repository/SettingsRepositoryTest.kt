package com.crownfall.realm.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository
    private lateinit var dao: FakeSettingsDao

    @Before
    fun setUp() = runBlocking {
        dao = FakeSettingsDao()
        repository = SettingsRepository(dao)
        repository.ensureDefaults()
    }

    @Test
    fun defaultsAreSeededOnce() = runBlocking {
        val defaults = repository.current()
        assertEquals(1, defaults.id)
        assertTrue(defaults.combatAnimation)
        assertTrue(defaults.combatSounds)
        assertEquals("NORMAL", defaults.animationSpeed)
        repository.ensureDefaults()
        assertEquals(1, dao.stored!!.id)
    }

    @Test
    fun volumesAreClampedToValidRange() = runBlocking {
        repository.setMusicVolume(3.5f)
        assertEquals(1f, repository.current().musicVolume, 0.0001f)
        repository.setSoundVolume(-2f)
        assertEquals(0f, repository.current().soundVolume, 0.0001f)
        repository.setSoundVolume(0.42f)
        assertEquals(0.42f, repository.current().soundVolume, 0.0001f)
    }

    @Test
    fun togglesPersist() = runBlocking {
        repository.setCombatAnimation(false)
        repository.setCombatSounds(false)
        assertFalse(repository.current().combatAnimation)
        assertFalse(repository.current().combatSounds)

        repository.setCombatAnimation(true)
        assertTrue(repository.current().combatAnimation)
    }

    @Test
    fun screenShakeDefaultsOnAndPersists() = runBlocking {
        assertTrue(repository.current().screenShake)
        repository.setScreenShake(false)
        assertFalse(repository.current().screenShake)
        repository.setScreenShake(true)
        assertTrue(repository.current().screenShake)
    }

    @Test
    fun animationSpeedRoundTripsAndDrivesTheMultiplier() = runBlocking {
        repository.setAnimationSpeed(AnimationSpeed.FAST)
        assertEquals(AnimationSpeed.FAST, repository.animationSpeed())
        assertEquals(0.55f, repository.animationSpeed().multiplier, 0.0001f)

        repository.setAnimationSpeed(AnimationSpeed.SLOW)
        assertEquals(1.6f, repository.animationSpeed().multiplier, 0.0001f)
        assertTrue(AnimationSpeed.SLOW.multiplier > AnimationSpeed.FAST.multiplier)
    }

    @Test
    fun unknownSpeedNameFallsBackToNormal() {
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromName("turbo"))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromName(null))
    }

    @Test
    fun resetRestoresDefaults() = runBlocking {
        repository.setCombatAnimation(false)
        repository.setDefaultDifficulty("MASTER")
        repository.reset()
        assertTrue(repository.current().combatAnimation)
        assertEquals("HARD", repository.current().defaultDifficulty)
    }
}
