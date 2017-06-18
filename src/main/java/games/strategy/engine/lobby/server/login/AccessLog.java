package games.strategy.engine.lobby.server.login;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import games.strategy.util.TimeManager;

/**
 * This class is used to allow an access log to be easily created. The log settings should
 * be set up to log messages from this class to a seperate access log file.
 */
public class AccessLog {
  private static final Logger s_logger = Logger.getLogger(AccessLog.class.getName());

  public static void successfulLogin(final String userName, final InetAddress from) {
    s_logger.info("LOGIN name:" + userName + " ip:" + from.getHostAddress() + " time_ms:" + System.currentTimeMillis()
        + " time:" + TimeManager.toDateString(LocalDateTime.now()));
  }

  public static void failedLogin(final String userName, final InetAddress from, final String error) {
    s_logger.info("FAILED name:" + userName + " ip:" + from.getHostAddress() + " time_ms:" + System.currentTimeMillis()
        + " error:" + error + " time:" + TimeManager.toDateString(LocalDateTime.now()));
  }
}
