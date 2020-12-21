package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import org.junit.jupiter.api.Test;

class UnitsThatCantFightUtilTest {

  @Test
  void testCantFightAttacksV3() {
    final GameDataInjections data = TestMapGameData.WW2V3_1941.getGameData();

    Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.isEmpty());

    // Add a german transport which would be attempting to attack UK ships
    final Territory sz12 = territory("12 Sea Zone", data);
    addTo(sz12, transport(data).create(1, germans(data)));
    territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.contains(sz12));
  }

  @Test
  void testCantFightAttackInRevised() {
    final GameDataInjections data = TestMapGameData.REVISED.getGameData();
    final Territory sz15 = territory("15 Sea Zone", data);
    addTo(sz15, transport(data).create(1, germans(data)));
    final Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.contains(sz15));
  }
}
