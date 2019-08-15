package org.triplea.lobby.server.db;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;

/** Implementation of {@link UserDao} for a Postgres database. */
@AllArgsConstructor
final class UserController implements UserDao {
  private final Supplier<Connection> connection;

  @Override
  public HashedPassword getPassword(final String username) {
    try (Connection con = connection.get();
        PreparedStatement ps =
            con.prepareStatement(
                "select coalesce(bcrypt_password, password) "
                    + "from lobby_user "
                    + "where username = ?")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return new HashedPassword(rs.getString(1));
        }
        return null;
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error getting password for user: " + username, e);
    }
  }

  @Override
  public boolean doesUserExist(final String username) {
    final String sql = "select username from lobby_user where upper(username) = upper(?)";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error testing for existence of user: " + username, e);
    }
  }

  @Override
  public void updateUser(
      final String name, final String email, final HashedPassword hashedPassword) {
    try (Connection con = connection.get();
        PreparedStatement ps =
            con.prepareStatement(
                String.format(
                    "update lobby_user set %s=?%s where username=?",
                    getPasswordColumn(hashedPassword), email == null ? "" : ", email=?"))) {
      ps.setString(1, hashedPassword.value);
      int index = 2;
      if (email != null) {
        ps.setString(index, email);
        index++;
      }
      ps.setString(index, name);
      ps.execute();
      if (!hashedPassword.isBcrypted()) {
        try (PreparedStatement ps2 =
            con.prepareStatement("update lobby_user set bcrypt_password = null where username=?")) {
          ps2.setString(1, name);
          ps2.execute();
        }
      }
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException(
          String.format(
              "Error updating name: %s, email: %s, (masked) pwd: %s",
              name, email, hashedPassword.mask()),
          e);
    }
  }

  /** Workaround utility method. Should be removed in the next lobby-incompatible release. */
  private static String getPasswordColumn(final HashedPassword hashedPassword) {
    return hashedPassword.isBcrypted() ? "bcrypt_password" : "password";
  }

  @Override
  public void createUser(
      final String name, final String email, final HashedPassword hashedPassword) {
    try (Connection con = connection.get();
        PreparedStatement ps =
            con.prepareStatement(
                "insert into lobby_user (username, bcrypt_password, email) "
                    + "values (?, ?, ?)")) {
      ps.setString(1, name);
      ps.setString(2, hashedPassword.value);
      ps.setString(3, email);
      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      throw new DatabaseException(
          String.format(
              "Error inserting name: %s, email: %s, (masked) pwd: %s",
              name, email, hashedPassword.mask()),
          e);
    }
  }

  @Override
  public boolean login(final String username, final HashedPassword hashedPassword) {
    final HashedPassword actualPassword = getPassword(username);
    if (actualPassword == null) {
      return false;
    }
    Preconditions.checkState(actualPassword.isBcrypted());
    if (!BCrypt.checkpw(hashedPassword.value, actualPassword.value)) {
      return false;
    }
    // update last login time
    try (Connection con = connection.get();
        PreparedStatement ps =
            con.prepareStatement("update lobby_user set last_login = ? where username = ?")) {
      ps.setTimestamp(1, Timestamp.from(Instant.now()));
      ps.setString(2, username);
      ps.execute();
      con.commit();
      return true;
    } catch (final SQLException e) {
      throw new DatabaseException(
          String.format(
              "Error validating password name: %s, (masked) pwd: %s",
              username, hashedPassword.mask()),
          e);
    }
  }

  @Override
  public String getUserEmailByName(final String username) {
    final String sql = "select email from lobby_user where username = ?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return rs.getString("email");
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error getting user email for user: " + username, e);
    }
  }

  @Override
  public boolean isAdmin(final String username) {
    final String sql = "select admin from lobby_user where username = ?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return false;
        }
        return rs.getBoolean("admin");
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error getting admin flag for user: " + username, e);
    }
  }
}
