package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete muted usernames (there is no update).
 */
public class MutedUsernameController {
  private static final Logger logger = Logger.getLogger(MutedUsernameController.class.getName());

  /**
   * Mute the username permanently.
   */
  public void addMutedUsername(final String username) {
    addMutedUsername(username, null);
  }

  /**
   * Mute the given username. If muteTill is not null, the mute will expire when muteTill is reached.
   *
   * <p>
   * If this username is already muted, this call will update the mute_end.
   * </p>
   */
  public void addMutedUsername(final String username, final Instant muteTill) {
    if (isUsernameMuted(username)) {
      removeMutedUsername(username);
    }
    Timestamp muteTillTs = null;
    if (muteTill != null) {
      muteTillTs = new Timestamp(muteTill.toEpochMilli());
    }
    logger.fine("Muting username:" + username);

    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps =
            con.prepareStatement("insert into muted_usernames (username, mute_till) values (?, ?)")) {
      ps.setString(1, username);
      ps.setTimestamp(2, muteTillTs);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      if (sqle.getErrorCode() == 30000) {
        // this is ok
        // the username is muted as expected
        logger.info("Tried to create duplicate muted username:" + username + " error:" + sqle.getMessage());
        return;
      }
      throw new IllegalStateException("Error inserting muted username:" + username, sqle);
    }
  }

  private void removeMutedUsername(final String username) {
    logger.fine("Removing muted username:" + username);

    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement("delete from muted_usernames where username = ?")) {
      ps.setString(1, username);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error deleting muted username:" + username, sqle);
    }
  }

  /**
   * Is the given username muted? This may have the side effect of removing from the
   * database any username's whose mute has expired.
   */
  public boolean isUsernameMuted(final String username) {
    final long muteTill = getUsernameUnmuteTime(username);
    return muteTill > System.currentTimeMillis();
  }

  /**
   * Returns epoch milli's of when mute expires, or negative one if there is no active mute.
   */
  public long getUsernameUnmuteTime(final String username) {
    long result = -1;
    boolean expired = false;
    final String sql = "select username, mute_till from muted_usernames where username = ?";

    try (final Connection con = Database.getPostgresConnection(); final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        final boolean found = rs.next();
        if (found) {
          final Timestamp muteTill = rs.getTimestamp(2);
          result = muteTill.getTime();
          if (result < System.currentTimeMillis()) {
            logger.fine("Mute expired for:" + username);
            expired = true;
          }
        } else {
          result = -1;
        }
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error for testing muted username existence:" + username, sqle);
    }
    // If the mute has expired, allow the username
    if (expired) {
      removeMutedUsername(username);
      // Signal as not-muted
      result = -1;
    }
    return result;
  }
}
