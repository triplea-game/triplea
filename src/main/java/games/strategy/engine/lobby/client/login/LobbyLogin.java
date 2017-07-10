package games.strategy.engine.lobby.client.login;

import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.MacFinder;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.MD5Crypt;

public class LobbyLogin {
  private final Window parentWindow;
  private final LobbyServerProperties lobbyServerProperties;

  public LobbyLogin(final Window parent) {
    parentWindow = parent;
    lobbyServerProperties = ClientContext.gameEnginePropertyReader().fetchLobbyServerProperties();
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
      if (ClientFileSystemHelper.areWeOldExtraJar()) {
        JOptionPane.showMessageDialog(parentWindow,
            "<html>Could not find lobby server for this version of TripleA, <br>"
                + "Please make sure you are using the latest version: "
                + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE
                + "<br /><br />This is because you are using an old engine that is kept for backwards compatibility. "
                + "<br /><b>In order to load your Old savegames in the New lobby, you must First join the lobby with "
                + "the latest engine, Then host a game, Then load the old savegame!</b></html>",
            "Could not connect to server", JOptionPane.ERROR_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(parentWindow,
            "<html>Could not find lobby server for this version of TripleA, <br>"
                + "Please make sure you are using the latest version: " + UrlConstants.LATEST_GAME_DOWNLOAD_WEBSITE
                + "</html>",
            "Could not connect to server", JOptionPane.ERROR_MESSAGE);
      }
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
              if (panel.isAnonymous()) {
                props.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
              } else {
                String salt = challengProperties.get(LobbyLoginValidator.SALT_KEY);
                if (salt == null) {
                  // the server does not have a salt value
                  // so there is no user with our name,
                  // continue as before
                  internalError.set("No account with that name exists");
                  salt = "none";
                }
                final String hashedPassword = MD5Crypt.crypt(panel.getPassword(), salt);
                props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
              }
              props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
              return props;
            }
          });
      // sucess, store prefs
      LoginPanel.storePrefs(panel.getUserName(), panel.isAnonymous());
      return new LobbyClient(messenger, panel.isAnonymous());
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
    final LoginPanel panel = new LoginPanel();
    final LoginPanel.ReturnValue value = panel.show(parentWindow);
    if (value == LoginPanel.ReturnValue.LOGON) {
      return login(panel);
    } else if (value == LoginPanel.ReturnValue.CANCEL) {
      return null;
    } else if (value == LoginPanel.ReturnValue.CREATE_ACCOUNT) {
      return createAccount();
    } else {
      throw new IllegalStateException("??");
    }
  }

  private LobbyClient createAccount() {
    final CreateUpdateAccountPanel createAccount = CreateUpdateAccountPanel.newCreatePanel();
    final CreateUpdateAccountPanel.ReturnValue value = createAccount.show(parentWindow);
    if (value == CreateUpdateAccountPanel.ReturnValue.OK) {
      return createAccount(createAccount);
    } else {
      return null;
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
              props.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt(createAccount.getPassword()));
              props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
              return props;
            }
          });
      // default
      LoginPanel.storePrefs(createAccount.getUserName(), false);
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
