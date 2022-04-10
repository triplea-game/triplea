package games.strategy.engine.random;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import lombok.Getter;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/** A source of random numbers that uses a pseudorandom number generator. */
@ThreadSafe
public final class PlainRandomSource implements IRandomSource {
  private final Object lock = new Object();

  @GuardedBy("lock")
  private final RandomGenerator random = new MersenneTwister();

  @Override
  public int[] getRandom(final int max, final int count, final String annotation) {
    checkArgument(max > 0, "max must be > 0 (%s)", annotation);
    checkArgument(count > 0, "count must be > 0 (%s)", annotation);

    if (count == 1) {
      return new int[] {getRandom(max, annotation)};
    }

    final int[] numbers = new int[count]; // initialize return array of drawn numbers
    // get base power results (compute if needed)
    BasePowers basePowers = basePowersMap.get(max);
    if (basePowers == null) {
      basePowers = new BasePowers((int) Math.abs(Math.log(Integer.MAX_VALUE) / Math.log(max)), max);
      basePowersMap.put(max, basePowers);
    }
    final int basePowerMax = basePowers.getBasePowerMax();
    int restCount = count;
    int diceIdBase = 0;
    while (restCount > 0) {
      final int powerCur = Math.min(basePowerMax, restCount); // = random numbers to draw now
      int randomInt = getRandom(basePowers.getPowerResult(powerCur), annotation);
      // Get individual dice x by projecting from range 1 until base^x to 0 until (base-1) for
      // each x via modular arithmetic from the largest power to the smallest
      // Note: x is diceIdOffset
      for (int diceIdOffset = powerCur - 1; diceIdOffset >= 0; --diceIdOffset) {
        final int powerResult = basePowers.getPowerResult(diceIdOffset);
        final int powerFit = randomInt / powerResult;
        numbers[diceIdBase + diceIdOffset] = powerFit;
        randomInt -= powerFit * powerResult;
      }
      diceIdBase += powerCur;
      restCount -= powerCur; // newly remaining random numbers to be drawn
    }
    return numbers;
  }

  @Override
  public int getRandom(final int max, final String annotation) {
    checkArgument(max > 0, "max must be > 0 (%s)", annotation);

    synchronized (lock) {
      return random.nextInt(max);
    }
  }

  /**
   * Maps base to its {@link BasePowers} to avoid repeated computation. Each {@link
   * BasePowers#powerResults} represents the limit of a range (1-base^x) of all possible
   * combinations of x numbers between 0 and the base-1 (base range). Therefore, drawing one random
   * number in this range corresponds to drawing x random numbers in the base range.
   */
  private static final Map<Integer, BasePowers> basePowersMap = new HashMap<>();
  /**
   * An instances stores the calculation results of
   *
   * <pre>{@code
   * base^x
   * }</pre>
   *
   * for any power x <= {@link BasePowers#basePowerMax} to avoid repeated computation.
   */
  static class BasePowers {
    @Getter final int basePowerMax;
    final int[] powerResults;

    BasePowers(final int basePowerMax, final int base) {
      this.basePowerMax = basePowerMax;
      powerResults = new int[basePowerMax + 1];
      powerResults[0] = 1; // calculate the power results until the basePowerMax
      for (int power = 1; power <= basePowerMax; ++power) {
        powerResults[power] = powerResults[power - 1] * base;
      }
    }

    /**
     * Lookup of result of
     *
     * <pre>{@code
     * base^power
     * }</pre>
     *
     * @param power Power to be used for lookup
     * @return Looked up (pre-calculated) value
     */
    public final int getPowerResult(final int power) {
      return powerResults[power];
    }
  }
}
