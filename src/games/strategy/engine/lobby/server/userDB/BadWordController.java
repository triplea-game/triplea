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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitiy class to create/read/delete bad words (there is no update).
 * 
 * @author sgb
 */
public class BadWordController
{

    private static final Logger s_logger = Logger.getLogger(BadWordController.class.getName());
    
    public void addBadWord(String word) 
    {
        
        s_logger.fine("Adding bad word word:" + word);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("insert into bad_words (word) values (?)");
            ps.setString(1, word);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            if(sqle.getErrorCode() == 30000)
            {
                //this is ok
                //the word is bad as expected
                s_logger.info("Tried to create duplicate banned word:" + word + " error:" + sqle.getMessage());
                return;
            }
            
            s_logger.log(Level.SEVERE, "Error inserting banned word:" + word, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }
    
    public void removeBannedWord(String word) 
    {

        s_logger.fine("Removing banned word:" + word);
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("delete from bad_words where word = ?");
            ps.setString(1, word);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            s_logger.log(Level.SEVERE, "Error deleting banned word:" + word, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }
    
    public List<String> list() 
    {
        String sql = "select word from bad_words";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            
            
            ResultSet rs = ps.executeQuery();
            
            List<String> rVal = new ArrayList<String>();
            while(rs.next()) 
            {
                rVal.add(rs.getString(1));
            }
            
            rs.close();
            ps.close();
            
            return rVal;
            
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error reading bad words error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());

        }
        finally
        {
            DbUtil.closeConnection(con);
        }
    }
    
   
}
