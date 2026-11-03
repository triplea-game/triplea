package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

/**
 * Read-only per-profile facts a {@link
 * games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder} ranks on. {@code cost} is the
 * per-profile TUV tiebreak. {@code effectivePower} is the side-relative power a profile is worth to
 * its force including support — the marginal power the force loses when one such unit dies (the
 * support it receives plus the support it gives others), mirroring the engine's {@code
 * CasualtyOrderOfLosses}. It is recomputed per firing exchange off the live force; an empty map
 * means "not supplied", so the order falls back to each profile's base attack/defense.
 */
public record ProfileStats(
    Map<CombatProfile, Integer> cost, Map<CombatProfile, Integer> effectivePower) {

  /** No support-adjusted power supplied: the order falls back to base attack/defense. */
  public ProfileStats(final Map<CombatProfile, Integer> cost) {
    this(cost, Map.of());
  }
}
