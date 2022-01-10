package games.strategy.triplea.odds.calculator

import games.strategy.triplea.odds.calculator.Distribution.Companion.countAsBitsInKey
import games.strategy.triplea.odds.calculator.Distribution.Companion.countOfUnitsWithHitPower
import games.strategy.triplea.odds.calculator.Distribution.Companion.diceSides
import games.strategy.triplea.odds.calculator.Distribution.Companion.dropLast
import games.strategy.triplea.odds.calculator.Distribution.Companion.hitPowerOfSingleUnit
import games.strategy.triplea.odds.calculator.Distribution.Companion.takeLast
import games.strategy.triplea.odds.calculator.Distribution.Companion.totalDiceCount
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.HitReceiver
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.HitReceiver.Companion.getKey
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import util.TestData
import kotlin.math.absoluteValue
import kotlin.math.pow

internal class DistributionTest {
    @Test
    fun binaryDistribution() {
        binaryDistribution(6)
//        binaryDistribution(12) TODO enable with 12 sided dice
    }

    @Suppress("SameParameterValue")
    private fun binaryDistribution(diceSides: Int) {
        Distribution.diceSides = diceSides
        for (i in 0..Distribution.diceSides) {
            val d = Distribution[countAsBitsInKey(1, i)]

            if (i == 0) {
                assertEquals(
                    1.0, d[0],
                    "probability for zero is wrong with dice $i/${Distribution.diceSides}",
                )
            } else {
                assertEquals(
                    i.toDouble() / Distribution.diceSides, d[1],
                    "probability for one is wrong with dice $i/${Distribution.diceSides}",
                )

                assertEquals(
                    1.0 - d[1], d[0],
                    "probability for zero is wrong with dice $i/${Distribution.diceSides}",
                )
            }
        }
    }

    @Test
    fun ternaryDistribution() {
        val d00 = Distribution[key("00")]

        assertEquals(1.0, d00[0])

        val d60 = Distribution[key("60")]
        assertEquals(0.0, d60[0])
        assertEquals(1.0, d60[1])

        val d66 = Distribution[key("66")]
        assertEquals(0.0, d66[0])
        assertEquals(0.0, d66[1])
        assertEquals(1.0, d66[2])

        val d33 = Distribution[key("33")]
        assertEquals(0.25, d33[0])
        assertEquals(0.50, d33[1])
        assertEquals(0.25, d33[2])

        with(Distribution[key("32")]) {
            assertTrue((1.0 / 3 - this[0]).absoluteValue < .000001)
            assertTrue((1.0 / 2 - this[1]).absoluteValue < .000001)
            assertTrue((1.0 / 6 - this[2]).absoluteValue < .000001)
        }
    }

    @Test
    fun precedingDistributions() {
        precedingDistributions("32")
        precedingDistributions("3320")
    }

    private fun precedingDistributions(convenientKey: String) {
        val d = Distribution[key(convenientKey)]
        assertTrue(Distribution.distributions[key(convenientKey)] == d)

        for (len in 1..convenientKey.lastIndex) {
            val subKey = convenientKey.substring(0, len)
            if (!subKey.contains('0'))
                assertTrue(Distribution.distributions.hashMap.containsKey(key(subKey))) { "no distribution \"$subKey\"" }
        }
    }


    @Test
    fun countAsBitsInKey() {
        assertEquals(0, countAsBitsInKey(1, 0))

        for (hitPower in 1..8)
            assertEquals((hitPower + 1) * 2.0.pow(8 * (hitPower - 1)).toLong(),
                countAsBitsInKey(hitPower + 1L, hitPower))
    }

    @Test
    fun list_getKey() {
        val testData = TestData.twoOnOne()
        val attacker = testData.players[0].gamePlayer
        val defender = testData.players[0].gamePlayer

        val attackerRecs = ArrayList<HitReceiver>()
        testData.players[0].units.forEach { attackerRecs.add(HitReceiver(it)) }

        val defenderRecs = ArrayList<HitReceiver>()
        testData.players[1].units.forEach { defenderRecs.add(HitReceiver(it)) }

        var key = defenderRecs.getKey { unitAttachment.getDefense(defender) }
        assertEquals(countAsBitsInKey(1, 3), key)

        key = attackerRecs.getKey { unitAttachment.getAttack(attacker) }
        assertEquals(countAsBitsInKey(2, 3), key)
    }

    @Test
    fun countOfUnitsWithHitPower() {
        for (hitPower in 1..8) {
            val key = countAsBitsInKey(hitPower + 1L, hitPower)
            assertEquals(hitPower + 1, key.countOfUnitsWithHitPower(hitPower))
        }
    }

    @Test
    fun getKey() {
        val counter = LongArray(8) { it.toLong() }

        val key = Distribution.getKey(counter)
        for (i in 0..counter.lastIndex) {
            assertEquals(i, key.countOfUnitsWithHitPower(i + 1))
        }
    }

    @Test
    fun numberOfUnits() {
        for (hitPower in 1..8) {
            val key = countAsBitsInKey(hitPower + 1L, hitPower)
            assertEquals(hitPower + 1, key.totalDiceCount)
        }
    }

    @Test
    fun hitPowerOfSingleUnit() {
        for (hitPower in 1..8) {
            val key = countAsBitsInKey(1, hitPower)
            assertEquals(hitPower, key.hitPowerOfSingleUnit)
        }

        assertThrows(IllegalStateException::class.java) { 0L.hitPowerOfSingleUnit }
        assertThrows(IllegalStateException::class.java) { 2L.hitPowerOfSingleUnit }
    }

    @Test
    fun dropLast() {
        for (hitPower in 1..8) {
            val key = countAsBitsInKey(hitPower + 1L, hitPower)
            assertEquals(hitPower, key.dropLast.totalDiceCount)
        }

        assertEquals(1L shl 8, ((1L shl 8) + 1).dropLast)
        assertEquals(((1L shl 8) + 1), ((1L shl 8) + 2).dropLast)
        assertEquals(0L, (1L shl 8).dropLast)
        assertEquals(0L, 0L.dropLast)
    }

    @Test
    fun takeLast() {
        for (hitPower in 1..8) {
            val key = countAsBitsInKey(hitPower + 1L, hitPower)
            assertEquals(1, key.takeLast.totalDiceCount)
            assertEquals(1, key.takeLast.countOfUnitsWithHitPower(hitPower))
        }

        assertEquals(1, ((1L shl 8) + 1).takeLast)
        assertEquals(1, ((1L shl 8) + 2).takeLast)
        assertEquals((1L shl 8), (1L shl 8).takeLast)

        assertThrows(IllegalStateException::class.java) { 0L.takeLast }
    }


    companion object {
        fun key(convenientKey: String): Long {
            val counter = LongArray(diceSides) // TODO support dice with more than 8 sides, e.g. 12
            for (hitPowerChar in convenientKey) {
                val hitPower = hitPowerChar - '0'

                if (hitPower > 0)
                    counter[hitPower - 1]++
            }
            return Distribution.getKey(counter)
        }
    }
}