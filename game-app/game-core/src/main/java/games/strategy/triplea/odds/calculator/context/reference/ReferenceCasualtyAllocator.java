package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyAllocator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;

/**
 * Reference constraint enforcer: keep-one-land + dependent cascade over per-hit onHit() migrations.
 */
public class ReferenceCasualtyAllocator implements CasualtyAllocator {
  @Override
  public void allocate(
      final Force side,
      final int hits,
      final TargetFilter eligible,
      final Dependents deps,
      final Constraints constraints,
      final CasualtyOrder order) {
    throw new UnsupportedOperationException("phase 1");
  }
}
