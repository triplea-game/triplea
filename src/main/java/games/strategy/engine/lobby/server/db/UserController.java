package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Supplier;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.userDB.DBUser;

// TODO: Lobby DB Migration - merge with DbUserController once completed
public class UserController implements UserDao {
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
      throw new IllegalStateException("Error getting password for user:" + userName, sqle);
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
      throw new IllegalStateException("Error testing for existence of user:" + userName, sqle);
    } finally {
      DbUtil.closeConnection(con);
    }
  }

  @Override
  public void updateUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get()) {
      try (final PreparedStatement ps =
          con.prepareStatement("update ta_users set password = ?,  email = ?, admin = ? where username = ?")) {
        ps.setString(1, hashedPassword.value);
        ps.setString(2, user.getEmail());
        ps.setInt(3, user.isAdmin() ? 1 : 0);
        ps.setString(4, user.getName());
        ps.execute();
      }
      con.commit();
    } catch (final SQLException e) {
      throw new IllegalStateException(
          "Error updating name:" + user.getName() + " email: " + user.getEmail() + " pwd: " + hashedPassword.mask(),
          e);
    }
  }

  @Override
  public void createUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkState(hashedPassword.isValidSyntax());
    Preconditions.checkState(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get()) {
      try (final PreparedStatement ps = con.prepareStatement(
          "insert into ta_users (username, password, email, joined, lastLogin, admin) values (?, ?, ?, ?, ?, ?)")) {
        ps.setString(1, user.getName());
        ps.setString(2, hashedPassword.value);
        ps.setString(3, user.getEmail());
        ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
        ps.setInt(6, user.isAdmin() ? 1 : 0);
        ps.execute();
      }
      con.commit();
    } catch (final SQLException e) {
      throw new RuntimeException(
          String.format(
              "Error inserting name: %s, email: %s, (masked) pwd: %s",
              user.getName(), user.getEmail(), hashedPassword.mask()),
          e);
    }
  }

  @Override
  public boolean login(final String userName, final HashedPassword hashedPassword) {
    try (final Connection con = connectionSupplier.get()) {
      if (hashedPassword.isValidSyntax()) {
        try (final PreparedStatement ps =
            con.prepareStatement("select username from  ta_users where username = ? and password = ?")) {
          ps.setString(1, userName);
          ps.setString(2, hashedPassword.value);
          try (final ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
              return false;
            }
          }
        }
      } else {
        HashedPassword actualPassword = getPassword(userName);
        if (actualPassword == null) {
          return false;
        }
        Preconditions.checkState(actualPassword.isBcrypted());
        if (!BCrypt.checkpw(hashedPassword.value, actualPassword.value)) {
          return false;
        }
      }
      // update last login time
      try (
          final PreparedStatement ps = con.prepareStatement("update ta_users set lastLogin = ? where username = ?")) {
        ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
        ps.setString(2, userName);
        ps.execute();
        con.commit();
        return true;
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException(
          "Error validating password name:" + userName + " : " + " pwd:" + hashedPassword.mask(),
          sqle);
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
      throw new IllegalStateException("Error getting user:" + userName, sqle);
    } finally {
      DbUtil.closeConnection(con);
    }
  }
}
