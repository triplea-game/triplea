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

public class BannedUsernameControllerTest {

  private final BannedUsernameController controller = spy(new BannedUsernameController());

  @Test
  public void testBanUsernameForever() {
    final String username = banUsername(null);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  public void testBanUsername() {
    final Instant banUntil = Instant.now().plusSeconds(100L);
    final String username = banUsername(banUntil);
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
    final Instant banUntil = Instant.now().plusSeconds(100L);
    final String username = banUsername(banUntil);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    controller.addBannedUsername(username, Instant.now().minusSeconds(10L));
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertFalse(usernameDetails2.getFirst());
    assertNull(usernameDetails2.getSecond());
  }

  @Test
  public void testBanUsernameInThePast() {
    final Instant banUntil = Instant.now().minusSeconds(10L);
    final String username = banUsername(banUntil);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertFalse(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  public void testBanUsernameUpdate() {
    final String username = banUsername(null);
    final Tuple<Boolean, Timestamp> usernameDetails = controller.isUsernameBanned(username);
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
    final Instant banUntill = Instant.now().plusSeconds(100L);
    controller.addBannedUsername(username, banUntill);
    final Tuple<Boolean, Timestamp> usernameDetails2 = controller.isUsernameBanned(username);
    assertTrue(usernameDetails2.getFirst());
    assertEquals(banUntill, usernameDetails2.getSecond().toInstant());
  }

  private String banUsername(final Instant length) {
    final String username = Util.createUniqueTimeStamp();
    controller.addBannedUsername(username, length);
    return username;
  }
}
