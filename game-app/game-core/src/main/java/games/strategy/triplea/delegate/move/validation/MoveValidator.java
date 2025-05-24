package games.strategy.triplea.delegate.move.validation;

import static games.strategy.triplea.delegate.EditDelegate.getEditMode;
import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.CanalAttachment;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.data.MoveValidationResult;
import games.strategy.triplea.delegate.data.MustMoveWithDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/** Responsible for validating unit movement. */
@AllArgsConstructor
public class MoveValidator {

  public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE =
      "Transport has already unloaded units in a previous phase";
  public static final String
      TRANSPORT_MAY_NOT_UNLOAD_TO_FRIENDLY_TERRITORIES_UNTIL_AFTER_COMBAT_IS_RESOLVED =
          "Transport may not unload to friendly territories until after combat is resolved";
  public static final String ENEMY_SUBMARINE_PREVENTING_UNESCORTED_AMPHIBIOUS_ASSAULT_LANDING =
      "Enemy Submarine Preventing Unescorted Amphibious Assault Landing";
  public static final String TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO =
      "Transport has already unloaded units to ";
  public static final String CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND =
      "Cannot load and unload an allied transport in the same round";
  public static final String CANT_MOVE_THROUGH_IMPASSABLE =
      "Can't move through impassable territories";
  public static final String CANT_MOVE_THROUGH_RESTRICTED =
      "Can't move through restricted territories";
  public static final String TOO_POOR_TO_VIOLATE_NEUTRALITY =
      "Not enough money to pay for violating neutrality";
  public static final String CANNOT_VIOLATE_NEUTRALITY = "Cannot violate neutrality";
  public static final String NOT_ALL_AIR_UNITS_CAN_LAND = "Not all air units can land";
  public static final String TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT =
      "Transport cannot both load AND unload after being in combat";
  public static final String TRANSPORT_CANNOT_LOAD_AFTER_COMBAT =
      "Transport cannot load after being in combat";
  public static final String NOT_ALL_UNITS_CAN_BLITZ = "Not all units can blitz";
  public static final String CANNOT_BLITZ_OUT_OF_BATTLE_INTO_ENEMY_TERRITORY =
      "Cannot blitz out of a battle into enemy territory";
  public static final String NOT_ALL_UNITS_CAN_BLITZ_OUT_OF_EMPTY_ENEMY_TERRITORY =
      "Not all units can blitz out of empty enemy territory";
  public static final String CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY =
      "Cannot blitz out of a battle further into enemy territory";

  private final GameData data;
  private final boolean isNonCombat;

  public MoveValidationResult validateMove(final MoveDescription move, final GamePlayer player) {
    return validateMove(move, player, List.of());
  }

  /** Validates the specified move. */
  public MoveValidationResult validateMove(
      final MoveDescription move, final GamePlayer player, final List<UndoableMove> undoableMoves) {
    final Collection<Unit> units = move.getUnits();
    final Route route = move.getRoute();
    final Map<Unit, Unit> unitsToSeaTransports = move.getUnitsToSeaTransports();
    final Map<Unit, Collection<Unit>> airTransportDependents = move.getAirTransportsDependents();
    final MoveValidationResult result = new MoveValidationResult();
    if (route.hasNoSteps()) {
      return result;
    }
    if (validateFirst(units, route, player, result).hasError()) {
      return result;
    }
    if (isNonCombat) {
      if (validateNonCombat(units, route, player, result).hasError()) {
        return result;
      }
    } else {
      if (validateCombat(units, airTransportDependents, route, player, result).hasError()) {
        return result;
      }
    }
    if (validateNonEnemyUnitsOnPath(units, route, player, result).hasError()) {
      return result;
    }
    if (validateBasic(units, route, player, unitsToSeaTransports, airTransportDependents, result)
        .hasError()) {
      return result;
    }
    if (AirMovementValidator.validateAirCanLand(units, route, player, result).hasError()) {
      return result;
    }
    if (validateTransport(undoableMoves, units, route, player, unitsToSeaTransports, result)
        .hasError()) {
      return result;
    }
    if (validateParatroops(units, airTransportDependents, route, player, result).hasError()) {
      return result;
    }
    if (validateCanal(units, route, player, airTransportDependents, result).hasError()) {
      return result;
    }
    if (validateFuel(units, route, player, result).hasError()) {
      return result;
    }

    // Don't let the user move out of a battle zone, the exception is air units and unloading units
    // into a battle zone
    if (AbstractMoveDelegate.getBattleTracker(data).hasPendingNonBombingBattle(route.getStart())
        && units.stream().anyMatch(Matches.unitIsNotAir())) {
      // if the units did not move into the territory, then they can move out this will happen if
      // there is a submerged
      // sub in the area, and a different unit moved into the sea zone setting up a battle but the
      // original unit can
      // still remain
      boolean unitsStartedInTerritory = true;
      for (final Unit unit : units) {
        if (AbstractMoveDelegate.getRouteUsedToMoveInto(undoableMoves, unit, route.getEnd())
            != null) {
          unitsStartedInTerritory = false;
          break;
        }
      }
      if (!unitsStartedInTerritory) {
        final boolean unload = route.isUnload();
        final GamePlayer endOwner = route.getEnd().getOwner();
        final boolean attack =
            !endOwner.isAllied(player)
                || AbstractMoveDelegate.getBattleTracker(data).wasConquered(route.getEnd());
        // unless they are unloading into another battle
        if (!(unload && attack)) {
          return result.setErrorReturnResult("Cannot move units out of battle zone");
        }
      }
    }
    return result;
  }

  private MoveValidationResult validateFirst(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    if (validateFirstUnits(units, route, result).hasError()) return result;

    if (validateFirstRoute(route, units, player, result).hasError()) return result;

    if (!getEditMode(data.getProperties())) {
      final Collection<Unit> matches =
          CollectionUtils.getMatches(
              units,
              Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, player, true)
                  .negate());
      if (matches.isEmpty() || !matches.stream().allMatch(Matches.unitIsOwnedBy(player))) {
        result.setError(
            String.format(
                "Player, %s, is not owner of all the units: %s",
                player.getName(), MyFormatter.unitsToTextNoOwner(units)));
        return result;
      }
    }

    return result;
  }

  private MoveValidationResult validateFirstRoute(
      Route route, Collection<Unit> units, GamePlayer player, MoveValidationResult result) {
    if (!data.getMap().isValidRoute(route)) {
      return result.setErrorReturnResult("Invalid route: " + route);
    } else if (validateMovementRestrictedByTerritory(route, player, result).hasError()) {
      return result;
    }
    // cannot enter territories owned by a player to which we are neutral towards
    final Collection<Territory> landOnRoute = route.getMatches(Matches.territoryIsLand());
    // TODO: if this ever changes, we need to also update getBestRoute(), because getBestRoute is
    // also checking to make sure we avoid land territories owned by nations with these 2
    // relationship type attachment options
    for (final Territory t : landOnRoute) {
      if (units.stream().anyMatch(Matches.unitIsLand())
          && !data.getRelationshipTracker().canMoveLandUnitsOverOwnedLand(player, t.getOwner())) {
        return result.setErrorReturnResult(
            player.getName()
                + " may not move land units over land owned by "
                + t.getOwner().getName());
      }
      if (units.stream().anyMatch(Matches.unitIsAir())
          && !data.getRelationshipTracker().canMoveAirUnitsOverOwnedLand(player, t.getOwner())) {
        return result.setErrorReturnResult(
            player.getName()
                + " may not move air units over land owned by "
                + t.getOwner().getName());
      }
    }
    return result;
  }

  private static MoveValidationResult validateFirstUnits(
      Collection<Unit> units, Route route, MoveValidationResult result) {
    if (units.isEmpty()) {
      result.setErrorReturnResult("No units");
    }
    // make sure all units are actually in the start territory
    else if (!route.getStart().getUnitCollection().containsAll(units)) {
      result.setErrorReturnResult("Not enough units in starting territory");
    }
    // this should never happen
    else if (new HashSet<>(units).size() != units.size()) {
      result.setError("Not all units unique, units: " + units + " unique: " + new HashSet<>(units));
    } else {
      units.forEach(
          unit -> {
            if (unit.getSubmerged()) {
              result.addDisallowedUnit("Cannot move submerged units", unit);
            } else if (Matches.unitIsDisabled().test(unit)) {
              result.addDisallowedUnit("Cannot move disabled units", unit);
            }
          });
    }
    return result;
  }

  private MoveValidationResult validateFuel(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    if (getEditMode(data.getProperties())) {
      return result;
    }
    if (!Properties.getUseFuelCost(data.getProperties())) {
      return result;
    }
    final ResourceCollection fuelCost = Route.getMovementFuelCostCharge(units, route, player, data);
    if (player.getResources().has(fuelCost.getResourcesCopy())) {
      return result;
    }
    return result.setErrorReturnResult(
        "Not enough resources to perform this move, you need: " + fuelCost + " for this move");
  }

  private MoveValidationResult validateCanal(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final MoveValidationResult result) {
    if (getEditMode(data.getProperties())) {
      return result;
    }
    // TODO: merge validateCanal here and provide granular unit warnings
    return result.setErrorReturnResult(validateCanal(route, units, airTransportDependents, player));
  }

  /**
   * Test a route's canals to see if you can move through it.
   *
   * @param units (Can be null. If null we will assume all units would be stopped by the canal.)
   */
  public @Nullable String validateCanal(
      final Route route, @Nullable final Collection<Unit> units, final GamePlayer player) {
    return validateCanal(route, units, Map.of(), player);
  }

  @Nullable
  String validateCanal(
      final Route route,
      @Nullable final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final GamePlayer player) {
    Map<Territory, Collection<CanalAttachment>> territoryCanals = new HashMap<>();
    int numCanals = 0;
    for (Territory t : route.getAllTerritories()) {
      // Only check canals that are on the route
      var canals = CanalAttachment.get(t, route);
      territoryCanals.put(t, canals);
      numCanals += canals.size();
    }
    if (numCanals == 0) {
      return null;
    }
    final boolean mustControlAllCanals =
        Properties.getControlAllCanalsBetweenTerritoriesToPass(data.getProperties());

    // Check each unit 1 by 1 to see if they can move through necessary canals on route
    String result = null;
    final Set<Unit> unitsThatFailCanal = new HashSet<>();
    final Set<Unit> setWithNull = new HashSet<>();
    setWithNull.add(null);
    final Collection<Unit> unitsWithoutDependents =
        (units == null) ? setWithNull : findNonDependentUnits(units, route, airTransportDependents);
    for (final Unit unit : unitsWithoutDependents) {
      for (final Territory t : route.getAllTerritories()) {
        Optional<String> failureMessage = Optional.empty();
        for (CanalAttachment canalAttachment : territoryCanals.get(t)) {
          failureMessage = canPassThroughCanal(canalAttachment, unit, player);
          final boolean canPass = failureMessage.isEmpty();
          if (mustControlAllCanals != canPass) {
            // If need to control any canal and can pass OR need to control all and can't pass.
            break;
          }
        }
        if (failureMessage.isPresent()) {
          result = failureMessage.get();
          unitsThatFailCanal.add(unit);
        }
      }
    }
    if (result == null || units == null) {
      return result;
    }

    // If any units failed canal check then try to land transport them
    final Set<Unit> potentialLandTransports =
        unitsWithoutDependents.stream()
            .filter(
                unit ->
                    !unitsThatFailCanal.contains(unit)
                        && Matches.unitHasEnoughMovementForRoute(route).test(unit))
            .collect(Collectors.toSet());
    final Set<Unit> unitsToLandTransport =
        unitsWithoutDependents.stream()
            .filter(
                unit ->
                    unitsThatFailCanal.contains(unit)
                        || !Matches.unitHasEnoughMovementForRoute(route).test(unit))
            .collect(Collectors.toSet());
    return checkLandTransports(player, potentialLandTransports, unitsToLandTransport).isEmpty()
        ? null
        : result;
  }

  /**
   * Simplified version of {@link #validateCanal(Route, Collection, GamePlayer) validateCanal} used
   * for route finding to check neighboring territories and avoid validating every unit. Performance
   * of this method is critical.
   */
  public boolean canAnyUnitsPassCanal(
      final Territory start,
      final Territory end,
      final Collection<Unit> units,
      final GamePlayer player) {
    boolean canPass = true;
    final Route route = new Route(start, end);
    for (final CanalAttachment canalAttachment : CanalAttachment.get(start, route)) {
      final Collection<Unit> unitsWithoutDependents = findNonDependentUnits(units, route, Map.of());
      canPass = canAnyPassThroughCanal(canalAttachment, unitsWithoutDependents, player).isEmpty();
      final boolean mustControlAllCanals =
          Properties.getControlAllCanalsBetweenTerritoriesToPass(data.getProperties());
      if (mustControlAllCanals != canPass) {
        // If need to control any canal and can pass OR need to control all and can't pass.
        break;
      }
    }
    return canPass;
  }

  private MoveValidationResult validateCombat(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    if (getEditMode(data.getProperties())) {
      return result;
    }
    for (final Territory t : route.getSteps()) {
      if (!Matches.territoryOwnerRelationshipTypeCanMoveIntoDuringCombatMove(player).test(t)) {
        return result.setErrorReturnResult(
            "Cannot move into territories owned by "
                + t.getOwner().getName()
                + " during Combat Movement Phase");
      }
    }

    // We are in a contested territory owned by the enemy, and we want to move to another enemy
    // owned territory. Do not allow unless each unit can blitz the current territory or allowed by
    // property.
    if (!Properties.getAllUnitsCanAttackFromContestedTerritories(data.getProperties())
        && !route.getStart().isWater()
        && Matches.isAtWar(route.getStart().getOwner()).test(player)
        && (route.anyMatch(Matches.isTerritoryEnemy(player))
            && !route.allMatchMiddleSteps(Matches.isTerritoryEnemy(player).negate()))) {
      if (!Matches.territoryIsBlitzable(player).test(route.getStart())
          && !units.stream().allMatch(Matches.unitIsAir())) {
        return result.setErrorReturnResult(CANNOT_BLITZ_OUT_OF_BATTLE_FURTHER_INTO_ENEMY_TERRITORY);
      }
      Predicate<Unit> disallowed = Matches.unitCanBlitz().negate().and(Matches.unitIsNotAir());
      for (final Unit u : CollectionUtils.getMatches(units, disallowed)) {
        result.addDisallowedUnit(NOT_ALL_UNITS_CAN_BLITZ_OUT_OF_EMPTY_ENEMY_TERRITORY, u);
      }
    }

    // We are in a contested territory owned by us, and we want to move to an enemy owned territory.
    // Do not allow unless the territory is blitzable or allowed by property.
    if (!Properties.getAllUnitsCanAttackFromContestedTerritories(data.getProperties())
        && !route.getStart().isWater()
        && !Matches.isAtWar(route.getStart().getOwner()).test(player)
        && (route.anyMatch(Matches.isTerritoryEnemy(player))
            && !route.allMatchMiddleSteps(Matches.isTerritoryEnemy(player).negate()))
        && !Matches.territoryIsBlitzable(player).test(route.getStart())
        && !units.stream().allMatch(Matches.unitIsAir())) {
      return result.setErrorReturnResult(CANNOT_BLITZ_OUT_OF_BATTLE_INTO_ENEMY_TERRITORY);
    }

    // Don't allow aa guns (and other disallowed units) to move in combat unless they are in a
    // transport
    if (units.stream().anyMatch(Matches.unitCanNotMoveDuringCombatMove())
        && (!route.getStart().isWater() || !route.getEnd().isWater())) {
      for (final Unit unit :
          CollectionUtils.getMatches(units, Matches.unitCanNotMoveDuringCombatMove())) {
        result.addDisallowedUnit("Cannot move AA guns in combat movement phase", unit);
      }
    }

    // If there is a neutral in the middle must stop unless all are air or getNeutralsBlitzable
    if (nonAirPassingThroughNeutralTerritory(route, units, data.getProperties())) {
      return result.setErrorReturnResult(
          "Must stop land units when passing through neutral territories");
    }
    if (units.stream().anyMatch(Matches.unitIsLand()) && route.hasSteps()) {
      // Check all the territories but the end, if there are enemy territories, make sure they are
      // blitzable
      // if they are not blitzable, or we aren't all blitz units fail
      int enemyCount = 0;
      boolean allEnemyBlitzable = true;
      for (final Territory current : route.getMiddleSteps()) {
        if (current.isWater()) {
          continue;
        }
        if (current.getOwner().isAtWar(player)
            || AbstractMoveDelegate.getBattleTracker(data).wasConquered(current)) {
          enemyCount++;
          allEnemyBlitzable &= Matches.territoryIsBlitzable(player).test(current);
        }
      }
      if (enemyCount > 0 && !allEnemyBlitzable) {
        if (nonParatroopersPresent(player, units)) {
          return result.setErrorReturnResult("Cannot blitz on that route");
        }
      } else if (allEnemyBlitzable && !(route.getStart().isWater() || route.getEnd().isWater())) {
        final Predicate<Unit> blitzingUnit = Matches.unitCanBlitz().or(Matches.unitIsAir());
        final Predicate<Unit> nonBlitzing = blitzingUnit.negate();
        // Only validate units without dependents (exclude units being transported).
        final Collection<Unit> unitsWithoutDependents =
            findNonDependentUnits(units, route, airTransportDependents);
        final Collection<Unit> nonBlitzingUnits =
            CollectionUtils.getMatches(unitsWithoutDependents, nonBlitzing);
        // remove any units that gain blitz due to certain abilities
        nonBlitzingUnits.removeAll(
            UnitAttachment.getUnitsWhichReceivesAbilityWhenWith(
                units, "canBlitz", data.getUnitTypeList()));
        final Predicate<Territory> territoryIsNotEnd = Matches.territoryIs(route.getEnd()).negate();
        final Predicate<Territory> nonFriendlyTerritories =
            Matches.isTerritoryFriendly(player).negate();
        final Predicate<Territory> notEndOrFriendlyTerrs =
            nonFriendlyTerritories.and(territoryIsNotEnd);
        final Predicate<Territory> foughtOver =
            Matches.territoryWasFoughtOver(AbstractMoveDelegate.getBattleTracker(data));
        final Predicate<Territory> notEndWasFought = territoryIsNotEnd.and(foughtOver);
        final boolean wasStartFoughtOver =
            AbstractMoveDelegate.getBattleTracker(data).wasConquered(route.getStart())
                || AbstractMoveDelegate.getBattleTracker(data).wasBlitzed(route.getStart());
        nonBlitzingUnits.addAll(
            CollectionUtils.getMatches(
                units,
                Matches.unitIsOfTypes(
                    TerritoryEffectHelper.getUnitTypesThatLostBlitz(
                        (wasStartFoughtOver ? route.getAllTerritories() : route.getSteps())))));
        if (!wasStartFoughtOver
            && !route.anyMatch(notEndOrFriendlyTerrs)
            && !route.anyMatch(notEndWasFought)) {
          nonBlitzingUnits.removeIf(not(Unit::getWasInCombat));
        }
        // For any units that couldn't blitz, see if they can be land transported instead.
        if (!nonBlitzingUnits.isEmpty()) {
          final Collection<Unit> potentialLandTransports =
              CollectionUtils.difference(units, nonBlitzingUnits);
          // Mark any non-transportable units as disallowed.
          for (Unit unit : checkLandTransports(player, potentialLandTransports, nonBlitzingUnits)) {
            result.addDisallowedUnit(NOT_ALL_UNITS_CAN_BLITZ, unit);
          }
        }
      }
    }

    // check aircraft
    if (route.hasSteps()
        && units.stream().anyMatch(Matches.unitIsAir())
        && (!Properties.getNeutralFlyoverAllowed(data.getProperties())
            || Properties.getNeutralsImpassable(data.getProperties()))
        && (route.getMiddleSteps().stream().anyMatch(Matches.territoryIsNeutralButNotWater())
            || Matches.territoryIsNeutralButNotWater().test(route.getStart())
                && units.stream().anyMatch(Matches.unitIsAir().and(Matches.unitHasMoved())))) {
      return result.setErrorReturnResult("Air units cannot fly over neutral territories");
    }
    // make sure no conquered territories on route
    // unless we are all air, or we are in non combat OR the route is water (was a bug in convoy
    // zone movement)
    if (hasConqueredNonBlitzedNonWaterOnRoute(route, data)
        && !units.stream().allMatch(Matches.unitIsAir())) {
      // what if we are paratroopers?
      return result.setErrorReturnResult("Cannot move through newly captured territories");
    }
    // See if they've already been in combat
    if (units.stream().anyMatch(Matches.unitWasInCombat())
        && units.stream().anyMatch(Matches.unitWasUnloadedThisTurn())
        && Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player)
            .test(route.getEnd())
        && !route.getEnd().getUnitCollection().isEmpty()) {
      return result.setErrorReturnResult("Units cannot participate in multiple battles");
    }
    // See if we are doing invasions in combat phase, with units or transports that can't do
    // invasion.
    if (route.isUnload() && Matches.isTerritoryEnemy(player).test(route.getEnd())) {
      for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitCanInvade().negate())) {
        result.addDisallowedUnit(
            unit.getType().getName()
                + " can't invade from "
                + unit.getTransportedBy().getType().getName(),
            unit);
      }
    }
    return result;
  }

  private static boolean nonAirPassingThroughNeutralTerritory(
      final Route route, final Collection<Unit> units, final GameProperties properties) {
    return route.hasNeutralBeforeEnd()
        && !units.stream().allMatch(Matches.unitIsAir())
        && !isNeutralsBlitzable(properties);
  }

  private MoveValidationResult validateNonCombat(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    if (getEditMode(data.getProperties())) {
      return result;
    }
    if (route.anyMatch(Matches.territoryIsImpassable())) {
      return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSABLE);
    }
    if (!route.anyMatch(Matches.territoryIsPassableAndNotRestricted(player))) {
      return result.setErrorReturnResult(CANT_MOVE_THROUGH_RESTRICTED);
    }
    final Predicate<Territory> neutralOrEnemy =
        Matches.territoryIsNeutralButNotWater()
            .or(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(player));
    final boolean navalMayNotNonComIntoControlled =
        Properties.getWW2V2(data.getProperties())
            || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(
                data.getProperties());
    // TODO need to account for subs AND transports that are ignored, not just OR
    final Territory end = route.getEnd();
    if (neutralOrEnemy.test(end)) {
      // a convoy zone is controlled, so we must make sure we can still move there if there are
      // actual battle there
      if (!end.isWater() || navalMayNotNonComIntoControlled) {
        return result.setErrorReturnResult("Cannot advance units to battle in non combat");
      }
    }
    // Subs can't move under destroyers
    if (units.stream().allMatch(Matches.unitCanMoveThroughEnemies())
        && enemyDestroyerOnPath(route, player)) {
      return result.setErrorReturnResult("Cannot move submarines under destroyers");
    }
    // Can't advance to battle unless only ignored units on route, only air units to sea, or only
    // units that can enter  territories with enemy units during NCM
    if (end.anyUnitsMatch(Matches.enemyUnit(player).and(Matches.unitIsSubmerged().negate()))
        && !onlyIgnoredUnitsOnPath(route, player, false)
        && !(end.isWater() && units.stream().allMatch(Matches.unitIsAir()))
        && !(Properties.getSubsCanEndNonCombatMoveWithEnemies(data.getProperties())
            && units.stream().allMatch(Matches.unitCanMoveThroughEnemies()))) {
      return result.setErrorReturnResult("Cannot advance to battle in non combat");
    }
    // if there are enemy units on the path blocking us, that is validated elsewhere
    // (validateNonEnemyUnitsOnPath)
    // now check if we can move over neutral or enemies territories in noncombat
    if (units.stream().allMatch(Matches.unitIsAir())
        || (units.stream().noneMatch(Matches.unitIsSea())
            && !nonParatroopersPresent(player, units))) {
      // if there are non-paratroopers present, then we cannot fly over stuff
      // if there are neutral territories in the middle, we cannot fly over (unless allowed to)
      // otherwise we can generally fly over anything in noncombat
      if (route.anyMatch(
              Matches.territoryIsNeutralButNotWater().and(Matches.territoryIsWater().negate()))
          && (!Properties.getNeutralFlyoverAllowed(data.getProperties())
              || Properties.getNeutralsImpassable(data.getProperties()))) {
        return result.setErrorReturnResult(
            "Air units cannot fly over neutral territories in non combat");
      }
      // if sea units, or land units moving over/onto sea (ex: loading onto a transport), then only
      // check if old rules stop us
    } else if (units.stream().anyMatch(Matches.unitIsSea())
        || route.anyMatch(Matches.territoryIsWater())) {
      // if there are neutral or owned territories, we cannot move through them (only under old
      // rules. under new rules we can move through owned sea zones.)
      if (navalMayNotNonComIntoControlled && route.anyMatch(neutralOrEnemy)) {
        return result.setErrorReturnResult(
            "Cannot move units through neutral or enemy territories in non combat");
      }
    } else {
      if (route.anyMatch(neutralOrEnemy)) {
        return result.setErrorReturnResult(
            "Cannot move units through neutral or enemy territories in non combat");
      }
    }
    return result;
  }

  // Added to handle restriction of movement to listed territories
  private MoveValidationResult validateMovementRestrictedByTerritory(
      final Route route, final GamePlayer player, final MoveValidationResult result) {
    if (getEditMode(data.getProperties())) {
      return result;
    }
    if (!Properties.getMovementByTerritoryRestricted(data.getProperties())) {
      return result;
    }
    final RulesAttachment ra = player.getRulesAttachment();
    if (ra == null || ra.getMovementRestrictionTerritories() == null) {
      return result;
    }
    final String movementRestrictionType = ra.getMovementRestrictionType();
    final Collection<Territory> listedTerritories =
        ra.getListedTerritories(ra.getMovementRestrictionTerritories(), true, true);
    if (movementRestrictionType.equals("allowed")) {
      for (final Territory current : route.getAllTerritories()) {
        if (!listedTerritories.contains(current)) {
          return result.setErrorReturnResult("Cannot move outside restricted territories");
        }
      }
    } else if (movementRestrictionType.equals("disallowed")) {
      for (final Territory current : route.getAllTerritories()) {
        if (listedTerritories.contains(current)) {
          return result.setErrorReturnResult("Cannot move to restricted territories");
        }
      }
    }
    return result;
  }

  private MoveValidationResult validateNonEnemyUnitsOnPath(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    if (getEditMode(data.getProperties())) {
      return result;
    }
    // check to see no enemy units on path
    if (noEnemyUnitsOnPathMiddleSteps(route, player)) {
      return result;
    }
    // if we are all air, then it's ok
    if (units.stream().allMatch(Matches.unitIsAir())) {
      return result;
    }
    // subs may possibly carry units...
    final Collection<Unit> matches =
        CollectionUtils.getMatches(units, Matches.unitIsBeingTransported().negate());
    if (!matches.isEmpty() && matches.stream().allMatch(Matches.unitCanMoveThroughEnemies())) {
      // this is ok unless there are destroyer on the path
      return enemyDestroyerOnPath(route, player)
          ? result.setErrorReturnResult("Cannot move submarines under destroyers")
          : result;
    }
    if (onlyIgnoredUnitsOnPath(route, player, true)) {
      return result;
    }
    // omit paratroops
    if (nonParatroopersPresent(player, units)) {
      return result.setErrorReturnResult("Enemy units on path");
    }
    return result;
  }

  private MoveValidationResult validateBasic(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final Map<Unit, Unit> unitsToSeaTransports,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final MoveValidationResult result) {
    final boolean isEditMode = getEditMode(data.getProperties());
    // make sure transports in the destination
    if (!route.getEnd().getUnitCollection().containsAll(unitsToSeaTransports.values())
        && !units.containsAll(unitsToSeaTransports.values())) {
      return result.setErrorReturnResult("Transports not found in route end");
    }
    // All units in air transport map must be present in the units collection.
    if (!units.containsAll(airTransportDependents.keySet())
        || airTransportDependents.values().stream().anyMatch(not(units::containsAll))) {
      return result.setErrorReturnResult("Air transports map contains units not being moved");
    }
    if (!isEditMode) {
      // Make sure all units are at least friendly
      for (final Unit unit : CollectionUtils.getMatches(units, Matches.enemyUnit(player))) {
        result.addDisallowedUnit("Can only move friendly units", unit);
      }

      // Ensure all air transports are included
      for (final Unit airTransport : airTransportDependents.keySet()) {
        if (!units.contains(airTransport)) {
          for (final Unit unit : airTransportDependents.get(airTransport)) {
            if (units.contains(unit)) {
              result.addDisallowedUnit("Not all units have enough movement", unit);
            }
          }
        }
      }

      // Check that units have enough movement considering land transports
      final Collection<Unit> unitsWithoutDependents =
          findNonDependentUnits(units, route, airTransportDependents);
      final Set<Unit> unitsWithEnoughMovement =
          unitsWithoutDependents.stream()
              .filter(unit -> Matches.unitHasEnoughMovementForRoute(route).test(unit))
              .collect(Collectors.toSet());
      final Set<Unit> unitsWithoutEnoughMovement =
          unitsWithoutDependents.stream()
              .filter(unit -> !Matches.unitHasEnoughMovementForRoute(route).test(unit))
              .collect(Collectors.toSet());
      checkLandTransports(player, unitsWithEnoughMovement, unitsWithoutEnoughMovement)
          .forEach(unit -> result.addDisallowedUnit("Not all units have enough movement", unit));

      // Can only move owned units except transported units or allied air on carriers
      for (final Unit unit :
          CollectionUtils.getMatches(
              unitsWithoutDependents, Matches.unitIsOwnedBy(player).negate())) {
        if (!(unit.getUnitAttachment().getCarrierCost() > 0 && player.isAllied(unit.getOwner()))) {
          result.addDisallowedUnit("Can only move own troops", unit);
        }
      }

      // if there is a neutral in the middle must stop unless all are air or getNeutralsBlitzable
      if (nonAirPassingThroughNeutralTerritory(route, units, data.getProperties())) {
        return result.setErrorReturnResult(
            "Must stop land units when passing through neutral territories");
      }
      // a territory effect can disallow unit types in
      if (units.stream()
          .anyMatch(
              Matches.unitIsOfTypes(
                  TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(
                      route.getSteps())))) {
        return result.setErrorReturnResult(
            "Territory Effects disallow some units into "
                + (route.numberOfSteps() > 1 ? "these territories" : "this territory"));
      }
      // Check requiresUnitsToMove conditions
      Collection<Unit> requiresUnitsToMoveList = unitsWithoutDependents;
      if (route.isUnload()) {
        requiresUnitsToMoveList = units;
      }
      for (final Territory t : route.getAllTerritories()) {
        if (!requiresUnitsToMoveList.stream().allMatch(Matches.unitHasRequiredUnitsToMove(t))) {
          return result.setErrorReturnResult(
              t.getName()
                  + " doesn't have the required units to allow moving the selected units into it");
        }
      }
    } // !isEditMode

    // make sure that no non sea non transportable no carriable units end at sea
    if (route.getEnd().isWater()) {
      for (final Unit unit : getUnitsThatCantGoOnWater(units)) {
        result.addDisallowedUnit("Not all units can end at water", unit);
      }
    }
    // if we are water make sure no land
    if (units.stream().anyMatch(Matches.unitIsSea()) && route.hasLand()) {
      for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitIsSea())) {
        result.addDisallowedUnit("Sea units cannot go on land", unit);
      }
    }

    // test for stack limits per unit
    final PlayerAttachment pa = PlayerAttachment.get(player);
    final Set<Triple<Integer, String, Set<UnitType>>> playerMovementLimit =
        (pa != null ? pa.getMovementLimit() : Set.of());
    final Set<Triple<Integer, String, Set<UnitType>>> playerAttackingLimit =
        (pa != null ? pa.getAttackingLimit() : Set.of());
    final Predicate<Unit> hasMovementOrAttackingLimit =
        unit -> {
          final var ua = unit.getUnitAttachment();
          if (ua.getMovementLimit() != null || ua.getAttackingLimit() != null) {
            return true;
          }
          for (final var limit : playerMovementLimit) {
            if (limit.getThird().contains(unit.getType())) {
              return true;
            }
          }
          for (final var limit : playerAttackingLimit) {
            if (limit.getThird().contains(unit.getType())) {
              return true;
            }
          }
          return false;
        };
    final Collection<Unit> unitsWithStackingLimits =
        CollectionUtils.getMatches(units, hasMovementOrAttackingLimit);
    for (final Territory t : route.getSteps()) {
      final String limitType;
      if (Matches.isTerritoryEnemyAndNotUnownedWater(player).test(t)
          || t.anyUnitsMatch(Matches.unitIsEnemyOf(player))) {
        limitType = UnitStackingLimitFilter.ATTACKING_LIMIT;
      } else {
        limitType = UnitStackingLimitFilter.MOVEMENT_LIMIT;
      }
      final Collection<Unit> allowedUnits =
          UnitStackingLimitFilter.filterUnits(unitsWithStackingLimits, limitType, player, t);
      for (Unit unit : CollectionUtils.difference(unitsWithStackingLimits, allowedUnits)) {
        result.addDisallowedUnit(
            "Unit type " + unit.getType().getName() + " has reached stacking limit", unit);
      }
    }

    // Don't allow move through impassable territories
    if (!isEditMode && route.anyMatch(Matches.territoryIsImpassable())) {
      return result.setErrorReturnResult(CANT_MOVE_THROUGH_IMPASSABLE);
    }
    if (canPayToCrossNeutralTerritory(route, player, result).hasError()) {
      return result;
    }
    if (Properties.getNeutralsImpassable(data.getProperties())
        && !isNeutralsBlitzable(data.getProperties())
        && !route.getMatches(Matches.territoryIsNeutralButNotWater()).isEmpty()) {
      return result.setErrorReturnResult(CANNOT_VIOLATE_NEUTRALITY);
    }
    return result;
  }

  private static Collection<Unit> findNonDependentUnits(
      final Collection<Unit> units,
      final Route route,
      final Map<Unit, Collection<Unit>> airTransportDependents) {
    final Collection<Unit> unitsWithoutDependents = new ArrayList<>();
    unitsWithoutDependents.addAll(route.getStart().isWater() ? getNonLand(units) : units);
    unitsWithoutDependents.removeIf(u -> u.getTransportedBy() != null);
    for (Collection<Unit> deps : airTransportDependents.values()) {
      unitsWithoutDependents.removeAll(deps);
    }
    return unitsWithoutDependents;
  }

  /**
   * Returns any units that couldn't be land transported and handles both styles of land transports:
   * 1. Transport units on a 1-to-1 basis (have no capacity set) 2. Transport like sea transports
   * using capacity and cost
   */
  private Set<Unit> checkLandTransports(
      final GamePlayer player,
      final Collection<Unit> possibleLandTransports,
      final Collection<Unit> unitsToLandTransport) {
    final Set<Unit> disallowedUnits = new HashSet<>();

    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      int numLandTransportsWithoutCapacity =
          getNumLandTransportsWithoutCapacity(possibleLandTransports, player);
      final IntegerMap<Unit> landTransportsWithCapacity =
          getLandTransportsWithCapacity(possibleLandTransports, player);
      for (final Unit unit : TransportUtils.sortByTransportCostDescending(unitsToLandTransport)) {
        boolean unitOk = false;
        if (Matches.unitHasNotMoved().test(unit) && Matches.unitIsLandTransportable().test(unit)) {
          if (numLandTransportsWithoutCapacity > 0) {
            numLandTransportsWithoutCapacity--;
            unitOk = true;
          } else {
            for (final Unit transport : landTransportsWithCapacity.keySet()) {
              final int cost = unit.getUnitAttachment().getTransportCost();
              if (cost <= landTransportsWithCapacity.getInt(transport)) {
                landTransportsWithCapacity.add(transport, -cost);
                unitOk = true;
                break;
              }
            }
          }
        }
        if (!unitOk) {
          disallowedUnits.add(unit);
        }
      }
    }
    return disallowedUnits;
  }

  private static int getNumLandTransportsWithoutCapacity(
      final Collection<Unit> units, final GamePlayer player) {
    if (player.getTechAttachment().getMechanizedInfantry()) {
      final Predicate<Unit> transportLand =
          Matches.unitIsLandTransportWithoutCapacity().and(Matches.unitIsOwnedBy(player));
      return CollectionUtils.countMatches(units, transportLand);
    }
    return 0;
  }

  private static IntegerMap<Unit> getLandTransportsWithCapacity(
      final Collection<Unit> units, final GamePlayer player) {
    final IntegerMap<Unit> map = new IntegerMap<>();
    if (player.getTechAttachment().getMechanizedInfantry()) {
      final Predicate<Unit> transportLand =
          Matches.unitIsLandTransportWithCapacity().and(Matches.unitIsOwnedBy(player));
      for (final Unit unit : CollectionUtils.getMatches(units, transportLand)) {
        map.put(unit, unit.getUnitAttachment().getTransportCapacity());
      }
    }
    return map;
  }

  /**
   * Checks that there are no enemy units on the route except possibly at the end. Submerged enemy
   * units are not considered as they don't affect movement. AA and factory dont count as enemy.
   */
  private boolean noEnemyUnitsOnPathMiddleSteps(final Route route, final GamePlayer player) {
    final Predicate<Unit> alliedOrNonCombat =
        Matches.unitIsInfrastructure()
            .or(Matches.enemyUnit(player).negate())
            .or(Matches.unitIsSubmerged());
    // Submerged units do not interfere with movement
    return route.getMiddleSteps().stream()
        .allMatch(current -> current.getUnitCollection().allMatch(alliedOrNonCombat));
  }

  /**
   * Checks that there only transports, subs and/or allies on the route except at the end. AA and
   * factory dont count as enemy.
   */
  public boolean onlyIgnoredUnitsOnPath(
      final Route route, final GamePlayer player, final boolean ignoreRouteEnd) {
    final Predicate<Unit> transportOnly =
        Matches.unitIsInfrastructure()
            .or(Matches.unitIsSeaTransportButNotCombatSeaTransport())
            .or(Matches.unitIsLand())
            .or(Matches.enemyUnit(player).negate());
    final Predicate<Unit> subOnly =
        Matches.unitIsInfrastructure()
            .or(Matches.unitCanBeMovedThroughByEnemies())
            .or(Matches.enemyUnit(player).negate());
    final Predicate<Unit> transportOrSubOnly = transportOnly.or(subOnly);
    final boolean getIgnoreTransportInMovement =
        Properties.getIgnoreTransportInMovement(data.getProperties());
    List<Territory> steps;
    if (ignoreRouteEnd) {
      steps = route.getMiddleSteps();
    } else {
      steps = route.getSteps();
      // if there are no steps, then we began in this sea zone, so see if there are ignored units in
      // this sea zone (not sure if we need !ignoreRouteEnd here).
      if (steps.isEmpty()) {
        steps = List.of(route.getStart());
      }
    }
    boolean validMove = false;
    for (final Territory current : steps) {
      if (current.isWater()) {
        if ((getIgnoreTransportInMovement
                && current.getUnitCollection().allMatch(transportOrSubOnly))
            || current.getUnitCollection().allMatch(subOnly)) {
          validMove = true;
          continue;
        }
        return false;
      }
    }
    return validMove;
  }

  private boolean enemyDestroyerOnPath(final Route route, final GamePlayer player) {
    final Predicate<Unit> enemyDestroyer = Matches.unitIsDestroyer().and(Matches.enemyUnit(player));
    return route.getMiddleSteps().stream()
        .anyMatch(current -> current.anyUnitsMatch(enemyDestroyer));
  }

  private static boolean hasConqueredNonBlitzedNonWaterOnRoute(
      final Route route, final GameData data) {
    for (final Territory current : route.getMiddleSteps()) {
      if (!Matches.territoryIsWater().test(current)
          && AbstractMoveDelegate.getBattleTracker(data).wasConquered(current)
          && !AbstractMoveDelegate.getBattleTracker(data).wasBlitzed(current)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if moving the specified units along the specified route requires them to
   * be loaded/unloaded on/off a transport.
   */
  // TODO KEV revise these to include paratroop load/unload
  public static boolean isLoad(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final Route route,
      final GamePlayer player) {
    final Map<Unit, Collection<Unit>> alreadyLoaded =
        mustMoveWith(route.getStart(), units, airTransportDependents, player);
    if (route.hasNoSteps() && alreadyLoaded.isEmpty()) {
      return false;
    }
    // See if we even need to go to the trouble of checking for AirTransported units
    final boolean checkForAlreadyTransported = !route.getStart().isWater() && route.hasWater();
    if (checkForAlreadyTransported) {
      // TODO Leaving unitIsTransport() for potential use with amphib transports (hovercraft, ducks,
      // etc...)
      final List<Unit> transports =
          CollectionUtils.getMatches(
              units, Matches.unitIsSeaTransport().or(Matches.unitIsAirTransport()));
      final List<Unit> transportable =
          CollectionUtils.getMatches(
              units, Matches.unitCanBeTransported().or(Matches.unitIsAirTransportable()));
      // Check if there are transports in the group to be checked
      if (alreadyLoaded.keySet().containsAll(transports)) {
        // Check each transportable unit -vs those already loaded.
        for (final Unit unit : transportable) {
          boolean found = false;
          for (final Unit transport : transports) {
            if (alreadyLoaded.get(transport) == null
                || alreadyLoaded.get(transport).contains(unit)) {
              found = true;
              break;
            }
          }
          if (!found) {
            return true;
          }
        }
      } else {
        // TODO I think this is right
        return true;
      }
    }
    return false;
  }

  private static Collection<Unit> getUnitsThatCantGoOnWater(final Collection<Unit> units) {
    final Collection<Unit> retUnits = new ArrayList<>();
    for (final Unit unit : units) {
      final UnitAttachment ua = unit.getUnitAttachment();
      if (!ua.isSea() && !ua.isAir() && ua.getTransportCost() == -1) {
        retUnits.add(unit);
      }
    }
    return retUnits;
  }

  static boolean hasUnitsThatCantGoOnWater(final Collection<Unit> units) {
    return !getUnitsThatCantGoOnWater(units).isEmpty();
  }

  private static Collection<Unit> getNonLand(final Collection<Unit> units) {
    return CollectionUtils.getMatches(units, Matches.unitIsAir().or(Matches.unitIsSea()));
  }

  public static BigDecimal getMaxMovement(final Collection<Unit> units) {
    if (units.isEmpty()) {
      throw new IllegalArgumentException("no units");
    }
    BigDecimal max = BigDecimal.ZERO;
    for (final Unit unit : units) {
      final BigDecimal left = unit.getMovementLeft();
      max = left.max(max);
    }
    return max;
  }

  static BigDecimal getLeastMovement(final Collection<Unit> units) {
    if (units.isEmpty()) {
      throw new IllegalArgumentException("no units");
    }
    BigDecimal least = new BigDecimal(Integer.MAX_VALUE);
    for (final Unit unit : units) {
      final BigDecimal left = unit.getMovementLeft();
      least = left.min(least);
    }
    return least;
  }

  // Determines whether we can pay the neutral territory charge for a given route for air units. We
  // can't cross neutral
  // territories in WW2V2.
  private MoveValidationResult canPayToCrossNeutralTerritory(
      final Route route, final GamePlayer player, final MoveValidationResult result) {
    // neutrals we will overfly in the first place
    final Collection<Territory> neutrals = MoveDelegate.getEmptyNeutral(route);
    final int pus = player.isNull() ? 0 : player.getResources().getQuantity(Constants.PUS);
    if (pus < getNeutralCharge(data.getProperties(), neutrals.size())) {
      return result.setErrorReturnResult(TOO_POOR_TO_VIOLATE_NEUTRALITY);
    }
    return result;
  }

  private static Territory getTerritoryTransportHasUnloadedTo(
      final List<UndoableMove> undoableMoves, final Unit transport) {
    for (final UndoableMove undoableMove : undoableMoves) {
      if (undoableMove.wasTransportUnloaded(transport)) {
        return undoableMove.getRoute().getEnd();
      }
    }
    return null;
  }

  private MoveValidationResult validateTransport(
      final List<UndoableMove> undoableMoves,
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final Map<Unit, Unit> unitsToSeaTransports,
      final MoveValidationResult result) {
    if (!route.hasWater() || units.stream().allMatch(Matches.unitIsAir())) {
      return result;
    }
    // If there are non-sea transports return
    final boolean loadingNonSeaTransportsOnly =
        !unitsToSeaTransports.isEmpty()
            && unitsToSeaTransports.values().stream()
                .noneMatch(Matches.unitIsSea().and(Matches.unitCanTransport()));
    if (loadingNonSeaTransportsOnly) {
      return result;
    }
    final Territory routeEnd = route.getEnd();
    final Territory routeStart = route.getStart();
    // if unloading make sure length of route is only 1
    final boolean isEditMode = getEditMode(data.getProperties());
    if (!isEditMode && route.isUnload()) {
      if (route.hasMoreThanOneStep()) {
        return result.setErrorReturnResult("Unloading units must stop where they are unloaded");
      }
      for (final Unit unit : TransportTracker.getUnitsLoadedOnAlliedTransportsThisTurn(units)) {
        result.addDisallowedUnit(
            CANNOT_LOAD_AND_UNLOAD_AN_ALLIED_TRANSPORT_IN_THE_SAME_ROUND, unit);
      }
      final Collection<Unit> transports = TransportUtils.mapTransports(route, units, null).values();
      final boolean isScramblingOrKamikazeAttacksEnabled =
          Properties.getScrambleRulesInEffect(data.getProperties())
              || Properties.getUseKamikazeSuicideAttacks(data.getProperties());
      final boolean subsPreventUnescortedAmphibAssaults =
          Properties.getSubmarinesPreventUnescortedAmphibiousAssaults(data.getProperties());
      final Predicate<Unit> enemySubMatch =
          Matches.unitIsEnemyOf(player).and(Matches.unitCanBeMovedThroughByEnemies());
      final Predicate<Unit> ownedSeaNonTransportMatch =
          Matches.unitIsOwnedBy(player)
              .and(Matches.unitIsSea())
              .and(Matches.unitIsNotSeaTransportButCouldBeCombatSeaTransport());
      for (final Unit transport : transports) {
        if (!isNonCombat) {
          if (Matches.territoryHasEnemyUnits(player).test(routeEnd)
              || Matches.isTerritoryEnemyAndNotUnownedWater(player).test(routeEnd)) {
            // this is an amphibious assault
            if (subsPreventUnescortedAmphibAssaults
                && !Matches.territoryHasUnitsThatMatch(ownedSeaNonTransportMatch).test(routeStart)
                && Matches.territoryHasUnitsThatMatch(enemySubMatch).test(routeStart)) {
              // we must have at least one warship (non-transport) unit, otherwise the enemy sub
              // stops our unloading for amphibious assault
              for (final Unit unit : transport.getTransporting()) {
                result.addDisallowedUnit(
                    ENEMY_SUBMARINE_PREVENTING_UNESCORTED_AMPHIBIOUS_ASSAULT_LANDING, unit);
              }
            }
          } else if (!AbstractMoveDelegate.getBattleTracker(data).wasConquered(routeEnd)
              &&
              // this is an unload to a friendly territory
              (isScramblingOrKamikazeAttacksEnabled
                  || !Matches.territoryIsEmptyOfCombatUnits(player).test(routeStart))) {
            // Unloading a transport from a sea zone with a battle, to a friendly land territory,
            // during combat move phase, is illegal and in addition to being illegal, it is also
            // causing problems if
            // the sea transports get killed (the land units are not dying)
            // TODO: should we use the battle tracker for this instead?
            for (final Unit unit : transport.getTransporting()) {
              result.addDisallowedUnit(
                  TRANSPORT_MAY_NOT_UNLOAD_TO_FRIENDLY_TERRITORIES_UNTIL_AFTER_COMBAT_IS_RESOLVED,
                  unit);
            }
          }
        }
        // TODO This is very sensitive to the order of the transport collection. The users may need
        // to modify the order in which they perform their actions. check whether transport has
        // already unloaded
        if (TransportTracker.hasTransportUnloadedInPreviousPhase(transport)) {
          for (final Unit unit : transport.getTransporting()) {
            result.addDisallowedUnit(
                TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, unit);
          }
          // check whether transport is restricted to another territory
        } else if (TransportTracker.isTransportUnloadRestrictedToAnotherTerritory(
            transport, route.getEnd())) {
          final Territory alreadyUnloadedTo =
              getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
          if (alreadyUnloadedTo != null) {
            for (final Unit unit : transport.getTransporting()) {
              result.addDisallowedUnit(
                  TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), unit);
            }
          }
          // Check if the transport has already loaded after being in combat
        } else if (TransportTracker.isTransportUnloadRestrictedInNonCombat(transport)) {
          for (final Unit unit : transport.getTransporting()) {
            result.addDisallowedUnit(TRANSPORT_CANNOT_LOAD_AND_UNLOAD_AFTER_COMBAT, unit);
          }
        }
      }
    }
    // if we are land make sure no water in route except for transport situations
    final Collection<Unit> land = CollectionUtils.getMatches(units, Matches.unitIsLand());
    final Collection<Unit> landAndAir =
        CollectionUtils.getMatches(units, Matches.unitIsLand().or(Matches.unitIsAir()));
    // make sure we can be transported
    final Predicate<Unit> cantBeTransported = Matches.unitCanBeTransported().negate();
    for (final Unit unit : CollectionUtils.getMatches(land, cantBeTransported)) {
      result.addDisallowedUnit("Not all units can be transported", unit);
    }
    // make sure that the only the first or last territory is land don't want situation where they
    // go sea land sea
    if (!isEditMode
        && route.hasLand()
        && !(route.getStart().isWater() || route.getEnd().isWater())
        &&
        // needs to include all land and air to work, since it makes sure the land units can be
        // carried by the air and that the air has enough capacity
        nonParatroopersPresent(player, landAndAir)) {
      return result.setErrorReturnResult(
          "Invalid move, only start or end can be land when route has water.");
    }

    // simply because I don't want to handle it yet checks are done at the start and end, don't want
    // to worry about just using a transport as a bridge yet
    // TODO handle this
    if (!isEditMode
        && !route.getEnd().isWater()
        && !route.getStart().isWater()
        && nonParatroopersPresent(player, landAndAir)) {
      return result.setErrorReturnResult("Must stop units at a transport on route");
    }
    if (route.getEnd().isWater() && route.getStart().isWater()) {
      // make sure units and transports stick together
      for (final Unit unit : units) {
        final UnitAttachment ua = unit.getUnitAttachment();
        // make sure transports dont leave their units behind
        if (ua.getTransportCapacity() != -1) {
          final Collection<Unit> holding = unit.getTransporting();
          if (!units.containsAll(holding)) {
            result.addDisallowedUnit("Transports cannot leave their units", unit);
          }
        }
        // make sure units don't leave their transports behind
        if (ua.getTransportCost() != -1) {
          final Unit transport = unit.getTransportedBy();
          if (transport != null && !units.contains(transport)) {
            result.addDisallowedUnit("Unit must stay with its transport while moving", unit);
          }
        }
      }
    }
    if (route.isLoad()) {
      if (!isEditMode && !route.hasExactlyOneStep() && nonParatroopersPresent(player, landAndAir)) {
        return result.setErrorReturnResult("Units cannot move before loading onto transports");
      }
      final Predicate<Unit> enemyNonSubmerged =
          Matches.enemyUnit(player).and(Matches.unitIsSubmerged().negate());
      if (!Properties.getUnitsCanLoadInHostileSeaZones(data.getProperties())
          && route.getEnd().anyUnitsMatch(enemyNonSubmerged)
          && nonParatroopersPresent(player, landAndAir)
          && !onlyIgnoredUnitsOnPath(route, player, false)
          && !AbstractMoveDelegate.getBattleTracker(data)
              .didAllThesePlayersJustGoToWarThisTurn(player, route.getEnd().getUnits())) {
        return result.setErrorReturnResult("Cannot load when enemy sea units are present");
      }
      if (!isEditMode) {
        for (final Unit baseUnit : land) {
          if (baseUnit.hasMoved()) {
            result.addDisallowedUnit("Units cannot move before loading onto transports", baseUnit);
          }
          final Unit transport = unitsToSeaTransports.get(baseUnit);
          if (transport == null) {
            continue;
          }
          if (TransportTracker.hasTransportUnloadedInPreviousPhase(transport)) {
            result.addDisallowedUnit(
                TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_IN_A_PREVIOUS_PHASE, baseUnit);
          } else if (TransportTracker.isTransportLoadRestrictedAfterCombat(transport)) {
            result.addDisallowedUnit(TRANSPORT_CANNOT_LOAD_AFTER_COMBAT, baseUnit);
          } else if (TransportTracker.isTransportUnloadRestrictedToAnotherTerritory(
              transport, route.getEnd())) {
            Territory alreadyUnloadedTo =
                getTerritoryTransportHasUnloadedTo(undoableMoves, transport);
            for (final Unit transportToLoad : unitsToSeaTransports.values()) {
              if (!TransportTracker.isTransportUnloadRestrictedToAnotherTerritory(
                  transportToLoad, route.getEnd())) {
                final UnitAttachment ua = baseUnit.getUnitAttachment();
                if (TransportTracker.getAvailableCapacity(transportToLoad)
                    >= ua.getTransportCost()) {
                  alreadyUnloadedTo = null;
                  break;
                }
              }
            }
            if (alreadyUnloadedTo != null) {
              result.addDisallowedUnit(
                  TRANSPORT_HAS_ALREADY_UNLOADED_UNITS_TO + alreadyUnloadedTo.getName(), baseUnit);
            }
          }
        }
      }
      if (!unitsToSeaTransports.keySet().containsAll(land)) {
        // some units didn't get mapped to a transport
        final Collection<UnitCategory> unitsToLoadCategories = UnitSeparator.categorize(land);
        if (unitsToSeaTransports.isEmpty() || unitsToLoadCategories.size() == 1) {
          // set all unmapped units as disallowed if there are no transports or only one unit
          // category
          for (final Unit unit : land) {
            if (unitsToSeaTransports.containsKey(unit)) {
              continue;
            }
            final UnitAttachment ua = unit.getUnitAttachment();
            if (ua.getTransportCost() != -1) {
              result.addDisallowedUnit("Not enough transports", unit);
            }
          }
        } else {
          // set all units as unresolved if there is at least one transport and mixed unit
          // categories
          for (final Unit unit : land) {
            final UnitAttachment ua = unit.getUnitAttachment();
            if (ua.getTransportCost() != -1) {
              result.addUnresolvedUnit("Not enough transports", unit);
            }
          }
        }
      }
    }
    return result;
  }

  public static boolean allLandUnitsAreBeingParatroopered(final Collection<Unit> units) {
    // some units that can't be paratrooped
    if (units.isEmpty()
        || !units.stream()
            .allMatch(
                Matches.unitIsAirTransportable()
                    .or(Matches.unitIsAirTransport())
                    .or(Matches.unitIsAir()))) {
      return false;
    }
    // final List<Unit> paratroopsRequiringTransport = getParatroopsRequiringTransport(units,
    // route);
    // due to various problems with units like tanks, we will assume that if we are in this method,
    // then all the land units need transports
    final List<Unit> paratroopsRequiringTransport =
        CollectionUtils.getMatches(units, Matches.unitIsAirTransportable());
    if (paratroopsRequiringTransport.isEmpty()) {
      return false;
    }
    final List<Unit> airTransports =
        CollectionUtils.getMatches(units, Matches.unitIsAirTransport());
    final List<Unit> allParatroops =
        TransportUtils.findUnitsToLoadOnAirTransports(paratroopsRequiringTransport, airTransports);
    if (!allParatroops.containsAll(paratroopsRequiringTransport)) {
      return false;
    }
    final Map<Unit, Unit> transportLoadMap =
        TransportUtils.mapTransportsToLoad(units, airTransports);
    return transportLoadMap.keySet().containsAll(paratroopsRequiringTransport);
  }

  // checks if there are non-paratroopers present that cause move validations to fail
  private static boolean nonParatroopersPresent(
      final GamePlayer player, final Collection<Unit> units) {
    if (!player.getTechAttachment().getParatroopers()) {
      return true;
    }
    if (!units.stream().allMatch(Matches.unitIsAir().or(Matches.unitIsLand()))) {
      return true;
    }
    if (units.stream().anyMatch(not(Matches.unitIsAirTransportable()).and(Matches.unitIsLand()))) {
      return true;
    }
    return !allLandUnitsAreBeingParatroopered(units);
  }

  private MoveValidationResult validateParatroops(
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    if (!player.getTechAttachment().getParatroopers()) {
      return result;
    }
    if (units.stream().noneMatch(Matches.unitIsAirTransportable())
        || units.stream().noneMatch(Matches.unitIsAirTransport())) {
      return result;
    }
    if (isNonCombat && !Properties.getParatroopersCanMoveDuringNonCombat(data.getProperties())) {
      return result.setErrorReturnResult("Paratroops may not move during NonCombat");
    }
    if (!getEditMode(data.getProperties())) {
      // if we can move without using paratroop tech, do so this allows moving a bomber/infantry
      // from one friendly territory to another
      final Map<Unit, Unit> paratroopersToAirTransports =
          convertTransportKeyedMapToLoadedUnitKeyedMap(airTransportDependents, result);
      if (result.hasError()) {
        return result;
      }
      for (final Unit airTransport : airTransportDependents.keySet()) {
        if (airTransport.hasMoved()) {
          result.addDisallowedUnit("Cannot move then transport paratroops", airTransport);
        }
      }
      final boolean friendlyEnd = Matches.isTerritoryFriendly(player).test(route.getEnd());
      final boolean canMoveNonCombat =
          Properties.getParatroopersCanMoveDuringNonCombat(data.getProperties());
      final boolean isWrongPhase = !isNonCombat && friendlyEnd && canMoveNonCombat;
      final boolean mustAdvanceToBattle = friendlyEnd && !canMoveNonCombat;
      for (final Unit paratroop : paratroopersToAirTransports.keySet()) {
        if (paratroop.hasMoved()) {
          result.addDisallowedUnit("Cannot paratroop units that have already moved", paratroop);
        }
        if (mustAdvanceToBattle) {
          result.addDisallowedUnit("Paratroops must advance to battle", paratroop);
        }
        if (isWrongPhase) {
          result.addDisallowedUnit(
              "Paratroops may only airlift during Non-Combat Movement Phase", paratroop);
        }
      }
      if (!Properties.getParatroopersCanAttackDeepIntoEnemyTerritory(data.getProperties())
          && route.getMiddleSteps().stream()
              .anyMatch(Matches.territoryIsLand().and(Matches.isTerritoryEnemy(player)))) {
        return result.setErrorReturnResult("Must stop paratroops in first enemy territory");
      }
    }
    return result;
  }

  private Map<Unit, Unit> convertTransportKeyedMapToLoadedUnitKeyedMap(
      final Map<Unit, Collection<Unit>> airTransportDependents, final MoveValidationResult result) {
    Map<Unit, Unit> unitsToTransport = new HashMap<>();
    for (Unit transport : airTransportDependents.keySet()) {
      int capacity = TransportTracker.getAvailableCapacity(transport);
      for (Unit beingTransported : airTransportDependents.get(transport)) {
        int cost = beingTransported.getUnitAttachment().getTransportCost();
        // Validate capacity, as airTransportDependents is coming from the move we're validating.
        if (capacity < cost) {
          // Note: The UI will doesn't allow such a move to be submitted, so we no need for a more
          // fancy error message here listing specific units.
          result.setError("Not all units could be air transported");
          return Map.of();
        }
        unitsToTransport.put(beingTransported, transport);
        capacity -= cost;
      }
    }
    return unitsToTransport;
  }

  private Optional<String> canPassThroughCanal(
      final CanalAttachment canalAttachment, final Unit unit, final GamePlayer player) {
    if (unit != null && Matches.unitIsOfTypes(canalAttachment.getExcludedUnits()).test(unit)) {
      return Optional.empty();
    }
    return checkCanalStepAndOwnership(canalAttachment, player);
  }

  private Optional<String> canAnyPassThroughCanal(
      final CanalAttachment canalAttachment,
      final Collection<Unit> units,
      final GamePlayer player) {
    if (units.stream().anyMatch(Matches.unitIsOfTypes(canalAttachment.getExcludedUnits()))) {
      return Optional.empty();
    }
    return checkCanalStepAndOwnership(canalAttachment, player);
  }

  private Optional<String> checkCanalStepAndOwnership(
      final CanalAttachment canalAttachment, final GamePlayer player) {
    if (canalAttachment.getCanNotMoveThroughDuringCombatMove()
        && GameStepPropertiesHelper.isCombatMove(data, true)) {
      return Optional.of(
          "Can only move through " + canalAttachment.getCanalName() + " during non-combat move");
    }
    for (final Territory borderTerritory : canalAttachment.getLandTerritories()) {
      if (!data.getRelationshipTracker().canMoveThroughCanals(player, borderTerritory.getOwner())) {
        return Optional.of("Must control " + canalAttachment.getCanalName() + " to move through");
      }
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(borderTerritory)) {
        return Optional.of(
            "Must control "
                + canalAttachment.getCanalName()
                + " for an entire turn to move through");
      }
    }
    return Optional.empty();
  }

  public static MustMoveWithDetails getMustMoveWith(
      final Territory start,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final GamePlayer player) {
    return new MustMoveWithDetails(
        mustMoveWith(start, start.getUnits(), airTransportDependents, player));
  }

  private static Map<Unit, Collection<Unit>> mustMoveWith(
      final Territory start,
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> airTransportDependents,
      final GamePlayer player) {
    final List<Unit> sortedUnits = new ArrayList<>(units);
    sortedUnits.sort(UnitComparator.getHighestToLowestMovementComparator());
    final Map<Unit, Collection<Unit>> mapping = transportsMustMoveWith(start, sortedUnits);
    // Check if there are combined transports (carriers that are transports) and load them.
    addToMapping(mapping, carrierMustMoveWith(sortedUnits, start, player));
    addToMapping(mapping, airTransportsMustMoveWith(start, sortedUnits, airTransportDependents));
    return mapping;
  }

  private static void addToMapping(
      final Map<Unit, Collection<Unit>> mapping, final Map<Unit, Collection<Unit>> newMapping) {
    for (final Map.Entry<Unit, Collection<Unit>> entry : newMapping.entrySet()) {
      mapping.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
    }
  }

  private static Map<Unit, Collection<Unit>> transportsMustMoveWith(
      final Territory start, final Collection<Unit> units) {
    final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<>();
    final Collection<Unit> transports =
        CollectionUtils.getMatches(units, Matches.unitIsSeaTransport());
    for (final Unit transport : transports) {
      final Collection<Unit> transporting = transport.getTransporting(start);
      if (!transporting.isEmpty()) {
        mustMoveWith.put(transport, new ArrayList<>(transporting));
      }
    }
    return mustMoveWith;
  }

  private static Map<Unit, Collection<Unit>> airTransportsMustMoveWith(
      final Territory start,
      final Collection<Unit> units,
      final Map<Unit, Collection<Unit>> airTransportDependents) {
    final Map<Unit, Collection<Unit>> mustMoveWith = new HashMap<>();
    final Collection<Unit> airTransports =
        CollectionUtils.getMatches(units, Matches.unitIsAirTransport());
    // Then check those that have already had their transportedBy set
    for (final Unit airTransport : airTransports) {
      Collection<Unit> transporting = airTransport.getTransporting(start);
      if (transporting.isEmpty()) {
        transporting = airTransportDependents.getOrDefault(airTransport, List.of());
      }
      if (!transporting.isEmpty()) {
        mustMoveWith.put(airTransport, transporting);
      }
    }
    return mustMoveWith;
  }

  public static Map<Unit, Collection<Unit>> carrierMustMoveWith(
      final Territory start, final GamePlayer player) {
    return carrierMustMoveWith(start.getUnits(), start.getUnits(), player);
  }

  public static Map<Unit, Collection<Unit>> carrierMustMoveWith(
      final Collection<Unit> units, final Territory start, final GamePlayer player) {
    return carrierMustMoveWith(units, start.getUnits(), player);
  }

  public static Map<Unit, Collection<Unit>> carrierMustMoveWith(
      final Collection<Unit> units, final Collection<Unit> startUnits, final GamePlayer player) {
    // we want to get all air units that are owned by our allies but not us that can land on a
    // carrier
    final Predicate<Unit> friendlyNotOwnedAir =
        Matches.alliedUnit(player)
            .and(Matches.unitIsOwnedBy(player).negate())
            .and(Matches.unitCanLandOnCarrier());
    final Collection<Unit> alliedAir = CollectionUtils.getMatches(startUnits, friendlyNotOwnedAir);
    if (alliedAir.isEmpty()) {
      return Map.of();
    }
    // remove air that can be carried by allied
    final Predicate<Unit> friendlyNotOwnedCarrier =
        Matches.unitIsCarrier()
            .and(Matches.alliedUnit(player))
            .and(Matches.unitIsOwnedBy(player).negate());
    final Collection<Unit> alliedCarrier =
        CollectionUtils.getMatches(startUnits, friendlyNotOwnedCarrier);
    for (final Unit carrier : alliedCarrier) {
      final Collection<Unit> carrying = getCanCarry(carrier, alliedAir, player);
      alliedAir.removeAll(carrying);
    }
    if (alliedAir.isEmpty()) {
      return Map.of();
    }
    final Map<Unit, Collection<Unit>> mapping = new HashMap<>();
    // get air that must be carried by our carriers
    final Collection<Unit> ownedCarrier =
        CollectionUtils.getMatches(
            units, Matches.unitIsCarrier().and(Matches.unitIsOwnedBy(player)));
    for (final Unit carrier : ownedCarrier) {
      final Collection<Unit> carrying = getCanCarry(carrier, alliedAir, player);
      alliedAir.removeAll(carrying);
      mapping.put(carrier, carrying);
    }
    return ImmutableMap.copyOf(mapping);
  }

  private static Collection<Unit> getCanCarry(
      final Unit carrier,
      final Collection<Unit> selectFrom,
      final GamePlayer playerWhoIsDoingTheMovement) {
    final UnitAttachment ua = carrier.getUnitAttachment();
    final Collection<Unit> canCarry = new ArrayList<>();
    int available = ua.getCarrierCapacity();
    for (final Unit plane : selectFrom) {
      final UnitAttachment planeAttachment = plane.getUnitAttachment();
      final int cost = planeAttachment.getCarrierCost();
      if (available >= cost
          &&
          // this is to test if they started in the same sea zone or not, and its not a very good
          // way of testing it.
          ((carrier.getAlreadyMoved().compareTo(plane.getAlreadyMoved()) == 0)
              || (Matches.unitHasNotMoved().test(plane) && Matches.unitHasNotMoved().test(carrier))
              || (Matches.unitIsOwnedBy(playerWhoIsDoingTheMovement).negate().test(plane)
                  && Matches.alliedUnit(playerWhoIsDoingTheMovement).test(plane)))) {
        available -= cost;
        canCarry.add(plane);
      }

      if (available == 0) {
        break;
      }
    }
    return canCarry;
  }

  /** Get the route ignoring forced territories. */
  public static @Nullable Route getBestRoute(
      @Nonnull final Territory start,
      @Nonnull final Territory end,
      final GameData data,
      final GamePlayer player,
      final Collection<Unit> units,
      final boolean forceLandOrSeaRoute) {
    final boolean hasLand = units.stream().anyMatch(Matches.unitIsLand());
    final boolean hasAir = units.stream().anyMatch(Matches.unitIsAir());
    final boolean isNeutralsImpassable =
        Properties.getNeutralsImpassable(data.getProperties())
            || (hasAir && !Properties.getNeutralFlyoverAllowed(data.getProperties()));
    final Predicate<Territory> noNeutral = Matches.territoryIsNeutralButNotWater().negate();
    final Predicate<Territory> noImpassableOrRestrictedOrNeutral =
        PredicateBuilder.of(Matches.territoryIsPassableAndNotRestricted(player))
            .and(Matches.territoryEffectsAllowUnits(units))
            .andIf(hasAir, Matches.territoryAllowsCanMoveAirUnitsOverOwnedLand(player))
            .andIf(hasLand, Matches.territoryAllowsCanMoveLandUnitsOverOwnedLand(player))
            .andIf(isNeutralsImpassable, noNeutral)
            .build();

    Route defaultRoute =
        data.getMap()
            .getRouteForUnits(start, end, noImpassableOrRestrictedOrNeutral, units, player);
    if (defaultRoute == null) {
      // Try for a route without impassable territories, but allowing restricted territories, since
      // there is a chance politics may change in the future
      defaultRoute =
          data.getMap()
              .getRoute(
                  start,
                  end,
                  (isNeutralsImpassable
                      ? noNeutral.and(Matches.territoryIsImpassable())
                      : Matches.territoryIsImpassable()));
      // There really is nothing, so just return any route, without conditions
      if (defaultRoute == null) {
        return data.getMap().getRoute(start, end, it -> true);
      }
      return defaultRoute;
    }

    // Avoid looking at the dependents
    final Collection<Unit> unitsWhichAreNotBeingTransportedOrDependent =
        CollectionUtils.getMatches(
            units,
            Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, player, true)
                .negate());

    // If start and end are land, try a land route. Don't force a land route, since planes may be
    // moving
    boolean mustGoLand = false;
    if (!start.isWater() && !end.isWater()) {
      final Route landRoute =
          data.getMap()
              .getRouteForUnits(
                  start,
                  end,
                  Matches.territoryIsLand().and(noImpassableOrRestrictedOrNeutral),
                  units,
                  player);
      if ((landRoute != null)
          && ((landRoute.numberOfSteps() <= defaultRoute.numberOfSteps())
              || (forceLandOrSeaRoute
                  && unitsWhichAreNotBeingTransportedOrDependent.stream()
                      .anyMatch(Matches.unitIsLand())))) {
        defaultRoute = landRoute;
        mustGoLand = true;
      }
    }

    // If the start and end are water, try and get a water route don't force a water route, since
    // planes may be moving
    boolean mustGoSea = false;
    if (start.isWater() && end.isWater()) {
      final Route waterRoute =
          data.getMap()
              .getRouteForUnits(
                  start,
                  end,
                  Matches.territoryIsWater().and(noImpassableOrRestrictedOrNeutral),
                  units,
                  player);
      if ((waterRoute != null)
          && ((waterRoute.numberOfSteps() <= defaultRoute.numberOfSteps())
              || (forceLandOrSeaRoute
                  && unitsWhichAreNotBeingTransportedOrDependent.stream()
                      .anyMatch(Matches.unitIsSea())))) {
        defaultRoute = waterRoute;
        mustGoSea = true;
      }
    }

    // These are the conditions we would like the route to satisfy, starting with the most important
    final Predicate<Territory> hasRequiredUnitsToMove =
        Matches.territoryHasRequiredUnitsToMove(unitsWhichAreNotBeingTransportedOrDependent);
    final Predicate<Territory> notEnemyOwned =
        Matches.isTerritoryEnemy(player)
            .negate()
            .and(
                Matches.territoryWasFoughtOver(AbstractMoveDelegate.getBattleTracker(data))
                    .negate());
    final Predicate<Territory> noEnemyUnits = Matches.territoryHasNoEnemyUnits(player);
    final Predicate<Territory> noAa = Matches.territoryHasEnemyAaForFlyOver(player).negate();
    final List<Predicate<Territory>> prioritizedMovePreferences =
        new ArrayList<>(
            List.of(
                hasRequiredUnitsToMove.and(notEnemyOwned).and(noEnemyUnits),
                hasRequiredUnitsToMove.and(noEnemyUnits),
                hasRequiredUnitsToMove.and(noAa),
                notEnemyOwned.and(noEnemyUnits),
                noEnemyUnits,
                noAa));

    // Determine max distance route is willing to accept
    final List<Unit> landUnits =
        CollectionUtils.getMatches(
            unitsWhichAreNotBeingTransportedOrDependent, Matches.unitIsLand());
    final int maxLandMoves = landUnits.isEmpty() ? 0 : getMaxMovement(landUnits).intValue();
    final int maxSteps =
        GameStepPropertiesHelper.isCombatMove(data)
            ? defaultRoute.numberOfSteps()
            : Math.max(defaultRoute.numberOfSteps(), maxLandMoves);

    // Try to find preferred route
    for (final Predicate<Territory> movePreference : prioritizedMovePreferences) {
      final Predicate<Territory> moveCondition;
      if (mustGoLand) {
        moveCondition =
            movePreference.and(Matches.territoryIsLand()).and(noImpassableOrRestrictedOrNeutral);
      } else if (mustGoSea) {
        moveCondition =
            movePreference.and(Matches.territoryIsWater()).and(noImpassableOrRestrictedOrNeutral);
      } else {
        moveCondition = movePreference.and(noImpassableOrRestrictedOrNeutral);
      }
      final Route route = data.getMap().getRouteForUnits(start, end, moveCondition, units, player);
      if ((route != null) && (route.numberOfSteps() <= maxSteps)) {
        return route;
      }
    }

    return defaultRoute;
  }

  private static boolean isNeutralsBlitzable(final GameProperties properties) {
    return Properties.getNeutralsBlitzable(properties)
        && !Properties.getNeutralsImpassable(properties);
  }

  private static int getNeutralCharge(
      final GameProperties properties, final int numberOfTerritories) {
    return numberOfTerritories * Properties.getNeutralCharge(properties);
  }

  public MoveValidationResult validateSpecialMove(
      final MoveDescription move, final GamePlayer player) {
    final Collection<Unit> units = move.getUnits();
    final Route route = move.getRoute();
    final MoveValidationResult result = new MoveValidationResult();
    if (validateFirst(units, route, player, result).hasError()) {
      return result;
    }
    if (validateFuel(move.getUnits(), move.getRoute(), player, result).hasError()) {
      return result;
    }
    final boolean isEditMode = getEditMode(data.getProperties());
    if (!isEditMode) {
      // make sure all units are at least friendly
      for (final Unit unit :
          CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(player).negate())) {
        result.addDisallowedUnit("Can only move owned units", unit);
      }
    }
    if (validateAirborneMovements(units, route, player, result).hasError()) {
      return result;
    }
    return result;
  }

  private static MoveValidationResult validateAirborneMovements(
      final Collection<Unit> units,
      final Route route,
      final GamePlayer player,
      final MoveValidationResult result) {
    final GameData data = player.getData();
    if (!TechAbilityAttachment.getAllowAirborneForces(
        TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()))) {
      return result.setErrorReturnResult("Do Not Have Airborne Tech");
    }
    final int airborneDistance =
        TechAbilityAttachment.getAirborneDistance(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    final Set<UnitType> airborneBases =
        TechAbilityAttachment.getAirborneBases(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    final Set<UnitType> airborneTypes =
        TechAbilityAttachment.getAirborneTypes(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    if (airborneDistance <= 0 || airborneBases.isEmpty() || airborneTypes.isEmpty()) {
      return result.setErrorReturnResult("Require Airborne Forces And Launch Capacity Tech");
    }
    if (route.numberOfSteps() > airborneDistance) {
      return result.setErrorReturnResult("Destination Is Out Of Range");
    }
    final Collection<GamePlayer> alliesForBases =
        data.getRelationshipTracker().getAllies(player, true);
    final Predicate<Unit> airborneBaseMatch = getAirborneMatch(airborneBases, alliesForBases);
    final Territory start = route.getStart();
    final Territory end = route.getEnd();
    final Collection<Unit> basesAtStart = start.getMatches(airborneBaseMatch);
    if (basesAtStart.isEmpty()) {
      return result.setErrorReturnResult("Require Airborne Base At Originating Territory");
    }

    final int airborneCapacity =
        TechAbilityAttachment.getAirborneCapacity(
            basesAtStart, TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier()));
    if (airborneCapacity <= 0) {
      return result.setErrorReturnResult("Airborne Bases Must Have Launch Capacity");
    } else if (airborneCapacity < units.size()) {
      final Collection<Unit> overMax = new ArrayList<>(units);
      overMax.removeAll(CollectionUtils.getNMatches(units, airborneCapacity, it -> true));
      for (final Unit u : overMax) {
        result.addDisallowedUnit("Airborne Base Capacity Has Been Reached", u);
      }
    }
    final Collection<Unit> airborne = new ArrayList<>();
    for (final Unit u : units) {
      if (!Matches.unitIsOwnedBy(player).test(u)) {
        result.addDisallowedUnit("Must Own All Airborne Forces", u);
      } else if (!Matches.unitIsOfTypes(airborneTypes).test(u)) {
        result.addDisallowedUnit("Can Only Launch Airborne Forces", u);
      } else if (Matches.unitIsDisabled().test(u)) {
        result.addDisallowedUnit("Must Not Be Disabled", u);
      } else if (!Matches.unitHasNotMoved().test(u)) {
        result.addDisallowedUnit("Must Not Have Previously Moved Airborne Forces", u);
      } else if (Matches.unitIsAirborne().test(u)) {
        result.addDisallowedUnit("Cannot Move Units Already Airborne", u);
      } else {
        airborne.add(u);
      }
    }
    if (airborne.isEmpty()) {
      return result;
    }
    final BattleTracker battleTracker = AbstractMoveDelegate.getBattleTracker(data);
    final boolean onlyWhereUnderAttackAlready =
        Properties.getAirborneAttacksOnlyInExistingBattles(data.getProperties());
    final boolean onlyEnemyTerritories =
        Properties.getAirborneAttacksOnlyInEnemyTerritories(data.getProperties());
    final List<Territory> steps = route.getSteps();
    if (steps.isEmpty()
        || !steps.stream().allMatch(Matches.territoryIsPassableAndNotRestricted(player))) {
      return result.setErrorReturnResult("May Not Fly Over Impassable or Restricted Territories");
    }
    if (steps.isEmpty()
        || !steps.stream().allMatch(Matches.territoryAllowsCanMoveAirUnitsOverOwnedLand(player))) {
      return result.setErrorReturnResult("May Only Fly Over Territories Where Air May Move");
    }
    final boolean someLand = airborne.stream().anyMatch(Matches.unitIsLand());
    final boolean someSea = airborne.stream().anyMatch(Matches.unitIsSea());
    final boolean land = Matches.territoryIsLand().test(end);
    final boolean sea = Matches.territoryIsWater().test(end);
    if (someLand && someSea) {
      return result.setErrorReturnResult("Cannot Mix Land and Sea Units");
    } else if (someLand) {
      if (!land) {
        return result.setErrorReturnResult("Cannot Move Land Units To Sea");
      }
    } else if (someSea && !sea) {
      return result.setErrorReturnResult("Cannot Move Sea Units To Land");
    }
    if (onlyWhereUnderAttackAlready) {
      if (!battleTracker.getConquered().contains(end)) {
        final IBattle battle = battleTracker.getPendingBattle(end, IBattle.BattleType.NORMAL);
        if (battle == null) {
          return result.setErrorReturnResult(
              "Airborne May Only Attack Territories Already Under Assault");
        } else if (land
            && someLand
            && battle.getAttackingUnits().stream().noneMatch(Matches.unitIsLand())) {
          return result.setErrorReturnResult(
              "Battle Must Have Some Land Units Participating Already");
        } else if (sea
            && someSea
            && battle.getAttackingUnits().stream().noneMatch(Matches.unitIsSea())) {
          return result.setErrorReturnResult(
              "Battle Must Have Some Sea Units Participating Already");
        }
      }
    } else if (onlyEnemyTerritories
        && !(Matches.isTerritoryEnemyAndNotUnownedWater(player).test(end)
            || Matches.territoryHasEnemyUnits(player).test(end))) {
      return result.setErrorReturnResult("Destination Must Be Enemy Or Contain Enemy Units");
    }
    return result;
  }

  public static Predicate<Unit> getAirborneBaseMatch(
      final GamePlayer player, final GameState data) {
    return getAirborneMatch(
        TechAbilityAttachment.getAirborneBases(
            TechTracker.getCurrentTechAdvances(player, data.getTechnologyFrontier())),
        data.getRelationshipTracker().getAllies(player, true));
  }

  public static Predicate<Unit> getAirborneMatch(
      final Set<UnitType> types, final Collection<GamePlayer> unitOwners) {
    return Matches.unitIsOwnedByAnyOf(unitOwners)
        .and(Matches.unitIsOfTypes(types))
        .and(Matches.unitIsNotDisabled())
        .and(Matches.unitHasNotMoved())
        .and(Matches.unitIsAirborne().negate());
  }
}
