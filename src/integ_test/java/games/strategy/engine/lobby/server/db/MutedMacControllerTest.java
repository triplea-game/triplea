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
  final String hashedMac = MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");

  @Test
  public void testMuteMacForever() {
    muteMac(Long.MAX_VALUE);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Long.MAX_VALUE, controller.getMacUnmuteTime(hashedMac));
  }

  @Test
  public void testMuteMac() {
    final Instant muteUntil = muteMac(100L);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getMacUnmuteTime(hashedMac)));
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isMacMuted(hashedMac));
  }

  @Test
  public void testUnmuteMac() {
    final Instant muteUntil = muteMac(100L);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getMacUnmuteTime(hashedMac)));
    controller.addMutedMac(hashedMac, Instant.now().minusSeconds(10L));
    assertFalse(controller.isMacMuted(hashedMac));
  }

  @Test
  public void testMuteMacInThePast() {
    muteMac(-10L);
    assertFalse(controller.isMacMuted(hashedMac));
  }

  @Test
  public void testMuteMacUpdate() {
    muteMac(Long.MAX_VALUE);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(Long.MAX_VALUE, controller.getMacUnmuteTime(hashedMac));
    final Instant muteUntill = Instant.now().plusSeconds(100L);
    controller.addMutedMac(hashedMac, muteUntill);
    assertTrue(controller.isMacMuted(hashedMac));
    assertEquals(muteUntill, Instant.ofEpochMilli(controller.getMacUnmuteTime(hashedMac)));
  }

  private Instant muteMac(final long length) {
    final Instant muteEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addMutedMac(hashedMac, muteEnd);
    return muteEnd;
  }
}
