package games.strategy.engine.lobby.server.db;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Counts login hits/misses against primary and secondary database. This is to let us know
 * how the database migration is progressing along.
 * TODO: Lobby DB Migration - remove once completed
 */
class MigrationCounter {

  private static final Logger logger = Logger.getLogger(MigrationCounter.class.getName());

  private static final AtomicInteger secondaryLogin = new AtomicInteger(0);
  private static final AtomicInteger primaryLogin = new AtomicInteger(0);
  private static final AtomicInteger failedLogin = new AtomicInteger(0);

  /**
   * Marks a successful login to the secondary database.
   * The secondary database is where we wish to migrate to. Once we sustain near enough 100%, we can turn off
   * the primary and make the secondary primary.
   */
  void secondaryLoginSuccess() {
    secondaryLogin.incrementAndGet();
    logStats();
  }

  private static void logStats() {
    final double loginSuccessPercentage = (secondaryLogin.get() + primaryLogin.get()) == 0 ? 0 :
        (double) secondaryLogin.get() / (double) (secondaryLogin.get() + primaryLogin.get());
    final String statsMsg = String.format(
        "Login stats - failed (neither): %s, derby (primary): %s, postgres (secondary): %s, "
            + "successfull migration percentage: %s",
        failedLogin, primaryLogin, secondaryLogin, loginSuccessPercentage);
    logger.info(statsMsg);
  }

  /**
   * Marks a successful login to the primary database.
   * Succcessful means we matched creds, primary database is where we have our complete datastore.
   */
  void primaryLoginSuccess() {
    primaryLogin.incrementAndGet();
    logStats();
  }

  /**
   * Records how many times we failed to match creds in any database.
   * We record this data set to give a more complete picture when looking at primary/secondary success, and allows
   * us to calculate success/failure ratios.
   */
  void loginFailure() {
    failedLogin.incrementAndGet();
    logStats();
  }
}
