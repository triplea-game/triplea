package games.strategy.triplea.odds.calculator;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class OpponentSelectorTest {

  private GameData gameData = TestMapGameData.REVISED.getGameData();
  private GamePlayer russians = russians(gameData);
  private GamePlayer germans = germans(gameData);
  private GamePlayer british = british(gameData);
  private GamePlayer japanese = japanese(gameData);
  private GamePlayer americans = americans(gameData);
  private List<GamePlayer> players = List.of(russians, germans, british, japanese, americans);
  private OpponentSelector opponentSelector =
      OpponentSelector.builder()
          .players(players)
          .currentPlayer(russians)
          .relationshipTracker(gameData.getRelationshipTracker())
          .build();

  @Test
  void testNoCurrentPlayer() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    opponentSelector =
        OpponentSelector.builder()
            .players(players)
            .relationshipTracker(gameData.getRelationshipTracker())
            .build();
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isEmpty());
    assertThat(attAndDef.getDefender(), isEmpty());
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testNoTerritory() {
    // Algorithm only ensures "some enemy" as defender
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(null);
    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(gameData.getRelationshipTracker().isAtWar(attacker.get(), defender.get()), is(true));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testSingleDefender1() {
    // Fight in Germany -> Germans defend
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final Matcher<Unit>[] expectedUnits =
        germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testSingleDefender2() {
    // Fight in Japan -> Japans defend
    final Territory japan = gameData.getMap().getTerritory("Japan");
    final Matcher<Unit>[] expectedUnits =
        japan.getUnitCollection().getMatches(Matches.unitIsOwnedBy(japanese)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(japan);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testSingleDefender3() {
    // Fight in Britain -> "some enemy" defends (British are allied
    final Territory unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(unitedKingdom);
    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(gameData.getRelationshipTracker().isAtWar(attacker.get(), defender.get()), is(true));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testMultipleDefenders1() {
    // Fight in Germany -> 100 Japanese defend
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final List<Unit> expectedUnits0 =
        germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans));
    expectedUnits0.addAll(infantry(gameData).create(100, japanese));
    final Matcher<Unit>[] expectedUnits =
        expectedUnits0.stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);

    addTo(germany, infantry(gameData).create(100, japanese));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testMultipleDefenders2() {
    // Fight in Japan -> 100 Germans defend
    final Territory japan = gameData.getMap().getTerritory("Japan");
    final List<Unit> expectedUnits0 =
        japan.getUnitCollection().getMatches(Matches.unitIsOwnedBy(japanese));
    expectedUnits0.addAll(infantry(gameData).create(100, germans));
    final Matcher<Unit>[] expectedUnits =
        expectedUnits0.stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);

    addTo(japan, infantry(gameData).create(100, germans));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(japan);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testMultipleDefenders3() {
    // Fight in Britain -> Germans defend (British & Americans are allied; Germans are the first
    // enemy)
    final Territory unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
    addTo(unitedKingdom, infantry(gameData).create(100, americans));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(unitedKingdom);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testMixedDefendersAlliesAndEnemies1() {
    // Fight in Germany -> Japanese defend, Americans are allied
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final List<Unit> expectedUnits0 =
        germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans));
    expectedUnits0.addAll(infantry(gameData).create(100, japanese));
    final Matcher<Unit>[] expectedUnits =
        expectedUnits0.stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);

    addTo(germany, infantry(gameData).create(200, americans));
    addTo(germany, infantry(gameData).create(100, japanese));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(germany);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testMixedDefendersAlliesAndEnemies2() {
    // Fight in Japan -> Japanese defend, Americans are allied
    final Territory japan = gameData.getMap().getTerritory("Japan");
    final Matcher<Unit>[] expectedUnits =
        japan.getUnitCollection().getMatches(Matches.unitIsOwnedBy(japanese)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    addTo(japan, infantry(gameData).create(100, americans));
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(japan);
    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testNoDefender() {
    // Algorithm only ensures "some enemy" as defender
    final Territory seaZone32 = gameData.getMap().getTerritory("32 Sea Zone");
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(seaZone32);
    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(gameData.getRelationshipTracker().isAtWar(attacker.get(), defender.get()), is(true));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testNoDefenderOnEnemyTerritory() {
    // Fight in Kenya -> British (territory owner) defend
    final Territory kenya = gameData.getMap().getTerritory("Kenya");
    opponentSelector =
        OpponentSelector.builder()
            .players(players)
            .currentPlayer(germans) // An Enemy of the British
            .relationshipTracker(gameData.getRelationshipTracker())
            .build();
    final Territory territory = kenya;
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(territory);
    assertThat(territory.getOwner(), is(equalTo(british)));
    assertThat(attAndDef.getAttacker(), isPresentAndIs(germans));
    assertThat(attAndDef.getDefender(), isPresentAndIs(british));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testNoDefenderAllPlayersAllied() {
    // Every player is allied with every other player, i.e. there are no enemies. In this case the
    // algorithm only ensures "some player" as defender.
    final Territory seaZone32 = gameData.getMap().getTerritory("32 Sea Zone");
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
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testAttackOnOwnTerritory() {
    // Fight in Russia -> russian army attacks, "some enemy" defends
    final Territory russia = gameData.getMap().getTerritory("Russia");
    final Matcher<Unit>[] expectedUnits =
        russia.getUnitCollection().getMatches(Matches.unitIsOwnedBy(russians)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    final OpponentSelector.AttackerAndDefender attAndDef =
        opponentSelector.getAttackerAndDefender(russia);
    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(gameData.getRelationshipTracker().isAtWar(attacker.get(), defender.get()), is(true));
    assertThat(attAndDef.getAttackingUnits(), containsInAnyOrder(expectedUnits));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }
}
