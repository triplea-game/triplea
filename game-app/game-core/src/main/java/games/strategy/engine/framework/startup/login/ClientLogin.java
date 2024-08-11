package games.strategy.engine.framework.startup.login;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.net.IConnectionLogin;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import lombok.AllArgsConstructor;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;
import org.triplea.util.Version;

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

  private final Component parentComponent;
  private final Version engineVersion;

  @Override
  public Map<String, String> getProperties(final Map<String, String> challenge) {
    final Map<String, String> response = new HashMap<>();

    if (Boolean.TRUE
        .toString()
        .equals(challenge.get(ClientLoginValidator.PASSWORD_REQUIRED_PROPERTY))) {
      addAuthenticationResponseProperties(promptForPassword(), challenge, response);
    }

    response.put(ENGINE_VERSION_PROPERTY, engineVersion.toString());

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

  @VisibleForTesting
  protected String promptForPassword() {
    return Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> {
                      final JPasswordField passwordField = new JPasswordField();
                      passwordField.setColumns(15);
                      final JOptionPane optionPane =
                          new JOptionPane(
                              passwordField,
                              JOptionPane.QUESTION_MESSAGE,
                              JOptionPane.DEFAULT_OPTION) {
                            private static final long serialVersionUID = -6461902648509091914L;

                            @Override
                            public void selectInitialValue() {
                              super.selectInitialValue();
                              passwordField.requestFocusInWindow();
                            }
                          };
                      optionPane
                          .createDialog(parentComponent, "Enter a password to join the game")
                          .setVisible(true);
                      return new String(passwordField.getPassword());
                    }))
        .result
        .orElse("");
  }
}
