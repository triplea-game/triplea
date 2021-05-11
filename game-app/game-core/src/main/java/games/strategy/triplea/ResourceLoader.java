package games.strategy.triplea;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.file.system.loader.DownloadedMapsListing;
import games.strategy.engine.framework.startup.launcher.MapNotFoundException;
import games.strategy.triplea.ui.OrderedProperties;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.ImageLoader;
import org.triplea.java.UrlStreams;
import org.triplea.swing.SwingComponents;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
@Slf4j
public class ResourceLoader implements Closeable {
  public static final String ASSETS_FOLDER = "assets";

  private final URLClassLoader loader;

  @Getter private final List<URL> searchUrls;
  @Getter private final Path mapLocation;

  public ResourceLoader(final String mapName) {
    mapLocation =
        mapName == null || mapName.isBlank()
            ? null
            : DownloadedMapsListing.parseMapFiles()
                .findContentRootForMapName(mapName)
                .orElseThrow(
                    () -> {
                      SwingComponents.promptUser(
                          "Download Map?",
                          "Map missing: "
                              + mapName
                              + ", could not join game.\nWould you like to download the map now?"
                              + "\nOnce the download completes, you may reconnect to this game.",
                          () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));

                      return new MapNotFoundException(mapName);
                    });

    // Add the assets folder from the game installation path. This assets folder supplements
    // any map resources.
    final File gameAssetsDirectory =
        findDirectory(ClientFileSystemHelper.getRootFolder().toFile(), ASSETS_FOLDER)
            .orElseThrow(GameAssetsNotFoundException::new);

    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.
    try {
      searchUrls = new ArrayList<>();
      if (mapLocation != null) {
        searchUrls.add(mapLocation.toUri().toURL());
      }
      searchUrls.add(gameAssetsDirectory.toURI().toURL());
      loader = new URLClassLoader(searchUrls.toArray(URL[]::new));
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(
          "Error creating file system paths with map: "
              + mapName
              + ", engine assets path: "
              + gameAssetsDirectory.getAbsolutePath()
              + ", and path to map: "
              + mapLocation.toAbsolutePath(),
          e);
    }
  }

  @VisibleForTesting
  ResourceLoader(final URLClassLoader loader) {
    this.loader = loader;
    searchUrls = List.of();
    mapLocation = null;
  }

  /**
   * Resource loader that loads generic sounds and images, no map loaded. A standard resource loader
   * will look for map assets first before falling back to game engine assets. This resource loader
   * is to be used in the launching screens before any map has been launched.
   */
  public static ResourceLoader getGameEngineAssetLoader() {
    return new ResourceLoader("");
  }

  /**
   * Loads an image from the 'assets' folder. Images downloaded as part of the build to be included
   * with the game are downloaded to this location. Check the gradle build file download images task
   * for more information on what will be contained in that folder.
   */
  public static Image loadImageAsset(final Path path) {
    return ImageLoader.getImage(Path.of(ASSETS_FOLDER).resolve(path).toFile());
  }

  private static class GameAssetsNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -8274500540886412040L;

    GameAssetsNotFoundException() {
      super(
          "Unable to find game assets folder starting from location: "
              + ClientFileSystemHelper.getRootFolder().toAbsolutePath()
              + "\nThere is a problem with the installation, please report this to TripleA "
              + "and the path where TripleA is installed.");
    }
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

  @Override
  public void close() {
    try {
      loader.close();
    } catch (final IOException e) {
      log.error("Failed to close resource loader", e);
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
    return findResource(inputPath).orElse(null);
  }

  /**
   * Returns the URL of the resource at the specified path or {@code null} if the resource does not
   * exist. Tries the given 2 paths in order first in the map resources then engine resources.
   *
   * @param inputPath (The name of a resource is a '/'-separated path name that identifies the
   *     resource. Do not use '\' or File.separator)
   * @param inputPath2 Same as inputPath but this takes second priority when loading
   */
  public @Nullable URL getResource(final String inputPath, final String inputPath2) {
    return findResource(inputPath).or(() -> findResource(inputPath2)).orElse(null);
  }

  private Optional<URL> findResource(final String searchPath) {
    return loader.resources(searchPath).findFirst();
  }

  public Optional<Path> optionalResource(final String path) {
    return findResource(path)
        .map(
            url -> {
              try {
                return url.toURI();
              } catch (final URISyntaxException e) {
                throw new IllegalStateException(e);
              }
            })
        .map(Path::of);
  }

  public Path requiredResource(final String path) throws IOException {
    return optionalResource(path).orElseThrow(() -> new FileNotFoundException(path));
  }

  public Optional<Image> loadImage(final String imageName) {
    final URL url = getResource(imageName);
    if (url == null) {
      // this is actually pretty common that we try to read images that are not there. Let the
      // caller
      // decide if this is an error or not.
      return Optional.empty();
    }
    try {
      final BufferedImage bufferedImage = ImageIO.read(url);
      if (bufferedImage == null) {
        log.error("Unsupported Image Format: " + url);
      }
      return Optional.ofNullable(bufferedImage);
    } catch (final IOException e) {
      log.error("Image loading failed: " + imageName, e);
      return Optional.empty();
    }
  }

  public Properties loadAsResource(final String fileName) {
    final Properties properties = new OrderedProperties();
    final URL url = getResource(fileName);
    if (url != null) {
      final Optional<InputStream> optionalInputStream = UrlStreams.openStream(url);
      if (optionalInputStream.isPresent()) {
        try (InputStream inputStream = optionalInputStream.get()) {
          properties.load(inputStream);
        } catch (final IOException e) {
          log.error("Error reading " + fileName, e);
        }
      }
    }
    return properties;
  }
}
