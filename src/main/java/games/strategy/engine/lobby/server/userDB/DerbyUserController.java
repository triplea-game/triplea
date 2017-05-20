package games.strategy.engine.lobby.server.userDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: legacy, migrate away, can be removed when no longer needs to be primary.
 * @deprecated Avoid using this class, we are trying to move away from Derby DB to a different datasource.
 */
@Deprecated
public class DerbyUserController implements UserDaoPrimarySecondary {

  private static final Logger s_logger = Logger.getLogger(DbUserController.class.getName());

  @Override
  public boolean isPrimary() {
    return true;
  }

  /**
   * @return null if the user does not exist.
   */
  @Override
  public String getPassword(final String userName) {
    final String sql = "select password from ta_users where username = ?";
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, userName);
      final ResultSet rs = ps.executeQuery();
      String returnValue = null;
      if (rs.next()) {
        returnValue = rs.getString(1);
      }
      rs.close();
      ps.close();
      return returnValue;
    } catch (final SQLException sqle) {
      s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public boolean doesUserExist(final String userName) {
    final String sql = "select username from ta_users where upper(username) = upper(?)";
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, userName);
      final ResultSet rs = ps.executeQuery();
      final boolean found = rs.next();
      rs.close();
      ps.close();
      return found;
    } catch (final SQLException sqle) {
      s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public void updateUser(final String name, final String email, final String hashedPassword, final boolean admin) {
    final String validationErrors = UserDao.validate(name, email, hashedPassword);
    if (validationErrors != null) {
      throw new IllegalStateException(validationErrors);
    }
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps =
          con.prepareStatement("update ta_users set password = ?,  email = ?, admin = ? where username = ?");
      ps.setString(1, hashedPassword);
      ps.setString(2, email);
      ps.setBoolean(3, admin);
      ps.setString(4, name);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      s_logger.log(Level.SEVERE, "Error updating name:" + name + " email: " + email + " pwd: " + hashedPassword, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public void createUser(final String name, final String email, final String hashedPassword, final boolean admin) {
    final String validationErrors = UserDao.validate(name, email, hashedPassword);
    if (validationErrors != null) {
      throw new IllegalStateException(validationErrors);
    }
    if (doesUserExist(name)) {
      throw new IllegalStateException("That user name has already been taken");
    }
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement(
          "insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)");
      ps.setString(1, name);
      ps.setString(2, hashedPassword);
      ps.setString(3, email);
      ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
      ps.setInt(6, admin ? 1 : 0);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      if (sqle.getErrorCode() == 30000) {
        s_logger.info("Tried to create duplicate user for name:" + name + " error:" + sqle.getMessage());
        throw new IllegalStateException("That user name is already taken");
      }
      s_logger.log(Level.SEVERE, "Error inserting name:" + name + " email: " + email + " pwd: " + hashedPassword, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public boolean login(final String userName, final String hashedPassword) {
    final Connection con = Database.getConnection();
    try {
      PreparedStatement ps = con.prepareStatement("select username from  ta_users where username = ? and password = ?");
      ps.setString(1, userName);
      ps.setString(2, hashedPassword);
      final ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
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
    } catch (final SQLException sqle) {
      s_logger.log(Level.SEVERE, "Error validating password name:" + userName + " : " + " pwd:" + hashedPassword, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public DbUser getUser(final String userName) {
    final String sql = "select * from ta_users where username = ?";
    final Connection con = Database.getConnection();
    try {
      final PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, userName);
      final ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        return null;
      }
      final DbUser user = new DbUser(rs.getString("username"), rs.getString("email"), rs.getBoolean("admin"),
          rs.getTimestamp("lastLogin"), rs.getTimestamp("joined"));
      rs.close();
      ps.close();
      return user;
    } catch (final SQLException sqle) {
      s_logger.info("Error for testing user existence:" + userName + " error:" + sqle.getMessage());
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }
}
