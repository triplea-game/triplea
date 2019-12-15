package games.strategy.triplea.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNull.nullValue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MockDelegateBridge;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

public class MovableUnitsFilterTest {
  final GameData gameData = TestMapGameData.REVISED.getGameData();
  final PlayerId germans = germans(gameData);
  final Territory germany = territory("Germany", gameData);
  final Territory easternEurope = territory("Eastern Europe", gameData);
  final Territory kareliaSsr = territory("Karelia S.S.R.", gameData);
  final UnitType infantryType = infantry(gameData);
  final UnitType armourType = armour(gameData);

  public MovableUnitsFilterTest() throws Exception {}

  private IDelegateBridge newDelegateBridge(final PlayerId player) {
    return MockDelegateBridge.newInstance(gameData, player);
  }

  private MovableUnitsFilter.Result filterUnits(final Route route, final Collection<Unit> units) {
    final IDelegateBridge bridge = newDelegateBridge(germans);
    advanceToStep(bridge, "NonCombatMove");
    final MovableUnitsFilter filter =
        new MovableUnitsFilter(
            gameData, germans, route, false, AbstractMoveDelegate.MoveType.DEFAULT, List.of());
    return filter.filterUnitsThatCanMove(units, Map.of());
  }

  private Collection<Unit> germanyUnits(final Predicate<Unit> predicate) {
    return CollectionUtils.getMatches(germany.getUnits(), predicate);
  }

  @Test
  @DisplayName("moving infantry and tanks two territories should filter to just tanks")
  void onlyTanksCanMoveTwo() {
    final Route route = new Route(germany, easternEurope, kareliaSsr);
    final Collection<Unit> units = germanyUnits(Matches.unitIsOfTypes(infantryType, armourType));
    assertThat(units, hasSize(5));
    final Collection<Unit> justTanks = germanyUnits(Matches.unitIsOfType(armourType));
    assertThat(justTanks, hasSize(2));

    final var result = filterUnits(route, units);
    assertThat(result.getErrorMessage(), is(nullValue()));
    assertThat(result.getWarningMessage(), is("Not all units have enough movement"));
    assertThat(result.getUnitsWithDependents(), containsInAnyOrder(justTanks.toArray()));
  }

  @Test
  @DisplayName("moving infantry two territories is not possible")
  void noInfantryCanMoveTwo() {
    final Route route = new Route(germany, easternEurope, kareliaSsr);
    final Collection<Unit> infantry = germanyUnits(Matches.unitIsOfTypes(infantryType));
    assertThat(infantry, hasSize(3));

    final var result = filterUnits(route, infantry);
    assertThat(result.getErrorMessage(), is("Not all units have enough movement"));
    assertThat(result.getWarningMessage(), is(nullValue()));
    assertThat(result.getUnitsWithDependents(), is(empty()));
  }

  @Test
  @DisplayName("tanks can move two spaces")
  void tanksCanMoveTwo() {
    final Route route = new Route(germany, easternEurope, kareliaSsr);
    final Collection<Unit> tanks = germanyUnits(Matches.unitIsOfTypes(armourType));
    assertThat(tanks, hasSize(2));

    final var result = filterUnits(route, tanks);
    assertThat(result.getErrorMessage(), is(nullValue()));
    assertThat(result.getWarningMessage(), is(nullValue()));
    assertThat(result.getUnitsWithDependents(), containsInAnyOrder(tanks.toArray()));
  }

  @Test
  @DisplayName("infantry can move one space")
  void infantryCanMoveOne() {
    final Route route = new Route(germany, easternEurope);
    final Collection<Unit> infantry = germanyUnits(Matches.unitIsOfTypes(infantryType));
    assertThat(infantry, hasSize(3));

    final var result = filterUnits(route, infantry);
    assertThat(result.getErrorMessage(), is(nullValue()));
    assertThat(result.getWarningMessage(), is(nullValue()));
    assertThat(result.getUnitsWithDependents(), containsInAnyOrder(infantry.toArray()));
  }
}
