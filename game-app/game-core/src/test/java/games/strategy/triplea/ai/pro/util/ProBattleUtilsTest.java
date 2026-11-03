package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProBattleUtilsTest {
  private final GameData gameData = TestMapGameData.LHTR.getGameData();
  private final ProData proData = new ProData();
  private final Territory germany = territory("Germany", gameData);
  private final Territory easternEurope = territory("Eastern Europe", gameData);

  ProBattleUtilsTest() {
    proData.initializeSimulation(null, gameData, germans(gameData));
    addTo(easternEurope, armour(gameData).create(5, russians(gameData)));
  }

  @Test
  void localLandSuperiorityAfterMovesCountsUnitsInTheTerritory() {
    final Map<Territory, ProTerritory> moveMap = new HashMap<>();
    final List<Unit> garrison = infantry(gameData).create(30, germans(gameData));
    addTo(germany, garrison);
    final ProTerritory capital = new ProTerritory(germany, proData);
    capital.addCantMoveUnits(garrison);
    moveMap.put(germany, capital);

    assertThat(
            ProBattleUtils.territoryHasLocalLandSuperiorityAfterMoves(
                proData, germany, 2, germans(gameData), moveMap))
        .isTrue();
  }

  @Test
  void localLandSuperiorityAfterMovesIgnoresUnitsInNeighboringTerritories() {
    final Map<Territory, ProTerritory> moveMap = new HashMap<>();
    final List<Unit> neighbors = infantry(gameData).create(30, germans(gameData));
    final Territory southernEurope = territory("Southern Europe", gameData);
    addTo(southernEurope, neighbors);
    final ProTerritory neighbor = new ProTerritory(southernEurope, proData);
    neighbor.addCantMoveUnits(neighbors);
    moveMap.put(southernEurope, neighbor);
    moveMap.put(germany, new ProTerritory(germany, proData));

    assertThat(
            ProBattleUtils.territoryHasLocalLandSuperiorityAfterMoves(
                proData, germany, 2, germans(gameData), moveMap))
        .isFalse();
  }
}
