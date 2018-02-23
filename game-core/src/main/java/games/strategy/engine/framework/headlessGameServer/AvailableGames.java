package games.strategy.engine.framework.headlessGameServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.io.FileUtils;
import games.strategy.util.UrlStreams;

/**
 * A list of all available games. We make sure we can parse them all, but we don't keep them in memory.
 */
@Immutable
public final class AvailableGames {
  private static final String ZIP_EXTENSION = ".zip";
  private final Map<String, URI> availableGames;
  private final Set<String> availableMapFolderOrZipNames;

  AvailableGames() {
    final GameRepository gameRepository = newGameRepository();
    availableGames = Collections.unmodifiableMap(new TreeMap<>(gameRepository.availableGames));
    availableMapFolderOrZipNames =
        Collections.unmodifiableSet(new HashSet<>(gameRepository.availableMapFolderOrZipNames));
  }

  @ThreadSafe
  private static final class GameRepository {
    final Map<String, URI> availableGames = Collections.synchronizedMap(new HashMap<>());
    final Set<String> availableMapFolderOrZipNames = Collections.synchronizedSet(new HashSet<>());
  }

  private static GameRepository newGameRepository() {
    final GameRepository gameRepository = new GameRepository();
    FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).parallelStream()
        .forEach(map -> {
          if (map.isDirectory()) {
            populateFromDirectory(map, gameRepository.availableGames, gameRepository.availableMapFolderOrZipNames);
          } else if (map.isFile() && map.getName().toLowerCase().endsWith(ZIP_EXTENSION)) {
            populateFromZip(map, gameRepository.availableGames, gameRepository.availableMapFolderOrZipNames);
          }
        });
    return gameRepository;
  }

  private static void populateFromDirectory(
      final File mapDir,
      final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames) {
    final File games = new File(mapDir, "games");
    for (final File game : FileUtils.listFiles(games)) {
      if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
        final boolean added = addToAvailableGames(game.toURI(), availableGames);
        if (added) {
          availableMapFolderOrZipNames.add(mapDir.getName());
        }
      }
    }
  }

  private static void populateFromZip(final File map, final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames) {
    try (InputStream fis = new FileInputStream(map);
        ZipInputStream zis = new ZipInputStream(fis);
        URLClassLoader loader = new URLClassLoader(new URL[] {map.toURI().toURL()})) {
      ZipEntry entry = zis.getNextEntry();
      while (entry != null) {
        if (entry.getName().contains("games/") && entry.getName().toLowerCase().endsWith(".xml")) {
          final URL url = loader.getResource(entry.getName());
          if (url != null) {
            final boolean added = addToAvailableGames(
                URI.create(url.toString().replace(" ", "%20")),
                availableGames);
            if (added && (map.getName().length() > 4)) {
              availableMapFolderOrZipNames
                  .add(map.getName().substring(0, map.getName().length() - ZIP_EXTENSION.length()));
            }
          }
        }
        // we have to close the loader to allow files to be deleted on windows
        zis.closeEntry();
        entry = zis.getNextEntry();
      }
    } catch (final IOException e) {
      ClientLogger.logQuietly("Map: " + map, e);
    }
  }

  private static boolean addToAvailableGames(
      @Nonnull final URI uri,
      @Nonnull final Map<String, URI> availableGames) {
    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        final GameData data = GameParser.parse(uri.toString(), input);
        final String name = data.getGameName();
        if (!availableGames.containsKey(name)) {
          availableGames.put(name, uri);
          return true;
        }
      } catch (final Exception e) {
        ClientLogger.logError("Exception while parsing: " + uri.toString(), e);
      }
    }
    return false;
  }

  Set<String> getGameNames() {
    return new HashSet<>(availableGames.keySet());
  }

  /**
   * Returns the path to the file associated with the specified game.
   *
   * <p>
   * The "path" is actually a URI in string form.
   * </p>
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   *
   * @return The path to the game file; or {@code null} if the game is not available.
   */
  String getGameFilePath(final String gameName) {
    return Optional.ofNullable(availableGames.get(gameName))
        .map(Object::toString)
        .orElse(null);
  }

  /**
   * Can return null.
   */
  public GameData getGameData(final String gameName) {
    return Optional.ofNullable(availableGames.get(gameName))
        .map(uri -> parse(uri).orElse(null))
        .orElse(null);
  }

  private static Optional<GameData> parse(final URI uri) {
    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        return Optional.of(GameParser.parse(uri.toString(), input));
      } catch (final Exception e) {
        ClientLogger.logError("Exception while parsing: " + uri.toString(), e);
      }
    }
    return Optional.empty();
  }

  public boolean containsMapName(final String mapNameProperty) {
    return availableMapFolderOrZipNames.contains(mapNameProperty)
        || availableMapFolderOrZipNames.contains(mapNameProperty + "-master");
  }
}
