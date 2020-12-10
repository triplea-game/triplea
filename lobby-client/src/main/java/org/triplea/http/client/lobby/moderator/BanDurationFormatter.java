package org.triplea.http.client.lobby.moderator;

import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BanDurationFormatter {

  /**
   * Converts parameter to a human readable value with labels. The time unit label is automatically
   * converted to a human time scale, for example, minutes, hours, days, or permanently.
   */
  public static String formatBanMinutes(final long banMinutes) {
    Preconditions.checkState(banMinutes >= 0);

    if (banMinutes == 0) {
      return "less than a minute";
    } else if (banMinutes < 60) {
      return banMinutes + " minutes";
    } else if (TimeUnit.MINUTES.toHours(banMinutes) < 24) {
      return TimeUnit.MINUTES.toHours(banMinutes) + " hours";
    } else if (TimeUnit.MINUTES.toDays(banMinutes) < 1000) {
      return TimeUnit.MINUTES.toDays(banMinutes) + " days";
    } else {
      return "permanently";
    }
  }
}
