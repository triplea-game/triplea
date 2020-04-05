package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.StringJoiner;
import javax.swing.JFileChooser;

/** A file chooser for save games. Defaults to the user's configured save game folder. */
public final class SaveGameFileChooser extends JFileChooser {
  private static final long serialVersionUID = 1548668790891292106L;

  /**
   * Displays a file chooser dialog for the user to select the file to which the current game should
   * be saved.
   *
   * @param frame The owner of the file chooser dialog; may be {@code null}.
   * @return The file to which the current game should be saved or {@code null} if the user
   *     cancelled the operation.
   */
  public static File getSaveGameLocation(final Frame frame, final GameData gameData) {
    final FileDialog fileDialog = new FileDialog(frame);
    fileDialog.setMode(FileDialog.SAVE);
    fileDialog.setDirectory(ClientSetting.saveGamesFolderPath.getValueOrThrow().toString());
    fileDialog.setFilenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name));
    fileDialog.setFile(getSaveGameName(gameData));

    fileDialog.setVisible(true);
    final String fileName = fileDialog.getFile();
    if (fileName == null) {
      return null;
    }

    // If the user selects a filename that already exists,
    // the AWT Dialog will ask the user for confirmation
    return new File(fileDialog.getDirectory(), GameDataFileUtils.addExtensionIfAbsent(fileName));
  }

  private static String getSaveGameName(final GameData gameData) {
    return gameData.getSaveGameFileName().orElse(formatGameName(gameData.getGameName()));
  }

  private static String formatGameName(final String gameName) {
    final ZonedDateTime now = ZonedDateTime.now();
    return new StringJoiner("-")
        .add(String.valueOf(now.getYear()))
        .add(String.valueOf(now.getMonthValue()))
        .add(String.valueOf(now.getDayOfMonth()))
        .add(gameName.replaceAll(" ", "-") + GameDataFileUtils.getExtension())
        .toString();
  }
}
