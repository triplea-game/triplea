package org.triplea.game.server;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;

/**
 * Setup panel model for headless server.
 */
class HeadlessServerSetupPanelModel extends SetupPanelModel {
  HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel) {
    super(gameSelectorModel);
  }

  @Override
  public void showSelectType() {
    final ServerModel model = new ServerModel(gameSelectorModel, this, ServerModel.InteractionMode.HEADLESS);
    if (!model.createServerMessenger(null)) {
      model.cancel();
      return;
    }
    setGameTypePanel(new HeadlessServerSetup(model, gameSelectorModel));
  }

  @Override
  protected void setGameTypePanel(final ISetupPanel panel) {
    if (panel == null || panel instanceof HeadlessServerSetup) {
      super.setGameTypePanel(panel);
    } else {
      throw new IllegalArgumentException("Invalid panel of type " + panel.getClass());
    }
  }

  @Override
  public HeadlessServerSetup getPanel() {
    return (HeadlessServerSetup) super.getPanel();
  }
}
