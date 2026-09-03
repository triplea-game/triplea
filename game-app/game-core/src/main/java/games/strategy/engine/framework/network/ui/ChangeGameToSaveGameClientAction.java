package games.strategy.engine.framework.network.ui;

import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameFileSelector;
import games.strategy.net.Messengers;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;

/** An action for loading a save game across all network nodes from a client node. */
@Slf4j
public class ChangeGameToSaveGameClientAction {

  public static void execute(Messengers messengers, Frame owner) {
    GameFileSelector.builder()
        .fileDoesNotExistAction(file -> {}) // no-op if selected game file does not exist
        .build()
        .selectGameFile(owner)
        .ifPresent(saveGame -> changeToGameSave(saveGame, messengers));
  }

  private static void changeToGameSave(final Path saveGame, Messengers messengers) {
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
        () ->
            messengers.invokeRemoteMessage(
                ServerModel.SERVER_REMOTE_NAME,
                new IServerStartupRemote.ChangeToGameSaveRequest(
                    bytes, saveGame.getFileName().toString()),
                IServerStartupRemote.ChangeToGameSaveResponse.TYPE));
  }
}
