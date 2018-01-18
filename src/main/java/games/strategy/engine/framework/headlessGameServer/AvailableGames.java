package games.strategy.engine.framework.headlessGameServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.util.UrlStreams;

/**
 * A list of all available games. We make sure we can parse them all, but we don't keep them in memory.
 */
public class AvailableGames {
  private static final String ZIP_EXTENSION = ".zip";
  private final Map<String, URI> availableGames = new TreeMap<>();
  private final Set<String> availableMapFolderOrZipNames = new HashSet<>();

  AvailableGames() {
    populateAvailableGames(
        Collections.synchronizedMap(availableGames),
        Collections.synchronizedSet(availableMapFolderOrZipNames));
  }

  Set<String> getGameNames() {
    return new HashSet<>(availableGames.keySet());
  }

  Set<String> getAvailableMapFolderOrZipNames() {
    return Collections.unmodifiableSet(availableMapFolderOrZipNames);
  }

  /**
   * Can return null.
   */
  public GameData getGameData(final String gameName) {
    return Optional.ofNullable(availableGames.get(gameName))
        .map(AvailableGames::getGameDataFromXml)
        .orElse(null);
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

  private static void populateAvailableGames(
      final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames) {

    Arrays.asList(Optional.ofNullable(ClientFileSystemHelper.getUserMapsFolder().listFiles())
        .orElse(new File[0]))
        .parallelStream()
        .forEach(map -> {
          if (map.isDirectory()) {
            populateFromDirectory(map, availableGames, availableMapFolderOrZipNames);
          } else if (map.isFile() && map.getName().toLowerCase().endsWith(ZIP_EXTENSION)) {
            populateFromZip(map, availableGames, availableMapFolderOrZipNames);
          }
        });
  }

  private static void populateFromDirectory(
      final File mapDir,
      final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames) {
    final File games = new File(mapDir, "games");
    if (!games.exists()) {
      // no games in this map dir
      return;
    }
    if (games.listFiles() != null) {
      for (final File game : games.listFiles()) {
        if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
          final boolean added = addToAvailableGames(game.toURI(), availableGames);
          if (added) {
            availableMapFolderOrZipNames.add(mapDir.getName());
          }
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
            try {
              final boolean added = addToAvailableGames(
                  new URI(url.toString().replace(" ", "%20")),
                  availableGames);
              if (added && map.getName().length() > 4) {
                availableMapFolderOrZipNames
                    .add(map.getName().substring(0, map.getName().length() - ZIP_EXTENSION.length()));
              }
            } catch (final URISyntaxException e) {
              // only happens when URI couldn't be build and therefore no entry was added. That's fine
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

  private static GameData getGameDataFromXml(final URI uri) {
    if (uri == null) {
      return null;
    }

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        return GameParser.parse(uri.toString(), input);
      } catch (final Exception e) {
        ClientLogger.logError("Exception while parsing: " + uri.toString(), e);
      }
    }
    return null;
  }
}
