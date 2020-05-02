package games.strategy.engine.lobby.client.ui.action.player.info;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;

@UtilityClass
class PlayerInfoSummaryTextArea {
  /** Returns a text area with the players name, their IP and system ID. */
  JComponent buildPlayerInfoSummary(final PlayerSummaryForModerator playerSummaryForModerator) {
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(
        String.format(
            "%s\nIP: %s\nSystem ID: %s",
            playerSummaryForModerator.getName(),
            playerSummaryForModerator.getIp(),
            playerSummaryForModerator.getSystemId()));
    return textPane;
  }
}
