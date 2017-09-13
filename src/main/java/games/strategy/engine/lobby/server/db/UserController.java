package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Supplier;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.userDB.DBUser;

public class UserController implements UserDao {
  private final Supplier<Connection> connectionSupplier;


  public UserController() {
    this(Database::getPostgresConnection);
  }

  UserController(final Supplier<Connection> connectionSupplier) {
    this.connectionSupplier = connectionSupplier;
  }

  /**
   * @return null if the user does not exist.
   */
  @Override
  public HashedPassword getPassword(final String userName) {
    final String sql = "select password from ta_users where username = ?";
    try (final Connection con = connectionSupplier.get(); final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, userName);
      try (final ResultSet rs = ps.executeQuery()) {
        String returnValue = null;
        if (rs.next()) {
          returnValue = rs.getString(1);
        }
        return returnValue == null ? null : new HashedPassword(returnValue);
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error getting password for user:" + userName, sqle);
    }
  }

  @Override
  public boolean doesUserExist(final String userName) {
    final String sql = "select username from ta_users where upper(username) = upper(?)";
    try (final Connection con = connectionSupplier.get(); final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, userName);
      try (final ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error testing for existence of user:" + userName, sqle);
    }
  }

  @Override
  public void updateUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get();
        final PreparedStatement ps =
            con.prepareStatement("update ta_users set password = ?,  email = ? where username = ?")) {
      ps.setString(1, hashedPassword.value);
      ps.setString(2, user.getEmail());
      ps.setString(3, user.getName());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new IllegalStateException(String.format("Error updating name: %s email: %s pwd: %s",
          user.getName(), user.getEmail(), hashedPassword.mask()), e);
    }
  }

  public void makeAdmin(final DBUser user) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get();
        final PreparedStatement ps = con.prepareStatement("update ta_users set admin=? where username = ?")) {
      ps.setBoolean(1, user.isAdmin());
      ps.setString(2, user.getName());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new IllegalStateException(String.format("Error while trying to make %s an admin", user.getName()), e);
    }
  }

  @Override
  public void createUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkState(hashedPassword.isValidSyntax());
    Preconditions.checkState(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get();
        final PreparedStatement ps =
            con.prepareStatement("insert into ta_users (username, password, email) values (?, ?, ?)")) {
      ps.setString(1, user.getName());
      ps.setString(2, hashedPassword.value);
      ps.setString(3, user.getEmail());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new RuntimeException(String.format("Error inserting name: %s, email: %s, (masked) pwd: %s",
          user.getName(), user.getEmail(), hashedPassword.mask()), e);
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
        final HashedPassword actualPassword = getPassword(userName);
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
        ps.setTimestamp(1, Timestamp.from(Instant.now()));
        ps.setString(2, userName);
        ps.execute();
        con.commit();
        return true;
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException(
          "Error validating password name:" + userName + " : " + " pwd:" + hashedPassword.mask(), sqle);
    }
  }

  @Override
  public DBUser getUserByName(final String userName) {
    final String sql = "select * from ta_users where username = ?";
    try (final Connection con = connectionSupplier.get(); final PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, userName);
      try (final ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return new DBUser(
            new DBUser.UserName(rs.getString("username")),
            new DBUser.UserEmail(rs.getString("email")),
            rs.getBoolean("admin") ? DBUser.Role.ADMIN : DBUser.Role.NOT_ADMIN);
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error getting user:" + userName, sqle);
    }
  }
}
