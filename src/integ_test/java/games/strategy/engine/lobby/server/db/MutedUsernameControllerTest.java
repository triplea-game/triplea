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
    final Instant banUntil = Instant.now();
    when(controller.now()).thenReturn(Instant.now().minusSeconds(1L));
    final String username = muteUsername(banUntil);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(banUntil, Instant.ofEpochMilli(controller.getUsernameUnmuteTime(username)));
    when(controller.now()).thenCallRealMethod();
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testMuteUsernameInThePast() {
    final Instant banUntil = Instant.now().minusSeconds(10L);
    final String username = muteUsername(banUntil);
    assertFalse(controller.isUsernameMuted(username));
  }

  @Test
  public void testMuteUsernameUpdate() {
    final String username = muteUsername(null);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Long.MAX_VALUE, controller.getUsernameUnmuteTime(username));
    final Instant banTill = Instant.now().plusSeconds(100L);
    controller.addMutedUsername(username, banTill);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(banTill, Instant.ofEpochMilli(controller.getUsernameUnmuteTime(username)));
  }

  private String muteUsername(final Instant length) {
    final String username = Util.createUniqueTimeStamp();
    controller.addMutedUsername(username, length);
    return username;
  }
}
