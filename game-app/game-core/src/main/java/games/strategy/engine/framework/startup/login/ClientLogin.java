package games.strategy.engine.framework.startup.login;

import games.strategy.net.IConnectionLogin;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NonNls;

/**
 * The client side of the peer-to-peer network game authentication protocol.
 *
 * <p>In the peer-to-peer network game authentication protocol, the client receives a challenge from
 * the server. The client is responsible for obtaining the game password from the user and using it
 * to send a response to the server's challenge proving that the client knows the game password.
 */
@AllArgsConstructor
public class ClientLogin implements IConnectionLogin {
  @NonNls static final String ENGINE_VERSION_PROPERTY = "Engine.Version";

  private final ClientLoginUi ui;

  @Override
  public Map<String, String> getProperties(final Map<String, String> challenge) {
    final Map<String, String> response = new HashMap<>();

    if (Boolean.TRUE
        .toString()
        .equals(challenge.get(ClientLoginValidator.PASSWORD_REQUIRED_PROPERTY))) {
      addAuthenticationResponseProperties(ui.promptForPassword(), challenge, response);
    }

    return response;
  }

  private static void addAuthenticationResponseProperties(
      final String password,
      final Map<String, String> challenge,
      final Map<String, String> response) {
    try {
      response.putAll(HmacSha512Authenticator.newResponse(password, challenge));
    } catch (final AuthenticationException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void onAssignedDifferentName(final String assignedName) {
    ui.showAlreadyLoggedIn(assignedName);
  }
}
