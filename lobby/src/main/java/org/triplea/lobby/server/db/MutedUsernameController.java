package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.triplea.lobby.server.User;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class MutedUsernameController implements MutedUsernameDao {
  private final Supplier<Connection> connection;

  /**
   * Mute the given username. If muteTill is not null, the mute will expire when muteTill is reached.
   *
   * <p>
   * If this username is already muted, this call will update the mute_end.
   * </p>
   *
   * @param mutedUser The user whose username will be muted.
   * @param muteTill The instant at which the mute will expire or {@code null} to mute the username forever.
   * @param moderator The moderator executing the mute.
   *
   * @throws IllegalStateException If an error occurs while adding, updating, or removing the mute.
   */
  @Override
  public void addMutedUsername(final User mutedUser, final @Nullable Instant muteTill, final User moderator) {
    checkNotNull(mutedUser);
    checkNotNull(moderator);

    if (muteTill != null && muteTill.isBefore(Instant.now())) {
      removeMutedUsername(mutedUser.getUsername());
      return;
    }

    final String sql = ""
        + "insert into muted_usernames "
        + "  (username, ip, mac, mute_till, mod_username, mod_ip, mod_mac) values (?, ?::inet, ?, ?, ?, ?::inet, ?) "
        + "on conflict (username) do update set "
        + "  ip=excluded.ip, "
        + "  mac=excluded.mac, "
        + "  mute_till=excluded.mute_till, "
        + "  mod_username=excluded.mod_username, "
        + "  mod_ip=excluded.mod_ip, "
        + "  mod_mac=excluded.mod_mac";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, mutedUser.getUsername());
      ps.setString(2, mutedUser.getInetAddress().getHostAddress());
      ps.setString(3, mutedUser.getHashedMacAddress());
      ps.setTimestamp(4, muteTill != null ? Timestamp.from(muteTill) : null);
      ps.setString(5, moderator.getUsername());
      ps.setString(6, moderator.getInetAddress().getHostAddress());
      ps.setString(7, moderator.getHashedMacAddress());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error inserting muted username: " + mutedUser.getUsername(), e);
    }
  }

  private void removeMutedUsername(final String username) {
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement("delete from muted_usernames where username = ?")) {
      ps.setString(1, username);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error deleting muted username: " + username, e);
    }
  }

  /**
   * Is the given username muted? This may have the side effect of removing from the
   * database any username's whose mute has expired.
   */
  @Override
  public boolean isUsernameMuted(final Instant nowTime, final String username) {
    return getUsernameUnmuteTime(username).map(nowTime::isBefore).orElse(false);
  }

  /**
   * Returns an Optional Instant of the moment when the mute expires.
   * The optional is empty when the username is not muted or the mute has already expired.
   */
  @Override
  public Optional<Instant> getUsernameUnmuteTime(final String username) {
    final String sql = "select username, mute_till from muted_usernames where username = ?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        final boolean found = rs.next();
        if (found) {
          final Timestamp muteTill = rs.getTimestamp(2);
          if (muteTill == null) {
            return Optional.of(Instant.MAX);
          }
          final Instant expiration = muteTill.toInstant();
          if (expiration.isBefore(Instant.now())) {
            // If the mute has expired, allow the username
            removeMutedUsername(username);
            // Signal as not-muted
            return Optional.empty();
          }
          return Optional.of(muteTill.toInstant());
        }
        return Optional.empty();
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error for testing muted username existence: " + username, e);
    }
  }
}
