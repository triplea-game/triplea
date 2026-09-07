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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * The count-backed {@link AggregateResults}: every statistic is computed straight from the
 * simulator's per-run survivor counts and the baked per-type cost, never from materialized {@code
 * Unit} objects. TUV therefore comes from {@code BattleScenario.cost()} (keyed by {@link
 * UnitTypeId}), so the {@code GameData}/player and cost-map parameters the old signatures carried
 * are unused here; they stay only for source compatibility.
 *
 * <p>Survivor <em>identity</em> — the caller's own units for "average units remaining" — is the one
 * thing counts cannot supply, so those two accessors delegate to {@link SurvivorMapper}.
 */
public class BoundedContextAggregateResults extends AggregateResults {
  private final List<BattleResult> runs;
  private final Map<UnitTypeId, Integer> cost;
  private final SurvivorMapper survivors;

  public BoundedContextAggregateResults(
      final SimulationResults results,
      final Map<UnitTypeId, Integer> cost,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    this.runs = results.results();
    this.cost = cost;
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
    final double attacker = mean(run -> true, run -> tuvOf(run.attackerSurvivors()));
    final double defender = mean(run -> true, run -> tuvOf(run.defenderSurvivors()));
    return Tuple.of(attacker, defender);
  }

  @Override
  public double getAverageTuvSwing(
      final GamePlayer attacker,
      final Collection<Unit> attackers,
      final GamePlayer defender,
      final Collection<Unit> defenders,
      final GameData data) {
    final int attackerStartingTuv = startingTuv(attackers);
    final int defenderStartingTuv = startingTuv(defenders);
    final double meanRemainingSwing =
        mean(run -> true, run -> tuvOf(run.attackerSurvivors()) - tuvOf(run.defenderSurvivors()));
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

  private int tuvOf(final Force force) {
    return force.counts().entrySet().stream()
        .filter(entry -> entry.getKey().state() != Lifecycle.DEAD)
        .mapToInt(entry -> cost.getOrDefault(entry.getKey().profile().type(), 0) * entry.getValue())
        .sum();
  }

  private int startingTuv(final Collection<Unit> units) {
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
}
