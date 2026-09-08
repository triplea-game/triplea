package org.triplea.test.common;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IsInstant {

  /**
   * Returns the instant with the equivalent date as defined by the parameters (parameters are
   * assumed to be UTC).
   *
   * <p>Example usage: <code>
   *   assertThat(Instant.now()).isEqualTo(isInstant(2020, 12, 24, 23, 59, 59));
   * </code> <br>
   * The above is equivalent to:<code>
   *   assertThat(Instant.now()).isEqualTo(Instant.parse("2020-12-24T23:59:59Z"));
   * </code>
   *
   * @param year The year to match (should be YYYY format, eg: 2020)
   * @param month The month to match (1-12)
   * @param day The day to match (1-31)
   * @param hour The hour of day to match (1-23)
   * @param minute The minute of the hour to match (0-59)
   * @param second The second of the minute to match (0-59)
   */
  public static Instant isInstant(
      final int year,
      final int month,
      final int day,
      final int hour,
      final int minute,
      final int second) {
    return LocalDateTime.of(year, month, day, hour, minute, second).toInstant(ZoneOffset.UTC);
  }
}
