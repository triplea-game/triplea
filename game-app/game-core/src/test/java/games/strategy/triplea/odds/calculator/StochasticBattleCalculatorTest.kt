package games.strategy.triplea.odds.calculator

import games.strategy.engine.data.TerritoryEffect
import games.strategy.triplea.delegate.GameDataTestUtil
import games.strategy.triplea.odds.calculator.DistributionTest.Companion.key
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.Companion.dec
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.Companion.isSortedBy
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.Companion.overkill
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.StateDistribution
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.WhenCallBackup.OnlyWhenNecessary
import games.strategy.triplea.odds.calculator.StochasticResultTest.Companion.calculate
import games.strategy.triplea.util.TuvUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.triplea.debug.LoggerManager
import util.TestData
import util.TestData.Companion.threeOnThree
import util.TestData.Companion.twoOnOne
import util.almostEquals
import util.unitsOfType
import java.time.Instant
import java.util.logging.Level
import games.strategy.engine.data.Unit as GameUnit

internal class StochasticBattleCalculatorTest {
    @BeforeEach
    fun setup() {
        LoggerManager.setLogLevel(Level.ALL)
    }

    @Test
    fun coreSetup() {
        val testData = threeOnThree()

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        val location = "Western Germany"
        val core = s.Core(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            ArrayList<GameUnit>(),
            testData.players[0].units,
            testData.players[1].units,
            GameDataTestUtil.territory(location, testData.gameData),
            ArrayList<TerritoryEffect>()
        )

        core.setupInitialState()

        val attackers = core.attackerRecs
        assertEquals("marine", attackers[0].unit!!.type.name)
        assertEquals("marine", attackers[1].unit!!.type.name)
        assertEquals("marine", attackers[2].unit!!.type.name)

        val attackersHitPower = attackers[0].unit!!.unitAttachment.getAttack(testData.players[0].gamePlayer)
        val chanceHit = attackersHitPower / 6.0
        val chanceMissed = 1.0 - chanceHit
        val attackersHitDistribution = core.attackerHitDist.last()
        assertTrue(chanceMissed * chanceMissed * chanceMissed almostEquals attackersHitDistribution[0])
        assertTrue(3 * chanceHit * chanceMissed * chanceMissed almostEquals attackersHitDistribution[1])
        assertTrue(3 * chanceHit * chanceHit * chanceMissed almostEquals attackersHitDistribution[2])
        assertTrue(chanceHit * chanceHit * chanceHit almostEquals attackersHitDistribution[3])

        val defenders = core.defenderRecs
        assertEquals("armour", defenders[0].unit!!.type.name)
        assertEquals("infantry", defenders[1].unit!!.type.name)
        assertEquals("infantry", defenders[2].unit!!.type.name)

        val armourHitPower = defenders[0].unit!!.unitAttachment.getDefense(testData.players[1].gamePlayer)
        val armourHit = armourHitPower / 6.0
        val armourMissed = 1.0 - armourHit

        val infantryHitPower = defenders[1].unit!!.unitAttachment.getDefense(testData.players[1].gamePlayer)
        val infantryHit = infantryHitPower / 6.0
        val infantryMissed = 1.0 - infantryHit

        val defendersHitDistribution = core.defenderHitDist.last()
        assertTrue(armourMissed * infantryMissed * infantryMissed almostEquals defendersHitDistribution[0])
        assertTrue(armourHit * infantryMissed * infantryMissed + 2 * infantryHit * armourMissed * infantryMissed almostEquals
                defendersHitDistribution[1])
        assertTrue(armourMissed * infantryHit * infantryHit + 2 * infantryMissed * armourHit * infantryHit almostEquals
                defendersHitDistribution[2])
        assertTrue(armourHit * infantryHit * infantryHit almostEquals defendersHitDistribution[3])
    }

    @Test
    fun simpleCalculation() {
        val location = "Western Germany"
        val testData = twoOnOne()

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        s.calculate(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            GameDataTestUtil.territory(location, testData.gameData),
            testData.players[0].units,
            testData.players[1].units,
            ArrayList<GameUnit>(),
            ArrayList<TerritoryEffect>(),
            false,
            16
        )

        // TODO complete test by asserting requirements
    }

    @Test
    fun unitsWithMoreThanOneHitpoint() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Carthage",
            (1 unitsOfType "warelephant"),
        ).apply {
            add(
                "Rome",
                (2 unitsOfType "legionary"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        val res = s.calculate(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            GameDataTestUtil.territory("Barcino", testData.gameData),
            testData.players[0].units,
            testData.players[1].units,
            ArrayList<GameUnit>(),
            ArrayList<TerritoryEffect>(),
            false,
            16
        )

        assertTrue(res.attackerWinPercent > res.defenderWinPercent,
            "a hitpower 4 unit with 2 hitpoits should be better than two hitpower 2 units with 1 hitpoint each")
    }


    @Test
    fun infrastructure() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Carthage",
            (1 unitsOfType "swordman"),
            (2 unitsOfType "spearman"),
        ).apply {
            add(
                "barbarians",
                (1 unitsOfType "swordman"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        val res = s.calculate(testData,"Barcino")

        testData.add("barbarians", (1 unitsOfType "territory"),)

        val resWithTerritory = s.calculate(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            GameDataTestUtil.territory("Barcino", testData.gameData),
            testData.players[0].units,
            testData.players[1].units,
            ArrayList<GameUnit>(),
            ArrayList<TerritoryEffect>(),
            false,
            16
        )

        assertEquals(res.attackerWinPercent, resWithTerritory.attackerWinPercent,
            "territory unit must not influence battle result")
    }

    @Test
    fun getMaxHitDist() {
        val testData = TestData(
            "big_world\\map\\games\\Big_World_1942_v3rules.xml",
            "Russians",
            (1 unitsOfType "artillery"),
            (2 unitsOfType "infantry"),
        ).apply {
            add(
                "Germans",
                (1 unitsOfType "artillery"),
                (2 unitsOfType "infantry"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        val location = GameDataTestUtil.territory("Western Germany", testData.gameData)
        val core = s.Core(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            ArrayList<GameUnit>(),
            testData.players[0].units,
            testData.players[1].units,
            location,
            ArrayList<TerritoryEffect>(),
        )

        val attackerMaxHitDist = core.setHitRecsAndReturnMaxHitDist(true, false)
        assertEquals("221", attackerMaxHitDist.toString())

        val defenderMaxHitDist = core.setHitRecsAndReturnMaxHitDist(false, false)
        assertEquals("222", defenderMaxHitDist.toString())
    }

    @Test
    fun averageBattleRoundsFought() {
        val testData = TestData(
            "big_world\\map\\games\\Big_World_1942_v3rules.xml",
            "Americans",
            (1 unitsOfType "armour")
        ).apply {
            add("Germans",
                (1 unitsOfType "armour"))
        }


        val location = GameDataTestUtil.territory("Western Germany", testData.gameData)

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        val res = s.calculate(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            location,
            testData.players[0].units,
            testData.players[1].units,
            ArrayList<GameUnit>(),
            ArrayList<TerritoryEffect>(),
            false,
            16
        )

/*
round	p end	p more	abs. p end	abs p more		contribution
    1	0,75	0,25	0,75	    0,25		    0,75
    2	0,75	0,25	0,1875	    0,0625		    0,375
    3	0,75	0,25	0,046875	0,015625		0,140625
    4	0,75	0,25	0,01171875	0,00390625		0,046875
    5						                        0,01953125
					                            sum	1,33203125

 */

        assertTrue(1.33203125
                almostEquals
                res.averageBattleRoundsFought)
    }

    @Test
    fun navalBattle() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "quinquereme"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "quinquereme"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        var res: AggregateResults? = null

        assertDoesNotThrow { res = s.calculate(testData, "44") as StochasticResult }
        assertTrue(1.0/3 almostEquals (res?.defenderWinPercent ?: .0) )
    }

    @Test
    fun navalBattleWithCargo() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "quinquereme"),
            territoryName = "44"
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "quinquereme"),
                (1 unitsOfType "legionary"),
            )
        }

        testData.players[1].units[1].transportedBy = testData.players[1].units[0]

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData) as StochasticResult

        val defenderCostsForTuv = TuvUtils.getCostsForTuv(testData.players[1].gamePlayer, testData.gameData)
        val legionary = testData.players[1].units[1]
        val tuvLegionary = defenderCostsForTuv.getInt(legionary.type)

        assertTrue( res.averageTuvSwingOfBattle almostEquals tuvLegionary * (res.drawPercent + res.defenderWinPercent) )
    }


    @Test
    fun navalBattleWithoutNavalAttackers() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "horseman"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "quinquereme"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val location = GameDataTestUtil.territory("44", testData.gameData)
        location.unitCollection.addAll(testData.players[0].units)
        location.unitCollection.addAll(testData.players[1].units)
        val res = s.calculate(testData, location) as StochasticResult

        assertEquals( 1.0, res.defenderWinPercent)
    }

    @Test
    fun navalBattleWithoutNavalAttackersAndWithoutDefenders() {
        val testData = TestData(
            "big_world\\map\\games\\Big_World_1942_v3rules.xml",
            "Americans",
            (1 unitsOfType "transport"),
            territoryName = "SZ 36 East Pacific"
        ).apply {
            add(
                "Japanese",
                (1 unitsOfType "transport"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData) as StochasticResult

        assertEquals( 1.0, res.drawPercent)
    }

    @Test
    fun overkillOneDistribution() {
        assertEquals(0, RobustDoubleArray(0).overkill.size)
        assertEquals(0, RobustDoubleArray(1).overkill.size)
        assertEquals(.7, RobustDoubleArray(doubleArrayOf(.3, .7)).overkill[0])

        with(RobustDoubleArray(doubleArrayOf(.3, .4, .3))) {
            assertEquals(.7, overkill[0])
            assertEquals(.3, overkill[1])
        }
    }

    @Test
    fun setupInitialState() {
        val location = "Western Germany"
        val testData = twoOnOne()

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        val s = StochasticBattleCalculator()
        s.gameData = testData.gameData

        val core = s.Core(
            testData.players[0].gamePlayer,
            testData.players[1].gamePlayer,
            ArrayList<GameUnit>(),
            testData.players[0].units,
            testData.players[1].units,
            GameDataTestUtil.territory(location, testData.gameData),
            ArrayList<TerritoryEffect>()
        )

        core.setupInitialState()
        with(core) {
            with(attackerOverkill.last()) {
                assertEquals(2, size)
                assertEquals(.75, get(0))
                assertEquals(.25, get(1))
            }

            with(defenderOverkill.last()) {
                assertEquals(1, size)
                assertEquals(.5, get(0))
            }
        }
    }

    @Test
    fun getHitDistributions() {
        val hitDistributions = StochasticBattleCalculator.getHitDistributions(3, Distribution[key("332")])
        assertEquals(4, hitDistributions.size)
        assertEquals(Distribution[0], hitDistributions[0])
        assertEquals(Distribution[key("3")], hitDistributions[1])
        assertEquals(Distribution[key("33")], hitDistributions[2])
        assertEquals(Distribution[key("332")], hitDistributions[3])

        val hitDistributions2 = StochasticBattleCalculator.getHitDistributions(2, Distribution[key("30")])
        assertEquals(3, hitDistributions2.size)
        val hitDistributions22 = hitDistributions2[2]
        assertEquals(Distribution[key("3")], hitDistributions22)
        val hitDistributions21 = hitDistributions2[1]
        assertEquals(hitDistributions22, hitDistributions21)
        assertEquals(Distribution[0], hitDistributions2[0])

        val hitDistributions3 = StochasticBattleCalculator.getHitDistributions(4, Distribution[key("3320")])
        assertEquals(5, hitDistributions3.size)
        assertEquals(Distribution[0], hitDistributions3[0])
        assertEquals(hitDistributions3[0][4], hitDistributions3[0][3])
        for (i in 0..hitDistributions3.lastIndex - 1)
            assertEquals(hitDistributions[i], hitDistributions3[i])
    }

    @Test
    fun isSortedBy() {
        assertTrue(intArrayOf(1, 2, 3).toList() isSortedBy { it })
        assertTrue(intArrayOf(3, 2, 1).toList() isSortedBy { -it })
        assertFalse(intArrayOf(1, 2, 1).toList() isSortedBy { -it })

        assertTrue(intArrayOf().toList() isSortedBy { it })
        assertTrue(intArrayOf(1, 1).toList() isSortedBy { it })

        val arrayListWithNull = ArrayList<Int?>().apply { add(null) }
        assertTrue(arrayListWithNull isSortedBy { it })
        arrayListWithNull.add(1)
        assertTrue(arrayListWithNull isSortedBy { it })
        arrayListWithNull.add(null)
        assertFalse(arrayListWithNull isSortedBy { it })
        arrayListWithNull.clear()
        arrayListWithNull.add(null)
        arrayListWithNull.add(-1)
        assertTrue(arrayListWithNull isSortedBy { it })
        arrayListWithNull.clear()
        arrayListWithNull.add(null)
        arrayListWithNull.add(null)
        assertTrue(arrayListWithNull isSortedBy { it })
    }

    @Test
    fun overkillDistributionArray() {
        val hitDistributions = StochasticBattleCalculator.getHitDistributions(3, Distribution[key("332")])
        val overkill = hitDistributions.overkill
        assertEquals(4, overkill.size)
        for ((i, d) in doubleArrayOf(10.0 / 12, 5.0 / 12, 1.0 / 12).withIndex())
            assertTrue(d almostEquals overkill[3][i])

        for ((i, d) in doubleArrayOf(3.0 / 4, 1.0 / 4).withIndex())
            assertTrue(d almostEquals overkill[2][i])

        for ((i, d) in doubleArrayOf(1.0 / 2).withIndex())
            assertTrue(d almostEquals overkill[1][i])

        assertTrue(overkill[0].isEmpty())
    }

    @Test
    fun noAttackers() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "legionary"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        var res: AggregateResults? = null

        org.junit.jupiter.api.assertDoesNotThrow { res = s.calculate(testData, "Barcino") as StochasticResult }
        assertEquals(1.0, res?.defenderWinPercent ?: .0 )
    }


    @Test
    fun noAttackersAndAllDefendersAreInfrastructure() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "wall"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        var res: AggregateResults? = null

        org.junit.jupiter.api.assertDoesNotThrow { res = s.calculate(testData, "Barcino") as StochasticResult }
        assertEquals(1.0, res?.defenderWinPercent ?: .0 )
    }
    @Test
    fun noDefenders() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "swordman"),
        ).apply {
            add(
                "Rome",
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        var res: AggregateResults? = null

        org.junit.jupiter.api.assertDoesNotThrow { res = s.calculate(testData, "Barcino") as StochasticResult }
        assertEquals(1.0, res?.attackerWinPercent ?: .0 )
    }

    @Test
    fun noAttackersNoDefendersIsDraw() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
        ).apply {
            add(
                "Rome",
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        var res: AggregateResults? = null

        org.junit.jupiter.api.assertDoesNotThrow { res = s.calculate(testData, "Barcino") as StochasticResult }
        assertEquals(1.0, res?.drawPercent ?: .0 )
    }

    @Test
    fun oneAttackerButAllDefendersAreInfrastructure() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "swordman"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "wall"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        val s = StochasticBattleCalculator()
        var res: AggregateResults? = null

        org.junit.jupiter.api.assertDoesNotThrow { res = s.calculate(testData, "Barcino") as StochasticResult }
        assertTrue(1.0 almostEquals  (res?.attackerWinPercent ?: .0) )
    }

    @Test
    fun gracefulFailWhenTooManyUnits() {
        val testData = TestData.nArmoursOnBothSides(256)

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()

        try {
            val res = s.calculate(testData,"Western Germany")
            assertFalse(res is StochasticResult)
        } catch (e: Exception) {
            assertTrue(false, "StochasticCalculator fails with exception $e")
        }
    }

    @Test
    fun bombard() {
        val testData = TestData.nArmoursOnBothSides(1)

        StochasticBattleCalculator.instrumentationMonitor = instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()

        val unitType = testData.gameData.unitTypeList.getUnitType("battleship")
        val battleship = unitType.createTemp(1, testData.players[0].gamePlayer)
        val res = s.calculate(testData,"Western Germany", battleship) as StochasticResult
        assertTrue(res.attackerWinPercent > res.defenderWinPercent)
    }

    companion object {
        val runtime = Runtime.getRuntime()

        var sd: StateDistribution? = null
        fun println() {
            println(sd)
        }

        val instrumentationMonitor = object : InstrumentationMonitor {
            override fun performanceReport(beforeMe: Instant, beforeBuddy: Instant, afterBuddy: Instant) {
                println("I took ${dec(beforeBuddy.toEpochMilli() - beforeMe.toEpochMilli())}ms and buddy took ${
                    dec(afterBuddy.toEpochMilli() - beforeBuddy.toEpochMilli())
                }")
            }

            override var text: String
                get() = ""
                set(value) {
                }

            override var stateDistribution: StateDistribution?
                get() = null
                set(value) {
                    sd = value
                    println(value)
                    println()
                }

        }
    }
}
