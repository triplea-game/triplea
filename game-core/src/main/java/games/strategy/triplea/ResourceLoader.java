package games.strategy.triplea;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.file.system.loader.DownloadedMaps;
import games.strategy.engine.framework.startup.launcher.MapNotFoundException;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.ImageLoader;
import org.triplea.java.UrlStreams;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.swing.SwingComponents;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
@Slf4j
public class ResourceLoader implements Closeable {
  public static final String ASSETS_FOLDER = "assets";

  private final URLClassLoader loader;
  @Getter private final String mapName;

  public ResourceLoader(final String mapName) {
    Preconditions.checkNotNull(mapName);

    final File mapLocation =
        DownloadedMaps.findPathToMapFolder(mapName)
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
        findDirectory(ClientFileSystemHelper.getRootFolder(), ASSETS_FOLDER)
            .orElseThrow(GameAssetsNotFoundException::new);

    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.
    try {
      loader =
          new URLClassLoader(
              new URL[] {mapLocation.toURI().toURL(), gameAssetsDirectory.toURI().toURL()});
    } catch (final MalformedURLException e) {
      throw new IllegalArgumentException(
          "Error creating file system paths with map: "
              + mapName
              + ", engine assets path: "
              + gameAssetsDirectory.getAbsolutePath()
              + ", and path to map: "
              + mapLocation.getAbsolutePath(),
          e);
    }
    this.mapName = mapName;
  }

  /**
   * Loads an image from the 'assets' folder. Images downloaded as part of the build to be included
   * with the game are downloaded to this location. Check the gradle build file download images task
   * for more information on what will be contained in that folder.
   */
  public static Image loadImageAssert(final Path path) {
    return ImageLoader.getImage(Path.of(ASSETS_FOLDER).resolve(path).toFile());
  }

  private static class GameAssetsNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -8274500540886412040L;

    GameAssetsNotFoundException() {
      super(
          "Unable to find game assets folder starting from location: "
              + ClientFileSystemHelper.getRootFolder().getAbsolutePath()
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
    return findResource(inputPath) //
        .or(() -> findResource(inputPath))
        .orElse(null);
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
    return findResource(inputPath)
        .or(() -> findResource(inputPath2))
        .or(() -> findResource(inputPath))
        .or(() -> findResource(inputPath2))
        .orElse(null);
  }

  private Optional<URL> findResource(final String searchPath) {
    return loader.resources(searchPath).findFirst();
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

  public ThrowingSupplier<InputStream, IOException> optionalResource(final String path) {
    return () ->
        Optional.ofNullable(getResourceAsStream(path))
            .orElseGet(() -> new ByteArrayInputStream(new byte[0]));
  }

  public ThrowingSupplier<InputStream, IOException> requiredResource(final String path) {
    return () ->
        Optional.ofNullable(getResourceAsStream(path))
            .orElseThrow(() -> new FileNotFoundException(path));
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
}
