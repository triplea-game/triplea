package org.triplea.game.server;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.HeadlessAutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.player.Player;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.triplea.sound.HeadlessSoundChannel;
import org.triplea.sound.ISound;

@Slf4j
public class HeadlessLaunchAction implements LaunchAction {
  @Override
  public void handleGameInterruption(
      final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {
    log.info("Game ended, going back to waiting.");
    // if we do not do this, we can get into an infinite loop of launching a game,
    // then crashing out, then launching, etc.
    serverModel.setAllPlayersToNullNodes();
    final File autoSaveFile = getAutoSaveFileUtils().getHeadlessAutoSaveFile();
    if (autoSaveFile.exists()) {
      gameSelectorModel.load(autoSaveFile);
    }
  }

  @Override
  public void onGameInterrupt() {
    // tell headless server to wait for new connections:
    HeadlessGameServer.waitForUsersHeadlessInstance();
  }

  @Override
  public void onEnd(final String message) {
    log.info(message);
  }

  @Override
  public IDisplay startGame(
      final LocalPlayers localPlayers,
      final IGame game,
      final Set<Player> players,
      final Chat chat) {
    UiContext.setResourceLoader(game.getData());
    return new HeadlessDisplay();
  }

  @Override
  public ISound getSoundChannel(final LocalPlayers localPlayers) {
    return new HeadlessSoundChannel();
  }

  @Override
  public File getAutoSaveFile() {
    return getAutoSaveFileUtils().getHeadlessAutoSaveFile();
  }

  @Override
  public void onLaunch(final ServerGame serverGame) {
    HeadlessGameServer.setServerGame(serverGame);
  }

  @Override
  public HeadlessAutoSaveFileUtils getAutoSaveFileUtils() {
    return new HeadlessAutoSaveFileUtils();
  }
}
