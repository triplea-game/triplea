package games.strategy.triplea.odds.calculator

class RobustDoubleArray(val data: DoubleArray) : IRobustDoubleArray {
    constructor(size: Int) : this(DoubleArray(size))

    override operator fun get(i: Int) = if (i in 0..data.lastIndex) data[i] else .0
    override operator fun set(i: Int, value: Double) {
        data[i] = value
    }

    override val size get() = data.size
    override val lastIndex get() = data.lastIndex
    override fun sum() = data.sum()
    override fun last() = data.last()
    override fun isEmpty() = data.isEmpty()

    fun clone(): RobustDoubleArray = RobustDoubleArray(data.clone())
}
