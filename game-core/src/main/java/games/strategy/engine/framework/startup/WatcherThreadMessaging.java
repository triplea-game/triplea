package games.strategy.engine.framework.startup;

import games.strategy.triplea.UrlConstants;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.awt.OpenFileUtility;
import org.triplea.util.ExitStatus;

/** Interface to communicate lobby watcher events to user. */
public interface WatcherThreadMessaging {
  void serverNotAvailableHandler(String message);

  /** Simply logs notifications */
  @Log
  class HeadlessWatcherThreadMessaging implements WatcherThreadMessaging {
    @Override
    public void serverNotAvailableHandler(final String message) {
      log.severe(message);
      ExitStatus.FAILURE.exit();
    }
  }

  /** Shows UI notifications to the user. */
  @AllArgsConstructor
  class HeadedWatcherThreadMessaging implements WatcherThreadMessaging {
    private final Component parent;

    @Override
    public void serverNotAvailableHandler(final String message) {
      SwingUtilities.invokeLater(
          () -> {
            final Frame parentComponent = JOptionPane.getFrameForComponent(parent);
            if (JOptionPane.showConfirmDialog(
                    parentComponent,
                    message
                        + "\nDo you want to view the tutorial on how to host? "
                        + "This will open in your internet browser.",
                    "View Help Website?",
                    JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
              OpenFileUtility.openUrl(UrlConstants.USER_GUIDE);
            }
            ExitStatus.FAILURE.exit();
          });
    }
  }
}
