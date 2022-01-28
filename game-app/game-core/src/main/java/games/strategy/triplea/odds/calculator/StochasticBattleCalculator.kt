package games.strategy.triplea.odds.calculator

import com.google.common.base.Preconditions
import games.strategy.engine.data.*
import games.strategy.triplea.Properties
import games.strategy.triplea.attachments.UnitAttachment
import games.strategy.triplea.delegate.battle.BattleState
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls
import games.strategy.triplea.odds.calculator.Distribution.Companion.dropLast
import games.strategy.triplea.odds.calculator.Distribution.Companion.totalDiceCount
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.HitReceiver.Companion.getKey
import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.WhenCallBackup.*
import games.strategy.triplea.util.TuvUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.triplea.java.Postconditions
import org.triplea.java.ThreadRunner
import org.triplea.java.concurrency.CountUpAndDownLatch
import java.text.DecimalFormat
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.absoluteValue
import games.strategy.engine.data.Unit as GameUnit

@Suppress("NOTHING_TO_INLINE")
class StochasticBattleCalculator(private val dataLoadedAction: Runnable = Runnable { }) : IBattleCalculator {
    @Volatile
    private var cancelled = false
    private val isRunning = AtomicBoolean(false)

    private var backupCalculator: ConcurrentBattleCalculator? = null

    private fun getBackupCalculator(): ConcurrentBattleCalculator {
        if (backupCalculator == null) {
            backupCalculator = ConcurrentBattleCalculator(dataLoadedAction)
            with(backupCalculator!!) {
                setGameData(gameData)
                setRetreatAfterRound(retreatAfterRound)
                setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft)
                setKeepOneAttackingLandUnit(keepOneAttackingLandUnit)
                setAmphibious(amphibious)
                setAttackerOrderOfLosses(attackerOrderOfLosses)
                setDefenderOrderOfLosses(defenderOrderOfLosses)
            }
        }

        return backupCalculator!!
    }

    var gameData: GameData? = null
        set(value) {
            field = value
            diceSides = field?.diceSides ?: 6
        }

    var diceSides
        get() = Distribution.diceSides
        set(value) {
            Distribution.diceSides = value
        }

    private var beforeMe: Instant? = null
    private var beforeBackup: Instant? = null
    private var afterBackup: Instant? = null

    override fun calculate(
        attacker: GamePlayer?,
        defender: GamePlayer?,
        location: Territory?,
        attacking: Collection<GameUnit>?,
        defending: Collection<GameUnit>?,
        bombarding: Collection<GameUnit>?,
        territoryEffects: Collection<TerritoryEffect>?,
        retreatWhenOnlyAirLeft: Boolean,
        runCount: Int,
    ): AggregateResults {
        Preconditions.checkState(
            !isRunning.getAndSet(true), "Can't calculate while operation is still running!")

        try {
            Preconditions.checkArgument(attacker != null)
            Preconditions.checkArgument(defender != null)
            Preconditions.checkArgument(location != null)
            Preconditions.checkArgument(attacking != null)
            Preconditions.checkArgument(defending != null)
            Preconditions.checkArgument(bombarding != null)
            Preconditions.checkArgument(territoryEffects != null)

            Preconditions.checkArgument(attacking != null)
            Preconditions.checkArgument(defending != null)

            ++requestsTotal

            val iCanDoIt = !retreatWhenOnlyAirLeft &&
                    (attacking!!.firstOrNull { it.unitAttachment.isInfrastructure } == null) &&
                    (attacking.firstOrNull {
                        it.cantBeHandledByStochasticBattleCalculator(attacker!!) ||
                                it.unitAttachment.getAttackRolls(attacker) != 1
                    } == null) &&
                    (defending!!.firstOrNull {
                        it.cantBeHandledByStochasticBattleCalculator(defender!!) ||
                                it.unitAttachment.getDefenseRolls(defender) != 1
                    } == null) && (!location!!.water || waterBattleSetupAsExpected(attacking, defending))

            if (!iCanDoIt) {
                val attackingInfrastructure = attacking!!.firstOrNull { it.unitAttachment.isInfrastructure }

                log.info("can't calculate, because " +
                        if (attackingInfrastructure != null)
                            "attacker comes with infrastructure unit ${attackingInfrastructure.type.name} "
                        else "" +
                                if (location!!.water && !waterBattleSetupAsExpected(attacking,
                                        defending!!)
                                ) "water with unexpected setup" else "")
            }

            beforeMe = Instant.now()

            val myRet =
                if (iCanDoIt && whenCallBackup != BackupCalculatorOnly) {
                    try {
                        Core(
                            attacker!!,
                            defender!!,
                            bombarding!!,
                            attacking!!,
                            defending!!,
                            location!!,
                            territoryEffects!!,
                        ).calculate()
                    } catch (e: Exception) {
                        log.error("${javaClass.name}.calculate() failed unexpectedly", e)
                        null
                    }
                } else null

            beforeBackup = Instant.now()
            val backupRet = if (whenCallBackup == Always ||
                whenCallBackup == BackupCalculatorOnly ||
                whenCallBackup == Sometimes && requestsTotal % 10 == 0 ||
                whenCallBackup == OnlyWhenNecessary && myRet == null
            ) {
                getBackupCalculator().calculate(attacker,
                    defender,
                    location,
                    attacking,
                    defending,
                    bombarding,
                    territoryEffects,
                    retreatWhenOnlyAirLeft,
                    runCount)
            } else null

            afterBackup = if (backupRet == null) beforeBackup else Instant.now()

            reportToMonitor(beforeMe, beforeBackup, afterBackup)

            if (myRet != null) {
                with(myRet as StochasticResult) {

                    if (backupRet != null) {
                        val myTuv = averageTuvSwingOfBattle
                        val cbTuv = backupRet.getAverageTuvSwing(attacker, attacking, defender, defending, gameData)
                        log.info("${dec00(attackerWinPercent * 100)}/${dec00(drawPercent * 100)}/${
                            dec00(defenderWinPercent * 100)
                        } " +
                                dec2Decimals(myTuv) +
                                " - ${dec00(backupRet.attackerWinPercent * 100)}/${dec00(backupRet.drawPercent * 100)}/${
                                    dec00(backupRet.defenderWinPercent * 100)
                                } " +
                                dec2Decimals(cbTuv)
                        )

                        if (location!!.water && attacking!! contains { unit -> unit.isLand && unit.transportedBy == null }) {
                            log.debug("land units outside transports attacking on water!")
                        } else if (((myTuv - cbTuv) / myTuv).absoluteValue > 1.0 && (myTuv - cbTuv).absoluteValue > 1.5) {
                            fun Collection<GameUnit>.tS() = StringBuilder().also { s ->
                                forEach {
                                    s.append(it.type.name)
                                    s.append('\n')
                                }
                            }.toString()

                            log.debug("differing tuv-result with ${attacker!!.name} attacking\n" + attacking!!.tS() +
                                    "\n${defender!!.name}  defending:\n" + defending!!.tS())
                        }
                    }
                }
            }

            cancelled = false
            return myRet ?: backupRet!!
        } finally {
            isRunning.set(false)
        }
    }

    private fun waterBattleSetupAsExpected(
        attacking: Collection<GameUnit>,
        defending: Collection<GameUnit>,
    ) = waterBattleSetupAsExpected(attacking) && waterBattleSetupAsExpected(defending)

    private fun waterBattleSetupAsExpected(army: Collection<GameUnit>): Boolean {
        for (unit in army) {
            val transport = unit.transportedBy
            if (unit.unitAttachment.isSea || unit.unitAttachment.isAir) {
                if (transport != null)
                    return false
            } else {
                if (//transport == null ||  //it can happen that land units without transport are passed to
                // the battle calculator. They are considered non-combatants.
                    transport != null && !army.contains(transport))
                    return false
            }
        }

        return true
    }

    inner class Core(
        val attacker: GamePlayer,
        val defender: GamePlayer,
        val bombarding: Collection<GameUnit>,
        val attacking: Collection<GameUnit>,
        val defending: Collection<GameUnit>,
        val location: Territory,
        val territoryEffects: Collection<TerritoryEffect>,
    ) {
        /* The following uses the concept of a _receptor_. A receptor can take one hit.
            Each unit is represented by one receptor per hits it can take until and including when it is killed.
            For code brevity, receptor is abbreviated by "rec". 
        
            Since code deals a lot with probability distributions, 
            for code brevity, distribution is sometimes abbreviated by "dist".
         */

        private val stateDistributionChangedSignificantly: Boolean
            get() {
                var delta = .0

                for (iAttacker in 0..nAttackerRecs)
                    for (iDefender in 0..nDefenderRecs) {
                        delta += (stateDistribution1[iAttacker, iDefender] -
                                stateDistribution2[iAttacker, iDefender]).absoluteValue

                        if (delta > significanceThresholdArray)
                            return true
                    }

                return false
            }

        val gameData get() = this@StochasticBattleCalculator.gameData // provide access to users of core object

        lateinit var attackerHitDist: Array<Distribution>
        lateinit var defenderHitDist: Array<Distribution>
        lateinit var attackerOverkill: Array<RobustDoubleArray>
        lateinit var defenderOverkill: Array<RobustDoubleArray>
        lateinit var attackerRecs: List<HitReceiver>
        lateinit var defenderRecs: List<HitReceiver>
        lateinit var infrastructureUnits: List<GameUnit>

        var nAttackerRecs = -1
        var nDefenderRecs = -1

        lateinit var stateDistribution1: StateDistribution
        private lateinit var stateDistribution2: StateDistribution

        var averageBattleRoundsFought = -1.0
        var time = 0L

        fun calculate(): AggregateResults {
            ++requestsHandled
            val start = System.currentTimeMillis()

            val initialStateIsResultOfBombarding = setupInitialState()

            stateDistribution2 = StateDistribution(nAttackerRecs, nDefenderRecs)

            var probability: Double
            val probabilityEndedThisRound = ArrayList<Double>()

            var probabilityEndedRoundBefore = if (initialStateIsResultOfBombarding) .0
            else {
                ((0..nAttackerRecs).sumOf { stateDistribution1[it, 0] } +
                        (1..nDefenderRecs).sumOf { stateDistribution1[0, it] }).also { probabilityEndedThisRound.add(it) }
            }

            probability = 1.0 - probabilityEndedRoundBefore

            while (probability >= significanceThresholdArray &&
                stateDistributionChangedSignificantly &&
                (retreatAfterRound < 0 || probabilityEndedThisRound.size < retreatAfterRound - 1) &&
                !cancelled
            ) {
                stateDistribution2.clear()
                probability = calculateNextState()
                probabilityEndedThisRound.add(1.0 - probabilityEndedRoundBefore - probability)
                probabilityEndedRoundBefore = 1.0 - probability
            }

            averageBattleRoundsFought =
                (0..probabilityEndedThisRound.lastIndex).sumOf { (it + 1) * probabilityEndedThisRound[it] } +
                        (probabilityEndedThisRound.size + 1) * (1 - probabilityEndedThisRound.sum())

            nAttackerRecs = attackerRecs.size
            nDefenderRecs = defenderRecs.size

            time = System.currentTimeMillis() - start
            tidyUpResultDistribution()
            return StochasticResult(this)
        }

        private fun tidyUpResultDistribution() {
            val probabilityDefinitiveResult =
                (0..nAttackerRecs).sumOf { stateDistribution1[it, 0] } +
                        (1..nDefenderRecs).sumOf { stateDistribution1[0, it] }

            if (stateDistributionChangedSignificantly) {
                (0..nAttackerRecs).forEach { stateDistribution1[it, 0] /= probabilityDefinitiveResult }
                (1..nDefenderRecs).forEach { stateDistribution1[0, it] /= probabilityDefinitiveResult }
            } else {
                stateDistribution1[0, 0] = 1.0 - probabilityDefinitiveResult
            }

            for (a in 1..nAttackerRecs)
                for (d in 1..nDefenderRecs)
                    stateDistribution1[a, d] = .0
        }

        /**
         *  @return true if the calculated initial state was the result of bombarding
         *  rather than the first normal battle round
         */

        fun setupInitialState(): Boolean {
            val latchWorkerThreadsCreation = CountUpAndDownLatch()
            latchWorkerThreadsCreation.increment()

            var attackerMaxHitDistInThread: Distribution? = null
            var bombardingHitDistInThread: Distribution? = null
            ThreadRunner.runInNewThread("attackerHitRecs") {
                attackerMaxHitDistInThread = setHitRecsAndReturnMaxHitDist(true, location.water)
                nAttackerRecs = attackerRecs.size
                attackerHitDist = getHitDistributions(nAttackerRecs, attackerMaxHitDistInThread!!)
                attackerOverkill = attackerHitDist.overkill
                bombardingHitDistInThread = getBombardingHitDist()
                latchWorkerThreadsCreation.countDown()
            }

            var defenderMaxHitDist = setHitRecsAndReturnMaxHitDist(false, location.water)
            nDefenderRecs = defenderRecs.size
            defenderHitDist = getHitDistributions(nDefenderRecs, defenderMaxHitDist)
            defenderOverkill = defenderHitDist.overkill
            infrastructureUnits = defending.filter { it.unitAttachment.isInfrastructure }

            latchWorkerThreadsCreation.await()

            val attackerMaxHitDist: Distribution
            val calculateBombarding = (bombardingHitDistInThread?.key ?: 0L) > 0L
            if (calculateBombarding) {
                attackerMaxHitDist = bombardingHitDistInThread!!
                defenderMaxHitDist = Distribution[0] // defenders don't fight back against bombarding
            } else {
                attackerMaxHitDist = attackerMaxHitDistInThread!!
            }

            val attackerMaxOverkill = attackerMaxHitDist.distribution.overkill
            val defenderMaxOverkill = defenderMaxHitDist.distribution.overkill

            stateDistribution1 = StateDistribution(nAttackerRecs, nDefenderRecs)

            for (remainingAttackerRecs in 0..nAttackerRecs) {
                val attackersHit = nAttackerRecs - remainingAttackerRecs

                for (remainingDefenderRecs in 0..nDefenderRecs) {
                    val defendersHit = nDefenderRecs - remainingDefenderRecs

                    stateDistribution1[remainingAttackerRecs, remainingDefenderRecs] =
                        attackerMaxHitDist[defendersHit] * defenderMaxHitDist[attackersHit]
                }

                stateDistribution1[remainingAttackerRecs, 0] +=
                    attackerMaxOverkill[nDefenderRecs] * defenderMaxHitDist[attackersHit]
            }

            for (remainingDefenderRecs in 0..nDefenderRecs) {
                val defendersHit = nDefenderRecs - remainingDefenderRecs

                stateDistribution1[0, remainingDefenderRecs] +=
                    defenderMaxOverkill[nAttackerRecs] * attackerMaxHitDist[defendersHit]
            }

            stateDistribution1.verify()
            instrumentationMonitor?.stateDistribution = stateDistribution1

            return calculateBombarding
        }

        private fun getBombardingHitDist(): Distribution {
            val key = bombarding.getKey { unitAttachment.bombard }

            return Distribution[key]
        }

        fun setHitRecsAndReturnMaxHitDist(isAttacker: Boolean, isWaterBattle: Boolean): Distribution {
            val combatValue = CombatValueBuilder.buildMainCombatValue(
                if (isAttacker) defending else attacking,
                if (isAttacker) attacking else defending,
                if (isAttacker) BattleState.Side.OFFENSE else BattleState.Side.DEFENSE,
                gameData!!.sequence,
                gameData!!.unitTypeList.supportRules,
                Properties.getLhtrHeavyBombers(gameData!!.properties),
                diceSides,
                territoryEffects)

            val player = if (isAttacker) attacker else defender

            val targetsToPickFrom = if (isAttacker) attacking else defending
            val costsForTuv = TuvUtils.getCostsForTuv(player, gameData!!)
            val parameters = ParametersBuilder.build(
                targetsToPickFrom,
                player,
                combatValue,
                location,
                costsForTuv,
                gameData!!)!!

            var sortedUnits = ParametersBuilder.sortUnitsForCasualtiesWithSupport(parameters)
            val unitPowerAndRolls = PowerStrengthAndRolls.buildWithPreSortedUnits(sortedUnits, combatValue)

            val getHitPower: GameUnit.() -> Int = {
                val rolls = unitPowerAndRolls.getRolls(this)
                val power = unitPowerAndRolls.getPower(this)
                if (rolls != 1 && power != 0) { // if power == 0 then the number of rolls is irrelevant
                    log.debug("unitPowerAndRolls.getRolls(${this.type.name}) == $rolls")
                    log.debug("attachment rolls = ${
                        if (isAttacker) unitAttachment.getAttackRolls(player) else unitAttachment.getDefenseRolls(player)
                    }")
                }

                Preconditions.checkState(rolls == 1 || power == 0)
                power
            }

            sortedUnits =
                sortedUnits.filter {
                    !it.unitAttachment.isInfrastructure &&
                            (!isWaterBattle || it.unitAttachment.isSea || it.unitAttachment.isAir)
                }

            sortedUnits.sortBy { -it.getHitPower() }
            val hitRecs = sortedUnits.withDamageReceivers

            if (isAttacker) attackerRecs = hitRecs else defenderRecs = hitRecs

            Postconditions.assertState(hitRecs isSortedBy { -(it.unit?.getHitPower() ?: 0) })

            val key = hitRecs.getKey(getHitPower)

            return Distribution[key]
        }

        /**
         * @return the probability for remaining units on both sides
         */

        private fun calculateNextState(): Double {
            for (completelyCalculatedAttackerRecs in nAttackerRecs + 1..attackerRecs.size)
                stateDistribution2[completelyCalculatedAttackerRecs, 0] =
                    stateDistribution1[completelyCalculatedAttackerRecs, 0]

            for (completelyCalculatedDefenderRecs in nDefenderRecs + 1..defenderRecs.size)
                stateDistribution2[0, completelyCalculatedDefenderRecs] =
                    stateDistribution1[0, completelyCalculatedDefenderRecs]

            val probabilityForRemainingUnitsOnBothSides =
                calculateForRemainingAttackers()

            stateDistribution1 = stateDistribution2.also { stateDistribution2 = stateDistribution1 }
            stateDistribution1.verify()
            instrumentationMonitor?.stateDistribution = stateDistribution1

            return probabilityForRemainingUnitsOnBothSides
        }

        private fun calculateForRemainingAttackers(): Double {
            var probabilityForRemainingUnitsOnBothSides = .0

            val nAttackerRecs = nAttackerRecs
            val attackerHitDist = attackerHitDist
            val attackerOverkill = attackerOverkill

            val nDefenderRecs = nDefenderRecs
            val defenderHitDist = defenderHitDist
            val defenderOverkill = defenderOverkill

            val stateDistribution1 = stateDistribution1
            val stateDistribution2 = stateDistribution2

            val probabilityNAttackerRecs = DoubleArray(nAttackerRecs + 1)
            val probabilityNDefenderRecs = DoubleArray(nDefenderRecs + 1)

            fun addToNextState(remainingAttackerRecs: Int, remainingDefenderRecs: Int, probability: Double) {
                stateDistribution2[remainingAttackerRecs, remainingDefenderRecs] += probability

                if (remainingAttackerRecs > 0 && remainingDefenderRecs > 0) {
                    probabilityForRemainingUnitsOnBothSides += probability

                    probabilityNAttackerRecs[remainingAttackerRecs] += probability
                    probabilityNDefenderRecs[remainingDefenderRecs] += probability
                }
            }

            for (remainingAttackerRecs in 0..nAttackerRecs) {
                for (remainingDefenderRecs in 0..nDefenderRecs) {
                    for (beginningAttackerRecs in remainingAttackerRecs..nAttackerRecs) {
                        val attackersHit = beginningAttackerRecs - remainingAttackerRecs

                        for (beginningDefenderRecs in remainingDefenderRecs..nDefenderRecs) {
                            val defendersHit = beginningDefenderRecs - remainingDefenderRecs

                            addToNextState(remainingAttackerRecs,
                                remainingDefenderRecs,
                                stateDistribution1[beginningAttackerRecs, beginningDefenderRecs] *
                                        attackerHitDist[beginningAttackerRecs][defendersHit] *
                                        defenderHitDist[beginningDefenderRecs][attackersHit])

                        }
                    }
                }

                for (beginningAttackerRecs in remainingAttackerRecs..nAttackerRecs) {
                    val attackersHit = beginningAttackerRecs - remainingAttackerRecs

                    for (beginningDefenderRecs in 0..nDefenderRecs) {
                        addToNextState(remainingAttackerRecs,
                            0,
                            stateDistribution1[beginningAttackerRecs, beginningDefenderRecs] *
                                    attackerOverkill[beginningAttackerRecs][beginningDefenderRecs] *
                                    defenderHitDist[beginningDefenderRecs][attackersHit])
                    }
                }
            }

            for (remainingDefenderRecs in 0..nDefenderRecs) {
                for (beginningDefenderRecs in remainingDefenderRecs..nDefenderRecs) {
                    val defendersHit = beginningDefenderRecs - remainingDefenderRecs

                    for (beginningAttackerRecs in 0..nAttackerRecs) {
                        addToNextState(0,
                            remainingDefenderRecs,
                            stateDistribution1[beginningAttackerRecs, beginningDefenderRecs] *
                                    defenderOverkill[beginningDefenderRecs][beginningAttackerRecs] *
                                    attackerHitDist[beginningAttackerRecs][defendersHit])
                    }
                }
            }

            while (this.nAttackerRecs > 0 && probabilityNAttackerRecs[this.nAttackerRecs] < significanceThresholdLine)
                this.nAttackerRecs--

            while (this.nDefenderRecs > 0 && probabilityNDefenderRecs[this.nDefenderRecs] < significanceThresholdLine)
                this.nDefenderRecs--

            return probabilityForRemainingUnitsOnBothSides
        }

    }

    class StateDistribution(private val maxAttackerRecs: Int, private val maxDefenderRecs: Int) {
        val defenderRecsIncluding0 = maxDefenderRecs + 1
        val data = DoubleArray((maxAttackerRecs + 1) * defenderRecsIncluding0)

        inline operator fun set(remainingAttackerRecs: Int, remainingDefenderRecs: Int, probability: Double) {
            data[remainingAttackerRecs * defenderRecsIncluding0 + remainingDefenderRecs] = probability
        }

        override fun toString(): String {
            val s = StringBuilder()
            for (remainingAttackerRecs in 0..maxAttackerRecs) {
                for (remainingDefenderRecs in 0..maxDefenderRecs)
                    s.append("${decPad6(get(remainingAttackerRecs, remainingDefenderRecs) * 100_000)} ")

                s.append('\n')
            }

            return s.toString()
        }

        inline fun index(remainingAttackerRecs: Int, remainingDefenderRecs: Int) =
            remainingAttackerRecs * defenderRecsIncluding0 + remainingDefenderRecs

        inline operator fun get(remainingAttackerRecs: Int, remainingDefenderRecs: Int) =
            data[index(remainingAttackerRecs, remainingDefenderRecs)]

        fun clear() {
            data.fill(.0)
        }

        fun verify() {
            val sum = data.sum()
            log.info("verify finds: ${dec(sum * 100)}%")
//            log.debug("\n" + toString())
//            Preconditions.checkState(sum > .7) { "total probability distribution should be 1.0 but is only $sum" }
        }
    }

    private fun reportToMonitor(beforeMe: Instant?, beforeBackup: Instant?, afterBackup: Instant?) {
        if (beforeMe != null && beforeBackup != null && afterBackup != null)
            instrumentationMonitor?.performanceReport(beforeMe, beforeBackup, afterBackup)

        instrumentationMonitor?.text =
            "callsStatistics\n" +
                    "$requestsHandled of $requestsTotal handled (${dec(requestsHandled * 100 / requestsTotal)}%)"
    }

    var retreatAfterRound = -1
        set(value) {
            field = value
            backupCalculator?.setRetreatAfterRound(field)
        }

    var retreatAfterXUnitsLeft = -1
        set(value) {
            field = value
            backupCalculator?.setRetreatAfterXUnitsLeft(field)
        }

    var keepOneAttackingLandUnit = false
        set(value) {
            field = value
            backupCalculator?.setKeepOneAttackingLandUnit(field)
        }

    var amphibious = false
        set(value) {
            field = value
            backupCalculator?.setAmphibious(field)
        }

    var attackerOrderOfLosses: String? = null
        set(value) {
            field = value
            backupCalculator?.setAttackerOrderOfLosses(field)
        }

    var defenderOrderOfLosses: String? = null
        set(value) {
            field = value
            backupCalculator?.setDefenderOrderOfLosses(field)
        }

    init {
        dataLoadedAction.run()
    }

    fun cancel() {
        cancelled = true
        backupCalculator?.cancel()
    }

    enum class WhenCallBackup {
        Always, Sometimes, OnlyWhenNecessary, BackupCalculatorOnly
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(StochasticBattleCalculator::class.java)

        var instrumentationMonitor: InstrumentationMonitor? = null

        private const val significanceThresholdArray = .01
        private const val significanceThresholdLine = .01

        private val decimalFormat = DecimalFormat("#,###")
        fun dec(value: Number): String = decimalFormat.format(value)
        private val decimalFormat00 = DecimalFormat("00")
        fun dec00(value: Number): String = decimalFormat00.format(value)
        fun decPad6(value: Number): String = decimalFormat.format(value).run { padStart(6, ' ') }
        private val decimalFormat__ = DecimalFormat("#.00")
        fun dec2Decimals(value: Number): String = decimalFormat__.format(value)

        var requestsTotal = 0
        var requestsHandled = 0
        var mismatches = 0

        /**
         * given <code>this</code> is a hit distribution,
         * overkill[i] is the probability of more than i hits
         */

        val RobustDoubleArray.overkill
            get() = RobustDoubleArray(if (size < 2) DoubleArray(0) else DoubleArray(size - 1).also { overkill ->
                overkill[overkill.lastIndex] = data[lastIndex]
                for (i in overkill.lastIndex - 1 downTo 0)
                    overkill[i] = overkill[i + 1] + data[i + 1]
            })

        /**
         * getHitDistributions[x] is the hit distribution with the best x units of maximumHitDistribution causing damage.
         * getHitDistributions[x][y] is the probability that the best x units of maximumHitDistribution cause of y hits.
         */

        fun getHitDistributions(nRecs: Int, maximumHitDistribution: Distribution): Array<Distribution> {
            val ret = Array(nRecs + 1) { maximumHitDistribution }

            val nRecsWithoutHitPower = nRecs - maximumHitDistribution.key.totalDiceCount
            var keyOfNextDistribution = maximumHitDistribution.key
            for (i in ret.lastIndex - 1 - nRecsWithoutHitPower downTo 0) {
                keyOfNextDistribution = keyOfNextDistribution.dropLast
                ret[i] = Distribution[keyOfNextDistribution]
            }

            return ret
        }

        /**
         * overkill[x] is the overkill probability distribution with the best x units causing damage.
         * overkill[x][y] is the probability that the best x units cause more than y damage.
         */

        val Array<Distribution>.overkill
            get() = Array(size) {
                this[it].distribution.overkill
            }

        @Suppress("UNUSED_PARAMETER")
        fun GameUnit.cantBeHandledByStochasticBattleCalculator(player: GamePlayer): Boolean {
            var mayChangeIntoNonInfrastructure = false
            var nonInfrastructureUnitWithWhenChangeEvent = false

            return with(unitAttachment) {
                mayChangeIntoNonInfrastructure = whenHitPointsDamagedChangesInto.isNotEmpty() &&
                        whenHitPointsDamagedChangesInto.filter { predicate ->
                            val unitType = predicate.value.second
                            val attachment = unitType.getAttachment("unitAttachment") as UnitAttachment
                            return@filter !attachment.isInfrastructure
                        }.isNotEmpty()

                nonInfrastructureUnitWithWhenChangeEvent = !isInfrastructure &&
                        (whenCapturedChangesInto.isNotEmpty() || destroyedWhenCapturedBy.isNotEmpty())

                hitPoints == 0 && !isInfrastructure ||
                        isSuicideOnAttack ||
                        isSuicideOnHit ||
                        attackingLimit != null ||
                        chooseBestRoll ||
                        isFirstStrike ||
                        canNotTarget.isNotEmpty() ||
                        canNotBeTargetedBy.isNotEmpty() ||
                        isAaForCombatOnly ||
                        willNotFireIfPresent.isNotEmpty() ||
                        mayChangeIntoNonInfrastructure ||
                        canBeCapturedOnEnteringBy.isNotEmpty() ||
                        nonInfrastructureUnitWithWhenChangeEvent
                // whenHitPointsDamagedChangesInto.isNotEmpty()
                // TODO evaluate whenHitPointsDamagedChangesInto - but how?
            }.also {
                if (it) {
                    val s = StringBuilder("cant calculate battle, because unit(attachment) ${type.name}...")
                    with(unitAttachment) {
                        if (hitPoints == 0 && !isInfrastructure) s.append("\nhitPoints == 0 && !isInfrastructure")
                        if (isSuicideOnAttack) s.append("\nisSuicideOnAttack")
                        if (isSuicideOnHit) s.append("\nisSuicideOnHit")
                        if (attackingLimit != null) s.append("\nattackingLimit != null")
                        if (chooseBestRoll) s.append("\nchooseBestRoll")
                        if (isFirstStrike) s.append("\nisFirstStrike")
                        if (canNotTarget.isNotEmpty()) s.append("\ncanNotTarget.isNotEmpty()")
                        if (canNotBeTargetedBy.isNotEmpty()) s.append("\ncanNotBeTargetedBy.isNotEmpty()")
                        if (isAaForCombatOnly) s.append("\nisAaForCombatOnly")
                        if (willNotFireIfPresent.isNotEmpty()) s.append("\nwillNotFireIfPresent.isNotEmpty()")
                        if (mayChangeIntoNonInfrastructure) s.append("\nmayChangeIntoNonInfrastructure")
                        if (canBeCapturedOnEnteringBy.isNotEmpty()) s.append("\ncanBeCapturedOnEnteringBy.isNotEmpty()")
                        if (nonInfrastructureUnitWithWhenChangeEvent)
                            s.append("\nnonInfrastructureUnitWithWhenChangeEvent")
                    }

                    log.debug(s.toString())
                }
            }
        }

        private val Iterable<GameUnit>.withDamageReceivers: List<HitReceiver>
            get() {
                val ret = ArrayList<HitReceiver>()

                forEach { unit ->
                    ret.add(HitReceiver(unit))
                }

                forEach { unit ->
                    repeat((2..(unit.unitAttachment.hitPoints - unit.hits)).count()) {
                        ret.add(HitReceiver.nonLethalDamageReceiver)
                    }
                }

                return ret
            }

        /**
         * this is not specific to anything within StochasticBattleCalculator
         * and hence a candidate for a generic extension function of Iterable
         */

        infix fun <T, R : Comparable<R>> Iterable<T>.isSortedBy(selector: (T) -> R?): Boolean {
            val iterator = iterator()

            var allSortedCorrectly = true
            if (iterator.hasNext()) {
                var previousSelector = selector(iterator.next())

                while (iterator.hasNext() && allSortedCorrectly) {
                    val nextSelector = selector(iterator.next())
                    val cmp = previousSelector?.let { nextSelector?.compareTo(it) ?: -1 }
                        ?: if (nextSelector == null) 0 else 1

                    allSortedCorrectly = cmp >= 0
                    previousSelector = nextSelector
                }
            }

            return allSortedCorrectly
        }

        /**
         * this is not specific to anything within StochasticBattleCalculator
         * and hence a candidate for a generic extension function of Iterable
         */

        inline infix fun <T> Iterable<T>.contains(predicate: (T) -> Boolean) = find(predicate) != null

        private val GameUnit.isLand: Boolean get() = with(unitAttachment) { !isAir && !isSea }

        var whenCallBackup = OnlyWhenNecessary
    }

    class HitReceiver private constructor(
        val unit: GameUnit? = null,
        @Suppress("UNUSED_PARAMETER") compilerNeedsThis: Boolean = true,
    ) {
        constructor(unit: GameUnit) : this(unit, true)

        companion object {
            val nonLethalDamageReceiver = HitReceiver()

            val List<HitReceiver>.unitCount get() = indexOfFirst { it.unit == null }.let { if (it == -1) size else it }

            fun List<HitReceiver>.getKey(getHitPower: GameUnit.() -> Int): Long {
                val hitReceiverList = this

                val iterable = object : Iterable<GameUnit> {
                    override fun iterator(): Iterator<GameUnit> {
                        return object : Iterator<GameUnit> {
                            var i = 0

                            override fun hasNext() = i <= hitReceiverList.lastIndex && hitReceiverList[i].unit != null
                            override fun next() = hitReceiverList[i++].unit!!
                        }
                    }
                }

                return iterable.getKey(getHitPower)
            }

            fun Iterable<GameUnit>.getKey(getHitPower: GameUnit.() -> Int): Long {
                Preconditions.checkState(Distribution.diceSides <= 8)
                { "can't handle dice with more than 8 sides for the moment" }

                val counter = LongArray(Distribution.diceSides) // TODO support dice with more than 8 sides, e.g. 12
                forEach {
                    val hitPower = it.getHitPower()

                    if (hitPower > 0)
                        counter[hitPower - 1]++
                }

                return Distribution.getKey(counter)
            }

            fun List<HitReceiver>.firstNUnits(length: Int): ArrayList<GameUnit> {
                val ret = ArrayList<GameUnit>()

                (0 until length).forEach {
                    ret.add(this[it].unit!!)
                }

                return ret
            }
        }

    }
}

