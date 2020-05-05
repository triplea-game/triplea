package games.strategy.triplea.ui.panel.move;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.panel.move.MovableUnitsFilter.FilterOperationResult;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

public class MovableUnitsFilterTest {

  private List<UnitType> getUnitTypes(final FilterOperationResult result) {
    return result.getUnitsWithDependents().stream().map(Unit::getType).collect(Collectors.toList());
  }

  private Matcher<Optional<String>> isNotAllUnitsHaveEnoughMovement() {
    return isPresentAndIs("Not all units have enough movement");
  }

  private Matcher<Optional<String>> isNotEnoughTransports() {
    return isPresentAndIs("Not enough transports");
  }

  private FilterOperationResult filterUnits(
      final GameData gameData,
      final GamePlayer player,
      final Route route,
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> dependentUnits) {
    final IDelegateBridge bridge = newDelegateBridge(player);
    advanceToStep(bridge, "CombatMove");
    final MovableUnitsFilter filter =
        new MovableUnitsFilter(
            gameData,
            player,
            route,
            false,
            AbstractMoveDelegate.MoveType.DEFAULT,
            List.of(),
            dependentUnits);
    return filter.filterUnitsThatCanMove(units);
  }

  @Nested
  public class RevisedTests {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final GamePlayer germans = germans(gameData);
    final Territory germany = territory("Germany", gameData);
    final Territory easternEurope = territory("Eastern Europe", gameData);
    final Territory kareliaSsr = territory("Karelia S.S.R.", gameData);
    final Territory sz5 = territory("5 Sea Zone", gameData);
    final UnitType infantryType = infantry(gameData);
    final UnitType armourType = armour(gameData);

    private Collection<Unit> germanyUnits(final Predicate<Unit> predicate) {
      return CollectionUtils.getMatches(germany.getUnits(), predicate);
    }

    private Collection<Unit> germanyTanks() {
      final Collection<Unit> units = germanyUnits(Matches.unitIsOfTypes(armourType));
      assertThat(units, hasSize(2));
      return units;
    }

    private Collection<Unit> germanyInfantry() {
      final Collection<Unit> units = germanyUnits(Matches.unitIsOfTypes(infantryType));
      assertThat(units, hasSize(3));
      return units;
    }

    private Collection<Unit> germanyTanksAndInfantry() {
      final Collection<Unit> units = germanyUnits(Matches.unitIsOfTypes(infantryType, armourType));
      assertThat(units, hasSize(5));
      return units;
    }

    @Test
    @DisplayName("moving infantry and tanks two territories should filter to just tanks")
    void onlyTanksCanMoveTwo() {
      final Route route = new Route(germany, easternEurope, kareliaSsr);
      final Collection<Unit> units = germanyTanksAndInfantry();
      final Collection<Unit> justTanks = germanyTanks();

      final var result = filterUnits(gameData, germans, route, units, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), isNotAllUnitsHaveEnoughMovement());
      assertThat(result.getUnitsWithDependents(), containsInAnyOrder(justTanks.toArray()));
    }

    @Test
    @DisplayName("moving infantry two territories is not possible")
    void noInfantryCanMoveTwo() {
      final Route route = new Route(germany, easternEurope, kareliaSsr);
      final Collection<Unit> infantry = germanyInfantry();

      final var result = filterUnits(gameData, germans, route, infantry, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.NO_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), isNotAllUnitsHaveEnoughMovement());
      assertThat(result.getUnitsWithDependents(), is(empty()));
    }

    @Test
    @DisplayName("tanks can move two spaces")
    void tanksCanMoveTwo() {
      final Route route = new Route(germany, easternEurope, kareliaSsr);
      final Collection<Unit> tanks = germanyTanks();

      final var result = filterUnits(gameData, germans, route, tanks, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
      assertThat(result.getUnitsWithDependents(), containsInAnyOrder(tanks.toArray()));
    }

    @Test
    @DisplayName("infantry can move one space")
    void infantryCanMoveOne() {
      final Route route = new Route(germany, easternEurope);
      final Collection<Unit> infantry = germanyInfantry();

      final var result = filterUnits(gameData, germans, route, infantry, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
      assertThat(result.getUnitsWithDependents(), containsInAnyOrder(infantry.toArray()));
    }

    @Test
    @DisplayName("moving 3 infantry and 2 tanks onto a transport loads 1 infantry and 1 tank")
    void filterMixedUnitsLoadingOntoTransport() throws Exception {
      final Route route = new Route(germany, sz5);
      final Collection<Unit> units = germanyTanksAndInfantry();

      final var result = filterUnits(gameData, germans, route, units, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), isNotEnoughTransports());
      assertThat(getUnitTypes(result), containsInAnyOrder(infantryType, armourType));
    }

    @Test
    @DisplayName("moving 3 infantry onto a transport loads 2 infantry")
    void filterInfantryLoadingOntoTransport() throws Exception {
      final Route route = new Route(germany, sz5);
      final Collection<Unit> units = germanyInfantry();

      final var result = filterUnits(gameData, germans, route, units, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), isNotEnoughTransports());
      assertThat(getUnitTypes(result), containsInAnyOrder(infantryType, infantryType));
    }

    @Test
    @DisplayName("moving 2 tanks onto a transport loads 1 tank")
    void filterTankLoadingOntoTransport() throws Exception {
      final Route route = new Route(germany, sz5);
      final Collection<Unit> units = germanyTanks();

      final var result = filterUnits(gameData, germans, route, units, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), isNotEnoughTransports());
      assertThat(getUnitTypes(result), containsInAnyOrder(armourType));
    }

    @Test
    @DisplayName("moving 1 tank and 1 infantry onto a transport loads them both")
    void filterFittingMixedUnitsLoadingOntoTransport() throws Exception {
      final Route route = new Route(germany, sz5);
      final Collection<Unit> units =
          List.of(germanyTanks().iterator().next(), germanyInfantry().iterator().next());

      final var result = filterUnits(gameData, germans, route, units, Map.of());
      assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
      assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
      assertThat(getUnitTypes(result), containsInAnyOrder(infantryType, armourType));
    }
  }

  @Nested
  public class WW2v3Tests {
    final GameData data = TestMapGameData.WW2V3_1942.getGameData();
    final GamePlayer russians = russians(data);
    final Territory russia = territory("Russia", data);
    final Territory caucasus = territory("Caucasus", data);
    final Territory persia = territory("Persia", data);
    final Territory archangel = territory("Archangel", data);
    final Territory karelia = territory("Karelia S.S.R.", data);
    final Territory finland = territory("Finland", data);
    final UnitType bomberType = bomber(data);
    final UnitType infType = infantry(data);
    final UnitType tankType = armour(data);

    @Test
    @DisplayName("infantry can move 2 spaces when paired with tanks and mech infantry tech")
    void mechInfantry() {
      final Predicate<Unit> infantryAndTanks = Matches.unitIsOfTypes(infType, tankType);
      final Collection<Unit> units =
          CollectionUtils.getMatches(russia.getUnits(), infantryAndTanks);
      assertThat(units, hasSize(5));

      final Route route = new Route(russia, caucasus, persia);

      // Without mech infantry tech, only 2 tanks can move.
      {
        final var result = filterUnits(data, russians, route, units, Map.of());
        assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
        assertThat(result.getWarningOrErrorMessage(), isNotAllUnitsHaveEnoughMovement());
        assertThat(getUnitTypes(result), containsInAnyOrder(tankType, tankType));
      }

      // With mech infantry tech, 2 infantry and 2 tanks can move.
      TechAttachment.get(russians).setMechanizedInfantry("true");
      {
        final var result = filterUnits(data, russians, route, units, Map.of());
        assertThat(result.getStatus(), is(FilterOperationResult.Status.SOME_UNITS_CAN_MOVE));
        assertThat(result.getWarningOrErrorMessage(), isNotAllUnitsHaveEnoughMovement());
        assertThat(getUnitTypes(result), containsInAnyOrder(infType, infType, tankType, tankType));
      }
    }

    @Test
    @DisplayName("paratroopers tech allows paratroopers to move with bombers")
    void paratroopers() {
      final Collection<Unit> allUnits = russia.getUnits();
      final Unit bomber =
          allUnits.stream().filter(Matches.unitIsOfTypes(bomberType)).findAny().get();
      final Unit infantry =
          allUnits.stream().filter(Matches.unitIsOfTypes(infType)).findAny().get();
      final Route route = new Route(russia, archangel, karelia, finland);
      final Map<Unit, Collection<Unit>> dependentUnits = Map.of(bomber, List.of(infantry));

      // With paratroopers tech, the bomber can carry an infantry.
      TechAttachment.get(russians).setParatroopers("true");
      {
        final var result = filterUnits(data, russians, route, List.of(bomber), dependentUnits);
        assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
        assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
        assertThat(getUnitTypes(result), containsInAnyOrder(infType, bomberType));
      }

      // Without the tech, only the bomber can move.
      TechAttachment.get(russians).setParatroopers("false");
      {
        final var result = filterUnits(data, russians, route, List.of(bomber), dependentUnits);
        // TODO: This should probably be SOME_UNITS_CAN_MOVE, but the UI code never actually passes
        // invalid dependent units when the tech doesn't exist, so this does not matter much.
        assertThat(result.getStatus(), is(FilterOperationResult.Status.ALL_UNITS_CAN_MOVE));
        assertThat(result.getWarningOrErrorMessage(), not(isPresent()));
        assertThat(getUnitTypes(result), containsInAnyOrder(bomberType));
      }
    }
  }
}
