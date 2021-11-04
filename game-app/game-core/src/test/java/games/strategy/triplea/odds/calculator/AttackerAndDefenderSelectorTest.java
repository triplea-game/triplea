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
import static org.hamcrest.Matchers.not;

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

public class AttackerAndDefenderSelectorTest {

  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final GamePlayer russians = russians(gameData);
  private final GamePlayer germans = germans(gameData);
  private final GamePlayer british = british(gameData);
  private final GamePlayer japanese = japanese(gameData);
  private final GamePlayer americans = americans(gameData);
  private final Territory germany = gameData.getMap().getTerritory("Germany");
  private final Territory japan = gameData.getMap().getTerritory("Japan");
  private final Territory unitedKingdom = gameData.getMap().getTerritory("United Kingdom");
  private final Territory seaZone32 = gameData.getMap().getTerritory("32 Sea Zone");
  private final Territory kenya = gameData.getMap().getTerritory("Kenya");
  private final Territory russia = gameData.getMap().getTerritory("Russia");

  private final List<GamePlayer> players =
      List.of(russians, germans, british, japanese, americans, GamePlayer.NULL_PLAYERID);

  @Test
  void testNoCurrentPlayer() {
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(null)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(germany)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isEmpty());
    assertThat(attAndDef.getDefender(), isEmpty());
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testNoTerritory() {
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(null)
            .build();

    // Algorithm only ensures "some enemy" as defender
    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(
        gameData.getRelationshipTracker().isAtWar(attacker.orElseThrow(), defender.orElseThrow()),
        is(true));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSingleDefender1() {
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(germany)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    // Fight in Germany -> Germans defend
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    final Matcher<Unit>[] expectedUnits =
        germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testSingleDefender2() {
    // Fight in Japan -> Japans defend
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(japan)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    final Matcher<Unit>[] expectedUnits =
        japan.getUnitCollection().getMatches(Matches.unitIsOwnedBy(japanese)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testSingleDefender3() {
    // Fight in Britain -> "some enemy" defends (British are allied)
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(unitedKingdom)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(
        gameData.getRelationshipTracker().isAtWar(attacker.orElseThrow(), defender.orElseThrow()),
        is(true));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMultipleDefenders1() {
    // Fight in Germany -> 100 Japanese defend
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(germany)
            .build();

    final List<Unit> expectedUnits0 =
        germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans));
    expectedUnits0.addAll(infantry(gameData).create(100, japanese));
    final Matcher<Unit>[] expectedUnits =
        expectedUnits0.stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);

    addTo(germany, infantry(gameData).create(100, japanese));

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMultipleDefenders2() {
    // Fight in Japan -> 100 Germans defend
    addTo(japan, infantry(gameData).create(100, germans));
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(japan)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    final List<Unit> expectedUnits0 =
        japan.getUnitCollection().getMatches(Matches.unitIsOwnedBy(japanese));
    expectedUnits0.addAll(infantry(gameData).create(100, germans));
    final Matcher<Unit>[] expectedUnits =
        expectedUnits0.stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testMultipleDefenders3() {
    // Fight in Britain -> Germans defend (British & Americans are allied; Germans are the first
    // enemy)
    addTo(unitedKingdom, infantry(gameData).create(100, americans));
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(unitedKingdom)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMixedDefendersAlliesAndEnemies1() {
    // Fight in Germany -> Japanese defend, Americans are allied
    addTo(germany, infantry(gameData).create(200, americans));
    addTo(germany, infantry(gameData).create(100, japanese));

    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(germany)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    final List<Unit> expectedUnits0 =
        germany.getUnitCollection().getMatches(Matches.unitIsOwnedBy(germans));
    expectedUnits0.addAll(infantry(gameData).create(100, japanese));
    final Matcher<Unit>[] expectedUnits =
        expectedUnits0.stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testMixedDefendersAlliesAndEnemies2() {
    // Fight in Japan -> Japanese defend, Americans are allied
    addTo(japan, infantry(gameData).create(100, americans));
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(japan)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
    assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    final Matcher<Unit>[] expectedUnits =
        japan.getUnitCollection().getMatches(Matches.unitIsOwnedBy(japanese)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    assertThat(attAndDef.getDefendingUnits(), containsInAnyOrder(expectedUnits));
  }

  @Test
  void testNoDefender() {
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(seaZone32)
            .build();

    // Algorithm only ensures "some enemy" as defender
    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(
        gameData.getRelationshipTracker().isAtWar(attacker.orElseThrow(), defender.orElseThrow()),
        is(true));
    assertThat(defender.orElseThrow(), is(not(GamePlayer.NULL_PLAYERID)));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testNoDefenderOnEnemyTerritory() {
    // Fight in Kenya -> British (territory owner) defend
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(germans) // An Enemy of the British
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(kenya)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    assertThat(kenya.getOwner(), is(equalTo(british)));
    assertThat(attAndDef.getAttacker(), isPresentAndIs(germans));
    assertThat(attAndDef.getDefender(), isPresentAndIs(british));
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @Test
  void testNoDefenderAllPlayersAllied() {
    // Every player is allied with every other player, i.e. there are no enemies. In this case the
    // algorithm only ensures "some player" as defender.
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(List.of(russians, british, americans)) // only allies
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(seaZone32)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(attAndDef.getAttackingUnits(), is(empty()));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  void testAttackOnOwnTerritory() {
    // Fight in Russia -> russian army attacks, "some enemy" defends
    final AttackerAndDefenderSelector attackerAndDefenderSelector =
        AttackerAndDefenderSelector.builder()
            .players(players)
            .currentPlayer(russians)
            .relationshipTracker(gameData.getRelationshipTracker())
            .territory(russia)
            .build();

    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        attackerAndDefenderSelector.getAttackerAndDefender();

    final Optional<GamePlayer> attacker = attAndDef.getAttacker();
    final Optional<GamePlayer> defender = attAndDef.getDefender();
    assertThat(attacker, isPresentAndIs(russians));
    assertThat(defender, isPresent());
    assertThat(
        gameData.getRelationshipTracker().isAtWar(attacker.orElseThrow(), defender.orElseThrow()),
        is(true));
    final Matcher<Unit>[] expectedUnits =
        russia.getUnitCollection().getMatches(Matches.unitIsOwnedBy(russians)).stream()
            .map(GameDataTestUtil.IsEquivalentUnit::equivalentTo)
            .toArray(Matcher[]::new);
    assertThat(attAndDef.getAttackingUnits(), containsInAnyOrder(expectedUnits));
    assertThat(attAndDef.getDefendingUnits(), is(empty()));
  }
}
