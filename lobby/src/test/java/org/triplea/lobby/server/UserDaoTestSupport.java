package org.triplea.lobby.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.triplea.lobby.server.db.TestDatabase;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class UserDaoTestSupport {
  /**
   * A method to set a given user as admin.
   */
  static void makeAdmin(final String username) throws SQLException {
    try (Connection con = TestDatabase.newConnection();
        PreparedStatement ps = con.prepareStatement("update lobby_user set admin = true where username = ?")) {
      ps.setString(1, username);
      ps.execute();
      con.commit();
    }
  }
}
