package games.strategy.engine.framework.ui;

import static games.strategy.engine.framework.ArgParser.CliProperties.LOBBY_GAME_HOSTED_BY;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_NAME;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.triplea.settings.ClientSetting;

public class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;
  private static final String AUTOSAVE_FILE_NAME = GameDataFileUtils.addExtension("autosave");
  private static final String AUTOSAVE_ODD_ROUND_FILE_NAME = GameDataFileUtils.addExtension("autosave_round_odd");
  private static final String AUTOSAVE_EVEN_ROUND_FILE_NAME = GameDataFileUtils.addExtension("autosave_round_even");
  private static SaveGameFileChooser instance;

  public enum AUTOSAVE_TYPE {
    AUTOSAVE(getAutoSaveFileName()),

    AUTOSAVE2(""),

    AUTOSAVE_ODD(getAutoSaveOddFileName()),

    AUTOSAVE_EVEN(getAutoSaveEvenFileName());

    private final String fileName;

    AUTOSAVE_TYPE(final String fileName) {
      this.fileName = fileName;
    }

    public String getFileName() {
      return fileName;
    }
  }

  public static String getAutoSaveFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(TRIPLEA_NAME,
          System.getProperty(LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_FILE_NAME;
      }
    }
    return AUTOSAVE_FILE_NAME;
  }

  public static String getAutoSaveOddFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(TRIPLEA_NAME,
          System.getProperty(LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_ODD_ROUND_FILE_NAME;
      }
    }
    return AUTOSAVE_ODD_ROUND_FILE_NAME;
  }

  public static String getAutoSaveEvenFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(TRIPLEA_NAME,
          System.getProperty(LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_EVEN_ROUND_FILE_NAME;
      }
    }
    return AUTOSAVE_EVEN_ROUND_FILE_NAME;
  }

  public static SaveGameFileChooser getInstance() {
    if (instance == null) {
      instance = new SaveGameFileChooser();
    }
    return instance;
  }

  private SaveGameFileChooser() {
    super();
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
