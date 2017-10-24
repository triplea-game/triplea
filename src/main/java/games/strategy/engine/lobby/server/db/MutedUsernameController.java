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
public class MutedUsernameController extends TimedController {
  private static final Logger logger = Logger.getLogger(MutedUsernameController.class.getName());

  /**
   * Mute the given username. If muteTill is not null, the mute will expire when muteTill is reached.
   *
   * <p>
   * If this username is already muted, this call will update the mute_end.
   * </p>
   */
  public void addMutedUsername(final String username, final Instant muteTill) {
    if (muteTill == null || muteTill.isAfter(now())) {
      logger.fine("Muting username:" + username);

      try (Connection con = Database.getPostgresConnection();
          PreparedStatement ps = con.prepareStatement("insert into muted_usernames (username, mute_till) values (?, ?)"
              + " on conflict (username) do update set mute_till=excluded.mute_till")) {
        ps.setString(1, username);
        ps.setTimestamp(2, muteTill != null ? Timestamp.from(muteTill) : null);
        ps.execute();
        con.commit();
      } catch (final SQLException sqle) {
        throw new IllegalStateException("Error inserting muted username:" + username, sqle);
      }
    } else {
      removeMutedUsername(username);
    }
  }

  private static void removeMutedUsername(final String username) {
    logger.fine("Removing muted username:" + username);

    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement("delete from muted_usernames where username = ?")) {
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
    return muteTill > now().toEpochMilli();
  }

  /**
   * Returns epoch milli's of when mute expires, or negative one if there is no active mute.
   */
  public long getUsernameUnmuteTime(final String username) {
    final String sql = "select username, mute_till from muted_usernames where username = ?";

    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        final boolean found = rs.next();
        if (found) {
          final Timestamp muteTill = rs.getTimestamp(2);
          if (muteTill == null) {
            return Long.MAX_VALUE;
          }
          if (muteTill.toInstant().isBefore(now())) {
            // If the mute has expired, allow the username
            logger.fine("Mute expired for:" + username);
            removeMutedUsername(username);
            // Signal as not-muted
            return -1;
          }
          return muteTill.getTime();
        }
        return -1;
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error for testing muted username existence:" + username, sqle);
    }
  }
}
