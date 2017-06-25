package games.strategy.engine.lobby.server.userDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;

// TODO: *Controller are really *Dao classes, this is a DAO pattern, not controller!
public class UserController implements UserDaoPrimarySecondary {

  private static final Logger s_logger = Logger.getLogger(DbUserController.class.getName());

  private final Supplier<Connection> connectionSupplier;
  private final UserDaoPrimarySecondary.Role usageRole;

  UserController(final UserDaoPrimarySecondary.Role usageRole, final Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
    this.usageRole = usageRole;
  }

  @Override
  public boolean isPrimary() {
    return usageRole == Role.PRIMARY;
  }

  /**
   * @return null if the user does not exist.
   */
  @Override
  public HashedPassword getPassword(final String userName) {
    final String sql = "select password from ta_users where username = ?";
    final Connection con = connectionSupplier.get();
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
      return new HashedPassword(returnValue);
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
    final Connection con = connectionSupplier.get();
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
  public void updateUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    final Connection con = connectionSupplier.get();
    try {
      final PreparedStatement ps =
          con.prepareStatement("update ta_users set password = ?,  email = ?, admin = ? where username = ?");
      ps.setString(1, hashedPassword.value);
      ps.setString(2, user.getEmail());
      ps.setInt(3, user.isAdmin() ? 1 : 0);
      ps.setString(4, user.getName());
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      s_logger.log(Level.SEVERE,
          "Error updating name:" + user.getName() + " email: " + user.getEmail() + " pwd: " + hashedPassword.value,
          sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public void createUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkState(user.isValid(), user.getValidationErrorMessage());
    if (doesUserExist(user.getName())) {
      throw new IllegalStateException("That user name has already been taken");
    }
    final Connection con = connectionSupplier.get();
    try {
      final PreparedStatement ps = con.prepareStatement(
          "insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)");
      ps.setString(1, user.getName());
      ps.setString(2, hashedPassword.value);
      ps.setString(3, user.getEmail());
      ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      ps.setTimestamp(5, new java.sql.Timestamp(System.currentTimeMillis()));
      ps.setInt(6, user.isAdmin() ? 1 : 0);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      // TODO: let's instead first check if the user exists, if we do that then we can just
      // treat this as a normal error.
      if (sqle.getErrorCode() == 30000) {
        s_logger.info("Tried to create duplicate user for name:" + user.getName() + " error:" + sqle.getMessage());
        throw new IllegalStateException("That user name is already taken");
      }
      s_logger.log(
          Level.SEVERE,
          String.format("Error inserting name: %s, email: %s, (masked) pwd: %s",
              user.getName(), user.getEmail(), hashedPassword.value.replaceAll(".", "*")),
          sqle);
      throw new IllegalStateException(sqle);
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public boolean login(final String userName, final HashedPassword hashedPassword) {
    final Connection con = connectionSupplier.get();
    try {
      PreparedStatement ps = con.prepareStatement("select username from  ta_users where username = ? and password = ?");
      ps.setString(1, userName);
      ps.setString(2, hashedPassword.value);
      final ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        return false;
      }
      ps.close();
      rs.close();
      // update last login time
      ps = con.prepareStatement("update ta_users set lastLogin = ? where username = ?");
      ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      ps.setString(2, userName);
      ps.execute();
      ps.close();
      con.commit();
      return true;
    } catch (final SQLException sqle) {
      s_logger.log(Level.SEVERE, "Error validating password name:" + userName + " : " + " pwd:" + hashedPassword, sqle);
      throw new IllegalStateException(sqle.getMessage());
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public DBUser getUserByName(final String userName) {
    final String sql = "select * from ta_users where username = ?";
    final Connection con = connectionSupplier.get();
    try {
      final PreparedStatement ps = con.prepareStatement(sql);
      ps.setString(1, userName);
      final ResultSet rs = ps.executeQuery();
      if (!rs.next()) {
        return null;
      }
      final DBUser user = new DBUser(
          new DBUser.UserName(rs.getString("username")),
          new DBUser.UserEmail(rs.getString("email")),
          rs.getBoolean("admin") ? DBUser.Role.ADMIN : DBUser.Role.NOT_ADMIN);
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
