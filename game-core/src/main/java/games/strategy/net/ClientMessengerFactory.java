package games.strategy.net;

import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginChallengeKeys;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;

/** Default implementation of {@link IClientMessenger}. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientMessengerFactory {

  /** Creates a client messenger suitable for connecting to a hosted game server. */
  public static IClientMessenger newClientMessenger(
      final ClientModel.ClientProps props,
      final IObjectStreamFactory objectStreamFactory,
      final ClientLogin clientLogin)
      throws IOException {
    final String mac = MacFinder.getHashedMacAddress();
    return new ClientMessenger(
        props.getHost(), props.getPort(), props.getName(), mac, objectStreamFactory, clientLogin);
  }

  /**
   * Creates a client messenger that will connect to lobby, creating a new account.
   *
   * @param lobbyServerProperties Properties used to connect to the lobby.
   * @param username Desired new username to connect with.
   * @param email Email associated with the new user account.
   * @param password Plain-text (new) password read from UI.
   * @throws IOException Thrown if there are any errors establishing a socket connection to lobby.
   */
  public static IClientMessenger newCreateAccountMessenger(
      final LobbyServerProperties lobbyServerProperties,
      final String username,
      final String email,
      final String password)
      throws IOException {
    return new ClientMessenger(
        lobbyServerProperties.getHost(),
        lobbyServerProperties.getPort(),
        username,
        MacFinder.getHashedMacAddress(),
        challenge -> {
          final Map<String, String> response = new HashMap<>();
          response.put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString());
          response.put(LobbyLoginResponseKeys.EMAIL, email);
          response.put(
              LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD, encryptPassword(challenge, password));
          response.put(
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
          return response;
        });
  }

  private static String encryptPassword(
      final Map<String, String> challenge, final String password) {
    final String rsaKeyFromServer = challenge.get(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY);
    return RsaAuthenticator.encrpytPassword(rsaKeyFromServer, password);
  }

  /**
   * Creates a messenger that will connect to lobby and do a name-only login. This is called
   * anonymous login, a registered account is not required.
   */
  public static IClientMessenger newAnonymousUserMessenger(
      final LobbyServerProperties lobbyServerProperties, final String username) throws IOException {
    return login(lobbyServerProperties, username, null);
  }

  /**
   * Creates a client messenger that will connect to a lobby using username and password (registered
   * users).
   */
  public static IClientMessenger newRegisteredUserMessenger(
      final LobbyServerProperties lobbyServerProperties,
      final String username,
      final String password)
      throws IOException {
    return login(lobbyServerProperties, username, password);
  }

  private static IClientMessenger login(
      final LobbyServerProperties lobbyServerProperties,
      final String userName,
      final String password)
      throws IOException {

    return new ClientMessenger(
        lobbyServerProperties.getHost(),
        lobbyServerProperties.getPort(),
        userName,
        MacFinder.getHashedMacAddress(),
        challenge -> {
          final Map<String, String> response = new HashMap<>();
          if (password == null) {
            response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
          } else {
            response.put(
                LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD,
                encryptPassword(challenge, password));
          }
          response.put(
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
          return response;
        });
  }

  /** Creates a messenger suitable for lobby watchers (bots). */
  public static IClientMessenger newLobbyWatcherMessenger(
      final String host, final int port, final String hostedByName) throws IOException {

    final IConnectionLogin login =
        challenge -> {
          final Map<String, String> response = new HashMap<>();
          response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
          response.put(
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
          response.put(LobbyLoginResponseKeys.LOBBY_WATCHER_LOGIN, Boolean.TRUE.toString());
          return response;
        };

    return new ClientMessenger(
        host,
        port,
        IServerMessenger.getRealName(hostedByName) + "_" + LobbyConstants.LOBBY_WATCHER_NAME,
        MacFinder.getHashedMacAddress(),
        login);
  }

  /**
   * Creates a messenger that will connect to lobby and request a temporary password to be sent to
   * the users email.
   */
  public static IClientMessenger newForgotPasswordMessenger(
      final LobbyServerProperties lobbyServerProperties, final String userName) throws IOException {

    return new ClientMessenger(
        lobbyServerProperties.getHost(),
        lobbyServerProperties.getPort(),
        userName,
        MacFinder.getHashedMacAddress(),
        challenge -> {
          final Map<String, String> response = new HashMap<>();
          response.put(LobbyLoginResponseKeys.FORGOT_PASSWORD, Boolean.TRUE.toString());
          response.put(
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
          return response;
        });
  }
}
