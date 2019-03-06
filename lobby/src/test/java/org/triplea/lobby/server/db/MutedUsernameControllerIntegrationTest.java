package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;

final class MutedUsernameControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final MutedUsernameDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getMutedUsernameDao();

  @Test
  void testMuteUsername() {
    muteUsername(null);
    assertTrue(isUsernameMuted(Instant.now()));

    final Instant muteUntil = Instant.now().plusSeconds(100L);
    muteUsername(muteUntil);
    assertTrue(isUsernameMuted(Instant.now()));
    assertEquals(Optional.of(muteUntil), getUsernameUnmuteTime());
  }

  @Test
  void testMuteUsernameWithExpiry() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    muteUsername(muteUntil);
    assertTrue(isUsernameMuted(Instant.now()));
    assertEquals(Optional.of(muteUntil), getUsernameUnmuteTime());

    assertFalse(isUsernameMuted(muteUntil.plusSeconds(1L)));
  }

  @Test
  void testUnmuteUsername() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    muteUsername(muteUntil);
    muteUsername(Instant.now().minusSeconds(10L));

    assertFalse(isUsernameMuted(Instant.now()));
    assertEquals(Optional.empty(), getUsernameUnmuteTime());
  }

  @Test
  void testMuteUsernameInThePast() {
    final Instant muteUntil = Instant.now().minusSeconds(100L);
    muteUsername(muteUntil);

    assertFalse(isUsernameMuted(Instant.now()));
    assertEquals(Optional.empty(), getUsernameUnmuteTime());
  }


  @Test
  void testMuteUsernameUpdatesMutedUserAndModerator() {
    muteUsername(Instant.now().plusSeconds(10L));
    assertModeratorEquals(moderator);
  }


  private void muteUsername(final Instant expiry) {
    controller.addMutedUsername(user, expiry, moderator);
  }

  private Optional<Instant> getUsernameUnmuteTime() {
    return controller.getUsernameUnmuteTime(user.getUsername());
  }

  private boolean isUsernameMuted(final Instant checkTime) {
    return controller.isUsernameMuted(checkTime, user.getUsername());
  }

  private void assertModeratorEquals(final User expected) {
    assertUserEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from muted_usernames where username=?",
        ps -> ps.setString(1, user.getUsername()),
        "unknown muted username: " + user.getUsername());
  }
}
