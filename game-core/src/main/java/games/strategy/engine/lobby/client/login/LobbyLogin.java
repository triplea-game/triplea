package games.strategy.engine.lobby.client.login;

import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;

import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IMessenger;
import games.strategy.net.MacFinder;
import games.strategy.triplea.UrlConstants;

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
    if (!lobbyServerProperties.isServerAvailable()) {
      showError("Could not connect to server", lobbyServerProperties.getServerErrorMessage());
      return null;
    }
    if (lobbyServerProperties.getPort() == -1) {
      showError(
          "Could not connect to server",
          "<html>Could not find lobby server for this version of TripleA, <br>"
              + "Please make sure you are using the latest version: "
              + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE
              + "</html>");
      return null;
    }
    return loginToServer();
  }

  private @Nullable LobbyClient login(final LoginPanel panel) {
    try {
      final IMessenger messenger =
          GameRunner.newBackgroundTaskRunner()
              .runInBackgroundAndReturn(
                  "Connecting to lobby...",
                  () -> login(panel.getUserName(), panel.getPassword(), panel.isAnonymousLogin()),
                  IOException.class);
      panel.getLobbyLoginPreferences().save();
      return new LobbyClient(messenger, panel.isAnonymousLogin());
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

  private IMessenger login(
      final String userName, final String password, final boolean anonymousLogin)
      throws IOException {
    return new ClientMessenger(
        lobbyServerProperties.getHost(),
        lobbyServerProperties.getPort(),
        userName,
        MacFinder.getHashedMacAddress(),
        challenge -> {
          final Map<String, String> response = new HashMap<>();
          if (anonymousLogin) {
            response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
          } else {
            response.putAll(RsaAuthenticator.newResponse(challenge, password));
          }
          response.put(
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
          return response;
        });
  }

  private static String playerMacIdString() {
    final String mac = MacFinder.getHashedMacAddress();
    return mac.substring(mac.length() - 10);
  }

  private void showError(final String title, final String message) {
    JOptionPane.showMessageDialog(parentWindow, message, title, JOptionPane.ERROR_MESSAGE);
  }

  private @Nullable LobbyClient loginToServer() {
    final LoginPanel loginPanel = new LoginPanel(LobbyLoginPreferences.load());
    final LoginPanel.ReturnValue returnValue = loginPanel.show(parentWindow);
    switch (returnValue) {
      case LOGON:
        return login(loginPanel);
      case CANCEL:
        return null;
      case CREATE_ACCOUNT:
        return createAccount();
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
      final IMessenger messenger =
          GameRunner.newBackgroundTaskRunner()
              .runInBackgroundAndReturn(
                  "Connecting to lobby...",
                  () -> createAccount(panel.getUserName(), panel.getPassword(), panel.getEmail()),
                  IOException.class);
      panel.getLobbyLoginPreferences().save();
      return new LobbyClient(messenger, false);
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

  private IMessenger createAccount(final String userName, final String password, final String email)
      throws IOException {
    return new ClientMessenger(
        lobbyServerProperties.getHost(),
        lobbyServerProperties.getPort(),
        userName,
        MacFinder.getHashedMacAddress(),
        challenge -> {
          final Map<String, String> response = new HashMap<>();
          response.put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString());
          response.put(LobbyLoginResponseKeys.EMAIL, email);
          response.putAll(RsaAuthenticator.newResponse(challenge, password));
          response.put(
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
          return response;
        });
  }
}
