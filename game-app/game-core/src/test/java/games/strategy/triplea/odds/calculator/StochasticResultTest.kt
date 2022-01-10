package games.strategy.triplea.odds.calculator

import ch.qos.logback.classic.Level
import games.strategy.engine.data.Territory
import games.strategy.engine.data.TerritoryEffect
import games.strategy.engine.data.Unit
import games.strategy.triplea.delegate.GameDataTestUtil
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.Companion.log
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.WhenCallBackup.OnlyWhenNecessary
import games.strategy.triplea.odds.calculator.StochasticResult.Companion.capturedBy
import games.strategy.triplea.odds.calculator.StochasticResult.Companion.from
import games.strategy.triplea.util.TuvUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import util.TestData
import util.unitsOfType

internal class StochasticResultTest {
    @BeforeEach
    fun setup() {
        val logToSetLevel = log as ch.qos.logback.classic.Logger
        logToSetLevel.level = Level.ALL
    }

    @Test
    fun takeOverTuv() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Carthage",
            (1 unitsOfType "warelephant"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "metropolis"),
            )
        }

        val attackerCostsForTuv = TuvUtils.getCostsForTuv(testData.players[0].gamePlayer, testData.gameData)
        val elephant = testData.players[0].units[0]
        val tuvElephant = attackerCostsForTuv.getInt(elephant.type)
        val metropolis = testData.players[1].units[0]
        val tuvMetropolis = attackerCostsForTuv.getInt(metropolis.type)

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData, "Barcino") as StochasticResult

        assertTrue(res.averageAttackingUnitsRemaining.contains { it.type.name == "metropolis" })
        assertEquals((tuvElephant + tuvMetropolis).toDouble(), res.attacker.averageTuvOfUnitsLeft)
    }

    @Test
    fun takeOverTuvOnlyIfWon() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "swordman"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "metropolis"),
                (1 unitsOfType "legionary"),
            )
        }

        val attackerCostsForTuv = TuvUtils.getCostsForTuv(testData.players[0].gamePlayer, testData.gameData)
        val swordman = testData.players[0].units[0]
        val tuvSwordman = attackerCostsForTuv.getInt(swordman.type)
        val metropolis = testData.players[1].units[0]
        val tuvMetropolis = attackerCostsForTuv.getInt(metropolis.type)

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData, "Barcino") as StochasticResult

        assertEquals((tuvSwordman + tuvMetropolis) * res.attackerWinPercent, res.attacker.averageTuvOfUnitsLeft)
    }

    @Test
    fun defenderResultIncludesInfrastructure() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Macedonia",
            (1 unitsOfType "swordman"),
        ).apply {
            add(
                "Rome",
                (10 unitsOfType "wall"),
                (1 unitsOfType "legionary"),
            )
        }

        val defenderCostsForTuv = TuvUtils.getCostsForTuv(testData.players[1].gamePlayer, testData.gameData)
        val wall = testData.players[1].units[0]
        val tuvWall = defenderCostsForTuv.getInt(wall.type)
        val legionary = testData.players[1].units[10]
        val tuvLegionary = defenderCostsForTuv.getInt(legionary.type)

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData, "Barcino") as StochasticResult

        assertTrue { res.averageDefendingUnitsRemaining.contains() { it.type.name == "wall" } }
        assertEquals(10 * tuvWall + tuvLegionary, res.defender.tuv)
    }

    @Test
    fun getAverageUnitsRemainingReturnsBestUnits() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Syria",
            (1 unitsOfType "horsearcher"),
            (1 unitsOfType "cataphract"),
        ).apply {
            add(
                "Rome",
                (2 unitsOfType "legionary"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        val s = StochasticBattleCalculator()
        var res = s.calculate(testData, "Barcino") as StochasticResult

        assertEquals(1, res.attacker.getAverageUnitsRemaining().size)
        assertEquals("cataphract", res.attacker.getAverageUnitsRemaining().first().type.name)

        with(testData.players[0].units) {
            this[0] = this[1].also { this[1] = this[0] }
        }

        res = s.calculate(testData, "Barcino") as StochasticResult

        assertEquals(1, res.attacker.getAverageUnitsRemaining().size)
        assertEquals("cataphract", res.attacker.getAverageUnitsRemaining().first().type.name)
    }

    @Test
    fun takeOverTuvOfFallenWall() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Carthage",
            (1 unitsOfType "warelephant"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "wall"),
                (1 unitsOfType "wall_fallen"),
            )
        }

        val attackerCostsForTuv = TuvUtils.getCostsForTuv(testData.players[0].gamePlayer, testData.gameData)
        val fallenWall = testData.players[1].units[1]
        val tuvFallenWall = attackerCostsForTuv.getInt(fallenWall.type)
        testData.players[1].units.remove(fallenWall)

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData, "Barcino") as StochasticResult

//        assertEquals(tuvFallenWall.toDouble(), res.attacker.takeOverTuv(res.defender))
        // TODO change into equivalent test w current code
    }

    @Test
    fun whenCapturedChangesInto() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Carthage",
            (1 unitsOfType "warelephant"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "territory"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData, "Barcino") as StochasticResult

        assertTrue { res.attacker.getAverageUnitsRemaining().contains { it.type.name == "conquest" } }
    }

    @Test
    fun changesInto() {
        val testData = TestData(
            "270bc_wars\\map\\games\\270BC_Wars.xml",
            "Carthage",
            (1 unitsOfType "warelephant"),
        ).apply {
            add(
                "Rome",
                (1 unitsOfType "legionary"),
                (1 unitsOfType "territory"),
            )
        }

        StochasticBattleCalculator.instrumentationMonitor = StochasticBattleCalculatorTest.instrumentationMonitor
        StochasticBattleCalculator.whenCallBackup = OnlyWhenNecessary
        val s = StochasticBattleCalculator()
        val res = s.calculate(testData, "Barcino") as StochasticResult

        val legionary = testData.players[1].units[0]
        val territory = testData.players[1].units[1]
        val attacker = testData.players[0].gamePlayer
        val defender = testData.players[1].gamePlayer
        val changeResult = (territory capturedBy attacker from defender).changesInto
        assertNotEquals(null, changeResult)
        assertEquals(2, changeResult!!.size)
        assertTrue { changeResult.contains { it.type.name == "territory" } }
        assertTrue { changeResult.contains { it.type.name == "conquest" } }

        assertEquals(null, (legionary capturedBy attacker from defender).changesInto)
    }

    companion object {
        // this is independent of the context and a candidate for a generic expansion method
        inline fun <T> Iterable<T>.contains(predicate: (T) -> Boolean) = find(predicate) != null

        fun StochasticBattleCalculator.calculate(
            testData: TestData,
            territoryName: String? = null,
            bombard: Collection<Unit> = ArrayList<Unit>(),
        ): AggregateResults {
            val territory = if (territoryName == null) {
                testData.territory!!
            } else GameDataTestUtil.territory(territoryName, testData.gameData)

            return calculate(testData, territory, bombard)
        }

        fun StochasticBattleCalculator.calculate(
            testData: TestData,
            territory: Territory,
            bombard: Collection<Unit> = ArrayList<Unit>(),
        ): AggregateResults {
            if (gameData != testData.gameData)
                gameData = testData.gameData

            return calculate(
                testData.players[0].gamePlayer,
                testData.players[1].gamePlayer,
                territory,
                testData.players[0].units,
                testData.players[1].units,
                bombard,
                ArrayList<TerritoryEffect>(),
                false,
                16
            )
        }
    }
}