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
 * Utilitiy class to create/read/delete muted usernames (there is no update).
 *
 * @author Wisconsin
 */
public class MutedUsernameController
{
    private static final Logger s_logger = Logger.getLogger(MutedUsernameController.class.getName());

    /**
     * Mute the username permanently
     */
    public void addMutedUsername(String username)
    {
        addMutedUsername(username, null);
    }

    /**
     * Mute the given username.  If muteTill is not null, the mute will expire when muteTill is reached.<p>
     *
     * If this username is already muted, this call will update the mute_end.
     */
    public void addMutedUsername(String username, Date muteTill)
    {
        if(isUsernameMuted(username))
            removeMutedUsername(username);

        Timestamp muteTillTs = null;
        if(muteTill != null) {
            muteTillTs = new Timestamp(muteTill.getTime());
        }

        s_logger.fine("Muting username:" + username);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("insert into muted_usernames (username, mute_till) values (?, ?)");
            ps.setString(1, username);
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
                //the username is muted as expected
                s_logger.info("Tried to create duplicate muted username:" + username + " error:" + sqle.getMessage());
                return;
            }

            s_logger.log(Level.SEVERE, "Error inserting muted username:" + username, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }

    public void removeMutedUsername(String username)
    {
        s_logger.fine("Removing muted username:" + username);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("delete from muted_usernames where username = ?");
            ps.setString(1, username);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            s_logger.log(Level.SEVERE, "Error deleting muted username:" + username, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }

    /**
     * Is the given username muted?  This may have the side effect of removing from the
     * database any username's whose mute has expired
     */
    public boolean isUsernameMuted(String username)
    {
        long muteTill = getUsernameUnmuteTime(username);
        return muteTill > System.currentTimeMillis();
    }
    public long getUsernameUnmuteTime(String username)
    {
        long result = -1;
        boolean expired = false;
        String sql = "select username, mute_till from muted_usernames where username = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();

            if(found)
            {
                Timestamp muteTill = rs.getTimestamp(2);
                result = muteTill.getTime();
                if(result < System.currentTimeMillis())
                {
                    s_logger.fine("Mute expired for:" + username);
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
            s_logger.info("Error for testing muted username existence:" + username + " error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }

        //If the mute has expired, allow the username
        if(expired)
        {
            removeMutedUsername(username);
            result = -1; //Signal as not-muted
        }
        return result;
    }

    public List<String> getUsernamesThatAreStillMuted(List<String> usernames)
    {
        List<String> results = new ArrayList<String>();

        String sql = "select username, mute_till from muted_usernames where username = ?";
        Connection con = Database.getConnection();
        try
        {
            for (String username : usernames)
            {
                boolean found = false;
                boolean expired = false;

                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                found = rs.next();

                //If the mute has expired, allow the username
                if (found)
                {
                    Timestamp muteTill = rs.getTimestamp(2);
                    if (muteTill != null && muteTill.getTime() < System.currentTimeMillis())
                    {
                        s_logger.fine("Mute expired for: " + username);
                        expired = true;
                    }
                }

                rs.close();
                ps.close();

                if(found && !expired)
                    results.add(username);
            }
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error testing whether usernames were muted: " + usernames);
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }

        return results;
    }
}
