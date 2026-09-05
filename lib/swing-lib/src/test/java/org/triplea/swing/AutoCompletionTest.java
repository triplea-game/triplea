package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.triplea.swing.AutoCompletion.startsWith;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class AutoCompletionTest {
  @Nested
  final class StartsWithTest {
    @Test
    void shouldReturnTrueWhenFirstStartsWithSecond() {
      assertThat(startsWith("Mongolia", "M")).isTrue();
      assertThat(startsWith("Mongolia", "Mong")).isTrue();
      assertThat(startsWith("Mongolia", "Mongolia")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenFirstStartsWithSecondIgnoringCase() {
      assertThat(startsWith("Mongolia", "m")).isTrue();
      assertThat(startsWith("Mongolia", "mong")).isTrue();
      assertThat(startsWith("Mongolia", "monGOLia")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenFirstStartsWithSecondIgnoringCombiningMarks() {
      assertThat(startsWith("Lhûn", "Lhûn")).isTrue();
      assertThat(startsWith("Lhûn", "Lhu")).isTrue();
      assertThat(startsWith("Lhûn", "Lhun")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenFirstDoesNotStartWithSecond() {
      assertThat(startsWith("Mongolia", "N")).isFalse();
      assertThat(startsWith("Mongolia", "Mont")).isFalse();
      assertThat(startsWith("Mongolia", "mont")).isFalse();
      assertThat(startsWith("Mongolia", "Mongoliaa")).isFalse();
    }
  }
}
