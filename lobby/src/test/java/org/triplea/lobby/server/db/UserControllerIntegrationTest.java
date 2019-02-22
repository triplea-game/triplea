package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.java.Util;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.test.common.Integration;

import games.strategy.engine.lobby.server.userDB.DBUser;

@Integration
final class UserControllerIntegrationTest extends AbstractControllerTestCase {
  private final UserController controller = new UserController(database);

  @Test
  void testCreate() throws Exception {
    final int startCount = getUserCount();

    newUserWithBCryptHash();
    assertEquals(getUserCount(), startCount + 1);
  }

  @Test
  void testGet() {
    final DBUser user = newUserWithBCryptHash();
    assertEquals(user, controller.getUserByName(user.getName()));
  }

  @Test
  void testDoesUserExist() {
    assertTrue(controller.doesUserExist(newUserWithBCryptHash().getName()));
  }

  @Test
  void testLogin() {
    final String password = bcrypt(Util.newUniqueTimestamp());
    final DBUser user = newUserWithHash(password, Function.identity());
    controller.updateUser(user, new HashedPassword(bcrypt(obfuscate(password))));
    assertTrue(controller.login(user.getName(), new HashedPassword(obfuscate(password))));
  }

  @Test
  void testUpdate() throws Exception {
    final DBUser user = newUserWithBCryptHash();
    assertTrue(controller.doesUserExist(user.getName()));
    final String password2 = bcrypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (Connection con = database.newConnection()) {
      final String sql = " select * from ta_users where username = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("bcrypt_password"));
    }
  }

  private DBUser newUserWithBCryptHash() {
    return newUserWithHash(Util.newUniqueTimestamp(), UserControllerIntegrationTest::bcrypt);
  }

  private DBUser newUserWithHash(final @Nullable String password, final Function<String, String> hashingMethod) {
    final String name = UUID.randomUUID().toString().substring(0, 20);
    final DBUser user = new DBUser(
        new DBUser.UserName(name),
        new DBUser.UserEmail(name + "@none.none"));
    controller.createUser(user, new HashedPassword(hashingMethod.apply(password)));
    return user;
  }

  private int getUserCount() throws Exception {
    try (Connection dbConnection = database.newConnection()) {
      final String sql = "select count(*) from TA_USERS";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      return rs.getInt(1);
    }
  }

  private static String bcrypt(final String string) {
    return BCrypt.hashpw(string, BCrypt.gensalt());
  }

  private static String obfuscate(final String string) {
    return RsaAuthenticator.hashPasswordWithSalt(string);
  }
}
