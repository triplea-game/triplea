package org.triplea.http.client.lobby.moderator;

import java.util.Collection;
import javax.annotation.Nullable;
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
public class PlayerSummary {
  @Nullable private String ip;
  @Nullable private String systemId;
  @Nullable private Collection<Alias> aliases;
  @Nullable private Collection<BanInformation> bans;
  private Collection<String> currentGames;
  @Nullable private Long registrationDateEpochMillis;

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
