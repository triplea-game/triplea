package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.util.Tuple;

final class BannedMacControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {

  private final BannedMacDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getBannedMacDao();

  @Test
  void testBanMacForever() {
    banUser();
    final Tuple<Boolean, Timestamp> macDetails = isMacBanned(Instant.now());
    assertTrue(macDetails.getFirst());
    assertNull(macDetails.getSecond());
  }

  @Test
  void testBanMac() {
    final Instant expiry = Instant.now().plusSeconds(100L);
    banUserWithExpiry(expiry);

    final Tuple<Boolean, Timestamp> macDetails = isMacBanned(Instant.now());
    assertTrue(macDetails.getFirst());
    assertEquals(expiry, macDetails.getSecond().toInstant());

    final Tuple<Boolean, Timestamp> macDetails2 = isMacBanned(expiry.plusSeconds(1L));
    assertFalse(macDetails2.getFirst());
    assertEquals(expiry, macDetails2.getSecond().toInstant());
  }

  @Test
  void testBanMacUpdate() {
    banUser();
    final Instant expiry = Instant.now().plusSeconds(100L);
    banUserWithExpiry(expiry);

    final Tuple<Boolean, Timestamp> macDetails = isMacBanned(Instant.now());
    assertTrue(macDetails.getFirst());
    assertEquals(expiry, macDetails.getSecond().toInstant());

    final Tuple<Boolean, Timestamp> futureDetails = isMacBanned(expiry.plusSeconds(200L));
    assertFalse(futureDetails.getFirst());
    assertEquals(expiry, futureDetails.getSecond().toInstant());
  }

  @Test
  void testBanMacUpdatesBannedUserAndModerator() {
    banUser();
    assertModeratorEquals(moderator);
  }

  private void banUser() {
    banUserWithExpiry(null);
  }

  private void banUserWithExpiry(final Instant expiry) {
    controller.addBannedMac(user, expiry, moderator);
  }

  private Tuple<Boolean, /* @Nullable */ Timestamp> isMacBanned(final Instant nowTime) {
    return controller.isMacBanned(nowTime, user.getHashedMacAddress());
  }

  private void assertModeratorEquals(final User expected) {
    assertUserEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from banned_macs where mac=?",
        ps -> ps.setString(1, user.getHashedMacAddress()),
        "unknown banned hashed MAC address: " + user.getHashedMacAddress());
  }
}
