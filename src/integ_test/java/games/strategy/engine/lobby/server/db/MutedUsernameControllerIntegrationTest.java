package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import games.strategy.util.Util;

public class MutedUsernameControllerIntegrationTest {

  private final MutedUsernameController controller = spy(new MutedUsernameController());
  private final String username = Util.createUniqueTimeStamp();

  @Test
  public void testMuteUsernameForever() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isUsernameMuted(username));
    assertFalse(controller.getUsernameUnmuteTime(username).isPresent());
  }

  @Test
  public void testMuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(muteUntil, controller.getUsernameUnmuteTime(username).get());
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testUnmuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(muteUntil, controller.getUsernameUnmuteTime(username).get());
    controller.addMutedUsername(username, Instant.now().minusSeconds(10L));
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testMuteUsernameInThePast() {
    muteUsernameForSeconds(-10L);
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testMuteUsernameUpdate() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isUsernameMuted(username));
    assertFalse(controller.getUsernameUnmuteTime(username).isPresent());
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(muteUntil, controller.getUsernameUnmuteTime(username).get());
  }

  private Instant muteUsernameForSeconds(final long length) {
    final Instant muteEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addMutedUsername(username, muteEnd);
    return muteEnd;
  }
}
