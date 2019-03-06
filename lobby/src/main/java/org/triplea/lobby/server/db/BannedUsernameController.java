package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.triplea.lobby.server.User;
import org.triplea.util.Tuple;

import lombok.AllArgsConstructor;

/**
 * Utility class to create/read/delete banned usernames (there is no update).
 */
@AllArgsConstructor
class BannedUsernameController implements BannedUsernameDao {
  private final Supplier<Connection> connection;

  @Override
  public void addBannedUsername(final User bannedUser, final @Nullable Instant banTill, final User moderator) {
    checkNotNull(bannedUser);
    checkNotNull(moderator);

    if (banTill != null && banTill.isBefore(Instant.now())) {
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
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, bannedUser.getUsername());
      ps.setString(2, bannedUser.getInetAddress().getHostAddress());
      ps.setString(3, bannedUser.getHashedMacAddress());
      ps.setTimestamp(4, banTill != null ? Timestamp.from(banTill) : null);
      ps.setString(5, moderator.getUsername());
      ps.setString(6, moderator.getInetAddress().getHostAddress());
      ps.setString(7, moderator.getHashedMacAddress());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error inserting banned username: " + bannedUser.getUsername(), e);
    }
  }

  private void removeBannedUsername(final String username) {
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement("delete from banned_usernames where username = ?")) {
      ps.setString(1, username);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error deleting banned username: " + username, e);
    }
  }

  /**
   * This implementation has the side effect of removing any usernames whose ban has expired.
   */
  @Override
  public Tuple<Boolean, /* @Nullable */ Timestamp> isUsernameBanned(final String username) {
    final String sql = "select username, ban_till from banned_usernames where username = ?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        // If the ban has expired, allow the username
        if (rs.next()) {
          final Timestamp banTill = rs.getTimestamp(2);
          if (banTill != null && banTill.toInstant().isBefore(Instant.now())) {
            removeBannedUsername(username);
            return Tuple.of(false, banTill);
          }
          return Tuple.of(true, banTill);
        }
        return Tuple.of(false, null);
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error for testing banned username existence: " + username, e);
    }
  }
}
