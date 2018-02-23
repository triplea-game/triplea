package games.strategy.triplea.ai.proAI.util;

import java.util.List;
import java.util.function.Predicate;

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

/**
 * Pro AI matches.
 */
public class ProMatches {

  public static Predicate<Territory> territoryCanLandAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> enemyTerritories, final List<Territory> alliedTerritories) {
    return Matches.territoryIsInList(alliedTerritories).or(
        Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data)
            .and(Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false,
                false, true, true))
            .and(Matches.territoryIsInList(enemyTerritories).negate()));
  }

  public static Predicate<Territory> territoryCanMoveAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Matches.territoryDoesNotCostMoneyToEnter(data)
        .and(Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, false,
            true, false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveAirUnits(final PlayerID player, final GameData data) {
    return Matches.territoryDoesNotCostMoneyToEnter(data)
        .and(Matches.territoryIsPassableAndNotRestricted(player, data));
  }

  public static Predicate<Territory> territoryCanMoveAirUnitsAndNoAa(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove)
        .and(Matches.territoryHasEnemyAaForAnything(player, data).negate());
  }

  public static Predicate<Territory> territoryCanMoveSpecificLandUnit(final PlayerID player, final GameData data,
      final boolean isCombatMove, final Unit u) {
    return t -> {
      final Predicate<Territory> territoryMatch = Matches.territoryDoesNotCostMoneyToEnter(data)
          .and(Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
              false, false));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).negate();
      return territoryMatch.test(t) && unitMatch.test(u);
    };
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveSpecificLandUnit(final PlayerID player,
      final GameData data,
      final Unit u) {
    return t -> {
      final Predicate<Territory> territoryMatch = Matches.territoryDoesNotCostMoneyToEnter(data)
          .and(Matches.territoryIsPassableAndNotRestricted(player, data));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).negate();
      return territoryMatch.test(t) && unitMatch.test(u);
    };
  }

  public static Predicate<Territory> territoryCanMoveLandUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Matches.territoryDoesNotCostMoneyToEnter(data)
        .and(Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
            false, false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveLandUnits(final PlayerID player, final GameData data) {
    return Matches.territoryIsLand()
        .and(Matches.territoryDoesNotCostMoneyToEnter(data))
        .and(Matches.territoryIsPassableAndNotRestricted(player, data));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsAndIsAllied(final PlayerID player, final GameData data) {
    return Matches.isTerritoryAllied(player, data).and(territoryCanMoveLandUnits(player, data, false));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThrough(final PlayerID player, final GameData data,
      final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> enemyTerritories) {
    return t -> {
      if (isCombatMove && Matches.unitCanBlitz().test(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
        final Predicate<Territory> alliedWithNoEnemiesMatch = Matches.isTerritoryAllied(player, data)
            .and(Matches.territoryHasNoEnemyUnits(player, data));
        final Predicate<Territory> alliedOrBlitzableMatch =
            alliedWithNoEnemiesMatch.or(territoryIsBlitzable(player, data, u));
        return ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u)
            .and(alliedOrBlitzableMatch)
            .and(Matches.territoryIsInList(enemyTerritories).negate())
            .test(t);
      }
      return ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u)
          .and(Matches.isTerritoryAllied(player, data))
          .and(Matches.territoryHasNoEnemyUnits(player, data))
          .and(Matches.territoryIsInList(enemyTerritories).negate())
          .test(t);
    };
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(final PlayerID player,
      final GameData data, final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> blockedTerritories, final List<Territory> clearedTerritories) {
    Predicate<Territory> alliedMatch = Matches.isTerritoryAllied(player, data)
        .or(Matches.territoryIsInList(clearedTerritories));
    if (isCombatMove && Matches.unitCanBlitz().test(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
      alliedMatch = Matches.isTerritoryAllied(player, data)
          .or(Matches.territoryIsInList(clearedTerritories))
          .or(territoryIsBlitzable(player, data, u));
    }
    return ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u)
        .and(alliedMatch)
        .and(Matches.territoryIsInList(blockedTerritories).negate());
  }

  private static Predicate<Territory> territoryIsBlitzable(final PlayerID player, final GameData data, final Unit u) {
    return t -> Matches.territoryIsBlitzable(player, data).test(t) && TerritoryEffectHelper.unitKeepsBlitz(u, t);
  }

  public static Predicate<Territory> territoryCanMoveSeaUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return t -> {
      final boolean navalMayNotNonComIntoControlled =
          Properties.getWW2V2(data) || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
      if (!isCombatMove && navalMayNotNonComIntoControlled
          && Matches.isTerritoryEnemyAndNotUnownedWater(player, data).test(t)) {
        return false;
      }
      final Predicate<Territory> match = Matches.territoryDoesNotCostMoneyToEnter(data)
          .and(Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, true,
              false, false));
      return match.test(t);
    };
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThrough(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return territoryCanMoveSeaUnits(player, data, isCombatMove).and(territoryHasOnlyIgnoredUnits(player, data));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsAndNotInList(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> notTerritories) {
    return territoryCanMoveSeaUnits(player, data, isCombatMove).and(Matches.territoryIsNotInList(notTerritories));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(final PlayerID player,
      final GameData data, final boolean isCombatMove, final List<Territory> clearedTerritories,
      final List<Territory> notTerritories) {
    final Predicate<Territory> onlyIgnoredOrClearedMatch = territoryHasOnlyIgnoredUnits(player, data)
        .or(Matches.territoryIsInList(clearedTerritories));
    return territoryCanMoveSeaUnits(player, data, isCombatMove)
        .and(onlyIgnoredOrClearedMatch)
        .and(Matches.territoryIsNotInList(notTerritories));
  }

  private static Predicate<Territory> territoryHasOnlyIgnoredUnits(final PlayerID player, final GameData data) {
    return t -> {
      final Predicate<Unit> subOnly = Matches.unitIsInfrastructure()
          .or(Matches.unitIsSub())
          .or(Matches.enemyUnit(player, data).negate());
      return (Properties.getIgnoreSubInMovement(data) && t.getUnits().allMatch(subOnly))
          || Matches.territoryHasNoEnemyUnits(player, data).test(t);
    };
  }

  public static Predicate<Territory> territoryHasEnemyUnitsOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.territoryHasEnemyUnits(player, data).or(Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryHasPotentialEnemyUnits(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return Matches.territoryHasEnemyUnits(player, data)
        .or(Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(players)));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnitsOrCleared(final PlayerID player, final GameData data,
      final List<Territory> clearedTerritories) {
    return Matches.territoryHasNoEnemyUnits(player, data).or(Matches.territoryIsInList(clearedTerritories));
  }

  public static Predicate<Territory> territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(final PlayerID player,
      final GameData data, final List<Territory> territoriesThatCantBeHeld) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, data)
        .or(Matches.territoryHasEnemyUnits(player, data))
        .or(Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsLand() {
    final Predicate<Unit> infraFactory = Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure());
    return Matches.territoryIsLand().and(Matches.territoryHasUnitsThatMatch(infraFactory));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsEnemyLand(final PlayerID player,
      final GameData data) {
    return territoryHasInfraFactoryAndIsLand().and(Matches.isTerritoryEnemy(player, data));
  }

  static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(final PlayerID player,
      final List<PlayerID> players, final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Territory> ownedAndCantBeHeld = Matches.isTerritoryOwnedBy(player)
        .and(Matches.territoryIsInList(territoriesThatCantBeHeld));
    final Predicate<Territory> enemyOrOwnedCantBeHeld = Matches.isTerritoryOwnedBy(players).or(ownedAndCantBeHeld);
    return territoryHasInfraFactoryAndIsLand().and(enemyOrOwnedCantBeHeld);
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return territoryIsNotConqueredOwnedLand(player, data).and(territoryHasInfraFactoryAndIsOwnedLand(player));
  }

  public static Predicate<Territory> territoryHasNonMobileInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return territoryHasNonMobileInfraFactory().and(territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
  }

  private static Predicate<Territory> territoryHasNonMobileInfraFactory() {
    final Predicate<Unit> nonMobileInfraFactoryMatch = Matches.unitCanProduceUnits()
        .and(Matches.unitIsInfrastructure())
        .and(Matches.unitHasMovementLeft().negate());
    return Matches.territoryHasUnitsThatMatch(nonMobileInfraFactoryMatch);
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLand(final PlayerID player) {
    final Predicate<Unit> infraFactoryMatch = Matches.unitIsOwnedBy(player)
        .and(Matches.unitCanProduceUnits())
        .and(Matches.unitIsInfrastructure());
    return Matches.isTerritoryOwnedBy(player)
        .and(Matches.territoryIsLand())
        .and(Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsAlliedLand(final PlayerID player,
      final GameData data) {
    final Predicate<Unit> infraFactoryMatch = Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure());
    return Matches.isTerritoryAllied(player, data)
        .and(Matches.territoryIsLand())
        .and(Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(final PlayerID player,
      final GameData data) {
    return territoryHasInfraFactoryAndIsOwnedLand(player)
        .and(Matches.territoryHasNeighborMatching(data, Matches.territoryIsWater()));
  }

  public static Predicate<Territory> territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return territoryIsNotConqueredOwnedLand(player, data)
        .and(territoryHasInfraFactoryAndIsOwnedLand(player).negate());
  }

  public static Predicate<Territory> territoryHasNeighborOwnedByAndHasLandUnit(final GameData data,
      final List<PlayerID> players) {
    final Predicate<Territory> territoryMatch = Matches.isTerritoryOwnedBy(players)
        .and(Matches.territoryHasUnitsThatMatch(Matches.unitIsLand()));
    return Matches.territoryHasNeighborMatching(data, territoryMatch);
  }

  static Predicate<Territory> territoryIsAlliedLandAndHasNoEnemyNeighbors(final PlayerID player, final GameData data) {
    final Predicate<Territory> alliedLand = territoryCanMoveLandUnits(player, data, false)
        .and(Matches.isTerritoryAllied(player, data));
    final Predicate<Territory> hasNoEnemyNeighbors = Matches
        .territoryHasNeighborMatching(data, ProMatches.territoryIsEnemyNotNeutralLand(player, data)).negate();
    return alliedLand.and(hasNoEnemyNeighbors);
  }

  public static Predicate<Territory> territoryIsEnemyLand(final PlayerID player, final GameData data) {
    return territoryCanMoveLandUnits(player, data, false).and(Matches.isTerritoryEnemy(player, data));
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralLand(final PlayerID player, final GameData data) {
    return territoryIsEnemyLand(player, data).and(Matches.territoryIsNeutralButNotWater().negate());
  }

  public static Predicate<Territory> territoryIsOrAdjacentToEnemyNotNeutralLand(final PlayerID player,
      final GameData data) {
    final Predicate<Territory> isMatch = territoryIsEnemyLand(player, data)
        .and(Matches.territoryIsNeutralButNotWater().negate());
    final Predicate<Territory> adjacentMatch = territoryCanMoveLandUnits(player, data, false)
        .and(Matches.territoryHasNeighborMatching(data, isMatch));
    return isMatch.or(adjacentMatch);
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralOrAllied(final PlayerID player, final GameData data) {
    return territoryIsEnemyNotNeutralLand(player, data)
        .or(Matches.territoryIsLand().and(Matches.isTerritoryAllied(player, data)));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, data)
        .or(Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsPotentialEnemy(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, data).or(Matches.isTerritoryOwnedBy(players));
  }

  public static Predicate<Territory> territoryIsPotentialEnemyOrHasPotentialEnemyUnits(final PlayerID player,
      final GameData data, final List<PlayerID> players) {
    return territoryIsPotentialEnemy(player, data, players).or(territoryHasPotentialEnemyUnits(player, data, players));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(final PlayerID player,
      final GameData data, final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Unit> myUnitIsLand = Matches.unitIsOwnedBy(player).and(Matches.unitIsLand());
    final Predicate<Territory> territoryIsLandAndAdjacentToMyLandUnits = Matches.territoryIsLand()
        .and(Matches.territoryHasNeighborMatching(data, Matches.territoryHasUnitsThatMatch(myUnitIsLand)));
    return territoryIsLandAndAdjacentToMyLandUnits
        .and(territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsNotConqueredAlliedLand(final PlayerID player, final GameData data) {
    return t -> {
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
        return false;
      }
      return Matches.isTerritoryAllied(player, data).and(Matches.territoryIsLand()).test(t);
    };
  }

  public static Predicate<Territory> territoryIsNotConqueredOwnedLand(final PlayerID player, final GameData data) {
    return t -> {
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
        return false;
      }
      return Matches.isTerritoryOwnedBy(player).and(Matches.territoryIsLand()).test(t);
    };
  }

  public static Predicate<Territory> territoryIsWaterAndAdjacentToOwnedFactory(final PlayerID player,
      final GameData data) {
    final Predicate<Territory> hasOwnedFactoryNeighbor =
        Matches.territoryHasNeighborMatching(data, ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player));
    return hasOwnedFactoryNeighbor.and(ProMatches.territoryCanMoveSeaUnits(player, data, true));
  }

  private static Predicate<Unit> unitCanBeMovedAndIsOwned(final PlayerID player) {
    return Matches.unitIsOwnedBy(player).and(Matches.unitHasMovementLeft());
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedAir(final PlayerID player, final boolean isCombatMove) {
    return u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      return unitCanBeMovedAndIsOwned(player).and(Matches.unitIsAir()).test(u);
    };
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedLand(final PlayerID player, final boolean isCombatMove) {
    return u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      return unitCanBeMovedAndIsOwned(player)
          .and(Matches.unitIsLand())
          .and(Matches.unitIsBeingTransported().negate())
          .test(u);
    };
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedSea(final PlayerID player, final boolean isCombatMove) {
    return u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      return unitCanBeMovedAndIsOwned(player).and(Matches.unitIsSea()).test(u);
    };
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedTransport(final PlayerID player, final boolean isCombatMove) {
    return u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      return unitCanBeMovedAndIsOwned(player).and(Matches.unitIsTransport()).test(u);
    };
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedBombard(final PlayerID player) {
    return u -> {
      if (Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      return unitCanBeMovedAndIsOwned(player).and(Matches.unitCanBombard(player)).test(u);
    };
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedNonCombatInfra(final PlayerID player) {
    return unitCanBeMovedAndIsOwned(player)
        .and(Matches.unitCanNotMoveDuringCombatMove())
        .and(Matches.unitIsInfrastructure());
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefender(final PlayerID player, final GameData data,
      final Territory t) {
    final Predicate<Unit> myUnitHasNoMovementMatch = Matches.unitIsOwnedBy(player)
        .and(Matches.unitHasMovementLeft().negate());
    final Predicate<Unit> alliedUnitMatch = Matches.unitIsOwnedBy(player).negate()
        .and(Matches.isUnitAllied(player, data))
        .and(Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(
            t.getUnits().getUnits(), player, data, false).negate());
    return myUnitHasNoMovementMatch.or(alliedUnitMatch);
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefenderAndNotInfra(final PlayerID player,
      final GameData data,
      final Territory t) {
    return unitCantBeMovedAndIsAlliedDefender(player, data, t).and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedLandAndNotInfra(final PlayerID player, final GameData data) {
    return Matches.unitIsLand()
        .and(Matches.isUnitAllied(player, data))
        .and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedNotOwned(final PlayerID player, final GameData data) {
    return Matches.unitIsOwnedBy(player).negate().and(Matches.isUnitAllied(player, data));
  }

  public static Predicate<Unit> unitIsAlliedNotOwnedAir(final PlayerID player, final GameData data) {
    return unitIsAlliedNotOwned(player, data).and(Matches.unitIsAir());
  }

  static Predicate<Unit> unitIsAlliedAir(final PlayerID player, final GameData data) {
    return Matches.isUnitAllied(player, data).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAir(final PlayerID player, final GameData data) {
    return Matches.enemyUnit(player, data).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAndNotAa(final PlayerID player, final GameData data) {
    return Matches.enemyUnit(player, data).and(Matches.unitIsAaForAnything().negate());
  }

  public static Predicate<Unit> unitIsEnemyAndNotInfa(final PlayerID player, final GameData data) {
    return Matches.enemyUnit(player, data).and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsEnemyNotLand(final PlayerID player, final GameData data) {
    return Matches.enemyUnit(player, data).and(Matches.unitIsNotLand());
  }

  static Predicate<Unit> unitIsEnemyNotNeutral(final PlayerID player, final GameData data) {
    return Matches.enemyUnit(player, data).and(unitIsNeutral().negate());
  }

  private static Predicate<Unit> unitIsNeutral() {
    return u -> u.getOwner().isNull();
  }

  static Predicate<Unit> unitIsOwnedAir(final PlayerID player) {
    return Matches.unitOwnedBy(player).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsOwnedAndMatchesTypeAndIsTransporting(final PlayerID player,
      final UnitType unitType) {
    return Matches.unitIsOwnedBy(player)
        .and(Matches.unitIsOfType(unitType))
        .and(Matches.unitIsTransporting());
  }

  public static Predicate<Unit> unitIsOwnedAndMatchesTypeAndNotTransporting(final PlayerID player,
      final UnitType unitType) {
    return Matches.unitIsOwnedBy(player)
        .and(Matches.unitIsOfType(unitType))
        .and(Matches.unitIsTransporting().negate());
  }

  public static Predicate<Unit> unitIsOwnedCarrier(final PlayerID player) {
    return unit -> (UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1)
        && Matches.unitIsOwnedBy(player).test(unit);
  }

  public static Predicate<Unit> unitIsOwnedNotLand(final PlayerID player) {
    return Matches.unitIsNotLand().and(Matches.unitIsOwnedBy(player));
  }

  public static Predicate<Unit> unitIsOwnedTransport(final PlayerID player) {
    return Matches.unitIsOwnedBy(player).and(Matches.unitIsTransport());
  }

  public static Predicate<Unit> unitIsOwnedTransportableUnit(final PlayerID player) {
    return Matches.unitIsOwnedBy(player)
        .and(Matches.unitCanBeTransported())
        .and(Matches.unitCanMove());
  }

  public static Predicate<Unit> unitIsOwnedCombatTransportableUnit(final PlayerID player) {
    return unitIsOwnedTransportableUnit(player).and(Matches.unitCanNotMoveDuringCombatMove().negate());
  }

  public static Predicate<Unit> unitIsOwnedTransportableUnitAndCanBeLoaded(final PlayerID player, final Unit transport,
      final boolean isCombatMove) {
    return u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (isCombatMove && (Matches.unitCanNotMoveDuringCombatMove().test(u) || !ua.canInvadeFrom(transport))) {
        return false;
      }
      return unitIsOwnedTransportableUnit(player)
          .and(Matches.unitHasNotMoved())
          .and(Matches.unitHasMovementLeft())
          .and(Matches.unitIsBeingTransported().negate())
          .test(u);
    };
  }
}
