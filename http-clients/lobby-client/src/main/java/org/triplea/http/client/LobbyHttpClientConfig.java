package org.triplea.http.client;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

/**
 * Configuration class that accepts a static data configuration that is then used in low layers in
 * the code when sending data to the lobby via Http.
 */
@Getter
@Builder
public class LobbyHttpClientConfig {
  private static LobbyHttpClientConfig lobbyHttpClientConfig;

  public static String VERSION_HEADER = "Triplea-Version";
  public static final String SYSTEM_ID_HEADER = "System-Id-Header";


  /** The "major.minor" version of the client using the http-client (EG: "3.6") */
  @Nonnull private final String clientVersion;
  /** The system-id of the client using the http-client. */
  @Nonnull private final String systemId;

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
}
