package org.triplea.lobby.server.userDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.util.Tuple;

/**
 * Utilitiy class to create/read/delete banned ips (there is no update).
 */
public class BannedIpController {
  private static final Logger s_logger = Logger.getLogger(BannedIpController.class.getName());

  /**
   * Ban the ip permanently
   */
  public void addBannedIp(final String ip) {
    addBannedIp(ip, null);
  }

  /**
   * Ban the given ip. If banTill is not null, the ban will expire when banTill is reached.
   * <p>
   * If this ip is already banned, this call will update the ban_end.
   */
  public void addBannedIp(final String ip, final Date banTill) {
    if (isIpBanned(ip).getFirst()) {
      removeBannedIp(ip);
    }
    Timestamp banTillTs = null;
    if (banTill != null) {
      banTillTs = new Timestamp(banTill.getTime());
    }
    s_logger.fine("Banning ip:" + ip);
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement("insert into banned_ips (ip, ban_till) values (?, ?)");
      ps.setString(1, ip);
      ps.setTimestamp(2, banTillTs);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      if (sqle.getErrorCode() == 30000) {
        // this is ok
        // the ip is banned as expected
        s_logger.info("Tried to create duplicate banned ip:" + ip + " error:" + sqle.getMessage());
        return;
      }
      s_logger.log(Level.SEVERE, "Error inserting banned ip:" + ip, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  public void removeBannedIp(final String ip) {
    s_logger.fine("Removing banned ip:" + ip);
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement("delete from banned_ips where ip = ?");
      ps.setString(1, ip);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      s_logger.log(Level.SEVERE, "Error deleting banned ip:" + ip, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  /**
   * Is the given ip banned? This may have the side effect of removing from the
   * database any ip's whose ban has expired
   */
  public Tuple<Boolean, Timestamp> isIpBanned(final String ip) {
    boolean found = false;
    boolean expired = false;
    Timestamp banTill = null;
    final String sql = "select ip, ban_till from banned_ips where ip = ?";
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, ip);
      final ResultSet rs = ps.executeQuery();
      found = rs.next();
      // if the ban has expird, allow the ip
      if (found) {
        banTill = rs.getTimestamp(2);
        if (banTill != null && banTill.getTime() < System.currentTimeMillis()) {
          s_logger.fine("Ban expired for:" + ip);
          expired = true;
        }
      }
      rs.close();
      ps.close();
    } catch (final SQLException sqle) {
      s_logger.info("Error for testing banned ip existence:" + ip + " error:" + sqle.getMessage());
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
    if (expired) {
      removeBannedIp(ip);
      return Tuple.of(false, banTill);
    }
    return Tuple.of(found, banTill);
  }
}
