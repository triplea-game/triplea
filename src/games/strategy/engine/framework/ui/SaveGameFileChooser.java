package games.strategy.engine.framework.ui;

import java.io.File;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

public class SaveGameFileChooser {
  private static final String AUTOSAVE_FILE_NAME = "autosave.tsvg";
  private static final String AUTOSAVE_2_FILE_NAME = "autosave2.tsvg";
  private static final String AUTOSAVE_ODD_ROUND_FILE_NAME = "autosave_round_odd.tsvg";
  private static final String AUTOSAVE_EVEN_ROUND_FILE_NAME = "autosave_round_even.tsvg";
  private static SaveGameFileChooser s_instance;
  private FileChooser fileChooser;

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

  public static String getAutoSave2FileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_2_FILE_NAME;
      }
    }
    return AUTOSAVE_2_FILE_NAME;
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

  public SaveGameFileChooser() {
    super();
    fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Saved Games", "*.tsvg", "*.svg", "*.tsvg.gz"));
    ensureMapsFolderExists();
    fileChooser.setInitialDirectory(new File(ClientContext.folderSettings().getSaveGamePath()));
  }

  public static void ensureMapsFolderExists() {
    ensureDirectoryExists(new File(ClientContext.folderSettings().getSaveGamePath()));
  }

  private static void ensureDirectoryExists(final File f) {
    if (!f.getParentFile().exists()) {
      ensureDirectoryExists(f.getParentFile());
    }
    if (!f.exists()) {
      f.mkdir();
    }
  }

  public static File getSelectedFile(Window ownerWindow) {
    return getInstance().fileChooser.showSaveDialog(ownerWindow);
  }
}
