package org.triplea.live.servers;

import games.strategy.triplea.settings.ClientSetting;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.latest.version.LatestVersionClient;
import org.triplea.http.client.latest.version.LatestVersionResponse;
import org.triplea.util.Version;

@Slf4j
@UtilityClass
public class LiveServersFetcher {

  /**
   * Queries the lobby-server for the latest game engine version.
   *
   * @return Empty optional if server fails to return a value, otherwise latest game engine version
   *     as known to the lobby-server.
   */
  public static Optional<LatestVersionResponse> latestVersion() {
    try {
      return Optional.of(
          LatestVersionClient.newClient(ClientSetting.lobbyUri.getValueOrThrow())
              .fetchLatestVersion());
    } catch (final HttpInteractionException e) {
      log.info(
          "Unable to complete engine out-of-date check. (Offline or server not available?)", e);
      return Optional.empty();
    }
  }
}
