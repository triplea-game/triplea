package org.triplea.http.client;

import com.google.common.base.Preconditions;

/**
 * Configuration class that accepts a static data configuration that is then used in low layers in
 * the code when sending data to the lobby via Http.
 */
public abstract class LobbyHttpClientConfig {
  private static LobbyHttpClientConfig lobbyHttpClientConfig;

  /**
   * Sets the config, should only be invoked once. We expect this to be invoked (initialized) as
   * early as possible in the life cycle of a game-client.
   */
  public static void setConfig(LobbyHttpClientConfig config) {
    Preconditions.checkState(lobbyHttpClientConfig == null);
    lobbyHttpClientConfig = config;
  }

  public static LobbyHttpClientConfig getConfig() {
    return Preconditions.checkNotNull(lobbyHttpClientConfig);
  }

  /** Return the "major.minor" version of the client using the http-client (EG: "3.6") */
  public abstract String getClientVersion();

  /** Return the system-id of the client using the http-client. */
  public abstract String getSystemId();
}
