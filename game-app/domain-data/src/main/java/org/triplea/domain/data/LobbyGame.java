package org.triplea.domain.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.With;

/** Data structure representing a game in the lobby. */
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class LobbyGame {
  private String hostAddress;
  private Integer hostPort;
  private String hostName;
  private String mapName;
  private Integer playerCount;
  private Integer gameRound;
  private Long epochMilliTimeStarted;
  private Boolean passworded;
  private String status;
  @With private String comments;
}
