package games.strategy.triplea.attachments;

import static games.strategy.triplea.Constants.PUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.security.SecureRandom;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RulesAttachmentTest {

  /**
   * "Victory" map is just a branch/mod of Pact of Steel 2. POS2 is an actual game with good
   * gameplay that we don't want to mess with, so "Victory" is more of an xml purely for testing
   * purposes, and probably should never be played.
   */
  private final GameData gameData = TestMapGameData.VICTORY_TEST.getGameData();

  private final RulesAttachment attachment = new RulesAttachment("Test attachment", null, gameData);

  @Nested
  class HaveResources {

    private final GamePlayer italians = GameDataTestUtil.italians(gameData);
    private final GamePlayer germans = GameDataTestUtil.germans(gameData);
    private final String fuel = "Fuel";
    private final String ore = "Ore";
    private final String addString = "add";
    private final String sumString = "SUM";

    /* Length test for haveResources */
    @Test
    void setHaveResourcesInvalidLength() {
      assertThrows(GameParseException.class, () -> attachment.setHaveResources(""));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources(":add"));

      assertThrows(GameParseException.class, () -> attachment.setHaveResources("a"));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("a:add"));
    }

    /* Invalid arguments for haveResources */
    @Test
    void setHaveResourcesInvalidArgs() {
      /* Not a number (NAN) test */
      assertThrows(IllegalArgumentException.class, () -> attachment.setHaveResources("NAN:PUs"));
      assertThrows(
          IllegalArgumentException.class, () -> attachment.setHaveResources("NAN:add:PUs"));
      /* -1 value test */
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("0:PUs"));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("0:add:PUs"));
      /* Not a resource test */
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("1:NOT A RESOURCE"));
      assertThrows(
          GameParseException.class, () -> attachment.setHaveResources("1:Sum:NOT A RESOURCE"));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("0:w"));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("0:w:e"));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("0:add:w"));
      assertThrows(GameParseException.class, () -> attachment.setHaveResources("0:add:w:e"));
    }

    /* Testing stored values with getHaveResources */
    @Test
    void setHaveResourcesTest() throws Exception {
      final SecureRandom rand = new SecureRandom();
      final String random1 = Integer.toString(Math.abs(rand.nextInt()));
      final String[] expected1 = new String[] {random1, PUS};

      attachment.setHaveResources(concatWithColon(random1, addString, PUS));
      assertEquals(expected1[0], attachment.getHaveResources()[0]);
      assertEquals(expected1[1], attachment.getHaveResources()[2]);
    }

    /* Testing checkHaveResources */
    @Test
    void testCheckHaveResources() throws Exception {
      final int italianFuelAmount = italians.getResources().getQuantity(fuel);
      final int italianPuAmount = italians.getResources().getQuantity(PUS);
      final int italianOreAmount = italians.getResources().getQuantity(ore);
      final int germanFuelAmount = germans.getResources().getQuantity(fuel);
      final int germanPuAmount = germans.getResources().getQuantity(PUS);
      final int germanOreAmount = germans.getResources().getQuantity(ore);
      final int testItalianPU = italianPuAmount;
      final int testItalianResources = italianOreAmount + italianFuelAmount + italianPuAmount;
      final int testPUs = testItalianPU + germanPuAmount;
      final int testResources = testItalianResources + germanPuAmount + germanFuelAmount + germanOreAmount;

      /* testing with 1 player */
      final List<GamePlayer> players = List.of(italians);
      attachment.setHaveResources(concatWithColon(String.valueOf(testItalianPU), PUS));
      assertTrue(attachment.checkHaveResources(players));
      attachment.setHaveResources(
          concatWithColon(String.valueOf(testItalianResources), addString, PUS));
      assertFalse(attachment.checkHaveResources(players));
      attachment.setHaveResources(
          concatWithColon(String.valueOf(testItalianResources), addString, PUS, fuel));
      assertFalse(attachment.checkHaveResources(players));
      attachment.setHaveResources(
          concatWithColon(String.valueOf(testItalianResources), addString, PUS, fuel, ore));
      assertTrue(attachment.checkHaveResources(players));

      /* testing with 2 players */
      final List<GamePlayer> players1 = List.of(italians, germans);
      attachment.setHaveResources(concatWithColon(String.valueOf(testPUs), PUS));
      assertFalse(attachment.checkHaveResources(players1));
      attachment.setHaveResources(concatWithColon(String.valueOf(testPUs), sumString, PUS));
      assertTrue(attachment.checkHaveResources(players1));
      attachment.setHaveResources(
          concatWithColon(String.valueOf(testResources), sumString, PUS));
      assertFalse(attachment.checkHaveResources(players1));
      attachment.setHaveResources(
          concatWithColon(String.valueOf(testResources), sumString, PUS, fuel));
      assertFalse(attachment.checkHaveResources(players1));
      attachment.setHaveResources(
          concatWithColon(String.valueOf(testResources), sumString, PUS, fuel, ore));
      assertTrue(attachment.checkHaveResources(players1));

    }
      @Test
    private String concatWithColon(final String... args) {
      return String.join(":", args);
    }
  }
}
