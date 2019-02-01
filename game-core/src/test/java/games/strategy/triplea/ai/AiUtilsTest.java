package games.strategy.triplea.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;

public class AiUtilsTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.REVISED.getGameData();
  }

  @Test
  public void testCost() {
    final UnitType infantry = GameDataTestUtil.infantry(gameData);
    final PlayerId british = GameDataTestUtil.british(gameData);
    assertEquals(3, AiUtils.getCost(infantry, british, gameData));
  }

  @Test
  public void testSortByCost() {
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final List<Unit> sorted = new ArrayList<>(germany.getUnitCollection().getUnits());
    Collections.sort(sorted, AiUtils.getCostComparator());
    assertEquals(Constants.UNIT_TYPE_INFANTRY, sorted.get(0).getType().getName());
  }
}
