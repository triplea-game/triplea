package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.load;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class UnitComparatorTest {
  private static void startCombatMoveFor(final GamePlayer gamePlayer, final GameData gameData) {
    final MoveDelegate moveDelegate = moveDelegate(gameData);
    final IDelegateBridge bridge = newDelegateBridge(gamePlayer);
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
  }

  @Nested
  final class GetUnloadableUnitsComparatorTest {
    @Test
    void shouldSortUnloadableUnitsFirst() {
      final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
      final GamePlayer germans = germans(gameData);
      final Territory germany = territory("Germany", gameData);
      final Territory seaZone5 = territory("5 Sea Zone", gameData);
      final Territory kareliaSsr = territory("Karelia S.S.R.", gameData);
      startCombatMoveFor(germans, gameData);
      final List<Unit> transportedUnits =
          germany
              .getUnitCollection()
              .getMatches(u -> armour(gameData).equals(u.getType()))
              .subList(0, 1);
      load(transportedUnits, new Route(germany, seaZone5));

      final List<Unit> units = new ArrayList<>(seaZone5.getUnits());
      final List<Unit> sortedUnits = new ArrayList<>(units);
      sortedUnits.sort(
          UnitComparator.getUnloadableUnitsComparator(
              units, new Route(seaZone5, kareliaSsr), germans));

      assertThat(sortedUnits.get(0), is(transportedUnits.get(0)));
    }

    @Test
    void unitsOfSameTypeAreSortedTogether() throws Exception {
      final GameDataInjections gameData = TestMapGameData.REVISED.getGameData();

      final List<Unit> units = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        units.add(infantry(gameData).create(germans(gameData)));
      }
      for (int i = 0; i < 10; i++) {
        units.add(artillery(gameData).create(germans(gameData)));
      }
      for (int i = 0; i < 10; i++) {
        units.add(armour(gameData).create(germans(gameData)));
      }

      final Territory germany = territory("Germany", gameData);
      final Territory balkans = territory("Balkans", gameData);
      units.sort(UnitComparator.getMovableUnitsComparator(units, new Route(germany, balkans)));
      Unit lastUnit = units.get(0);
      int transitions = 0;
      for (final Unit unit : units) {
        if (!unit.getType().equals(lastUnit.getType())) {
          transitions++;
        }
        lastUnit = unit;
      }
      assertThat("expected 2 transitions between 3 unit types", transitions, is(2));
    }
  }
}
