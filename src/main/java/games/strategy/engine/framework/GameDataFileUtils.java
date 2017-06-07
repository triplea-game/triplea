package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOCase;

import com.google.common.annotations.VisibleForTesting;

/**
 * A collection of utilities for working with game data files.
 */
public final class GameDataFileUtils {
  private static final String EXTENSION = ".tsvg";

  private static final String LEGACY_EXTENSION = ".svg";

  /**
   * Macs download a game data file as "tsvg.gz", so that extension must be used when evaluating candidate game data
   * files.
   */
  private static final String MAC_OS_ALTERNATIVE_EXTENSION = "tsvg.gz";

  private static final Collection<String> CANDIDATE_EXTENSIONS = Arrays.asList(
      EXTENSION,
      LEGACY_EXTENSION,
      MAC_OS_ALTERNATIVE_EXTENSION);

  private GameDataFileUtils() {}

  /**
   * Appends the game data file extension to the specified file name.
   *
   * @param fileName The file name; must not be {@code null}.
   *
   * @return The file name with the game data file extension appended; never {@code null}.
   */
  public static String addExtension(final String fileName) {
    checkNotNull(fileName);

    return fileName + EXTENSION;
  }

  /**
   * Appends the game data file extension to the specified file name if the file name does not end with the game data
   * file extension.
   *
   * @param fileName The file name; must not be {@code null}.
   *
   * @return The file name ending with at most one occurrence of the game data file extension; never {@code null}.
   */
  public static String addExtensionIfAbsent(final String fileName) {
    checkNotNull(fileName);

    return addExtensionIfAbsent(fileName, IOCase.SYSTEM);
  }

  @VisibleForTesting
  static String addExtensionIfAbsent(final String fileName, final IOCase ioCase) {
    return ioCase.checkEndsWith(fileName, EXTENSION) ? fileName : addExtension(fileName);
  }

  /**
   * Gets the game data file extension.
   *
   * @return The game data file extension including the leading period; never {@code null}.
   */
  public static String getExtension() {
    return EXTENSION;
  }

  /**
   * Indicates the specified file name is a game data file candidate.
   *
   * @param fileName The file name; must not be {@code null}.
   *
   * @return {@code true} if the specified file name is a game data file candidate; otherwise {@code false}.
   */
  public static boolean isCandidateFileName(final String fileName) {
    checkNotNull(fileName);

    return isCandidateFileName(fileName, IOCase.SYSTEM);
  }

  @VisibleForTesting
  static boolean isCandidateFileName(final String fileName, final IOCase ioCase) {
    return CANDIDATE_EXTENSIONS.stream().anyMatch(extension -> ioCase.checkEndsWith(fileName, extension));
  }
}
