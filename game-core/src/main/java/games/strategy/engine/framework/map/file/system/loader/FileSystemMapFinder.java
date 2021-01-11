package games.strategy.engine.framework.map.file.system.loader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.ClientFileSystemHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
class FileSystemMapFinder {

  static Optional<File> getPath(final String mapName) {
    return getCandidatePaths(mapName).stream().filter(File::exists).findAny();
  }

  private static List<File> getCandidatePaths(final String mapName) {
    final List<File> candidates = new ArrayList<>();
    candidates.addAll(
        getMapDirectoryCandidates(mapName, ClientFileSystemHelper.getUserMapsFolder()));
    candidates.addAll(getMapZipFileCandidates(mapName, ClientFileSystemHelper.getUserMapsFolder()));
    return candidates;
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
  static List<File> getMapDirectoryCandidates(final String mapName, final File userMapsFolder) {
    Preconditions.checkNotNull(mapName);

    final String dirName = File.separator + mapName;
    final String normalizedMapName = File.separator + normalizeMapName(mapName) + "-master";
    return List.of(
        new File(userMapsFolder, dirName + File.separator + "map"),
        new File(userMapsFolder, dirName),
        new File(userMapsFolder, normalizedMapName + File.separator + "map"),
        new File(userMapsFolder, normalizedMapName));
  }

  /**
   * Returns a list of candidate zip files from which the specified map may be loaded.
   *
   * <p>The candidate zip files are returned in order of preference. That is, a candidate file
   * earlier in the list should be preferred to a candidate file later in the list assuming they
   * both exist.
   *
   * @param mapName The map name; must not be {@code null}.
   * @return A list of candidate zip files; never {@code null}.
   */
  public static List<File> getMapZipFileCandidates(
      final String mapName, final File userMapsFolder) {
    Preconditions.checkNotNull(mapName);

    final String normalizedMapName = normalizeMapName(mapName);
    return List.of(
        new File(userMapsFolder, mapName + ".zip"),
        new File(userMapsFolder, normalizedMapName + "-master.zip"),
        new File(userMapsFolder, normalizedMapName + ".zip"));
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
