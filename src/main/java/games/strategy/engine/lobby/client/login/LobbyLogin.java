package games.strategy.engine.lobby.client.login;

import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.engine.lobby.server.login.RsaAuthenticator;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.MacFinder;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.MD5Crypt;

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
  public LobbyClient login() {
    if (!lobbyServerProperties.isServerAvailable()) {
      JOptionPane.showMessageDialog(
          parentWindow,
          lobbyServerProperties.serverErrorMessage,
          "Could not connect to server",
          JOptionPane.ERROR_MESSAGE);
      return null;
    }
    if (lobbyServerProperties.port == -1) {
      JOptionPane.showMessageDialog(parentWindow,
          "<html>Could not find lobby server for this version of TripleA, <br>"
              + "Please make sure you are using the latest version: " + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE
              + "</html>",
          "Could not connect to server", JOptionPane.ERROR_MESSAGE);
      return null;
    }
    return loginToServer();
  }

  private LobbyClient login(final LoginPanel panel) {
    try {
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger(lobbyServerProperties.host, lobbyServerProperties.port,
          panel.getUserName(), mac, new IConnectionLogin() {
            private final AtomicReference<String> internalError = new AtomicReference<>();

            @Override
            public void notifyFailedLogin(String message) {
              if (internalError.get() != null) {
                message = internalError.get();
              }
              JOptionPane.showMessageDialog(parentWindow, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            public Map<String, String> getProperties(final Map<String, String> challengProperties) {
              final Map<String, String> props = new HashMap<>();
              if (panel.isAnonymousLogin()) {
                props.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
              } else {
                final boolean isUpdatedLobby = RsaAuthenticator.canProcessChallenge(challengProperties);
                String salt = challengProperties.get(LobbyLoginValidator.SALT_KEY);
                if (salt == null) {
                  if (!isUpdatedLobby) {
                    // the server does not have a salt value
                    // so there is no user with our name,
                    // continue as before
                    internalError.set("No account with that name exists");
                  }
                  salt = "none";
                }
                final String hashedPassword = MD5Crypt.crypt(panel.getPassword(), salt);
                props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
                if (isUpdatedLobby) {
                  props.putAll(RsaAuthenticator.getEncryptedPassword(challengProperties, panel.getPassword()));
                }
              }
              props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
              return props;
            }
          });

      // lobby login was successful if we reach this point
      panel.getLobbyLoginPreferences().save();
      return new LobbyClient(messenger, panel.isAnonymousLogin());
    } catch (final CouldNotLogInException e) {
      // this has already been dealt with
      return loginToServer();
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(
          parentWindow,
          "Could Not Connect to Lobby : " + e.getMessage(),
          "Could not connect",
          JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }

  private LobbyClient loginToServer() {
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

  private LobbyClient createAccount() {
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

  private LobbyClient createAccount(final CreateUpdateAccountPanel createAccount) {
    try {
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger(lobbyServerProperties.host, lobbyServerProperties.port,
          createAccount.getUserName(), mac, new IConnectionLogin() {
            @Override
            public void notifyFailedLogin(final String message) {
              JOptionPane.showMessageDialog(parentWindow, message, "Login Failed", JOptionPane.ERROR_MESSAGE);
            }

            @Override
            public Map<String, String> getProperties(final Map<String, String> challengProperties) {
              final Map<String, String> props = new HashMap<>();
              props.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
              props.put(LobbyLoginValidator.EMAIL_KEY, createAccount.getEmail());
              // TODO: Don't send the md5-hashed password once the lobby removes the support, kept for
              // backwards-compatibility
              props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt(createAccount.getPassword()));
              if (RsaAuthenticator.canProcessChallenge(challengProperties)) {
                props.putAll(RsaAuthenticator.getEncryptedPassword(challengProperties, createAccount.getPassword()));
              }
              props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
              return props;
            }
          });

      // lobby login was successful if we reach this point
      createAccount.getLobbyLoginPreferences().save();
      return new LobbyClient(messenger, false);
    } catch (final CouldNotLogInException clne) {
      // this has already been dealt with
      return createAccount();
    } catch (final IOException e) {
      JOptionPane.showMessageDialog(
          parentWindow,
          e.getMessage(),
          "Account creation failed",
          JOptionPane.ERROR_MESSAGE);
      return null;
    }
  }
}
