package org.triplea.http.client.lobby.moderator;

import java.util.Collection;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * A data structure to power the 'show player information' display pop-up for moderators. Gives an
 * overview of a player, their aliases and history from what we can find of matching IPs or
 * system-ids from lobby (database).
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class PlayerSummaryForModerator {
  private String name;
  private String ip;
  private String systemId;
  private Collection<Alias> aliases;
  private Collection<BanInformation> bans;

  @Builder
  @Getter
  @ToString
  @EqualsAndHashCode
  public static class Alias {
    private String name;
    private String ip;
    private String systemId;
    private long epochMilliDate;
  }

  @Builder
  @Getter
  @ToString
  @EqualsAndHashCode
  public static class BanInformation {
    private String name;
    private String ip;
    private String systemId;
    private long epochMilliStartDate;
    private long epochMillEndDate;
  }
}
