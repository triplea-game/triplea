package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * A container for the results of multiple battle simulation runs.
 *
 * <p>This class wraps a collection of {@code BattleResult}s and provides methods to query certain
 * statistical properties, e.g. the win probability, or the standard deviation for the number of
 * unit left.
 *
 * <p>This class does not restrict the battle result to come from the same battle setup. If this is
 * desired, the user must ensure that this property hold.
 *
 * <p>Note on bias correction: Given a set of data points there are different variances (or standard
 * deviations). If the data set for which the variance is calculated contains not all possible
 * observations/states, i.e. is a subset, then bias correction should be applied. This is because
 * the variance needs the expected value/mean, however, since we do not have all observations the
 * calculated mean has an error which propagates to the variance. To mitigate this, a bias corrected
 * variance is used. For more information see any book about statistics or [1].
 *
 * <p>[1] https://en.wikipedia.org/wiki/Variance#Population_variance_and_sample_variance
 */
public class AggregateResults {
  private final List<BattleResults> results;
  @Getter @Setter private long time;

  /**
   * Creates a new aggregator and sets the internal storage size to {@code expectedCount}. Choosing
   * a good estimate reduces the number of reallocations of the internal storage once it hits its
   * limit.
   *
   * @param expectedCount number of expected results to store
   */
  public AggregateResults(final int expectedCount) {
    results = new ArrayList<>(expectedCount);
  }

  /**
   * Creates a new aggregator and populates it with thr battle results {@code results}.
   *
   * @param results the battle results to add to this aggregator.
   */
  public AggregateResults(final List<BattleResults> results) {
    this.results = new ArrayList<>(results);
  }

  /**
   * Add the battle result {@code result} to this aggregator.
   *
   * @param result the battle result to add.
   */
  public void addResult(final BattleResults result) {
    results.add(result);
  }

  /**
   * Add all battle results in {@code results} to this aggregator.
   *
   * @param results the battle results to add
   */
  public void addResults(final Collection<BattleResults> results) {
    this.results.addAll(results);
  }

  /**
   * Returns the stored battle results.
   *
   * <p>Note: The returned list is the encapsulated list and not a copy.
   *
   * @return list of all battle results
   */
  public List<BattleResults> getResults() {
    return results;
  }

  private Optional<BattleResults> getBattleResultsClosestToAverage() {
    return results.stream()
        .min(
            Comparator.comparingDouble(
                result ->
                    Math.abs(
                            result.getRemainingAttackingUnits().size()
                                - getAverageAttackingUnitsLeft())
                        + Math.abs(
                            result.getRemainingDefendingUnits().size()
                                - getAverageDefendingUnitsLeft())));
  }

  public Collection<Unit> getAverageAttackingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingAttackingUnits)
        .orElseGet(ArrayList::new);
  }

  public Collection<Unit> getAverageDefendingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingDefendingUnits)
        .orElseGet(ArrayList::new);
  }

  /**
   * Returns the average TUV value of the units left over.
   *
   * @param attackerCostsForTuv lookup table assigning the TUV value to the attacking units
   * @param defenderCostsForTuv lookup table assigning the TUV value to the defending units
   * @return tuple of the average TUV value of the units left over. First is Attacker, Second is
   *     Defender.
   */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    final Mean attackerTuvMean = new Mean();
    final Mean defenderTuvMean = new Mean();
    for (final BattleResults result : results) {
      attackerTuvMean.increment(
          TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv));
      defenderTuvMean.increment(
          TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv));
    }
    final double attackerM = attackerTuvMean.getResult();
    final double defenderM = defenderTuvMean.getResult();
    // If there are no battles, mean.getResult() returns Double.NaN.
    return Tuple.of(
        Double.isNaN(attackerM) ? 0 : attackerM, Double.isNaN(defenderM) ? 0 : defenderM);
  }

  /**
   * Returns the standard deviation of the TUV of units left over after the battle. The first
   * component is Attacker, the second is Defender.
   *
   * @param attackerCostsForTuv lookup table assigning the TUV value to the attacking units
   * @param defenderCostsForTuv lookup table assigning the TUV value to the defending units
   * @return tuple of the standard deviation of the TUV value of the units left over. First is
   *     Attacker, Second is Defender.
   */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOverStandardDeviation(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation attackerTuvStdDev = new StandardDeviation(true);
    final StandardDeviation defenderTuvStdDev = new StandardDeviation(true);
    for (final BattleResults result : results) {
      attackerTuvStdDev.increment(
          TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv));
      defenderTuvStdDev.increment(
          TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv));
    }
    final double attackerS = attackerTuvStdDev.getResult();
    final double defenderS = defenderTuvStdDev.getResult();
    // If there are no battles, stdDev.getResult() returns Double.NaN.
    return Tuple.of(
        Double.isNaN(attackerS) ? 0 : attackerS, Double.isNaN(defenderS) ? 0 : defenderS);
  }

  /**
   * Returns the average TUV swing across all simulations of the battle.
   *
   * <p>Note: The result of this function only makes sense for battle with the same initial setup,
   * e.g. results coming from simulations of the same battle. The {@code attackers} and {@code
   * defenders} are the units attacking and defending in this setup, respectively.
   *
   * <p>{@code attacker}, {@code defender} and {@code data} are used to derive the TUV values of the
   * units.
   *
   * @param attacker the attacking player
   * @param attackers a collection with the attacking units
   * @param defender the defending player
   * @param defenders a collection with the defending units
   * @param data the game data
   * @return A positive value indicates the defender lost more unit value, on average, than the
   *     attacker (i.e. the attacker "won"). A negative value indicates the attacker lost more unit
   *     value, on average, than the defender (i.e. the defender "won"). Zero indicates the attacker
   *     and defender lost, on average, equal unit value (i.e. a tie).
   */
  public double getAverageTuvSwing(
      final GamePlayer attacker,
      final Collection<Unit> attackers,
      final GamePlayer defender,
      final Collection<Unit> defenders,
      final GameData data) {
    // The TUV swing is defenderTuvLost - attackerTuvLost and tuvLost = startingTuv - remainingTuv.
    // Thus, the TUV swing of a singe battle is:
    // TUV swing = defenderStartingTuv - attackerStartingTuv - defenderRemainingTuv +
    // attackerRemainingTuv
    //
    // Because mean(x_i+c) = mean(x_i)+c for a constant c - the startingTuv in this case - we save
    // some computations and add the startingTuv after we have calculated the mean.
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final int attackerStartingTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderStartingTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream()
                .mapToDouble(
                    result ->
                        TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv)
                            - TuvUtils.getTuv(
                                result.getRemainingDefendingUnits(), defenderCostsForTuv))
                .toArray());
    // If there are no battles, stdDev.getResult() returns Double.NaN.
    return Double.isNaN(m) ? 0 : defenderStartingTuv - attackerStartingTuv + m;
  }

  /**
   * Returns the standard deviation of the TUV swing.
   *
   * <p>Note: The result of this function only makes sense for battle with the same initial setup,
   * e.g. results coming from simulations of the same battle. The {@code attackers} and {@code
   * defenders} are the units attacking and defending in this setup, respectively.
   *
   * <p>{@code attacker}, {@code defender} and {@code data} are used to derive the TUV values of the
   * units.
   *
   * @param attacker the attacking player
   * @param defender the defending player
   * @param data the game data
   * @return the standard deviation for the TUV swing of the battles.
   */
  public double getAverageTuvSwingStandardDeviation(
      final GamePlayer attacker, final GamePlayer defender, final GameData data) {
    // The variance is invariant under a constant shift.  Thus, the variance of
    // the TUV of left-over units and the variance of the TUV of lost units is
    // the same as they differ only be the (constant) total TUV the units at
    // the start of the battle.
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation stdDev = new StandardDeviation(true);
    final double s =
        stdDev.evaluate(
            results.stream()
                .mapToDouble(
                    result ->
                        TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv)
                            - TuvUtils.getTuv(
                                result.getRemainingDefendingUnits(), defenderCostsForTuv))
                .toArray());
    return Double.isNaN(s) ? 0 : s;
  }

  /**
   * Returns the average number of attacking units surviving the battles.
   *
   * @return the average of surviving attacking units
   */
  public double getAverageAttackingUnitsLeft() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream()
                .map(BattleResults::getRemainingAttackingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the attacking units left.
   *
   * @return the standard deviation of the attacking units left.
   */
  double getAverageAttackingUnitsLeftStandardDeviation() {
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation stdDev = new StandardDeviation(true);
    final double s =
        stdDev.evaluate(
            results.stream()
                .map(BattleResults::getRemainingAttackingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, stdDev.evaluate() returns Double.NaN.
    return Double.isNaN(s) ? 0 : s;
  }

  /**
   * Returns the average number of attacking units surviving the battles restricted to the battles
   * where the attacker won.
   *
   * @return the average of surviving attacking units or 0 if the attacker did not win any battles.
   */
  public double getAverageAttackingUnitsLeftWhenAttackerWon() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream()
                .filter(BattleResults::attackerWon)
                .map(BattleResults::getRemainingAttackingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the number of attacking units surviving the battles
   * restricted to the battles where the attacker won.
   *
   * @return the standard deviation of surviving attacking units or 0 if the attacker did not win
   *     any battles.
   */
  double getAverageAttackingUnitsLeftWhenAttackerWonStandardDeviation() {
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation stdDev = new StandardDeviation(true);
    final double s =
        stdDev.evaluate(
            results.stream()
                .filter(BattleResults::attackerWon)
                .map(BattleResults::getRemainingAttackingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, stdDev.evaluate() returns Double.NaN.
    return Double.isNaN(s) ? 0 : s;
  }

  /**
   * Returns the average number of defending units surviving the battles.
   *
   * @return the average of surviving defending units
   */
  public double getAverageDefendingUnitsLeft() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream()
                .map(BattleResults::getRemainingDefendingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the defending units left.
   *
   * @return the standard deviation of the defending units left.
   */
  double getAverageDefendingUnitsLeftStandardDeviation() {
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation stdDev = new StandardDeviation(true);
    final double s =
        stdDev.evaluate(
            results.stream()
                .map(BattleResults::getRemainingDefendingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, stdDev.evaluate() returns Double.NaN.
    return Double.isNaN(s) ? 0 : s;
  }

  /**
   * Returns the average number of defending units surviving the battles restricted to the battles
   * where the defender won.
   *
   * @return the average of surviving defending units or 0 if the defender did not win any battles.
   */
  public double getAverageDefendingUnitsLeftWhenDefenderWon() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream()
                .filter(BattleResults::defenderWon)
                .map(BattleResults::getRemainingDefendingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the number of defending units surviving the battles
   * restricted to the battles where the defender won.
   *
   * @return the standard deviation of surviving defending units or 0 if the defender did not win
   *     any battles.
   */
  double getAverageDefendingUnitsLeftWhenDefenderWonStandardDeviation() {
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation stdDev = new StandardDeviation(true);
    final double s =
        stdDev.evaluate(
            results.stream()
                .filter(BattleResults::defenderWon)
                .map(BattleResults::getRemainingDefendingUnits)
                .mapToDouble(Collection::size)
                .toArray());
    // If there are no battles, stdDev.evaluate() returns Double.NaN.
    return Double.isNaN(s) ? 0 : s;
  }

  /**
   * Returns the average number of battles won by the attacker. This can be interpreted as the
   * probability that the attacker wins given all aggregated battles started with the same initial
   * setup.
   *
   * @return the average number of battles won by the attacker
   */
  public double getAttackerWinPercent() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream().mapToDouble(result -> result.attackerWon() ? 1 : 0).toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the number of battles won by the attacker.
   *
   * @return the standard deviation of the number of battles won by the attacker
   */
  public double getAttackerWinStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    // Note that Winning is a Bernoulli Experiment with probability
    // p = getAttackerWinPercent(), thus the variance is p(1-p).
    // [Or do the math yourself: w = number of wins, n = num of battles, p = w/n
    // Var = (w*(1-p)^2 + (n-w)*(0-p)^2)/n = ... = p(1-p)]
    final double p = getAttackerWinPercent();
    return AggregateResults.getBernoulliExperimentStandardDeviation(p);
  }

  /**
   * Returns the average number of battles won by the defender. This can be interpreted as the
   * probability that the defender wins given all aggregated battles started with the same initial
   * setup.
   *
   * @return the average number of battles won by the defender
   */
  public double getDefenderWinPercent() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(
            results.stream().mapToDouble(result -> result.defenderWon() ? 1 : 0).toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the number of battles won by the defender.
   *
   * @return the standard deviation of the number of battles won by the defender
   */
  public double getDefenderWinStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    // Note that Winning is a Bernoulli Experiment with probability
    // p = getDefenderWinPercent(), thus the variance is p(1-p).
    // [Or do the math yourself: w = number of wins, n = num of battles, p = w/n
    // Var = (w*(1-p)^2 + (n-w)*(0-p)^2)/n = ... = p(1-p)]
    final double p = getDefenderWinPercent();
    return AggregateResults.getBernoulliExperimentStandardDeviation(p);
  }

  /**
   * Returns the average number of battles drawn. This can be interpreted as the probability that a
   * battle ends in a draw given all aggregated battles started with the same initial setup.
   *
   * @return the average number of battles won by the defender
   */
  public double getDrawPercent() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(results.stream().mapToDouble(result -> result.draw() ? 1 : 0).toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the number of battles drawn.
   *
   * @return the standard deviation of the number of battles drawn.
   */
  public double getDrawStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    // Note that Drawing is a Bernoulli Experiment with probability
    // p = getDrawPercent(), thus the variance is p(1-p).
    // [Or do the math yourself: w = number of wins, n = num of battles, p = w/n
    // Var = (w*(1-p)^2 + (n-w)*(0-p)^2)/n = ... = p(1-p)]
    final double p = getDrawPercent();
    return AggregateResults.getBernoulliExperimentStandardDeviation(p);
  }

  /**
   * Returns the standard deviation for a Bernoulli Experiment ("coin flip") with probability {@code
   * p}. The formula for the standard variance is
   *
   * <pre>
   * Var = sqrt(p*(1-p))
   * </pre>
   *
   * See: https://en.wikipedia.org/wiki/Bernoulli_distribution
   */
  private static double getBernoulliExperimentStandardDeviation(final double p) {
    return Math.sqrt(p * (1 - p));
  }

  /**
   * Returns the average number of rounds fought across all simulations of the battle.
   *
   * @return the average number of rounds fought
   */
  public double getAverageBattleRoundsFought() {
    final Mean mean = new Mean();
    final double m =
        mean.evaluate(results.stream().mapToDouble(BattleResults::getBattleRoundsFought).toArray());
    // If there are no battles, mean.evaluate() returns Double.NaN.
    return Double.isNaN(m) ? 0 : m;
  }

  /**
   * Returns the standard deviation of the number of battle rounds.
   *
   * @return the standard deviation of the number of battle rounds.
   */
  public double getAverageBattleRoundsFoughtStandardDeviation() {
    // The 'true' argument means calculate the variance using bias correction.
    final StandardDeviation stdDev = new StandardDeviation(true);
    final double s =
        stdDev.evaluate(
            results.stream().mapToDouble(BattleResults::getBattleRoundsFought).toArray());
    // If there are no battles, stdDev.evaluate() returns Double.NaN.
    return Double.isNaN(s) ? 0 : s;
  }

  /**
   * Returns the number of battles aggregated by this instance.
   *
   * @return number of aggregated battles
   */
  public int getRollCount() {
    return results.size();
  }
}
