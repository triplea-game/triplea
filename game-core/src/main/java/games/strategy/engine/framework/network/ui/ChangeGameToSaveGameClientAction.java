package games.strategy.engine.framework.network.ui;

import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameFileSelector;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import javax.swing.AbstractAction;
import lombok.extern.java.Log;

/** An action for loading a save game across all network nodes from a client node. */
@Log
public class ChangeGameToSaveGameClientAction extends AbstractAction {
  private static final long serialVersionUID = -6986376382381381377L;
  private final IServerStartupRemote serverStartupRemote;
  private final Frame owner;

  public ChangeGameToSaveGameClientAction(
      final IServerStartupRemote serverStartupRemote, final Frame owner) {
    super("Change To Gamesave (Load Game)");
    this.serverStartupRemote = serverStartupRemote;
    this.owner = owner;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    GameFileSelector.builder()
        .fileDoesNotExistAction(file -> {}) // no-op if selected game file does not exist
        .build()
        .selectGameFile(owner)
        .ifPresent(this::changeToGameSave);
  }

  private void changeToGameSave(final File saveGame) {
    if (!saveGame.exists()) {
      return;
    }
    final byte[] bytes;
    try {
      bytes = Files.readAllBytes(saveGame.toPath());
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to read file: " + saveGame, e);
      return;
    }
    serverStartupRemote.changeToGameSave(bytes, saveGame.getName());
  }
}
