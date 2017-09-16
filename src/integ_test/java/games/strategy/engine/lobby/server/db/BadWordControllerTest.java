package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import games.strategy.util.Util;

public class BadWordControllerTest {

  @Test
  public void testInsertAndRemoveBadWord() {
    final BadWordController controller = new BadWordController();
    final String word = Util.createUniqueTimeStamp();
    controller.addBadWord(word);
    assertTrue(controller.list().contains(word));
    controller.removeBannedWord(word);
    assertFalse(controller.list().contains(word));
  }

  @Test
  public void testDuplicateBadWord() {
    final BadWordController controller = new BadWordController();
    final String word = Util.createUniqueTimeStamp();
    final int previousCount = controller.list().size();
    controller.addBadWord(word);
    controller.addBadWord(word);
    assertTrue(controller.list().contains(word));
    assertEquals(previousCount + 1, controller.list().size());
  }
}
