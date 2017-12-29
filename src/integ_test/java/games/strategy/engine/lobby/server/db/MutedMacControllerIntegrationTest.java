package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import games.strategy.util.Util;

public class MutedMacControllerIntegrationTest {

  private final MutedMacController controller = spy(new MutedMacController());
  private final String hashedMac = games.strategy.util.MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");

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

  private Instant muteMacForSeconds(final long length) {
    final Instant muteEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addMutedMac(hashedMac, muteEnd);
    return muteEnd;
  }
}
