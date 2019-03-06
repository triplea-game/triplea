package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;

public final class MutedMacControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final MutedMacDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getMutedMacDao();

  @Test
  public void testMuteMacForever() {
    muteMacForSeconds(Long.MAX_VALUE);
    assertTrue(isMacMuted());
    assertEquals(Optional.of(Instant.MAX), getMacUnmuteTime());
  }

  @Test
  public void testUnmuteMac() {
    final Instant muteUntil = muteMacForSeconds(100L);
    assertTrue(isMacMuted());
    assertEquals(Optional.of(muteUntil), getMacUnmuteTime());
    muteMacForSeconds(-10L);
    assertFalse(isMacMuted());
    assertEquals(Optional.empty(), getMacUnmuteTime());
  }

  @Test
  public void testMuteMacInThePast() {
    muteMacForSeconds(-10L);
    assertFalse(isMacMuted());
    assertEquals(Optional.empty(), getMacUnmuteTime());
  }

  @Test
  public void testMuteMacUpdate() {
    muteMacForSeconds(Long.MAX_VALUE);
    assertTrue(isMacMuted());
    assertEquals(Optional.of(Instant.MAX), getMacUnmuteTime());
    final Instant muteUntil = muteMacForSeconds(100L);
    assertTrue(isMacMuted());
    assertEquals(Optional.of(muteUntil), getMacUnmuteTime());
  }

  @Test
  public void testMuteMacUpdatesMutedUserAndModerator() {
    muteMacForSeconds(user, Long.MAX_VALUE, moderator);

    final User otherUser = newUser().withHashedMacAddress(user.getHashedMacAddress());
    final User otherModerator = newUser();
    muteMacForSeconds(otherUser, Long.MAX_VALUE, otherModerator);

    assertMutedUserEquals(otherUser);
    assertModeratorEquals(otherModerator);
  }

  private @Nullable Instant muteMacForSeconds(final long seconds) {
    return muteMacForSeconds(user, seconds, moderator);
  }

  private @Nullable Instant muteMacForSeconds(final User mutedUser, final long seconds, final User moderator) {
    final @Nullable Instant muteEnd = (seconds == Long.MAX_VALUE) ? null : Instant.now().plusSeconds(seconds);
    controller.addMutedMac(mutedUser, muteEnd, moderator);
    return muteEnd;
  }

  private Optional<Instant> getMacUnmuteTime() {
    return controller.getMacUnmuteTime(user.getHashedMacAddress());
  }

  private boolean isMacMuted() {
    return controller.isMacMuted(user.getHashedMacAddress());
  }

  private void assertMutedUserEquals(final User expected) {
    assertUserEquals(
        expected,
        "select username, ip, mac from muted_macs where mac=?",
        ps -> ps.setString(1, user.getHashedMacAddress()),
        "unknown muted hashed MAC address: " + user.getHashedMacAddress());
  }

  private void assertModeratorEquals(final User expected) {
    assertUserEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from muted_macs where mac=?",
        ps -> ps.setString(1, user.getHashedMacAddress()),
        "unknown muted hashed MAC address: " + user.getHashedMacAddress());
  }
}
