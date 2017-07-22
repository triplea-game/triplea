package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOCase;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.ClientContext;

/**
 * A collection of utilities for working with game data files.
 */
public final class GameDataFileUtils {
  private GameDataFileUtils() {}

  /**
   * Appends the game data file extension to the specified file name.
   *
   * @param fileName The file name.
   *
   * @return The file name with the game data file extension appended.
   */
  public static String addExtension(final String fileName) {
    checkNotNull(fileName);

    return fileName + getExtension();
  }

  /**
   * Appends the game data file extension to the specified file name if the file name does not end with the game data
   * file extension.
   *
   * @param fileName The file name.
   *
   * @return The file name ending with at most one occurrence of the game data file extension.
   */
  public static String addExtensionIfAbsent(final String fileName) {
    checkNotNull(fileName);

    return addExtensionIfAbsent(fileName, IOCase.SYSTEM);
  }

  @VisibleForTesting
  static String addExtensionIfAbsent(final String fileName, final IOCase ioCase) {
    return ioCase.checkEndsWith(fileName, getExtension()) ? fileName : addExtension(fileName);
  }

  private static Collection<String> getCandidateExtensions() {
    return ClientContext.gameEnginePropertyReader().useNewSaveGameFormat()
        ? getNewCandidateExtensions()
        : getCurrentCandidateExtensions();
  }

  private static Collection<String> getNewCandidateExtensions() {
    return Arrays.asList(getExtension());
  }

  private static Collection<String> getCurrentCandidateExtensions() {
    final String legacyExtension = ".svg";

    // Macs download a game data file as "tsvg.gz", so that extension must be used when evaluating candidate game data
    // files.
    final String macOsAlternativeExtension = "tsvg.gz";

    return Arrays.asList(getExtension(), legacyExtension, macOsAlternativeExtension);
  }

  /**
   * Gets the game data file extension.
   *
   * @return The game data file extension including the leading period.
   */
  public static String getExtension() {
    return ClientContext.gameEnginePropertyReader().useNewSaveGameFormat()
        ? getNewExtension()
        : getCurrentExtension();
  }

  private static String getNewExtension() {
    return ".tsvgx";
  }

  private static String getCurrentExtension() {
    return ".tsvg";
  }

  /**
   * Indicates the specified file name is a game data file candidate.
   *
   * @param fileName The file name.
   *
   * @return {@code true} if the specified file name is a game data file candidate; otherwise {@code false}.
   */
  public static boolean isCandidateFileName(final String fileName) {
    checkNotNull(fileName);

    return isCandidateFileName(fileName, IOCase.SYSTEM);
  }

  @VisibleForTesting
  static boolean isCandidateFileName(final String fileName, final IOCase ioCase) {
    return getCandidateExtensions().stream().anyMatch(extension -> ioCase.checkEndsWith(fileName, extension));
  }
}
