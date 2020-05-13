package org.triplea.modules.chat.event.processing;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiFunction;
import lombok.experimental.UtilityClass;

@UtilityClass
class PlayerIsMutedMessage {

  @VisibleForTesting
  static final BiFunction<Clock, Instant, String> muteDurationRemainingToString =
      (clock, muteExpiry) -> {
        final long minutes = Duration.between(clock.instant(), muteExpiry).toMinutes();
        return minutes > 0
            ? minutes + " minutes"
            : Duration.between(clock.instant(), muteExpiry).toSeconds() + " seconds";
      };

  String build(final Instant muteExpiry) {
    return "You have been muted, expiring in: "
        + muteDurationRemainingToString.apply(Clock.systemUTC(), muteExpiry);
  }
}
