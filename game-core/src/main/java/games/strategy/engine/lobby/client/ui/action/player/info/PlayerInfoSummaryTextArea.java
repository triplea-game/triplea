package games.strategy.engine.lobby.client.ui.action.player.info;

import com.google.common.annotations.VisibleForTesting;
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
    textPane.setText(buildPlayerInfoText(playerSummary));
    return textPane;
  }

  @VisibleForTesting
  static String buildPlayerInfoText(final PlayerSummary playerSummary) {
    return playerSummary.getName()
        + (playerSummary.getIp() == null ? "" : "\nIP: " + playerSummary.getIp())
        + (playerSummary.getSystemId() == null
            ? ""
            : "\nSystem ID: " + playerSummary.getSystemId());
  }
}
