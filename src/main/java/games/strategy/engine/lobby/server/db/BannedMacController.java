package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import games.strategy.util.Tuple;

/**
 * Utility class to create/read/delete banned macs (there is no update).
 */
public class BannedMacController extends TimedController implements BannedMacDao {
  private static final Logger logger = Logger.getLogger(BannedMacController.class.getName());

  @Override
  public void addBannedMac(final String mac, final @Nullable Instant banTill) {
    if (banTill == null || banTill.isAfter(now())) {
      try (final Connection con = Database.getPostgresConnection();
          final PreparedStatement ps = con.prepareStatement("insert into banned_macs (mac, ban_till) values (?, ?)"
              + " on conflict (mac) do update set ban_till=excluded.ban_till")) {
        ps.setString(1, mac);
        ps.setTimestamp(2, banTill != null ? Timestamp.from(banTill) : null);
        ps.execute();
        con.commit();
      } catch (final SQLException sqle) {
        throw new IllegalStateException("Error inserting banned mac:" + mac, sqle);
      }
    } else {
      removeBannedMac(mac);
    }
  }

  private void removeBannedMac(final String mac) {
    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement("delete from banned_macs where mac=?")) {
      ps.setString(1, mac);
      ps.execute();
      con.commit();
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error deleting banned mac:" + mac, sqle);
    }
  }

  /**
   * This implementation has the side effect of removing any MACs whose ban has expired.
   */
  @Override
  public Tuple<Boolean, /* @Nullable */ Timestamp> isMacBanned(final String mac) {
    final String sql = "select mac, ban_till from banned_macs where mac=?";

    try (final Connection con = Database.getPostgresConnection();
        final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, mac);
      try (final ResultSet rs = ps.executeQuery()) {
        // If the ban has expired, allow the mac
        if (rs.next()) {
          final Timestamp banTill = rs.getTimestamp(2);
          if (banTill != null && banTill.toInstant().isBefore(now())) {
            logger.fine("Ban expired for:" + mac);
            removeBannedMac(mac);
            return Tuple.of(false, banTill);
          }
          return Tuple.of(true, banTill);
        }
        return Tuple.of(false, null);
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error for testing banned mac existence:" + mac, sqle);
    }
  }
}
