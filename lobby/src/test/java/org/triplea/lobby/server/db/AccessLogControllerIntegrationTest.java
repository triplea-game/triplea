package org.triplea.lobby.server.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.lobby.server.login.UserType;
import org.triplea.test.common.Integration;

@Integration
final class AccessLogControllerIntegrationTest {
  private final AccessLogDao accessLogController =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getAccessLogDao();

  @Test
  void insert_ShouldInsertNewRecord() throws Exception {
    final User user = TestUserUtils.newUser();

    for (final UserType userType : UserType.values()) {
      accessLogController.insert(user, userType);

      thenAccessLogRecordShouldExist(user, userType);
    }
  }

  private void thenAccessLogRecordShouldExist(final User user, final UserType userType)
      throws Exception {
    final String sql =
        "select access_time from access_log where username=? and ip=?::inet and mac=? and registered=?";
    try (Connection conn = TestDatabase.newConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getUsername());
      ps.setString(2, user.getInetAddress().getHostAddress());
      ps.setString(3, user.getHashedMacAddress());
      ps.setBoolean(4, userType == UserType.REGISTERED);
      try (ResultSet rs = ps.executeQuery()) {
        assertThat("record should exist", rs.next(), is(true));
        assertThat(
            "access_time column should have a default value",
            rs.getTimestamp(1),
            is(not(nullValue())));
        assertThat(
            "only one record should exist "
                + "(possible aliasing from another test run due to no control over access_time value)",
            rs.next(),
            is(false));
      }
    }
  }
}
