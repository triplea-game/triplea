package games.strategy.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;

public class TimeManager {
  /**
   * Replacement for {@code Date.toGMTString();}.
   *
   * @param instant The {@link Instant} being returned as String
   * @return formatted GMT Date String
   */
  public static String getGMTString(final Instant instant) {
    return DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss 'GMT'").withZone(ZoneOffset.UTC).format(instant);
  }

  /**
   * Returns a String representing the current {@link LocalDateTime}.
   * Based on where you live this might be either for example 13:45 or 1:45pm.
   * 
   * @return The formatted String
   */
  public static String getLocalizedTime() {
    return new DateTimeFormatterBuilder().appendLocalized(null, FormatStyle.MEDIUM).toFormatter()
        .format(LocalDateTime.now());
  }

  /**
   * Returns a String representing this {@link LocalDateTime}.
   * Based on where you live this might be either for example 13:45 or 1:45pm.
   * 
   * @param dateTime The LocalDateTime representing the desired time
   * @return The formatted String
   */
  public static String getLocalizedTimeWithoutSeconds(LocalDateTime dateTime) {
    return new DateTimeFormatterBuilder().appendLocalized(null, FormatStyle.SHORT).toFormatter()
        .format(dateTime);
  }

  /**
   * Replacement for {@code Date.toString}.
   * 
   * @param dateTime The DateTime which should be formatted
   * @return a Formatted String of the given DateTime
   */
  public static String toDateString(LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy").withZone(ZoneOffset.systemDefault())
        .format(dateTime);
  }
}
