package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.getDelegateBridge;
import static games.strategy.triplea.delegate.GameDataTestUtil.load;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveToStep;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;

public final class UnitComparatorTest {
  private static void startCombatMoveFor(final PlayerID playerId, final GameData gameData) {
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(playerId, gameData);
    moveToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
  }

  @Nested
  public final class GetUnloadableUnitsComparatorTest {
    @Test
    public void shouldSortUnloadableUnitsFirst() throws Exception {
      final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
      final PlayerID germans = germans(gameData);
      final Territory germany = territory("Germany", gameData);
      final Territory seaZone5 = territory("5 Sea Zone", gameData);
      final Territory kareliaSsr = territory("Karelia S.S.R.", gameData);
      startCombatMoveFor(germans, gameData);
      final List<Unit> transportedUnits = germany.getUnits()
          .getMatches(u -> armour(gameData).equals(u.getType()))
          .subList(0, 1);
      load(transportedUnits, new Route(germany, seaZone5));

      final List<Unit> units = new ArrayList<>(seaZone5.getUnits().getUnits());
      final List<Unit> sortedUnits = new ArrayList<>(units);
      sortedUnits.sort(UnitComparator.getUnloadableUnitsComparator(units, new Route(seaZone5, kareliaSsr), germans));

      assertThat(sortedUnits.get(0), is(transportedUnits.get(0)));
    }
  }
}
