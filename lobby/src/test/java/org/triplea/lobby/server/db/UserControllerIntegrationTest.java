package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.test.common.Integration;
import org.triplea.util.Md5Crypt;

import games.strategy.engine.lobby.server.userDB.DBUser;

@Integration
public final class UserControllerIntegrationTest {
  private final UserDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getUserDao();

  @Test
  void testCreate() throws Exception {
    final int startCount = getUserCount();

    newUserWithMd5CryptHash();
    assertEquals(getUserCount(), startCount + 1);

    newUserWithBCryptHash();
    assertEquals(getUserCount(), startCount + 2);
  }

  @Test
  void testGet() {
    final DBUser user = newUserWithMd5CryptHash();
    assertEquals(user, controller.getUserByName(user.getName()));
  }

  @Test
  void testDoesUserExist() {
    assertTrue(controller.doesUserExist(newUserWithMd5CryptHash().getName()));
    assertTrue(controller.doesUserExist(newUserWithBCryptHash().getName()));
  }

  @Test
  void testCreateDupe() {
    assertThrows(Exception.class,
        () -> controller.createUser(newUserWithMd5CryptHash(),
            new HashedPassword(md5Crypt(Util.newUniqueTimestamp()))),
        "Should not be allowed to create a dupe user");
  }

  @Test
  void testLogin() {
    final String password = md5Crypt(Util.newUniqueTimestamp());
    final DBUser user = newUserWithHash(password, Function.identity());
    controller.updateUser(user, new HashedPassword(bcrypt(obfuscate(password))));
    assertTrue(controller.login(user.getName(), new HashedPassword(password)));
    assertTrue(controller.login(user.getName(), new HashedPassword(obfuscate(password))));
  }

  @Test
  void testUpdate() throws Exception {
    final DBUser user = newUserWithMd5CryptHash();
    assertTrue(controller.doesUserExist(user.getName()));
    final String password2 = md5Crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(bcrypt(obfuscate(Util.newUniqueTimestamp()))));
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (Connection con = TestDatabase.newConnection()) {
      final String sql = " select * from ta_users where username = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
      assertNull(rs.getString("bcrypt_password"));
    }
  }

  private DBUser newUserWithMd5CryptHash() {
    return newUserWithHash(Util.newUniqueTimestamp(), UserControllerIntegrationTest::md5Crypt);
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
    try (Connection dbConnection = TestDatabase.newConnection()) {
      final String sql = "select count(*) from TA_USERS";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      return rs.getInt(1);
    }
  }

  private static String bcrypt(final String string) {
    return BCrypt.hashpw(string, BCrypt.gensalt());
  }

  @SuppressWarnings("deprecation") // required for testing; remove upon next lobby-incompatible release
  private static String md5Crypt(final String value) {
    return Md5Crypt.hashPassword(value, Md5Crypt.newSalt());
  }

  private static String obfuscate(final String string) {
    return RsaAuthenticator.hashPasswordWithSalt(string);
  }
}
