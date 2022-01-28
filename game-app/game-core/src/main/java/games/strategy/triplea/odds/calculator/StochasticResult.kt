package games.strategy.triplea.odds.calculator

import com.google.common.base.Preconditions
import games.strategy.engine.data.GameData
import games.strategy.engine.data.GamePlayer
import games.strategy.engine.data.UnitType
import games.strategy.triplea.delegate.battle.BattleState.Side
import games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE
import games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.Companion.isSortedBy
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.Core
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.HitReceiver
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.HitReceiver.Companion.firstNUnits
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.HitReceiver.Companion.unitCount
import games.strategy.triplea.util.TuvUtils
import org.triplea.java.collections.IntegerMap
import org.triplea.util.Tuple
import kotlin.math.roundToInt
import games.strategy.engine.data.Unit as GameUnit

class StochasticResult(data: Core) : AggregateResults(0) {
    class Party(
        val player: GamePlayer,
        val side: Side,
        private val recs: List<HitReceiver>,
        private val distributionLeft: RobustDoubleArray,
        private val infrastructureUnits: List<GameUnit>,
        gameData: GameData?,
    ) {
        init {
            Preconditions.checkArgument(recs.size >= distributionLeft.size - 1)
        }

        private val tuvByUnit: IntegerMap<UnitType> = TuvUtils.getCostsForTuv(player, gameData)

        val averageTuvOfUnitsLeft by lazy {
            var tuvWhenSoManyUnitsLeft = 0
            var averageTuvOfUnitsLeft = .0

            Preconditions.checkState(recs isSortedBy { if (it.unit == null) 1 else 0 },
                "in lists of hit receivers units must come before damage receivers")

            for ((iUnit, i) in (1..distributionLeft.lastIndex).withIndex()) {
                val unit = recs[iUnit].unit

                if (unit != null) {
                    tuvWhenSoManyUnitsLeft += tuvByUnit.getTuvOfUnitAndCargo(unit)
                }

                averageTuvOfUnitsLeft += tuvWhenSoManyUnitsLeft * distributionLeft[i]
            }

            averageTuvOfUnitsLeft + infrastructureUnits.tuv * (winPercent + if (side == DEFENSE) drawPercent else .0)
        }

        val tuv by lazy {
            recs.sumOf { if (it.unit == null) 0 else tuvByUnit.getTuvOfUnitAndCargo(it.unit) } +
                    if (side == DEFENSE) infrastructureUnits.tuv else 0
        }

        private val averageNonInfrastructureUnitsLeft = run {
            val unitCount = recs.unitCount

            (1..unitCount).sumOf { it * distributionLeft[it] } +
                    unitCount * (unitCount + 1..distributionLeft.lastIndex).sumOf { distributionLeft[it] }
        }

        val winPercent = (1..distributionLeft.lastIndex).sumOf { distributionLeft[it] }

        val drawPercent = distributionLeft[0]

        private val averageInfrastructureUnitsLeft =
            infrastructureUnits.size * (winPercent + if (side == DEFENSE) drawPercent else .0)

        val averageUnitsLeft = averageNonInfrastructureUnitsLeft + averageInfrastructureUnitsLeft
        val averageUnitsLeftWhenWon = averageNonInfrastructureUnitsLeft / winPercent + infrastructureUnits.size

        fun getAverageUnitsRemaining(): Collection<GameUnit> =
            recs.firstNUnits(averageNonInfrastructureUnitsLeft.roundToInt()).apply {
                addAll(infrastructureUnits.sortedBy { -tuvByUnit.getTuvOfUnitAndCargo(it) }
                    .subList(0, averageInfrastructureUnitsLeft.roundToInt()))
            }

        private val List<GameUnit>.tuv get() = sumOf { unit -> tuvByUnit.getTuvOfUnitAndCargo(unit) }
    }

    init {
        time = data.time
    }

    val attacker = Party(data.attacker, OFFENSE, data.attackerRecs,
        RobustDoubleArray(DoubleArray(data.nAttackerRecs + 1) {
            data.stateDistribution1[it, 0]
        }),
        data.infrastructureUnitsIfAttackerWins,
        data.gameData
    )

    val defender = Party(data.defender, DEFENSE, data.defenderRecs,
        RobustDoubleArray(DoubleArray(data.nDefenderRecs + 1) {
            data.stateDistribution1[0, it]
        }),
        data.infrastructureUnits,
        data.gameData
    )

    val gameData = data.gameData

    override fun getAverageAttackingUnitsRemaining() = attacker.getAverageUnitsRemaining()
    override fun getAverageAttackingUnitsLeft() = attacker.averageUnitsLeft
    override fun getAverageAttackingUnitsLeftWhenAttackerWon() = attacker.averageUnitsLeftWhenWon
    override fun getAttackerWinPercent() = attacker.winPercent

    override fun getAverageDefendingUnitsRemaining() = defender.getAverageUnitsRemaining()
    override fun getAverageDefendingUnitsLeft() = defender.averageUnitsLeft
    override fun getAverageDefendingUnitsLeftWhenDefenderWon() = defender.averageUnitsLeftWhenWon
    override fun getDefenderWinPercent() = defender.winPercent

    override fun getAverageTuvOfUnitsLeftOver(
        attackerCostsForTuv: IntegerMap<UnitType>?,
        defenderCostsForTuv: IntegerMap<UnitType>?,
    ): Tuple<Double, Double> = Tuple.of(attacker.averageTuvOfUnitsLeft, defender.averageTuvOfUnitsLeft)

    val averageTuvSwingOfBattle: Double
        get() {
            val attackerStartingTuv = attacker.tuv
            val defenderStartingTuv = defender.tuv

            val tuvLeft = getAverageTuvOfUnitsLeftOver(null, null)

            return defenderStartingTuv - tuvLeft.second - (attackerStartingTuv - tuvLeft.first)
        }

    override fun getAverageTuvSwing(
        attacker: GamePlayer?,
        attackers: MutableCollection<GameUnit>?,
        defender: GamePlayer?,
        defenders: MutableCollection<GameUnit>?,
        data: GameData?,
    ) = averageTuvSwingOfBattle

    override fun getDrawPercent() = attacker.drawPercent

    private val averageBattleRoundsFought = data.averageBattleRoundsFought
    override fun getAverageBattleRoundsFought() = averageBattleRoundsFought

    /**
     * @return Int.MAX_VALUE, because we approximate a perfect probability distribution
     * equivalent to the result of infinite battles
     */
    override fun getRollCount() = Int.MAX_VALUE

    companion object {
        data class UnitCapturedBy(val unit: GameUnit, val attacker: GamePlayer)

        infix fun GameUnit.capturedBy(attacker: GamePlayer) = UnitCapturedBy(this, attacker)

        class UnitCapturedByFrom(val unit: GameUnit, val attacker: GamePlayer, val defender: GamePlayer) {
            val changesInto: List<GameUnit>?
                get() {
                    val whenCapturedChanges = unit.unitAttachment.whenCapturedChangesInto
                    val change = whenCapturedChanges[keyFromAnyToAny] ?: whenCapturedChanges["any:${attacker.name}"]
                    ?: whenCapturedChanges["${defender.name}:any"] ?: return null

                    val unitsToMake = change.second
                    val res = ArrayList<GameUnit>()
                    for (key in unitsToMake.keySet())
                        repeat(unitsToMake.getInt(key)) { res.add(key.create(attacker)) }

                    return res
                }

            companion object {
                const val keyFromAnyToAny = "any:any"
            }
        }

        infix fun UnitCapturedBy.from(defender: GamePlayer) = UnitCapturedByFrom(unit, attacker, defender)

        val StochasticBattleCalculator.Core.infrastructureUnitsIfAttackerWins: List<GameUnit>
            get() {
                val ret = ArrayList<GameUnit>()

                infrastructureUnits.forEach { unit ->
                    if (!unit.unitAttachment.destroyedWhenCapturedBy.contains(Tuple.of("BY", attacker)) &&
                        !unit.unitAttachment.destroyedWhenCapturedBy.contains(Tuple.of("FROM", defender))
                    ) {
                        (unit capturedBy attacker from defender).changesInto?.let { ret.addAll(it) } ?: ret.add(unit)
                    }
                }

                defenderRecs.forEach {
                    it.unit?.unitAttachment?.run {
                        whenHitPointsDamagedChangesInto?.get(hitPoints)?.let { boolUnitType ->
                            ret.add(boolUnitType.second.create(attacker))
                        }
                    }
                }

                return ret
            }

        fun IntegerMap<UnitType>.getTuvOfUnitAndCargo(unit: GameUnit) =
            getInt(unit.type) + unit.transporting.sumOf { getInt(it.type) }
    }
}