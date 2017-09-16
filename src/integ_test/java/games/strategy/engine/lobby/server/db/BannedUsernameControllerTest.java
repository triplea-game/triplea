package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.Test;

import games.strategy.util.ThreadUtil;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

public class BannedUsernameControllerTest {

  private final BannedUsernameController controller = new BannedUsernameController();

  @Test
  public void testBanUsernameForever() {
    final String hashedUsername = banUsername(null);
    final Tuple<Boolean, Timestamp> UsernameDetails = controller.isUsernameBanned(hashedUsername);
    assertTrue(UsernameDetails.getFirst());
    assertNull(UsernameDetails.getSecond());
  }

  @Test
  public void testBanUsername() {
    final Instant banUntil = Instant.now().plusSeconds(2L);
    final String hashedUsername = banUsername(banUntil);
    final Tuple<Boolean, Timestamp> UsernameDetails = controller.isUsernameBanned(hashedUsername);
    assertTrue(UsernameDetails.getFirst());
    assertEquals(banUntil, UsernameDetails.getSecond().toInstant());
    while (banUntil.isAfter(Instant.now())) {
      ThreadUtil.sleep(100);
    }
    final Tuple<Boolean, Timestamp> UsernameDetails2 = controller.isUsernameBanned(hashedUsername);
    assertFalse(UsernameDetails2.getFirst());
    assertEquals(banUntil, UsernameDetails2.getSecond().toInstant());
  }

  @Test
  public void testBanUsernameInThePast() {
    final Instant banUntil = Instant.now().minusSeconds(10L);
    final String hashedUsername = banUsername(banUntil);
    final Tuple<Boolean, Timestamp> UsernameDetails = controller.isUsernameBanned(hashedUsername);
    assertFalse(UsernameDetails.getFirst());
    assertEquals(banUntil, UsernameDetails.getSecond().toInstant());
  }

  @Test
  public void testBanUsernameUpdate() {
    final String hashedUsername = banUsername(null);
    final Tuple<Boolean, Timestamp> UsernameDetails = controller.isUsernameBanned(hashedUsername);
    assertTrue(UsernameDetails.getFirst());
    assertNull(UsernameDetails.getSecond());
    final Instant banTill = Instant.now().plusSeconds(100L);
    controller.addBannedUsername(hashedUsername, banTill);
    final Tuple<Boolean, Timestamp> UsernameDetails2 = controller.isUsernameBanned(hashedUsername);
    assertTrue(UsernameDetails2.getFirst());
    assertEquals(banTill, UsernameDetails2.getSecond().toInstant());
  }

  private String banUsername(final Instant length) {
    final String hashedUsername = Util.createUniqueTimeStamp();
    controller.addBannedUsername(hashedUsername, length);
    return hashedUsername;
  }
}
