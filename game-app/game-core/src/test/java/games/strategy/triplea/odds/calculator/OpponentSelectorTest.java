package games.strategy.triplea.odds.calculator;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpponentSelectorTest {

  private GameData gameData;
  private Territory germany;
  private Territory japan;
  private Territory unitedKingdom;
  private GamePlayer russians;
  private GamePlayer germans;
  private GamePlayer japanese;
  private GamePlayer americans;
  private List<GamePlayer> players;
  private OpponentSelector opponentSelector;

  @BeforeEach
  void setupGame() {
    gameData = TestMapGameData.REVISED.getGameData();
    germany = gameData.getMap().getTerritory("Germany");
    japan = gameData.getMap().getTerritory("Japan");
    unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    russians = russians(gameData);
    germans = germans(gameData);
    japanese = japanese(gameData);
    americans = americans(gameData);
    players = List.of(russians, germans, japanese, americans);
    opponentSelector =
        OpponentSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .build();
  }

  @Test
  void testSingleDefender1() {
    // Fight in Germany -> Germans defend
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
  }

  @Test
  void testSingleDefender2() {
    // Fight in Japan -> Japans defend
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(japan);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
  }

  @Test
  void testSingleDefender3() {
    // Fight in Britain -> Germans defend (British are allied; Germans are the first enemy)
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(unitedKingdom);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
  }

  @Test
  void testMultipleDefenders1() {
    // Fight in Germany -> 100 Japanese defend
    addTo(germany, infantry(gameData).create(100, japanese));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
  }

  @Test
  void testMultipleDefenders2() {
    // Fight in Japan -> 100 Germans defend
    addTo(japan, infantry(gameData).create(100, germans));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(japan);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
  }

  @Test
  void testMultipleDefenders3() {
    // Fight in Britain -> Germans defend (British & Americans are allied; Germans are the first
    // enemy)
    addTo(unitedKingdom, infantry(gameData).create(100, americans));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(unitedKingdom);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
  }

  @Test
  void testMixedDefendersAlliesAndEnemies1() {
    // Fight in Germany -> Japanese defend, Americans are allied
    addTo(germany, infantry(gameData).create(200, americans));
    addTo(germany, infantry(gameData).create(100, japanese));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
  }

  @Test
  void testMixedDefendersAlliesAndEnemies2() {
    // Fight in Japan -> Japanese defend, Americans are allied
    addTo(japan, infantry(gameData).create(100, americans));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(japan);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
  }
}
