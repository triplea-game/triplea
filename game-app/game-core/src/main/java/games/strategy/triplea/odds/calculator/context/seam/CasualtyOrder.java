package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
import java.util.Set;

/**
 * Preference only: a pure per-hit ranking over eligible buckets incl. damage level. Applied per
 * hit, so each onHit() migration feeds back — finish-vs-spread falls out of the order.
 *
 * <p>The default (non-OOL) order ranks by side-relative power (attack on {@link Side#OFFENSE},
 * defense on {@link Side#DEFENSE}) plus cost; the engine's additional support-power interleave in
 * that order is a deferred 1a fidelity item the differential harness gates.
 */
public interface CasualtyOrder {
  CombatProfile next(Set<CombatProfile> eligible, ProfileStats stats, Side side);
}
