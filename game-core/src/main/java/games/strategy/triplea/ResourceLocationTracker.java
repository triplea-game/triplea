package games.strategy.triplea;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;

/**
 * Utility class containing the logic for whether or not to create a special resource loading path
 * prefix.
 */
@UtilityClass
class ResourceLocationTracker {
  // All maps must have at least a "baseTiles" folder
  private static final String REQUIRED_ASSET_EXAMPLE_FOLDER = "baseTiles/";

  /**
   * Will return an empty string unless a special prefix is needed, in which case that prefix is *
   * constructed based on where the {@code baseTiles} folder is located within the zip.
   *
   * @param resourcePaths The list of paths used for a map as resources. From this we can determine
   *     if the map is being loaded from a zip or a directory, and if zip, if it matches any
   *     particular naming.
   */
  static String getMapPrefix(final URL[] resourcePaths) {
    for (final URL url : resourcePaths) {
      try (ZipFile zip = new ZipFile(new File(url.toURI()))) {
        final Optional<? extends ZipEntry> e =
            zip.stream().filter($ -> $.getName().endsWith(REQUIRED_ASSET_EXAMPLE_FOLDER)).findAny();
        if (e.isPresent()) {
          final String path = e.get().getName();
          return path.substring(0, path.length() - REQUIRED_ASSET_EXAMPLE_FOLDER.length());
        }
      } catch (final IOException | URISyntaxException e) {
        // File is not a zip or can't be opened
      }
    }
    return "";
  }
}
