package org.triplea.java;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;

/** Utility class for date-time formatting operations. */
@UtilityClass
public class DateTimeFormatterUtil {

  private static Locale defaultLocale = Locale.getDefault();
  private static ZoneId defaultZone = ZoneId.systemDefault();

  @AllArgsConstructor
  public enum FormatOption {
    /** EG: "2000-12-1 23:59 (GMT-5) */
    WITH_TIMEZONE("y-M-d H:m (O)"),

    /** EG: "2000-12-1 23:59 */
    WITHOUT_TIMEZONE("y-M-d H:m");

    private final String pattern;
  }

  @VisibleForTesting
  public void setDefaultToUtc() {
    defaultLocale = Locale.US;
    defaultZone = ZoneId.ofOffset("UTC", ZoneOffset.UTC);
  }

  /**
   * Converts an epoch milli timestamp into a Date formatted string that contains the date, time,
   * and timezone offset. EG: "2000-12-1 23:59 (GMT-5)"
   */
  public static String formatEpochMilli(final Long epochMilli) {
    return formatEpochMilli(epochMilli, FormatOption.WITH_TIMEZONE);
  }

  public static String formatEpochMilli(final Long epochMilli, final FormatOption formatOption) {
    return epochMilli == null ? "" : formatInstant(Instant.ofEpochMilli(epochMilli), formatOption);
  }

  /**
   * Converts an instant into a Date formatted string that contains the date, time and timezone
   * offset. EG: "2000-12-1 23:59 (GMT-5)"
   */
  public static String formatInstant(final Instant instant) {
    return formatInstant(instant, FormatOption.WITH_TIMEZONE);
  }

  private static String formatInstant(final Instant instant, final FormatOption formatOption) {
    return DateTimeFormatter.ofPattern(formatOption.pattern)
        .withLocale(defaultLocale)
        .withZone(defaultZone)
        .format(instant);
  }

  /**
   * Replacement for {@code Date.toString}.
   *
   * @param dateTime The DateTime which should be formatted
   * @return a Formatted String of the given DateTime
   */
  public static String toDateString(final LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
        .withLocale(defaultLocale)
        .withZone(defaultZone)
        .format(dateTime);
  }
}
