package games.strategy.triplea.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitSeparatorTest {

  @Mock private MapData mockMapData;

  @Test
  void testGetSortedUnitCategories() {
    final GameData data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnitCollection().clear();
    final List<Unit> units = new ArrayList<>();
    final GamePlayer italians = GameDataTestUtil.italy(data);
    units.addAll(GameDataTestUtil.italianInfantry(data).create(1, italians));
    units.addAll(GameDataTestUtil.italianFactory(data).create(1, italians));
    units.addAll(GameDataTestUtil.truck(data).create(1, italians));
    final GamePlayer british = GameDataTestUtil.britain(data);
    units.addAll(GameDataTestUtil.britishInfantry(data).create(1, british));
    units.addAll(GameDataTestUtil.britishFactory(data).create(1, british));
    units.addAll(GameDataTestUtil.truck(data).create(1, british));
    final GamePlayer germans = GameDataTestUtil.germany(data);
    units.addAll(GameDataTestUtil.germanInfantry(data).create(1, germans));
    units.addAll(GameDataTestUtil.germanFactory(data).create(1, germans));
    units.addAll(GameDataTestUtil.truck(data).create(1, germans));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(true);
    final List<UnitCategory> categories =
        UnitSeparator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    expected.add(newUnitCategory("germanFactory", germans, data));
    expected.add(newUnitCategory("Truck", germans, data));
    expected.add(newUnitCategory("germanInfantry", germans, data));
    expected.add(newUnitCategory("italianFactory", italians, data));
    expected.add(newUnitCategory("Truck", italians, data));
    expected.add(newUnitCategory("italianInfantry", italians, data));
    expected.add(newUnitCategory("britishFactory", british, data));
    expected.add(newUnitCategory("Truck", british, data));
    expected.add(newUnitCategory("britishInfantry", british, data));
    assertEquals(expected, categories);
  }

  @Test
  void testGetSortedUnitCategoriesDontDrawUnit() {
    final GameDataInjections data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnitCollection().clear();
    final GamePlayer italians = GameDataTestUtil.italy(data);
    final List<Unit> units =
        new ArrayList<>(GameDataTestUtil.italianInfantry(data).create(1, italians));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(false);
    final List<UnitCategory> categories =
        UnitSeparator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    assertEquals(expected, categories);
  }

  private static UnitCategory newUnitCategory(
      final String unitName, final GamePlayer player, final GameData data) {
    return new UnitCategory(new UnitType(unitName, data), player);
  }
}
