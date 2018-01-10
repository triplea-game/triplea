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

public class BannedMacControllerIntegrationTest {

  private final BannedMacController controller = spy(new BannedMacController());
  private final String hashedMac = newHashedMacAddress();
  private final Moderator moderator = new Moderator("mod", newInetAddress(), newHashedMacAddress());

  @Test
  public void testBanMacForever() {
    banMacForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> macDetails = controller.isMacBanned(hashedMac);
    assertTrue(macDetails.getFirst());
    assertNull(macDetails.getSecond());
  }

  @Test
  public void testBanMac() {
    final Instant banUntil = banMacForSeconds(100L);
    final Tuple<Boolean, Timestamp> macDetails = controller.isMacBanned(hashedMac);
    assertTrue(macDetails.getFirst());
    assertEquals(banUntil, macDetails.getSecond().toInstant());
    when(controller.now()).thenReturn(banUntil.plusSeconds(1L));
    final Tuple<Boolean, Timestamp> macDetails2 = controller.isMacBanned(hashedMac);
    assertFalse(macDetails2.getFirst());
    assertEquals(banUntil, macDetails2.getSecond().toInstant());
  }

  @Test
  public void testUnbanMac() {
    final Instant banUntil = banMacForSeconds(100L);
    final Tuple<Boolean, Timestamp> macDetails = controller.isMacBanned(hashedMac);
    assertTrue(macDetails.getFirst());
    assertEquals(banUntil, macDetails.getSecond().toInstant());
    banMacForSeconds(-10L);
    final Tuple<Boolean, Timestamp> macDetails2 = controller.isMacBanned(hashedMac);
    assertFalse(macDetails2.getFirst());
    assertNull(macDetails2.getSecond());
  }

  @Test
  public void testBanMacInThePast() {
    banMacForSeconds(-10L);
    final Tuple<Boolean, Timestamp> macDetails = controller.isMacBanned(hashedMac);
    assertFalse(macDetails.getFirst());
    assertNull(macDetails.getSecond());
  }

  @Test
  public void testBanMacUpdate() {
    banMacForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> macDetails = controller.isMacBanned(hashedMac);
    assertTrue(macDetails.getFirst());
    assertNull(macDetails.getSecond());
    final Instant banUntil = banMacForSeconds(100L);
    final Tuple<Boolean, Timestamp> macDetails2 = controller.isMacBanned(hashedMac);
    assertTrue(macDetails2.getFirst());
    assertEquals(banUntil, macDetails2.getSecond().toInstant());
  }

  @Test
  public void testBanMacUpdatesModerator() {
    banMacForSeconds(Long.MAX_VALUE, moderator);

    final Moderator otherModerator = new Moderator("otherMod", newInetAddress(), newHashedMacAddress());
    banMacForSeconds(Long.MAX_VALUE, otherModerator);

    assertModeratorForBannedMacEquals(otherModerator);
  }

  private Instant banMacForSeconds(final long length) {
    return banMacForSeconds(length, moderator);
  }

  private Instant banMacForSeconds(final long length, final Moderator moderator) {
    final Instant banEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addBannedMac(hashedMac, banEnd, moderator);
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

  private void assertModeratorForBannedMacEquals(final Moderator expected) {
    final String sql = "select mod_username, mod_ip, mod_mac from banned_macs where mac=?";
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, hashedMac);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail("unknown banned hashed MAC address: " + hashedMac);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed moderator IP address", e);
    } catch (final SQLException e) {
      fail("moderator query failed", e);
    }
  }
}
