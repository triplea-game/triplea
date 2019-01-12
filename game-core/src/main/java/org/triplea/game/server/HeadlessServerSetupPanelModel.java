package org.triplea.game.server;

import java.util.Optional;

import org.triplea.game.startup.ServerSetupModel;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;

/**
 * Setup panel model for headless server.
 */
public class HeadlessServerSetupPanelModel implements ServerSetupModel {

  private final GameSelectorModel gameSelectorModel;
  private HeadlessServerSetup headlessServerSetup;

  HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  @Override
  public void showSelectType() {
    new ServerModel(gameSelectorModel, this, null).createServerMessenger();
  }

  @Override
  public void onServerMessengerCreated(final ServerModel serverModel) {
    Optional.ofNullable(headlessServerSetup).ifPresent(HeadlessServerSetup::cancel);
    headlessServerSetup = new HeadlessServerSetup(serverModel, gameSelectorModel);
  }

  public HeadlessServerSetup getPanel() {
    return headlessServerSetup;
  }

  @Override
  public boolean isHeadless() {
    return true;
  }
}
