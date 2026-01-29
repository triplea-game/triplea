package org.triplea.http.client.web.socket;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

@UtilityClass
public class WebsocketPaths {
  @NonNls public static final String GAME_CONNECTIONS = "/lobby/game-connection/ws";
  @NonNls public static final String PLAYER_CONNECTIONS = "/lobby/player-connection/ws";
}
