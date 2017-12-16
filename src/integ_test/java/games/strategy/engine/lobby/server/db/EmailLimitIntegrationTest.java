package games.strategy.engine.lobby.server.db;

import static games.strategy.test.Assertions.assertNotThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.common.base.Strings;

import games.strategy.util.Util;

/**
 * Emails have a limit of 254 chars, accepted by the IETF.
 * More information: http://www.rfc-editor.org/errata_search.php?rfc=3696&eid=1690
 * This class checks if those lengths are supported.
 */
public class EmailLimitIntegrationTest {

  private static Connection connection;

  @BeforeAll
  public static void setup() throws SQLException {
    connection = Database.getPostgresConnection();
    connection.setAutoCommit(true);
  }

  @Test
  public void testAllowsMaximumLength() {
    assertNotThrows(() -> createAccountWithEmail(getStringWithLength(60) + "@" + getStringWithLength(193)));
  }

  @Test
  public void testAllowsMaximumLocalLength() {
    assertNotThrows(() -> createAccountWithEmail(getStringWithLength(64) + "@" + getStringWithLength(189)));
  }

  private static String getStringWithLength(final int length) {
    return Strings.padStart(Util.createUniqueTimeStamp(), length, 'a');
  }

  private static void createAccountWithEmail(final String email) throws SQLException {
    try (PreparedStatement ps =
        connection.prepareStatement("insert into ta_users (username, email, password) values (?, ?, ?)")) {
      ps.setString(1, Util.createUniqueTimeStamp());
      ps.setString(2, email);
      ps.setString(3, games.strategy.util.MD5Crypt.crypt("password"));
      ps.execute();
    }
  }

  @AfterAll
  public static void tearDown() throws SQLException {
    connection.close();
  }
}
