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

public class MutedMacControllerIntegrationTest {

  private final MutedMacController controller = spy(new MutedMacController());
  private final String hashedMac = newHashedMacAddress();
  private final Moderator moderator = new Moderator("mod", newInetAddress(), newHashedMacAddress());

  @Test
  public void testMuteMacForever() {
    muteMacForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Optional.of(Instant.MAX), controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testMuteMac() {
    final Instant muteUntil = muteMacForSeconds(100L);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Optional.of(muteUntil), controller.getMacUnmuteTime(hashedMac));
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isMacMuted(hashedMac));
    assertEquals(Optional.empty(), controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testUnmuteMac() {
    final Instant muteUntil = muteMacForSeconds(100L);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Optional.of(muteUntil), controller.getMacUnmuteTime(hashedMac));
    muteMacForSeconds(-10L);
    assertFalse(controller.isMacMuted(hashedMac));
    assertEquals(Optional.empty(), controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testMuteMacInThePast() {
    muteMacForSeconds(-10L);
    assertFalse(controller.isMacMuted(hashedMac));
    assertEquals(Optional.empty(), controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testMuteMacUpdate() {
    muteMacForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Optional.of(Instant.MAX), controller.getMacUnmuteTime(hashedMac));
    final Instant muteUntil = muteMacForSeconds(100L);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Optional.of(muteUntil), controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testMuteMacUpdatesModerator() {
    muteMacForSeconds(Long.MAX_VALUE, moderator);

    final Moderator otherModerator = new Moderator("otherMod", newInetAddress(), newHashedMacAddress());
    muteMacForSeconds(Long.MAX_VALUE, otherModerator);

    assertModeratorForMutedMacEquals(otherModerator);
  }

  private Instant muteMacForSeconds(final long length) {
    return muteMacForSeconds(length, moderator);
  }

  private Instant muteMacForSeconds(final long length, final Moderator moderator) {
    final Instant muteEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addMutedMac(hashedMac, muteEnd, moderator);
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

  private void assertModeratorForMutedMacEquals(final Moderator expected) {
    final String sql = "select mod_username, mod_ip, mod_mac from muted_macs where mac=?";
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, hashedMac);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail("unknown muted hashed MAC address: " + hashedMac);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed moderator IP address", e);
    } catch (final SQLException e) {
      fail("moderator query failed", e);
    }
  }
}
