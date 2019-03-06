package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.util.Tuple;

public final class BannedMacControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final BannedMacDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getBannedMacDao();

  @Test
  public void testBanMacForever() {
    banMacForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> macDetails = isMacBanned();
    assertTrue(macDetails.getFirst());
    assertNull(macDetails.getSecond());
  }

  @Test
  void testBanMac() {
    final Instant banUntil = banMacForSeconds(100L);
    assertBannedUserEquals(user);
    final Tuple<Boolean, Timestamp> macDetails = isMacBanned();
    assertTrue(macDetails.getFirst());
    assertEquals(banUntil, macDetails.getSecond().toInstant());
    when(controller.now()).thenReturn(banUntil.plusSeconds(1L));
    final Tuple<Boolean, Timestamp> macDetails2 = isMacBanned();
    assertFalse(macDetails2.getFirst());
    assertEquals(banUntil, macDetails2.getSecond().toInstant());
  }

  @Test
  void testUnbanMac() {
    final Instant banUntil = banMacForSeconds(100L);
    final Tuple<Boolean, Timestamp> macDetails = isMacBanned();
    assertTrue(macDetails.getFirst());
    assertEquals(banUntil, macDetails.getSecond().toInstant());
    banMacForSeconds(-10L);
    final Tuple<Boolean, Timestamp> macDetails2 = isMacBanned();
    assertFalse(macDetails2.getFirst());
    assertNull(macDetails2.getSecond());
  }

  @Test
  public void testBanMacInThePast() {
    banMacForSeconds(-10L);
    final Tuple<Boolean, Timestamp> macDetails = isMacBanned();
    assertFalse(macDetails.getFirst());
    assertNull(macDetails.getSecond());
  }

  @Test
  public void testBanMacUpdate() {
    banMacForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> macDetails = isMacBanned();
    assertTrue(macDetails.getFirst());
    assertNull(macDetails.getSecond());
    final Instant banUntil = banMacForSeconds(100L);
    final Tuple<Boolean, Timestamp> macDetails2 = isMacBanned();
    assertTrue(macDetails2.getFirst());
    assertEquals(banUntil, macDetails2.getSecond().toInstant());
  }

  @Test
  public void testBanMacUpdatesBannedUserAndModerator() {
    banMacForSeconds(user, Long.MAX_VALUE, moderator);

    final User otherUser = newUser().withHashedMacAddress(user.getHashedMacAddress());
    final User otherModerator = newUser();
    banMacForSeconds(otherUser, Long.MAX_VALUE, otherModerator);

    assertBannedUserEquals(otherUser);
    assertModeratorEquals(otherModerator);
  }

  private @Nullable Instant banMacForSeconds(final long seconds) {
    return banMacForSeconds(user, seconds, moderator);
  }

  private @Nullable Instant banMacForSeconds(final User bannedUser, final long seconds, final User moderator) {
    final @Nullable Instant banEnd = (seconds == Long.MAX_VALUE) ? null : Instant.now().plusSeconds(seconds);
    controller.addBannedMac(bannedUser, banEnd, moderator);
    return banEnd;
  }

  private Tuple<Boolean, /* @Nullable */ Timestamp> isMacBanned() {
    return controller.isMacBanned(user.getHashedMacAddress());
  }

  private void assertBannedUserEquals(final User expected) {
    assertUserEquals(
        expected,
        "select username, ip, mac from banned_macs where mac=?",
        ps -> ps.setString(1, user.getHashedMacAddress()),
        "unknown banned hashed MAC address: " + user.getHashedMacAddress());
  }

  private void assertModeratorEquals(final User expected) {
    assertUserEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from banned_macs where mac=?",
        ps -> ps.setString(1, user.getHashedMacAddress()),
        "unknown banned hashed MAC address: " + user.getHashedMacAddress());
  }
}
