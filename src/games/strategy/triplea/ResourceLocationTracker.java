package games.strategy.triplea;

import java.net.URL;
import java.util.Arrays;

/**
 * Utility class containing the logic for whether or not to create a special resource loading path prefix.
 */
class ResourceLocationTracker {

  /**
   * master zip is the zipped folder format you get when downloading from a map repo via the 'clone or download' button
   */
  static final String MASTER_ZIP_MAGIC_PREFIX = "-master/map/";

  static final String MASTER_ZIP_IDENTIFYING_SUFFIX = "-master.zip";

  private final String mapPrefix;

  /**
   *
   * @param mapName Used to construct any special resource loading path prefixes, used as needed depending upon which
   *                resources are in the path
   * @param resourcePaths The list of paths used for a map as resources. From this we can determine if the map is being
   *                      loaded from a zip or a directory, and if zip, if it matches any particular naming.
   */
  ResourceLocationTracker(String mapName, URL[] resourcePaths) {
    boolean isUsingMasterZip = Arrays.asList(resourcePaths)
        .stream().filter(path -> path.toString().endsWith(MASTER_ZIP_IDENTIFYING_SUFFIX)).findAny().isPresent();
    mapPrefix = isUsingMasterZip ? mapName + MASTER_ZIP_MAGIC_PREFIX : "";
  }

  /**
   * Will return an empty string unless a special prefix is needed, in which case  that prefix is constructed
   * basd on the map name.
   */
  String getMapPrefix() {
    return mapPrefix;
  }
}
