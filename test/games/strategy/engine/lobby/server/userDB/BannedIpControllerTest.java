package games.strategy.engine.lobby.server.userDB;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import games.strategy.util.Util;

public class BannedIpControllerTest {

  @Test
  public void testCRUD() {
    final BannedIpController controller = new BannedIpController();
    final String ip = Util.createUniqueTimeStamp();
    controller.addBannedIp(ip);
    assertTrue(controller.isIpBanned(ip).getFirst());
    controller.removeBannedIp(ip);
    assertFalse(controller.isIpBanned(ip).getFirst());
  }

  @Test
  public void testNonBannedIp() {
    final BannedIpController controller = new BannedIpController();
    assertFalse(controller.isIpBanned(Util.createUniqueTimeStamp()).getFirst());
  }

  @Test
  public void testBanExpires() {
    final BannedIpController controller = new BannedIpController();
    final String ip = Util.createUniqueTimeStamp();
    final Date expire = new Date(System.currentTimeMillis() - 5000);
    controller.addBannedIp(ip, expire);
    assertFalse(controller.isIpBanned(ip).getFirst());
  }

  @Test
  public void testUpdate() {
    final BannedIpController controller = new BannedIpController();
    final String ip = Util.createUniqueTimeStamp();
    final Date expire = new Date(System.currentTimeMillis() - 5000);
    controller.addBannedIp(ip, expire);
    controller.addBannedIp(ip);
    assertTrue(controller.isIpBanned(ip).getFirst());
  }
}
