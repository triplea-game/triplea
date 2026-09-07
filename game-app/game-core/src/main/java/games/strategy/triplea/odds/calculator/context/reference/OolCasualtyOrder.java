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
   * The engine default when no eligible type is in the OOL: weakest first by side-relative power
   * (attack on offense, defense on defense), cost as tiebreak — mirrors {@code
   * DummyPlayer#selectCasualties}'s fall-through to {@code defaultCasualties}. The engine's
   * support-power interleave in that sort is a deferred fidelity item the differential harness
   * owns.
   */
  private static Comparator<CombatProfile> defaultOrder(final Side side, final ProfileStats stats) {
    return Comparator.comparingInt(
            (final CombatProfile p) -> side == Side.OFFENSE ? p.attack() : p.defense())
        .thenComparingInt(p -> stats.cost().getOrDefault(p, 0));
  }
}
