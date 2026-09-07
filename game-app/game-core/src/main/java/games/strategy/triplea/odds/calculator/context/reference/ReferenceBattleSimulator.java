package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.seam.BattleSimulator;
import games.strategy.triplea.odds.calculator.context.seam.RandomSource;

/** Reference per-round loop wiring the resolver, roller, allocator, and retreat seams together. */
public class ReferenceBattleSimulator implements BattleSimulator {
  @Override
  public SimulationResults simulate(
      final BattleScenario scenario, final int runCount, final RandomSource rng) {
    throw new UnsupportedOperationException("phase 1");
  }
}
