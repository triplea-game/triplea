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
import games.strategy.engine.framework.startup.ui.MultiplayerMenuPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.posted.game.pbem.PbemSetupPanel;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.PbfSetupPanel;
import games.strategy.engine.framework.ui.MainFrame;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LoginMode;
import games.strategy.engine.lobby.client.login.LoginResult;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.client.ui.LobbyModel;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.triplea.game.client.HeadedGameRunner;
import org.triplea.swing.SwingComponents;

/** This class provides a way to switch between different ISetupPanel displays. */
@RequiredArgsConstructor
public class HeadedServerSetupModel {
  @Getter protected final GameSelectorModel gameSelectorModel;
  @Getter protected SetupPanel panel = null;

  /** Shows the screen the current one was reached from; absent on the welcome screen. */
  @Nullable private Runnable backAction;

  @Setter private Consumer<SetupPanel> panelChangeListener;
  @Getter @Setter private JFrame ui;

  public void showSelectType() {
    setGameTypePanel(new MetaSetupPanel(this), null);
  }

  public void showMultiplayer() {
    setGameTypePanel(new MultiplayerMenuPanel(this), this::showSelectType);
  }

  public void showLocal() {
    setGameTypePanel(new LocalSetupPanel(gameSelectorModel), this::showSelectType);
  }

  public void showPbf() {
    setGameTypePanel(new PbfSetupPanel(gameSelectorModel), this::showMultiplayer);
  }

  public void showPbem() {
    setGameTypePanel(new PbemSetupPanel(gameSelectorModel), this::showMultiplayer);
  }

  public Optional<Runnable> getBackAction() {
    return Optional.ofNullable(backAction);
  }

  /**
   * Starts the game server and then displays the game start screen, awaiting remote game clients.
   */
  public ServerModel showServer() {
    final ServerModel serverModel = new ServerModel(gameSelectorModel, new HeadedLaunchAction(ui));
    if (serverModel.initialize()) {
      onServerMessengerCreated(serverModel);
    }
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
        () ->
            setGameTypePanel(
                new ServerSetupPanel(serverModel, gameSelectorModel), this::showMultiplayer));
  }

  /**
   * A method that establishes a connection to a remote game and displays the game start screen
   * after the connection was successfully established.
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
      SwingUtilities.invokeLater(
          () -> setGameTypePanel(new ClientSetupPanel(model), this::showMultiplayer));
    } else {
      model.cancel();
    }
  }

  private void setGameTypePanel(final SetupPanel panel, @Nullable final Runnable backAction) {
    if (this.panel != null) {
      this.panel.cancel();
    }
    this.panel = panel;
    this.backAction = backAction;

    Optional.ofNullable(panelChangeListener).ifPresent(listener -> listener.accept(panel));
  }

  /**
   * Executes a login sequence prompting the user for their lobby username+password and sends it to
   * server. If successful the user is presented with the lobby frame. Failure cases are handled and
   * user is presented with another try, or they can abort. In the abort case this method is a
   * no-op.
   */
  public void login() {
    new LobbyLogin(ui)
        .promptLogin(LoginMode.REGISTRATION_NOT_REQUIRED)
        .ifPresent(this::showLobbyWindow);
  }

  private void showLobbyWindow(final LoginResult loginResult) {
    final var lobbyModel =
        new LobbyModel(
            loginResult,
            error -> SwingComponents.showError(null, "Error communicating with lobby", error));
    final LobbyFrame lobbyFrame = new LobbyFrame(lobbyModel);
    MainFrame.hide();
    lobbyFrame.setVisible(true);
  }
}
