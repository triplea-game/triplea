package games.strategy.engine.lobby.server.login;

import java.net.InetAddress;

/**
 * A lobby access log.
 */
interface AccessLog {
  void logFailedLogin(LoginType loginType, String username, InetAddress address, String errorMessage);

  void logSuccessfulLogin(LoginType loginType, String username, InetAddress address);
}
