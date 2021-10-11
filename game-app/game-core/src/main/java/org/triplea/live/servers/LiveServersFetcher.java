package org.triplea.live.servers;

import games.strategy.triplea.settings.ClientSetting;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.latest.version.LatestVersionClient;
import org.triplea.util.Version;

/**
 * Fetches {@code LiveServers} information from network (or if already fetched, from cache). {@code
 * LiveServers} can be used to determine the latest TripleA version and the address of any lobbies
 * that can be connected to.
 */
@Slf4j
@UtilityClass
public class LiveServersFetcher {
  public static Optional<Version> latestVersion() {
    try {
      final String latestVersion =
          LatestVersionClient.newClient(ClientSetting.lobbyUri.getValueOrThrow())
              .fetchLatestVersion()
              .getLatestEngineVersion();
      final var version = new Version(latestVersion);
      return Optional.of(version);
    } catch (final HttpInteractionException e) {
      log.info(
          "Unable to complete engine out-of-date check. (Offline or server not available?)", e);
      return Optional.empty();
    }
  }
}
