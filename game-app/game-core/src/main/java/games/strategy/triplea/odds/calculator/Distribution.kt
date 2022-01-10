package games.strategy.triplea.odds.calculator

import com.google.common.base.Preconditions

/**
 * the probability distribution of the number of hits given by a set if units with hitPower according to key
 *
 * @param key has one character per unit '0'..'9' meaning hitPower 0..9; 'A', 'B', 'C' meaning 10, 11, resp. 12
 */

class Distribution private constructor(
    val key: Long,
    val distribution: RobustDoubleArray = RobustDoubleArray(when (key.totalDiceCount) {
        0 -> DoubleArray(1) { 1.0 }
        1 -> {
            val probability = key.hitPowerOfSingleUnit.toDouble() / diceSides
            DoubleArray(2) { if (it == 0) 1.0 - probability else probability }
        }
        else ->
            distributions[key.dropLast] * distributions[key.takeLast]
    }),
) : IRobustDoubleArray by distribution {
    fun clone(): Distribution = Distribution(key, distribution.clone())

    operator fun times(other: Distribution): DoubleArray {
        Preconditions.checkArgument(other.distribution.size == 2) // TODO remove this requirement

        val probabilityZero = other.distribution[0]
        val probabilityOne = other.distribution[1]

        return DoubleArray(size + 1) {
            when (it) {
                0 -> probabilityZero * distribution[0]
                size -> probabilityOne * distribution.last()
                else -> probabilityZero * distribution[it] + probabilityOne * distribution[it - 1]
            }
        }
    }

    class DistributionMap {
        val hashMap = HashMap<Long, Distribution>()

        operator fun get(key: Long) = hashMap[key] ?: Distribution(key).also { hashMap[key] = it }
    }

    override fun toString(): String {
        val s = StringBuilder()

        for (hitPower in diceSides downTo 1) {
            val keyChar = keyChar(hitPower)
            for (i in 0 until key.countOfUnitsWithHitPower(hitPower)) {
                s.append(keyChar)
            }
        }
        return s.toString()
    }

    companion object {
        private const val bitsInLong = 64
        private const val bitsInKeyPerCount = 8
        const val maxDiceSides = bitsInLong / bitsInKeyPerCount
        private const val maxUnitsWithSameHitPower = (1L shl bitsInKeyPerCount) - 1

        var diceSides = 6
            set(value) {
                if (field != value) {
                    distributions.hashMap.clear()
                    field = value
                }
            }

        fun keyChar(i: Int) = (if (i <= 9) '0' else 'A' - 10) + i

        val distributions = DistributionMap()
        operator fun get(key: Long) = distributions[key]

        fun getKey(counter: LongArray): Long {
            var key = 0L
            for ((i, count) in counter.withIndex()) {
                Preconditions.checkState(count <= maxUnitsWithSameHitPower)
                { "can't handle armies with more than $maxUnitsWithSameHitPower units of the same hit power, but encountered $count" }

                val hitPower = i + 1
                key = key or countAsBitsInKey(count, hitPower)
            }

            return key
        }

        fun countAsBitsInKey(count: Long, hitPower: Int) =
            if (hitPower == 0) 0 else count shl ((hitPower - 1) * bitsInKeyPerCount)

        fun Long.countOfUnitsWithHitPower(hitPower: Int) =
            if (hitPower == 0) 0 else (this shr ((hitPower - 1) * bitsInKeyPerCount) and maxUnitsWithSameHitPower).toInt()

        /**
         * treats a unit with 0 hit power as if it has dice count 0
         */

        val Long.totalDiceCount: Int
            get() {
                var num = 0

                for (hitPower in 1..maxDiceSides) {
                    num += countOfUnitsWithHitPower(hitPower)
                }

                return num
            }

        val Long.hitPowerOfSingleUnit: Int
            get() {
                for (hitPower in 1..maxDiceSides) {
                    val count = countOfUnitsWithHitPower(hitPower)
                    Preconditions.checkState(count <= 1)

                    if (count == 1)
                        return hitPower
                }

                Preconditions.checkState(false, "hitPowerOfSingleUnit called for a key indicating zero units")
                return -1
            }

        /**
         * @return key for the distribution that is minimally weaker than the distribution <code>this</code> is
         * the key for resp. 0L (i.e. a key for the empty distribution), if <code>this</code> is also a key
         * for the empty distribution.
         *
         * The distribution that is minimally weaker than a given, non-empty distribution contains one count less
         * in the lowest hitPower level in which the given distribution has a count > 0.
         */

        val Long.dropLast: Long
            get() {
                for (hitPower in 1..maxDiceSides) {
                    val count = countOfUnitsWithHitPower(hitPower).toLong()

                    if (count > 0) {
                        var newKey = this
                        newKey = newKey xor countAsBitsInKey(count, hitPower) // remove bits of the current count
                        newKey =
                            newKey or countAsBitsInKey(count - 1, hitPower) // place bits of new count, i.e. count-1

                        return newKey
                    }
                }

                return 0 // key has not hit points, so dropping the last also results key without hit points
            }

        val Long.takeLast: Long
            get() {
                for (hitPower in 1..maxDiceSides) {
                    val count = countOfUnitsWithHitPower(hitPower).toLong()

                    if (count > 0) {
                        return countAsBitsInKey(1, hitPower)
                    }
                }

                Preconditions.checkState(false, "key representing one or more units required")
                return -1
            }

        val Char.hitPower: Int
            get() {
                Preconditions.checkArgument(this in '0'..'9' || this in 'A'..'C')
                return if (this <= '9') this - '0' else this - 'A' + 10
            }
    }
}
