package games.strategy.triplea.ui.menubar;

import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.lobby.client.ui.action.EditGameCommentAction;
import games.strategy.engine.lobby.client.ui.action.RemoveGameFromLobbyAction;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.TripleAFrame;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;

/** The game client menu bar. */
public final class TripleAMenuBar extends JMenuBar {
  private static final long serialVersionUID = -1447295944297939539L;

  private final TripleAFrame frame;

  public TripleAMenuBar(final TripleAFrame frame) {
    this.frame = frame;

    add(new FileMenu(frame));
    add(new ViewMenu(frame));
    add(new GameMenu(frame));
    add(new ExportMenu(frame));

    final Optional<InGameLobbyWatcherWrapper> watcher = frame.getInGameLobbyWatcher();
    watcher.filter(InGameLobbyWatcherWrapper::isActive).ifPresent(this::createLobbyMenu);
    if (frame.getGame().getMessengers().isConnected()) {
      add(new NetworkMenu(watcher, frame));
    }

    add(new WebHelpMenu());
    add(new DebugMenu(frame));
    add(new HelpMenu(frame.getUiContext(), frame.getGame().getData()));
  }

  private void createLobbyMenu(final InGameLobbyWatcherWrapper watcher) {
    final JMenu lobby = new JMenu("Lobby");
    lobby.setMnemonic(KeyEvent.VK_L);
    add(lobby);
    lobby.add(new EditGameCommentAction(watcher, frame));
    lobby.add(new RemoveGameFromLobbyAction(watcher));
  }

  /**
   * Displays a file chooser dialog for the user to select the file to which the current game should
   * be saved.
   *
   * @param frame The owner of the file chooser dialog; may be {@code null}.
   * @return The file to which the current game should be saved or {@code null} if the user
   *     cancelled the operation.
   */
  public static File getSaveGameLocation(final Frame frame) {
    // For some strange reason, the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    if (SystemProperties.isMac()) {
      final FileDialog fileDialog = new FileDialog(frame);
      fileDialog.setMode(FileDialog.SAVE);
      fileDialog.setDirectory(ClientSetting.saveGamesFolderPath.getValueOrThrow().toString());
      fileDialog.setFilenameFilter((dir, name) -> GameDataFileUtils.isCandidateFileName(name));
      fileDialog.setVisible(true);

      final String fileName = fileDialog.getFile();
      if (fileName == null) {
        return null;
      }

      // If the user selects a filename that already exists,
      // the AWT Dialog on Mac OS X will ask the user for confirmation
      return new File(fileDialog.getDirectory(), GameDataFileUtils.addExtensionIfAbsent(fileName));
    }

    // Non-Mac platforms should use the normal Swing JFileChooser
    final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
    final int selectedOption = fileChooser.showSaveDialog(frame);
    if (selectedOption != JFileChooser.APPROVE_OPTION) {
      return null;
    }
    File f = fileChooser.getSelectedFile();
    // disallow sub directories to be entered (in the form directory/name) for Windows boxes
    if (SystemProperties.isWindows()) {
      final int slashIndex = Math.min(f.getPath().lastIndexOf("\\"), f.getPath().length());
      final String filePath = f.getPath().substring(0, slashIndex);
      if (!fileChooser.getCurrentDirectory().toString().equals(filePath)) {
        JOptionPane.showConfirmDialog(
            frame,
            "Sub directories are not allowed in the file name.  Please rename it.",
            "Cancel?",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }
    f = new File(f.getParent(), GameDataFileUtils.addExtensionIfAbsent(f.getName()));
    // A small warning so users will not over-write a file
    if (f.exists()) {
      final int choice =
          JOptionPane.showConfirmDialog(
              frame,
              "A file by that name already exists. Do you wish to over write it?",
              "Over-write?",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
      if (choice != JOptionPane.OK_OPTION) {
        return null;
      }
    }
    return f;
  }
}
