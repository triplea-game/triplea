package org.triplea.game.server;

import java.io.File;
import java.util.Optional;
import java.util.logging.Level;

import org.triplea.game.startup.ServerSetupModel;

import games.strategy.engine.framework.HeadlessAutoSaveFileUtils;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import lombok.extern.java.Log;

/**
 * Setup panel model for headless server.
 */
@Log
public class HeadlessServerSetupPanelModel implements ServerSetupModel {

  private final GameSelectorModel gameSelectorModel;
  private HeadlessServerSetup headlessServerSetup;

  HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  @Override
  public void showSelectType() {
    new ServerModel(gameSelectorModel, this, null, new LaunchAction() {
      @Override
      public void handleGameInterruption(final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {
        try {
          log.info("Game ended, going back to waiting.");
          // if we do not do this, we can get into an infinite loop of launching a game,
          // then crashing out, then launching, etc.
          serverModel.setAllPlayersToNullNodes();
          final File f1 = getAutoSaveFileUtils().getHeadlessAutoSaveFile();
          if (!f1.exists() || !gameSelectorModel.load(f1)) {
            gameSelectorModel.resetGameDataToNull();
          }
        } catch (final Exception e1) {
          log.log(Level.SEVERE, "Failed to load game", e1);
          gameSelectorModel.resetGameDataToNull();
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
      public boolean isHeadless() {
        return true;
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
    }).createServerMessenger();
  }

  @Override
  public void onServerMessengerCreated(final ServerModel serverModel) {
    Optional.ofNullable(headlessServerSetup).ifPresent(HeadlessServerSetup::cancel);
    headlessServerSetup = new HeadlessServerSetup(serverModel, gameSelectorModel);
  }

  public HeadlessServerSetup getPanel() {
    return headlessServerSetup;
  }
}
