package games.strategy.engine.framework.startup.mc;

import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Simple data class for parameter values to connect to a remote host server (EG: to connect to a
 * hosted game).
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
class ServerConnectionProps {
  /** Player name, the desired name for the current player connecting to remote host. */
  @Nonnull private final String name;
  /** Remote host address. */
  @Nonnull private final Integer port;
  /**
   * Password to use to connect to the remotely hosted game, this is set by the host of the game.
   */
  private final char[] password;
}
