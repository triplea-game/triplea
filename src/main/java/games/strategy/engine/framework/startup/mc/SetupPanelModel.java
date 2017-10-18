package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Observable;

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
  private ISetupPanel setupPanel = null;

  public SetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  public GameSelectorModel getGameSelectorModel() {
    return gameSelectorModel;
  }

  public void setWidgetActivation() {
    if (setupPanel != null) {
      setupPanel.setWidgetActivation();
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

  /**
   * Similar to 'showServer', get all the side effects but no UI.
   * TODO: parameter is an in-parameter, would be nice if we did not need the side effect, initialization
   * ordering is why we have it..
   */
  void setServerMode(final ServerModel model, final ServerConnectionProps props) {
    model.createServerMessenger(new JPanel(), props);
    Preconditions.checkNotNull(model.getChatPanel());
    model.createServerLauncher();
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
    final int x = (ui.getPreferredSize().width > 800 ? ui.getPreferredSize().width : 800);
    final int y = (ui.getPreferredSize().height > 660 ? ui.getPreferredSize().height : 660);
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
    if (setupPanel != null) {
      setupPanel.cancel();
    }
    setupPanel = panel;
    super.setChanged();
    super.notifyObservers(setupPanel);
    super.clearChanged();
  }

  public ISetupPanel getPanel() {
    return setupPanel;
  }
}
