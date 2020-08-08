package org.triplea.java;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Locale.Category;
import lombok.experimental.UtilityClass;

/** Provides methods for getting time and date instances. */
@UtilityClass
public final class DateTimeUtil {
  @VisibleForTesting static ZoneId defaultZoneId = ZoneId.systemDefault();
  @VisibleForTesting static Clock clock = Clock.system(defaultZoneId);
  @VisibleForTesting static Locale defaultLocale = Locale.getDefault(Category.FORMAT);

  /**
   * Returns a String representing the current {@link LocalDateTime}. Based on where you live this
   * might be either for example 13:45 or 1:45pm.
   *
   * @return The formatted String
   */
  public static String getLocalizedTime() {
    return new DateTimeFormatterBuilder()
        .appendLocalized(null, FormatStyle.MEDIUM)
        .toFormatter(defaultLocale)
        .format(LocalDateTime.ofInstant(clock.instant(), defaultZoneId));
  }

  /** Returns an {@code Instant} in UTC with a specified year, month, day, hour and minute. */
  public static Instant utcInstantOf(
      final int year, final int month, final int day, final int hour, final int minute) {
    return LocalDateTime.of(year, month, day, hour, minute).toInstant(ZoneOffset.UTC);
  }
}
