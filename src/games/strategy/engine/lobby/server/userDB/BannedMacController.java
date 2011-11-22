/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.lobby.server.userDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete banned macs (there is no update).
 * 
 * @author Wisconsin
 */
public class BannedMacController
{
	private static final Logger s_logger = Logger.getLogger(BannedMacController.class.getName());
	
	/**
	 * Ban the mac permanently
	 */
	public void addBannedMac(final String mac)
	{
		addBannedMac(mac, null);
	}
	
	/**
	 * Ban the given mac. If banTill is not null, the ban will expire when banTill is reached.
	 * <p>
	 * 
	 * If this mac is already banned, this call will update the ban_end.
	 */
	public void addBannedMac(final String mac, final Date banTill)
	{
		if (isMacBanned(mac))
		{
			removeBannedMac(mac);
		}
		Timestamp banTillTs = null;
		if (banTill != null)
		{
			banTillTs = new Timestamp(banTill.getTime());
		}
		s_logger.fine("Banning mac:" + mac);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("insert into banned_macs (mac, ban_till) values (?, ?)");
			ps.setString(1, mac);
			ps.setTimestamp(2, banTillTs);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			if (sqle.getErrorCode() == 30000)
			{
				// this is ok
				// the mac is banned as expected
				s_logger.info("Tried to create duplicate banned mac:" + mac + " error:" + sqle.getMessage());
				return;
			}
			s_logger.log(Level.SEVERE, "Error inserting banned mac:" + mac, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	public void removeBannedMac(final String mac)
	{
		s_logger.fine("Removing banned mac:" + mac);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("delete from banned_macs where mac = ?");
			ps.setString(1, mac);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			s_logger.log(Level.SEVERE, "Error deleting banned mac:" + mac, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	/**
	 * Is the given mac banned? This may have the side effect of removing from the
	 * database any mac's whose ban has expired
	 */
	public boolean isMacBanned(final String mac)
	{
		boolean found = false;
		boolean expired = false;
		final String sql = "select mac, ban_till from banned_macs where mac = ?";
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, mac);
			final ResultSet rs = ps.executeQuery();
			found = rs.next();
			// If the ban has expired, allow the mac
			if (found)
			{
				final Timestamp banTill = rs.getTimestamp(2);
				if (banTill != null && banTill.getTime() < System.currentTimeMillis())
				{
					s_logger.fine("Ban expired for:" + mac);
					expired = true;
				}
			}
			rs.close();
			ps.close();
		} catch (final SQLException sqle)
		{
			s_logger.info("Error for testing banned mac existence:" + mac + " error:" + sqle.getMessage());
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
		if (expired)
		{
			removeBannedMac(mac);
			return false;
		}
		return found;
	}
}
