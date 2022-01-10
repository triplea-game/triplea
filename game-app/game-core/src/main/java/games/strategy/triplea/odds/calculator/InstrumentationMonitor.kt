package games.strategy.triplea.odds.calculator

import games.strategy.triplea.odds.calculator.StochasticBattleCalculator.StateDistribution
import java.time.Instant

interface InstrumentationMonitor {
    fun performanceReport(beforeMe: Instant, beforeBuddy: Instant, afterBuddy: Instant)
    var text: String
    var stateDistribution: StateDistribution?

    companion object {
        private val runtime = Runtime.getRuntime()

        fun memoryStatus(): String {
            val maxMemory = runtime.maxMemory()
            val freeMemoryBeforeGC = runtime.freeMemory()
            val freeMemoryAfterGC = runtime.freeMemory()

            fun absAndRelative(value: Number) = "${StochasticBattleCalculator.dec(value)} (${
                StochasticBattleCalculator.dec(value.toDouble() * 100.0 / maxMemory)
            })"

            return "Memory (free/garbage/used) = " +
                    absAndRelative(freeMemoryBeforeGC) +
                    "/${absAndRelative(freeMemoryAfterGC - freeMemoryBeforeGC)}" +
                    "/${absAndRelative(maxMemory - freeMemoryBeforeGC)}"
        }

    }
}
