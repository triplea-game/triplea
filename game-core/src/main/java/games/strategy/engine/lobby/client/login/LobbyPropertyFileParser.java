package games.strategy.engine.lobby.client.login;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import games.strategy.util.OpenJsonUtils;
import games.strategy.util.Version;

/**
 * Parses a downloaded lobby properties file (yaml format expected). Lobby properties include IP
 * address and port of the lobby.
 */
class LobbyPropertyFileParser {

  public static LobbyServerProperties parse(
      final String fileContents, final Version currentVersion) {
    final Map<String, Object> yamlProps =
        OpenJsonUtils.toMap(matchCurrentVersion(loadYaml(fileContents), currentVersion));

    return LobbyServerProperties.builder()
        .host((String) yamlProps.get("host"))
        .port((Integer) yamlProps.get("port"))
        .serverMessage((String) yamlProps.get("message"))
        .serverErrorMessage((String) yamlProps.get("error_message"))
        .build();
  }

  private static JSONObject matchCurrentVersion(
      final JSONArray lobbyProps, final Version currentVersion) {
    checkNotNull(lobbyProps);

    return OpenJsonUtils.stream(lobbyProps)
        .map(JSONObject.class::cast)
        .filter(props -> currentVersion.equals(new Version(props.getString("version"))))
        .findFirst()
        .orElse(lobbyProps.getJSONObject(0));
  }

  private static JSONArray loadYaml(final String yamlContent) {
    final Yaml yaml = new Yaml();
    return new JSONArray(yaml.loadAs(yamlContent, List.class));
  }
}
