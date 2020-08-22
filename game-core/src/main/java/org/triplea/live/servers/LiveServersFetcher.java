package org.triplea.live.servers;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.download.CloseableDownloader;
import games.strategy.engine.framework.map.download.ContentDownloader;
import games.strategy.triplea.UrlConstants;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.util.Version;

/**
 * Fetches {@code LiveServers} information from network (or if already fetched, from cache). {@code
 * LiveServers} can be used to determine the latest TripleA version and the address of any lobbies
 * that can be connected to.
 */
@Log
@AllArgsConstructor(onConstructor_ = @VisibleForTesting)
public class LiveServersFetcher {
  private final Function<LiveServers, ServerProperties> currentVersionSelector;
  private final ThrowingSupplier<LiveServers, IOException> liveServersFetcher;

  public LiveServersFetcher() {
    this(() -> new ContentDownloader(UrlConstants.LIVE_SERVERS_URI));
  }

  @VisibleForTesting
  LiveServersFetcher(final ThrowingSupplier<CloseableDownloader, IOException> networkFetcher) {
    this(
        new CurrentVersionSelector(),
        FetchingCache.builder()
            .contentDownloader(networkFetcher)
            .yamlParser(new ServerYamlParser())
            .build());
  }

  public Optional<Version> latestVersion() {
    try {
      return Optional.of(liveServersFetcher.get().getLatestEngineVersion());
    } catch (final IOException e) {
      log.log(Level.INFO, "(No network connection?) Failed to get server properties", e);
      return Optional.empty();
    }
  }

  public ServerProperties serverForCurrentVersion() {
    try {
      final var liveServers = liveServersFetcher.get();
      return currentVersionSelector.apply(liveServers);
    } catch (final IOException e) {
      throw new LobbyAddressFetchException(e);
    }
  }

  @VisibleForTesting
  static final class LobbyAddressFetchException extends RuntimeException {
    private static final long serialVersionUID = -301010780022774627L;

    LobbyAddressFetchException(final IOException e) {
      super("Failed to fetch lobby address, check network connection.", e);
    }
  }

  public Optional<URI> lobbyUriForCurrentVersion() {
    return fetchServerProperties().map(ServerProperties::getUri);
  }

  private Optional<ServerProperties> fetchServerProperties() {
    try {
      final var liveServers = liveServersFetcher.get();
      return Optional.of(currentVersionSelector.apply(liveServers));
    } catch (final IOException e) {
      log.log(Level.WARNING, "(No network connection?) Failed to get server locations", e);
      return Optional.empty();
    }
  }

  public Optional<URI> getMapsServerUri() {
    return fetchServerProperties().map(ServerProperties::getMapsServerUri);
  }
}
