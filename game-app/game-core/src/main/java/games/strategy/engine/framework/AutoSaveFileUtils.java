package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.GameDataFileUtils.addExtension;
import static org.triplea.java.StringUtils.capitalize;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides methods for getting the names of auto-save files periodically generated during a game.
 */
@Slf4j
public class AutoSaveFileUtils {
  /** Returns a list path objects representing each auto save file. */
  public static List<Path> getAutoSavePaths() {
    var autoSaveFolder = ClientSetting.saveGamesFolderPath.getValueOrThrow().resolve("autoSave");

    if (!Files.exists(autoSaveFolder)) {
      try {
        Files.createDirectories(autoSaveFolder);
      } catch (IOException e) {
        throw new RuntimeException(
            "Autosave folder did not exist, was not able to create it. Required folder: "
                + autoSaveFolder.toAbsolutePath(),
            e);
      }
    }

    try (Stream<Path> paths = Files.list(autoSaveFolder)) {
      return paths.filter(f -> !Files.isDirectory(f)).collect(Collectors.toList());
    } catch (IOException e) {
      log.warn("Unable to list auto-save game files", e);
      return List.of();
    }
  }

  /** Returns the name of all auto save files. */
  public static List<String> getAutoSaveFiles() {
    return getAutoSavePaths().stream()
        .map(Path::toFile)
        .map(File::getName)
        .sorted()
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  Path getAutoSaveFile(final String baseFileName) {
    return ClientSetting.saveGamesFolderPath
        .getValueOrThrow()
        .resolve("autoSave")
        .resolve(getAutoSaveFileName(baseFileName));
  }

  @VisibleForTesting
  String getAutoSaveFileName(final String baseFileName) {
    return baseFileName;
  }

  public Path getOddRoundAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave_round_odd"));
  }

  public Path getEvenRoundAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave_round_even"));
  }

  public Path getLostConnectionAutoSaveFile(final LocalDateTime localDateTime) {
    checkNotNull(localDateTime);

    return getAutoSaveFile(
        addExtension(
            "connection_lost_on_"
                + DateTimeFormatter.ofPattern("MMM_dd_'at'_HH_mm", Locale.ENGLISH)
                    .format(localDateTime)));
  }

  public Path getBeforeStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveBefore" + capitalize(stepName)));
  }

  public Path getAfterStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveAfter" + capitalize(stepName)));
  }

  public String getAutoSaveStepName(final String stepName) {
    return stepName;
  }
}
