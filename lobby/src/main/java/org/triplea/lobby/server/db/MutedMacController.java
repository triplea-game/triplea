package org.triplea.lobby.server.db;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

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
  public void addMutedMac(
      final InetAddress netAddress,
      final String hashedMac,
      final @Nullable Instant muteTill,
      final String moderatorName) {
    checkNotNull(netAddress);
    checkNotNull(netAddress.getHostAddress());
    checkNotNull(hashedMac);
    checkNotNull(moderatorName);

    final String sql = ""
        + "insert into muted_macs "
        + "  (ip, mac, mute_till, mod_username) values (?::inet, ?, ?, ?) "
        + "on conflict (mac) do update set "
        + "  ip=excluded.ip, "
        + "  mute_till=excluded.mute_till, "
        + "  mod_username=excluded.mod_username";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, netAddress.getHostAddress());
      ps.setString(2, hashedMac);
      ps.setTimestamp(3, Optional.ofNullable(muteTill).map(Timestamp::from).orElse(null));
      ps.setString(4, moderatorName);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException(
          String.format("Error inserting muted mac, address: %s, mac: %s, expiration: %s",
              netAddress, hashedMac, muteTill),
          e);
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
