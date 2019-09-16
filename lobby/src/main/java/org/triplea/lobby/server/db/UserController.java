package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

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
      if (email != null) {
        ps.setString(2, email);
      }
      ps.setString(email != null ? 3 : 2, name);
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
  public boolean login(final String username, final String hashedPassword) {
    final UserJdbiDao userJdbiDao = JdbiDatabase.newConnection().onDemand(UserJdbiDao.class);
    final String actualPassword = userJdbiDao.getPassword(username).orElse(null);
    if (actualPassword == null) {
      return false;
    }
    if (!BCrypt.checkpw(hashedPassword, actualPassword)) {
      return false;
    }
    userJdbiDao.updateLastLoginTime(username);
    return true;
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
    final String sql = "select role from lobby_user where username = ?";
    try (Connection con = connection.get();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return false;
        }
        final String role = rs.getString("role");
        return "ADMIN".equals(role) || "MODERATOR".equals(role);
      }
    } catch (final SQLException e) {
      throw new DatabaseException("Error getting admin flag for user: " + username, e);
    }
  }
}
