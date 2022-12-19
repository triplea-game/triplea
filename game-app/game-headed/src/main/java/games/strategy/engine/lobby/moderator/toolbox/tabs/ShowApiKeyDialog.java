package games.strategy.engine.lobby.moderator.toolbox.tabs;

import javax.swing.JDialog;
import javax.swing.JFrame;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Parameterized dialog window intended for showing a an API key to the user. The window has a label
 * header, a read-only text field for displaying the API-key value, and a close button.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ShowApiKeyDialog {

  /**
   * Displays the swing window.
   *
   * @param parent Parent frame used for positioning.
   * @param headerLabel The label text to display at the top of the window.
   * @param newKey The API key value to show to the user.
   */
  public static void showKey(final JFrame parent, final String headerLabel, final String newKey) {
    final JDialog frame = new JDialog(parent, "API Key", true);
    frame.setLocationRelativeTo(null);
    frame
        .getContentPane()
        .add(
            new JPanelBuilder()
                .borderLayout()
                .addNorth(JLabelBuilder.builder().border(20).text(headerLabel).build())
                .addCenter(
                    JTextAreaBuilder.builder()
                        .text(newKey)
                        .selectAllTextOnFocus()
                        .readOnly()
                        .build())
                .addSouth(
                    new JButtonBuilder().title("Close").actionListener(frame::dispose).build())
                .build());
    frame.pack();
    frame.setVisible(true);
  }
}
