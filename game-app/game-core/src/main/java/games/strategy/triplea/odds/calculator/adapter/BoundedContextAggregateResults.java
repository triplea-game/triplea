package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import java.util.Collection;
import java.util.List;

/**
 * The bounded-context calc's results: the aggregate stats stay list-backed for now, but the
 * "average units remaining" accessors return the caller's original {@link Unit} instances via
 * {@link SurvivorMapper}, restoring the survivor identity that AI consumers depend on.
 */
public class BoundedContextAggregateResults extends AggregateResults {
  private final SurvivorMapper survivors;

  public BoundedContextAggregateResults(
      final List<BattleResults> bridged,
      final SimulationResults results,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    super(bridged);
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
}
