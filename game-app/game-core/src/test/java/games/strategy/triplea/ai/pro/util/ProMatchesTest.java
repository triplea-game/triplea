package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
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
}
