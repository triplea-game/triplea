package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.server.Moderator;
import games.strategy.util.Tuple;

public final class BannedUsernameControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final BannedUsernameController controller = spy(new BannedUsernameController());
  private final String username = newUsername();

  @Test
  public void testBanUsernameForever() {
    banUsernameForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  public void testBanUsername() {
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    when(controller.now()).thenReturn(banUntil.plusSeconds(1L));
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertFalse(usernameDetails2.getFirst());
    assertEquals(banUntil, usernameDetails2.getSecond().toInstant());
  }

  @Test
  public void testUnbanUsername() {
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    banUsernameForSeconds(-10L);
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertFalse(usernameDetails2.getFirst());
    assertNull(usernameDetails2.getSecond());
  }

  @Test
  public void testBanUsernameInThePast() {
    banUsernameForSeconds(-10L);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertFalse(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  public void testBanUsernameUpdate() {
    banUsernameForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertTrue(usernameDetails2.getFirst());
    assertEquals(banUntil, usernameDetails2.getSecond().toInstant());
  }

  @Test
  public void testBanUsernameUpdatesModerator() {
    banUsernameForSeconds(Long.MAX_VALUE, moderator);

    final Moderator otherModerator = newModerator();
    banUsernameForSeconds(Long.MAX_VALUE, otherModerator);

    assertModeratorForBannedUsernameEquals(otherModerator);
  }

  private Instant banUsernameForSeconds(final long length) {
    return banUsernameForSeconds(length, moderator);
  }

  private Instant banUsernameForSeconds(final long length, final Moderator moderator) {
    final Instant banEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addBannedUsername(username, banEnd, moderator);
    return banEnd;
  }

  private void assertModeratorForBannedUsernameEquals(final Moderator expected) {
    assertModeratorEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from banned_usernames where username=?",
        ps -> ps.setString(1, username),
        "unknown banned username: " + username);
  }
}
