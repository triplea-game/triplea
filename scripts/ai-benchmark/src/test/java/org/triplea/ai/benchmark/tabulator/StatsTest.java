package org.triplea.ai.benchmark.tabulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatsTest {
  @Test
  void emptySampleHasNoMean() {
    assertThat(Stats.summarize(List.of())).isEqualTo(new Stats.Summary(0, null, null));
  }

  @Test
  void singleValueHasNoInterval() {
    assertThat(Stats.summarize(List.of(7.0))).isEqualTo(new Stats.Summary(1, 7.0, null));
  }

  @Test
  void intervalUsesStudentT() {
    // sd = 1.5811, se = 0.7071, t(0.975, df 4) = 2.7764
    final Stats.Summary summary = Stats.summarize(List.of(1.0, 2.0, 3.0, 4.0, 5.0));

    assertThat(summary.n()).isEqualTo(5);
    assertThat(summary.mean()).isEqualTo(3.0);
    assertThat(summary.ci()).isCloseTo(1.9632, within(1e-4));
  }

  @Test
  void differenceIsSecondMinusFirstWithWelchInterval() {
    // se = sqrt(1/3 + 4/3) = 1.2910, Welch df = 2.9412 -> t = 3.2188
    final Stats.Difference diff = Stats.difference(List.of(1.0, 2.0, 3.0), List.of(4.0, 6.0, 8.0));

    assertThat(diff.mean()).isEqualTo(4.0);
    assertThat(diff.ci()).isCloseTo(4.1554, within(1e-4));
    assertThat(diff.excludesZero()).isFalse();
  }

  @Test
  void separatedSamplesExcludeZero() {
    final Stats.Difference diff =
        Stats.difference(List.of(1.0, 2.0, 3.0), List.of(11.0, 12.0, 13.0));

    assertThat(diff.excludesZero()).isTrue();
  }

  @Test
  void differenceNeedsTwoValuesPerSideForAnInterval() {
    assertThat(Stats.difference(List.of(1.0), List.of(4.0, 6.0)))
        .isEqualTo(new Stats.Difference(4.0, null));
  }
}
