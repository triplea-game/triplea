package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Random;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.Moderator;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

public class BannedUsernameControllerIntegrationTest {

  private final BannedUsernameController controller = spy(new BannedUsernameController());
  private final String username = Util.createUniqueTimeStamp();
  private final Moderator moderator = new Moderator("mod", newInetAddress(), newHashedMacAddress());

  @Test
  public void testBanUsernameForever() {
    banUsernameForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  public void testBanUsername() {
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    when(controller.now()).thenReturn(banUntil.plusSeconds(1L));
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertFalse(usernameDetails2.getFirst());
    assertEquals(banUntil, usernameDetails2.getSecond().toInstant());
  }

  @Test
  public void testUnbanUsername() {
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    banUsernameForSeconds(-10L);
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertFalse(usernameDetails2.getFirst());
    assertNull(usernameDetails2.getSecond());
  }

  @Test
  public void testBanUsernameInThePast() {
    banUsernameForSeconds(-10L);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertFalse(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  public void testBanUsernameUpdate() {
    banUsernameForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertTrue(usernameDetails2.getFirst());
    assertEquals(banUntil, usernameDetails2.getSecond().toInstant());
  }

  @Test
  public void testBanUsernameUpdatesModerator() {
    banUsernameForSeconds(Long.MAX_VALUE, moderator);

    final Moderator otherModerator = new Moderator("otherMod", newInetAddress(), newHashedMacAddress());
    banUsernameForSeconds(Long.MAX_VALUE, otherModerator);

    assertModeratorForBannedUsernameEquals(otherModerator);
  }

  private Instant banUsernameForSeconds(final long length) {
    return banUsernameForSeconds(length, moderator);
  }

  private Instant banUsernameForSeconds(final long length, final Moderator moderator) {
    final Instant banEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addBannedUsername(username, banEnd, moderator);
    return banEnd;
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
    return games.strategy.util.MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");
  }

  private void assertModeratorForBannedUsernameEquals(final Moderator expected) {
    final String sql = "select mod_username, mod_ip, mod_mac from banned_usernames where username=?";
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail("unknown banned username: " + username);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed moderator IP address", e);
    } catch (final SQLException e) {
      fail("moderator query failed", e);
    }
  }
}
