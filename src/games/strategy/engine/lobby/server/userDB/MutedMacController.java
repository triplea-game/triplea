/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
 * Utilitiy class to create/read/delete muted macs (there is no update).
 *
 * @author Wisconsin
 */
public class MutedMacController
{

    private static final Logger s_logger = Logger.getLogger(MutedMacController.class.getName());

    /**
     * Mute the mac permanently
     */
    public void addMutedMac(String mac)
    {
        addMutedMac(mac, null);
    }

    /**
     * Mute the given mac.  If muteTill is not null, the mute will expire when muteTill is reached.<p>
     *
     * If this mac is already muted, this call will update the mute_end.
     */
    public void addMutedMac(String mac, Date muteTill)
    {
        if(isMacMuted(mac))
        {
            removeMutedMac(mac);
        }

        Timestamp muteTillTs = null;
        if(muteTill != null) {
            muteTillTs = new Timestamp(muteTill.getTime());
        }

        s_logger.fine("Muting mac:" + mac);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("insert into muted_macs (mac, mute_till) values (?, ?)");
            ps.setString(1, mac);
            ps.setTimestamp(2, muteTillTs);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            if(sqle.getErrorCode() == 30000)
            {
                //this is ok
                //the mac is muted as expected
                s_logger.info("Tried to create duplicate muted mac:" + mac + " error:" + sqle.getMessage());
                return;
            }

            s_logger.log(Level.SEVERE, "Error inserting muted mac:" + mac, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }

    public void removeMutedMac(String mac)
    {
        s_logger.fine("Removing muted mac:" + mac);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("delete from muted_macs where mac = ?");
            ps.setString(1, mac);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            s_logger.log(Level.SEVERE, "Error deleting muted mac:" + mac, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }

    /**
     * Is the given mac muted?  This may have the side effect of removing from the
     * database any mac's whose mute has expired
     */
    public boolean isMacMuted(String mac)
    {
        long muteTill = getMacUnmuteTime(mac);
        return muteTill <= System.currentTimeMillis();
    }
    public long getMacUnmuteTime(String mac)
    {
        long result = -1;
        boolean expired = false;
        String sql = "select mac, mute_till from muted_macs where mac = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, mac);
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();

            if(found)
            {
                Timestamp muteTill = rs.getTimestamp(2);
                result = muteTill.getTime();
                if(result < System.currentTimeMillis())
                {
                    s_logger.fine("Mute expired for:" + mac);
                    expired = true;
                }
            }
            else
                result = -1;

            rs.close();
            ps.close();
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error for testing muted mac existence:" + mac + " error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }

        //If the mute has expired, allow the mac
        if(expired)
        {
            removeMutedMac(mac);
            result = -1; //Signal as not-muted
        }
        return result;
    }

    public List<String> getMacsThatAreStillMuted(List<String> macs)
    {
        List<String> results = new ArrayList<String>();

        String sql = "select mac, mute_till from muted_macs where mac = ?";
        Connection con = Database.getConnection();
        try
        {
            for (String mac : macs)
            {
                boolean found = false;
                boolean expired = false;

                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, mac);
                ResultSet rs = ps.executeQuery();
                found = rs.next();

                //If the mute has expired, allow the mac
                if (found)
                {
                    Timestamp muteTill = rs.getTimestamp(2);
                    if (muteTill != null && muteTill.getTime() < System.currentTimeMillis())
                    {
                        s_logger.fine("Mute expired for: " + mac);
                        expired = true;
                    }
                }

                rs.close();
                ps.close();

                if(found && !expired)
                    results.add(mac);
            }
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error testing whether macs were muted: " + macs);
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }

        return results;
    }
}
