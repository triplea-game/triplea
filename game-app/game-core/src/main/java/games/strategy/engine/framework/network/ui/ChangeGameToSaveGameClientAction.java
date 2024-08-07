package games.strategy.engine.framework.network.ui;

import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameFileSelector;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;

/** An action for loading a save game across all network nodes from a client node. */
@Slf4j
public class ChangeGameToSaveGameClientAction {

  public static void execute(IServerStartupRemote serverStartupRemote, Frame owner) {
    GameFileSelector.builder()
        .fileDoesNotExistAction(file -> {}) // no-op if selected game file does not exist
        .build()
        .selectGameFile(owner)
        .ifPresent(saveGame -> changeToGameSave(saveGame, serverStartupRemote));
  }

  private static void changeToGameSave(
      final Path saveGame, IServerStartupRemote serverStartupRemote) {
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
