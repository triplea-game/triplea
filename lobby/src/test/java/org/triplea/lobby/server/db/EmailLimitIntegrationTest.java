package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.java.Util;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.test.common.Integration;

import com.google.common.base.Strings;

/**
 * Emails have a limit of 254 chars, accepted by the IETF.
 * More information: http://www.rfc-editor.org/errata_search.php?rfc=3696&eid=1690
 * This class checks if those lengths are supported.
 */
@Integration
public class EmailLimitIntegrationTest {
  private final Database database = new Database(TestLobbyConfigurations.INTEGRATION_TEST);

  @Test
  public void testAllowsMaximumLength() {
    assertDoesNotThrow(() -> createAccountWithEmail(getStringWithLength(60) + "@" + getStringWithLength(193)));
  }

  @Test
  public void testAllowsMaximumLocalLength() {
    assertDoesNotThrow(() -> createAccountWithEmail(getStringWithLength(64) + "@" + getStringWithLength(189)));
  }

  private static String getStringWithLength(final int length) {
    return Strings.padStart(Util.newUniqueTimestamp(), length, 'a');
  }

  private void createAccountWithEmail(final String email) throws SQLException {
    try (Connection connection = database.newConnection();
        PreparedStatement ps =
            connection.prepareStatement("insert into ta_users (username, email, bcrypt_password) values (?, ?, ?)")) {
      ps.setString(1, Util.newUniqueTimestamp());
      ps.setString(2, email);
      ps.setString(3, BCrypt.hashpw("password", BCrypt.gensalt()));
      ps.execute();
    }
  }
}
