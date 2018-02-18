package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;

import games.strategy.util.Util;

public class BadWordControllerIntegrationTest {

  @Test
  public void testInsertAndRemoveBadWord() throws Exception {
    final BadWordController controller = new BadWordController();
    final String word = Util.createUniqueTimeStamp();
    controller.addBadWord(word);
    assertTrue(controller.list().contains(word));
    removeBadWord(word);
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

  private static void removeBadWord(final String word) throws Exception {
    try (Connection con = Database.getPostgresConnection();
        PreparedStatement ps = con.prepareStatement("delete from bad_words where word = ?")) {
      ps.setString(1, word);
      ps.execute();
      con.commit();
    }
  }
}
