package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProMatchesTest {
  private final GameData data = TestMapGameData.DELEGATE_TEST.getGameData();
  // West Canada is a marsh, whose territory effect bars armour.
  private final Territory westCanada = data.getMap().getTerritoryOrThrow("West Canada");
  private final Territory eastCanada = data.getMap().getTerritoryOrThrow("East Canada");

  @Test
  void routeIntoTerritoryThatBarsTheUnitTypeIsNotAllowed() {
    final Unit tank = armour(data).create(1, british(data)).get(0);

    assertThat(ProMatches.routeAllowsUnit(new Route(eastCanada, westCanada), tank)).isFalse();
  }

  @Test
  void routeIntoTerritoryThatBarsOtherUnitTypesIsAllowed() {
    final Unit infantry = infantry(data).create(1, british(data)).get(0);

    assertThat(ProMatches.routeAllowsUnit(new Route(eastCanada, westCanada), infantry)).isTrue();
  }

  @Test
  void routeOutOfTerritoryThatBarsTheUnitTypeIsAllowed() {
    final Unit tank = armour(data).create(1, british(data)).get(0);

    assertThat(ProMatches.routeAllowsUnit(new Route(westCanada, eastCanada), tank)).isTrue();
  }

  @Test
  void trainCannotMoveBetweenTerritoriesWithoutRail() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Territory easternGermany = tww.getMap().getTerritoryOrThrow("Eastern Germany");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);

    assertThat(ProMatches.routeHasRequiredUnitsToMove(new Route(berlin, easternGermany), train))
        .isFalse();
  }

  @Test
  void trainCanMoveBetweenTerritoriesThatBothHoldRail() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Territory easternGermany = tww.getMap().getTerritoryOrThrow("Eastern Germany");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);
    addTo(berlin, GameDataTestUtil.germanRail(tww).create(1, germans));
    addTo(easternGermany, GameDataTestUtil.germanRail(tww).create(1, germans));

    assertThat(ProMatches.routeHasRequiredUnitsToMove(new Route(berlin, easternGermany), train))
        .isTrue();
  }

  @Test
  void trainThatStaysWhereThereIsNoRailIsNotBlocked() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);

    assertThat(ProMatches.routeHasRequiredUnitsToMove(new Route(berlin), train)).isTrue();
  }
}
