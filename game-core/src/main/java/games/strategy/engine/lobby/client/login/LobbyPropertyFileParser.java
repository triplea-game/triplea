package games.strategy.engine.lobby.client.login;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import games.strategy.triplea.UrlConstants;
import games.strategy.util.OpenJsonUtils;
import games.strategy.util.Version;

/**
 * Parses a downloaded lobby properties file (yaml format expected).
 * Lobby properties include IP address and port of the lobby.
 */
class LobbyPropertyFileParser {


  public static LobbyServerProperties parse(final File file, final Version currentVersion) {
    try {
      final Map<String, Object> yamlProps =
          OpenJsonUtils.toMap(matchCurrentVersion(loadYaml(file), currentVersion));

      return LobbyServerProperties.builder()
          .host((String) yamlProps.get("host"))
          .port((Integer) yamlProps.get("port"))
          .serverMessage((String) yamlProps.get("message"))
          .serverErrorMessage((String) yamlProps.get("error_message"))
          .build();
    } catch (final IOException e) {
      throw new RuntimeException("Failed loading file: " + file.getAbsolutePath() + ", please try again, if the "
          + "problem does not go away please report a bug: " + UrlConstants.GITHUB_ISSUES);
    }
  }

  private static JSONObject matchCurrentVersion(final JSONArray lobbyProps, final Version currentVersion) {
    checkNotNull(lobbyProps);

    return OpenJsonUtils.stream(lobbyProps)
        .map(JSONObject.class::cast)
        .filter(props -> currentVersion.equals(new Version(props.getString("version"))))
        .findFirst()
        .orElse(lobbyProps.getJSONObject(0));
  }

  private static JSONArray loadYaml(final File yamlFile) throws IOException {
    final String yamlContent = new String(Files.readAllBytes(yamlFile.toPath()), StandardCharsets.UTF_8);
    final Yaml yaml = new Yaml();
    return new JSONArray(yaml.loadAs(yamlContent, List.class));
  }
}
