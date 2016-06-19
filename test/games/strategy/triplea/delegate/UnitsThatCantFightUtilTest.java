package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.xml.LoadGameUtil;

public class UnitsThatCantFightUtilTest {

  @Test
  public void testNoSuicideAttacksAA50AtStart() {
    // at the start of the game, there are no suicide attacks
    final GameData data = LoadGameUtil.loadTestGame("ww2v3_1941_test.xml");
    Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.isEmpty());

    // add a german sub to sz 12
    final Territory sz12 = territory("12 Sea Zone", data);
    addTo(sz12, transport(data).create(1, germans(data)));
    territories = new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.contains(sz12));
  }

  @Test
  public void testSuicideAttackInRevised() {
    final GameData data = LoadGameUtil.loadTestGame("revised_test.xml");
    final Territory sz15 = territory("15 Sea Zone", data);
    addTo(sz15, transport(data).create(1, germans(data)));
    final Collection<Territory> territories =
        new UnitsThatCantFightUtil(data).getTerritoriesWhereUnitsCantFight(germans(data));
    assertTrue(territories.contains(sz15));
  }
}
