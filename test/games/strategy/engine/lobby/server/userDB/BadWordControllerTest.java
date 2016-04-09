package games.strategy.engine.lobby.server.userDB;

import games.strategy.util.Util;
import junit.framework.TestCase;

public class BadWordControllerTest extends TestCase { 
  public void testCRUD() {
    final BadWordController controller = new BadWordController();
    final String word = Util.createUniqueTimeStamp();
    controller.addBadWord(word);
    assertTrue(controller.list().contains(word));
    controller.removeBannedWord(word);
    assertFalse(controller.list().contains(word));
  }
}
