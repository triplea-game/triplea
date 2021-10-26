package games.strategy.triplea.odds.calculator;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpponentSelectorTest {

  private GameData gameData;
  private Territory germany;
  private Territory japan;
  private Territory kenya;
  private Territory unitedKingdom;
  private Territory seaZone32;
  private GamePlayer russians;
  private GamePlayer germans;
  private GamePlayer british;
  private GamePlayer japanese;
  private GamePlayer americans;
  private List<GamePlayer> players;
  private OpponentSelector opponentSelector;

  @BeforeEach
  void setupGame() {
    gameData = TestMapGameData.REVISED.getGameData();
    germany = gameData.getMap().getTerritory("Germany");
    japan = gameData.getMap().getTerritory("Japan");
    kenya = gameData.getMap().getTerritory("Kenya");
    unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    seaZone32 = gameData.getMap().getTerritory("32 Sea Zone");
    russians = russians(gameData);
    germans = germans(gameData);
    british = british(gameData);
    japanese = japanese(gameData);
    americans = americans(gameData);
    players = List.of(russians, germans, british, japanese, americans);
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

  @Test
  void testNoDefender() {
    // Algorithm only ensures "some enemy" as defender
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(seaZone32);
    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertTrue(gameData.getRelationshipTracker().isAtWar(attacker.get(), defender.get()));
  }

  @Test
  void testNoDefenderOnEnemyTerritory() {
    // Fight in Kenya -> British (territory owner) defend
    opponentSelector =
        OpponentSelector.builder()
            .players(players)
            .currentPlayer(germans) // An Enemy of the British
            .relationshipTracker(gameData.getRelationshipTracker())
            .build();
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(kenya);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(germans));
    assertThat(attAndDef.getDefender(), isPresentAndIs(british));
  }

  @Test
  void testNoDefenderAllPlayersAllied() {
    // Every player is allied with every other player, i.e. there are no enemies.  In this case the
    // algorithm only ensures "some player" as defender.
    opponentSelector =
        OpponentSelector.builder()
            .players(List.of(russians, british, americans)) // only allies
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .build();
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(seaZone32);
    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
  }
}
