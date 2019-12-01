package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.Optional;
import javax.swing.JFileChooser;

/**
 * Utility class containing swing logic to show a file prompt to user. Used for selecting saved
 * games.
 */
public final class GameFileSelector {
  private GameFileSelector() {}

  /**
   * Opens up a UI pop-up allowing user to select a game file. Returns nothing if user closes the
   * pop-up.
   */
  public static Optional<File> selectGameFile(final Frame owner) {
    // For some strange reason, the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (SystemProperties.isMac()) {
      final FileDialog fileDialog = new FileDialog(owner);
      fileDialog.setMode(FileDialog.LOAD);
      fileDialog.setDirectory(ClientSetting.saveGamesFolderPath.getValueOrThrow().toString());
      fileDialog.setFilenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name));
      fileDialog.setVisible(true);
      final String fileName = fileDialog.getFile();
      final String dirName = fileDialog.getDirectory();
      return Optional.ofNullable(fileName).map(name -> new File(dirName, fileName));
    }

    // Non-Mac platforms should use the normal Swing JFileChooser
    final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
    final int selectedOption = fileChooser.showOpenDialog(owner);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile());
    }
    return Optional.empty();
  }
}
