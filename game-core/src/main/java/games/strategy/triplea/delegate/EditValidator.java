package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

/**
 * Provides some static methods for validating game edits.
 */
class EditValidator {
  private static String validateTerritoryBasic(final GameData data, final Territory territory) {
    // territory cannot be in an UndoableMove route
    final List<UndoableMove> moves = DelegateFinder.moveDelegate(data).getMovesMade();
    for (final UndoableMove move : moves) {
      if ((move.getRoute().getStart() == territory) || (move.getRoute().getEnd() == territory)) {
        return "Territory is start or end of a pending move";
      }
    }
    return null;
  }

  static String validateChangeTerritoryOwner(final GameData data, final Territory territory) {
    if (Matches.territoryIsWater().test(territory) && territory.getOwner().equals(PlayerID.NULL_PLAYERID)
        && (TerritoryAttachment.get(territory) == null)) {
      return "Territory is water and has no attachment";
    }
    return validateTerritoryBasic(data, territory);
  }

  static String validateAddUnits(final GameData data, final Territory territory, final Collection<Unit> units) {
    if (units.isEmpty()) {
      return "No units selected";
    }
    final PlayerID player = units.iterator().next().getOwner();
    // check land/water sanity
    if (territory.isWater()) {
      if (units.isEmpty() || !units.stream().allMatch(Matches.unitIsSea())) {
        if (units.stream().anyMatch(Matches.unitIsLand())) {
          if (units.isEmpty() || !units.stream().allMatch(Matches.alliedUnit(player, data))) {
            return "Can't add mixed nationality units to water";
          }
          final Predicate<Unit> friendlySeaTransports = Matches.unitIsTransport()
              .and(Matches.unitIsSea())
              .and(Matches.alliedUnit(player, data));
          final Collection<Unit> seaTransports = CollectionUtils.getMatches(units, friendlySeaTransports);
          final Collection<Unit> landUnitsToAdd = CollectionUtils.getMatches(units, Matches.unitIsLand());
          if (landUnitsToAdd.isEmpty() || !landUnitsToAdd.stream().allMatch(Matches.unitCanBeTransported())) {
            return "Can't add land units that can't be transported, to water";
          }
          seaTransports.addAll(territory.getUnits().getMatches(friendlySeaTransports));
          if (seaTransports.isEmpty()) {
            return "Can't add land units to water without enough transports";
          }
          final Map<Unit, Unit> mapLoading = TransportUtils.mapTransportsToLoad(landUnitsToAdd, seaTransports);
          if (!mapLoading.keySet().containsAll(landUnitsToAdd)) {
            return "Can't add land units to water without enough transports";
          }
        }
        if (units.stream().anyMatch(Matches.unitIsAir())) {
          if (units.stream().anyMatch(Matches.unitIsAir()
              .and(Matches.unitCanLandOnCarrier().negate()))) {
            return "Cannot add air to water unless it can land on carriers";
          }
          // Set up matches
          final Predicate<Unit> friendlyCarriers = Matches.unitIsCarrier().and(Matches.alliedUnit(player, data));
          final Predicate<Unit> friendlyAirUnits = Matches.unitIsAir().and(Matches.alliedUnit(player, data));
          // Determine transport capacity
          final int carrierCapacityTotal =
              AirMovementValidator.carrierCapacity(territory.getUnits().getMatches(friendlyCarriers), territory)
                  + AirMovementValidator.carrierCapacity(units, territory);
          final int carrierCost = AirMovementValidator.carrierCost(territory.getUnits().getMatches(friendlyAirUnits))
              + AirMovementValidator.carrierCost(units);
          if (carrierCapacityTotal < carrierCost) {
            return "Can't add more air units to water without sufficient space";
          }
        }
      }
    } else if (units.stream().anyMatch(Matches.unitIsSea())) {
      return "Can't add sea units to land";
    }

    return validateTerritoryBasic(data, territory);
  }

  static String validateRemoveUnits(final GameData data, final Territory territory, final Collection<Unit> units) {
    if (units.isEmpty()) {
      return "No units selected";
    }
    /*
     * all units should be same owner
     * if (!Match.allMatch(units, Matches.unitIsOwnedBy(player)))
     * return "Not all units have the same owner";
     */
    final String result = validateTerritoryBasic(data, territory);
    if (result != null) {
      return result;
    }
    // if transport selected, all transported units must be deleted too
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitCanTransport())) {
      if (!units.containsAll(TransportTracker.transporting(unit))) {
        return "Can't remove transport without removing transported units";
      }
    }
    // if transported units selected, transport must be deleted too
    for (final Unit unit : CollectionUtils.getMatches(units, Matches.unitCanBeTransported())) {
      final Unit transport = TransportTracker.transportedBy(unit);
      if ((transport != null) && !units.contains(transport)) {
        return "Can't remove transported units without removing transport";
      }
    }
    // TODO: if carrier selected, all carried planes must be deleted too
    // TODO: if carried planes selected, carrier must be deleted too
    return null;
  }

  static String validateAddTech(final GameData data, final Collection<TechAdvance> techs, final PlayerID player) {
    final String result = null;
    if (techs == null) {
      return "No tech selected";
    }
    if (player == null) {
      return "No player selected";
    }
    if (!Properties.getTechDevelopment(data)) {
      return "Technology not enabled";
    }
    if (player.getAttachment(Constants.TECH_ATTACHMENT_NAME) == null) {
      return "Player has no Tech Attachment";
    }
    for (final TechAdvance tech : techs) {
      if (tech == null) {
        return "No tech selected";
      }
      if (!TechnologyDelegate.getAvailableTechs(player, data).contains(tech)) {
        return "Technology not available for this player";
      }
    }
    return result;
  }

  static String validateRemoveTech(final GameData data, final Collection<TechAdvance> techs, final PlayerID player) {
    final String result = null;
    if (techs == null) {
      return "No tech selected";
    }
    if (player == null) {
      return "No player selected";
    }
    if (!Properties.getTechDevelopment(data)) {
      return "Technology not enabled";
    }
    for (final TechAdvance tech : techs) {
      if (tech == null) {
        return "No tech selected";
      }
      if (!TechTracker.getCurrentTechAdvances(player, data).contains(tech)) {
        return "Player does not have this tech";
      }
      if (tech.getProperty().equals(TechAdvance.TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY)) {
        return "Cannot remove " + TechAdvance.TECH_NAME_INDUSTRIAL_TECHNOLOGY;
      }
      if (tech.getProperty().equals(TechAdvance.TECH_PROPERTY_IMPROVED_SHIPYARDS)) {
        return "Cannot remove " + TechAdvance.TECH_NAME_IMPROVED_SHIPYARDS;
      }
    }
    return result;
  }

  static String validateChangeHitDamage(final GameData data, final IntegerMap<Unit> unitDamageMap,
      final Territory territory) {
    if ((unitDamageMap == null) || unitDamageMap.isEmpty()) {
      return "Damage map is empty";
    }
    final String result = validateTerritoryBasic(data, territory);
    if (result != null) {
      return result;
    }
    final Collection<Unit> units = new ArrayList<>(unitDamageMap.keySet());
    if (!territory.getUnits().getUnits().containsAll(units)) {
      return "Selected Territory does not contain all of the selected units";
    }
    final PlayerID player = units.iterator().next().getOwner();
    // all units should be same owner
    if (units.isEmpty() || !units.stream().allMatch(Matches.unitIsOwnedBy(player))) {
      return "Not all units have the same owner";
    }
    if (units.isEmpty() || !units.stream().allMatch(Matches.unitHasMoreThanOneHitPointTotal())) {
      return "Not all units have more than one total hitpoints";
    }
    for (final Unit u : units) {
      final int dmg = unitDamageMap.getInt(u);
      if ((dmg < 0) || (dmg >= UnitAttachment.get(u.getType()).getHitPoints())) {
        return "Damage cannot be less than zero or equal to or greater than unit hitpoints (if you want to kill the "
            + "unit, use remove unit)";
      }
    }
    return null;
  }

  static String validateChangeBombingDamage(final GameData data, final IntegerMap<Unit> unitDamageMap,
      final Territory territory) {
    if ((unitDamageMap == null) || unitDamageMap.isEmpty()) {
      return "Damage map is empty";
    }
    final String result = validateTerritoryBasic(data, territory);
    if (result != null) {
      return result;
    }
    if (!Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
      return "Game does not allow bombing damage";
    }
    final Collection<Unit> units = new ArrayList<>(unitDamageMap.keySet());
    if (!territory.getUnits().getUnits().containsAll(units)) {
      return "Selected Territory does not contain all of the selected units";
    }
    final PlayerID player = units.iterator().next().getOwner();
    // all units should be same owner
    if (units.isEmpty() || !units.stream().allMatch(Matches.unitIsOwnedBy(player))) {
      return "Not all units have the same owner";
    }
    if (units.isEmpty() || !units.stream().allMatch(Matches.unitCanBeDamaged())) {
      return "Not all units can take bombing damage";
    }
    for (final Unit u : units) {
      final int dmg = unitDamageMap.getInt(u);
      if ((dmg < 0) || (dmg > ((TripleAUnit) u).getHowMuchDamageCanThisUnitTakeTotal(u, territory))) {
        return "Damage cannot be less than zero or greater than the max damage of the unit";
      }
    }
    return null;
  }

  static String validateChangePoliticalRelationships(
      final Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges) {
    final String result = null;
    if ((relationshipChanges == null) || relationshipChanges.isEmpty()) {
      return "Relationship Changes are empty";
    }
    for (final Triple<PlayerID, PlayerID, RelationshipType> relationshipChange : relationshipChanges) {
      if ((relationshipChange.getFirst() == null) || (relationshipChange.getSecond() == null)) {
        return "Players are null";
      }
      if (relationshipChange.getThird() == null) {
        return "New Relationship is null";
      }
    }
    return result;
  }
}
