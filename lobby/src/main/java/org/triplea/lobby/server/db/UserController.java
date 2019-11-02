package org.triplea.lobby.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.server.db.dao.UserJdbiDao;
import org.triplea.lobby.server.db.dao.UserRoleDao;
import org.triplea.lobby.server.db.data.UserRole;

/** Implementation of {@link UserDao} for a Postgres database. */
// TODO: Project#12 dead code soon, ensure this code is removed.
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
  public void createUser(
      final String name, final String email, final HashedPassword hashedPassword) {

    try (Connection con = connection.get();
        PreparedStatement ps =
            con.prepareStatement("insert into user_role(id, name) values(10, 'ANONYMOUS')")) {

      ps.execute();
      con.commit();
    } catch (final SQLException e) {
      // ignore
      // TODO: Project#12 this is a super-dirty hack to support test code.
      // The only exception we expect is a UK violation error which we can safely ignore.
    }

    final UserRoleDao dao = JdbiDatabase.newConnection().onDemand(UserRoleDao.class);
    final int anonymousRoleId = dao.lookupRoleId(UserRole.ANONYMOUS);

    try (Connection con = connection.get();
        PreparedStatement ps =
            con.prepareStatement(
                "insert into lobby_user (username, bcrypt_password, email, user_role_id) "
                    + "values (?, ?, ?, "
                    + anonymousRoleId
                    + ")")) {
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
  public boolean isAdmin(final String username) {
    // TODO: Project#12 deprecated/dead code, Usercontroller should be removed soon.
    return false;
  }
}
