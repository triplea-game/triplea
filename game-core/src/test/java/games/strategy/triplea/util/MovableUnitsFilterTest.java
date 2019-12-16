package games.strategy.triplea.util;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MockDelegateBridge;
import games.strategy.triplea.util.MovableUnitsFilter.FilterOperationResult;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

  private IDelegateBridge newDelegateBridge(final PlayerId player) {
    return MockDelegateBridge.newInstance(gameData, player);
  }

  private FilterOperationResult filterUnits(
      final PlayerId player, final Route route, final Collection<Unit> units) {
    final IDelegateBridge bridge = newDelegateBridge(player);
    advanceToStep(bridge, "NonCombatMove");
    final MovableUnitsFilter filter =
        new MovableUnitsFilter(
            player, route, false, AbstractMoveDelegate.MoveType.DEFAULT, List.of());
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

    final var result = filterUnits(germans, route, units);
    assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
    assertThat(
        result.getWarningOrErrorMessage(), isPresentAndIs("Not all units have enough movement"));
    assertThat(result.getUnitsWithDependents(), containsInAnyOrder(justTanks.toArray()));
  }

  @Test
  @DisplayName("moving infantry two territories is not possible")
  void noInfantryCanMoveTwo() {
    final Route route = new Route(germany, easternEurope, kareliaSsr);
    final Collection<Unit> infantry = germanyUnits(Matches.unitIsOfTypes(infantryType));
    assertThat(infantry, hasSize(3));

    final var result = filterUnits(germans, route, infantry);
    assertThat(result.getStatus(), is(FilterOperationResult.Status.NO_UNITS_CAN_MOVE));
    assertThat(
        result.getWarningOrErrorMessage(), isPresentAndIs("Not all units have enough movement"));
    assertThat(result.getUnitsWithDependents(), is(empty()));
  }

  @Test
  @DisplayName("tanks can move two spaces")
  void tanksCanMoveTwo() {
    final Route route = new Route(germany, easternEurope, kareliaSsr);
    final Collection<Unit> tanks = germanyUnits(Matches.unitIsOfTypes(armourType));
    assertThat(tanks, hasSize(2));

    final var result = filterUnits(germans, route, tanks);
    assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
    assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
    assertThat(result.getUnitsWithDependents(), containsInAnyOrder(tanks.toArray()));
  }

  @Test
  @DisplayName("infantry can move one space")
  void infantryCanMoveOne() {
    final Route route = new Route(germany, easternEurope);
    final Collection<Unit> infantry = germanyUnits(Matches.unitIsOfTypes(infantryType));
    assertThat(infantry, hasSize(3));

    final var result = filterUnits(germans, route, infantry);
    assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
    assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
    assertThat(result.getUnitsWithDependents(), containsInAnyOrder(infantry.toArray()));
  }

  @Test
  @DisplayName("infantry can move 2 spaces when paired with tanks and mech infantry tech")
  void mechInfantry() {
    final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
    final PlayerId russians = russians(gameData);
    final Territory russia = territory("Russia", gameData);
    final Territory caucasus = territory("Caucasus", gameData);
    final Territory persia = territory("Persia", gameData);

    final Predicate<Unit> infantryAndTanks = Matches.unitIsOfTypes(infantryType, armourType);
    final Collection<Unit> units = CollectionUtils.getMatches(russia.getUnits(), infantryAndTanks);
    assertThat(units, hasSize(5));

    final Route route = new Route(russia, caucasus, persia);

    // Without mech infantry tech, only tanks can move.
    var result = filterUnits(russians, route, units);
    assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
    assertThat(
        result.getWarningOrErrorMessage(), isPresentAndIs("Not all units have enough movement"));
    Collection<UnitType> unitTypes =
        result.getUnitsWithDependents().stream().map(u -> u.getType()).collect(Collectors.toList());
    assertThat(unitTypes, containsInAnyOrder(armourType, armourType));

    // With mech infantry tech, 2 infantry and 2 tanks can move.
    TechAttachment.get(russians).setMechanizedInfantry("true");

    result = filterUnits(russians, route, units);
    assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
    assertThat(
        result.getWarningOrErrorMessage(), isPresentAndIs("Not all units have enough movement"));
    unitTypes =
        result.getUnitsWithDependents().stream().map(u -> u.getType()).collect(Collectors.toList());
    assertThat(unitTypes, containsInAnyOrder(infantryType, infantryType, armourType, armourType));
  }
}
