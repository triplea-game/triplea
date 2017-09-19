package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Test;

import games.strategy.util.MD5Crypt;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

public class BannedMacControllerTest {

  private final BannedMacController controller = spy(new BannedMacController());
  private final String hashedMac = MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");

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
    final Instant banUntill = Instant.now().plusSeconds(100L);
    controller.addBannedMac(hashedMac, banUntill);
    final Tuple<Boolean, Timestamp> macDetails2 = controller.isMacBanned(hashedMac);
    assertTrue(macDetails2.getFirst());
    assertEquals(banUntill, macDetails2.getSecond().toInstant());
  }

  private Instant banMacForSeconds(final long length) {
    final Instant banEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addBannedMac(hashedMac, banEnd);
    return banEnd;
  }
}
