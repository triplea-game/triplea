package games.strategy.engine.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import javax.annotation.Nullable;

import games.strategy.engine.lobby.server.User;
import games.strategy.util.Tuple;

/**
 * Utility class to create/read/delete banned usernames (there is no update).
 */
public class BannedUsernameController extends TimedController implements BannedUsernameDao {
  @Override
  public void addBannedUsername(final User bannedUser, final @Nullable Instant banTill, final User moderator) {
    checkNotNull(bannedUser);
    checkNotNull(moderator);

    if ((banTill != null) && banTill.isBefore(now())) {
      removeBannedUsername(bannedUser.getUsername());
      return;
    }

    final String sql = ""
        + "insert into banned_usernames "
        + "  (username, ip, mac, ban_till, mod_username, mod_ip, mod_mac) values (?, ?::inet, ?, ?, ?, ?::inet, ?) "
        + "on conflict (username) do update set "
        + "  ip=excluded.ip, "
        + "  mac=excluded.mac, "
        + "  ban_till=excluded.ban_till, "
        + "  mod_username=excluded.mod_username, "
        + "  mod_ip=excluded.mod_ip, "
        + "  mod_mac=excluded.mod_mac";
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, bannedUser.getUsername());
      ps.setString(2, bannedUser.getInetAddress().getHostAddress());
      ps.setString(3, bannedUser.getHashedMacAddress());
      ps.setTimestamp(4, (banTill != null) ? Timestamp.from(banTill) : null);
      ps.setString(5, moderator.getUsername());
      ps.setString(6, moderator.getInetAddress().getHostAddress());
      ps.setString(7, moderator.getHashedMacAddress());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new IllegalStateException("Error inserting banned username: " + bannedUser.getUsername(), e);
    }
  }

  private static void removeBannedUsername(final String username) {
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement("delete from banned_usernames where username = ?")) {
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
  public Tuple<Boolean, /* @Nullable */ Timestamp> isUsernameBanned(final String username) {
    final String sql = "select username, ban_till from banned_usernames where username = ?";
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        // If the ban has expired, allow the username
        if (rs.next()) {
          final Timestamp banTill = rs.getTimestamp(2);
          if ((banTill != null) && banTill.toInstant().isBefore(now())) {
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
