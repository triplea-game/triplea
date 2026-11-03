package games.strategy.triplea.odds.calculator.context;

import games.strategy.triplea.odds.calculator.context.reference.DiceHitRoller;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;

/**
 * Runs the shared {@link HitRoller} contract against the reference per-die {@link DiceHitRoller}.
 */
class DiceHitRollerContractTest extends HitRollerContractTest {

  @Override
  protected HitRoller newHitRoller() {
    return new DiceHitRoller();
  }
}
