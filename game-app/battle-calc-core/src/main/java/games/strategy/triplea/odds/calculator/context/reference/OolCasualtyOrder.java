package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Order-of-losses preference, falling back to the engine default order when the OOL runs out. */
public class OolCasualtyOrder implements CasualtyOrder {

  private final List<UnitTypeId> orderedLosses;

  public OolCasualtyOrder(final List<UnitTypeId> orderedLosses) {
    this.orderedLosses = orderedLosses;
  }

  @Override
  public CombatProfile next(
      final Set<CombatProfile> eligible, final ProfileStats stats, final Side side) {
    for (final UnitTypeId type : orderedLosses) {
      final Optional<CombatProfile> listed =
          eligible.stream().filter(profile -> profile.type().equals(type)).findFirst();
      if (listed.isPresent()) {
        return listed.get();
      }
    }
    return eligible.stream().min(defaultOrder(side, stats)).orElseThrow();
  }

  /**
   * The engine default when no eligible type is in the OOL: weakest first by support-adjusted
   * power, cost as tiebreak — mirrors {@code DummyPlayer#selectCasualties}'s fall-through to {@code
   * CasualtyOrderOfLosses}, which ranks each unit by the power its force loses when it dies (the
   * support it receives and the support it gives others), not its raw stat. When {@link
   * ProfileStats#effectivePower()} carries no entry for a profile — no support in play, or a
   * damaged successor the exchange-start map never saw — it falls back to the profile's base
   * side-relative stat (attack on offense, defense on defense).
   */
  private static Comparator<CombatProfile> defaultOrder(final Side side, final ProfileStats stats) {
    return Comparator.comparingInt((final CombatProfile p) -> effectivePower(p, side, stats))
        .thenComparingInt(p -> stats.cost().getOrDefault(p, 0));
  }

  private static int effectivePower(
      final CombatProfile profile, final Side side, final ProfileStats stats) {
    final Integer supported = stats.effectivePower().get(profile);
    if (supported != null) {
      return supported;
    }
    return side == Side.OFFENSE ? profile.attack() : profile.defense();
  }
}
