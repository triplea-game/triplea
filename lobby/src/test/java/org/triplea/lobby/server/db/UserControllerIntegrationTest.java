package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
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
  void testDoesUserExist() {
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
}
