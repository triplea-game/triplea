package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * The old-path {@link AggregateResults}: a container over the per-run {@link BattleResults} the
 * {@code MustFightBattle} simulator produces, deriving every statistic by iterating those unit
 * lists. It remains the shipping implementation and the only one that exposes {@link
 * #getResults()}.
 *
 * <p>This class does not restrict the added battle results to come from the same battle setup; if
 * that is desired, the caller must ensure the results added have that property.
 */
public class ListBackedAggregateResults extends AggregateResults {
  /**
   * -- GETTER -- Returns the stored battle results.
   *
   * <p>Note: The returned list is the encapsulated list and not a copy.
   */
  @Getter private final List<BattleResults> results;

  /**
   * Creates a new aggregator and sets the internal storage size to {@code expectedCount}. Choosing
   * a good estimate reduces the number of reallocations of the internal storage when adding
   * results.
   *
   * @param expectedCount number of expected results to store
   */
  public ListBackedAggregateResults(final int expectedCount) {
    results = new ArrayList<>(expectedCount);
  }

  /**
   * Creates a new aggregator and populates it with the battle results {@code results}. Further
   * results can later be added with the usual methods.
   *
   * @param results the battle results to add initially to this aggregator.
   */
  public ListBackedAggregateResults(final List<BattleResults> results) {
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

  @Override
  public Collection<Unit> getAverageAttackingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingAttackingUnits)
        .orElseGet(ArrayList::new);
  }

  @Override
  public Collection<Unit> getAverageDefendingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingDefendingUnits)
        .orElseGet(ArrayList::new);
  }

  @Override
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
    return Tuple.of(attackerTuvMean.getResult(), defenderTuvMean.getResult());
  }

  @Override
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
    final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();
    final IntegerMap<UnitType> attackerCostsForTuv = tuvCalculator.getCostsForTuv(attacker);
    final IntegerMap<UnitType> defenderCostsForTuv = tuvCalculator.getCostsForTuv(defender);
    final int attackerStartingTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderStartingTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    final Mean mean = new Mean();
    return defenderStartingTuv
        - attackerStartingTuv
        + mean.evaluate(
            results.stream()
                .mapToDouble(
                    result ->
                        TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv)
                            - TuvUtils.getTuv(
                                result.getRemainingDefendingUnits(), defenderCostsForTuv))
                .toArray());
  }

  @Override
  public double getAverageAttackingUnitsLeft() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  @Override
  public double getAverageAttackingUnitsLeftWhenAttackerWon() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .filter(BattleResults::attackerWon)
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  @Override
  public double getAverageDefendingUnitsLeft() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  @Override
  public double getAverageDefendingUnitsLeftWhenDefenderWon() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .filter(BattleResults::defenderWon)
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  @Override
  public double getAttackerWinPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(result -> result.attackerWon() ? 1 : 0).toArray());
  }

  @Override
  public double getDefenderWinPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(result -> result.defenderWon() ? 1 : 0).toArray());
  }

  @Override
  public double getDrawPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(results.stream().mapToDouble(result -> result.draw() ? 1 : 0).toArray());
  }

  @Override
  public double getAverageBattleRoundsFought() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(BattleResults::getBattleRoundsFought).toArray());
  }

  @Override
  public int getRollCount() {
    return results.size();
  }
}
