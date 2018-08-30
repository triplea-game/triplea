package games.strategy.triplea.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.xml.TestMapGameData;

@ExtendWith(MockitoExtension.class)
public class UnitSeperatorTest {

  @Mock
  private MapData mockMapData;

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
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(true);
    final List<UnitCategory> categories = UnitSeperator.getSortedUnitCategories(northernGermany, mockMapData);
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

  @Test
  public void testGetSortedUnitCategoriesDontDrawUnit() throws Exception {
    final GameData data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnits().clear();
    final List<Unit> units = new ArrayList<>();
    final PlayerID italians = GameDataTestUtil.italy(data);
    units.addAll(GameDataTestUtil.italianInfantry(data).create(1, italians));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(false);
    final List<UnitCategory> categories = UnitSeperator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    assertEquals(expected, categories);
  }

  private static UnitCategory createUnitCategory(final String unitName, final PlayerID player, final GameData data) {
    return new UnitCategory(new UnitType(unitName, data), player);
  }
}
