package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete muted macs (there is no update).
 */
public class MutedMacController extends TimedController {
  private static final Logger logger = Logger.getLogger(MutedMacController.class.getName());

  /**
   * Mute the given mac. If muteTill is not null, the mute will expire when muteTill is reached.
   *
   * <p>
   * If this mac is already muted, this call will update the mute_end.
   * </p>
   */
  public void addMutedMac(final String mac, final Instant muteTill) {
    if (muteTill == null || muteTill.isAfter(now())) {
      logger.fine("Muting mac:" + mac);

      try (Connection con = Database.getPostgresConnection();
          PreparedStatement ps = con.prepareStatement("insert into muted_macs (mac, mute_till) values (?, ?)"
              + " on conflict (mac) do update set mute_till=excluded.mute_till")) {
        ps.setString(1, mac);
        ps.setTimestamp(2, muteTill != null ? Timestamp.from(muteTill) : null);
        ps.execute();
        con.commit();
      } catch (final SQLException sqle) {
        throw new IllegalStateException("Error inserting muted mac:" + mac, sqle);
      }
    } else {
      removeMutedMac(mac);
    }
  }

  private static void removeMutedMac(final String mac) {
    logger.fine("Removing muted mac:" + mac);

    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement("delete from muted_macs where mac=?")) {
      ps.setString(1, mac);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error deleting muted mac:" + mac, sqle);
    }
  }

  /**
   * Is the given mac muted? This may have the side effect of removing from the
   * database any mac's whose mute has expired.
   */
  public boolean isMacMuted(final String mac) {
    final Optional<Instant> muteTill = getMacUnmuteTime(mac);
    return !muteTill.isPresent() || muteTill.get().isAfter(now());
  }

  /**
   * Returns an Optional Instant of the moment when the mute expires.
   * The optional is empty when the mute never expires.
   * If the mac isn't muted or the mute already expired the optional instant is
   * Instant.EPOCH
   */
  public Optional<Instant> getMacUnmuteTime(final String mac) {
    final String sql = "select mac, mute_till from muted_macs where mac=?";

    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, mac);
      try (ResultSet rs = ps.executeQuery()) {
        final boolean found = rs.next();
        if (found) {
          final Timestamp muteTill = rs.getTimestamp(2);
          if (muteTill == null) {
            return Optional.empty();
          }
          final Instant expiration = muteTill.toInstant();
          if (expiration.isBefore(now())) {
            logger.fine("Mute expired for:" + mac);
            // If the mute has expired, allow the mac
            removeMutedMac(mac);
            // Signal as not-muted
            return Optional.of(Instant.EPOCH);
          }
          return Optional.of(expiration);
        }
        return Optional.of(Instant.EPOCH);
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error for testing muted mac existence:" + mac, sqle);
    }
  }
}
