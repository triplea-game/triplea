package org.triplea.live.servers;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.io.CloseableDownloader;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.util.Version;

@Builder
class FetchingCache implements ThrowingSupplier<LiveServers, IOException> {
  /** Static cache so that cached value is shared across all instances. */
  @VisibleForTesting static LiveServers liveServersCache;

//  @Nonnull private final ThrowingSupplier<CloseableDownloader, IOException> contentDownloader;
//  @Nonnull private final Function<InputStream, LiveServers> yamlParser;
  @Nonnull private final Version engineVersion;

  @Override
  public synchronized LiveServers get() throws IOException {
    return LiveServers.builder()
        .latestEngineVersion(engineVersion)
        .servers(
            List.of(
                ServerProperties.builder()
                    .minEngineVersion(engineVersion)
                    .uri(ClientSetting.lobbyUri.getValueOrThrow())
                    .build()))
        .build();
  }
}
