package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.Test;

import games.strategy.util.Util;

public class MutedUsernameControllerTest {

  private final MutedUsernameController controller = spy(new MutedUsernameController());

  @Test
  public void testMuteUsernameForever() {
    final String username = muteUsername(null);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Long.MAX_VALUE, controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsername() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    final String username = muteUsername(muteUntil);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getUsernameUnmuteTime(username)));
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testUnmuteUsername() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    final String username = muteUsername(muteUntil);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getUsernameUnmuteTime(username)));
    controller.addMutedUsername(username, Instant.now().minusSeconds(10L));
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testMuteUsernameInThePast() {
    final Instant muteUntil = Instant.now().minusSeconds(10L);
    final String username = muteUsername(muteUntil);
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testMuteUsernameUpdate() {
    final String username = muteUsername(null);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Long.MAX_VALUE, controller.getUsernameUnmuteTime(username));
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    controller.addMutedUsername(username, muteUntil);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(muteUntil, Instant.ofEpochMilli(controller.getUsernameUnmuteTime(username)));
  }

  private String muteUsername(final Instant length) {
    final String username = Util.createUniqueTimeStamp();
    controller.addMutedUsername(username, length);
    return username;
  }
}
