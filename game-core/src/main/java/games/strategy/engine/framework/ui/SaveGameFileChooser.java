package games.strategy.engine.framework.ui;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameDataFileUtils;
import java.io.File;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/** A file chooser for save games. Defaults to the user's configured save game folder. */
public final class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;

  private static SaveGameFileChooser instance;

  private static final Path saveGameFolder =
      ClientFileSystemHelper.getUserRootFolder().toPath().resolve("savedGames");

  private SaveGameFileChooser() {
    setFileFilter(newGameDataFileFilter());
    setCurrentDirectory(getSaveGameFolder());
  }

  public static File getSaveGameFolder() {
    final File folder = saveGameFolder.toFile();
    if (!folder.mkdirs() && !folder.exists()) {
      throw new IllegalStateException(
          "Unable to create save game folder: " + folder.getAbsolutePath());
    }
    return folder;
  }

  public static SaveGameFileChooser getInstance() {
    if (instance == null) {
      instance = new SaveGameFileChooser();
    }
    return instance;
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
