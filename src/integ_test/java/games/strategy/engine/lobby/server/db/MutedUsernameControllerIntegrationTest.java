package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.Moderator;
import games.strategy.util.Util;

public class MutedUsernameControllerIntegrationTest {

  private final MutedUsernameController controller = spy(new MutedUsernameController());
  private final String username = Util.createUniqueTimeStamp();
  private final Moderator moderator = new Moderator("mod", newInetAddress(), newHashedMacAddress());

  @Test
  public void testMuteUsernameForever() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(Instant.MAX), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(muteUntil), controller.getUsernameUnmuteTime(username));
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isUsernameMuted(username));
    assertEquals(Optional.empty(), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testUnmuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(muteUntil), controller.getUsernameUnmuteTime(username));
    muteUsernameForSeconds(-10L);
    assertFalse(controller.isUsernameMuted(username));
    assertEquals(Optional.empty(), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsernameInThePast() {
    muteUsernameForSeconds(-10L);
    assertFalse(controller.isUsernameMuted(username));
    assertEquals(Optional.empty(), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsernameUpdate() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(Instant.MAX), controller.getUsernameUnmuteTime(username));
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(muteUntil), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsernameUpdatesModerator() {
    muteUsernameForSeconds(Long.MAX_VALUE, moderator);

    final Moderator otherModerator = new Moderator("otherMod", newInetAddress(), newHashedMacAddress());
    muteUsernameForSeconds(Long.MAX_VALUE, otherModerator);

    assertModeratorForMutedUsernameEquals(otherModerator);
  }

  private Instant muteUsernameForSeconds(final long length) {
    return muteUsernameForSeconds(length, moderator);
  }

  private Instant muteUsernameForSeconds(final long length, final Moderator moderator) {
    final Instant muteEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addMutedUsername(username, muteEnd, moderator);
    return muteEnd;
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

  private void assertModeratorForMutedUsernameEquals(final Moderator expected) {
    final String sql = "select mod_username, mod_ip, mod_mac from muted_usernames where username=?";
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail("unknown muted username: " + username);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed moderator IP address", e);
    } catch (final SQLException e) {
      fail("moderator query failed", e);
    }
  }
}
