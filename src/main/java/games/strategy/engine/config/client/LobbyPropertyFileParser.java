package games.strategy.engine.config.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

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
    final JSONArray lobbyProperties;
    try {
      lobbyProperties = loadYaml(file);
    } catch (final IOException e) {
      throw new RuntimeException("Failed loading file: " + file.getAbsolutePath() + ", please try again, if the "
          + "problem does not go away please report a bug: " + UrlConstants.GITHUB_ISSUES);
    }
    return new LobbyServerProperties(matchCurrentVersion(lobbyProperties, currentVersion).toMap());
  }

  private static JSONObject matchCurrentVersion(
      final JSONArray lobbyProps,
      final Version currentVersion) {
    checkNotNull(lobbyProps);

    for (int i = 0; i < lobbyProps.length(); i++) {
      final JSONObject currentObject = lobbyProps.getJSONObject(i);
      if (currentVersion.equals(currentObject.opt("version"))) {
        return currentObject;
      }
    }
    return lobbyProps.getJSONObject(0);
  }

  private static JSONArray loadYaml(final File yamlFile) throws IOException {
    final String yamlContent = new String(Files.readAllBytes(yamlFile.toPath()));
    final Yaml yaml = new Yaml();
    return new JSONArray(yaml.loadAs(yamlContent, List.class));
  }
}
