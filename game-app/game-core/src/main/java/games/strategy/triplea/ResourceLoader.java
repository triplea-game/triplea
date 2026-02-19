package games.strategy.triplea;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.ui.OrderedProperties;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.PathUtils;
import org.triplea.java.Postconditions;
import org.triplea.java.UrlStreams;

/**
 * Utility for managing where images and property files for maps and units should be loaded from.
 * Based on java Classloaders.
 */
@Slf4j
public class ResourceLoader implements Closeable {
  private static final String ASSETS_FOLDER = "assets";

  private final URLClassLoader loader;

  @Getter private final List<Path> assetPaths;

  /**
   * Assembles the full path to an asset from the root of the classpath using the given path
   * components.
   *
   * <p>Note that classpath resources are always loaded using '/', regardless of the file platform
   * separator, so ensure that's the separator we're using.
   *
   * @param assetsImageFileStrings segments of the path from the assets folder to an image, eg:
   *     {@code getAssetsImageFileLocation("folder-in-assets", "image.png");}
   * @return the full path from the root of the classpath, eg: {@code
   *     "/assets/folder-in-assets/image.png"}
   */
  public static String getAssetsFileLocation(String... assetsImageFileStrings) {
    String assetsFileLocation =
        ASSETS_FOLDER + File.separator + String.join(File.separator, assetsImageFileStrings);
    return assetsFileLocation.replace(File.separatorChar, '/');
  }

  public ResourceLoader(@Nonnull final Path assetFolderPath) {
    this(List.of(assetFolderPath));
  }

  public ResourceLoader(List<Path> assetPaths) {
    this.assetPaths = assetPaths;
    List<URL> searchUrls = assetPaths.stream().map(PathUtils::toUrl).toList();

    // Note: URLClassLoader does not always respect the ordering of the search URLs
    // To solve this we will get all matching paths and then filter by what matched
    // the assets folder.

    loader = new URLClassLoader(searchUrls.toArray(URL[]::new));
  }

  @VisibleForTesting
  ResourceLoader(final URLClassLoader loader) {
    this.loader = loader;
    this.assetPaths = List.of();
  }

  /**
   * Searches from a starting directory for a given directory. If not found, recursively goes up to
   * parent directories searching for the given directory.
   *
   * @param startFolder The start of the search path.
   * @param targetFolderName The name of the directory to find (must be a directory, not a file)
   * @return Path of the directory as found, otherwise empty.
   */
  @VisibleForTesting
  static Optional<Path> findDirectory(final Path startFolder, final String targetFolderName) {
    for (Path currentDir = startFolder; currentDir != null; currentDir = currentDir.getParent()) {
      final Path targetDir = currentDir.resolve(targetFolderName);
      if (Files.isDirectory(targetDir)) {
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

  public boolean hasPathString(final String pathString) {
    return loader.getResource(pathString) != null;
  }

  /**
   * Returns the URL of the resource at the specified path or {@code null} if the resource does not
   * exist.
   *
   * @param inputPathString (The name of a resource is a '/'-separated path name that identifies the
   *     resource. Do not use '\' or {@code File.separator})
   */
  public @Nullable URL getResource(final String inputPathString) {
    return findResource(inputPathString).orElse(null);
  }

  /**
   * Returns the URL of the resource at the specified path or {@code null} if the resource does not
   * exist. Tries the given 2 paths in order first in the map resources then engine resources.
   *
   * @param inputPathString (The name of a resource is a '/'-separated path name that identifies the
   *     resource. Do not use '\' or {@code File.separator})
   * @param inputPathString2 Same as {@code inputPathString} but this takes second priority when
   *     loading
   */
  public @Nullable URL getResource(final String inputPathString, final String inputPathString2) {
    return findResource(inputPathString).or(() -> findResource(inputPathString2)).orElse(null);
  }

  private Optional<URL> findResource(final String searchPathString) {
    return loader.resources(searchPathString).findFirst();
  }

  public Optional<Path> optionalResource(final String pathString) {
    return findResource(pathString)
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

  public Path requiredResource(final String pathString) throws IOException {
    return optionalResource(pathString).orElseThrow(() -> new FileNotFoundException(pathString));
  }

  public Optional<Image> loadImage(final String imageName) {
    final var bufferedImage = loadBufferedImage(imageName);
    return Optional.ofNullable(bufferedImage.orElse(null));
  }

  @VisibleForTesting
  String createResourcePathString(String firstPathElement, String... furtherPathStrings) {
    StringBuilder sb = new StringBuilder(firstPathElement);
    for (String element : furtherPathStrings) {
      sb.append('/').append(element);
    }

    String result = sb.toString();
    Postconditions.assertState(
        !result.isEmpty(),
        String.format(
            "Resource path cannot be empty (first element: %s, further path elements: %s)",
            firstPathElement, Arrays.toString(furtherPathStrings)));
    Postconditions.assertState(
        !result.equals("/"),
        String.format(
            "Resource path cannot be \"/\" (first element: %s, further path elements: %s)",
            firstPathElement, Arrays.toString(furtherPathStrings)));

    return result;
  }

  /**
   * Tries to load images in a priority order, first from the map, then from engine assets.
   *
   * @param firstPathElementString the image file name or the first element of the path to the image
   *     relative to the map folder of the game resp. the assets folder of the engine
   * @param furtherPathString zero or more further elements of the path to the image
   * @return the image or null, if the image could not be found
   */
  public Optional<BufferedImage> loadBufferedImage(
      final String firstPathElementString, final String... furtherPathString) {
    final String imagePathString =
        createResourcePathString(firstPathElementString, furtherPathString);
    URL url = getResource(imagePathString);
    if (url == null) {
      // Upon first failure to find resource, try to fallback to /assets
      url = getResource(createResourcePathString(ASSETS_FOLDER, imagePathString));
      if (url == null) {
        // this is actually pretty common that we try to read images that are not there. Let the
        // caller decide if this is an error or not.
        return Optional.empty();
      }
    }
    try {
      final BufferedImage bufferedImage = ImageIO.read(url);
      if (bufferedImage == null) {
        log.error("Unsupported Image Format: " + url);
      }
      return Optional.ofNullable(bufferedImage);
    } catch (final IOException e) {
      log.error("Image loading failed: " + imagePathString, e);
      return Optional.empty();
    }
  }

  public Properties loadPropertyFile(final String fileName) {
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
