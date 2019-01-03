package games.strategy.triplea;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.launcher.MapNotFoundException;
import games.strategy.ui.SwingComponents;
import games.strategy.util.UrlStreams;
import lombok.extern.java.Log;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
@Log
public class ResourceLoader implements Closeable {
  private final URLClassLoader loader;
  public static final String RESOURCE_FOLDER = "assets";

  private final ResourceLocationTracker resourceLocationTracker;

  public static ResourceLoader getGameEngineAssetLoader() {
    return getMapResourceLoader("");
  }

  /** Returns a resource loader that will find assets in a map directory. */
  public static ResourceLoader getMapResourceLoader(final String mapName) {
    final List<String> dirs = getPaths(mapName);
    if (mapName != null && dirs.isEmpty()) {
      SwingComponents.promptUser(
          "Download Map?",
          "Map missing: "
              + mapName
              + ", could not join game.\nWould you like to download the map now?"
              + "\nOnce the download completes, you may reconnect to this game.",
          () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));

      throw new MapNotFoundException();
    }

    findDirectory(ClientFileSystemHelper.getRootFolder(), RESOURCE_FOLDER)
        .map(File::getAbsolutePath)
        .ifPresent(dirs::add);

    return new ResourceLoader(mapName, dirs.toArray(new String[0]));
  }

  @VisibleForTesting
  static Optional<File> findDirectory(final File startDir, final String targetDirName) {
    for (File currentDir = startDir; currentDir != null; currentDir = currentDir.getParentFile()) {
      final File targetDir = new File(currentDir, targetDirName);
      if (targetDir.isDirectory()) {
        return Optional.of(targetDir);
      }
    }

    return Optional.empty();
  }

  /**
   * Returns a list of candidate directories from which the specified map may be loaded.
   *
   * <p>The candidate directories are returned in order of preference. That is, a candidate
   * directory earlier in the list should be preferred to a candidate directory later in the list
   * assuming they both exist.
   *
   * @param mapName The map name; must not be {@code null}.
   * @return A list of candidate directories; never {@code null}.
   */
  @VisibleForTesting
  static List<File> getMapDirectoryCandidates(final String mapName) {
    checkNotNull(mapName);

    final File userMapsFolder = ClientFileSystemHelper.getUserMapsFolder();
    final String dirName = File.separator + mapName;
    final String normalizedMapName = File.separator + normalizeMapName(mapName) + "-master";
    return Arrays.asList(
        new File(userMapsFolder, dirName + File.separator + "map"),
        new File(userMapsFolder, dirName),
        new File(userMapsFolder, normalizedMapName + File.separator + "map"),
        new File(userMapsFolder, normalizedMapName));
  }

  /**
   * Returns a list of candidate zip files from which the specified map may be loaded.
   *
   * <p>The candidate zip files are returned in order of preference. That is, a candidate file
   * earlier in the list should be preferred to a candidate file later in the list assuming they
   * both exist.
   *
   * @param mapName The map name; must not be {@code null}.
   * @return A list of candidate zip files; never {@code null}.
   */
  public static List<File> getMapZipFileCandidates(final String mapName) {
    checkNotNull(mapName);

    final File userMapsFolder = ClientFileSystemHelper.getUserMapsFolder();
    final String normalizedMapName = normalizeMapName(mapName);
    return Arrays.asList(
        new File(userMapsFolder, mapName + ".zip"),
        new File(userMapsFolder, normalizedMapName + "-master.zip"),
        new File(userMapsFolder, normalizedMapName + ".zip"));
  }

  @VisibleForTesting
  static String normalizeMapName(final String zipName) {
    final StringBuilder sb = new StringBuilder();
    Character lastChar = null;

    final String spacesReplaced = zipName.replace(' ', '_');

    for (final char c : spacesReplaced.toCharArray()) {
      // break up camel casing
      if (lastChar != null && Character.isLowerCase(lastChar) && Character.isUpperCase(c)) {
        sb.append("_");
      }
      sb.append(Character.toLowerCase(c));
      lastChar = c;
    }
    return sb.toString();
  }

  private static List<String> getPaths(final String mapName) {
    if (mapName == null) {
      return new ArrayList<>();
    }

    final List<File> candidates = new ArrayList<>();
    candidates.addAll(getMapDirectoryCandidates(mapName));
    candidates.addAll(getMapZipFileCandidates(mapName));

    final Optional<File> match = candidates.stream().filter(File::exists).findFirst();
    if (!match.isPresent()) {
      // if we get no results, we will eventually prompt the user to download the map
      return new ArrayList<>();
    }

    final List<String> paths = new ArrayList<>();
    paths.add(match.get().getAbsolutePath());
    // find dependencies
    try (URLClassLoader url = new URLClassLoader(new URL[] {match.get().toURI().toURL()})) {
      final URL dependencesUrl = url.getResource("dependencies.txt");
      if (dependencesUrl != null) {
        final Optional<InputStream> inputStream = UrlStreams.openStream(dependencesUrl);
        if (inputStream.isPresent()) {
          try (InputStream stream = inputStream.get()) {
            final java.util.Properties dependenciesFile = new java.util.Properties();
            dependenciesFile.load(stream);
            paths.addAll(
                Splitter.on(',')
                    .omitEmptyStrings()
                    .splitToList(dependenciesFile.getProperty("dependencies"))
                    .stream()
                    .map(ResourceLoader::getPaths)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));
          }
        }
      }
    } catch (final Exception e) {
      throw new IllegalStateException(e);
    }
    return paths;
  }

  private ResourceLoader(final String mapName, final String[] paths) {
    final URL[] urls = new URL[paths.length];
    for (int i = 0; i < paths.length; i++) {
      final File f = new File(paths[i]);
      if (!f.exists()) {
        log.severe(f + " does not exist");
      }
      if (!f.isDirectory() && !f.getName().endsWith(".zip")) {
        log.severe(f + " is not a directory or a zip file");
      }
      try {
        urls[i] = f.toURI().toURL();
      } catch (final MalformedURLException e) {
        throw new IllegalStateException(e);
      }
    }
    resourceLocationTracker = new ResourceLocationTracker(mapName, urls);
    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.
    loader = new URLClassLoader(urls);
  }

  @Override
  public void close() {
    try {
      loader.close();
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to close resource loader", e);
    }
  }

  public boolean hasPath(final String path) {
    return loader.getResource(path) != null;
  }

  /**
   * Returns the URL of the resource at the specified path or {@code null} if the resource does not
   * exist.
   *
   * @param inputPath (The name of a resource is a '/'-separated path name that identifies the
   *     resource. Do not use '\' or File.separator)
   */
  public @Nullable URL getResource(final String inputPath) {
    final String path = resourceLocationTracker.getMapPrefix() + inputPath;
    return getMatchingResources(path)
        .stream()
        .findFirst()
        .orElse(getMatchingResources(inputPath).stream().findFirst().orElse(null));
  }

  private List<URL> getMatchingResources(final String path) {
    try {
      return Collections.list(loader.getResources(path));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns an input stream for the specified resource or {@code null} if the resource does not
   * exist. The caller is responsible for closing the returned input stream.
   *
   * @throws IllegalStateException If the specified resource exists but the input stream cannot be
   *     opened.
   */
  public @Nullable InputStream getResourceAsStream(final String path) {
    final URL url = getResource(path);
    if (url == null) {
      return null;
    }

    return UrlStreams.openStream(url)
        .orElseThrow(() -> new IllegalStateException("Failed to open an input stream to: " + path));
  }
}
