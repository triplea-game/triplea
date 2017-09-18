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
    final String username = muteUsername(null);
    assertTrue(controller.isMacMuted(username));
    assertEquals(Long.MAX_VALUE, controller.getMacUnmuteTime(username));
  }

  @Test
  public void testMuteMac() {
    final Instant banUntil = Instant.now();
    when(controller.now()).thenReturn(Instant.now().minusSeconds(1L));
    final String username = muteUsername(banUntil);
    assertTrue(controller.isMacMuted(username));
    assertEquals(banUntil, Instant.ofEpochMilli(controller.getMacUnmuteTime(username)));
    when(controller.now()).thenCallRealMethod();
    assertFalse(controller.isMacMuted(username));
  }

  @Test
  public void testMuteMacInThePast() {
    final Instant banUntil = Instant.now().minusSeconds(10L);
    final String username = muteUsername(banUntil);
    assertFalse(controller.isMacMuted(username));
  }

  @Test
  public void testMuteMacUpdate() {
    final String username = muteUsername(null);
    assertTrue(controller.isMacMuted(username));
    assertEquals(Long.MAX_VALUE, controller.getMacUnmuteTime(username));
    final Instant banTill = Instant.now().plusSeconds(100L);
    controller.addMutedMac(username, banTill);
    assertTrue(controller.isMacMuted(username));
    assertEquals(banTill, Instant.ofEpochMilli(controller.getMacUnmuteTime(username)));
  }

  private String muteUsername(final Instant length) {
    final String hashedMac = MD5Crypt.crypt(Util.createUniqueTimeStamp(), "MH");
    controller.addMutedMac(hashedMac, length);
    return hashedMac;
  }
}
