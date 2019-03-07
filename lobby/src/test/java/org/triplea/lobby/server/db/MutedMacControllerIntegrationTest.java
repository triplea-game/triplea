package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;

final class MutedMacControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final MutedMacDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getMutedMacDao();

  @Test
  void testMuteMacForever() {
    muteMac();
    assertTrue(isMacMuted(Instant.now()));
    assertEquals(Optional.of(Instant.MAX), getMacUnmuteTime());
  }

  @Test
  void testMuteMac() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    muteMac(muteUntil);
    assertMutedUserEquals(user);
    assertTrue(isMacMuted(Instant.now()));
    assertEquals(Optional.of(muteUntil), getMacUnmuteTime());

    assertFalse(isMacMuted(muteUntil.plusSeconds(1L)));
  }

  @Test
  void testUnmuteMac() {
    final Instant muteUntil = Instant.now().plusSeconds(100L);
    muteMac(muteUntil);

    assertEquals(Optional.of(muteUntil), getMacUnmuteTime());

    muteMac(Instant.now().plusSeconds(10L));
    assertFalse(isMacMuted(Instant.now().plusSeconds(20L)));
  }

  @Test
  void testMuteMacInThePast() {
    muteMac(Instant.now().plusSeconds(10L));
    assertFalse(isMacMuted(Instant.now().plusSeconds(20L)));
  }

  @Test
  void testMuteMacUpdate() {
    muteMac();
    assertTrue(isMacMuted(Instant.now()));
    assertEquals(Optional.of(Instant.MAX), getMacUnmuteTime());

    final Instant muteUntil = Instant.now().plusSeconds(100L);
    muteMac(muteUntil);
    assertTrue(isMacMuted(Instant.now()));
    assertEquals(Optional.of(muteUntil), getMacUnmuteTime());
  }

  @Test
  void testMuteMacUpdatesMutedUserAndModerator() {
    muteMac();

    assertModeratorEquals(moderator);
  }

  private void muteMac() {
    muteMac(null);
  }

  private void muteMac(final Instant expiry) {
    controller.addMutedMac(user, expiry, moderator);
  }

  private Optional<Instant> getMacUnmuteTime() {
    return controller.getMacUnmuteTime(user.getHashedMacAddress());
  }

  private boolean isMacMuted(final Instant checkTime) {
    return controller.isMacMuted(checkTime, user.getHashedMacAddress());
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
