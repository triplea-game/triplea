package org.triplea.http.client.web.socket;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

@UtilityClass
public class WebsocketPaths {
  @NonNls public static final String GAME_CONNECTIONS = "/game-connection/ws";
  @NonNls public static final String PLAYER_CONNECTIONS = "/player-connection/ws";
}
