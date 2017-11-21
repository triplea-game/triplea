package games.strategy.triplea.ai.proAI.util;

import java.util.Collection;
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
import games.strategy.util.Match;

/**
 * Pro AI matches.
 */
public class ProMatches {

  public static Predicate<Territory> territoryCanLandAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> enemyTerritories, final List<Territory> alliedTerritories) {
    return Match.anyOf(Matches.territoryIsInList(alliedTerritories),
        Match.allOf(Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data),
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false,
                false, true, true),
            Matches.territoryIsInList(enemyTerritories).negate()));
  }

  public static Predicate<Territory> territoryCanMoveAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
        Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, false,
            true, false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveAirUnits(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
        Matches.territoryIsPassableAndNotRestricted(player, data));
  }

  public static Predicate<Territory> territoryCanMoveAirUnitsAndNoAa(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove),
        Matches.territoryHasEnemyAaForAnything(player, data).negate());
  }

  public static Predicate<Territory> territoryCanMoveSpecificLandUnit(final PlayerID player, final GameData data,
      final boolean isCombatMove, final Unit u) {
    return Match.of(t -> {
      final Predicate<Territory> territoryMatch = Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
          Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
              false, false));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).negate();
      return territoryMatch.test(t) && unitMatch.test(u);
    });
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveSpecificLandUnit(final PlayerID player,
      final GameData data,
      final Unit u) {
    return Match.of(t -> {
      final Predicate<Territory> territoryMatch = Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
          Matches.territoryIsPassableAndNotRestricted(player, data));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).negate();
      return territoryMatch.test(t) && unitMatch.test(u);
    });
  }

  public static Predicate<Territory> territoryCanMoveLandUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
        Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
            false, false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveLandUnits(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.territoryIsLand(),
        Matches.territoryDoesNotCostMoneyToEnter(data), Matches.territoryIsPassableAndNotRestricted(player, data));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsAndIsAllied(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.isTerritoryAllied(player, data),
        territoryCanMoveLandUnits(player, data, false));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThrough(final PlayerID player, final GameData data,
      final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> enemyTerritories) {
    return Match.of(t -> {
      Predicate<Territory> match =
          Match.allOf(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
              Matches.isTerritoryAllied(player, data), Matches.territoryHasNoEnemyUnits(player, data),
              Matches.territoryIsInList(enemyTerritories).negate());
      if (isCombatMove && Matches.unitCanBlitz().test(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
        final Predicate<Territory> alliedWithNoEnemiesMatch = Match.allOf(
            Matches.isTerritoryAllied(player, data), Matches.territoryHasNoEnemyUnits(player, data));
        final Predicate<Territory> alliedOrBlitzableMatch =
            Match.anyOf(alliedWithNoEnemiesMatch, territoryIsBlitzable(player, data, u));
        match = Match.allOf(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
            alliedOrBlitzableMatch, Matches.territoryIsInList(enemyTerritories).negate());
      }
      return match.test(t);
    });
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(final PlayerID player,
      final GameData data, final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> blockedTerritories, final List<Territory> clearedTerritories) {
    Predicate<Territory> alliedMatch = Match.anyOf(Matches.isTerritoryAllied(player, data),
        Matches.territoryIsInList(clearedTerritories));
    if (isCombatMove && Matches.unitCanBlitz().test(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
      alliedMatch = Match.anyOf(Matches.isTerritoryAllied(player, data),
          Matches.territoryIsInList(clearedTerritories), territoryIsBlitzable(player, data, u));
    }
    return Match.allOf(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
        alliedMatch, Matches.territoryIsInList(blockedTerritories).negate());
  }

  private static Predicate<Territory> territoryIsBlitzable(final PlayerID player, final GameData data, final Unit u) {
    return Match.of(t -> Matches.territoryIsBlitzable(player, data).test(t)
        && TerritoryEffectHelper.unitKeepsBlitz(u, t));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.of(t -> {
      final boolean navalMayNotNonComIntoControlled =
          Properties.getWW2V2(data) || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
      if (!isCombatMove && navalMayNotNonComIntoControlled
          && Matches.isTerritoryEnemyAndNotUnownedWater(player, data).test(t)) {
        return false;
      }
      final Predicate<Territory> match = Match.allOf(Matches.territoryDoesNotCostMoneyToEnter(data),
          Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, true,
              false, false));
      return match.test(t);
    });
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThrough(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return Match.allOf(territoryCanMoveSeaUnits(player, data, isCombatMove),
        territoryHasOnlyIgnoredUnits(player, data));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsAndNotInList(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> notTerritories) {
    return Match.allOf(territoryCanMoveSeaUnits(player, data, isCombatMove),
        Matches.territoryIsNotInList(notTerritories));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(final PlayerID player,
      final GameData data, final boolean isCombatMove, final List<Territory> clearedTerritories,
      final List<Territory> notTerritories) {
    final Predicate<Territory> onlyIgnoredOrClearedMatch = Match.anyOf(
        territoryHasOnlyIgnoredUnits(player, data), Matches.territoryIsInList(clearedTerritories));
    return Match.allOf(territoryCanMoveSeaUnits(player, data, isCombatMove),
        onlyIgnoredOrClearedMatch, Matches.territoryIsNotInList(notTerritories));
  }

  private static Predicate<Territory> territoryHasOnlyIgnoredUnits(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      final Predicate<Unit> subOnly = Match.anyOf(Matches.unitIsInfrastructure(), Matches.unitIsSub(),
          Matches.enemyUnit(player, data).negate());
      return (Properties.getIgnoreSubInMovement(data) && t.getUnits().allMatch(subOnly))
          || Matches.territoryHasNoEnemyUnits(player, data).test(t);
    });
  }

  public static Predicate<Territory> territoryHasEnemyUnitsOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Match.anyOf(Matches.territoryHasEnemyUnits(player, data),
        Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryHasPotentialEnemyUnits(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return Match.anyOf(Matches.territoryHasEnemyUnits(player, data),
        Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(players)));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnitsOrCleared(final PlayerID player, final GameData data,
      final List<Territory> clearedTerritories) {
    return Match.anyOf(Matches.territoryHasNoEnemyUnits(player, data), Matches.territoryIsInList(clearedTerritories));
  }

  public static Predicate<Territory> territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(final PlayerID player,
      final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Match.anyOf(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
        Matches.territoryHasEnemyUnits(player, data), Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsLand() {
    final Predicate<Unit> infraFactory = Match.allOf(Matches.unitCanProduceUnits(), Matches.unitIsInfrastructure());
    return Match.allOf(Matches.territoryIsLand(), Matches.territoryHasUnitsThatMatch(infraFactory));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsEnemyLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryHasInfraFactoryAndIsLand(), Matches.isTerritoryEnemy(player, data));
  }

  static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(final PlayerID player,
      final List<PlayerID> players, final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Territory> ownedAndCantBeHeld = Match.allOf(Matches.isTerritoryOwnedBy(player),
        Matches.territoryIsInList(territoriesThatCantBeHeld));
    final Predicate<Territory> enemyOrOwnedCantBeHeld =
        Match.anyOf(Matches.isTerritoryOwnedBy(players), ownedAndCantBeHeld);
    return Match.allOf(territoryHasInfraFactoryAndIsLand(), enemyOrOwnedCantBeHeld);
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryIsNotConqueredOwnedLand(player, data),
        territoryHasInfraFactoryAndIsOwnedLand(player));
  }

  public static Predicate<Territory> territoryHasNonMobileInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryHasNonMobileInfraFactory(),
        territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
  }

  private static Predicate<Territory> territoryHasNonMobileInfraFactory() {
    final Predicate<Unit> nonMobileInfraFactoryMatch = Match.allOf(Matches.unitCanProduceUnits(),
        Matches.unitIsInfrastructure(), Matches.unitHasMovementLeft().negate());
    return Matches.territoryHasUnitsThatMatch(nonMobileInfraFactoryMatch);
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLand(final PlayerID player) {
    final Predicate<Unit> infraFactoryMatch = Match.allOf(Matches.unitIsOwnedBy(player),
        Matches.unitCanProduceUnits(), Matches.unitIsInfrastructure());
    return Match.allOf(Matches.isTerritoryOwnedBy(player),
        Matches.territoryIsLand(), Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsAlliedLand(final PlayerID player,
      final GameData data) {
    final Predicate<Unit> infraFactoryMatch =
        Match.allOf(Matches.unitCanProduceUnits(), Matches.unitIsInfrastructure());
    return Match.allOf(Matches.isTerritoryAllied(player, data),
        Matches.territoryIsLand(), Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryHasInfraFactoryAndIsOwnedLand(player),
        Matches.territoryHasNeighborMatching(data, Matches.territoryIsWater()));
  }

  public static Predicate<Territory> territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return Match.allOf(territoryIsNotConqueredOwnedLand(player, data),
        territoryHasInfraFactoryAndIsOwnedLand(player).negate());
  }

  public static Predicate<Territory> territoryHasNeighborOwnedByAndHasLandUnit(final GameData data,
      final List<PlayerID> players) {
    final Predicate<Territory> territoryMatch = Match.allOf(Matches.isTerritoryOwnedBy(players),
        Matches.territoryHasUnitsThatMatch(Matches.unitIsLand()));
    return Matches.territoryHasNeighborMatching(data, territoryMatch);
  }

  static Predicate<Territory> territoryIsAlliedLandAndHasNoEnemyNeighbors(final PlayerID player, final GameData data) {
    final Predicate<Territory> alliedLand = Match.allOf(territoryCanMoveLandUnits(player, data, false),
        Matches.isTerritoryAllied(player, data));
    final Predicate<Territory> hasNoEnemyNeighbors = Matches
        .territoryHasNeighborMatching(data, ProMatches.territoryIsEnemyNotNeutralLand(player, data)).negate();
    return Match.allOf(alliedLand, hasNoEnemyNeighbors);
  }

  public static Predicate<Territory> territoryIsEnemyLand(final PlayerID player, final GameData data) {
    return Match.allOf(territoryCanMoveLandUnits(player, data, false),
        Matches.isTerritoryEnemy(player, data));
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralLand(final PlayerID player, final GameData data) {
    return Match.allOf(territoryIsEnemyLand(player, data), Matches.territoryIsNeutralButNotWater().negate());
  }

  public static Predicate<Territory> territoryIsOrAdjacentToEnemyNotNeutralLand(final PlayerID player,
      final GameData data) {
    final Predicate<Territory> isMatch =
        Match.allOf(territoryIsEnemyLand(player, data), Matches.territoryIsNeutralButNotWater().negate());
    final Predicate<Territory> adjacentMatch = Match.allOf(territoryCanMoveLandUnits(player, data, false),
        Matches.territoryHasNeighborMatching(data, isMatch));
    return Match.anyOf(isMatch, adjacentMatch);
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralOrAllied(final PlayerID player, final GameData data) {
    return Match.anyOf(territoryIsEnemyNotNeutralLand(player, data),
        Match.allOf(Matches.territoryIsLand(), Matches.isTerritoryAllied(player, data)));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Match.anyOf(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
        Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsPotentialEnemy(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return Match.anyOf(Matches.isTerritoryEnemyAndNotUnownedWater(player, data), Matches.isTerritoryOwnedBy(players));
  }

  public static Predicate<Territory> territoryIsPotentialEnemyOrHasPotentialEnemyUnits(final PlayerID player,
      final GameData data, final List<PlayerID> players) {
    return Match.anyOf(territoryIsPotentialEnemy(player, data, players),
        territoryHasPotentialEnemyUnits(player, data, players));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(final PlayerID player,
      final GameData data, final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Unit> myUnitIsLand = Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsLand());
    final Predicate<Territory> territoryIsLandAndAdjacentToMyLandUnits =
        Match.allOf(Matches.territoryIsLand(),
            Matches.territoryHasNeighborMatching(data, Matches.territoryHasUnitsThatMatch(myUnitIsLand)));
    return Match.allOf(territoryIsLandAndAdjacentToMyLandUnits,
        territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsNotConqueredAlliedLand(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
        return false;
      }
      final Predicate<Territory> match =
          Match.allOf(Matches.isTerritoryAllied(player, data), Matches.territoryIsLand());
      return match.test(t);
    });
  }

  public static Predicate<Territory> territoryIsNotConqueredOwnedLand(final PlayerID player, final GameData data) {
    return Match.of(t -> {
      if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
        return false;
      }
      final Predicate<Territory> match = Match.allOf(Matches.isTerritoryOwnedBy(player), Matches.territoryIsLand());
      return match.test(t);
    });
  }

  public static Predicate<Territory> territoryIsWaterAndAdjacentToOwnedFactory(final PlayerID player,
      final GameData data) {
    final Predicate<Territory> hasOwnedFactoryNeighbor =
        Matches.territoryHasNeighborMatching(data, ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player));
    return Match.allOf(hasOwnedFactoryNeighbor, ProMatches.territoryCanMoveSeaUnits(player, data, true));
  }

  private static Predicate<Unit> unitCanBeMovedAndIsOwned(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft());
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedAir(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      final Predicate<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsAir());
      return match.test(u);
    });
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedLand(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      final Predicate<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsLand(),
          Matches.unitIsBeingTransported().negate());
      return match.test(u);
    });
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedSea(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      final Predicate<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsSea());
      return match.test(u);
    });
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedTransport(final PlayerID player, final boolean isCombatMove) {
    return Match.of(u -> {
      if (isCombatMove && Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      final Predicate<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitIsTransport());
      return match.test(u);
    });
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedBombard(final PlayerID player) {
    return Match.of(u -> {
      if (Matches.unitCanNotMoveDuringCombatMove().test(u)) {
        return false;
      }
      final Predicate<Unit> match = Match.allOf(unitCanBeMovedAndIsOwned(player), Matches.unitCanBombard(player));
      return match.test(u);
    });
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedNonCombatInfra(final PlayerID player) {
    return Match.allOf(unitCanBeMovedAndIsOwned(player),
        Matches.unitCanNotMoveDuringCombatMove(), Matches.unitIsInfrastructure());
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefender(final PlayerID player, final GameData data,
      final Territory t) {
    final Predicate<Unit> myUnitHasNoMovementMatch =
        Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft().negate());
    final Predicate<Unit> alliedUnitMatch =
        Match.allOf(Matches.unitIsOwnedBy(player).negate(), Matches.isUnitAllied(player, data),
            Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(t.getUnits().getUnits(), null, player,
                data, false).negate());
    return Match.anyOf(myUnitHasNoMovementMatch, alliedUnitMatch);
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefenderAndNotInfra(final PlayerID player,
      final GameData data,
      final Territory t) {
    return Match.allOf(unitCantBeMovedAndIsAlliedDefender(player, data, t),
        Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedLandAndNotInfra(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.unitIsLand(), Matches.isUnitAllied(player, data),
        Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedNotOwned(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.unitIsOwnedBy(player).negate(), Matches.isUnitAllied(player, data));
  }

  public static Predicate<Unit> unitIsAlliedNotOwnedAir(final PlayerID player, final GameData data) {
    return Match.allOf(unitIsAlliedNotOwned(player, data), Matches.unitIsAir());
  }

  static Predicate<Unit> unitIsAlliedAir(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.isUnitAllied(player, data), Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAir(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAndNotAa(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsAaForAnything().negate());
  }

  public static Predicate<Unit> unitIsEnemyAndNotInfa(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsEnemyNotLand(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), Matches.unitIsNotLand());
  }

  static Predicate<Unit> unitIsEnemyNotNeutral(final PlayerID player, final GameData data) {
    return Match.allOf(Matches.enemyUnit(player, data), unitIsNeutral().negate());
  }

  private static Predicate<Unit> unitIsNeutral() {
    return Match.of(u -> u.getOwner().isNull());
  }

  static Predicate<Unit> unitIsOwnedAir(final PlayerID player) {
    return Match.allOf(Matches.unitOwnedBy(player), Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsOwnedAndMatchesTypeAndIsTransporting(final PlayerID player,
      final UnitType unitType) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(unitType),
        Matches.unitIsTransporting());
  }

  public static Predicate<Unit> unitIsOwnedAndMatchesTypeAndNotTransporting(final PlayerID player,
      final UnitType unitType) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(unitType),
        Matches.unitIsTransporting().negate());
  }

  public static Predicate<Unit> unitIsOwnedCarrier(final PlayerID player) {
    return Match.of(unit -> UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
        && Matches.unitIsOwnedBy(player).test(unit));
  }

  public static Predicate<Unit> unitIsOwnedNotLand(final PlayerID player) {
    return Match.allOf(Matches.unitIsNotLand(), Matches.unitIsOwnedBy(player));
  }

  public static Predicate<Unit> unitIsOwnedTransport(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitIsTransport());
  }

  public static Predicate<Unit> unitIsOwnedTransportableUnit(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitCanBeTransported(), Matches.unitCanMove());
  }

  public static Predicate<Unit> unitIsOwnedCombatTransportableUnit(final PlayerID player) {
    return Match.allOf(Matches.unitIsOwnedBy(player), Matches.unitCanBeTransported(),
        Matches.unitCanNotMoveDuringCombatMove().negate(), Matches.unitCanMove());
  }

  public static Predicate<Unit> unitIsOwnedTransportableUnitAndCanBeLoaded(final PlayerID player, final Unit transport,
      final boolean isCombatMove) {
    return Match.of(u -> {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      if (isCombatMove
          && (Matches.unitCanNotMoveDuringCombatMove().test(u) || !ua.canInvadeFrom(transport))) {
        return false;
      }
      final Predicate<Unit> match = Match.allOf(unitIsOwnedTransportableUnit(player), Matches.unitHasNotMoved(),
          Matches.unitHasMovementLeft(), Matches.unitIsBeingTransported().negate());
      return match.test(u);
    });
  }

  /**
   * Check what units a territory can produce.
   *
   * @param t
   *        territory we are testing for required units
   * @return whether the territory contains one of the required combos of units
   */
  public static Predicate<Unit> unitWhichRequiresUnitsHasRequiredUnits(final Territory t) {
    return Match.of(unitWhichRequiresUnits -> {
      if (!Matches.unitRequiresUnitsOnCreation().test(unitWhichRequiresUnits)) {
        return true;
      }
      final Collection<Unit> unitsAtStartOfTurnInProducer = t.getUnits().getUnits();
      if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer)
          .test(unitWhichRequiresUnits)) {
        return true;
      }
      if (t.isWater() && Matches.unitIsSea().test(unitWhichRequiresUnits)) {
        for (final Territory neighbor : t.getData().getMap().getNeighbors(t, Matches.territoryIsLand())) {
          final Collection<Unit> unitsAtStartOfTurnInCurrent = neighbor.getUnits().getUnits();
          if (Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent)
              .test(unitWhichRequiresUnits)) {
            return true;
          }
        }
      }
      return false;
    });
  }
}
