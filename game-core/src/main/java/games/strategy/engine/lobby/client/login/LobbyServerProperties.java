package games.strategy.engine.lobby.client.login;

import java.net.URI;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Strings;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Server properties.
 *
 * <p>Generally there is one lobby server, but that server may move.
 *
 * <p>To keep track of this, we always have a properties file in a constant location that points to
 * the current lobby server.
 *
 * <p>The properties file may indicate that the server is not available using the ERROR_MESSAGE key.
 */
@Builder
@Getter
@EqualsAndHashCode
public final class LobbyServerProperties {
  /** The host address of the lobby, typically an IP address. */
  @Nonnull private final String host;

  /** The port the lobby is listening on. */
  @Nonnull private final Integer port;

  /** URI for the http lobby server. */
  @Nonnull private final URI httpServerUri;

  @Nullable private final String serverErrorMessage;

  /**
   * Message from lobby, eg: "welcome, lobby rules are: xyz".
   */
  @Nullable private final String serverMessage;

  public Optional<String> getServerMessage() {
    return Optional.ofNullable(Strings.emptyToNull(serverMessage));
  }

  public Optional<String> getServerErrorMessage() {
    return Optional.ofNullable(Strings.emptyToNull(serverErrorMessage));
  }
}
