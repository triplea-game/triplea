package games.strategy.triplea.odds.calculator

interface IRobustDoubleArray {
    val lastIndex: Int

    operator fun get(i: Int): Double
    operator fun set(i: Int, value: Double)
    val size: Int
    fun sum(): Double
    fun last(): Double
    fun isEmpty(): Boolean
}
