package org.triplea.game.server;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import lombok.extern.java.Log;
import org.triplea.io.FileUtils;
import org.triplea.java.UrlStreams;

/**
 * A list of all available games. We make sure we can parse them all, but we don't keep them in
 * memory.
 */
@Log
@Immutable
final class AvailableGames {
  private static final String ZIP_EXTENSION = ".zip";
  private final Map<String, URI> availableGames;

  AvailableGames() {
    availableGames = Collections.synchronizedMap(new HashMap<>());

    FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder())
        .parallelStream()
        .forEach(
            map -> {
              log.info("Loading map: " + map);
              if (map.isDirectory()) {
                availableGames.putAll(getGamesFromDirectory(map));
              } else if (map.isFile() && map.getName().toLowerCase().endsWith(ZIP_EXTENSION)) {
                availableGames.putAll(getGamesFromZip(map));
              }
            });
    log.info(
        String.format(
            "Done loading maps, " + "availableGames count: %s, contents: %s",
            availableGames.keySet().size(), availableGames.keySet()));
  }

  private static Map<String, URI> getGamesFromDirectory(final File mapDir) {
    final Map<String, URI> availableGames = new HashMap<>();
    final File games = new File(mapDir, "games");
    for (final File game : FileUtils.listFiles(games)) {
      if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
        availableGames.putAll(getAvailableGames(game.toURI()));
      }
    }
    return availableGames;
  }

  private static Map<String, URI> getGamesFromZip(final File map) {
    final Map<String, URI> availableGames = new HashMap<>();

    try (InputStream fis = new FileInputStream(map);
        ZipInputStream zis = new ZipInputStream(fis);
        URLClassLoader loader = new URLClassLoader(new URL[] {map.toURI().toURL()})) {
      ZipEntry entry = zis.getNextEntry();
      while (entry != null) {
        if (entry.getName().contains("games/") && entry.getName().toLowerCase().endsWith(".xml")) {
          final URL url = loader.getResource(entry.getName());
          if (url != null) {
            availableGames.putAll(
                getAvailableGames(URI.create(url.toString().replace(" ", "%20"))));
          }
        }
        // we have to close the loader to allow files to be deleted on windows
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Error reading zip file in: " + map.getAbsolutePath(), e);
    }
    return availableGames;
  }

  private static Map<String, URI> getAvailableGames(@Nonnull final URI uri) {
    final Map<String, URI> availableGames = new HashMap<>();

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        final GameData data = GameParser.parseShallow(uri.toString(), input);
        final String name = data.getGameName();
        availableGames.put(name, uri);
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Exception while parsing: " + uri.toString(), e);
      }
    }
    return availableGames;
  }

  boolean hasGame(final String gameName) {
    return availableGames.containsKey(gameName);
  }

  Set<String> getGameNames() {
    return availableGames.keySet();
  }

  /**
   * Returns the path to the file associated with the specified game.
   *
   * <p>The "path" is actually a URI in string form.
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   * @return The path to the game file; or {@code null} if the game is not available.
   */
  String getGameFilePath(final String gameName) {
    return Optional.ofNullable(availableGames.get(gameName)).map(Object::toString).orElse(null);
  }

  /** Can return null. */
  GameData getGameData(final String gameName) {
    return Optional.ofNullable(availableGames.get(gameName))
        .flatMap(AvailableGames::parse)
        .orElse(null);
  }

  private static Optional<GameData> parse(final URI uri) {
    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        return Optional.of(GameParser.parse(uri.toString(), input));
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Exception while parsing: " + uri.toString(), e);
      }
    }
    return Optional.empty();
  }
}
