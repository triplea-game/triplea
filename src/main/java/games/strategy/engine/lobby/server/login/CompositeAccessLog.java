package games.strategy.engine.lobby.server.login;

import java.sql.SQLException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.db.AccessLogController;
import games.strategy.engine.lobby.server.db.AccessLogDao;

/**
 * Implementation of {@link AccessLog} that logs lobby access attempts to both the system logger framework and the
 * database login metrics tables.
 */
final class CompositeAccessLog implements AccessLog {
  private static final Logger logger = Logger.getLogger(CompositeAccessLog.class.getName());

  private final AccessLogDao accessLogDao;

  CompositeAccessLog() {
    this(new AccessLogController());
  }

  @VisibleForTesting
  CompositeAccessLog(final AccessLogDao accessLogDao) {
    this.accessLogDao = accessLogDao;
  }

  @Override
  public void logFailedAccess(
      final Instant instant,
      final User user,
      final AccessMethod accessMethod,
      final String errorMessage) {
    logger.info(String.format("Failed %s access [%s]: name: %s, IP: %s, MAC: %s, error: %s",
        accessMethod.toString().toLowerCase(),
        instant,
        user.getUsername(),
        user.getInetAddress().getHostAddress(),
        user.getHashedMacAddress(),
        errorMessage));
  }

  @Override
  public void logSuccessfulAccess(final Instant instant, final User user, final AccessMethod accessMethod) {
    logger.info(String.format("Successful %s access [%s]: name: %s, IP: %s, MAC: %s",
        accessMethod.toString().toLowerCase(),
        instant,
        user.getUsername(),
        user.getInetAddress().getHostAddress(),
        user.getHashedMacAddress()));

    try {
      accessLogDao.insert(instant, user, accessMethod == AccessMethod.AUTHENTICATION);
    } catch (final SQLException e) {
      logger.log(Level.SEVERE, "failed to record successful access", e);
    }
  }
}
