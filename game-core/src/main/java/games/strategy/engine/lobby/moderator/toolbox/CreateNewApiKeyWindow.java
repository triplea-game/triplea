package games.strategy.engine.lobby.moderator.toolbox;

import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTextFieldBuilder;

/**
 * The purpose of this window is to give the user a field for entering their single-use moderator
 * key and to create a new password. The single-use password is issued to them and is temporary. If
 * the key is valid then the backend will salt a new key with the password and returns a new key
 * that the client side should then store in client settings. From then on the moderator only needs
 * to enter their password and their key is retrieved from client settings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CreateNewApiKeyWindow {
  private static final int API_KEY_PASSWORD_MIN_LENGTH = 4;
  private static final String WINDOW_TITLE = "Enter API Key & Create Password";

  private static final String DESCRIPTION_LABEL =
      String.format(
          "<html>Enter the API key provided to you below. Password field is a is a new password for your API key.<br/>"
              + "Choose a password that is easy to remember. Password must be at least %s characters long.",
          API_KEY_PASSWORD_MIN_LENGTH);

  private static final String API_KEY_FIELD_LABEL = "API Key Provided to You:";
  private static final String API_KEY_FIELD_TOOL_TIP =
      "Enter the API key value provided to you by admins";

  private static final String PASSWORD_FIELD_LABEL = "Create a New Password:";
  private static final String PASSWORD_FIELD_TOOLTIP =
      "This password is combined with your API key to create a secure hash.\n"
          + "Choose a password that is easy to remember";

  /**
   * Builds the enter api key window. The window contains a field for entering the single-use key
   * and a second field for submitting a new password.
   *
   * <pre>
   * +-------------------------------+
   * |      DESCRIPTION LABEL        |
   * +-------------------------------+
   * | LABEL   | KEY_FIELD           |
   * +-------------------------------+
   * | LABEL   | PASSWORD_FIELD      |
   * +-------------------------------+
   * | CANCEL_BUTTON | SUBMIT_BUTTON |
   * +-------------------------------+
   * </pre>
   */
  static void show(final JFrame parent, final URI serverUri) {
    final JButton submitButton =
        JButtonBuilder.builder().title("Submit").enabled(false).biggerFont().build();
    final JTextField apiKeyField =
        JTextFieldBuilder.builder().columns(30).toolTip(API_KEY_FIELD_TOOL_TIP).build();
    final JTextField passwordField =
        JTextFieldBuilder.builder().columns(30).toolTip(PASSWORD_FIELD_TOOLTIP).build();

    DocumentListenerBuilder.attachDocumentListener(
        apiKeyField, enableSubmitButtonAction(apiKeyField, passwordField, submitButton));
    DocumentListenerBuilder.attachDocumentListener(
        passwordField, enableSubmitButtonAction(apiKeyField, passwordField, submitButton));

    final JButton cancelButton = JButtonBuilder.builder().title("Cancel").build();

    final JFrame frame =
        JFrameBuilder.builder()
            .title(WINDOW_TITLE)
            .locateRelativeTo(parent)
            .size(750, 250)
            .minSize(600, 225)
            .add(
                JPanelBuilder.builder()
                    .borderLayout()
                    .addNorth(JLabelBuilder.builder().border(10).text(DESCRIPTION_LABEL).build())
                    .addCenter(
                        JPanelBuilder.builder()
                            .gridBagLayout(2)
                            .addLabel(API_KEY_FIELD_LABEL)
                            .add(apiKeyField)
                            .addLabel(PASSWORD_FIELD_LABEL)
                            .add(passwordField)
                            .build())
                    .addSouth(
                        JPanelBuilder.builder()
                            .flowLayout(JPanelBuilder.FlowLayoutJustification.CENTER)
                            .add(
                                JPanelBuilder.builder()
                                    .horizontalBoxLayout()
                                    .add(cancelButton)
                                    .addHorizontalStrut(50)
                                    .add(submitButton)
                                    .build())
                            .build())
                    .build())
            .build();

    submitButton.addActionListener(
        e ->
            new BackgroundTaskRunner(frame)
                .awaitRunInBackground(
                    "Registering API Key",
                    () -> {
                      if (CreateNewApiKeyActions.registerApiKey(
                          serverUri,
                          ApiKeyPassword.builder()
                              .apiKey(apiKeyField.getText().trim())
                              .password(passwordField.getText().trim())
                              .build())) {
                        frame.dispose();
                        EnterApiKeyPasswordWindow.show(frame, serverUri);
                      }
                    }));
    cancelButton.addActionListener(e -> frame.dispose());
    frame.setVisible(true);
  }

  /**
   * Creates a text field listener that will enable the submit button when the password and api key
   * fields have a minimum amount of text filled in.
   */
  private static Runnable enableSubmitButtonAction(
      final JTextComponent apiKeyField,
      final JTextComponent passwordField,
      final JButton submitButton) {

    return () ->
        submitButton.setEnabled(
            // we're assuming here that the min api key length is at least the API key password min.
            apiKeyField.getText().trim().length() >= API_KEY_PASSWORD_MIN_LENGTH
                && passwordField.getText().trim().length() >= API_KEY_PASSWORD_MIN_LENGTH);
  }
}
