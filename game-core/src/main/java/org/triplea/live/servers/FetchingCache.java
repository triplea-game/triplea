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
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.util.Version;

@Builder
class FetchingCache implements ThrowingSupplier<LiveServers, IOException> {
  /** Static cache so that cached value is shared across all instances. */
  @VisibleForTesting static LiveServers liveServersCache;

  @Nonnull private final ThrowingSupplier<CloseableDownloader, IOException> contentDownloader;
  @Nonnull private final Function<InputStream, LiveServers> yamlParser;
  @Nonnull private final Version engineVersion;

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
        .latestEngineVersion(engineVersion)
        .servers(
            List.of(
                ServerProperties.builder()
                    .message("Override server")
                    .minEngineVersion(engineVersion)
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
