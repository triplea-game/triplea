package games.strategy.engine.framework.headlessGameServer;

import java.awt.Component;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;

/**
 * Setup panel model for headless server.
 */
public class HeadlessServerSetupPanelModel extends SetupPanelModel {
  protected final Component uiComponent;

  HeadlessServerSetupPanelModel(final GameSelectorModel gameSelectorModel, final Component uiComponent) {
    super(gameSelectorModel);
    this.uiComponent = uiComponent;
  }

  @Override
  public void showSelectType() {
    final ServerModel model = new ServerModel(gameSelectorModel, this, ServerModel.InteractionMode.HEADLESS);
    if (!model.createServerMessenger(uiComponent)) {
      model.cancel();
      return;
    }
    setGameTypePanel((uiComponent == null) ? new HeadlessServerSetup(model, gameSelectorModel)
        : new ServerSetupPanel(model, gameSelectorModel));
  }
}
