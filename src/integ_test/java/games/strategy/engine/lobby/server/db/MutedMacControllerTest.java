package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.Test;

import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class MutedMacControllerTest {

  private final MutedMacController controller = spy(new MutedMacController());

  @Test
  public void testMuteMacForever() {
    final String hashedMac = muteMac(null);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Long.MAX_VALUE, controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testMuteMac() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    final String hashedMac = muteMac(muteUntil);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getMacUnmuteTime(hashedMac)));
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isMacMuted(hashedMac));
  }

  @Test
  public void testUnmuteMac() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    final String hashedMac = muteMac(muteUntil);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getMacUnmuteTime(hashedMac)));
    controller.addMutedMac(hashedMac, Instant.now().minusSeconds(10L));
    assertFalse(controller.isMacMuted(hashedMac));
  }

  @Test
  public void testMuteMacInThePast() {
    final Instant muteUntil = Instant.now().minusSeconds(10L);
    final String hashedMac = muteMac(muteUntil);
    assertFalse(controller.isMacMuted(hashedMac));
  }

  @Test
  public void testMuteMacUpdate() {
    final String hashedMac = muteMac(null);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Long.MAX_VALUE, controller.getMacUnmuteTime(hashedMac));
    final Instant muteUntill = Instant.now().plusSeconds(100L);
    controller.addMutedMac(hashedMac, muteUntill);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(muteUntill, Instant.ofEpochMilli(controller.getMacUnmuteTime(hashedMac)));
  }

  private String muteMac(final Instant length) {
    final String hashedMac = MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");
    controller.addMutedMac(hashedMac, length);
    return hashedMac;
  }
}
