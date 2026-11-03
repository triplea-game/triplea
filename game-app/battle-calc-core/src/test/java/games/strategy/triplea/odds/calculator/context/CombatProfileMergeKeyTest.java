package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.SupportCategory;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pins the merge-key invariant for the set-valued {@code gives}/{@code receives} components: two
 * profiles that emit or consume the same categories are combat-fungible regardless of the order the
 * categories were collected in, and a profile carrying an extra category is a distinct bucket. If
 * these broke, fungible units would stop merging (a correctness-neutral slowdown) or, worse,
 * differently-supported units would merge into one bucket.
 */
class CombatProfileMergeKeyTest {

  private static final SupportCategory A = new SupportCategory("a");
  private static final SupportCategory B = new SupportCategory("b");

  @Test
  void givesCategorySetIsAnOrderIndependentMergeKey() {
    final CombatProfile ab = withGives(orderedSet(A, B));
    final CombatProfile ba = withGives(orderedSet(B, A));

    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());
  }

  @Test
  void receivesCategorySetIsAnOrderIndependentMergeKey() {
    final CombatProfile ab = withReceives(orderedSet(A, B));
    final CombatProfile ba = withReceives(orderedSet(B, A));

    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());
  }

  @Test
  void anExtraGivenCategoryIsADistinctBucket() {
    assertThat(withGives(orderedSet(A))).isNotEqualTo(withGives(orderedSet(A, B)));
  }

  @Test
  void aProfileWithNoSupportEqualsAnotherWithNoSupport() {
    assertThat(withGives(Set.of())).isEqualTo(withGives(Set.of()));
  }

  private static Set<SupportCategory> orderedSet(final SupportCategory... categories) {
    return new LinkedHashSet<>(Arrays.asList(categories));
  }

  private static CombatProfile withGives(final Set<SupportCategory> gives) {
    final CombatProfile base = land("infantry", 1, 2, 1);
    return new CombatProfile(
        base.type(),
        base.attack(),
        base.defense(),
        base.rolls(),
        base.maxRoundsAa(),
        base.maxAaAttacks(),
        base.hitPoints(),
        base.domain(),
        base.damage(),
        gives,
        base.receives(),
        base.flags(),
        base.next());
  }

  private static CombatProfile withReceives(final Set<SupportCategory> receives) {
    final CombatProfile base = land("infantry", 1, 2, 1);
    return new CombatProfile(
        base.type(),
        base.attack(),
        base.defense(),
        base.rolls(),
        base.maxRoundsAa(),
        base.maxAaAttacks(),
        base.hitPoints(),
        base.domain(),
        base.damage(),
        base.gives(),
        receives,
        base.flags(),
        base.next());
  }
}
