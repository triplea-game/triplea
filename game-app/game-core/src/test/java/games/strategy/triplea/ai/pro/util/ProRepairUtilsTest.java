package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.japanese;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProRepairUtilsTest {
  private final GameData data = TestMapGameData.GLOBAL1940.getGameData();
  private final Territory japan = territory("Japan", data);
  private final Territory dockZone =
      data.getMap().getNeighbors(japan, Matches.territoryIsWater()).iterator().next();
  private final Territory openSea = territory("1 Sea Zone", data);

  private Unit battleshipWithHits(final int hits) {
    final Unit battleship = unitType("battleship", data).create(1, japanese(data)).get(0);
    battleship.setHits(hits);
    return battleship;
  }

  @Test
  void damagedShipIsRepairedOnlyNextToItsFacility() {
    final Unit damaged = battleshipWithHits(1);

    assertThat(
            ProRepairUtils.findRepairTerritories(
                japanese(data), damaged, List.of(dockZone, openSea)))
        .containsExactly(dockZone);
  }

  @Test
  void undamagedShipNeedsNoRepair() {
    assertThat(
            ProRepairUtils.findRepairTerritories(
                japanese(data), battleshipWithHits(0), List.of(dockZone)))
        .isEmpty();
  }
}
