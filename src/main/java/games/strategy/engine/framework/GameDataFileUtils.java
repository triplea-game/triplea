package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOCase;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.triplea.settings.ClientSetting;

/**
 * A collection of utilities for working with game data files.
 */
public final class GameDataFileUtils {
  private static final IOCase DEFAULT_IO_CASE = IOCase.SYSTEM;

  private GameDataFileUtils() {}

  private static SaveGameFormat getDefaultSaveGameFormat() {
    return ClientSetting.TEST_USE_PROXY_SERIALIZATION.booleanValue()
        ? SaveGameFormat.PROXY_SERIALIZATION
        : SaveGameFormat.SERIALIZATION;
  }

  @VisibleForTesting
  enum SaveGameFormat {
    SERIALIZATION, PROXY_SERIALIZATION;
  }

  /**
   * Appends the game data file extension to the specified file name.
   *
   * @param fileName The file name.
   *
   * @return The file name with the game data file extension appended.
   */
  public static String addExtension(final String fileName) {
    checkNotNull(fileName);

    return addExtension(fileName, getDefaultSaveGameFormat());
  }

  @VisibleForTesting
  static String addExtension(final String fileName, final SaveGameFormat saveGameFormat) {
    return fileName + getExtension(saveGameFormat);
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

    return addExtensionIfAbsent(fileName, getDefaultSaveGameFormat(), DEFAULT_IO_CASE);
  }

  @VisibleForTesting
  static String addExtensionIfAbsent(final String fileName, final SaveGameFormat saveGameFormat, final IOCase ioCase) {
    return ioCase.checkEndsWith(fileName, getExtension(saveGameFormat))
        ? fileName
        : addExtension(fileName, saveGameFormat);
  }

  private static Collection<String> getCandidateExtensions(final SaveGameFormat saveGameFormat) {
    switch (saveGameFormat) {
      case SERIALIZATION:
        return getSerializationCandidateExtensions();
      case PROXY_SERIALIZATION:
        return getProxySerializationCandidateExtensions();
      default:
        throw new AssertionError(String.format("unknown save game format (%s)", saveGameFormat));
    }
  }

  private static Collection<String> getSerializationCandidateExtensions() {
    final String legacyExtension = ".svg";

    // Macs download a game data file as "tsvg.gz", so that extension must be used when evaluating candidate game data
    // files.
    final String macOsAlternativeExtension = "tsvg.gz";

    return Arrays.asList(getExtension(SaveGameFormat.SERIALIZATION), legacyExtension, macOsAlternativeExtension);
  }

  private static Collection<String> getProxySerializationCandidateExtensions() {
    return Arrays.asList(getExtension(SaveGameFormat.PROXY_SERIALIZATION));
  }

  /**
   * Gets the game data file extension.
   *
   * @return The game data file extension including the leading period.
   */
  public static String getExtension() {
    return getExtension(getDefaultSaveGameFormat());
  }

  private static String getExtension(final SaveGameFormat saveGameFormat) {
    switch (saveGameFormat) {
      case SERIALIZATION:
        return getSerializationExtension();
      case PROXY_SERIALIZATION:
        return getProxySerializationExtension();
      default:
        throw new AssertionError(String.format("unknown save game format (%s)", saveGameFormat));
    }
  }

  private static String getSerializationExtension() {
    return ".tsvg";
  }

  private static String getProxySerializationExtension() {
    return ".tsvgx";
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

    return isCandidateFileName(fileName, getDefaultSaveGameFormat(), DEFAULT_IO_CASE);
  }

  @VisibleForTesting
  static boolean isCandidateFileName(final String fileName, final SaveGameFormat saveGameFormat, final IOCase ioCase) {
    return getCandidateExtensions(saveGameFormat).stream()
        .anyMatch(extension -> ioCase.checkEndsWith(fileName, extension));
  }
}
