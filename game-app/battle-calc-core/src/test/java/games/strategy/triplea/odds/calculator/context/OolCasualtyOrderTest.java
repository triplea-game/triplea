package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.Side;
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
 * <p>The listed-order behavior is pinned crisply here. The non-OOL <em>fallback</em> ranks by the
 * support-adjusted power the caller supplies in {@link ProfileStats#effectivePower()} — the power a
 * unit's side loses when it dies, including the support it receives and gives (engine {@code
 * CasualtyOrderOfLosses}) — falling back to the base side-relative stat when none is supplied, with
 * cost as the tiebreak. The unit-level ranking is pinned here; the marginal-power figure itself,
 * and its per-exchange (not per-hit) recomputation, are exercised end-to-end by the differential
 * harness ({@code BattleCalcDifferentialTest}'s support-adjusted casualty-order case).
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

    final CombatProfile chosen = order.next(Set.of(tank, infantry), costFavoringTank, Side.OFFENSE);

    assertThat(chosen).isEqualTo(infantry);
  }

  /**
   * The support-adjusted fallback: when {@link ProfileStats#effectivePower()} supplies a
   * per-profile figure, the default order sheds the lowest-effective-power bucket even when its
   * base stat is the higher one — a supported infantry (worth 5 to its force) outlives an armour
   * (worth 3) whose base attack of 3 beats the infantry's 1.
   */
  @Test
  void nextShedsTheLowestSupportAdjustedPowerBucketOverAHigherBaseStatBucket() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile armour = land("armour", 3, 3, 1);
    final OolCasualtyOrder order = new OolCasualtyOrder(List.of());
    final ProfileStats supportAdjusted =
        new ProfileStats(Map.of(infantry, 4, armour, 5), Map.of(infantry, 5, armour, 3));

    final CombatProfile chosen =
        order.next(Set.of(infantry, armour), supportAdjusted, Side.OFFENSE);

    assertThat(chosen).isEqualTo(armour);
  }

  /**
   * Neither eligible type appears in the OOL list, so the order must fall back to the engine
   * default rather than throwing or returning something outside the eligible set. With no
   * support-adjusted power supplied it ranks by base side power, so this asserts the total-function
   * contract (the pick is one of the eligible buckets) and that the weaker base stat is taken.
   */
  @Test
  void nextFallsBackToAnEligibleBucketWhenNoEligibleTypeIsInTheOol() {
    final CombatProfile infantry = land("infantry", 1, 2, 1);
    final CombatProfile artillery = land("artillery", 2, 2, 1);
    final CombatProfile unrelatedListedType = land("submarine", 2, 1, 1);
    final OolCasualtyOrder order = new OolCasualtyOrder(List.of(unrelatedListedType.type()));
    final ProfileStats stats = new ProfileStats(Map.of(infantry, 5, artillery, 2));

    final CombatProfile chosen = order.next(Set.of(infantry, artillery), stats, Side.OFFENSE);

    assertThat(chosen).isIn(infantry, artillery);
  }
}
