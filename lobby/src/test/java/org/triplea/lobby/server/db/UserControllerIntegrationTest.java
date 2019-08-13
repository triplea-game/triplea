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
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.test.common.Integration;
import org.triplea.util.Md5Crypt;

@Integration
final class UserControllerIntegrationTest {
  private final UserDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getUserDao();

  @Test
  void testCreate() throws Exception {
    final int startCount = getUserCount();

    newUserWithBCryptHash();
    assertEquals(getUserCount(), startCount + 1);

    newUserWithBCryptHash();
    assertEquals(getUserCount(), startCount + 2);
  }

  @Test
  void testGet() {
    final String user = newUserWithBCryptHash();
    assertEquals(generateEmailAddress(user), controller.getUserEmailByName(user));
  }

  @Test
  void testDoesUserExist() {
    assertTrue(controller.doesUserExist(newUserWithBCryptHash()));
    assertTrue(controller.doesUserExist(newUserWithBCryptHash()));
  }

  @Test
  void testCreateDupe() {
    final String user = newUserWithBCryptHash();
    assertThrows(
        Exception.class,
        () ->
            controller.createUser(
                user,
                generateEmailAddress(user),
                new HashedPassword(md5Crypt(TestUserUtils.newUniqueTimestamp()))),
        "Should not be allowed to create a dupe user");
  }

  @Test
  void testLogin() {
    final String password = bcrypt(TestUserUtils.newUniqueTimestamp());
    final String user = newUserWithHash(password, Function.identity());
    controller.updateUser(
        user, generateEmailAddress(user), new HashedPassword(bcrypt(obfuscate(password))));
    assertTrue(controller.login(user, new HashedPassword(obfuscate(password))));
  }

  @Test
  void testUpdate() throws Exception {
    final String user = newUserWithBCryptHash();
    assertTrue(controller.doesUserExist(user));
    final String password2 = md5Crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        user, email2, new HashedPassword(bcrypt(obfuscate(TestUserUtils.newUniqueTimestamp()))));
    controller.updateUser(user, email2, new HashedPassword(password2));
    try (Connection con = TestDatabase.newConnection()) {
      final String sql = " select * from lobby_user where username = '" + user + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
      assertNull(rs.getString("bcrypt_password"));
    }
  }

  private String newUserWithBCryptHash() {
    return newUserWithHash(
        TestUserUtils.newUniqueTimestamp(), UserControllerIntegrationTest::bcrypt);
  }

  private String newUserWithHash(
      final @Nullable String password, final Function<String, String> hashingMethod) {
    final String name = UUID.randomUUID().toString().substring(0, 20);
    controller.createUser(
        name, generateEmailAddress(name), new HashedPassword(hashingMethod.apply(password)));
    return name;
  }

  private String generateEmailAddress(final String name) {
    return name + "@none.none";
  }

  private int getUserCount() throws Exception {
    try (Connection dbConnection = TestDatabase.newConnection()) {
      final String sql = "select count(*) from lobby_user";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      return rs.getInt(1);
    }
  }

  private static String bcrypt(final String string) {
    return BCrypt.hashpw(string, BCrypt.gensalt());
  }

  @SuppressWarnings(
      "deprecation") // required for testing; remove upon next lobby-incompatible release
  private static String md5Crypt(final String value) {
    return Md5Crypt.hashPassword(value, Md5Crypt.newSalt());
  }

  private static String obfuscate(final String string) {
    return RsaAuthenticator.hashPasswordWithSalt(string);
  }
}
