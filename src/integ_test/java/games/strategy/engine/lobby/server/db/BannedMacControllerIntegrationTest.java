package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.Moderator;
import games.strategy.util.Tuple;

public final class BannedMacControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final BannedMacController controller = spy(new BannedMacController());
  private final String hashedMac = newHashedMacAddress();

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

    final Moderator otherModerator = newModerator();
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

  private void assertModeratorForBannedMacEquals(final Moderator expected) {
    assertModeratorEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from banned_macs where mac=?",
        ps -> ps.setString(1, hashedMac),
        "unknown banned hashed MAC address: " + hashedMac);
  }
}
