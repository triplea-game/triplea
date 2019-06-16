package games.strategy.engine.lobby.moderator.toolbox;

import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.java.OptionalUtils;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;

import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.settings.ClientSetting;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * Window that takes as input just the API key password from moderator. This is combined with an API
 * key stored in client settings to do API key validation with the server. If successful then
 * we show the toolbox window and the password is used to create a {@code ModeratorToolboxClient}.
 * The client will then send the API key and password on any further interactions with server.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EnterApiKeyPasswordWindow {
  private static final String CANCEL_BUTTON_TOOL_TIP = "Closes this window";

  private static final String RE_REGISTER_BUTTON_TOOL_TIP =
      "Clears out any keys stored on your system and prompts for a new API key and password.\n"
          + "Contact moderator admins for a new key if you have forgotten your password.";

  private static final String SUBMIT_BUTTON_TOOL_TIP =
      "Sends the API key stored on your system and the password\n"
          + "you enter to the TripleA servers for validation.\n"
          + "Too many failed attempts will result in lockout.";

  /**
   * Builds the enter api key password window. The window contains a field for entering
   * the moderators API-key password. The re-register button will clear the API-Key
   * stored on the local OS.
   *
   * <pre>
   * +---------------------------------------------------------+
   * | LABEL   |   PASSWORD_FIELD                              |
   * +---------------------------------------------------------+
   * |  CANCEL_BUTTON  |  RE-REGISTER BUTTON  |  SUBMIT_BUTTON |
   * +---------------------------------------------------------+
   * </pre>
   */
  static void show(final JFrame parent, final URI serverUri) {
    SwingUtilities.invokeLater(() -> showAction(parent, serverUri));
  }

  private static void showAction(final JFrame parent, final URI serverUri) {
    if (!ClientSetting.moderatorApiKey.isSet()) {
      SwingComponents.showDialog("Error", "Error API key not found on system");
      CreateNewApiKeyWindow.show(parent, serverUri);
      return;
    }

    final JButton submitButton = JButtonBuilder.builder()
        .title("Submit")
        .enabled(false)
        .biggerFont()
        .toolTip(SUBMIT_BUTTON_TOOL_TIP)
        .build();

    final JTextField apiKeyPasswordField = JTextFieldBuilder.builder()
        .columns(30)
        .actionListener(textField -> submitButton.setEnabled(!textField.getText().trim().isEmpty()))
        .build();


    final JFrame frame = JFrameBuilder.builder()
        .title("Enter API Key Password")
        .locateRelativeTo(parent)
        .size(600, 125)
        .minSize(400, 100)
        .add(
            newFrame -> JPanelBuilder.builder()
                .verticalBoxLayout()
                .addVerticalStrut(10)
                .add(
                    JPanelBuilder.builder()
                        .horizontalBoxLayout()
                        .addHorizontalStrut(5)
                        .addLabel("API Key Password: ")
                        .addHorizontalStrut(5)
                        .add(apiKeyPasswordField)
                        .addHorizontalStrut(5)
                        .build())
                .addVerticalStrut(10)
                .add(
                    JPanelBuilder.builder()
                        .horizontalBoxLayout()
                        .addHorizontalStrut(5)
                        .add(JButtonBuilder.builder()
                            .title("Cancel")
                            .actionListener(cancelButtonAction(newFrame))
                            .toolTip(CANCEL_BUTTON_TOOL_TIP)
                            .build())
                        .addHorizontalStrut(25)
                        .add(JButtonBuilder.builder()
                            .title("Re-Register")
                            .actionListener(reRegisterButtonAction(newFrame, serverUri))
                            .toolTip(RE_REGISTER_BUTTON_TOOL_TIP)
                            .build())
                        .addHorizontalStrut(25)
                        .add(submitButton)
                        .addHorizontalStrut(5)
                        .build())
                .addVerticalStrut(10)
                .build())
        .visible(true)
        .build();

    submitButton.addActionListener(submitButtonAction(
        frame, apiKeyPasswordField, serverUri));
  }

  private static Runnable cancelButtonAction(final JFrame frame) {
    return frame::dispose;
  }

  private static Runnable reRegisterButtonAction(
      final JFrame frame,
      final URI serverUri) {
    return () -> SwingComponents.promptUser(
        "Clear stored API key?",
        "Are you sure you want to clear the API key on your system and re-register?",
        () -> {
          ClientSetting.moderatorApiKey.setValueAndFlush(null);
          frame.dispose();
          CreateNewApiKeyWindow.show(frame, serverUri);
        });
  }

  private static ActionListener submitButtonAction(
      final JFrame frame,
      final JTextField passwordField,
      final URI serverUri) {
    return e -> new BackgroundTaskRunner(frame)
        .awaitRunInBackground(
            "Verifying", () -> {
              final ApiKeyPassword apiKeyPassword = ApiKeyPassword.builder()
                  .password(passwordField.getText().trim())
                  .apiKey(ClientSetting.moderatorApiKey.getValueOrThrow())
                  .build();

              OptionalUtils.ifPresentOrElse(
                  ToolboxApiKeyClient.newClient(serverUri, apiKeyPassword).validateApiKey(),
                  error -> SwingComponents.showDialog(
                      "Incorrect Password",
                      "API key validation failed.\n"
                          + "Too many failed attempts will result in lockout.\n"
                          + "Error: " + error),
                  () -> {
                    frame.dispose();
                    ToolBoxWindow.showWindow(frame, serverUri, apiKeyPassword);
                  });
            });
  }
}
