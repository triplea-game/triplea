package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.odds.calculator.context.model.AggregateResults;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;

/** Batch entry point: one call runs every simulation so a vectorized impl can batch them. */
public interface BattleSimulator {
  AggregateResults simulate(BattleScenario scenario, int runCount, IRandomSource rng);
}
