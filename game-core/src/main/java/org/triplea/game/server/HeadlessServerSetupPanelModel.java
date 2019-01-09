package org.triplea.game.server;

import java.util.Optional;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;

/**
 * Setup panel model for headless server.
 */
public class HeadlessServerSetupPanelModel {

  private final GameSelectorModel gameSelectorModel;
  private HeadlessServerSetup headlessServerSetup;

  HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  public void showSelectType() {
    final ServerModel model = new ServerModel(gameSelectorModel, this);
    if (!model.createServerMessenger(null)) {
      model.cancel();
      return;
    }

    Optional.ofNullable(headlessServerSetup).ifPresent(HeadlessServerSetup::cancel);
    headlessServerSetup = new HeadlessServerSetup(model, gameSelectorModel);
  }

  public HeadlessServerSetup getPanel() {
    return headlessServerSetup;
  }
}
