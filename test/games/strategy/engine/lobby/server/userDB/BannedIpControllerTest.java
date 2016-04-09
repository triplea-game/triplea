package games.strategy.engine.lobby.server.userDB;

import java.util.Date;

import games.strategy.util.Util;
import junit.framework.TestCase;

public class BannedIpControllerTest extends TestCase { 
  public void testCRUD() {
    final BannedIpController controller = new BannedIpController();
    final String ip = Util.createUniqueTimeStamp();
    controller.addBannedIp(ip);
    assertTrue(controller.isIpBanned(ip).getFirst());
    controller.removeBannedIp(ip);
    assertFalse(controller.isIpBanned(ip).getFirst());
  }

  public void testNonBannedIp() {
    final BannedIpController controller = new BannedIpController();
    assertFalse(controller.isIpBanned(Util.createUniqueTimeStamp()).getFirst());
  }

  public void testBanExpires() {
    final BannedIpController controller = new BannedIpController();
    final String ip = Util.createUniqueTimeStamp();
    final Date expire = new Date(System.currentTimeMillis() - 5000);
    controller.addBannedIp(ip, expire);
    assertFalse(controller.isIpBanned(ip).getFirst());
  }

  public void testUpdate() {
    final BannedIpController controller = new BannedIpController();
    final String ip = Util.createUniqueTimeStamp();
    final Date expire = new Date(System.currentTimeMillis() - 5000);
    controller.addBannedIp(ip, expire);
    controller.addBannedIp(ip);
    assertTrue(controller.isIpBanned(ip).getFirst());
  }
}
