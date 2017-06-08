package games.strategy.engine.framework.ui;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;

public class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;
  private static final String AUTOSAVE_FILE_NAME = GameDataFileUtils.addExtension("autosave");
  private static final String AUTOSAVE_ODD_ROUND_FILE_NAME = GameDataFileUtils.addExtension("autosave_round_odd");
  private static final String AUTOSAVE_EVEN_ROUND_FILE_NAME = GameDataFileUtils.addExtension("autosave_round_even");
  private static SaveGameFileChooser s_instance;


  public enum AUTOSAVE_TYPE {
    AUTOSAVE, AUTOSAVE2, AUTOSAVE_ODD, AUTOSAVE_EVEN
  }

  public static String getAutoSaveFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_FILE_NAME;
      }
    }
    return AUTOSAVE_FILE_NAME;
  }

  public static String getAutoSaveOddFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_ODD_ROUND_FILE_NAME;
      }
    }
    return AUTOSAVE_ODD_ROUND_FILE_NAME;
  }

  public static String getAutoSaveEvenFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_EVEN_ROUND_FILE_NAME;
      }
    }
    return AUTOSAVE_EVEN_ROUND_FILE_NAME;
  }

  public static SaveGameFileChooser getInstance() {
    if (s_instance == null) {
      s_instance = new SaveGameFileChooser();
    }
    return s_instance;
  }

  private SaveGameFileChooser() {
    super();
    setFileFilter(createGameDataFileFilter());
    ensureMapsFolderExists();
    setCurrentDirectory(ClientContext.folderSettings().getSaveGamePath());
  }

  public static void ensureMapsFolderExists() {
    ensureDirectoryExists(ClientContext.folderSettings().getSaveGamePath());
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
