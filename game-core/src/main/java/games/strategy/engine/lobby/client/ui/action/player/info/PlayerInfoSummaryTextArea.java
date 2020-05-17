package games.strategy.engine.lobby.client.ui.action.player.info;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

@UtilityClass
class PlayerInfoSummaryTextArea {
  /** Returns a text area with the players name, their IP and system ID. */
  JComponent buildPlayerInfoSummary(final PlayerSummary playerSummary) {
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(
        String.format(
            "%s\nIP: %s\nSystem ID: %s",
            playerSummary.getName(),
            playerSummary.getIp(),
            playerSummary.getSystemId()));
    return textPane;
  }
}
