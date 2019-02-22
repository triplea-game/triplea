package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.mindrot.jbcrypt.BCrypt;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.engine.lobby.server.userDB.DBUser;

/**
 * Implementation of {@link UserDao} for a Postgres database.
 */
public final class UserController extends AbstractController implements UserDao {
  public UserController(final Database database) {
    super(database);
  }

  @Override
  public HashedPassword getPassword(final String username) {
    try (Connection con = newDatabaseConnection();
        PreparedStatement ps = con
            .prepareStatement("select bcrypt_password from ta_users where username=?")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new HashedPassword(rs.getString(1));
        }
        return null;
      }
    } catch (final SQLException e) {
      throw newDatabaseException("Error getting password for user: " + username, e);
    }
  }

  @Override
  public boolean doesUserExist(final String username) {
    final String sql = "select username from ta_users where upper(username) = upper(?)";
    try (Connection con = newDatabaseConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (final SQLException e) {
      throw newDatabaseException("Error testing for existence of user: " + username, e);
    }
  }

  @Override
  public void updateUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkArgument(user.isValid(), user.getValidationErrorMessage());

    try (Connection con = newDatabaseConnection();
        PreparedStatement ps = con.prepareStatement(
            "update ta_users set bcrypt_password=?, email=? where username=?")) {
      ps.setString(1, hashedPassword.value);
      ps.setString(2, user.getEmail());
      ps.setString(3, user.getName());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw newDatabaseException(String.format("Error updating name: %s, email: %s",
          user.getName(), user.getEmail()), e);
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

    try (Connection con = newDatabaseConnection();
        PreparedStatement ps = con.prepareStatement("update ta_users set admin=? where username = ?")) {
      ps.setBoolean(1, user.isAdmin());
      ps.setString(2, user.getName());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw newDatabaseException(String.format("Error while trying to make %s an admin", user.getName()), e);
    }
  }

  @Override
  public void createUser(final DBUser user, final HashedPassword hashedPassword) {
    Preconditions.checkState(hashedPassword.isHashedWithSalt());
    Preconditions.checkState(user.isValid(), user.getValidationErrorMessage());

    try (Connection con = newDatabaseConnection();
        PreparedStatement ps = con.prepareStatement(
            "insert into ta_users (username, bcrypt_password, email) values (?, ?, ?)")) {
      ps.setString(1, user.getName());
      ps.setString(2, hashedPassword.value);
      ps.setString(3, user.getEmail());
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw newDatabaseException(String.format("Error inserting name: %s, email: %s",
          user.getName(), user.getEmail()), e);
    }
  }

  @Override
  public boolean login(final String username, final HashedPassword hashedPassword) {
    try (Connection con = newDatabaseConnection()) {
      final HashedPassword actualPassword = getPassword(username);
      if (actualPassword == null) {
        return false;
      }
      Preconditions.checkState(actualPassword.isBcrypted());
      if (!BCrypt.checkpw(hashedPassword.value, actualPassword.value)) {
        return false;
      }
      // update last login time
      try (PreparedStatement ps = con.prepareStatement("update ta_users set lastLogin = ? where username = ?")) {
        ps.setTimestamp(1, Timestamp.from(Instant.now()));
        ps.setString(2, username);
        ps.execute();
        con.commit();
        return true;
      }
    } catch (final SQLException e) {
      throw newDatabaseException(
          String.format("Error validating password name: %s", username), e);
    }
  }

  @Override
  public DBUser getUserByName(final String username) {
    final String sql = "select * from ta_users where username = ?";
    try (Connection con = newDatabaseConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return new DBUser(
            new DBUser.UserName(rs.getString("username")),
            new DBUser.UserEmail(rs.getString("email")),
            rs.getBoolean("admin") ? DBUser.Role.ADMIN : DBUser.Role.NOT_ADMIN);
      }
    } catch (final SQLException e) {
      throw newDatabaseException("Error getting user: " + username, e);
    }
  }
}
