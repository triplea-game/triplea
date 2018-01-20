package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import games.strategy.engine.lobby.server.User;
import games.strategy.net.MacFinder;
import games.strategy.util.Util;

/**
 * Superclass for fixtures that test a moderator service controller.
 */
public abstract class AbstractModeratorServiceControllerTestCase {
  protected final User user = newUser();
  protected final User moderator = newUser();

  protected AbstractModeratorServiceControllerTestCase() {}

  /**
   * Creates a new unique user.
   */
  protected static User newUser() {
    return new User(newUsername(), newInetAddress(), newHashedMacAddress());
  }

  private static String newUsername() {
    return "user_" + Util.createUniqueTimeStamp();
  }

  private static InetAddress newInetAddress() {
    final byte[] addr = new byte[4];
    new Random().nextBytes(addr);
    try {
      return InetAddress.getByAddress(addr);
    } catch (final UnknownHostException e) {
      throw new AssertionError("should never happen", e);
    }
  }

  private static String newHashedMacAddress() {
    final byte[] bytes = new byte[6];
    new Random().nextBytes(bytes);
    return MacFinder.getHashedMacAddress(bytes);
  }

  /**
   * Asserts the user returned from the specified query is equal to the expected user.
   *
   * @param expected The expected user.
   * @param userQuerySql The SQL used to query for the user. It is expected that this query returns the user's name
   *        in the first column, the user's IP address in the second column, and the user's hashed MAC address in the
   *        third column.
   * @param preparedStatementInitializer Callback to initialize the parameters in the prepared statement used to query
   *        for the user.
   * @param unknownUserMessage The failure message to be used when the requested user does not exist.
   */
  protected static void assertUserEquals(
      final User expected,
      final String userQuerySql,
      final PreparedStatementInitializer preparedStatementInitializer,
      final String unknownUserMessage) {
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(userQuerySql)) {
      preparedStatementInitializer.initialize(ps);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail(unknownUserMessage);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed user IP address", e);
    } catch (final SQLException e) {
      fail("user query failed", e);
    }
  }

  /**
   * Initializes the parameters of a {@link PreparedStatement}.
   */
  @FunctionalInterface
  protected interface PreparedStatementInitializer {
    /**
     * Initializes the parameters of the specified prepared statement.
     *
     * @param ps The prepared statement to initialize.
     *
     * @throws SQLException If an error occurs while initializing the prepared statement.
     */
    void initialize(PreparedStatement ps) throws SQLException;
  }
}
