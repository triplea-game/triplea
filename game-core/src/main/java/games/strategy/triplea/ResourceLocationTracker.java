package games.strategy.triplea;

import java.net.URL;
import java.util.Arrays;

/**
 * Utility class containing the logic for whether or not to create a special resource loading path prefix.
 */
class ResourceLocationTracker {

  /**
   * master zip is the zipped folder format you get when downloading from a map repo via the 'clone or download' button.
   */
  static final String MASTER_ZIP_MAGIC_PREFIX = "-master/map/";

  static final String MASTER_ZIP_IDENTIFYING_SUFFIX = "-master.zip";

  private final String mapPrefix;

  /**
   *
   * @param mapName Used to construct any special resource loading path prefixes, used as needed depending upon which
   *        resources are in the path
   * @param resourcePaths The list of paths used for a map as resources. From this we can determine if the map is being
   *        loaded from a zip or a directory, and if zip, if it matches any particular naming.
   */
  ResourceLocationTracker(final String mapName, final URL[] resourcePaths) {
    final boolean isUsingMasterZip = Arrays.stream(resourcePaths)
        .map(Object::toString)
        .anyMatch(path -> path.endsWith(MASTER_ZIP_IDENTIFYING_SUFFIX));


    // map skins will have the full path name as their map name.
    if (mapName.endsWith("-master.zip")) {
      mapPrefix = mapName.substring(0, mapName.length() - "-master.zip".length()) + MASTER_ZIP_MAGIC_PREFIX;
    } else {
      mapPrefix = isUsingMasterZip ? (mapName + MASTER_ZIP_MAGIC_PREFIX) : "";
    }
  }

  /**
   * Will return an empty string unless a special prefix is needed, in which case that prefix is constructed
   * based on the map name.
   *
   * <p>
   * The 'mapPrefix' is the path within a map zip file where we will then find any map contents.
   * For example, if the map prefix is "map", then when we expand the map zip, we would expect
   * "/map" to be the first folder we see, and we woudl expect things like "/map/game" and
   * "/map/polygons.txt" to exist.
   * </p>
   */
  String getMapPrefix() {
    return mapPrefix;
  }
}
