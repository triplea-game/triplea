package org.triplea.live.servers;

import games.strategy.engine.lobby.client.login.LobbyPropertyFetcherConfiguration;

public class LiveServersFetcher {

  public ServerProperties serverForCurrentVersion() {
    return LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher()
        .fetchLobbyServerProperties()
        .orElseThrow(LobbyAddressFetchException::new);
  }

  private static final class LobbyAddressFetchException extends RuntimeException {
    private static final long serialVersionUID = -301010780022774627L;

    LobbyAddressFetchException() {
      super("Failed to fetch lobby address, check network connection.");
    }
  }
}
