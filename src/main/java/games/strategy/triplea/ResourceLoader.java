package games.strategy.triplea;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.launcher.MapNotFoundException;
import games.strategy.ui.SwingComponents;
import games.strategy.util.UrlStreams;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
public class ResourceLoader implements Closeable {
  private final URLClassLoader m_loader;
  public static String RESOURCE_FOLDER = "assets";

  private final ResourceLocationTracker resourceLocationTracker;


  public static ResourceLoader getGameEngineAssetLoader() {
    return getMapResourceLoader("");
  }

  /**
   * Returns a resource loader that will find assets in a map directory.
   */
  public static ResourceLoader getMapResourceLoader(final String mapName) {
    File atFolder = ClientFileSystemHelper.getRootFolder();
    File resourceFolder = new File(atFolder, RESOURCE_FOLDER);

    while (!resourceFolder.exists() && !resourceFolder.isDirectory()) {
      atFolder = atFolder.getParentFile();
      resourceFolder = new File(atFolder, RESOURCE_FOLDER);
    }

    final List<String> dirs = getPaths(mapName);
    if (mapName != null && dirs.isEmpty()) {
      SwingComponents.promptUser("Download Map?",
          "Map missing: " + mapName + ", could not join game.\nWould you like to download the map now?"
              + "\nOnce the download completes, you may reconnect to this game.",
          () -> DownloadMapsWindow.showDownloadMapsWindow(mapName));

      throw new MapNotFoundException();
    }

    dirs.add(resourceFolder.getAbsolutePath());

    ClientLogger.logQuietly("Loading resources from the following paths: " + dirs);
    return new ResourceLoader(mapName, dirs.toArray(new String[dirs.size()]));
  }

  protected static String normalizeMapZipName(final String zipName) {
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
    // find the primary directory/file
    final String dirName = File.separator + mapName;
    final String zipName = dirName + ".zip";
    final List<File> candidates = new ArrayList<>();
    // prioritize user maps folder over root folder
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), dirName + File.separator + "map"));
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), dirName));
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), zipName));

    final String normalizedZipName = normalizeMapZipName(zipName);

    // clicking github 'clone or download' and downloading the zip gives a zip that ends with "-master.zip"
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), normalizeMapZipName(mapName) + "-master.zip"));
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), mapName + "-master.zip"));
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), zipName));
    candidates.add(new File(ClientFileSystemHelper.getUserMapsFolder(), normalizedZipName));

    final Optional<File> match = candidates.stream().filter(file -> file.exists()).findFirst();
    if (!match.isPresent()) {
      // if we get no results, we will eventually prompt the user to download the map
      return new ArrayList<>();
    }
    ClientLogger.logQuietly("Loading map: " + mapName + ", from: " + match.get().getAbsolutePath());

    final List<String> rVal = new ArrayList<>();
    rVal.add(match.get().getAbsolutePath());
    // find dependencies
    try (final URLClassLoader url = new URLClassLoader(new URL[] {match.get().toURI().toURL()})) {
      final URL dependencesURL = url.getResource("dependencies.txt");
      if (dependencesURL != null) {
        final java.util.Properties dependenciesFile = new java.util.Properties();

        final Optional<InputStream> inputStream = UrlStreams.openStream(dependencesURL);
        if (inputStream.isPresent()) {
          try (final InputStream stream = inputStream.get()) {
            dependenciesFile.load(stream);
            final String dependencies = dependenciesFile.getProperty("dependencies");
            final StringTokenizer tokens = new StringTokenizer(dependencies, ",", false);
            while (tokens.hasMoreTokens()) {
              // add the dependencies recursivly
              rVal.addAll(getPaths(tokens.nextToken()));
            }
          }
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
    return rVal;
  }


  private ResourceLoader(final String mapName, final String[] paths) {
    final URL[] urls = new URL[paths.length];
    for (int i = 0; i < paths.length; i++) {
      final File f = new File(paths[i]);
      if (!f.exists()) {
        ClientLogger.logQuietly(f + " does not exist");
      }
      if (!f.isDirectory() && !f.getName().endsWith(".zip")) {
        ClientLogger.logQuietly(f + " is not a directory or a zip file");
      }
      try {
        urls[i] = f.toURI().toURL();
      } catch (final MalformedURLException e) {
        ClientLogger.logQuietly(e);
        throw new IllegalStateException(e.getMessage());
      }
    }
    resourceLocationTracker = new ResourceLocationTracker(mapName, urls);
    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.
    m_loader = new URLClassLoader(urls);
  }

  @Override
  public void close() {
    try {
      m_loader.close();
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  public boolean hasPath(final String path) {
    final URL rVal = m_loader.getResource(path);
    return rVal != null;
  }

  /**
   * @param inputPath
   *        (The name of a resource is a '/'-separated path name that identifies the resource. Do not use '\' or
   *        File.separator)
   */
  public URL getResource(final String inputPath) {
    String path = resourceLocationTracker.getMapPrefix() + inputPath;
    return getMatchingResources(path).stream().findFirst().orElse(
        getMatchingResources(inputPath).stream().findFirst().orElseGet(
            () -> null));
  }

  private List<URL> getMatchingResources(final String path) {
    try {
      return Collections.list(m_loader.getResources(path));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Ensure that you close the InputStream returned by this method when you are done using it.
   */
  public InputStream getResourceAsStream(final String path) {
    final URL url = getResource(path);
    if (url == null) {
      return null;
    }

    final Optional<InputStream> inputStream = UrlStreams.openStream(url);
    if (inputStream.isPresent()) {
      return inputStream.get();
    } else {
      throw new IllegalStateException("Failed to open an input stream to: " + path);
    }
  }
}
