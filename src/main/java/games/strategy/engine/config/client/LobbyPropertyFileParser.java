package games.strategy.engine.config.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.yaml.snakeyaml.Yaml;

import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.Version;

/**
 * Parses a downloaded lobby properties file (yaml format expected).
 * Lobby properties include IP address and port of the lobby.
 */
public class LobbyPropertyFileParser {


  public static LobbyServerProperties parse(final File file, final Version currentVersion) {
    final List<Map<String, Object>> lobbyProperties;
    try {
      lobbyProperties = loadYaml(file);
    } catch (final IOException e) {
      throw new RuntimeException("Failed loading file: " + file.getAbsolutePath() + ", please try again, if the "
          + "problem does not go away please report a bug: " + UrlConstants.GITHUB_ISSUES);
    }
    final Map<String, Object> configForThisVersion = matchCurrentVersion(lobbyProperties, currentVersion);
    return new LobbyServerProperties(configForThisVersion);
  }

  private static Map<String, Object> matchCurrentVersion(
      final List<Map<String, Object>> lobbyProps,
      final Version currentVersion) {
    checkNotNull(lobbyProps);

    final Optional<Map<String, Object>> matchingVersionProps = lobbyProps.stream()
        .filter(props -> currentVersion.equals(props.get("version")))
        .findFirst();
    return matchingVersionProps.orElse(lobbyProps.get(0));
  }

  private static List<Map<String, Object>> loadYaml(final File yamlFile) throws IOException {
    final String yamlContent = new String(Files.readAllBytes(yamlFile.toPath()));
    final Yaml yaml = new Yaml();
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> yamlData = (List<Map<String, Object>>) yaml.load(yamlContent);
    return yamlData;
  }
}
