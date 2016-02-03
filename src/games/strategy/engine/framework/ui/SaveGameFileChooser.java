package games.strategy.engine.framework.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;

public class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;
  private static final String AUTOSAVE_FILE_NAME = "autosave.tsvg";
  private static final String AUTOSAVE_2_FILE_NAME = "autosave2.tsvg";
  private static final String AUTOSAVE_ODD_ROUND_FILE_NAME = "autosave_round_odd.tsvg";
  private static final String AUTOSAVE_EVEN_ROUND_FILE_NAME = "autosave_round_even.tsvg";
  public static final File DEFAULT_DIRECTORY = new File(ClientFileSystemHelper.getUserRootFolder(), "savedGames");
  private static SaveGameFileChooser s_instance;

  public enum AUTOSAVE_TYPE {
    AUTOSAVE, AUTOSAVE2, AUTOSAVE_ODD, AUTOSAVE_EVEN
  }

  public static String getAutoSaveFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_FILE_NAME;
      }
    }
    return AUTOSAVE_FILE_NAME;
  }

  public static String getAutoSave2FileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_2_FILE_NAME;
      }
    }
    return AUTOSAVE_2_FILE_NAME;
  }

  public static String getAutoSaveOddFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY, ""));
      if (saveSuffix.length() > 0) {
        return saveSuffix + "_" + AUTOSAVE_ODD_ROUND_FILE_NAME;
      }
    }
    return AUTOSAVE_ODD_ROUND_FILE_NAME;
  }

  public static String getAutoSaveEvenFileName() {
    if (HeadlessGameServer.headless()) {
      final String saveSuffix = System.getProperty(GameRunner2.TRIPLEA_NAME_PROPERTY,
          System.getProperty(GameRunner2.LOBBY_GAME_HOSTED_BY, ""));
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
    setFileFilter(m_gameDataFileFilter);
    ensureDefaultDirExists();
    setCurrentDirectory(DEFAULT_DIRECTORY);
  }

  public static void ensureDefaultDirExists() {
    ensureDirectoryExists(DEFAULT_DIRECTORY);
  }

  private static void ensureDirectoryExists(final File f) {
    if (!f.getParentFile().exists()) {
      ensureDirectoryExists(f.getParentFile());
    }
    if (!f.exists()) {
      f.mkdir();
    }
  }

  FileFilter m_gameDataFileFilter = new FileFilter() {
    @Override
    public boolean accept(final File f) {
      if (f.isDirectory()) {
        return true;
      }
      // the extension should be .tsvg, but find svg extensions as well
      // also, macs download the file as tsvg.gz, so accept that as well
      return f.getName().endsWith(".tsvg") || f.getName().endsWith(".svg") || f.getName().endsWith("tsvg.gz");
    }

    @Override
    public String getDescription() {
      return "Saved Games, *.tsvg";
    }
  };
}
