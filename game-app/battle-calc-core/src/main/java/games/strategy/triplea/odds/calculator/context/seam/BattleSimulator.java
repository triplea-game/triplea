package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;

/** Batch entry point: one call runs every simulation so a vectorized impl can batch them. */
public interface BattleSimulator {
  SimulationResults simulate(BattleScenario scenario, int runCount, RandomSource rng);
}
