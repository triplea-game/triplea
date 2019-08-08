package org.triplea.java;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;

/** Provides methods for formatting time in various formats. */
public final class TimeManager {
  private TimeManager() {}

  /**
   * Returns a String representing the current {@link LocalDateTime}. Based on where you live this
   * might be either for example 13:45 or 1:45pm.
   *
   * @return The formatted String
   */
  public static String getLocalizedTime() {
    return new DateTimeFormatterBuilder()
        .appendLocalized(null, FormatStyle.MEDIUM)
        .toFormatter()
        .format(LocalDateTime.now(ZoneId.systemDefault()));
  }

  /**
   * Replacement for {@code Date.toString}.
   *
   * @param dateTime The DateTime which should be formatted
   * @return a Formatted String of the given DateTime
   */
  public static String toDateString(final LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy")
        .withZone(ZoneOffset.systemDefault())
        .format(dateTime);
  }
}
