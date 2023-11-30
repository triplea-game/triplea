package games.strategy.engine.random;

import java.io.Serializable;
import lombok.Getter;

/**
 * Captures statistics for rolling a series of dice, including sum (total), average, median,
 * variance, and standard deviation.
 */
@Getter
public class DiceStatistic implements Serializable {
  private static final long serialVersionUID = -1422839840110240480L;

  private final double average;
  private final int total;
  private final double median;
  private final double stdDeviation;
  private final double variance;

  DiceStatistic(
      final double average,
      final int total,
      final double median,
      final double stdDeviation,
      final double variance) {
    this.average = average;
    this.total = total;
    this.median = median;
    this.stdDeviation = stdDeviation;
    this.variance = variance;
  }
}
