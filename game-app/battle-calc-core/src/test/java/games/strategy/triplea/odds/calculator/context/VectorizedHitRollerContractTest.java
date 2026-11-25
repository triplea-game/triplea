package games.strategy.triplea.odds.calculator.context;

import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import games.strategy.triplea.odds.calculator.context.vector.VectorizedHitRoller;

/**
 * Runs the shared {@link HitRoller} contract against the batched-draw {@link VectorizedHitRoller},
 * proving it honors the same dice/low-luck rule as the reference roller.
 */
class VectorizedHitRollerContractTest extends HitRollerContractTest {

  @Override
  protected HitRoller newHitRoller() {
    return new VectorizedHitRoller();
  }
}
