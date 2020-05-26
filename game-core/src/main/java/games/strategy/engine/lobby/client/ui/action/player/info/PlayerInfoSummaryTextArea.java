package games.strategy.engine.lobby.client.ui.action.player.info;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Date;
import java.text.DateFormat;
import java.time.Instant;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTextPane;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.swing.SwingComponents;

@UtilityClass
class PlayerInfoSummaryTextArea {
  /** Returns a text area with the players name, their IP and system ID. */
  JComponent buildPlayerInfoSummary(final JDialog dialog, final PlayerSummary playerSummary) {
    final JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setText(buildPlayerInfoText(playerSummary));
    textPane.addKeyListener(SwingComponents.escapeKeyListener(dialog::dispose));
    return textPane;
  }

  @VisibleForTesting
  static String buildPlayerInfoText(final PlayerSummary playerSummary) {
    return (playerSummary.getRegistrationDateEpochMillis() == null
            ? "Not registered"
            : "Registered on: " + formatEpochMillis(playerSummary.getRegistrationDateEpochMillis()))
        + (playerSummary.getIp() == null ? "" : "\nIP: " + playerSummary.getIp())
        + (playerSummary.getSystemId() == null
            ? ""
            : "\nSystem ID: " + playerSummary.getSystemId());
  }

  private static String formatEpochMillis(final long epochMillis) {
    return DateFormat.getDateInstance(DateFormat.MEDIUM) //
        .format(Date.from(Instant.ofEpochMilli(epochMillis)));
  }
}
