package games.strategy.engine.lobby.client.login;

/**
 * Responsible for constructing a {@code LobbyServerPropertiesFetcher}.
 */
public class LobbyPropertyFetcherConfiguration {

  private static final LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher =
      new LobbyServerPropertiesFetcher();

  public static LobbyServerPropertiesFetcher lobbyServerPropertiesFetcher() {
    return lobbyServerPropertiesFetcher;
  }
}
