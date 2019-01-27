package games.strategy.engine.lobby.client.login;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.common.annotations.VisibleForTesting;

import games.strategy.util.OpenJsonUtils;
import games.strategy.util.Version;

/**
 * Parses a downloaded lobby properties file (yaml format expected).
 * Lobby properties include IP address and port of the lobby.
 */
class LobbyPropertyFileParser {

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

  public static LobbyServerProperties parse(final InputStream fileContents, final Version currentVersion) {
    final Map<String, Object> yamlProps =
        OpenJsonUtils.toMap(matchCurrentVersion(loadYaml(fileContents), currentVersion));

    return LobbyServerProperties.builder()
        .host((String) yamlProps.get("host"))
        .port((Integer) yamlProps.get("port"))
        .serverMessage((String) yamlProps.get("message"))
        .serverErrorMessage((String) yamlProps.get("error_message"))
        .httpServerUri(URI.create((String) yamlProps.get(YAML_HTTP_SERVER_URI)))
        .build();
  }

  private static JSONObject matchCurrentVersion(final JSONArray lobbyProps, final Version currentVersion) {
    checkNotNull(lobbyProps);

    return OpenJsonUtils.stream(lobbyProps)
        .map(JSONObject.class::cast)
        .filter(props -> currentVersion.equals(new Version(props.getString("version"))))
        .findFirst()
        .orElse(lobbyProps.getJSONObject(0));
  }

  private static JSONArray loadYaml(final InputStream yamlContent) {
    final Yaml yaml = new Yaml();
    return new JSONArray(yaml.loadAs(yamlContent, List.class));
  }
}
