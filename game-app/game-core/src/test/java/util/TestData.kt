package util

import games.strategy.engine.ClientFileSystemHelper
import games.strategy.engine.data.GameData
import games.strategy.engine.data.GamePlayer
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel
import games.strategy.triplea.delegate.GameDataTestUtil
import games.strategy.triplea.delegate.data.CasualtyList
import java.nio.file.Path
import kotlin.Unit
import kotlin.apply
import kotlin.math.absoluteValue
import games.strategy.engine.data.Unit as GameUnit

class TestData(
    gameFile: String,
    playerName: String,
    vararg unitsOfTypes: UnitsOfType,
    val territoryName: String? = null,
) {
    fun add(playerName: String, vararg unitsOfTypes: UnitsOfType) =
        Player(playerName, unitsOfTypes).also {
            players.add(it)
            territory?.unitCollection?.addAll(players.last().units)
        }

    init {
        gsm.load(userMapsFolder.resolve(gameFile))
    }

    val gameData: GameData = gsm.gameData!!
    val territory = if (territoryName != null) GameDataTestUtil.territory(territoryName, gameData) else null

    inner class Player(playerName: String, unitsOfTypes: Array<out UnitsOfType>) {
        val gamePlayer: GamePlayer = gameData.playerList.getPlayerId(playerName)!!
        val units: MutableList<GameUnit> = ArrayList()
        val killed: MutableList<GameUnit> = ArrayList()
        val hurt: MutableList<GameUnit> = ArrayList()

        init {
            for (unitsOfType in unitsOfTypes) {
                units.addAll(unitsOfType.get(gameData, gamePlayer, killed, hurt))
            }
        }

        val defaultCasualties = CasualtyList(killed, hurt)
        val hits = killed.size + hurt.size
    }

    val players = ArrayList<Player>().apply { add(Player(playerName, unitsOfTypes)) }

    companion object {
        val gsm = GameSelectorModel()

        init {
            UiTest.setup(Array(0) { "" })
        }

        val userMapsFolder: Path get() = ClientFileSystemHelper.getUserMapsFolder()

        fun twoOnOne() =
            TestData(
                "big_world\\map\\games\\Big_World_1942_v3rules.xml",
                "Americans",
                (2 unitsOfType "armour")
            ).apply {
                add("Germans",
                    (1 unitsOfType "armour"))
            }

        fun threeOnThree() =
            TestData(
                "big_world\\map\\games\\Big_World_1942_v3rules.xml",
                "Americans",
                (3 unitsOfType "marine") withInit { index, unit, killed, _ ->
                    if (index <= 1)
                        killed.add(unit)
                }
            ).apply {
                add("Germans",
                    (2 unitsOfType "infantry"),
                    (1 unitsOfType "armour"))
            }

        fun nArmoursOnBothSides(n: Int) =
            TestData(
                "big_world\\map\\games\\Big_World_1942_v3rules.xml",
                "Americans",
                (n unitsOfType "armour"),
            ).apply {
                add("Germans",
                    (n unitsOfType "armour"))
            }

    }
}

typealias UnitInitializer = (
    index: Int,
    unit: GameUnit,
    killed: MutableList<GameUnit>,
    hurt: MutableList<GameUnit>,
) -> Unit

class UnitsOfType(val typeName: String, val number: Int) {
    private var applyToUnit: UnitInitializer? = null

    infix fun withInit(applyToUnit: UnitInitializer): UnitsOfType {
        this.applyToUnit = applyToUnit

        return this
    }

    fun get(
        gameData: GameData,
        player: GamePlayer,
        killed: MutableList<GameUnit>,
        hurt: MutableList<GameUnit>,
    ): Collection<GameUnit> {
        return ArrayList<GameUnit>(number).apply {
            val unitType = gameData.unitTypeList.getUnitType(typeName)
            addAll(unitType.createTemp(number, player))
            applyToUnit?.let {
                for ((i, unit) in withIndex())
                    it(i, unit, killed, hurt)
            }
        }
    }
}

internal infix fun Int.unitsOfType(typeName: String) = UnitsOfType(typeName, this)

infix fun Double.almostEquals(other: Double) =
    other.absoluteValue <= .01 && this.absoluteValue <= .01 ||
            ((this / other).absoluteValue - 1.0).absoluteValue <= .01
