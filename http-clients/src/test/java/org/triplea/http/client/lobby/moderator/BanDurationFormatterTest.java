package org.triplea.http.client.lobby.moderator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BanDurationFormatterTest {

  @ParameterizedTest
  @MethodSource
  void formattedBanDuration(final long banMinutes, final String expectedOutput) {
    final String result = BanDurationFormatter.formatBanMinutes(banMinutes);

    assertThat(result, is(expectedOutput));
  }

  @SuppressWarnings("unused")
  private static List<Arguments> formattedBanDuration() {
    return List.of(
        Arguments.of(1, "1 minutes"),
        Arguments.of(5, "5 minutes"),
        Arguments.of(10, "10 minutes"),
        Arguments.of(59, "59 minutes"),
        Arguments.of(60, "1 hours"),
        Arguments.of(61, "1 hours"),
        Arguments.of(119, "1 hours"),
        Arguments.of(120, "2 hours"),
        Arguments.of(60 * 5, "5 hours"),
        Arguments.of((60 * 23), "23 hours"),
        Arguments.of((60 * 24) - 1, "23 hours"),
        Arguments.of((60 * 24), "1 days"),
        Arguments.of((60 * 24) * 2, "2 days"),
        Arguments.of((60 * 24) * 7, "7 days"),
        Arguments.of((60 * 24) * 1000, "permanently"));
  }
}
