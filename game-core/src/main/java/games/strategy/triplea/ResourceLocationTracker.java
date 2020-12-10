package games.strategy.triplea;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipFile;

import lombok.val;
import lombok.experimental.UtilityClass;

/**
 * Utility class containing the logic for whether or not to create a special resource loading path
 * prefix.
 */
@UtilityClass
class ResourceLocationTracker {
  private static final String REQUIRED_ASSET_FOLDER = "baseTiles/";

  /**
   * Will return an empty string unless a special prefix is needed, in which case that prefix is *
   * constructed based on where the {@code baseTiles} folder is located within the zip.
   *
   * @param resourcePaths The list of paths used for a map as resources. From this we can determine
   *     if the map is being loaded from a zip or a directory, and if zip, if it matches any
   *     particular naming.
   */
  	static String getMapPrefix(final URL[] resourcePaths) {
  		for (val url : resourcePaths) {
  			try (val zip = new ZipFile(new File(url.toURI()))) {
  				val e = zip.stream().filter($ -> $.getName().endsWith(REQUIRED_ASSET_FOLDER)).findAny();
  				if (e.isPresent()) {
  					val path = e.get().getName();
  					return path.substring(0, path.length() - REQUIRED_ASSET_FOLDER.length());
  				}
  		    } catch (IOException | URISyntaxException e) {
  		        // File is not a zip or can't be opened
  		    }
  		}
  		return "";
  }
}
