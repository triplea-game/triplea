package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Outcome;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * The count-backed {@link AggregateResults}: every statistic is computed straight from the
 * simulator's per-run survivor counts, never from materialized {@code Unit} objects. TUV is valued
 * per side by that side's own cost schedule — matching the old path — so a map with per-nation or
 * XML-overridden costs is scored correctly rather than collapsing both sides onto one merged map.
 *
 * <p>Survivor <em>identity</em> — the caller's own units for "average units remaining" — is the one
 * thing counts cannot supply, so those two accessors delegate to {@link SurvivorMapper}.
 */
public class BoundedContextAggregateResults extends AggregateResults {
  private final List<BattleResult> runs;
  private final SurvivorMapper survivors;

  public BoundedContextAggregateResults(
      final SimulationResults results,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    this.runs = results.results();
    this.survivors = new SurvivorMapper(results, attacking, defending);
  }

  @Override
  public Collection<Unit> getAverageAttackingUnitsRemaining() {
    return survivors.attackerSurvivors();
  }

  @Override
  public Collection<Unit> getAverageDefendingUnitsRemaining() {
    return survivors.defenderSurvivors();
  }

  @Override
  public double getAttackerWinPercent() {
    return outcomeShare(Outcome.ATTACKER_WINS);
  }

  @Override
  public double getDefenderWinPercent() {
    return outcomeShare(Outcome.DEFENDER_WINS);
  }

  @Override
  public double getDrawPercent() {
    return outcomeShare(Outcome.DRAW);
  }

  @Override
  public double getAverageAttackingUnitsLeft() {
    return mean(run -> true, run -> survivorCount(run.attackerSurvivors()));
  }

  @Override
  public double getAverageAttackingUnitsLeftWhenAttackerWon() {
    return mean(
        run -> run.outcome() == Outcome.ATTACKER_WINS,
        run -> survivorCount(run.attackerSurvivors()));
  }

  @Override
  public double getAverageDefendingUnitsLeft() {
    return mean(run -> true, run -> survivorCount(run.defenderSurvivors()));
  }

  @Override
  public double getAverageDefendingUnitsLeftWhenDefenderWon() {
    return mean(
        run -> run.outcome() == Outcome.DEFENDER_WINS,
        run -> survivorCount(run.defenderSurvivors()));
  }

  @Override
  public double getAverageBattleRoundsFought() {
    return mean(run -> true, BattleResult::roundsFought);
  }

  @Override
  public int getRollCount() {
    return runs.size();
  }

  @Override
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    final Map<UnitTypeId, Integer> attackerCost = byTypeId(attackerCostsForTuv);
    final Map<UnitTypeId, Integer> defenderCost = byTypeId(defenderCostsForTuv);
    final double attacker = mean(run -> true, run -> tuvOf(run.attackerSurvivors(), attackerCost));
    final double defender = mean(run -> true, run -> tuvOf(run.defenderSurvivors(), defenderCost));
    return Tuple.of(attacker, defender);
  }

  @Override
  public double getAverageTuvSwing(
      final GamePlayer attacker,
      final Collection<Unit> attackers,
      final GamePlayer defender,
      final Collection<Unit> defenders,
      final GameData data) {
    final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();
    final Map<UnitTypeId, Integer> attackerCost = byTypeId(tuvCalculator.getCostsForTuv(attacker));
    final Map<UnitTypeId, Integer> defenderCost = byTypeId(tuvCalculator.getCostsForTuv(defender));
    final int attackerStartingTuv = startingTuv(attackers, attackerCost);
    final int defenderStartingTuv = startingTuv(defenders, defenderCost);
    final double meanRemainingSwing =
        mean(
            run -> true,
            run ->
                tuvOf(run.attackerSurvivors(), attackerCost)
                    - tuvOf(run.defenderSurvivors(), defenderCost));
    return defenderStartingTuv - attackerStartingTuv + meanRemainingSwing;
  }

  private double outcomeShare(final Outcome outcome) {
    // NaN on an empty batch, matching the list-backed mean-over-nothing behavior.
    return mean(run -> true, run -> run.outcome() == outcome ? 1 : 0);
  }

  /** Mean of {@code value} over the runs matching {@code filter}; NaN when none match. */
  private double mean(
      final Predicate<BattleResult> filter, final ToDoubleFunction<BattleResult> value) {
    return new Mean().evaluate(runs.stream().filter(filter).mapToDouble(value).toArray());
  }

  private static int tuvOf(final Force force, final Map<UnitTypeId, Integer> cost) {
    return force.counts().entrySet().stream()
        .filter(entry -> entry.getKey().state() != Lifecycle.DEAD)
        .mapToInt(entry -> cost.getOrDefault(entry.getKey().profile().type(), 0) * entry.getValue())
        .sum();
  }

  private static int startingTuv(
      final Collection<Unit> units, final Map<UnitTypeId, Integer> cost) {
    return units.stream()
        .mapToInt(unit -> cost.getOrDefault(new UnitTypeId(unit.getType().getName()), 0))
        .sum();
  }

  private static int survivorCount(final Force force) {
    return force.counts().entrySet().stream()
        .filter(entry -> entry.getKey().state() != Lifecycle.DEAD)
        .mapToInt(Map.Entry::getValue)
        .sum();
  }

  /** Re-keys an engine {@code UnitType -> cost} schedule onto the calc's {@link UnitTypeId}. */
  private static Map<UnitTypeId, Integer> byTypeId(final IntegerMap<UnitType> costs) {
    final Map<UnitTypeId, Integer> byType = new HashMap<>();
    for (final UnitType type : costs.keySet()) {
      byType.put(new UnitTypeId(type.getName()), costs.getInt(type));
    }
    return byType;
  }
}
