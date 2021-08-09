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
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/** A container for the results of multiple battle simulation runs. */
public class AggregateResults {
  private final List<BattleResults> results;
  @Getter @Setter private long time;

  public AggregateResults(final int expectedCount) {
    results = new ArrayList<>(expectedCount);
  }

  public AggregateResults(final List<BattleResults> results) {
    this.results = new ArrayList<>(results);
  }

  public void addResult(final BattleResults result) {
    results.add(result);
  }

  public void addResults(final Collection<BattleResults> results) {
    this.results.addAll(results);
  }

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

  /** First is Attacker, Second is Defender. */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    if (results.isEmpty()) {
      return Tuple.of(0.0, 0.0);
    }
    double attackerTuv = 0;
    double defenderTuv = 0;
    for (final BattleResults result : results) {
      attackerTuv += TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv);
      defenderTuv += TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv);
    }
    return Tuple.of(attackerTuv / results.size(), defenderTuv / results.size());
  }

  /**
   * Returns the standard deviation of the TUV of units left over after the battle. The first
   * component is Attacker, the second is Defender.
   */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOverStandardDeviation(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    if (results.isEmpty()) {
      return Tuple.of(0.0, 0.0);
    }
    final Tuple<Double, Double> mu =
        getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
    final Double muAttacker = mu.getFirst();
    final Double muDefender = mu.getSecond();
    double attackerTuvSqrSum = 0;
    double defenderTuvSqrSum = 0;
    for (final BattleResults result : results) {
      attackerTuvSqrSum +=
          Math.pow(
              TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv)
                  - muAttacker,
              2);
      defenderTuvSqrSum +=
          Math.pow(
              TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv)
                  - muDefender,
              2);
    }
    return Tuple.of(
        Math.sqrt(attackerTuvSqrSum / results.size()),
        Math.sqrt(defenderTuvSqrSum / results.size()));
  }

  /**
   * Returns the average TUV swing across all simulations of the battle.
   *
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
    if (results.isEmpty()) {
      return 0.0;
    }
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final int attackerTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    // could we possibly cause a bug by comparing UnitType's from one game data, to a different game
    // data's UnitTypes?
    final Tuple<Double, Double> average =
        getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
    final double attackerLost = attackerTuv - average.getFirst();
    final double defenderLost = defenderTuv - average.getSecond();
    return defenderLost - attackerLost;
  }

  /** Returns the standard deviation of the TUV swing. */
  public double getAverageTuvSwingStandardDeviation(
      final GamePlayer attacker, final GamePlayer defender, final GameData data) {
    if (results.isEmpty()) {
      return 0.0;
    }
    // The Variance is invariant under a constant shift.  Thus, the variance of
    // the TUV of left-over units and the variance of the TUV of lost units is
    // the same as they differ only be the (constant) TUV the units at the
    // start of the battle.
    // The TUV swing is defender-TUV minus attacker-TUV.  Sadly, the variance
    // of the difference of two sets cannot be calculated as the difference of
    // the variances as "they mix" (covariance).  Therefore, we have to
    // calculate the variance of the TUV swing "by hand".
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final Tuple<Double, Double> mu =
        getAverageTuvOfUnitsLeftOver(attackerCostsForTuv, defenderCostsForTuv);
    final Double muAttacker = mu.getFirst();
    final Double muDefender = mu.getSecond();
    double sqrSum = 0;
    for (final BattleResults result : results) {
      final double swing =
          -(TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv) - muDefender)
              + (TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv)
                  - muAttacker);
      sqrSum += swing * swing;
    }
    return Math.sqrt(sqrSum / results.size());
  }

  public double getAverageAttackingUnitsLeft() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .sum()
        / results.size();
  }

  /** Returns the standard deviation of the attacking units left. */
  double getAverageAttackingUnitsLeftStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final double mu = getAverageAttackingUnitsLeft();
    return Math.sqrt(
        results.stream()
                .map(BattleResults::getRemainingAttackingUnits)
                .mapToDouble(Collection::size)
                .map(x -> (x - mu) * (x - mu))
                .sum()
            / results.size());
  }

  public double getAverageAttackingUnitsLeftWhenAttackerWon() {
    if (results.isEmpty()) {
      return 0.0;
    }
    double count = 0;
    double total = 0;
    for (final BattleResults result : results) {
      if (result.attackerWon()) {
        count += result.getRemainingAttackingUnits().size();
        total += 1;
      }
    }
    if (total <= 0) {
      return 0;
    }
    return count / total;
  }

  /** Returns the standard deviation of the attacking units left if the attacker won. */
  double getAverageAttackingUnitsLeftWhenAttackerWonStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final double mu = getAverageAttackingUnitsLeftWhenAttackerWon();
    double count = 0;
    double total = 0;
    for (final BattleResults result : results) {
      if (result.attackerWon()) {
        count += Math.pow(result.getRemainingAttackingUnits().size() - mu, 2);
        total += 1;
      }
    }
    if (total <= 0) {
      return 0;
    }
    return Math.sqrt(count / total);
  }

  public double getAverageDefendingUnitsLeft() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .sum()
        / results.size();
  }

  /** Returns the standard deviation of the defending units left. */
  double getAverageDefendingUnitsLeftStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final double mu = getAverageDefendingUnitsLeft();
    return Math.sqrt(
        results.stream()
                .map(BattleResults::getRemainingDefendingUnits)
                .mapToDouble(Collection::size)
                .map(x -> (x - mu) * (x - mu))
                .sum()
            / results.size());
  }

  public double getAverageDefendingUnitsLeftWhenDefenderWon() {
    if (results.isEmpty()) {
      return 0.0;
    }
    double count = 0;
    double total = 0;
    for (final BattleResults result : results) {
      if (result.defenderWon()) {
        count += result.getRemainingDefendingUnits().size();
        total += 1;
      }
    }
    if (total <= 0) {
      return 0;
    }
    return count / total;
  }

  /** Returns the standard deviation of the defending units left if the defender won. */
  double getAverageDefendingUnitsLeftWhenDefenderWonStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final double mu = getAverageDefendingUnitsLeftWhenDefenderWon();
    double count = 0;
    double total = 0;
    for (final BattleResults result : results) {
      if (result.defenderWon()) {
        count += Math.pow(result.getRemainingDefendingUnits().size() - mu, 2);
        total += 1;
      }
    }
    if (total <= 0) {
      return 0;
    }
    return Math.sqrt(count / total);
  }

  public double getAttackerWinPercent() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream().filter(BattleResults::attackerWon).count() / (double) results.size();
  }

  /** Returns the standard deviation of the attacker wins percentage. */
  public double getAttackerWinStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    // Winning is a Bernoulli Experiment with propability p = getAttackerWinPercent(),
    // thus the variance is p(1-p).
    // [Or do the math yourself: w = number of wins, n = num of battles, p = w/n
    // Var = (w*(1-p)^2 + (n-w)*(0-p)^2)/n = ... = p(1-p)]
    final double p = getAttackerWinPercent();
    return Math.sqrt(p * (1 - p));
  }

  public double getDefenderWinPercent() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream().filter(BattleResults::defenderWon).count() / (double) results.size();
  }

  /** Returns the standard deviation of the defender wins percentage. */
  public double getDefenderWinStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    // Winning is a Bernoulli Experiment with propability p = getDefenderWinPercent(),
    // thus the variance is p(1-p).
    // [Or do the math yourself: w = number of wins, n = num of battles, p = w/n
    // Var = (w*(1-p)^2 + (n-w)*(0-p)^2)/n = ... = p(1-p)]
    final double p = getDefenderWinPercent();
    return Math.sqrt(p * (1 - p));
  }

  public double getDrawPercent() {
    if (results.isEmpty()) {
      return 0.0;
    }
    return results.stream().filter(BattleResults::draw).count() / (double) results.size();
  }

  /** Returns the standard deviation of the draw percentage. */
  public double getDrawStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    // Drawing is a Bernoulli Experiment with propability p = getDrawPercent(),
    // thus the variance is p(1-p).
    // [Or do the math yourself: w = number of wins, n = num of battles, p = w/n
    // Var = (w*(1-p)^2 + (n-w)*(0-p)^2)/n = ... = p(1-p)]
    final double p = getDrawPercent();
    return Math.sqrt(p * (1 - p));
  }

  /** Returns the average number of rounds fought across all simulations of the battle. */
  public double getAverageBattleRoundsFought() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final long count = results.stream().mapToInt(BattleResults::getBattleRoundsFought).sum();
    if (count == 0) {
      // If this is a 'fake' aggregate result, return 1.0
      return 1.0;
    }
    return count / (double) results.size();
  }

  /** Returns the standard deviation of the number of battle rounds. */
  public double getAverageBattleRoundsFoughtStandardDeviation() {
    if (results.isEmpty()) {
      return 0.0;
    }
    final double mu = getAverageBattleRoundsFought();
    final double count =
        results.stream()
            .mapToInt(BattleResults::getBattleRoundsFought)
            .mapToDouble(x -> (x - mu) * (x - mu))
            .sum();
    if (count == 0) {
      // If this is a 'fake' aggregate result, return 0.0
      return 0.0;
    }
    return count / (double) results.size();
  }

  public int getRollCount() {
    return results.size();
  }
}
