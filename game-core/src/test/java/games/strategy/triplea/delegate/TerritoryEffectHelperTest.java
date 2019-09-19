package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryEffectHelperTest extends AbstractDelegateTestCase {

  @Test
  void testGetMaxMovementCostZero() throws Exception {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();
    final PlayerId germans = GameDataTestUtil.germany(twwGameData);
    final Territory sicily = territory("Sicily", twwGameData);
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily, GameDataTestUtil.unitType("germanInfantry", twwGameData).create(1, germans));
    assertTrue(result.compareTo(BigDecimal.ZERO) == 0);
  }

  @Test
  void testGetMaxMovementCostDecimal() throws Exception {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();
    final PlayerId germans = GameDataTestUtil.germany(twwGameData);
    final Territory sicily = territory("Sicily", twwGameData);
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanAlpineInfantry", twwGameData).create(1, germans));
    assertTrue(result.compareTo(new BigDecimal("0.5")) == 0);
  }

  @Test
  void testGetMaxMovementCostTwo() throws Exception {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();
    final PlayerId germans = GameDataTestUtil.germany(twwGameData);
    final Territory sicily = territory("Sicily", twwGameData);
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanCombatEngineer", twwGameData).create(1, germans));
    assertTrue(result.compareTo(new BigDecimal("2")) == 0);
  }

  @Test
  void testGetMaxMovementCostMultipleUnits() throws Exception {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();
    final PlayerId germans = GameDataTestUtil.germany(twwGameData);
    final Territory sicily = territory("Sicily", twwGameData);
    final List<Unit> units = new ArrayList<>();
    units.addAll(GameDataTestUtil.unitType("germanInfantry", twwGameData).create(1, germans));
    units.addAll(GameDataTestUtil.unitType("germanAlpineInfantry", twwGameData).create(1, germans));
    units.addAll(GameDataTestUtil.unitType("germanCombatEngineer", twwGameData).create(1, germans));
    final BigDecimal result = TerritoryEffectHelper.getMaxMovementCost(sicily, units);
    assertTrue(result.compareTo(new BigDecimal("2")) == 0);
  }
}
