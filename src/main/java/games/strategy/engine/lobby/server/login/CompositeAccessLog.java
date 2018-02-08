package games.strategy.engine.lobby.server.login;

import java.net.InetAddress;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.db.LoginMetricsController;
import games.strategy.engine.lobby.server.db.LoginMetricsDao;

/**
 * Implementation of {@link AccessLog} that logs lobby access attempts to both the system logger framework and the
 * database login metrics tables.
 */
final class CompositeAccessLog implements AccessLog {
  private static final Logger logger = Logger.getLogger(CompositeAccessLog.class.getName());

  private final LoginMetricsDao loginMetricsDao;

  CompositeAccessLog() {
    this(new LoginMetricsController());
  }

  @VisibleForTesting
  CompositeAccessLog(final LoginMetricsDao loginMetricsDao) {
    this.loginMetricsDao = loginMetricsDao;
  }

  @Override
  public void logFailedLogin(
      final LoginType loginType,
      final String username,
      final InetAddress address,
      final String errorMessage) {
    logger.info(String.format("FAILED login: type: '%s', name: '%s', IP: '%s', error: '%s'",
        loginType, username, address.getHostAddress(), errorMessage));
  }

  @Override
  public void logSuccessfulLogin(final LoginType loginType, final String username, final InetAddress address) {
    logger.info(String.format("SUCCESSFUL login: type: '%s', name: '%s', IP: '%s'",
        loginType, username, address.getHostAddress()));

    try {
      loginMetricsDao.addSuccessfulLogin(loginType);
    } catch (final SQLException e) {
      logger.log(Level.SEVERE, "failed to record successful login", e);
    }
  }
}
