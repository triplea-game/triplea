package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.map.download.DownloadConfiguration;

/** Responsible for constructing a {@code LobbyServerPropertiesFetcher}. */
public final class LobbyPropertyFetcherConfiguration {

  private static final LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher =
      new LobbyServerPropertiesFetcher(DownloadConfiguration.contentReader()::download);

  private LobbyPropertyFetcherConfiguration() {}

  public static LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher() {
    return lobbyServerPropertiesFetcher;
  }
}
