package games.strategy.engine.lobby.moderator.toolbox;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;

import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
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
 * This is a simple window that allows a moderator to set their API key. If the key is valid
 * we will store the new key in client settings and will show the moderator toolbox.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class EnterApiKeyWindow {

  /**
   * Builds the enter api key window. The window is a relatively simple horizontal row:
   * +-------------------------------------------------------------------+
   * | LABEL | TEXT_FIELD (for api key) | SUBMIT_BUTTON | CANCEL_BUTTON |
   * +-------------------------------------------------------------------+
   */
  static void show(final JFrame parent, final ModeratorToolboxClient toolboxClient) {
    final JButton submitButton = JButtonBuilder.builder()
        .title("Submit")
        .enabled(false)
        .biggerFont()
        .build();
    final JTextField apiKeyField = JTextFieldBuilder.builder()
        .columns(30)
        .actionListener(textField -> submitButton.setEnabled(!textField.getText().trim().isEmpty()))
        .build();

    final JButton cancelButton = JButtonBuilder.builder()
        .title("Cancel")
        .build();

    final JFrame frame = JFrameBuilder.builder()
        .title("Enter Moderator API Key")
        .locateRelativeTo(parent)
        .size(750, 100)
        .add(
            JPanelBuilder.builder()
                .add(
                    JPanelBuilder.builder()
                        .horizontalBoxLayout()
                        .addHorizontalStrut(5)
                        .addLabel("API Key: ")
                        .addHorizontalStrut(5)
                        .add(apiKeyField)
                        .addHorizontalStrut(5)
                        .add(submitButton)
                        .addHorizontalStrut(5)
                        .add(cancelButton)
                        .addHorizontalStrut(5)
                        .build())
                .build())
        .build();

    submitButton
        .addActionListener(
            e -> new BackgroundTaskRunner(frame).awaitRunInBackground("Verifying API key", () -> {
              final String result = toolboxClient.validateApiKey(apiKeyField.getText().trim());
              if (result.equalsIgnoreCase(ModeratorToolboxClient.SUCCESS)) {
                ClientSetting.moderatorApiKey.setValue(apiKeyField.getText().trim());
                frame.dispose();
                ToolBoxWindow.showWindow(parent, toolboxClient);
              } else {
                SwingComponents.showDialog(
                    "Incorrect API key",
                    "API key validation failed:\n" + result);
              }
            }));
    cancelButton.addActionListener(e -> frame.dispose());
    frame.setVisible(true);
  }
}
