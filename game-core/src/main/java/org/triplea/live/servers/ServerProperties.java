package org.triplea.live.servers;

import java.net.URI;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.util.Version;

@Getter
@Builder
public class ServerProperties {
  /** URI of the remote server */
  private final URI uri;
  /** Lobby welcome text shown to the user */
  private final String message;
  /** Minimum engine version compatible with this server */
  @Nonnull private final Version minEngineVersion;
  /** True if the server is inactive and indicates client is legacy and should upgrade */
  private final boolean inactive;
}
