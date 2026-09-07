package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import java.util.Set;

/**
 * Preference only: a pure per-hit ranking over eligible buckets incl. damage level. Applied per
 * hit, so each onHit() migration feeds back — finish-vs-spread falls out of the order.
 */
public interface CasualtyOrder {
  CombatProfile next(Set<CombatProfile> eligible, ProfileStats stats);
}
