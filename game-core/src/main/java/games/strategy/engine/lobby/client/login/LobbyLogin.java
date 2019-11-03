package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.net.ClientMessengerFactory;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IClientMessenger;
import games.strategy.net.MacFinder;
import java.awt.Window;
import java.io.IOException;
import java.util.Optional;
import javax.swing.JOptionPane;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.swing.DialogBuilder;

/**
 * The client side of the lobby authentication protocol.
 *
 * <p>The client is responsible for sending the initial authentication request to the server
 * containing the user's name. The server will send back an authentication challenge. The client
 * then sends a response to the challenge to prove the user knows the correct password.
 */
public class LobbyLogin {
  private final Window parentWindow;
  private final LobbyServerProperties lobbyServerProperties;

  public LobbyLogin(final Window parent, final LobbyServerProperties lobbyServerProperties) {
    parentWindow = parent;
    this.lobbyServerProperties = lobbyServerProperties;
  }

  /**
   * Attempt to login to the LobbyServer.
   *
   * <p>If we could not login, return null.
   */
  public Optional<LobbyClient> login() {
    if (lobbyServerProperties.getServerErrorMessage().isPresent()) {
      showError("Could not connect to server", lobbyServerProperties.getServerErrorMessage().get());
      return Optional.empty();
    }
    return loginToServer();
  }

  private Optional<LobbyClient> login(final LoginPanel panel) {
    try {
      final IClientMessenger messenger =
          GameRunner.newBackgroundTaskRunner()
              .runInBackgroundAndReturn(
                  "Connecting to lobby...",
                  () ->
                      panel.isAnonymousLogin()
                          ? ClientMessengerFactory.newAnonymousUserMessenger(
                              lobbyServerProperties, panel.getUserName())
                          : ClientMessengerFactory.newRegisteredUserMessenger(
                              lobbyServerProperties, panel.getUserName(), panel.getPassword()),
                  IOException.class);
      return Optional.of(
          new LobbyClient(
              messenger,
              HttpLobbyClient.newClient(
                  lobbyServerProperties.getHttpsServerUri(), messenger.getApiKey()),
              panel.isAnonymousLogin()));
    } catch (final CouldNotLogInException e) {
      showError("Login Failed", e.getMessage() + "\n" + playerMacIdString());
      return loginToServer(); // NB: potential stack overflow due to recursive call
    } catch (final IOException e) {
      showError("Could Not Connect", "Could not connect to lobby: " + e.getMessage());
      return Optional.empty();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private static String playerMacIdString() {
    final String mac = MacFinder.getHashedMacAddress();
    return mac.substring(mac.length() - 10);
  }

  private void showError(final String title, final String message) {
    JOptionPane.showMessageDialog(parentWindow, message, title, JOptionPane.ERROR_MESSAGE);
  }

  private Optional<LobbyClient> loginToServer() {
    final LoginPanel loginPanel = new LoginPanel();
    final LoginPanel.ReturnValue returnValue = loginPanel.show(parentWindow);
    switch (returnValue) {
      case LOGON:
        return login(loginPanel);
      case CANCEL:
        return Optional.empty();
      case CREATE_ACCOUNT:
        return createAccount();
      case FORGOT_PASSWORD:
        forgotPassword();
        return Optional.empty();
      default:
        throw new AssertionError("unknown login panel return value: " + returnValue);
    }
  }

  private Optional<LobbyClient> createAccount() {
    final CreateAccountPanel createAccountPanel = new CreateAccountPanel();
    final CreateAccountPanel.ReturnValue returnValue = createAccountPanel.show(parentWindow);
    switch (returnValue) {
      case OK:
        return createAccount(createAccountPanel);
      case CANCEL:
        return Optional.empty();
      default:
        throw new AssertionError("unknown create account panel return value: " + returnValue);
    }
  }

  private Optional<LobbyClient> createAccount(final CreateAccountPanel panel) {
    try {
      final IClientMessenger messenger =
          GameRunner.newBackgroundTaskRunner()
              .runInBackgroundAndReturn(
                  "Connecting to lobby...",
                  () ->
                      ClientMessengerFactory.newCreateAccountMessenger(
                          lobbyServerProperties,
                          panel.getUsername(),
                          panel.getEmail(),
                          panel.getPassword()),
                  IOException.class);
      return Optional.of(
          new LobbyClient(
              messenger,
              HttpLobbyClient.newClient(
                  lobbyServerProperties.getHttpsServerUri(), messenger.getApiKey())));
    } catch (final CouldNotLogInException e) {
      showError("Account Creation Failed", e.getMessage());
      return createAccount(); // NB: potential stack overflow due to recursive call
    } catch (final IOException e) {
      showError("Could Not Connect", "Could not connect to lobby: " + e.getMessage());
      return Optional.empty();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private void forgotPassword() {
    final ForgotPasswordPanel forgotPasswordPanel = ForgotPasswordPanel.newForgotPasswordPanel();
    final ForgotPasswordPanel.ReturnValue returnValue = forgotPasswordPanel.show(parentWindow);
    switch (returnValue) {
      case OK:
        forgotPassword(forgotPasswordPanel);
        return;
      case CANCEL:
        return;
      default:
        throw new AssertionError("unknown forgot password panel return value: " + returnValue);
    }
  }

  private void forgotPassword(final ForgotPasswordPanel panel) {
    try {
      final String response =
          GameRunner.newBackgroundTaskRunner()
              .runInBackgroundAndReturn(
                  "Sending forgot password request...",
                  () ->
                      ForgotPasswordClient.newClient(lobbyServerProperties.getHttpsServerUri())
                          .sendForgotPasswordRequest(
                              ForgotPasswordRequest.builder()
                                  .username(panel.getUserName())
                                  .email(panel.getEmail())
                                  .build()),
                  IOException.class)
              .getResponseMessage();
      DialogBuilder.builder()
          .parent(parentWindow)
          .title("Server Response")
          .infoMessage(response)
          .showDialog();
    } catch (final IOException e) {
      showError("Error", "Failed to generate a temporary password: " + e.getMessage());
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
