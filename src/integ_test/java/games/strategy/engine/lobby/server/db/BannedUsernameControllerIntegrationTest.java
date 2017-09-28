package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Test;

import games.strategy.util.Tuple;
import games.strategy.util.Util;

public class BannedUsernameControllerIntegrationTest {

  private final BannedUsernameController controller = spy(new BannedUsernameController());
  private final String username = Util.createUniqueTimeStamp();

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

  private Instant banUsernameForSeconds(final long length) {
    final Instant banEnd = length == Long.MAX_VALUE ? null : Instant.now().plusSeconds(length);
    controller.addBannedUsername(username, banEnd);
    return banEnd;
  }
}
