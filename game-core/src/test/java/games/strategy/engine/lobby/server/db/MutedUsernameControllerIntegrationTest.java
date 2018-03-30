package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.User;

public final class MutedUsernameControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final MutedUsernameController controller = spy(new MutedUsernameController());

  @Test
  public void testMuteUsernameForever() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(isUsernameMuted());
    assertEquals(Optional.of(Instant.MAX), getUsernameUnmuteTime());
  }

  @Test
  public void testMuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertMutedUserEquals(user);
    assertTrue(isUsernameMuted());
    assertEquals(Optional.of(muteUntil), getUsernameUnmuteTime());
    when(controller.now()).thenReturn(muteUntil.plusSeconds(1L));
    assertFalse(isUsernameMuted());
    assertEquals(Optional.empty(), getUsernameUnmuteTime());
  }

  @Test
  public void testUnmuteUsername() {
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(isUsernameMuted());
    assertEquals(Optional.of(muteUntil), getUsernameUnmuteTime());
    muteUsernameForSeconds(-10L);
    assertFalse(isUsernameMuted());
    assertEquals(Optional.empty(), getUsernameUnmuteTime());
  }

  @Test
  public void testMuteUsernameInThePast() {
    muteUsernameForSeconds(-10L);
    assertFalse(isUsernameMuted());
    assertEquals(Optional.empty(), getUsernameUnmuteTime());
  }

  @Test
  public void testMuteUsernameUpdate() {
    muteUsernameForSeconds(Long.MAX_VALUE);
    assertTrue(isUsernameMuted());
    assertEquals(Optional.of(Instant.MAX), getUsernameUnmuteTime());
    final Instant muteUntil = muteUsernameForSeconds(100L);
    assertTrue(isUsernameMuted());
    assertEquals(Optional.of(muteUntil), getUsernameUnmuteTime());
  }

  @Test
  public void testMuteUsernameUpdatesMutedUserAndModerator() {
    muteUsernameForSeconds(user, Long.MAX_VALUE, moderator);

    final User otherUser = newUser().withUsername(user.getUsername());
    final User otherModerator = newUser();
    muteUsernameForSeconds(otherUser, Long.MAX_VALUE, otherModerator);

    assertMutedUserEquals(otherUser);
    assertModeratorEquals(otherModerator);
  }

  private @Nullable Instant muteUsernameForSeconds(final long seconds) {
    return muteUsernameForSeconds(user, seconds, moderator);
  }

  private @Nullable Instant muteUsernameForSeconds(final User mutedUser, final long seconds, final User moderator) {
    final @Nullable Instant muteEnd = (seconds == Long.MAX_VALUE) ? null : Instant.now().plusSeconds(seconds);
    controller.addMutedUsername(mutedUser, muteEnd, moderator);
    return muteEnd;
  }

  private Optional<Instant> getUsernameUnmuteTime() {
    return controller.getUsernameUnmuteTime(user.getUsername());
  }

  private boolean isUsernameMuted() {
    return controller.isUsernameMuted(user.getUsername());
  }

  private void assertMutedUserEquals(final User expected) {
    assertUserEquals(
        expected,
        "select username, ip, mac from muted_usernames where username=?",
        ps -> ps.setString(1, user.getUsername()),
        "unknown muted username: " + user.getUsername());
  }

  private void assertModeratorEquals(final User expected) {
    assertUserEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from muted_usernames where username=?",
        ps -> ps.setString(1, user.getUsername()),
        "unknown muted username: " + user.getUsername());
  }
}
