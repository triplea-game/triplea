package games.strategy.engine.lobby.server.login;

import java.net.InetAddress;
import java.util.logging.Logger;

/**
 * This class is used to allow an access log to be easily created. The log settings should
 * be set up to log messages from this class to a seperate access log file.
 */
class AccessLog {
  private static final Logger s_logger = Logger.getLogger(AccessLog.class.getName());

  static void successfulLogin(final String userName, final InetAddress from) {
    s_logger.info(String.format("LOGIN name: %s, ip: %s",
        userName, from.getHostAddress()));
  }

  static void failedLogin(final String userName, final InetAddress from, final String error) {
    s_logger.info(String.format("FAILED name: %s, ip: %s",
        userName, from.getHostAddress()));
  }
}
