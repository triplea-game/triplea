package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Observable;

import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.LocalSetupPanel;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.PBEMSetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;

public class SetupPanelModel extends Observable {
  protected final GameSelectorModel m_gameSelectorModel;
  protected ISetupPanel m_panel = null;

  public SetupPanelModel(final GameSelectorModel gameSelectorModel) {
    m_gameSelectorModel = gameSelectorModel;
  }

  public GameSelectorModel getGameSelectorModel() {
    return m_gameSelectorModel;
  }

  public void setWidgetActivation() {
    if (m_panel != null) {
      m_panel.setWidgetActivation();
    }
  }

  public void showSelectType() {
    setGameTypePanel(new MetaSetupPanel(this));
  }

  public void showLocal() {
    setGameTypePanel(new LocalSetupPanel(m_gameSelectorModel));
  }

  public void showPBEM() {
    setGameTypePanel(new PBEMSetupPanel(m_gameSelectorModel));
  }

  public void showServer(final Component ui) {
    final ServerModel model = new ServerModel(m_gameSelectorModel, this);
    if (!model.createServerMessenger(ui)) {
      model.cancel();
      return;
    }
    setGameTypePanel(new ServerSetupPanel(model, m_gameSelectorModel));
    // for whatever reason, the server window is showing very very small, causing the nation info to be cut and
    // requiring scroll bars
    final int x = (ui.getPreferredSize().width > 800 ? ui.getPreferredSize().width : 800);
    final int y = (ui.getPreferredSize().height > 660 ? ui.getPreferredSize().height : 660);
    ui.setPreferredSize(new Dimension(x, y));
    ui.setSize(new Dimension(x, y));
  }

  public void showClient(final Component ui) {
    final ClientModel model = new ClientModel(m_gameSelectorModel, this);
    if (!model.createClientMessenger(ui)) {
      model.cancel();
      return;
    }
    setGameTypePanel(new ClientSetupPanel(model));
  }

  protected void setGameTypePanel(final ISetupPanel panel) {
    if (m_panel != null) {
      m_panel.cancel();
    }
    m_panel = panel;
    super.setChanged();
    super.notifyObservers(m_panel);
    super.clearChanged();
  }

  public ISetupPanel getPanel() {
    return m_panel;
  }
}
