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
      final Component parentComponent, final LatestVersionResponse serverResponse) {
    JOptionPane.showOptionDialog(
        parentComponent,
        new JEditorPaneWithClickableLinks(String.format(serverResponse.getUpgradeMessageHtml())),
        "Update TripleA",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.PLAIN_MESSAGE,
        null,
        new Object[] {"Open Download Page"},
        null);

    OpenFileUtility.openUrl(parentComponent, serverResponse.getDownloadPageUrl());
  }
}
