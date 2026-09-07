package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;

/**
 * Constraint enforcer: applies hits as onHit() migrations while owning every hard rule —
 * keep-one-land and dependent cascade — so swapping a preference can never break correctness.
 */
public interface CasualtyAllocator {
  void allocate(
      Force side,
      int hits,
      TargetFilter eligible,
      Dependents deps,
      Constraints constraints,
      CasualtyOrder order);
}
