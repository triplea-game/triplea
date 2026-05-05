package games.strategy.engine.lobby.client.ui;

import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JDialogBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * A non-blocking dialog shown while the lobby connection is being re-established. It displays the
 * current reconnect attempt number and offers a "Disconnect &amp; Exit" button that aborts the
 * reconnect and shuts down the lobby frame.
 */
class ReconnectOverlay {

  private final JDialog dialog;
  private final JLabel messageLabel;

  ReconnectOverlay(final Window owner, final Runnable onDisconnect) {
    messageLabel =
        new JLabelBuilder().border(BorderFactory.createEmptyBorder(16, 24, 8, 24)).build();

    final var buttonPanel =
        new JPanelBuilder()
            .flowLayout()
            .add(new JButtonBuilder("Disconnect & Exit").actionListener(onDisconnect).build())
            .build();

    final var content =
        new JPanelBuilder().borderLayout().addCenter(messageLabel).addSouth(buttonPanel).build();

    dialog =
        new JDialogBuilder()
            .parent(owner)
            .title("Reconnecting to Lobby")
            .alwaysOnTop()
            .add(content)
            .build();
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
  }

  /** Shows (or updates) the overlay for the given 1-based reconnect attempt number. */
  void show(final int attempt) {
    SwingUtilities.invokeLater(
        () -> {
          messageLabel.setText("Lost connection to lobby. Reconnecting… (attempt " + attempt + ")");
          dialog.pack();
          if (!dialog.isVisible()) {
            dialog.setVisible(true);
          }
        });
  }

  /** Dismisses the overlay after a successful reconnect. */
  void dismiss() {
    SwingUtilities.invokeLater(dialog::dispose);
  }
}
