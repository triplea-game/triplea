package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JFileChooser;
import lombok.Builder;

/**
 * Utility class containing swing logic to show a file prompt to user. Used for selecting saved
 * games.
 */
@Builder
public final class GameFileSelector {
  @Nonnull private final Consumer<File> fileDoesNotExistAction;

  /**
   * Opens up a UI pop-up allowing user to select a game file. Returns nothing if user closes the
   * pop-up.
   */
  public Optional<File> selectGameFile(final Frame owner) {
    // For some strange reason, the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (SystemProperties.isMac()) {
      final FileDialog fileDialog = new FileDialog(owner);
      fileDialog.setMode(FileDialog.LOAD);
      fileDialog.setDirectory(ClientSetting.saveGamesFolderPath.getValueOrThrow().toString());
      fileDialog.setFilenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name));
      fileDialog.setVisible(true);
      return Arrays.stream(fileDialog.getFiles()).findAny().map(this::mapFileResult);
    }

    // Non-Mac platforms should use the normal Swing JFileChooser
    final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
    final int selectedOption = fileChooser.showOpenDialog(owner);
    if (selectedOption == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile()).map(this::mapFileResult);
    }
    return Optional.empty();
  }

  @Nullable
  private File mapFileResult(final File file) {
    if (file.exists()) {
      return file;
    } else {
      fileDoesNotExistAction.accept(file);
      return null;
    }
  }
}
