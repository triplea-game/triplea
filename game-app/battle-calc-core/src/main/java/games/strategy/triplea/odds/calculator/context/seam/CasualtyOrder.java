package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
import java.util.Set;

/**
 * Preference only: a pure per-hit ranking over eligible buckets incl. damage level. Applied per
 * hit, so each onHit() migration feeds back — finish-vs-spread falls out of the order.
 *
 * <p>The default (non-OOL) order ranks by support-adjusted power — the power a unit's side loses
 * when it dies, folding in the support it receives and the support it gives others (engine {@code
 * CasualtyOrderOfLosses}) — supplied per profile in {@link ProfileStats#effectivePower()}, and
 * falls back to the base side-relative stat (attack on {@link Side#OFFENSE}, defense on {@link
 * Side#DEFENSE}) plus cost when none is supplied.
 */
public interface CasualtyOrder {
  CombatProfile next(Set<CombatProfile> eligible, ProfileStats stats, Side side);
}
