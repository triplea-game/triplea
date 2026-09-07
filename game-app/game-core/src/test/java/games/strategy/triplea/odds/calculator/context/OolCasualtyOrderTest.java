package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.reference.OolCasualtyOrder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Contract test for the real {@link OolCasualtyOrder} (design §4: {@code CasualtyOrder} is a pure,
 * hit-type-blind preference). Mirrors {@code DummyPlayer#selectCasualties} lines 223-244: an
 * order-of-losses list is consumed first, in listed order, and any type it doesn't cover falls back
 * to the engine default order. RED at runtime by design — the impl is still a throwing "phase 1"
 * stub.
 *
 * <p>The frozen contract gives the fallback nothing to rank on but {@link ProfileStats}'s cost map,
 * so these tests treat "default order" as ascending cost (cheapest dies first) — the only ordering
 * the seam can currently express. See the report: this is a guess standing in for whatever {@code
 * CasualtyDetails}' actual default-order algorithm turns out to need.
 */
class OolCasualtyOrderTest {

  /** OOL entries take priority over cost, even when cost would rank the other way. */
  @Test
  void nextReturnsTheOolListedTypeAheadOfACheaperUnlistedTypeAlternative() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile tank = land("tank", 3, 3, 2);
    final OolCasualtyOrder order = new OolCasualtyOrder(List.of(infantry.type(), tank.type()));
    // Cost says tank should die first if this were a pure cost ranking; the OOL says otherwise.
    final ProfileStats costFavoringTank = new ProfileStats(Map.of(infantry, 5, tank, 1));

    final CombatProfile chosen = order.next(Set.of(tank, infantry), costFavoringTank);

    assertThat(chosen).isEqualTo(infantry);
  }

  /**
   * Neither eligible type appears in the OOL list, so the order must fall back to the engine
   * default (cost-ascending) rather than throwing or picking arbitrarily.
   */
  @Test
  void nextFallsBackToCheapestFirstWhenNoEligibleTypeIsInTheOol() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile artillery = land("artillery", 2, 2, 1);
    final CombatProfile unrelatedListedType = land("submarine", 2, 1, 1);
    final OolCasualtyOrder order = new OolCasualtyOrder(List.of(unrelatedListedType.type()));
    final ProfileStats stats = new ProfileStats(Map.of(infantry, 5, artillery, 2));

    final CombatProfile chosen = order.next(Set.of(infantry, artillery), stats);

    assertThat(chosen).isEqualTo(artillery);
  }
}
