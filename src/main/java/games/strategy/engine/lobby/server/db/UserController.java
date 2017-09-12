package games.strategy.engine.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Supplier;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.annotations.VisibleForTesting;
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
  public HashedPassword getPassword(final String username) {
    return getPasswordFromColumn(username, "coalesce(password, bcrypt_password)");
  }

  /**
   * @return null if the user does not exist or the user has no legacy password stored.
   */
  @Override
  public HashedPassword getMd5Password(final String username) {
    return getPasswordFromColumn(username, "password");
  }

  private HashedPassword getPasswordFromColumn(final String username, final String column) {
    try (final Connection con = connectionSupplier.get();
        final PreparedStatement ps = con.prepareStatement("select " + column + " from ta_users where username=?")) {
      ps.setString(1, username);
      try (final ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new HashedPassword(rs.getString(1));
        }
        return null;
      }
    } catch (final SQLException sqle) {
      throw new IllegalStateException("Error getting password for user: " + username, sqle);
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
    updateUser(user, hashedPassword, hashedPassword.isBcrypted() ? "bcrypt_password" : "password");
  }

  private void updateUser(final DBUser user, final HashedPassword hashedPassword, final String column) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    try (final Connection con = connectionSupplier.get();
        final PreparedStatement ps =
            con.prepareStatement("update ta_users set " + column + " = ?,  email = ? where username = ?")) {
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

  /**
   * A method similar to update user, used by tests only.
   * Does only affect the admin state of the given user.
   * The DB is updated with user.isAdmin()
   */
  @VisibleForTesting
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
    createUser(user, hashedPassword, hashedPassword.isBcrypted() ? "bcrypt_password" : "password");
  }

  private void createUser(final DBUser user, final HashedPassword hashedPassword, final String passwordColumn) {
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
