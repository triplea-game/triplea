package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import java.util.Set;

/** Order-of-losses preference, falling back to the default order. */
public class OolCasualtyOrder implements CasualtyOrder {
  @Override
  public CombatProfile next(final Set<CombatProfile> eligible, final ProfileStats stats) {
    throw new UnsupportedOperationException("phase 1");
  }
}
