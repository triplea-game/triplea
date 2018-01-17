package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.Moderator;

public final class MutedUsernameControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final MutedUsernameController controller = spy(new MutedUsernameController());
  private final String username = newUsername();

  @Test
  public void testMuteUsernameForever() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(Instant.MAX), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(muteUntil), controller.getUsernameUnmuteTime(username));
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(controller.isUsernameMuted(username));
    assertEquals(Optional.empty(), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testUnmuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(muteUntil), controller.getUsernameUnmuteTime(username));
    muteUsernameForSeconds(-10L);
    assertFalse(controller.isUsernameMuted(username));
    assertEquals(Optional.empty(), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsernameInThePast() {
    muteUsernameForSeconds(-10L);
    assertFalse(controller.isUsernameMuted(username));
    assertEquals(Optional.empty(), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsernameUpdate() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(Instant.MAX), controller.getUsernameUnmuteTime(username));
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(controller.isUsernameMuted(username));
    assertEquals(Optional.of(muteUntil), controller.getUsernameUnmuteTime(username));
  }

  @Test
  public void testMuteUsernameUpdatesModerator() {
    muteUsernameForSeconds(Long.MAX_VALUE, moderator);

    final Moderator otherModerator = newModerator();
    muteUsernameForSeconds(Long.MAX_VALUE, otherModerator);

    assertModeratorForMutedUsernameEquals(otherModerator);
  }

  private Instant muteUsernameForSeconds(final long length) {
    return muteUsernameForSeconds(length, moderator);
  }

  private Instant muteUsernameForSeconds(final long length, final Moderator moderator) {
    final Instant muteEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addMutedUsername(username, muteEnd, moderator);
    return muteEnd;
  }

  private void assertModeratorForMutedUsernameEquals(final Moderator expected) {
    assertModeratorEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from muted_usernames where username=?",
        ps -> ps.setString(1, username),
        "unknown muted username: " + username);
  }
}
