package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

public class OpponentSelectorTest {

  @Test
  void testSingleDefender() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory japan = gameData.getMap().getTerritory("Japan");
    final Territory unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final GamePlayer japanese = japanese(gameData);
    OpponentSelector.AttackerAndDefender attAndDef;

    // Current Player is Russia
    // FIXME: No move was made, so there is no active player in the game and this fails.
    assertEquals(russians, gameData.getHistory().getActivePlayer());

    // Fight in Germany -> Germans defend
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(germany, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(germans, attAndDef.getDefender());

    // Fight in Japan -> Japans defend
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(japan, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(japanese, attAndDef.getDefender());

    // Fight in Britain -> Germans defend (British are allied; Germans are the first enemy)
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(unitedKingdom, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(germans, attAndDef.getDefender());
  }

  @Test
  void testMultipleDefenders() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory japan = gameData.getMap().getTerritory("Japan");
    final Territory unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final GamePlayer japanese = japanese(gameData);
    final GamePlayer americans = americans(gameData);
    OpponentSelector.AttackerAndDefender attAndDef;

    // Fill up the lands
    addTo(germany, infantry(gameData).create(100, japanese));
    addTo(japan, infantry(gameData).create(100, germans));
    addTo(unitedKingdom, infantry(gameData).create(100, americans));

    // Current Player is Russia
    // FIXME: No move was made, so there is no active player in the game and this fails.
    assertEquals(russians, gameData.getHistory().getActivePlayer());

    // Fight in Germany -> 100 Japanese defend
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(germany, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(japanese, attAndDef.getDefender());

    // Fight in Japan -> 100 Germans defend
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(japan, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(germans, attAndDef.getDefender());

    // Fight in Britain -> Germans defend (British & Americans are allied; Germans are the first
    // enemy)
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(unitedKingdom, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(germans, attAndDef.getDefender());
  }

  @Test
  void testMixedDefendersAlliesAndEnemies() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Territory japan = gameData.getMap().getTerritory("Japan");
    final Territory unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    final GamePlayer russians = russians(gameData);
    final GamePlayer germans = germans(gameData);
    final GamePlayer japanese = japanese(gameData);
    final GamePlayer americans = americans(gameData);
    OpponentSelector.AttackerAndDefender attAndDef;

    // Fill territory with a mix of allies and foes.
    addTo(japan, infantry(gameData).create(100, americans));

    // Current Player is Russia
    // FIXME: No move was made, so there is no active player in the game and this fails.
    assertEquals(russians, gameData.getHistory().getActivePlayer());

    // Fight in Japan -> Japanese defend, Americans are allied
    attAndDef = OpponentSelector.with(gameData).getAttackerAndDefender(japan, gameData);
    assertEquals(russians, attAndDef.getAttacker());
    assertEquals(japanese, attAndDef.getDefender());
  }
}
