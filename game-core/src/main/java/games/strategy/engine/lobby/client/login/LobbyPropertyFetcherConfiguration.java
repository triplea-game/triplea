package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.map.download.DownloadConfiguration;

/**
 * Responsible for constructing a {@code LobbyServerPropertiesFetcher}.
 */
public class LobbyPropertyFetcherConfiguration {

  private static final LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher =
      new LobbyServerPropertiesFetcher(url -> DownloadConfiguration.contentReader().downloadToFile(url));

  public static LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher() {
    return lobbyServerPropertiesFetcher;
  }
}
