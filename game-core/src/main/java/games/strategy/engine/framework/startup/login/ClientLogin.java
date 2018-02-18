package games.strategy.engine.framework.startup.login;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.ClientContext;
import games.strategy.net.IConnectionLogin;

/**
 * The client side of the peer-to-peer network game authentication protocol.
 *
 * <p>
 * In the peer-to-peer network game authentication protocol, the client receives a challenge from the server. The client
 * is responsible for obtaining the game password from the user and using it to send a response to the server's
 * challenge proving that the client knows the game password.
 * </p>
 */
public class ClientLogin implements IConnectionLogin {
  public static final String ENGINE_VERSION_PROPERTY = "Engine.Version";

  private final Component parentComponent;

  public ClientLogin(final Component parent) {
    parentComponent = parent;
  }

  @Override
  public Map<String, String> getProperties(final Map<String, String> challenge) {
    final Map<String, String> response = new HashMap<>();

    if (Boolean.TRUE.toString().equals(challenge.get(ClientLoginValidator.PASSWORD_REQUIRED_PROPERTY))) {
      addAuthenticationResponseProperties(promptForPassword(), challenge, response);
    }

    response.put(ENGINE_VERSION_PROPERTY, ClientContext.engineVersion().toString());

    return response;
  }

  @VisibleForTesting
  static void addAuthenticationResponseProperties(
      final String password,
      final Map<String, String> challenge,
      final Map<String, String> response) {
    try {
      response.putAll(Md5CryptAuthenticator.newResponse(password, challenge));
      response.putAll(HmacSha512Authenticator.newResponse(password, challenge));
    } catch (final AuthenticationException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  protected String promptForPassword() {
    final JPasswordField passwordField = new JPasswordField();
    passwordField.setColumns(15);
    JOptionPane.showMessageDialog(
        JOptionPane.getFrameForComponent(parentComponent),
        passwordField,
        "Enter a password to join the game",
        JOptionPane.QUESTION_MESSAGE);
    return new String(passwordField.getPassword());
  }
}
