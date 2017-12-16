package games.strategy.engine.lobby.client.login;

import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.login.RsaAuthenticator;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.IMessenger;
import games.strategy.net.MacFinder;
import games.strategy.triplea.UrlConstants;

public class LobbyLogin {
  private final Window parentWindow;
  private final LobbyServerProperties lobbyServerProperties;

  public LobbyLogin(final Window parent, final LobbyServerProperties lobbyServerProperties) {
    parentWindow = parent;
    this.lobbyServerProperties = lobbyServerProperties;
  }

  /**
   * Attempt to login to the LobbyServer
   *
   * <p>
   * If we could not login, return null.
   * </p>
   */
  public @Nullable LobbyClient login() {
    if (!lobbyServerProperties.isServerAvailable()) {
      showError("Could not connect to server", lobbyServerProperties.serverErrorMessage);
      return null;
    }
    if (lobbyServerProperties.port == -1) {
      showError("Could not connect to server",
          "<html>Could not find lobby server for this version of TripleA, <br>"
              + "Please make sure you are using the latest version: " + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE
              + "</html>");
      return null;
    }
    return loginToServer();
  }

  private @Nullable LobbyClient login(final LoginPanel panel) {
    try {
      final IMessenger messenger = GameRunner.newBackgroundTaskRunner().runInBackgroundAndReturn(
          "Connecting to lobby...",
          () -> login(panel.getUserName(), panel.getPassword(), panel.isAnonymousLogin()),
          IOException.class);
      panel.getLobbyLoginPreferences().save();
      return new LobbyClient(messenger, panel.isAnonymousLogin());
    } catch (final CouldNotLogInException e) {
      showError("Login Failed", e.getMessage());
      return loginToServer(); // NB: potential stack overflow due to recursive call
    } catch (final IOException e) {
      showError("Could Not Connect", "Could not connect to lobby: " + e.getMessage());
      return null;
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private IMessenger login(final String userName, final String password, final boolean anonymousLogin)
      throws IOException {
    return new ClientMessenger(
        lobbyServerProperties.host,
        lobbyServerProperties.port,
        userName,
        MacFinder.getHashedMacAddress(),
        new IConnectionLogin() {
          @Override
          public Map<String, String> getProperties(final Map<String, String> challenge) {
            final Map<String, String> response = new HashMap<>();
            if (anonymousLogin) {
              response.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
            } else {
              final String salt =
                  challenge.getOrDefault(LobbyLoginValidator.SALT_KEY, games.strategy.util.MD5Crypt.newSalt());
              response.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, games.strategy.util.MD5Crypt.crypt(password, salt));
              if (RsaAuthenticator.canProcessChallenge(challenge)) {
                response.putAll(RsaAuthenticator.newResponse(challenge, password));
              }
            }
            response.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
            return response;
          }
        });
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
      final IMessenger messenger = GameRunner.newBackgroundTaskRunner().runInBackgroundAndReturn(
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
        lobbyServerProperties.host,
        lobbyServerProperties.port,
        userName,
        MacFinder.getHashedMacAddress(),
        new IConnectionLogin() {
          @Override
          public Map<String, String> getProperties(final Map<String, String> challenge) {
            final Map<String, String> response = new HashMap<>();
            response.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
            response.put(LobbyLoginValidator.EMAIL_KEY, email);
            // TODO: Don't send the md5-hashed password once the lobby removes the support, kept for
            // backwards-compatibility
            response.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, games.strategy.util.MD5Crypt.crypt(password));
            if (RsaAuthenticator.canProcessChallenge(challenge)) {
              response.putAll(RsaAuthenticator.newResponse(challenge, password));
            }
            response.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
            return response;
          }
        });
  }
}
