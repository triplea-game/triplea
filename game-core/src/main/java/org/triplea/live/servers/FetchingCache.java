package org.triplea.live.servers;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.download.CloseableDownloader;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.Builder;
import org.triplea.injection.Injections;
import org.triplea.java.function.ThrowingSupplier;

@Builder
class FetchingCache implements ThrowingSupplier<LiveServers, IOException> {
  /** Static cache so that cached value is shared across all instances. */
  @VisibleForTesting static LiveServers liveServersCache;

  private final ThrowingSupplier<CloseableDownloader, IOException> contentDownloader;
  private final Function<InputStream, LiveServers> yamlParser;

  @Override
  public synchronized LiveServers get() throws IOException {
    final Optional<LiveServers> override = getOverride();
    if (override.isPresent()) {
      return override.get();
    }

    return fetchLiveServers();
  }

  private Optional<LiveServers> getOverride() {
    return ClientSetting.lobbyUriOverride.getValue().map(this::buildLiverServersFromOverride);
  }

  private LiveServers buildLiverServersFromOverride(final URI overrideUri) {
    return LiveServers.builder()
        .latestEngineVersion(Injections.instance.engineVersion())
        .servers(
            List.of(
                ServerProperties.builder()
                    .message("Override server")
                    .minEngineVersion(Injections.instance.engineVersion())
                    .uri(overrideUri)
                    .build()))
        .build();
  }

  private LiveServers fetchLiveServers() throws IOException {
    if (liveServersCache == null) {
      try (CloseableDownloader downloader = contentDownloader.get()) {
        liveServersCache = yamlParser.apply(downloader.getStream());
      }
    }
    return liveServersCache;
  }
}
