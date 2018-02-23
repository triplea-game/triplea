package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Observable;

import javax.annotation.Nonnull;
import javax.swing.JPanel;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.LocalSetupPanel;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.PbemSetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;

public class SetupPanelModel extends Observable {
  protected final GameSelectorModel gameSelectorModel;
  protected ISetupPanel panel = null;

  public SetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  public GameSelectorModel getGameSelectorModel() {
    return gameSelectorModel;
  }

  public void setWidgetActivation() {
    if (panel != null) {
      panel.setWidgetActivation();
    }
  }

  public void showSelectType() {
    setGameTypePanel(new MetaSetupPanel(this));
  }

  public void showLocal() {
    setGameTypePanel(new LocalSetupPanel(gameSelectorModel));
  }

  public void showPbem() {
    setGameTypePanel(new PbemSetupPanel(gameSelectorModel));
  }

  public void showServer(final Component ui) {
    final ServerModel model = new ServerModel(gameSelectorModel, this);
    if (!model.createServerMessenger(ui)) {
      model.cancel();
      return;
    }
    setGameTypePanel(new ServerSetupPanel(model, gameSelectorModel));
    // for whatever reason, the server window is showing very very small, causing the nation info to be cut and
    // requiring scroll bars
    final int x = ((ui.getPreferredSize().width > 800) ? ui.getPreferredSize().width : 800);
    final int y = ((ui.getPreferredSize().height > 660) ? ui.getPreferredSize().height : 660);
    ui.setPreferredSize(new Dimension(x, y));
    ui.setSize(new Dimension(x, y));
  }

  public void showClient(final Component ui) {
    final ClientModel model = new ClientModel(gameSelectorModel, this);
    if (!model.createClientMessenger(ui)) {
      model.cancel();
      return;
    }
    setGameTypePanel(new ClientSetupPanel(model));
  }

  protected void setGameTypePanel(final ISetupPanel panel) {
    if (this.panel != null) {
      this.panel.cancel();
    }
    this.panel = panel;
    super.setChanged();
    super.notifyObservers(this.panel);
    super.clearChanged();
  }

  public ISetupPanel getPanel() {
    return panel;
  }

  public void setServerMode(
      @Nonnull final ServerModel serverModel,
      @Nonnull final ServerConnectionProps props) {
    serverModel.createServerMessenger(new JPanel(), props);
    Preconditions.checkNotNull(serverModel.getChatPanel());
    serverModel.createServerLauncher();
  }
}
