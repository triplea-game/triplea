package org.triplea.modules;

import java.time.Instant;
import lombok.experimental.UtilityClass;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@UtilityClass
public class TestData {
  public static final ApiKey API_KEY = ApiKey.of("test");

  public static final AuthenticatedUser AUTHENTICATED_USER =
      AuthenticatedUser.builder().userId(100).userRole(UserRole.PLAYER).apiKey(API_KEY).build();

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
