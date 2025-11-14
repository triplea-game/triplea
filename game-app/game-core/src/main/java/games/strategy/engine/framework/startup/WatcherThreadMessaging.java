package games.strategy.engine.framework.startup;

import games.strategy.triplea.UrlConstants;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.awt.OpenFileUtility;
import org.triplea.util.ExitStatus;

/** Interface to communicate lobby watcher events to user. */
public interface WatcherThreadMessaging {
  String COMPUTER_NOT_REACHABLE_ERROR_MESSAGE =
      "Your computer is not reachable from the internet.\n"
          + "Please make sure your Firewall allows incoming connections (hosting) "
          + "for TripleA.\n"
          + "(The firewall exception must be updated every time a new version of "
          + "TripleA comes out.)\n"
          + "And that your Router is configured to send TCP traffic the correct port "
          + " to your local ip address.";

  /**
   * When a host posts a game to the lobby, the lobby will verify connectivity to the host via a
   * 'reverse connection' back to the game host. If the reverse connection fails then this method is
   * invoked. Once that happens the server game should initiate a shutdown and instruct the user
   * their host is not available on the internet and give them a link on how to set up port
   * forwarding.
   */
  void handleCurrentGameHostNotReachable();

  /** Simply logs notifications */
  @Slf4j
  class HeadlessWatcherThreadMessaging implements WatcherThreadMessaging {
    @Override
    public void handleCurrentGameHostNotReachable() {
      log.error(COMPUTER_NOT_REACHABLE_ERROR_MESSAGE);
      ExitStatus.FAILURE.exit();
    }
  }

  /** Shows UI notifications to the user. */
  @AllArgsConstructor
  class HeadedWatcherThreadMessaging implements WatcherThreadMessaging {
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
}
