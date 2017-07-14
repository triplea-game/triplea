package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.userDB.DBUser;

// TODO: Lobby DB Migration - merge with DbUserController once completed
public class UserController implements UserDao {

  private static final Logger s_logger = Logger.getLogger(DbUserController.class.getName());

  private final Supplier<Connection> connectionSupplier;

  UserController(final Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
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
      return returnValue == null ? null : new HashedPassword(returnValue);
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
  public void updateUser(final DBUser user, final String password) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get()) {
      try (final PreparedStatement ps =
          con.prepareStatement("update ta_users set password = ?,  email = ?, admin = ? where username = ?")) {
        ps.setString(1, BCrypt.hashpw(password, BCrypt.gensalt()));
        ps.setString(2, user.getEmail());
        ps.setInt(3, user.isAdmin() ? 1 : 0);
        ps.setString(4, user.getName());
        ps.execute();
      }
      con.commit();
    } catch (final SQLException e) {
      s_logger.log(Level.SEVERE,
          "Error updating name:" + user.getName() + " email: " + user.getEmail() + " pwd: <hidden>", e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void createUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkState(user.isValid(), user.getValidationErrorMessage());
    Preconditions.checkState(hashedPassword.isValidSyntax());

    final Connection con = connectionSupplier.get();
    try {
      final PreparedStatement ps = con.prepareStatement(
          "insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)");
      ps.setString(1, user.getName());
      ps.setString(2, hashedPassword.value);
      ps.setString(3, user.getEmail());
      ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
      ps.setInt(6, user.isAdmin() ? 1 : 0);
      ps.execute();
      ps.close();
      con.commit();
    } catch (final SQLException sqle) {
      s_logger.log(
          Level.SEVERE,
          String.format("Error inserting name: %s, email: %s, (masked) pwd: %s",
              user.getName(), user.getEmail(), hashedPassword.value.replaceAll(".", "*")),
          sqle);
      throw new RuntimeException(sqle);
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public void createUser(final DBUser user, final String password) {
    Preconditions.checkState(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get()) {
      try (final PreparedStatement ps = con.prepareStatement(
          "insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)")) {
        ps.setString(1, user.getName());
        ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
        ps.setString(3, user.getEmail());
        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
        ps.setInt(6, user.isAdmin() ? 1 : 0);
        ps.execute();
      }
      con.commit();
    } catch (final SQLException e) {
      s_logger.log(Level.SEVERE, String.format("Error inserting name: %s, email: %s, (masked) pwd: %s", user.getName(),
          user.getEmail(), password), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean login(final String userName, final HashedPassword hashedPassword) {
    Preconditions.checkState(hashedPassword.isValidSyntax());

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
  public boolean login(String userName, String password) {
    HashedPassword hashedPassword = getPassword(userName);
    if (hashedPassword == null) {
      return false;
    }
    Preconditions.checkState(hashedPassword.isBcrypted());
    final boolean correctCredentials = BCrypt.checkpw(password, hashedPassword.value);
    if (correctCredentials) {
      try (final Connection connection = connectionSupplier.get()) {
        try (final PreparedStatement statement =
            connection.prepareStatement("update ta_users set lastLogin = ? where username = ?")) {
          statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
          statement.setString(2, userName);
          statement.execute();
        }
        connection.commit();
      } catch (SQLException e) {
        s_logger.log(Level.SEVERE, "Error validating password name:" + userName + " : " + " pwd:" + hashedPassword);
        throw new IllegalStateException(e);
      }
    }
    return correctCredentials;
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
