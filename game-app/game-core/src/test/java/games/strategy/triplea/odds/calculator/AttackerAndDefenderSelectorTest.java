package games.strategy.triplea.odds.calculator;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.french;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.italians;
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
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AttackerAndDefenderSelectorTest {

  private static Unit[] filterTerritoryUnitsByOwner(
      final Territory territory, final GamePlayer... gamePlayers) {
    final List<Unit> units = new ArrayList<>();
    for (final GamePlayer gamePlayer : gamePlayers) {
      units.addAll(territory.getUnitCollection().getMatches(Matches.unitIsOwnedBy(gamePlayer)));
    }
    return units.toArray(Unit[]::new);
  }

  @Nested
  public class Revised {
    private final GameData gameData = TestMapGameData.REVISED.getGameData();
    private final GamePlayer russians = russians(gameData);
    private final GamePlayer germans = germans(gameData);
    private final GamePlayer british = british(gameData);
    private final GamePlayer japanese = japanese(gameData);
    private final GamePlayer americans = americans(gameData);
    private final Territory germany = gameData.getMap().getTerritoryOrNull("Germany");
    private final Territory japan = gameData.getMap().getTerritoryOrNull("Japan");
    private final Territory unitedKingdom = gameData.getMap().getTerritoryOrNull("United Kingdom");
    private final Territory seaZone32 = gameData.getMap().getTerritoryOrNull("32 Sea Zone");
    private final Territory kenya = gameData.getMap().getTerritoryOrNull("Kenya");
    private final Territory russia = gameData.getMap().getTerritoryOrNull("Russia");

    private final List<GamePlayer> players =
        List.of(
            russians,
            germans,
            british,
            japanese,
            americans,
            gameData.getPlayerList().getNullPlayer());

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
      assertThat(attacker.orElseThrow().isAtWar(defender.orElseThrow()), is(true));
      assertThat(attAndDef.getAttackingUnits(), is(empty()));
      assertThat(attAndDef.getDefendingUnits(), is(empty()));
    }

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
      assertThat(attAndDef.getDefendingUnits(), is(germany.getUnits()));
    }

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
      assertThat(attAndDef.getDefendingUnits(), is(japan.getUnits()));
    }

    @Test
    void testSingleDefender3() {
      // Fight in Britain; Germany defends (British are allied)
      // (Germany has some units there, but doesn't own the territory)
      addTo(unitedKingdom, infantry(gameData).create(5, germans));
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
      assertThat(defender, isPresentAndIs(germans));
      assertThat(attacker.orElseThrow().isAtWar(defender.orElseThrow()), is(true));
      assertThat(
          attAndDef.getAttackingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(unitedKingdom, russians)));
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(unitedKingdom, germans)));
    }

    @Test
    void testMultipleDefenders1() {
      // Fight in Germany, Germany defends along with 100 Japanese units
      final AttackerAndDefenderSelector attackerAndDefenderSelector =
          AttackerAndDefenderSelector.builder()
              .players(players)
              .currentPlayer(russians)
              .relationshipTracker(gameData.getRelationshipTracker())
              .territory(germany)
              .build();

      addTo(germany, infantry(gameData).create(100, japanese));

      final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
          attackerAndDefenderSelector.getAttackerAndDefender();

      assertThat(attAndDef.getAttacker(), isPresentAndIs(russians));
      assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
      assertThat(attAndDef.getAttackingUnits(), is(empty()));
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(germany, germans, japanese)));
    }

    @Test
    void testMultipleDefenders2() {
      // Fight in Japan, Japanese defend along with 100 Germans units
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
      assertThat(attAndDef.getDefender(), isPresentAndIs(japanese));
      assertThat(attAndDef.getAttackingUnits(), is(empty()));
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(japan, japanese, germans)));
    }

    @Test
    void testMultipleDefenders3() {
      // Fight in Britain; British defends (British & Americans are allied)
      addTo(unitedKingdom, infantry(gameData).create(5, americans));
      final AttackerAndDefenderSelector attackerAndDefenderSelector =
          AttackerAndDefenderSelector.builder()
              .players(players)
              .currentPlayer(russians)
              .relationshipTracker(gameData.getRelationshipTracker())
              .territory(unitedKingdom)
              .build();

      final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
          attackerAndDefenderSelector.getAttackerAndDefender();

      assertThat(attAndDef.getAttacker(), isPresentAndIs(germans)); // case: all units allied
      assertThat(attAndDef.getDefender(), isPresentAndIs(british));
      assertThat(attAndDef.getAttackingUnits(), is(empty()));
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(unitedKingdom, british, americans)));
    }

    @Test
    void testMixedDefendersAlliesAndEnemies1() {
      // Fight in Germany -> Germany defend along with Japanese units, Americans are allied
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
      assertThat(attAndDef.getDefender(), isPresentAndIs(germans));
      assertThat(attAndDef.getAttackingUnits(), is(empty()));
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(germany, germans, japanese)));
    }

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
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(japan, japanese)));
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
      assertThat(attacker.orElseThrow().isAtWar(defender.orElseThrow()), is(true));
      assertThat(defender.orElseThrow(), is(not(gameData.getPlayerList().getNullPlayer())));
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

    @Test
    void testAttackOnOwnTerritory() {
      // Fight in Russia, containing German and Russian units. Territory is under Russian control,
      // even if there are German units present (e.g. can happen with e.g. limited combat rounds).
      addTo(russia, infantry(gameData).create(10, germans));
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
      assertThat(defender, isPresentAndIs(germans));
      assertThat(attacker.orElseThrow().isAtWar(defender.orElseThrow()), is(true));
      assertThat(
          attAndDef.getAttackingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(russia, russians)));
      assertThat(
          attAndDef.getDefendingUnits(),
          containsInAnyOrder(filterTerritoryUnitsByOwner(russia, germans)));
    }
  }

  @Nested
  public class Global {
    private final GameData gameData = TestMapGameData.GLOBAL1940.getGameData();
    private final GamePlayer germans = germans(gameData);
    private final GamePlayer italians = italians(gameData);
    private final GamePlayer russians = russians(gameData);
    private final GamePlayer british = british(gameData);
    private final GamePlayer french = french(gameData);
    private final Territory northernItaly = gameData.getMap().getTerritoryOrNull("Northern Italy");
    private final Territory balticStates = gameData.getMap().getTerritoryOrNull("Baltic States");
    private final Territory sz97 = gameData.getMap().getTerritoryOrNull("97 Sea Zone");
    private final Territory uk = gameData.getMap().getTerritoryOrNull("United Kingdom");

    @Test
    void alliedTerritory() {
      final AttackerAndDefenderSelector attackerAndDefenderSelector =
          AttackerAndDefenderSelector.builder()
              .players(gameData.getPlayerList().getPlayers())
              .currentPlayer(germans)
              .relationshipTracker(gameData.getRelationshipTracker())
              .territory(northernItaly)
              .build();

      final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
          attackerAndDefenderSelector.getAttackerAndDefender();

      final Optional<GamePlayer> defender = attAndDef.getDefender();
      assertThat(defender, isPresentAndIs(italians));
      assertThat(attAndDef.getDefendingUnits(), equalTo(northernItaly.getUnits()));
    }

    @Test
    void seaZoneWithAlliedUnits() {
      assertThat(sz97.getUnits(), not(empty()));
      final AttackerAndDefenderSelector attackerAndDefenderSelector =
          AttackerAndDefenderSelector.builder()
              .players(gameData.getPlayerList().getPlayers())
              .currentPlayer(germans)
              .relationshipTracker(gameData.getRelationshipTracker())
              .territory(sz97)
              .build();

      final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
          attackerAndDefenderSelector.getAttackerAndDefender();

      final Optional<GamePlayer> defender = attAndDef.getDefender();
      assertThat(defender, isPresentAndIs(italians)); // only italy units present
      assertThat(attAndDef.getDefendingUnits(), equalTo(sz97.getUnits()));
    }

    @Test
    void neutralTerritory() {
      final AttackerAndDefenderSelector attackerAndDefenderSelector =
          AttackerAndDefenderSelector.builder()
              .players(gameData.getPlayerList().getPlayers())
              .currentPlayer(germans)
              .relationshipTracker(gameData.getRelationshipTracker())
              .territory(balticStates)
              .build();

      final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
          attackerAndDefenderSelector.getAttackerAndDefender();

      final Optional<GamePlayer> defender = attAndDef.getDefender();
      assertThat(defender, isPresentAndIs(russians));
      assertThat(attAndDef.getDefendingUnits(), equalTo(balticStates.getUnits()));
    }

    @Test
    void ownTerritoryWithAlliedUnits() {
      final AttackerAndDefenderSelector attackerAndDefenderSelector =
          AttackerAndDefenderSelector.builder()
              .players(gameData.getPlayerList().getPlayers())
              .currentPlayer(british)
              .relationshipTracker(gameData.getRelationshipTracker())
              .territory(uk)
              .build();

      final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
          attackerAndDefenderSelector.getAttackerAndDefender();

      assertThat(uk.getUnits().stream().anyMatch(Matches.unitIsOwnedBy(french)), is(true));
      assertThat(uk.getUnits().stream().anyMatch(Matches.unitIsOwnedBy(british)), is(true));
      assertThat(attAndDef.getDefender(), isPresentAndIs(british)); // only allied units
      assertThat(attAndDef.getDefendingUnits(), equalTo(uk.getUnits()));
      assertThat(
          attAndDef.getAttacker(), isPresentAndIs(germans)); // next in turn after current init step
      assertThat(attAndDef.getAttackingUnits(), is(empty()));
    }
  }
}
