package games.strategy.engine.config.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.StreamSupport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.Version;

/**
 * Parses a downloaded lobby properties file (yaml format expected).
 * Lobby properties include IP address and port of the lobby.
 */
class LobbyPropertyFileParser {


  public static LobbyServerProperties parse(final File file, final Version currentVersion) {
    try {
      return new LobbyServerProperties(matchCurrentVersion(loadYaml(file), currentVersion).toMap());
    } catch (final IOException e) {
      throw new RuntimeException("Failed loading file: " + file.getAbsolutePath() + ", please try again, if the "
          + "problem does not go away please report a bug: " + UrlConstants.GITHUB_ISSUES);
    }
  }

  private static JSONObject matchCurrentVersion(final JSONArray lobbyProps, final Version currentVersion) {
    checkNotNull(lobbyProps);

    return StreamSupport.stream(lobbyProps.spliterator(), false)
        .map(JSONObject.class::cast)
        .filter(props -> currentVersion.equals(props.opt("version")))
        .findFirst()
        .orElse(lobbyProps.getJSONObject(0));
  }

  private static JSONArray loadYaml(final File yamlFile) throws IOException {
    final String yamlContent = new String(Files.readAllBytes(yamlFile.toPath()));
    final Yaml yaml = new Yaml();
    return new JSONArray(yaml.loadAs(yamlContent, List.class));
  }
}
