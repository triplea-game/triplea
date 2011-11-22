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

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBUserController
{
	private final static Logger s_logger = Logger.getLogger(DBUserController.class.getName());
	
	/**
	 * 
	 * @return if this user is valid
	 */
	public String validate(final String userName, final String email, final String hashedPassword)
	{
		if (email == null || !Util.isMailValid(email))
		{
			return "Invalid email address";
		}
		if (hashedPassword == null || hashedPassword.length() < 3 || !hashedPassword.startsWith(MD5Crypt.MAGIC))
		{
			return "Invalid password";
		}
		return validateUserName(userName);
	}
	
	public static String validateUserName(final String userName)
	{
		// is this a valid user?
		if (userName == null || !userName.matches("[0-9a-zA-Z_-]+") || userName.length() <= 2)
		{
			return "Usernames must be at least 3 characters long and can only contain alpha numeric characters, -, and _";
		}
		if (userName.contains(InGameLobbyWatcher.LOBBY_WATCHER_NAME))
		{
			return InGameLobbyWatcher.LOBBY_WATCHER_NAME + " cannot be part of a name";
		}
		if (userName.toLowerCase().indexOf("admin") >= 0)
		{
			return "Username can't contain the word admin";
		}
		return null;
	}
	
	public static void main(final String[] args) throws SQLException
	{
		Database.getConnection().close();
	}
	
	/**
	 * @return null if the user does not exist
	 */
	public String getPassword(final String userName)
	{
		final String sql = "select password from ta_users where username = ?";
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, userName);
			final ResultSet rs = ps.executeQuery();
			String rVal = null;
			if (rs.next())
			{
				rVal = rs.getString(1);
			}
			rs.close();
			ps.close();
			return rVal;
		} catch (final SQLException sqle)
		{
			s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	public boolean doesUserExist(final String userName)
	{
		final String sql = "select username from ta_users where username = ?";
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, userName);
			final ResultSet rs = ps.executeQuery();
			final boolean found = rs.next();
			rs.close();
			ps.close();
			return found;
		} catch (final SQLException sqle)
		{
			s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	public void updateUser(final String name, final String email, final String hashedPassword, final boolean admin)
	{
		final String validationErrors = validate(name, email, hashedPassword);
		if (validationErrors != null)
			throw new IllegalStateException(validationErrors);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("update ta_users set password = ?,  email = ?, admin = ? where username = ?");
			ps.setString(1, hashedPassword);
			ps.setString(2, email);
			ps.setBoolean(3, admin);
			ps.setString(4, name);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			s_logger.log(Level.SEVERE, "Error updating name:" + name + " email: " + email + " pwd: " + hashedPassword, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	/**
	 * Create a user in the database.
	 * If an error occured, an IllegalStateException will be thrown with a user displayable error message.
	 */
	public void createUser(final String name, final String email, final String hashedPassword, final boolean admin)
	{
		final String validationErrors = validate(name, email, hashedPassword);
		if (validationErrors != null)
			throw new IllegalStateException(validationErrors);
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement("insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)");
			ps.setString(1, name);
			ps.setString(2, hashedPassword);
			ps.setString(3, email);
			ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
			ps.setInt(6, admin ? 1 : 0);
			ps.execute();
			ps.close();
			con.commit();
		} catch (final SQLException sqle)
		{
			if (sqle.getErrorCode() == 30000)
			{
				s_logger.info("Tried to create duplicate user for name:" + name + " error:" + sqle.getMessage());
				throw new IllegalStateException("That user name is already taken");
			}
			s_logger.log(Level.SEVERE, "Error inserting name:" + name + " email: " + email + " pwd: " + hashedPassword, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	/**
	 * Validate the username password, returning true if the user is able to login.
	 * 
	 * This has the side effect of updating the users last login time.
	 */
	public boolean login(final String userName, final String hashedPassword)
	{
		final Connection con = Database.getConnection();
		try
		{
			PreparedStatement ps = con.prepareStatement("select username from  ta_users where username = ? and password = ?");
			ps.setString(1, userName);
			ps.setString(2, hashedPassword);
			final ResultSet rs = ps.executeQuery();
			if (!rs.next())
			{
				return false;
			}
			ps.close();
			rs.close();
			// update last login time
			ps = con.prepareStatement("update ta_users set lastLogin = ? where username = ? ");
			ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			ps.setString(2, userName);
			ps.execute();
			ps.close();
			return true;
		} catch (final SQLException sqle)
		{
			s_logger.log(Level.SEVERE, "Error validating password name:" + userName + " : " + " pwd:" + hashedPassword, sqle);
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
	
	/**
	 * 
	 * @return null if no such user
	 */
	public DBUser getUser(final String userName)
	{
		final String sql = "select * from ta_users where username = ?";
		final Connection con = Database.getConnection();
		try
		{
			final PreparedStatement ps = con.prepareStatement(sql);
			ps.setString(1, userName);
			final ResultSet rs = ps.executeQuery();
			if (!rs.next())
			{
				return null;
			}
			final DBUser user = new DBUser(rs.getString("username"), rs.getString("email"), rs.getBoolean("admin"), rs.getTimestamp("lastLogin"), rs.getTimestamp("joined"));
			rs.close();
			ps.close();
			return user;
		} catch (final SQLException sqle)
		{
			s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
			throw new IllegalStateException(sqle.getMessage());
		} finally
		{
			DbUtil.closeConnection(con);
		}
	}
}
