package org.triplea.live.servers;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.util.Version;

/**
 * Data representation of the live servers YAML file. The file is hosted remotely, game clients read
 * this file to find out the latest version of TripleA and which lobby servers are available.
 */
@Builder
@Getter
class LiveServers {
  @Nonnull private final Version latestEngineVersion;
  @Nonnull private final List<ServerProperties> servers;
}
