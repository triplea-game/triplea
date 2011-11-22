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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete muted ips (there is no update).
 * 
 * @author sgb
 */
public class MutedIpController
{
	private static final Logger s_logger = Logger.getLogger(MutedIpController.class.getName());
	
	/**
	 * Mute the ip permanently
	 */
	public void addMutedIp(final String ip)
	{
		addMutedIp(ip, null);
	}
	
	/**
	 * Mute the given ip. If muteTill is not null, the mute will expire when muteTill is reached.
	 * <p>
	 * 
	 * If this ip is already muted, this call will update the mute_end.
	 */
	public void addMutedIp(final String ip, final Date muteTill)
	{
		if (isIpMuted(ip))
			removeMutedIp(ip);
		Timestamp muteTillTs = null;
		if (muteTill != null)
		{
			muteTillTs = new Timestamp(muteTill.getTime());
		}
		s_logger.fine("Muting ip:" + ip);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("insert into muted_ips (ip, mute_till) values (?, ?)");
			ps.setString(1, ip);
			ps.setTimestamp(2, muteTillTs);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			if (sqle.getErrorCode() == 30000)
			{
				// this is ok
				// the ip is muted as expected
				s_logger.info("Tried to create duplicate muted ip:" + ip + " error:" + sqle.getMessage());
				return;
			}
			s_logger.log(Level.SEVERE, "Error inserting muted ip:" + ip, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	public void removeMutedIp(final String ip)
	{
		s_logger.fine("Removing muted ip:" + ip);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("delete from muted_ips where ip = ?");
			ps.setString(1, ip);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			s_logger.log(Level.SEVERE, "Error deleting muted ip:" + ip, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	/**
	 * Is the given ip muted? This may have the side effect of removing from the
	 * database any ip's whose mute has expired
	 */
	public boolean isIpMuted(final String ip)
	{
		final long muteTill = getIpUnmuteTime(ip);
		return muteTill > System.currentTimeMillis();
	}
	
	public long getIpUnmuteTime(final String ip)
	{
		long result = -1;
		boolean expired = false;
		final String sql = "select ip, mute_till from muted_ips where ip = ?";
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, ip);
			final ResultSet rs = ps.executeQuery();
			final boolean found = rs.next();
			if (found)
			{
				final Timestamp muteTill = rs.getTimestamp(2);
				result = muteTill.getTime();
				if (result < System.currentTimeMillis())
				{
					s_logger.fine("Mute expired for:" + ip);
					expired = true;
				}
			}
			else
				result = -1;
			rs.close();
			ps.close();
		} catch (final SQLException sqle)
		{
			s_logger.info("Error for testing muted ip existence:" + ip + " error:" + sqle.getMessage());
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
		// If the mute has expired, allow the ip
		if (expired)
		{
			removeMutedIp(ip);
			result = -1; // Signal as not-muted
		}
		return result;
	}
	
	public List<String> getIPsThatAreStillMuted(final List<String> ips)
	{
		final List<String> results = new ArrayList<String>();
		final String sql = "select ip, mute_till from muted_ips where ip = ?";
		final Connection con = Database.getConnection();
		try
		{
			for (final String ip : ips)
			{
				boolean found = false;
				boolean expired = false;
				final PreparedStatement ps = con.prepareStatement(sql);
				ps.setString(1, ip);
				final ResultSet rs = ps.executeQuery();
				found = rs.next();
				// If the mute has expired, allow the ip
				if (found)
				{
					final Timestamp muteTill = rs.getTimestamp(2);
					if (muteTill != null && muteTill.getTime() < System.currentTimeMillis())
					{
						s_logger.fine("Mute expired for: " + ip);
						expired = true;
					}
				}
				rs.close();
				ps.close();
				if (found && !expired)
					results.add(ip);
			}
		} catch (final SQLException sqle)
		{
			s_logger.info("Error testing whether ips were muted: " + ips);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
		return results;
	}
}
