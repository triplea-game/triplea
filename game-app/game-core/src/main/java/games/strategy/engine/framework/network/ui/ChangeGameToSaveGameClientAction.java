package games.strategy.engine.framework.network.ui;

import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameFileSelector;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;

/** An action for loading a save game across all network nodes from a client node. */
@Slf4j
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

  private void changeToGameSave(final Path saveGame) {
    if (!Files.exists(saveGame)) {
      return;
    }
    final byte[] bytes;
    try {
      bytes = Files.readAllBytes(saveGame);
    } catch (final IOException e) {
      log.error("Failed to read file: " + saveGame, e);
      return;
    }
    ThreadRunner.runInNewThread(
        () -> serverStartupRemote.changeToGameSave(bytes, saveGame.getFileName().toString()));
  }
}
