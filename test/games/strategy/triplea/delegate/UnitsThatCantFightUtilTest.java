package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transports;

import java.util.Collection;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.LoadGameUtil;
import junit.framework.TestCase;

public class UnitsThatCantFightUtilTest extends TestCase {
  public void testNoSuicideAttacksAA50AtStart() {
    // at the start of the game, there are no suicide attacks
    final GameData data = LoadGameUtil.loadGame("World War II v3 1941 Test", "ww2v3_1941_test.xml");
    final Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.isEmpty());
  }

  public void testSuicideAttackInAA50() {
    final GameData data = LoadGameUtil.loadGame("World War II v3 1941 Test", "ww2v3_1941_test.xml");
    // add a german sub to sz 12
    final Territory sz12 = territory("12 Sea Zone", data);
    addTo(sz12, transports(data).create(1, germans(data)));
    final Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.contains(sz12));
  }

  public void testSuicideAttackInRevised() {
    final GameData data = LoadGameUtil.loadGame("World War II Revised Test", "revised_test.xml");
    final Territory sz15 = territory("15 Sea Zone", data);
    addTo(sz15, transports(data).create(1, germans(data)));
    final Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.contains(sz15));
  }
}
