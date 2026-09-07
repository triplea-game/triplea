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
 * <p>The listed-order behavior is pinned crisply here. The non-OOL <em>fallback</em> is not: the
 * engine default is NOT cost-ascending. It is {@code CasualtyOrderOfLosses}, a power/TUV-efficiency
 * sort — it repeatedly takes the lowest-combat-power unit (including the support power that unit
 * grants others), breaking ties by {@code UnitBattleComparator} (cost among them), interleaving
 * types so support is preserved. So this test asserts only what is rule-true of the fallback (it
 * returns an eligible bucket, totally and deterministically); the exact fallback ordering is owned
 * by the differential harness ({@code BattleCalcDifferentialTest}, the mixed-type no-OOL {@code
 * alwaysHits} case), not pinned to a cost guess here.
 *
 * <p>{@code next} now takes the battle {@link Side} so the default order can rank by side-relative
 * power (attack on offense, defense on defense); the remaining gap is the engine's support-power
 * interleave, still owned by the differential harness.
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
   * Neither eligible type appears in the OOL list, so the order must fall back to the engine
   * default rather than throwing or returning something outside the eligible set. This asserts only
   * the total-function contract (the pick is one of the eligible buckets); it deliberately does NOT
   * assert which one, because the real default is a power/TUV sort the cost-only seam cannot yet
   * reproduce — the differential harness owns that ordering (see the class note).
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
