package games.strategy.engine.framework.startup.ui.panels.main;

import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.HeadedLaunchAction;
import games.strategy.engine.framework.startup.mc.HeadedPlayerTypes;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.LocalSetupPanel;
import games.strategy.engine.framework.startup.ui.MetaSetupPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.posted.game.pbem.PbemSetupPanel;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.PbfSetupPanel;
import games.strategy.engine.framework.ui.MainFrame;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LoginMode;
import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.Dimension;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.triplea.game.client.HeadedGameRunner;

/** This class provides a way to switch between different ISetupPanel displays. */
@RequiredArgsConstructor
public class HeadedServerSetupModel {
  @Getter protected final GameSelectorModel gameSelectorModel;
  @Getter protected SetupPanel panel = null;

  @Setter private Consumer<SetupPanel> panelChangeListener;
  @Setter private JFrame ui;

  public void showSelectType() {
    setGameTypePanel(new MetaSetupPanel(this));
  }

  public void showLocal() {
    setGameTypePanel(new LocalSetupPanel(gameSelectorModel));
  }

  public void showPbf() {
    setGameTypePanel(new PbfSetupPanel(gameSelectorModel));
  }

  public void showPbem() {
    setGameTypePanel(new PbemSetupPanel(gameSelectorModel));
  }

  /**
   * Starts the game server and displays the game start screen afterwards, awaiting remote game
   * clients.
   */
  public ServerModel showServer() {
    final ServerModel serverModel = new ServerModel(gameSelectorModel, new HeadedLaunchAction(ui));
    serverModel.initialize();
    onServerMessengerCreated(serverModel);
    return serverModel;
  }

  private void onServerMessengerCreated(final ServerModel serverModel) {

    serverModel
        .getMessenger()
        .setLoginValidator(
            ClientLoginValidator.builder()
                .password(System.getProperty(SERVER_PASSWORD))
                .serverMessenger(serverModel.getMessenger())
                .build());

    SwingUtilities.invokeLater(
        () -> {
          setGameTypePanel(new ServerSetupPanel(serverModel, gameSelectorModel));

          // for whatever reason, the server window is showing very very small, causing the nation
          // info to be cut and requiring scroll bars
          final int x = Math.max(ui.getPreferredSize().width, 800);
          final int y = Math.max(ui.getPreferredSize().height, 660);
          ui.setPreferredSize(new Dimension(x, y));
          ui.setSize(new Dimension(x, y));
        });
  }

  /**
   * A method that establishes a connection to a remote game and displays the game start screen
   * afterwards if the connection was successfully established.
   */
  public void showClient() {
    Preconditions.checkState(!SwingUtilities.isEventDispatchThread());
    final ClientModel model =
        new ClientModel(
            gameSelectorModel,
            this::showSelectType,
            new HeadedLaunchAction(ui),
            HeadedGameRunner::showMainFrame,
            HeadedGameRunner::clientLeftGame,
            HeadedPlayerTypes.CLIENT_PLAYER);
    if (model.createClientMessenger(ui)) {
      SwingUtilities.invokeLater(() -> setGameTypePanel(new ClientSetupPanel(model)));
    } else {
      model.cancel();
    }
  }

  private void setGameTypePanel(final SetupPanel panel) {
    if (this.panel != null) {
      this.panel.cancel();
    }
    this.panel = panel;

    Optional.ofNullable(panelChangeListener).ifPresent(listener -> listener.accept(panel));
  }

  /**
   * Executes a login sequence prompting the user for their lobby username+password and sends it to
   * server. If successful the user is presented with the lobby frame. Failure cases are handled and
   * user is presented with another try or they can abort. In the abort case this method is a no-op.
   */
  public void login() {
    promptLobbyLogin(ClientSetting.lobbyUri.getValueOrThrow());
  }

  private void promptLobbyLogin(final URI lobbyUri) {
    new LobbyLogin(ui, lobbyUri)
        .promptLogin(LoginMode.REGISTRATION_NOT_REQUIRED)
        .ifPresent(loginResult -> showLobbyWindow(loginResult, lobbyUri));
  }

  private void showLobbyWindow(final LoginResult loginResult, final URI lobbyUri) {
    final var lobbyClient = LobbyClient.newLobbyClient(lobbyUri, loginResult);

    final LobbyFrame lobbyFrame = new LobbyFrame(lobbyClient, lobbyUri);
    MainFrame.hide();
    lobbyFrame.setVisible(true);
  }
}
