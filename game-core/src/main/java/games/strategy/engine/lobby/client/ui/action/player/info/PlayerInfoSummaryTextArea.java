package games.strategy.engine.lobby.client.ui.action.player.info;

import com.google.common.annotations.VisibleForTesting;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

@UtilityClass
class PlayerInfoSummaryTextArea {
  /** Returns a text area with the players name, their IP and system ID. */
  JComponent buildPlayerInfoSummary(final UserName playerName, final PlayerSummary playerSummary) {
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(buildPlayerInfoText(playerName, playerSummary));
    return textPane;
  }

  @VisibleForTesting
  static String buildPlayerInfoText(final UserName playerName, final PlayerSummary playerSummary) {
    return playerName.getValue()
        + (playerSummary.getIp() == null ? "" : "\nIP: " + playerSummary.getIp())
        + (playerSummary.getSystemId() == null
            ? ""
            : "\nSystem ID: " + playerSummary.getSystemId());
  }
}
