package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/** A file chooser for save games. Defaults to the user's configured save game folder. */
public final class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;

  private static SaveGameFileChooser instance;

  private SaveGameFileChooser() {
    setFileFilter(newGameDataFileFilter());
    final File saveGamesFolder = ClientSetting.saveGamesFolderPath.getValueOrThrow().toFile();
    ensureDirectoryExists(saveGamesFolder);
    setCurrentDirectory(saveGamesFolder);
  }

  public static SaveGameFileChooser getInstance() {
    if (instance == null) {
      instance = new SaveGameFileChooser();
    }
    return instance;
  }

  private static void ensureDirectoryExists(final File f) {
    if (!f.getParentFile().exists()) {
      ensureDirectoryExists(f.getParentFile());
    }
    if (!f.exists()) {
      f.mkdir();
    }
  }

  private static FileFilter newGameDataFileFilter() {
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
