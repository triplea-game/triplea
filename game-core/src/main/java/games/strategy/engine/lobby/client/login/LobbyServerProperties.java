package games.strategy.engine.lobby.client.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.client.utils.URIBuilder;

import com.google.common.base.Strings;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Server properties.
 *
 * <p>
 * Generally there is one lobby server, but that server may move.
 * </p>
 * <p>
 * To keep track of this, we always have a properties file in a constant location that points to
 * the current lobby server.
 * </p>
 * <p>
 * The properties file may indicate that the server is not available using the ERROR_MESSAGE key.
 * </p>
 */
@Builder
@Getter
@EqualsAndHashCode
@ToString
public final class LobbyServerProperties {

  /** The host address of the lobby, typically an IP address. */
  @Nonnull
  private final String host;

  /** The port the lobby is listening on. */
  @Nonnull
  private final Integer port;

  /** The port the https lobby server is listening on. */
  @Nonnull
  private final Integer httpsPort;

  @Nullable
  private final String serverErrorMessage;

  /**
   * Message from lobby, eg: "welcome, lobby rules are: xyz".
   */
  @Nullable
  private final String serverMessage;

  public Optional<String> getServerMessage() {
    return Optional.ofNullable(Strings.emptyToNull(serverMessage));
  }

  public Optional<String> getServerErrorMessage() {
    return Optional.ofNullable(Strings.emptyToNull(serverErrorMessage));
  }

  /**
   * Convenience method to get the URI of the lobby https server.
   */
  public URI getHttpsServerUri() {
    try {
      return new URIBuilder()
          .setScheme(
              // allow env variable override of https so we can do local development with http
              Optional.ofNullable(System.getenv("HTTP_SERVER_PROTOCOL")).orElse("https"))
          .setHost(host)
          .setPort(httpsPort)
          .build();
    } catch (final URISyntaxException e) {
      throw new RuntimeException("Error with lobby properties: " + this, e);
    }
  }
}
