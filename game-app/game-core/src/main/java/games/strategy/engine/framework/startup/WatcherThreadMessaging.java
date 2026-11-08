package games.strategy.engine.framework.startup;

import lombok.extern.slf4j.Slf4j;
import org.triplea.util.ExitStatus;

/** Interface to communicate lobby watcher events to user. */
public interface WatcherThreadMessaging {
  String COMPUTER_NOT_REACHABLE_ERROR_MESSAGE =
"""
Your computer is not reachable from the internet.
Please make sure your Firewall allows incoming connections (hosting) for TripleA.
(The firewall exception must be updated every time a new version of TripleA comes out.)
And that your Router is configured to send TCP traffic the correct port  to your local ip address.""";

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
}
