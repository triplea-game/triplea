package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.LocalSetupPanel;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.PbemSetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.ScreenChangeListener;
import lombok.Setter;

/**
 * This class provides a way to switch between different ISetupPanel displays.
 * TODO: rename this to MainPanelController
 */
public class SetupPanelModel {
  protected final GameSelectorModel gameSelectorModel;
  protected ISetupPanel panel = null;

  public SetupPanelModel(final GameSelectorModel gameSelectorModel) {
    this.gameSelectorModel = gameSelectorModel;
  }

  public GameSelectorModel getGameSelectorModel() {
    return gameSelectorModel;
  }

  @Setter
  private ScreenChangeListener panelChangeListener;


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
    SwingUtilities.invokeLater(() -> {
      setGameTypePanel(new ServerSetupPanel(model, gameSelectorModel));
      // for whatever reason, the server window is showing very very small, causing the nation info to be cut and
      // requiring scroll bars
      final int x = (ui.getPreferredSize().width > 800 ? ui.getPreferredSize().width : 800);
      final int y = (ui.getPreferredSize().height > 660 ? ui.getPreferredSize().height : 660);
      ui.setPreferredSize(new Dimension(x, y));
      ui.setSize(new Dimension(x, y));
    });
  }

  /**
   * A method that establishes a connection to a remote game
   * and displays the game start screen afterwards if the
   * connection was successfully established.
   */
  public void showClient(final Component ui) {
    Preconditions.checkState(!SwingUtilities.isEventDispatchThread());
    final ClientModel model = new ClientModel(gameSelectorModel, this);
    if (model.createClientMessenger(ui)) {
      SwingUtilities.invokeLater(() -> setGameTypePanel(new ClientSetupPanel(model)));
    } else {
      model.cancel();
    }
  }

  protected void setGameTypePanel(final ISetupPanel panel) {
    if (this.panel != null) {
      this.panel.cancel();
    }
    this.panel = panel;

    Optional.ofNullable(panelChangeListener)
        .ifPresent(listener -> listener.screenChangeEvent(panel));
  }

  public ISetupPanel getPanel() {
    return panel;
  }
}
