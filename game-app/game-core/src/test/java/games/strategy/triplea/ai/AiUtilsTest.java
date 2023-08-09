package games.strategy.triplea.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiUtilsTest {
  private final GameState gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void testCost() {
    final UnitType infantry = GameDataTestUtil.infantry(gameData);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    assertEquals(3, AiUtils.getCost(infantry, british, gameData));
  }

  @Test
  void testSortByCost() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final List<Unit> sorted = new ArrayList<>(germany.getUnits());
    sorted.sort(AiUtils.getCostComparator());
    assertEquals(Constants.UNIT_TYPE_INFANTRY, sorted.get(0).getType().getName());
  }
}
