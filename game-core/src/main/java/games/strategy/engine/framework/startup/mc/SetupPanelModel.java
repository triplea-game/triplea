package games.strategy.engine.framework.startup.mc;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;

import games.strategy.engine.config.client.LobbyServerPropertiesFetcher;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.LocalSetupPanel;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.PbemSetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.ScreenChangeListener;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class provides a way to switch between different ISetupPanel displays.
 */
@RequiredArgsConstructor
public class SetupPanelModel {
  @Getter
  protected final GameSelectorModel gameSelectorModel;
  protected ISetupPanel panel = null;

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

  /**
   * Starts the game server and displays the game start screen afterwards, awaiting remote game clients.
   */
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
   * A method that establishes a connection to a remote game and displays the game start screen afterwards if the
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

  /**
   * Executes a login sequence prompting the user for their lobby username+password and sends it to
   * server. If successful the user is presented with the lobby frame. Failure cases are handled and
   * user is presented with another try or they can abort. In the abort case this method is a no-op.
   *
   * @param uiParent Used to center pop-up's prompting user for their lobby credentials.
   */
  public void login(final Component uiParent) {
    final LobbyServerProperties lobbyServerProperties =
        new LobbyServerPropertiesFetcher().fetchLobbyServerProperties();
    final LobbyLogin login =
        new LobbyLogin(JOptionPane.getFrameForComponent(uiParent), lobbyServerProperties);

    Optional.ofNullable(login.login())
        .ifPresent(
            lobbyClient -> {
              final LobbyFrame lobbyFrame = new LobbyFrame(lobbyClient, lobbyServerProperties);
              GameRunner.hideMainFrame();
              lobbyFrame.setVisible(true);
            });
  }
}
