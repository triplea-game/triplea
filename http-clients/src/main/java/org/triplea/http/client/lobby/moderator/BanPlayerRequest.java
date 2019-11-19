package org.triplea.http.client.lobby.moderator;

import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class BanPlayerRequest {
  private String playerChatId;
  private long banMinutes;

  /**
   * Returns a formatted text of the 'banMinutes', this converts ban minutes to minutes, hours or
   * days as appropriate, or uses the label "permanently" if ban duration is effectively permanent.
   */
  public String formattedBanDuration() {
    Preconditions.checkState(banMinutes > 0);

    if (banMinutes < 60) {
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
