package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Logger;

import games.strategy.util.Tuple;

/**
 * Utility class to create/read/delete banned usernames (there is no update).
 */
public class BannedUsernameController extends TimedController implements BannedUsernameDao {
  private static final Logger logger = Logger.getLogger(BannedUsernameController.class.getName());

  @Override
  public void addBannedUsername(final String username, final Instant banTill) {
    if (banTill == null || banTill.isAfter(now())) {
      logger.fine("Banning username:" + username);

      try (final Connection con = Database.getPostgresConnection();
          final PreparedStatement ps =
              con.prepareStatement("insert into banned_usernames (username, ban_till) values (?, ?)"
                  + " on conflict (username) do update set ban_till=excluded.ban_till")) {
        ps.setString(1, username);
        ps.setTimestamp(2, banTill != null ? Timestamp.from(banTill) : null);
        ps.execute();
        con.commit();
      } catch (final SQLException sqle) {
        throw new IllegalStateException("Error inserting banned username:" + username, sqle);
      }
    } else {
      removeBannedUsername(username);
    }
  }

  private void removeBannedUsername(final String username) {
    logger.fine("Removing banned username:" + username);

    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement("delete from banned_usernames where username = ?")) {
      ps.setString(1, username);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error deleting banned username:" + username, sqle);
    }
  }

  /**
   * This implementation has the side effect of removing any usernames whose ban has expired.
   */
  @Override
  public Tuple<Boolean, Timestamp> isUsernameBanned(final String username) {
    final String sql = "select username, ban_till from banned_usernames where username = ?";

    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        // If the ban has expired, allow the username
        if (rs.next()) {
          final Timestamp banTill = rs.getTimestamp(2);
          if (banTill != null && banTill.toInstant().isBefore(now())) {
            logger.fine("Ban expired for:" + username);
            removeBannedUsername(username);
            return Tuple.of(false, banTill);
          }
          return Tuple.of(true, banTill);
        }
        return Tuple.of(false, null);
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error for testing banned username existence:" + username, sqle);
    }
  }
}
