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
class MutedMacController implements MutedMacDao {
  private final Supplier<Connection> connection;

  /**
   * Mute the given mac. If muteTill is not null, the mute will expire when muteTill is reached.
   *
   * <p>
   * If this mac is already muted, this call will update the mute_end.
   * </p>
   *
   * @param mutedUser The user whose MAC will be muted.
   * @param muteTill The instant at which the mute will expire or {@code null} to mute the MAC forever.
   * @param moderator The moderator executing the mute.
   *
   * @throws IllegalStateException If an error occurs while adding, updating, or removing the mute.
   */
  @Override
  public void addMutedMac(final User mutedUser, final @Nullable Instant muteTill, final User moderator) {
    checkNotNull(mutedUser);
    checkNotNull(moderator);

    final String sql = ""
        + "insert into muted_macs "
        + "  (username, ip, mac, mute_till, mod_username, mod_ip, mod_mac) values (?, ?::inet, ?, ?, ?, ?::inet, ?) "
        + "on conflict (mac) do update set "
        + "  username=excluded.username, "
        + "  ip=excluded.ip, "
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
      throw new DatabaseException("Error inserting muted mac: " + mutedUser.getHashedMacAddress(), e);
    }
  }

  private void removeMutedMac(final String mac) {
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement("delete from muted_macs where mac=?")) {
      ps.setString(1, mac);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException("Error deleting muted mac: " + mac, e);
    }
  }

  /**
   * Is the given mac muted? This may have the side effect of removing from the
   * database any mac's whose mute has expired.
   */
  @Override
  public boolean isMacMuted(final Instant nowTime, final String mac) {
    return getMacUnmuteTime(mac).map(nowTime::isBefore).orElse(false);
  }

  /**
   * Returns an Optional Instant of the moment when the mute expires.
   * The optional is empty when the mac is not muted or the mute has already expired.
   */
  @Override
  public Optional<Instant> getMacUnmuteTime(final String mac) {
    final String sql = "select mac, mute_till from muted_macs where mac=?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, mac);
      try (ResultSet rs = ps.executeQuery()) {
        final boolean found = rs.next();
        if (found) {
          final Timestamp muteTill = rs.getTimestamp(2);
          if (muteTill == null) {
            return Optional.of(Instant.MAX);
          }
          final Instant expiration = muteTill.toInstant();
          if (expiration.isBefore(Instant.now())) {
            // If the mute has expired, allow the mac
            removeMutedMac(mac);
            // Signal as not-muted
            return Optional.empty();
          }
          return Optional.of(expiration);
        }
        return Optional.empty();
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error for testing muted mac existence: " + mac, e);
    }
  }
}
