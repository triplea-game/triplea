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
   * @param date The {@link Instant} being returned as String
   * @return formatted GMT Date String
   */
  public static String getGMTString(final Instant date) {
    return DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss 'GMT'").withZone(ZoneOffset.UTC).format(date);
  }

  public static String getLocalizedTime() {
    return new DateTimeFormatterBuilder().appendLocalized(null, FormatStyle.MEDIUM).toFormatter()
        .format(LocalDateTime.now());
  }

  public static String getLocalizedTimeWithoutSeconds() {
    return getLocalizedTimeWithoutSeconds(LocalDateTime.now());
  }

  public static String getLocalizedTimeWithoutSeconds(LocalDateTime time) {
    return new DateTimeFormatterBuilder().appendLocalized(null, FormatStyle.SHORT).toFormatter()
        .format(time);
  }

  /**
   * Replacement for {@code Date.toString}.
   * 
   * @param dateTime The DateTime which should be formatted
   * @return a Formatted String of the given DateTime
   */
  public static String toDateString(LocalDateTime dateTime) {
    return DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy").format(dateTime);
  }
}
