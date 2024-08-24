package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.List;
import org.apache.commons.io.IOCase;
import org.jetbrains.annotations.NonNls;

/** A collection of utilities for working with game data files. */
public final class GameDataFileUtils {
  private static final IOCase DEFAULT_IO_CASE = IOCase.SYSTEM;

  private GameDataFileUtils() {}

  /**
   * Appends the game data file extension to the specified file name.
   *
   * @param fileName The file name.
   * @return The file name with the game data file extension appended.
   */
  public static String addExtension(final String fileName) {
    checkNotNull(fileName);

    return fileName + getExtension();
  }

  @VisibleForTesting
  static String addExtensionIfAbsent(final String fileName, final IOCase ioCase) {
    return ioCase.checkEndsWith(fileName, getExtension()) ? fileName : addExtension(fileName);
  }

  private static Collection<String> getCandidateExtensions() {
    @NonNls final String legacyExtension = ".svg";

    // Macs download a game data file as "tsvg.gz", so that extension must be used when evaluating
    // candidate game data files.
    @NonNls final String macOsAlternativeExtension = "tsvg.gz";

    return List.of(getExtension(), legacyExtension, macOsAlternativeExtension);
  }

  /**
   * Gets the game data file extension.
   *
   * @return The game data file extension including the leading period.
   */
  public static String getExtension() {
    return ".tsvg";
  }

  /**
   * Indicates the specified file name is a game data file candidate.
   *
   * @param fileName The file name.
   * @return {@code true} if the specified file name is a game data file candidate; otherwise {@code
   *     false}.
   */
  public static boolean isCandidateFileName(final String fileName) {
    checkNotNull(fileName);

    return isCandidateFileName(fileName, DEFAULT_IO_CASE);
  }

  @VisibleForTesting
  static boolean isCandidateFileName(final String fileName, final IOCase ioCase) {
    return getCandidateExtensions().stream()
        .anyMatch(extension -> ioCase.checkEndsWith(fileName, extension));
  }
}
