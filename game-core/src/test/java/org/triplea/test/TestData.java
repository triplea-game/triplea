package org.triplea.test;

import java.time.Instant;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.LobbyGame;

@UtilityClass
public class TestData {
  public static final LobbyGame LOBBY_GAME =
      LobbyGame.builder()
          .hostAddress("127.0.0.1")
          .hostPort(12)
          .hostName("name")
          .mapName("map")
          .playerCount(3)
          .gameRound(1)
          .epochMilliTimeStarted(Instant.now().toEpochMilli())
          .mapVersion("1")
          .passworded(false)
          .status("Waiting For Players")
          .comments("comments")
          .build();
}
