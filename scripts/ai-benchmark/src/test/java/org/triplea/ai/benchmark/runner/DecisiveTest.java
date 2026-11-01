package org.triplea.ai.benchmark.runner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DecisiveTest {
  private static Decisive.Standing standing(
      final double tuv,
      final double enemyTuv,
      final double income,
      final double enemyIncome,
      final boolean holdsAllOwnCapitals,
      final boolean holdsEnemyCapital) {
    return new Decisive.Standing(
        tuv, enemyTuv, income, enemyIncome, holdsAllOwnCapitals, holdsEnemyCapital);
  }

  @Test
  void leadsWhenAheadOnValueAndIncomeWithCapitals() {
    assertThat(standing(111, 100, 30, 29, true, true).leads()).isTrue();
  }

  @Test
  void doesNotLeadWithAnythingShort() {
    assertThat(standing(110, 100, 30, 29, true, true).leads())
        .as("tuv lead is not over 10%")
        .isFalse();
    assertThat(standing(120, 100, 29, 29, true, true).leads()).as("income only level").isFalse();
    assertThat(standing(120, 100, 30, 29, false, true).leads()).as("own capital lost").isFalse();
    assertThat(standing(120, 100, 30, 29, true, false).leads()).as("no enemy capital").isFalse();
  }

  @Test
  void isDecisiveOnlyAfterTheLeadHoldsForTwoRoundEnds() {
    final var decisive = new Decisive();

    assertThat(decisive.observe("Allies")).isNull();
    assertThat(decisive.observe("Allies")).isEqualTo("Allies");
  }

  @Test
  void aBrokenLeadRestartsTheCount() {
    final var decisive = new Decisive();

    decisive.observe("Allies");
    decisive.observe(null);

    assertThat(decisive.observe("Allies")).isNull();
  }

  @Test
  void aChangeOfLeaderRestartsTheCount() {
    final var decisive = new Decisive();

    decisive.observe("Allies");

    assertThat(decisive.observe("Axis")).isNull();
  }
}
