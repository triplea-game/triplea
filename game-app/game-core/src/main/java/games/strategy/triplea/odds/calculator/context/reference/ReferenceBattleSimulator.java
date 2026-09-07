package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.engine.random.IRandomSource;
import games.strategy.triplea.odds.calculator.context.model.AggregateResults;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.seam.BattleSimulator;

/** Reference per-round loop wiring the resolver, roller, allocator, and retreat seams together. */
public class ReferenceBattleSimulator implements BattleSimulator {
  @Override
  public AggregateResults simulate(
      final BattleScenario scenario, final int runCount, final IRandomSource rng) {
    throw new UnsupportedOperationException("phase 1");
  }
}
