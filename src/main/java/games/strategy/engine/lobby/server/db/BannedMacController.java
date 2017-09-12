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
 * Utilitiy class to create/read/delete banned macs (there is no update).
 */
public class BannedMacController {
  private static final Logger logger = Logger.getLogger(BannedMacController.class.getName());

  /**
   * Ban the mac permanently.
   */
  public void addBannedMac(final String mac) {
    addBannedMac(mac, null);
  }

  /**
   * Ban the given mac. If banTill is not null, the ban will expire when banTill is reached.
   *
   * <p>
   * If this mac is already banned, this call will update the ban_end.
   * </p>
   */
  public void addBannedMac(final String mac, final Instant banTill) {
    if (isMacBanned(mac).getFirst()) {
      removeBannedMac(mac);
    }
    Timestamp banTillTs = null;
    if (banTill != null) {
      banTillTs = Timestamp.from(banTill);
    }
    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps =
            con.prepareStatement("insert into banned_macs (mac, ban_till) values (?, ?) on conflict do update")) {
      ps.setString(1, mac);
      ps.setTimestamp(2, banTillTs);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error inserting banned mac:" + mac, sqle);
    }
  }

  private void removeBannedMac(final String mac) {
    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement("delete from banned_macs where mac = ?")) {
      ps.setString(1, mac);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error deleting banned mac:" + mac, sqle);
    }
  }

  /**
   * Is the given mac banned? This may have the side effect of removing from the
   * database any mac's whose ban has expired.
   */
  public Tuple<Boolean, Timestamp> isMacBanned(final String mac) {
    boolean found = false;
    boolean expired = false;
    Timestamp banTill = null;
    final String sql = "select mac, ban_till from banned_macs where mac = ?";

    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, mac);
      try (final ResultSet rs = ps.executeQuery()) {
        found = rs.next();
        // If the ban has expired, allow the mac
        if (found) {
          banTill = rs.getTimestamp(2);
          if (banTill != null && banTill.getTime() < System.currentTimeMillis()) {
            logger.fine("Ban expired for:" + mac);
            expired = true;
          }
        }
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error for testing banned mac existence:" + mac, sqle);
    }
    if (expired) {
      removeBannedMac(mac);
      return Tuple.of(false, banTill);
    }
    return Tuple.of(found, banTill);
  }
}
