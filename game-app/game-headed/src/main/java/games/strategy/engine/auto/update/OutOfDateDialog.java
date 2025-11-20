package games.strategy.engine.auto.update;

import java.awt.Component;
import javax.swing.JOptionPane;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.http.client.latest.version.LatestVersionResponse;
import org.triplea.swing.JEditorPaneWithClickableLinks;

@UtilityClass
class OutOfDateDialog {
  static void showOutOfDateComponent(
      final Component parentComponent, final LatestVersionResponse latestVersion) {
    final int result =
        JOptionPane.showOptionDialog(
            parentComponent,
            new JEditorPaneWithClickableLinks(getOutOfDateMessage(latestVersion)),
            "TripleA is out of date!",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[] {"Download Now", "Remind Me Later"},
            null);

    if (result == JOptionPane.YES_OPTION) {
      OpenFileUtility.openUrl(parentComponent, latestVersion.getDownloadUrl());
    }
  }

  private static String getOutOfDateMessage(final LatestVersionResponse latestVersionResponse) {
    return String.format(
        "<html>"
            + "<h2>TripleA %s is available!</h2>"
            + "<center><a class=\"external\" href=\"%s\">Release Notes</a></center>"
            + "</html>",
        latestVersionResponse.getLatestEngineVersion(), latestVersionResponse.getReleaseNotesUrl());
  }
}
