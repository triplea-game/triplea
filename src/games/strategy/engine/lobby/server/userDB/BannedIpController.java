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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete banned ips (there is no update).
 * 
 * @author sgb
 */
public class BannedIpController
{

    private static final Logger s_logger = Logger.getLogger(BannedIpController.class.getName());
    
    public void addBannedIp(String ip) 
    {
        s_logger.fine("Banning ip:" + ip);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("insert into banned_ips (ip) values (?)");
            ps.setString(1, ip);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            if(sqle.getErrorCode() == 30000)
            {
                //this is ok
                //the ip is banned as expected
                s_logger.info("Tried to create duplicate banned ip:" + ip + " error:" + sqle.getMessage());
                return;
            }
            
            s_logger.log(Level.SEVERE, "Error inserting banned ip:" + ip, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }
    
    public void removeBannedIp(String ip) 
    {
        s_logger.fine("Removing banned ip:" + ip);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("delete from banned_ips where ip = ?");
            ps.setString(1, ip);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            s_logger.log(Level.SEVERE, "Error deleting banned ip:" + ip, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }

    public boolean isIpBanned(String ip)
    {
        String sql = "select ip from banned_ips where ip = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, ip);
            
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();
            
            rs.close();
            ps.close();
            
            return found;
            
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error for testing banned ip existence:" + ip + " error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());

        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }
    
   
}
