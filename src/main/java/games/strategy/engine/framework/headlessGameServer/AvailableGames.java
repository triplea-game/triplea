package games.strategy.engine.framework.headlessGameServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.triplea.Constants;
import games.strategy.util.UrlStreams;

/**
 * A list of all available games. We make sure we can parse them all, but we don't keep them in memory.
 */
public class AvailableGames {
  private static final boolean delayedParsing = false;
  private static final String ZIP_EXTENSION = ".zip";
  private final Map<String, URI> availableGames = Collections.synchronizedMap(new TreeMap<>());
  private final Set<String> availableMapFolderOrZipNames = Collections.synchronizedSet(new HashSet<>());

  AvailableGames() {
    populateAvailableGames(availableGames, availableMapFolderOrZipNames, Collections.synchronizedSet(new HashSet<>()));
  }

  List<String> getGameNames() {
    return new ArrayList<>(availableGames.keySet());
  }

  Set<String> getAvailableMapFolderOrZipNames() {
    return new HashSet<>(availableMapFolderOrZipNames);
  }

  /**
   * Can return null.
   */
  public GameData getGameData(final String gameName) {
    return getGameDataFromXml(availableGames.get(gameName));
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
    return Optional.ofNullable(availableGames.get(gameName)).map(Object::toString).orElse(null);
  }

  private static void populateAvailableGames(final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames, final Set<String> mapNamePropertyList) {
    System.out.println("Parsing all available games (this could take a while). ");
    final List<File> files = allMapFiles();
    if (files.size() > 0) {
      final ExecutorService service = Executors.newWorkStealingPool(files.size());
      final List<Future<?>> tasks = new ArrayList<>(files.size());
      for (final File map : files) {
        if (map.isDirectory()) {
          tasks.add(service.submit(
              () -> populateFromDirectory(map, availableGames, availableMapFolderOrZipNames, mapNamePropertyList)));
        } else if (map.isFile() && map.getName().toLowerCase().endsWith(ZIP_EXTENSION)) {
          tasks.add(service.submit(
              () -> populateFromZip(map, availableGames, availableMapFolderOrZipNames, mapNamePropertyList)));
        }
      }
      service.shutdown();
      for (Future<?> future : tasks) {
        try {
          future.get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
          throw new RuntimeException(e);
        }
      }
    }
    System.out.println("Finished parsing all available game xmls. ");
  }

  private static List<File> allMapFiles() {
    final List<File> files = new ArrayList<>();
    files.addAll(safeListFiles(ClientFileSystemHelper.getUserMapsFolder()));
    return files;
  }

  private static List<File> safeListFiles(final File f) {
    final File[] files = f.listFiles();
    if (files == null) {
      return Collections.emptyList();
    }
    return Arrays.asList(files);
  }

  private static void populateFromDirectory(
      final File mapDir,
      final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames,
      final Set<String> mapNamePropertyList) {
    final File games = new File(mapDir, "games");
    if (!games.exists()) {
      // no games in this map dir
      return;
    }
    if (games.listFiles() != null) {
      for (final File game : games.listFiles()) {
        if (game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
          final boolean added = addToAvailableGames(game.toURI(), availableGames, mapNamePropertyList);
          if (added) {
            availableMapFolderOrZipNames.add(mapDir.getName());
          }
        }
      }
    }
  }

  private static void populateFromZip(final File map, final Map<String, URI> availableGames,
      final Set<String> availableMapFolderOrZipNames, final Set<String> mapNamePropertyList) {
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
                  availableGames,
                  mapNamePropertyList);
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


  private static boolean addToAvailableGames(final URI uri, final Map<String, URI> availableGames,
      final Set<String> mapNamePropertyList) {
    if (uri == null) {
      return false;
    }
    final AtomicReference<String> gameName = new AtomicReference<>();

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        final GameData data = new GameParser(uri.toString()).parse(input, gameName, delayedParsing);
        final String name = data.getGameName();
        final String mapName = data.getProperties().get(Constants.MAP_NAME, "");
        if (!availableGames.containsKey(name)) {
          availableGames.put(name, uri);
          if (mapName.length() > 0) {
            mapNamePropertyList.add(mapName);
          }
          return true;
        }
      } catch (final Exception e) {
        ClientLogger.logError("Exception while parsing: " + uri.toString() + " : "
            + (gameName.get() != null ? gameName.get() + " : " : ""), e);
      }
    }
    return false;
  }

  private static GameData getGameDataFromXml(final URI uri) {
    if (uri == null) {
      return null;
    }
    final AtomicReference<String> gameName = new AtomicReference<>();

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isPresent()) {
      try (InputStream input = inputStream.get()) {
        return new GameParser(uri.toString()).parse(input, gameName, false);
      } catch (final Exception e) {
        ClientLogger.logError("Exception while parsing: " + uri.toString() + " : "
            + (gameName.get() != null ? gameName.get() + " : " : ""), e);
      }
    }
    return null;
  }
}
