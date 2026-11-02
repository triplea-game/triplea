package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;

/**
 * Constraint enforcer: applies hits as onHit() migrations while owning every hard rule —
 * keep-one-land and dependent cascade — so swapping a preference can never break correctness.
 * Returns the post-hit force rather than mutating; {@code firingMode} carries the dependent-cascade
 * timing (IMMEDIATE removes cargo now, DEFERRED lets it fire this round then die at reconcile).
 */
public interface CasualtyAllocator {
  Force allocate(
      Force force,
      int hits,
      TargetFilter eligible,
      Dependents deps,
      Constraints constraints,
      CasualtyOrder order,
      FiringMode firingMode,
      Side side,
      ProfileStats stats);
}
