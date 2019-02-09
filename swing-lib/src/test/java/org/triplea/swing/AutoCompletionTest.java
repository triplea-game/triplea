package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.triplea.swing.AutoCompletion.startsWith;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class AutoCompletionTest {
  @Nested
  final class StartsWithTest {
    @Test
    void shouldReturnTrueWhenFirstStartsWithSecond() {
      assertThat(startsWith("Mongolia", "M"), is(true));
      assertThat(startsWith("Mongolia", "Mong"), is(true));
      assertThat(startsWith("Mongolia", "Mongolia"), is(true));
    }

    @Test
    void shouldReturnTrueWhenFirstStartsWithSecondIgnoringCase() {
      assertThat(startsWith("Mongolia", "m"), is(true));
      assertThat(startsWith("Mongolia", "mong"), is(true));
      assertThat(startsWith("Mongolia", "monGOLia"), is(true));
    }

    @Test
    void shouldReturnTrueWhenFirstStartsWithSecondIgnoringCombiningMarks() {
      assertThat(startsWith("Lhûn", "Lhûn"), is(true));
      assertThat(startsWith("Lhûn", "Lhu"), is(true));
      assertThat(startsWith("Lhûn", "Lhun"), is(true));
    }

    @Test
    void shouldReturnFalseWhenFirstDoesNotStartWithSecond() {
      assertThat(startsWith("Mongolia", "N"), is(false));
      assertThat(startsWith("Mongolia", "Mont"), is(false));
      assertThat(startsWith("Mongolia", "mont"), is(false));
      assertThat(startsWith("Mongolia", "Mongoliaa"), is(false));
    }
  }
}
