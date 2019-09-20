package org.triplea.http.client.lobby.game.listing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Data structure representing a game in the lobby. */
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LobbyGame {
  private String hostIpAddress;
  private Integer hostPort;
  private String hostName;
  private String mapName;
  private Integer playerCount;
  private Integer gameRound;
  private Long epochMilliTimeStarted;
  private String mapVersion;
  private Boolean passworded;
  private String status;
  private String comments;
}
