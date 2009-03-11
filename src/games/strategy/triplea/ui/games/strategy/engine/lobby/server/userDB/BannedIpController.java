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
import java.util.Date;
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
    
    /**
     * Ban the ip permanently
     */
    public void addBannedIp(String ip) 
    {
        addBannedIp(ip, null);
    }
    
    /**
     * Ban the given ip.  If banTill is not null, the ban will expire when banTill is reached.<p>
     * 
     * If this ip is already banned, this call will update the ban_end. 
     */
    public void addBannedIp(String ip, Date banTill) 
    {
        
        if(isIpBanned(ip)) 
        {
            removeBannedIp(ip);
        }
        
        Timestamp banTillTs = null;
        if(banTill != null) {
            banTillTs = new Timestamp(banTill.getTime());
        }
        
        s_logger.fine("Banning ip:" + ip);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("insert into banned_ips (ip, ban_till) values (?, ?)");
            ps.setString(1, ip);
            ps.setTimestamp(2, banTillTs);
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

    /**
     * Is the given ip banned?  This may have the side effect of removing from the
     * database any ip's whose ban has expired 
     */
    public boolean isIpBanned(String ip)
    {
        boolean found = false;
        boolean expired = false;
        String sql = "select ip, ban_till from banned_ips where ip = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, ip);
            
            ResultSet rs = ps.executeQuery();
            found = rs.next();
            
            //if the ban has expird, allow the ip
            if(found) 
            {
                Timestamp banTill = rs.getTimestamp(2);
                if( banTill != null && banTill.getTime() < System.currentTimeMillis()) 
                {
                    s_logger.fine("Ban expired for:" + ip);
                    expired = true;
                }
            }
                
            rs.close();
            ps.close();
            
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

        if(expired) 
        {
            removeBannedIp(ip);
            return false;
        }
        return found;
        
    }
    
   
}
