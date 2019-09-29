package games.strategy.engine.lobby.client.login;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.net.ClientMessengerFactory;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IClientMessenger;
import games.strategy.net.MacFinder;
import java.awt.Window;
import java.io.IOException;
import javax.annotation.Nullable;
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
  public @Nullable LobbyClient login() {
    if (lobbyServerProperties.getServerErrorMessage().isPresent()) {
      showError("Could not connect to server", lobbyServerProperties.getServerErrorMessage().get());
      return null;
    }
    return loginToServer();
  }

  private @Nullable LobbyClient login(final LoginPanel panel) {
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
      return new LobbyClient(
          messenger,
          HttpLobbyClient.newClient(
              lobbyServerProperties.getHttpsServerUri(), messenger.getApiKey()),
          panel.isAnonymousLogin());
    } catch (final CouldNotLogInException e) {
      showError("Login Failed", e.getMessage() + "\n" + playerMacIdString());
      return loginToServer(); // NB: potential stack overflow due to recursive call
    } catch (final IOException e) {
      showError("Could Not Connect", "Could not connect to lobby: " + e.getMessage());
      return null;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private static String playerMacIdString() {
    final String mac = MacFinder.getHashedMacAddress();
    return mac.substring(mac.length() - 10);
  }

  private void showError(final String title, final String message) {
    JOptionPane.showMessageDialog(parentWindow, message, title, JOptionPane.ERROR_MESSAGE);
  }

  private @Nullable LobbyClient loginToServer() {
    final LoginPanel loginPanel = new LoginPanel();
    final LoginPanel.ReturnValue returnValue = loginPanel.show(parentWindow);
    switch (returnValue) {
      case LOGON:
        return login(loginPanel);
      case CANCEL:
        return null;
      case CREATE_ACCOUNT:
        return createAccount();
      case FORGOT_PASSWORD:
        forgotPassword();
        return null;
      default:
        throw new AssertionError("unknown login panel return value: " + returnValue);
    }
  }

  private @Nullable LobbyClient createAccount() {
    final CreateUpdateAccountPanel createAccountPanel = CreateUpdateAccountPanel.newCreatePanel();
    final CreateUpdateAccountPanel.ReturnValue returnValue = createAccountPanel.show(parentWindow);
    switch (returnValue) {
      case OK:
        return createAccount(createAccountPanel);
      case CANCEL:
        return null;
      default:
        throw new AssertionError("unknown create account panel return value: " + returnValue);
    }
  }

  private @Nullable LobbyClient createAccount(final CreateUpdateAccountPanel panel) {
    try {
      final IClientMessenger messenger =
          GameRunner.newBackgroundTaskRunner()
              .runInBackgroundAndReturn(
                  "Connecting to lobby...",
                  () ->
                      ClientMessengerFactory.newCreateAccountMessenger(
                          lobbyServerProperties,
                          panel.getUserName(),
                          panel.getEmail(),
                          panel.getPassword()),
                  IOException.class);
      return new LobbyClient(
          messenger,
          HttpLobbyClient.newClient(
              lobbyServerProperties.getHttpsServerUri(), messenger.getApiKey()));
    } catch (final CouldNotLogInException e) {
      showError("Account Creation Failed", e.getMessage());
      return createAccount(); // NB: potential stack overflow due to recursive call
    } catch (final IOException e) {
      showError("Could Not Connect", "Could not connect to lobby: " + e.getMessage());
      return null;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
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
