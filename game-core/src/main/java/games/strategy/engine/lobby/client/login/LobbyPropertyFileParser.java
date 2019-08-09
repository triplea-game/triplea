package games.strategy.engine.lobby.client.login;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.triplea.util.Version;

/**
 * Parses a downloaded lobby properties file (yaml format expected). Lobby properties include IP
 * address and port of the lobby.
 */
final class LobbyPropertyFileParser {

  @VisibleForTesting static final String YAML_HOST = "host";
  @VisibleForTesting static final String YAML_PORT = "port";
  @VisibleForTesting static final String YAML_HTTPS_PORT = "https_port";
  @VisibleForTesting static final String YAML_MESSAGE = "message";
  @VisibleForTesting static final String YAML_ERROR_MESSAGE = "error_message";

  private LobbyPropertyFileParser() {}

  public static LobbyServerProperties parse(
      final InputStream stream, final Version currentVersion) {
    final Load load = new Load(new LoadSettingsBuilder().build());
    final Map<?, ?> yamlProps =
        matchCurrentVersion((List<?>) load.loadFromInputStream(stream), currentVersion);

    return LobbyServerProperties.builder()
        .host((String) yamlProps.get(YAML_HOST))
        .port((Integer) yamlProps.get(YAML_PORT))
        .serverMessage((String) yamlProps.get(YAML_MESSAGE))
        .serverErrorMessage((String) yamlProps.get(YAML_ERROR_MESSAGE))
        .httpsPort((Integer) yamlProps.get(YAML_HTTPS_PORT))
        .build();
  }

  private static Map<?, ?> matchCurrentVersion(
      final List<?> lobbyProps, final Version currentVersion) {
    checkNotNull(lobbyProps);

    return lobbyProps.stream()
        .map(Map.class::cast)
        .filter(
            props ->
                currentVersion.equals(
                    new Version(Preconditions.checkNotNull((String) props.get("version")))))
        .findFirst()
        .orElse((Map<?, ?>) lobbyProps.get(0));
  }
}
