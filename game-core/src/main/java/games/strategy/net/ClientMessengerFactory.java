package games.strategy.net;

import games.strategy.engine.framework.startup.login.ClientLogin;
import games.strategy.engine.framework.startup.mc.ClientModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.live.servers.ServerProperties;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginChallengeKeys;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;

/** Factory class for implementations of {@link IClientMessenger}. */
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
      final ServerProperties lobbyServerProperties, final String username) throws IOException {
    return login(lobbyServerProperties, username, null);
  }

  private static IClientMessenger login(
      final ServerProperties lobbyServerProperties, final String userName, final String password)
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
}
