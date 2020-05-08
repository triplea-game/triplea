package org.triplea.java;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.experimental.UtilityClass;

/** Utility class for date-time formatting operations. */
@UtilityClass
public class DateTimeFormatterUtil {

  /**
   * Converts an instant into a Date formatted string that contains the date and timezone offset.
   * EG: "2000-12-1 23:59 (GMT-5)"
   */
  public static String formatInstant(final Instant instant) {
    return formatEpochMilli(instant, Locale.getDefault(), ZoneId.systemDefault());
  }

  /**
   * Converts an epoch milli timestamp into a Date formatted string that contains the date and
   * timezone offset. EG: "2000-12-1 23:59 (GMT-5)"
   */
  public static String formatEpochMilli(final long epochMilli) {
    return formatEpochMilli(
        Instant.ofEpochMilli(epochMilli), Locale.getDefault(), ZoneId.systemDefault());
  }

  @VisibleForTesting
  static String formatEpochMilli(final Instant instant, final Locale locale, final ZoneId zoneId) {
    return DateTimeFormatter.ofPattern("y-M-d H:m (O)")
        .withLocale(locale)
        .withZone(zoneId)
        .format(instant);
  }
}
