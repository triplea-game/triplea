package games.strategy.engine.framework.startup;

import games.strategy.triplea.UrlConstants;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AllArgsConstructor;
import org.triplea.awt.OpenFileUtility;
import org.triplea.util.ExitStatus;

/** Shows UI notifications to the user. */
@AllArgsConstructor
public class HeadedWatcherThreadMessaging implements WatcherThreadMessaging {
  private final Component parent;

  @Override
  public void handleCurrentGameHostNotReachable() {
    SwingUtilities.invokeLater(
        () -> {
          final Frame parentComponent = JOptionPane.getFrameForComponent(parent);
          if (JOptionPane.showConfirmDialog(
                  parentComponent,
                  COMPUTER_NOT_REACHABLE_ERROR_MESSAGE
                      + "\nWould you like to view the TripleA user-guide which has a "
                      + "tutorial on how to host?",
                  "View Help Website?",
                  JOptionPane.YES_NO_OPTION)
              == JOptionPane.YES_OPTION) {
            OpenFileUtility.openUrl(parentComponent, UrlConstants.USER_GUIDE);
          }
          ExitStatus.FAILURE.exit();
        });
  }
}
