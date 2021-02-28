package games.strategy.engine.framework.network.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;

/** An action for downloading a save game from the server node to a client node. */
@Slf4j
public class GetGameSaveClientAction extends AbstractAction {
  private static final long serialVersionUID = 1118264715230932068L;
  private final Component parent;
  private final IServerStartupRemote serverRemote;
  private final GameData gameDataOnStartup;

  public GetGameSaveClientAction(
      final Component parent,
      final IServerStartupRemote serverRemote,
      final GameData gameDataOnStartup) {
    super("Download Gamesave (Save Game)");
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.serverRemote = serverRemote;
    this.gameDataOnStartup = gameDataOnStartup;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final Frame frame = JOptionPane.getFrameForComponent(parent);
    final File f = SaveGameFileChooser.getSaveGameLocation(frame, gameDataOnStartup);
    if (f != null) {
      final byte[] bytes = serverRemote.getSaveGame();
      try (var fileOutputStream = new FileOutputStream(f)) {
        fileOutputStream.write(bytes);
      } catch (final IOException exception) {
        log.error("Failed to download save game from server", exception);
      }
      JOptionPane.showMessageDialog(
          frame, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
    }
  }
}
