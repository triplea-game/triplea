package games.strategy.triplea.ui.screen;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.xml.TestMapGameData;

public class TileManagerTest {

  @Test
  public void testGetSortedUnitCategories() throws Exception {
    final GameData data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnits().clear();
    final List<Unit> units = new ArrayList<>();
    final PlayerID italians = GameDataTestUtil.italy(data);
    units.addAll(GameDataTestUtil.italianInfantry(data).create(1, italians));
    units.addAll(GameDataTestUtil.italianFactory(data).create(1, italians));
    units.addAll(GameDataTestUtil.truck(data).create(1, italians));
    final PlayerID british = GameDataTestUtil.britain(data);
    units.addAll(GameDataTestUtil.britishInfantry(data).create(1, british));
    units.addAll(GameDataTestUtil.britishFactory(data).create(1, british));
    units.addAll(GameDataTestUtil.truck(data).create(1, british));
    final PlayerID germans = GameDataTestUtil.germany(data);
    units.addAll(GameDataTestUtil.germanInfantry(data).create(1, germans));
    units.addAll(GameDataTestUtil.germanFactory(data).create(1, germans));
    units.addAll(GameDataTestUtil.truck(data).create(1, germans));
    GameDataTestUtil.addTo(northernGermany, units);
    final List<UnitCategory> categories = TileManager.getSortedUnitCategories(northernGermany, data);
    final List<UnitCategory> expected = new ArrayList<>();
    expected.add(createUnitCategory("germanFactory", germans, data));
    expected.add(createUnitCategory("Truck", germans, data));
    expected.add(createUnitCategory("germanInfantry", germans, data));
    expected.add(createUnitCategory("italianFactory", italians, data));
    expected.add(createUnitCategory("Truck", italians, data));
    expected.add(createUnitCategory("italianInfantry", italians, data));
    expected.add(createUnitCategory("britishFactory", british, data));
    expected.add(createUnitCategory("Truck", british, data));
    expected.add(createUnitCategory("britishInfantry", british, data));
    assertEquals(expected, categories);
  }

  private UnitCategory createUnitCategory(final String unitName, final PlayerID player, final GameData data) {
    return new UnitCategory(new UnitType(unitName, data), player);
  }

}
