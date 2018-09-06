package games.strategy.engine.framework.ui;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;

import java.io.File;

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

  @VisibleForTesting
  static final String HEADLESS_AUTOSAVE_FILE_NAME = GameDataFileUtils.addExtension("autosave");
  @VisibleForTesting
  static final String ODD_ROUND_AUTOSAVE_FILE_NAME = GameDataFileUtils.addExtension("autosave_round_odd");
  @VisibleForTesting
  static final String EVEN_ROUND_AUTOSAVE_FILE_NAME = GameDataFileUtils.addExtension("autosave_round_even");

  private static SaveGameFileChooser instance;

  /**
   * The available auto-saves that can be loaded by a headless game server.
   */
  public enum AUTOSAVE_TYPE {
    AUTOSAVE(getHeadlessAutoSaveFileName()),

    /**
     * A second auto-save that a headless game server will alternate between (the other being {@link #AUTOSAVE}).
     *
     * @deprecated No longer supported. If an old client happens to request this auto-save, it now forwards to the
     *             same file as {@link #AUTOSAVE} instead of simply doing nothing. Remove upon next stable release (i.e.
     *             once no stable client will ever request this auto-save).
     */
    @Deprecated
    AUTOSAVE2(getHeadlessAutoSaveFileName()),

    AUTOSAVE_ODD(getOddRoundAutoSaveFileName(true)),

    AUTOSAVE_EVEN(getEvenRoundAutoSaveFileName(true));

    private final String fileName;

    AUTOSAVE_TYPE(final String fileName) {
      this.fileName = fileName;
    }

    public String getFileName() {
      return fileName;
    }
  }

  public static String getHeadlessAutoSaveFileName() {
    return getAutoSaveFileName(HEADLESS_AUTOSAVE_FILE_NAME, true);
  }

  private static String getAutoSaveFileName(final String baseFileName, final boolean headless) {
    if (headless) {
      final String prefix = System.getProperty(TRIPLEA_NAME, System.getProperty(LOBBY_GAME_HOSTED_BY, ""));
      if (!prefix.isEmpty()) {
        return prefix + "_" + baseFileName;
      }
    }
    return baseFileName;
  }

  public static String getOddRoundAutoSaveFileName(final boolean headless) {
    return getAutoSaveFileName(ODD_ROUND_AUTOSAVE_FILE_NAME, headless);
  }

  public static String getEvenRoundAutoSaveFileName(final boolean headless) {
    return getAutoSaveFileName(EVEN_ROUND_AUTOSAVE_FILE_NAME, headless);
  }

  public static SaveGameFileChooser getInstance() {
    if (instance == null) {
      instance = new SaveGameFileChooser();
    }
    return instance;
  }

  private SaveGameFileChooser() {
    setFileFilter(createGameDataFileFilter());
    ensureDirectoryExists(new File(ClientSetting.SAVE_GAMES_FOLDER_PATH.value()));
    setCurrentDirectory(new File(ClientSetting.SAVE_GAMES_FOLDER_PATH.value()));
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
