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
import lombok.Setter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * A container for the results of multiple battle simulation runs.
 *
 * <p>This class wraps a collection of {@code BattleResult}s and provides methods to query certain
 * statistical properties over this set, e.g. the win probability, or the average number of units
 * left.
 *
 * <p>This class does not restrict the added battle result to come from the same battle setup. If
 * this is desired, the user must ensure that the results added have that property.
 */
@Getter
public class AggregateResults {
  /**
   * -- GETTER -- Returns the stored battle results.
   *
   * <p>Note: The returned list is the encapsulated list and not a copy.
   */
  private final List<BattleResults> results;

  @Setter private long time;

  /**
   * Creates a new aggregator and sets the internal storage size to {@code expectedCount}. Choosing
   * a good estimate reduces the number of reallocations of the internal storage when adding
   * results.
   *
   * @param expectedCount number of expected results to store
   */
  public AggregateResults(final int expectedCount) {
    results = new ArrayList<>(expectedCount);
  }

  /**
   * Creates a new aggregator and populates it with thr battle results {@code results}. Further
   * results can later be added with the usual methods.
   *
   * @param results the battle results to add initially to this aggregator.
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
   * <p>If no battle results were added to this aggregator instance, {@code (NaN, NaN)} is returned.
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
    return Tuple.of(attackerTuvMean.getResult(), defenderTuvMean.getResult());
  }

  /**
   * Returns the average TUV swing across all simulations of the battle.
   *
   * <p>A positive value indicates the defender lost more unit value, on average, than the attacker
   * (i.e. the attacker "won"). A negative value indicates the attacker lost more unit value, on
   * average, than the defender (i.e. the defender "won"). Zero indicates the attacker and defender
   * lost, on average, equal unit value (i.e. a tie).
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   *
   * <p>Note: The result of this function only makes sense for battles with the same initial setup,
   * e.g. results coming from simulations of the same battle. The {@code attackers} and {@code
   * defenders} are the units initially attacking and defending in this setup, respectively.
   *
   * <p>{@code attacker}, {@code defender} and {@code data} are used to derive the TUV values of the
   * units.
   *
   * @param attacker the attacking player
   * @param attackers a collection with the attacking units
   * @param defender the defending player
   * @param defenders a collection with the defending units
   * @param data the game data
   * @return the average TUV swing
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

  /**
   * Returns the average number of attacking units surviving the battles.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getAverageAttackingUnitsLeft() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /**
   * Returns the average number of attacking units surviving the battles restricted to the battles
   * where the attacker won.
   *
   * <p>If no battle results were added to this aggregator instance or if the attacker did not win
   * any of those battles, then {@code NaN} is returned.
   */
  public double getAverageAttackingUnitsLeftWhenAttackerWon() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .filter(BattleResults::attackerWon)
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /**
   * Returns the average number of defending units surviving the battles.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getAverageDefendingUnitsLeft() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /**
   * Returns the average number of defending units surviving the battles restricted to the battles
   * where the defender won.
   *
   * <p>If no battle results were added to this aggregator instance or if the defender did not win
   * any of those battles, then {@code NaN} is returned.
   */
  public double getAverageDefendingUnitsLeftWhenDefenderWon() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .filter(BattleResults::defenderWon)
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /**
   * Returns the average number of battles won by the attacker. This can be interpreted as the
   * probability that the attacker wins given all aggregated battles started with the same initial
   * setup.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getAttackerWinPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(result -> result.attackerWon() ? 1 : 0).toArray());
  }

  /**
   * Returns the average number of battles won by the defender. This can be interpreted as the
   * probability that the defender wins given all aggregated battles started with the same initial
   * setup.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getDefenderWinPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(result -> result.defenderWon() ? 1 : 0).toArray());
  }

  /**
   * Returns the average number of battles drawn. This can be interpreted as the probability that a
   * battle ends in a draw given all aggregated battles started with the same initial setup.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getDrawPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(results.stream().mapToDouble(result -> result.draw() ? 1 : 0).toArray());
  }

  /**
   * Returns the average number of rounds fought across all simulations of the battle.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getAverageBattleRoundsFought() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(BattleResults::getBattleRoundsFought).toArray());
  }

  /** Returns the number of battles aggregated by this instance. */
  public int getRollCount() {
    return results.size();
  }
}
