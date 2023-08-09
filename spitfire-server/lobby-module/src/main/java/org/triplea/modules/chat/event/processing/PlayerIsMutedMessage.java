package org.triplea.modules.chat.event.processing;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import lombok.Builder;
import lombok.experimental.UtilityClass;

@UtilityClass
class PlayerIsMutedMessage {
  private static final Function<Instant, String> muteDurationFormatter =
      MuteDurationRemainingCalculator.builder().build();

  String build(final Instant muteExpiry) {
    return "You have been muted, expiring in: " + muteDurationFormatter.apply(muteExpiry);
  }

  /**
   * Calculates a string of how many minutes are remaining in a mute until expired, or if less than
   * a minute then how many seconds are left.
   */
  @VisibleForTesting
  @Builder
  static class MuteDurationRemainingCalculator implements Function<Instant, String> {
    @Builder.Default private Clock clock = Clock.systemUTC();

    @Override
    public String apply(final Instant muteExpiry) {
      final long minutes = Duration.between(clock.instant(), muteExpiry).toMinutes();
      return minutes > 0
          ? minutes + " minutes"
          : Duration.between(clock.instant(), muteExpiry).toSeconds() + " seconds";
    }
  }
}
