package org.triplea.live.servers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.injection.ClientContext;
import org.triplea.util.Version;

@AllArgsConstructor(onConstructor_ = @VisibleForTesting)
class CurrentVersionSelector implements Function<LiveServers, ServerProperties> {

  private final Version currentVersion;

  CurrentVersionSelector() {
    this(Preconditions.checkNotNull(ClientContext.engineVersion()));
  }

  @Override
  public ServerProperties apply(final LiveServers liveServers) {
    Preconditions.checkNotNull(liveServers);
    Preconditions.checkArgument(!liveServers.getServers().isEmpty());

    return liveServers.getServers().stream()
        .filter(this::lessThanOrEqualToCurrentVersion)
        .max(maxVersion())
        .orElseThrow(() -> new IllegalArgumentException(noVersionFoundErrorMessage(liveServers)));
  }

  private boolean lessThanOrEqualToCurrentVersion(final ServerProperties server) {
    return !server.getMinEngineVersion().isGreaterThan(currentVersion);
  }

  private static Comparator<ServerProperties> maxVersion() {
    return Comparator.comparing(ServerProperties::getMinEngineVersion);
  }

  private String noVersionFoundErrorMessage(final LiveServers liveServers) {
    return String.format(
        "Configuration error, could not find any server versions less than %s, found versions: %s",
        currentVersion,
        liveServers.getServers().stream()
            .map(ServerProperties::getMinEngineVersion)
            .sorted()
            .collect(Collectors.toList()));
  }
}
