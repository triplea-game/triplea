package games.strategy.engine.lobby.client.login;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.triplea.util.Version;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Parses a downloaded lobby properties file (yaml format expected).
 * Lobby properties include IP address and port of the lobby.
 */
final class LobbyPropertyFileParser {

  @VisibleForTesting
  static final String YAML_HOST = "host";
  @VisibleForTesting
  static final String YAML_PORT = "port";
  @VisibleForTesting
  static final String YAML_HTTP_SERVER_URI = "http_server_uri";
  @VisibleForTesting
  static final String YAML_MESSAGE = "message";
  @VisibleForTesting
  static final String YAML_ERROR_MESSAGE = "error_message";

  private LobbyPropertyFileParser() {}

  public static LobbyServerProperties parse(final InputStream stream, final Version currentVersion) {
    final Load load = new Load(new LoadSettingsBuilder().build());
    final Map<?, ?> yamlProps = matchCurrentVersion((List<?>) load.loadFromInputStream(stream), currentVersion);

    return LobbyServerProperties.builder()
        .host((String) yamlProps.get("host"))
        .port((Integer) yamlProps.get("port"))
        .serverMessage((String) yamlProps.get("message"))
        .serverErrorMessage((String) yamlProps.get("error_message"))
        .httpServerUri(URI.create((String) yamlProps.get(YAML_HTTP_SERVER_URI)))
        .build();
  }

  private static Map<?, ?> matchCurrentVersion(final List<?> lobbyProps, final Version currentVersion) {
    checkNotNull(lobbyProps);

    return lobbyProps.stream()
        .map(Map.class::cast)
        .filter(props -> currentVersion.equals(new Version(Preconditions.checkNotNull((String) props.get("version")))))
        .findFirst()
        .orElse((Map<?, ?>) lobbyProps.get(0));
  }
}
