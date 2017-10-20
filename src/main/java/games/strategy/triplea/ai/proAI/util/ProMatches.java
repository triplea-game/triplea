package games.strategy.triplea.ai.proAI.util;

import java.util.Collection;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.util.Match;

/**
 * Pro AI matches.
 */
public class ProMatches {

  public static Match<Territory> territoryCanLandAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> enemyTerritories, final List<Territory> alliedTerritories) {
    return Match.anyOf(Matches.territoryIsInList(alliedTerritories),
        Match.allOf(Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data),
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false,
                false, true, true),
            Matches.territoryIsInList(enemyTerritories).invert()));
  }

  public static Match<Territory> territoryCanMoveAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
        Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, false,
            true, false));
  }

  public static Match<Territory> territoryCanPotentiallyMoveAirUnits(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
        Matches.territoryIsPassableAndNotRestricted(player, data));
  }

  public static Match<Territory> territoryCanMoveAirUnitsAndNoAa(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove),
        Matches.territoryHasEnemyAaForAnything(player, data).invert());
  }

  public static Match<Territory> territoryCanMoveSpecificLandUnit(final PlayerID player, final GameData data,
      final boolean isCombatMove, final Unit u) {
    return Match.of(t -> {
      final Match<Territory> territoryMatch = Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
          Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
              false, false));
      final Match<Unit> unitMatch =
          Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).invert();
      return territoryMatch.match(t) && unitMatch.match(u);
    });
  }

  public static Match<Territory> territoryCanPotentiallyMoveSpecificLandUnit(final PlayerID player, final GameData data,
      final Unit u) {
    return Match.of(t -> {
      final Match<Territory> territoryMatch = Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
          Matches.territoryIsPassableAndNotRestricted(player, data));
      final Match<Unit> unitMatch =
          Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).invert();
      return territoryMatch.match(t) && unitMatch.match(u);
    });
  }

  public static Match<Territory> territoryCanMoveLandUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
        Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
            false, false));
  }

  public static Match<Territory> territoryCanPotentiallyMoveLandUnits(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.territoryIsLand(),
        Matches.territoryDoesNotCostMoneyToEnter(data), Matches.territoryIsPassableAndNotRestricted(player, data));
  }

  public static Match<Territory> territoryCanMoveLandUnitsAndIsAllied(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.isTerritoryAllied(player, data),
        territoryCanMoveLandUnits(player, data, false));
  }

  public static Match<Territory> territoryCanMoveLandUnitsThrough(final PlayerID player, final GameData data,
      final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> enemyTerritories) {
    return Match.of(t -> {
      Match<Territory> match =
          Match.allOf(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
              Matches.isTerritoryAllied(player, data), Matches.territoryHasNoEnemyUnits(player, data),
              Matches.territoryIsInList(enemyTerritories).invert());
      if (isCombatMove && Matches.unitCanBlitz().match(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
        final Match<Territory> alliedWithNoEnemiesMatch = Match.allOf(
            Matches.isTerritoryAllied(player, data), Matches.territoryHasNoEnemyUnits(player, data));
        final Match<Territory> alliedOrBlitzableMatch =
            Match.anyOf(alliedWithNoEnemiesMatch, territoryIsBlitzable(player, data, u));
        match = Match.allOf(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
            alliedOrBlitzableMatch, Matches.territoryIsInList(enemyTerritories).invert());
      }
      return match.match(t);
    });
  }

  public static Match<Territory> territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(final PlayerID player,
      final GameData data, final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> blockedTerritories, final List<Territory> clearedTerritories) {
    Match<Territory> alliedMatch = Match.anyOf(Matches.isTerritoryAllied(player, data),
        Matches.territoryIsInList(clearedTerritories));
    if (isCombatMove && Matches.unitCanBlitz().match(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
      alliedMatch = Match.anyOf(Matches.isTerritoryAllied(player, data),
          Matches.territoryIsInList(clearedTerritories), territoryIsBlitzable(player, data, u));
    }
    return Match.allOf(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
        alliedMatch, Matches.territoryIsInList(blockedTerritories).invert());
  }

  private static Match<Territory> territoryIsBlitzable(final PlayerID player, final GameData data, final Unit u) {
    return Match.of(t -> Matches.territoryIsBlitzable(player, data).match(t)
        && TerritoryEffectHelper.unitKeepsBlitz(u, t));
  }

  public static Match<Territory> territoryCanMoveSeaUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.of(t -> {
      final boolean navalMayNotNonComIntoControlled =
          Properties.getWW2V2(data) || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
      if (!isCombatMove && navalMayNotNonComIntoControlled
          && Matches.isTerritoryEnemyAndNotUnownedWater(player, data).match(t)) {
        return false;
      }
      final Match<Territory> match = Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
          Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, true,
              false, false));
      return match.match(t);
    });
  }

  public static Match<Territory> territoryCanMoveSeaUnitsThrough(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(territoryCanMoveSeaUnits(player, data, isCombatMove),
        territoryHasOnlyIgnoredUnits(player, data));
  }

  public static Match<Territory> territoryCanMoveSeaUnitsAndNotInList(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> notTerritories) {
    return Match.allOf(territoryCanMoveSeaUnits(player, data, isCombatMove),
        Matches.territoryIsNotInList(notTerritories));
  }

  public static Match<Territory> territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(final PlayerID player,
      final GameData data, final boolean isCombatMove, final List<Territory> clearedTerritories,
      final List<Territory> notTerritories) {
    final Match<Territory> onlyIgnoredOrClearedMatch = Match.anyOf(
        territoryHasOnlyIgnoredUnits(player, data), Matches.territoryIsInList(clearedTerritories));
    return Match.allOf(territoryCanMoveSeaUnits(player, data, isCombatMove),
        onlyIgnoredOrClearedMatch, Matches.territoryIsNotInList(notTerritories));
  }

  private static Match<Territory> territoryHasOnlyIgnoredUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      final Match<Unit> subOnly = Match.anyOf(Matches.unitIsInfrastructure(), Matches.unitIsSub(),
          Matches.enemyUnit(player, data).invert());
      return (Properties.getIgnoreSubInMovement(data) && t.getUnits().allMatch(subOnly))
          || Matches.territoryHasNoEnemyUnits(player, data).match(t);
    });
  }

  public static Match<Territory> territoryHasEnemyUnitsOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Match.anyOf(Matches.territoryHasEnemyUnits(player, data),
        Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Match<Territory> territoryHasPotentialEnemyUnits(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return Match.anyOf(Matches.territoryHasEnemyUnits(player, data),
        Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(players)));
  }

  public static Match<Territory> territoryHasNoEnemyUnitsOrCleared(final PlayerID player, final GameData data,
      final List<Territory> clearedTerritories) {
    return Match.anyOf(Matches.territoryHasNoEnemyUnits(player, data), Matches.territoryIsInList(clearedTerritories));
  }

  public static Match<Territory> territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Match.anyOf(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
        Matches.territoryHasEnemyUnits(player, data), Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsLand() {
    final Match<Unit> infraFactoryMatch = Match.allOf(Matches.unitCanProduceUnits(), Matches.unitIsInfrastructure());
    return Match.allOf(Matches.territoryIsLand(), Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsEnemyLand(final PlayerID player, final GameData data) {
    return Match.allOf(territoryHasInfraFactoryAndIsLand(), Matches.isTerritoryEnemy(player, data));
  }

  static Match<Territory> territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(final PlayerID player,
      final List<PlayerID> players, final List<Territory> territoriesThatCantBeHeld) {
    final Match<Territory> ownedAndCantBeHeld = Match.allOf(Matches.isTerritoryOwnedBy(player),
        Matches.territoryIsInList(territoriesThatCantBeHeld));
    final Match<Territory> enemyOrOwnedCantBeHeld =
        Match.anyOf(Matches.isTerritoryOwnedBy(players), ownedAndCantBeHeld);
    return Match.allOf(territoryHasInfraFactoryAndIsLand(), enemyOrOwnedCantBeHeld);
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryIsNotConqueredOwnedLand(player, data),
        territoryHasInfraFactoryAndIsOwnedLand(player));
  }

  public static Match<Territory> territoryHasNonMobileInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryHasNonMobileInfraFactory(),
        territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
  }

  private static Match<Territory> territoryHasNonMobileInfraFactory() {
    final Match<Unit> nonMobileInfraFactoryMatch = Match.allOf(Matches.unitCanProduceUnits(),
        Matches.unitIsInfrastructure(), Matches.unitHasMovementLeft().invert());
    return Matches.territoryHasUnitsThatMatch(nonMobileInfraFactoryMatch);
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsOwnedLand(final PlayerID player) {
    final Match<Unit> infraFactoryMatch = Match.allOf(Matches.unitIsOwnedBy(player),
        Matches.unitCanProduceUnits(), Matches.unitIsInfrastructure());
    return Match.allOf(Matches.isTerritoryOwnedBy(player),
        Matches.territoryIsLand(), Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsAlliedLand(final PlayerID player, final GameData data) {
    final Match<Unit> infraFactoryMatch = Match.allOf(Matches.unitCanProduceUnits(), Matches.unitIsInfrastructure());
    return Match.allOf(Matches.isTerritoryAllied(player, data),
        Matches.territoryIsLand(), Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryHasInfraFactoryAndIsOwnedLand(player),
        Matches.territoryHasNeighborMatching(data, Matches.territoryIsWater()));
  }

  public static Match<Territory> territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryIsNotConqueredOwnedLand(player, data),
        territoryHasInfraFactoryAndIsOwnedLand(player).invert());
  }

  public static Match<Territory> territoryHasNeighborOwnedByAndHasLandUnit(final GameData data,
      final List<PlayerID> players) {
    final Match<Territory> territoryMatch = Match.allOf(Matches.isTerritoryOwnedBy(players),
        Matches.territoryHasUnitsThatMatch(Matches.unitIsLand()));
    return Matches.territoryHasNeighborMatching(data, territoryMatch);
  }

  static Match<Territory> territoryIsAlliedLandAndHasNoEnemyNeighbors(final PlayerID player, final GameData data) {
    final Match<Territory> alliedLand = Match.allOf(territoryCanMoveLandUnits(player, data, false),
        Matches.isTerritoryAllied(player, data));
    final Match<Territory> hasNoEnemyNeighbors = Matches
        .territoryHasNeighborMatching(data, ProMatches.territoryIsEnemyNotNeutralLand(player, data)).invert();
    return Match.allOf(alliedLand, hasNoEnemyNeighbors);
  }

  public static Match<Territory> territoryIsEnemyLand(final PlayerID player, final GameData data) {
    return Match.allOf(territoryCanMoveLandUnits(player, data, false),
        Matches.isTerritoryEnemy(player, data));
  }

  public static Match<Territory> territoryIsEnemyNotNeutralLand(final PlayerID player, final GameData data) {
    return Match.allOf(territoryIsEnemyLand(player, data), Matches.territoryIsNeutralButNotWater().invert());
  }

  public static Match<Territory> territoryIsOrAdjacentToEnemyNotNeutralLand(final PlayerID player,
      final GameData data) {
    final Match<Territory> isMatch =
        Match.allOf(territoryIsEnemyLand(player, data), Matches.territoryIsNeutralButNotWater().invert());
    final Match<Territory> adjacentMatch = Match.allOf(territoryCanMoveLandUnits(player, data, false),
        Matches.territoryHasNeighborMatching(data, isMatch));
    return Match.anyOf(isMatch, adjacentMatch);
  }

  public static Match<Territory> territoryIsEnemyNotNeutralOrAllied(final PlayerID player, final GameData data) {
    final Match<Territory> alliedLand = Match.allOf(Matches.territoryIsLand(), Matches.isTerritoryAllied(player, data));
    return Match.anyOf(territoryIsEnemyNotNeutralLand(player, data), alliedLand);
  }

  public static Match<Territory> territoryIsEnemyOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Match.anyOf(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
        Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Match<Territory> territoryIsPotentialEnemy(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return Match.anyOf(Matches.isTerritoryEnemyAndNotUnownedWater(player, data), Matches.isTerritoryOwnedBy(players));
  }

  public static Match<Territory> territoryIsPotentialEnemyOrHasPotentialEnemyUnits(final PlayerID player,
      final GameData data, final List<PlayerID> players) {
    return Match.anyOf(territoryIsPotentialEnemy(player, data, players),
        territoryHasPotentialEnemyUnits(player, data, players));
  }

  public static Match<Territory> territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(final PlayerID player,
      final GameData data, final List<Territory> territoriesThatCantBeHeld) {
    final Match<Unit> myUnitIsLand = Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsLand());
    final Match<Territory> territoryIsLandAndAdjacentToMyLandUnits =
        Match.allOf(Matches.territoryIsLand(),
            Matches.territoryHasNeighborMatching(data, Matches.territoryHasUnitsThatMatch(myUnitIsLand)));
    return Match.allOf(territoryIsLandAndAdjacentToMyLandUnits,
        territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
  }

  public static Match<Territory> territoryIsNotConqueredAlliedLand(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
        return false;
      }
      final Match<Territory> match = Match.allOf(Matches.isTerritoryAllied(player, data), Matches.territoryIsLand());
      return match.match(t);
    });
  }

  public static Match<Territory> territoryIsNotConqueredOwnedLand(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
        return false;
      }
      final Match<Territory> match = Match.allOf(Matches.isTerritoryOwnedBy(player), Matches.territoryIsLand());
      return match.match(t);
    });
  }

  public static Match<Territory> territoryIsWaterAndAdjacentToOwnedFactory(final PlayerID player, final GameData data) {
    final Match<Territory> hasOwnedFactoryNeighbor =
        Matches.territoryHasNeighborMatching(data, ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player));
    return Match.allOf(hasOwnedFactoryNeighbor, ProMatches.territoryCanMoveSeaUnits(player, data, true));
  }

  private static Match<Unit> unitCanBeMovedAndIsOwned(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft());
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedAir(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().match(u)) {
        return false;
      }
      final Match<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsAir());
      return match.match(u);
    });
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedLand(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().match(u)) {
        return false;
      }
      final Match<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsLand(),
          Matches.unitIsBeingTransported().invert());
      return match.match(u);
    });
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedSea(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().match(u)) {
        return false;
      }
      final Match<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsSea());
      return match.match(u);
    });
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedTransport(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().match(u)) {
        return false;
      }
      final Match<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsTransport());
      return match.match(u);
    });
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedBombard(final PlayerID player) {
    return Match.of(u -> {
      if (Matches.unitCanNotMoveDuringCombatMove().match(u)) {
        return false;
      }
      final Match<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitCanBombard(player));
      return match.match(u);
    });
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedNonCombatInfra(final PlayerID player) {
    return Match.allOf(unitCanBeMovedAndIsOwned(player),
        Matches.unitCanNotMoveDuringCombatMove(), Matches.unitIsInfrastructure());
  }

  public static Match<Unit> unitCantBeMovedAndIsAlliedDefender(final PlayerID player, final GameData data,
      final Territory t) {
    final Match<Unit> myUnitHasNoMovementMatch =
        Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft().invert());
    final Match<Unit> alliedUnitMatch =
        Match.allOf(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data),
            Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(t.getUnits().getUnits(), null, player,
                data, false).invert());
    return Match.anyOf(myUnitHasNoMovementMatch, alliedUnitMatch);
  }

  public static Match<Unit> unitCantBeMovedAndIsAlliedDefenderAndNotInfra(final PlayerID player, final GameData data,
      final Territory t) {
    return Match.allOf(unitCantBeMovedAndIsAlliedDefender(player, data, t),
        Matches.unitIsNotInfrastructure());
  }

  public static Match<Unit> unitIsAlliedLandAndNotInfra(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.unitIsLand(), Matches.isUnitAllied(player, data),
        Matches.unitIsNotInfrastructure());
  }

  public static Match<Unit> unitIsAlliedNotOwned(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data));
  }

  public static Match<Unit> unitIsAlliedNotOwnedAir(final PlayerID player, final GameData data) {
    return Match.allOf(unitIsAlliedNotOwned(player, data), Matches.unitIsAir());
  }

  static Match<Unit> unitIsAlliedAir(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.isUnitAllied(player, data), Matches.unitIsAir());
  }

  public static Match<Unit> unitIsEnemyAir(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsAir());
  }

  public static Match<Unit> unitIsEnemyAndNotAa(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsAaForAnything().invert());
  }

  public static Match<Unit> unitIsEnemyAndNotInfa(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsNotInfrastructure());
  }

  public static Match<Unit> unitIsEnemyNotLand(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsNotLand());
  }

  static Match<Unit> unitIsEnemyNotNeutral(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), unitIsNeutral().invert());
  }

  private static Match<Unit> unitIsNeutral() {
    return Match.of(u -> u.getOwner().isNull());
  }

  static Match<Unit> unitIsOwnedAir(final PlayerID player) {
    return Match.allOf(Matches.unitOwnedBy(player), Matches.unitIsAir());
  }

  public static Match<Unit> unitIsOwnedAndMatchesTypeAndIsTransporting(final PlayerID player, final UnitType unitType) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(unitType),
        Matches.unitIsTransporting());
  }

  public static Match<Unit> unitIsOwnedAndMatchesTypeAndNotTransporting(final PlayerID player,
      final UnitType unitType) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(unitType),
        Matches.unitIsTransporting().invert());
  }

  public static Match<Unit> unitIsOwnedCarrier(final PlayerID player) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
        && Matches.unitIsOwnedBy(player).match(unit));
  }

  public static Match<Unit> unitIsOwnedNotLand(final PlayerID player) {
    return Match.allOf(Matches.unitIsNotLand(), Matches.unitIsOwnedBy(player));
  }

  public static Match<Unit> unitIsOwnedTransport(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsTransport());
  }

  public static Match<Unit> unitIsOwnedTransportableUnit(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitCanBeTransported(), Matches.unitCanMove());
  }

  public static Match<Unit> unitIsOwnedCombatTransportableUnit(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitCanBeTransported(),
        Matches.unitCanNotMoveDuringCombatMove().invert(), Matches.unitCanMove());
  }

  public static Match<Unit> unitIsOwnedTransportableUnitAndCanBeLoaded(final PlayerID player, final Unit transport,
      final boolean isCombatMove) {
    return Match.of(u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (isCombatMove
          && (Matches.unitCanNotMoveDuringCombatMove().match(u) || !ua.canInvadeFrom(transport))) {
        return false;
      }
      final Match<Unit> match = Match.allOf(unitIsOwnedTransportableUnit(player), Matches.unitHasNotMoved(),
          Matches.unitHasMovementLeft(), Matches.unitIsBeingTransported().invert());
      return match.match(u);
    });
  }

  /**
   * Check what units a territory can produce.
   *
   * @param t
   *        territory we are testing for required units
   * @return whether the territory contains one of the required combos of units
   */
  public static Match<Unit> unitWhichRequiresUnitsHasRequiredUnits(final Territory t) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!Matches.unitRequiresUnitsOnCreation().match(unitWhichRequiresUnits)) {
        return true;
      }
      final Collection<Unit> unitsAtStartOfTurnInProducer = t.getUnits().getUnits();
      if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer)
          .match(unitWhichRequiresUnits)) {
        return true;
      }
      if (t.isWater() && Matches.unitIsSea().match(unitWhichRequiresUnits)) {
        for (final Territory neighbor : t.getData().getMap().getNeighbors(t, Matches.territoryIsLand())) {
          final Collection<Unit> unitsAtStartOfTurnInCurrent = neighbor.getUnits().getUnits();
          if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent)
              .match(unitWhichRequiresUnits)) {
            return true;
          }
        }
      }
      return false;
    });
  }
}
