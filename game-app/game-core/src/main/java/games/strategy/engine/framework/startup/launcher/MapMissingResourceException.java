package games.strategy.engine.framework.startup.launcher;

import java.nio.file.Path;
import java.util.List;
import lombok.Getter;

/**
 * Thrown when a map is installed but is missing a required resource file (such as {@code
 * centers.txt} or {@code polygons.txt}) needed to load the map data. This typically indicates a
 * corrupted map download or a map that is still being authored.
 */
@Getter
public class MapMissingResourceException extends IllegalStateException {
  private static final long serialVersionUID = 1L;

  private final String resourceName;
  private final List<Path> searchedPaths;

  public MapMissingResourceException(final String resourceName, final List<Path> searchedPaths) {
    super(
        "Map is missing required resource '"
            + resourceName
            + "' (searched: "
            + searchedPaths
            + ")");
    this.resourceName = resourceName;
    this.searchedPaths = List.copyOf(searchedPaths);
  }
}
