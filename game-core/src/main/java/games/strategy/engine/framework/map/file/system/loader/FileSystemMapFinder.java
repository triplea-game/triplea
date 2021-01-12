package games.strategy.engine.framework.map.file.system.loader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import java.io.File;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/** Internal utility class to find the path of a given map by name. */
@UtilityClass
class FileSystemMapFinder {

  static Optional<File> getPath(final String mapName) {
    return getCandidatePaths(mapName, ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(File::exists)
        .findFirst();
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
  static List<File> getCandidatePaths(final String mapName, final File userMapsFolder) {
    Preconditions.checkNotNull(mapName);

    final String dirName = File.separator + mapName;
    final String normalizedMapName = File.separator + normalizeMapName(mapName) + "-master";
    return List.of(
        new File(userMapsFolder, dirName + File.separator + "map"),
        new File(userMapsFolder, dirName),
        new File(userMapsFolder, normalizedMapName + File.separator + "map"),
        new File(userMapsFolder, normalizedMapName));
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
}
