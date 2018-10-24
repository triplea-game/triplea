package games.strategy.engine.framework.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.GameDataFileUtils.addExtension;
import static games.strategy.util.StringUtils.capitalize;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.triplea.settings.ClientSetting;

/**
 * A file chooser for save games. Defaults to the user's configured save game folder.
 *
 * <p>
 * Also provides several methods for getting the names of auto-save files periodically generated during a game.
 * </p>
 */
public final class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;

  private static SaveGameFileChooser instance;

  /**
   * The available auto-saves that can be loaded by a headless game server.
   */
  public enum AUTOSAVE_TYPE {
    AUTOSAVE(getHeadlessAutoSaveFile()),

    /**
     * A second auto-save that a headless game server will alternate between (the other being {@link #AUTOSAVE}).
     *
     * @deprecated No longer supported. If an old client happens to request this auto-save, it now forwards to the
     *             same file as {@link #AUTOSAVE} instead of simply doing nothing. Remove upon next stable release (i.e.
     *             once no stable client will ever request this auto-save).
     */
    @Deprecated
    AUTOSAVE2(getHeadlessAutoSaveFile()),

    AUTOSAVE_ODD(getOddRoundAutoSaveFile(true)),

    AUTOSAVE_EVEN(getEvenRoundAutoSaveFile(true));

    private final Path path;

    AUTOSAVE_TYPE(final File file) {
      path = file.toPath();
    }

    public File getFile() {
      return path.toFile();
    }
  }

  @VisibleForTesting
  static File getAutoSaveFile(final String fileName) {
    return Paths.get(ClientSetting.saveGamesFolderPath.getValueOrThrow().getPath(), "autoSave", fileName).toFile();
  }

  private static File getAutoSaveFile(final String baseFileName, final boolean headless) {
    return getAutoSaveFile(getAutoSaveFileName(baseFileName, headless));
  }

  @VisibleForTesting
  static String getAutoSaveFileName(final String baseFileName, final boolean headless) {
    if (headless) {
      final String prefix = System.getProperty(TRIPLEA_NAME, System.getProperty(LOBBY_GAME_HOSTED_BY, ""));
      if (!prefix.isEmpty()) {
        return prefix + "_" + baseFileName;
      }
    }
    return baseFileName;
  }

  public static File getHeadlessAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave"), true);
  }

  public static File getOddRoundAutoSaveFile(final boolean headless) {
    return getAutoSaveFile(addExtension("autosave_round_odd"), headless);
  }

  public static File getEvenRoundAutoSaveFile(final boolean headless) {
    return getAutoSaveFile(addExtension("autosave_round_even"), headless);
  }

  public static File getLostConnectionAutoSaveFile(final LocalDateTime localDateTime) {
    checkNotNull(localDateTime);

    return getAutoSaveFile(
        addExtension("connection_lost_on_" + DateTimeFormatter.ofPattern("MMM_dd_'at'_HH_mm").format(localDateTime)));
  }

  public static File getBeforeStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveBefore" + capitalize(stepName)));
  }

  public static File getAfterStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveAfter" + capitalize(stepName)));
  }

  public static SaveGameFileChooser getInstance() {
    if (instance == null) {
      instance = new SaveGameFileChooser();
    }
    return instance;
  }

  private SaveGameFileChooser() {
    setFileFilter(createGameDataFileFilter());
    final File saveGamesFolder = ClientSetting.saveGamesFolderPath.getValueOrThrow();
    ensureDirectoryExists(saveGamesFolder);
    setCurrentDirectory(saveGamesFolder);
  }

  private static void ensureDirectoryExists(final File f) {
    if (!f.getParentFile().exists()) {
      ensureDirectoryExists(f.getParentFile());
    }
    if (!f.exists()) {
      f.mkdir();
    }
  }

  private static FileFilter createGameDataFileFilter() {
    return new FileFilter() {
      @Override
      public boolean accept(final File file) {
        return file.isDirectory() || GameDataFileUtils.isCandidateFileName(file.getName());
      }

      @Override
      public String getDescription() {
        return "Saved Games, *" + GameDataFileUtils.getExtension();
      }
    };
  }
}
