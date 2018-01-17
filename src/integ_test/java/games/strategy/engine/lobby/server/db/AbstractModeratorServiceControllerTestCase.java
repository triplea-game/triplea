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

import games.strategy.engine.lobby.server.Moderator;
import games.strategy.util.Util;

/**
 * Superclass for fixtures that test a moderator service controller.
 */
public abstract class AbstractModeratorServiceControllerTestCase {
  protected final Moderator moderator = newModerator();

  protected AbstractModeratorServiceControllerTestCase() {}

  /**
   * Creates a new unique moderator.
   */
  protected static Moderator newModerator() {
    return new Moderator(newUsername(), newInetAddress(), newHashedMacAddress());
  }

  /**
   * Creates a new unique username.
   */
  protected static String newUsername() {
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

  /**
   * Creates a new unique hashed MAC address.
   */
  protected static String newHashedMacAddress() {
    return games.strategy.util.MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");
  }

  /**
   * Asserts the moderator returned from the specified query is equal to the expected moderator.
   *
   * @param expected The expected moderator.
   * @param moderatorQuerySql The SQL used to query for the moderator. It is expected that this query returns the
   *        moderator's username in the first column, the moderator's IP address in the second column, and the
   *        moderator's hashed MAC address in the third column.
   * @param preparedStatementInitializer Callback to initialize the parameters in the prepared statement used to query
   *        for the moderator.
   * @param unknownModeratorMessage The failure message to be used when the requested moderator does not exist.
   */
  protected static void assertModeratorEquals(
      final Moderator expected,
      final String moderatorQuerySql,
      final PreparedStatementInitializer preparedStatementInitializer,
      final String unknownModeratorMessage) {
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(moderatorQuerySql)) {
      preparedStatementInitializer.initialize(ps);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail(unknownModeratorMessage);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed moderator IP address", e);
    } catch (final SQLException e) {
      fail("moderator query failed", e);
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
