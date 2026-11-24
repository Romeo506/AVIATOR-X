package com.aviatorx.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AviatorUtilsTest {
    @Test
    fun parsesHeaderCsvAndIgnoresInvalidRows() {
        val data = "multiplier\n1.25x\n2.50\ninvalid\n0.80\n"
        assertEquals(listOf(1.25, 2.50), parseRounds(data))
    }

    @Test
    fun parsesPlainSeparatedValues() {
        assertEquals(listOf(1.1, 2.2, 3.3), parseRounds("1.1,2.2;3.3"))
    }

    @Test
    fun calculatesMedianForOddAndEvenLists() {
        assertEquals(2.0, median(listOf(1.0, 2.0, 3.0)), 0.0)
        assertEquals(2.5, median(listOf(1.0, 2.0, 3.0, 4.0)), 0.0)
    }

    @Test
    fun countsOnlyTrailingLowRounds() {
        assertEquals(2, currentLowStreak(listOf(1.2, 1.8, 2.1, 1.1, 1.9)))
        assertEquals(0, currentLowStreak(listOf(1.2, 2.0, 4.0)))
    }

    @Test
    fun providerProfilesUseDifferentStorageKeys() {
        assertTrue(providerStorageKey("Spribe Aviator") != providerStorageKey("SmartSoft JetX"))
        assertTrue(providerStorageKey("SmartSoft JetX") != providerStorageKey("Generic Crash Game"))
    }

    @Test
    fun sha256MatchesKnownDigest() {
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            sha256("hello")
        )
        assertTrue(sha256("hello") != sha256("world"))
    }
}
