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


import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.util.*;

import java.sql.*;
import java.util.logging.*;

public class DBUserController
{
    private final static Logger s_logger = Logger.getLogger(DBUserController.class.getName());

    /**
     * 
     * @return if this user is valid
     */
    public String validate(String userName, String email, String hashedPassword )
    {
        if(email == null || !Util.isMailValid(email) )
        {
            return "Invalid email address";
        }
        if(hashedPassword == null || hashedPassword.length() < 3 || !hashedPassword.startsWith(MD5Crypt.MAGIC) )
        {
            return "Invalid password";
        }
        
        return validateUserName(userName);
    }


    public static String validateUserName(String userName)
    {
        //is this a valid user?
        if(userName == null || !userName.matches("[0-9a-zA-Z_-]+") || userName.length() <= 2)
        {
            return "Invalid userName, usernames must be at least 3 characters long, and contain alpha numeric characters and _ or -"; 
        }
        
        if(userName.equals(InGameLobbyWatcher.LOBBY_WATCHER_NAME))
        {
            return InGameLobbyWatcher.LOBBY_WATCHER_NAME + " is a reserved name";
        }
        
        if(userName.toLowerCase().indexOf("admin") >= 0)
        {
            return "Username can't contain the word admin";
        }
        
        return null;
    }
     
    
    public static void main(String[] args) throws SQLException
    {
        Database.getConnection().close();
    }
    
    /**
     * @return null if the user does not exist
     */
    public String getPassword(String userName)
    {
        String sql = "select password from ta_users where username = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userName);
            
            ResultSet rs = ps.executeQuery();
            String rVal = null;
            if(rs.next())
            {
                rVal = rs.getString(1);
            }
            
            rs.close();
            ps.close();
            
            return rVal;
            
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());

        }
        finally
        {
            closeConnection(con);
        }
    }
    
    public boolean doesUserExist(String userName)
    {
        String sql = "select username from ta_users where username = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userName);
            
            ResultSet rs = ps.executeQuery();
            boolean found = rs.next();
            
            rs.close();
            ps.close();
            
            return found;
            
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());

        }
        finally
        {
            closeConnection(con);
        }
    }

    
    public void updateUser(String name, String email, String hashedPassword, boolean admin)
    {
        String validationErrors = validate(name, email, hashedPassword); 
        if(validationErrors != null)
            throw new IllegalStateException(validationErrors);
        
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("update ta_users set password = ?,  email = ?, admin = ? where username = ?");
            
            ps.setString(1, hashedPassword);
            ps.setString(2, email);
            ps.setBoolean(3, admin);
            ps.setString(4, name);
            
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            s_logger.log(Level.SEVERE, "Error updating name:" + name + " email: " + email + " pwd: " + hashedPassword, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            closeConnection(con);
        }
    }
    

    /**
     * Create a user in the database. 
     * If an error occured, an IllegalStateException will be thrown with a user displayable error message.
     */
    public void createUser(String name, String email, String hashedPassword, boolean admin)
    {
        String validationErrors = validate(name, email, hashedPassword); 
        if(validationErrors != null)
            throw new IllegalStateException(validationErrors);
        
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)");
            ps.setString(1, name);
            ps.setString(2, hashedPassword);
            ps.setString(3, email);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(6, admin ? 1 : 0);
            ps.execute();
            ps.close();
            con.commit();
        }
        catch(SQLException sqle)
        {
            if(sqle.getErrorCode() == 30000)
            {
                s_logger.info("Tried to create duplicate user for name:" + name + " error:" + sqle.getMessage());
                throw new IllegalStateException("That user name is already taken");
            }
            
            s_logger.log(Level.SEVERE, "Error inserting name:" + name + " email: " + email + " pwd: " + hashedPassword, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            closeConnection(con);
        }
        
    }


    private void closeConnection(Connection con)
    {
        try
        {
            con.close();
        } catch (SQLException e)
        {
            s_logger.log(Level.WARNING, "Error closing connection",e);
        }
    }
    
    /**
     * Validate the username password, returning true if the user is able to login.
     * 
     * This has the side effect of updating the users last login time.
     */
    public boolean login(String userName, String hashedPassword)
    {
        
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement("select username from  ta_users where username = ? and password = ?");
            ps.setString(1, userName);
            ps.setString(2, hashedPassword);
            ResultSet rs = ps.executeQuery();
            if(!rs.next())
            {
                return false;
            }
            
            ps.close();
            rs.close();
            
            //update last login time
            ps = con.prepareStatement( "update ta_users set lastLogin = ? where username = ? " );
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2, userName);
            ps.execute();
            ps.close();
            return true;
            
        }
        catch(SQLException sqle)
        {
            s_logger.log(Level.SEVERE, "Error validating password name:" + userName + " : " +  " pwd:" + hashedPassword, sqle );
            throw new IllegalStateException(sqle.getMessage());
        }
        finally
        {
            closeConnection(con);
        }
        
        
    }
    
    /**
     * 
     * @return null if no such user
     */
    public DBUser getUser(String userName)
    {
        String sql = "select * from ta_users where username = ?";
        Connection con = Database.getConnection();
        try
        {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userName);
            
            ResultSet rs = ps.executeQuery();
            
            if(!rs.next())
            {
                return null;
            }
            
            DBUser user = new DBUser(
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getBoolean("admin"),
                    rs.getTimestamp("lastLogin"),
                    rs.getTimestamp("joined")            
            );
            
            rs.close();
            ps.close();
            
            return user;
        }
        catch(SQLException sqle)
        {
            s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
            throw new IllegalStateException(sqle.getMessage());

        }
        finally
        {
            closeConnection(con);
        }
    }
    
    

    //we need to reset the password, and send an email to the user account.
    //perhaps we can do this manually for now
//    public void resetPassword(String userName, String email)
//    {
//        Connection con = Database.getConnection();
//        try
//        {
//            PreparedStatement ps = con.prepareStatement();
//            
//        }
//        finally
//        {
//            closeConnection(con);
//        }
//    }

    

//    login( // make sure to update last login time);
    
    
//    updateEmailAddress();
    
    
    
}
