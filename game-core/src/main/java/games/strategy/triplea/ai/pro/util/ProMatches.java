package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;

/** Pro AI matches. */
@UtilityClass
public final class ProMatches {

  public static BiPredicate<Territory, Territory> noCanalsBetweenTerritories(
      final GamePlayer player, final GameData gameData) {
    return (startTerritory, endTerritory) -> {
      final Route r = new Route(startTerritory, endTerritory);
      return new MoveValidator(gameData).validateCanal(r, null, player) == null;
    };
  }

  public static Predicate<Territory> territoryCanLandAirUnits(
      final GamePlayer player,
      final GameData data,
      final boolean isCombatMove,
      final List<Territory> enemyTerritories,
      final List<Territory> alliedTerritories) {
    Predicate<Territory> match =
        Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data)
            .and(
                Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                    player, data, isCombatMove, false, false, true, true))
            .and(Matches.territoryIsInList(enemyTerritories).negate());
    if (!isCombatMove) {
      match =
          match.and(
              Matches.territoryIsNeutralButNotWater()
                  .or(
                      Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(
                          player, data))
                  .negate());
    }
    return Matches.territoryIsInList(alliedTerritories).or(match);
  }

  public static Predicate<Territory> territoryCanMoveAirUnits(
      final GamePlayer player, final GameState data, final boolean isCombatMove) {
    return Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
        .and(
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                player, data, isCombatMove, false, false, true, false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveAirUnits(
      final GamePlayer player, final GameProperties properties) {
    return Matches.territoryDoesNotCostMoneyToEnter(properties)
        .and(Matches.territoryIsPassableAndNotRestricted(player, properties));
  }

  public static Predicate<Territory> territoryCanMoveAirUnitsAndNoAa(
      final GamePlayer player, final GameState data, final boolean isCombatMove) {
    return ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove)
        .and(Matches.territoryHasEnemyAaForFlyOver(player, data.getRelationshipTracker()).negate());
  }

  public static Predicate<Territory> territoryCanMoveSpecificLandUnit(
      final GamePlayer player, final GameState data, final boolean isCombatMove, final Unit u) {
    return t -> {
      final Predicate<Territory> territoryMatch =
          Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
              .and(
                  Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                      player, data, isCombatMove, true, false, false, false));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(
                  TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t))
              .negate();
      return territoryMatch.test(t) && unitMatch.test(u);
    };
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveSpecificLandUnit(
      final GamePlayer player, final GameProperties properties, final Unit u) {
    return t -> {
      final Predicate<Territory> territoryMatch =
          Matches.territoryDoesNotCostMoneyToEnter(properties)
              .and(Matches.territoryIsPassableAndNotRestricted(player, properties));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(
                  TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t))
              .negate();
      return territoryMatch.test(t) && unitMatch.test(u);
    };
  }

  public static Predicate<Territory> territoryCanMoveLandUnits(
      final GamePlayer player, final GameState data, final boolean isCombatMove) {
    return Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
        .and(
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                player, data, isCombatMove, true, false, false, false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveLandUnits(
      final GamePlayer player, final GameProperties properties) {
    return Matches.territoryIsLand()
        .and(Matches.territoryDoesNotCostMoneyToEnter(properties))
        .and(Matches.territoryIsPassableAndNotRestricted(player, properties));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsAndIsAllied(
      final GamePlayer player, final GameState data) {
    return Matches.isTerritoryAllied(player, data.getRelationshipTracker())
        .and(territoryCanMoveLandUnits(player, data, false));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThrough(
      final GamePlayer player,
      final GameData data,
      final Unit u,
      final Territory startTerritory,
      final boolean isCombatMove,
      final List<Territory> enemyTerritories) {
    return t -> {
      if (isCombatMove
          && Matches.unitCanBlitz().test(u)
          && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
        final Predicate<Territory> alliedWithNoEnemiesMatch =
            Matches.isTerritoryAllied(player, data.getRelationshipTracker())
                .and(Matches.territoryHasNoEnemyUnits(player, data.getRelationshipTracker()));
        final Predicate<Territory> alliedOrBlitzableMatch =
            alliedWithNoEnemiesMatch.or(territoryIsBlitzable(player, data, u));
        return ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u)
            .and(alliedOrBlitzableMatch)
            .and(Matches.territoryIsInList(enemyTerritories).negate())
            .test(t);
      }
      return ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u)
          .and(Matches.isTerritoryAllied(player, data.getRelationshipTracker()))
          .and(Matches.territoryHasNoEnemyUnits(player, data.getRelationshipTracker()))
          .and(Matches.territoryIsInList(enemyTerritories).negate())
          .test(t);
    };
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
      final GamePlayer player,
      final GameData data,
      final Unit u,
      final Territory startTerritory,
      final boolean isCombatMove,
      final List<Territory> blockedTerritories,
      final List<Territory> clearedTerritories) {
    Predicate<Territory> alliedMatch =
        Matches.isTerritoryAllied(player, data.getRelationshipTracker())
            .or(Matches.territoryIsInList(clearedTerritories));
    if (isCombatMove
        && Matches.unitCanBlitz().test(u)
        && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
      alliedMatch =
          Matches.isTerritoryAllied(player, data.getRelationshipTracker())
              .or(Matches.territoryIsInList(clearedTerritories))
              .or(territoryIsBlitzable(player, data, u));
    }
    return ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u)
        .and(alliedMatch)
        .and(Matches.territoryIsInList(blockedTerritories).negate());
  }

  private static Predicate<Territory> territoryIsBlitzable(
      final GamePlayer player, final GameData data, final Unit u) {
    return t ->
        Matches.territoryIsBlitzable(player, data).test(t)
            && TerritoryEffectHelper.unitKeepsBlitz(u, t);
  }

  public static Predicate<Territory> territoryCanMoveSeaUnits(
      final GamePlayer player, final GameState data, final boolean isCombatMove) {
    return t -> {
      final boolean navalMayNotNonComIntoControlled =
          Properties.getWW2V2(data.getProperties())
              || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(
                  data.getProperties());
      if (!isCombatMove
          && navalMayNotNonComIntoControlled
          && Matches.isTerritoryEnemyAndNotUnownedWater(player, data).test(t)) {
        return false;
      }
      final Predicate<Territory> match =
          Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
              .and(
                  Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                      player, data, isCombatMove, false, true, false, false));
      return match.test(t);
    };
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThrough(
      final GamePlayer player, final GameState data, final boolean isCombatMove) {
    return territoryCanMoveSeaUnits(player, data, isCombatMove)
        .and(territoryHasOnlyIgnoredUnits(player, data));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsAndNotInList(
      final GamePlayer player,
      final GameState data,
      final boolean isCombatMove,
      final List<Territory> notTerritories) {
    return territoryCanMoveSeaUnits(player, data, isCombatMove)
        .and(Matches.territoryIsNotInList(notTerritories));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(
      final GamePlayer player,
      final GameState data,
      final boolean isCombatMove,
      final List<Territory> clearedTerritories,
      final List<Territory> notTerritories) {
    final Predicate<Territory> onlyIgnoredOrClearedMatch =
        territoryHasOnlyIgnoredUnits(player, data)
            .or(Matches.territoryIsInList(clearedTerritories));
    return territoryCanMoveSeaUnits(player, data, isCombatMove)
        .and(onlyIgnoredOrClearedMatch)
        .and(Matches.territoryIsNotInList(notTerritories));
  }

  private static Predicate<Territory> territoryHasOnlyIgnoredUnits(
      final GamePlayer player, final GameState data) {
    return t -> {
      final Predicate<Unit> subOnly =
          Matches.unitIsInfrastructure()
              .or(Matches.unitCanBeMovedThroughByEnemies())
              .or(Matches.enemyUnit(player, data.getRelationshipTracker()).negate());
      return t.getUnitCollection().allMatch(subOnly)
          || Matches.territoryHasNoEnemyUnits(player, data.getRelationshipTracker()).test(t);
    };
  }

  public static Predicate<Territory> territoryHasEnemyUnitsOrCantBeHeld(
      final GamePlayer player,
      final GameState data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.territoryHasEnemyUnits(player, data.getRelationshipTracker())
        .or(Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryHasPotentialEnemyUnits(
      final GamePlayer player, final GameState data, final List<GamePlayer> players) {
    return Matches.territoryHasEnemyUnits(player, data.getRelationshipTracker())
        .or(Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(players)));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnitsOrCleared(
      final GamePlayer player, final GameState data, final List<Territory> clearedTerritories) {
    return Matches.territoryHasNoEnemyUnits(player, data.getRelationshipTracker())
        .or(Matches.territoryIsInList(clearedTerritories));
  }

  public static Predicate<Territory> territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(
      final GamePlayer player,
      final GameState data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, data)
        .or(Matches.territoryHasEnemyUnits(player, data.getRelationshipTracker()))
        .or(Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsLand() {
    final Predicate<Unit> infraFactory =
        Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure());
    return Matches.territoryIsLand().and(Matches.territoryHasUnitsThatMatch(infraFactory));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsEnemyLand(
      final GamePlayer player, final GameState data) {
    return territoryHasInfraFactoryAndIsLand()
        .and(Matches.isTerritoryEnemy(player, data.getRelationshipTracker()));
  }

  static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(
      final GamePlayer player,
      final List<GamePlayer> players,
      final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Territory> ownedAndCantBeHeld =
        Matches.isTerritoryOwnedBy(player)
            .and(Matches.territoryIsInList(territoriesThatCantBeHeld));
    final Predicate<Territory> enemyOrOwnedCantBeHeld =
        Matches.isTerritoryOwnedBy(players).or(ownedAndCantBeHeld);
    return territoryHasInfraFactoryAndIsLand().and(enemyOrOwnedCantBeHeld);
  }

  public static Predicate<Territory> territoryHasFactoryAndIsNotConqueredOwnedLand(
      final GamePlayer player, final GameData data) {
    return territoryIsNotConqueredOwnedLand(player, data)
        .and(territoryHasFactoryAndIsOwnedLand(player));
  }

  private static Predicate<Territory> territoryHasFactoryAndIsOwnedLand(final GamePlayer player) {
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedBy(player).and(Matches.unitCanProduceUnits());
    return Matches.isTerritoryOwnedBy(player)
        .and(Matches.territoryIsLand())
        .and(Matches.territoryHasUnitsThatMatch(factoryMatch));
  }

  public static Predicate<Territory> territoryHasNonMobileFactoryAndIsNotConqueredOwnedLand(
      final GamePlayer player, final GameData data) {
    return territoryHasNonMobileInfraFactory()
        .and(territoryHasFactoryAndIsNotConqueredOwnedLand(player, data));
  }

  private static Predicate<Territory> territoryHasNonMobileInfraFactory() {
    final Predicate<Unit> nonMobileInfraFactoryMatch =
        Matches.unitCanProduceUnits()
            .and(Matches.unitIsInfrastructure())
            .and(Matches.unitHasMovementLeft().negate());
    return Matches.territoryHasUnitsThatMatch(nonMobileInfraFactoryMatch);
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLand(
      final GamePlayer player) {
    final Predicate<Unit> infraFactoryMatch =
        Matches.unitIsOwnedBy(player)
            .and(Matches.unitCanProduceUnits())
            .and(Matches.unitIsInfrastructure());
    return Matches.isTerritoryOwnedBy(player)
        .and(Matches.territoryIsLand())
        .and(Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsAlliedLand(
      final GamePlayer player, final GameState data) {
    final Predicate<Unit> infraFactoryMatch =
        Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure());
    return Matches.isTerritoryAllied(player, data.getRelationshipTracker())
        .and(Matches.territoryIsLand())
        .and(Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(
      final GamePlayer player, final GameState data) {
    return territoryHasInfraFactoryAndIsOwnedLand(player)
        .and(Matches.territoryHasNeighborMatching(data.getMap(), Matches.territoryIsWater()));
  }

  public static Predicate<Territory> territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(
      final GamePlayer player, final GameData data) {
    return territoryIsNotConqueredOwnedLand(player, data)
        .and(territoryHasInfraFactoryAndIsOwnedLand(player).negate());
  }

  public static Predicate<Territory> territoryHasNeighborOwnedByAndHasLandUnit(
      final GameState data, final List<GamePlayer> players) {
    final Predicate<Territory> territoryMatch =
        Matches.isTerritoryOwnedBy(players)
            .and(Matches.territoryHasUnitsThatMatch(Matches.unitIsLand()));
    return Matches.territoryHasNeighborMatching(data.getMap(), territoryMatch);
  }

  static Predicate<Territory> territoryIsAlliedLandAndHasNoEnemyNeighbors(
      final GamePlayer player, final GameState data) {
    final Predicate<Territory> alliedLand =
        territoryCanMoveLandUnits(player, data, false)
            .and(Matches.isTerritoryAllied(player, data.getRelationshipTracker()));
    final Predicate<Territory> hasNoEnemyNeighbors =
        Matches.territoryHasNeighborMatching(
                data.getMap(), ProMatches.territoryIsEnemyNotNeutralLand(player, data))
            .negate();
    return alliedLand.and(hasNoEnemyNeighbors);
  }

  public static Predicate<Territory> territoryIsEnemyLand(
      final GamePlayer player, final GameState data) {
    return territoryCanMoveLandUnits(player, data, false)
        .and(Matches.isTerritoryEnemy(player, data.getRelationshipTracker()));
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralLand(
      final GamePlayer player, final GameState data) {
    return territoryIsEnemyLand(player, data)
        .and(Matches.territoryIsNeutralButNotWater().negate())
        .and(t -> !ProUtils.isPassiveNeutralPlayer(t.getOwner()));
  }

  public static Predicate<Territory> territoryIsOrAdjacentToEnemyNotNeutralLand(
      final GamePlayer player, final GameState data) {
    final Predicate<Territory> isMatch =
        territoryIsEnemyLand(player, data)
            .and(Matches.territoryIsNeutralButNotWater().negate())
            .and(t -> !ProUtils.isPassiveNeutralPlayer(t.getOwner()));
    final Predicate<Territory> adjacentMatch =
        territoryCanMoveLandUnits(player, data, false)
            .and(Matches.territoryHasNeighborMatching(data.getMap(), isMatch));
    return isMatch.or(adjacentMatch);
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralOrAllied(
      final GamePlayer player, final GameState data) {
    return territoryIsEnemyNotNeutralLand(player, data)
        .or(
            Matches.territoryIsLand()
                .and(Matches.isTerritoryAllied(player, data.getRelationshipTracker())));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeld(
      final GamePlayer player,
      final GameState data,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, data)
        .or(Matches.territoryIsInList(territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsPotentialEnemy(
      final GamePlayer player, final GameState data, final List<GamePlayer> players) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, data)
        .or(Matches.isTerritoryOwnedBy(players));
  }

  public static Predicate<Territory> territoryIsPotentialEnemyOrHasPotentialEnemyUnits(
      final GamePlayer player, final GameState data, final List<GamePlayer> players) {
    return territoryIsPotentialEnemy(player, data, players)
        .or(territoryHasPotentialEnemyUnits(player, data, players));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(
      final GamePlayer player,
      final GameState data,
      final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Unit> myUnitIsLand = Matches.unitIsOwnedBy(player).and(Matches.unitIsLand());
    final Predicate<Territory> territoryIsLandAndAdjacentToMyLandUnits =
        Matches.territoryIsLand()
            .and(
                Matches.territoryHasNeighborMatching(
                    data.getMap(), Matches.territoryHasUnitsThatMatch(myUnitIsLand)));
    return territoryIsLandAndAdjacentToMyLandUnits.and(
        territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsNotConqueredOwnedLand(
      final GamePlayer player, final GameData data) {
    return t ->
        !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)
            && Matches.isTerritoryOwnedBy(player).and(Matches.territoryIsLand()).test(t);
  }

  public static Predicate<Territory> territoryIsWaterAndAdjacentToOwnedFactory(
      final GamePlayer player, final GameState data) {
    final Predicate<Territory> hasOwnedFactoryNeighbor =
        Matches.territoryHasNeighborMatching(
            data.getMap(), ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player));
    return hasOwnedFactoryNeighbor.and(ProMatches.territoryCanMoveSeaUnits(player, data, true));
  }

  private static Predicate<Unit> unitCanBeMovedAndIsOwned(final GamePlayer player) {
    return Matches.unitIsOwnedBy(player).and(Matches.unitHasMovementLeft());
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedAir(
      final GamePlayer player, final boolean isCombatMove) {
    return u ->
        (!isCombatMove || !Matches.unitCanNotMoveDuringCombatMove().test(u))
            && unitCanBeMovedAndIsOwned(player).and(Matches.unitIsAir()).test(u);
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedLand(
      final GamePlayer player, final boolean isCombatMove) {
    return u ->
        (!isCombatMove || !Matches.unitCanNotMoveDuringCombatMove().test(u))
            && unitCanBeMovedAndIsOwned(player)
                .and(Matches.unitIsLand())
                .and(Matches.unitIsBeingTransported().negate())
                .test(u);
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedSea(
      final GamePlayer player, final boolean isCombatMove) {
    return u ->
        (!isCombatMove || !Matches.unitCanNotMoveDuringCombatMove().test(u))
            && unitCanBeMovedAndIsOwned(player).and(Matches.unitIsSea()).test(u);
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedTransport(
      final GamePlayer player, final boolean isCombatMove) {
    return u ->
        (!isCombatMove || !Matches.unitCanNotMoveDuringCombatMove().test(u))
            && unitCanBeMovedAndIsOwned(player).and(Matches.unitIsTransport()).test(u);
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedBombard(final GamePlayer player) {
    return u ->
        !Matches.unitCanNotMoveDuringCombatMove().test(u)
            && unitCanBeMovedAndIsOwned(player).and(Matches.unitCanBombard(player)).test(u);
  }

  public static Predicate<Unit> unitCanBeMovedAndIsOwnedNonCombatInfra(final GamePlayer player) {
    return unitCanBeMovedAndIsOwned(player)
        .and(Matches.unitCanNotMoveDuringCombatMove())
        .and(Matches.unitIsInfrastructure());
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefender(
      final GamePlayer player, final GameState data, final Territory t) {
    final Predicate<Unit> myUnitHasNoMovementMatch =
        Matches.unitIsOwnedBy(player).and(Matches.unitHasMovementLeft().negate());
    final Predicate<Unit> alliedUnitMatch =
        Matches.unitIsOwnedBy(player)
            .negate()
            .and(Matches.isUnitAllied(player, data.getRelationshipTracker()))
            .and(
                Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(
                        t.getUnits(), player, data, false)
                    .negate());
    return myUnitHasNoMovementMatch.or(alliedUnitMatch);
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefenderAndNotInfra(
      final GamePlayer player, final GameState data, final Territory t) {
    return unitCantBeMovedAndIsAlliedDefender(player, data, t)
        .and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedLandAndNotInfra(
      final GamePlayer player, final GameState data) {
    return Matches.unitIsLand()
        .and(Matches.isUnitAllied(player, data.getRelationshipTracker()))
        .and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedNotOwned(
      final GamePlayer player, final GameState data) {
    return Matches.unitIsOwnedBy(player)
        .negate()
        .and(Matches.isUnitAllied(player, data.getRelationshipTracker()));
  }

  public static Predicate<Unit> unitIsAlliedNotOwnedAir(
      final GamePlayer player, final GameState data) {
    return unitIsAlliedNotOwned(player, data).and(Matches.unitIsAir());
  }

  static Predicate<Unit> unitIsAlliedAir(final GamePlayer player, final GameState data) {
    return Matches.isUnitAllied(player, data.getRelationshipTracker()).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAir(final GamePlayer player, final GameState data) {
    return Matches.enemyUnit(player, data.getRelationshipTracker()).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAndNotInfa(
      final GamePlayer player, final GameState data) {
    return Matches.enemyUnit(player, data.getRelationshipTracker())
        .and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsEnemyNotLand(final GamePlayer player, final GameState data) {
    return Matches.enemyUnit(player, data.getRelationshipTracker()).and(Matches.unitIsNotLand());
  }

  static Predicate<Unit> unitIsEnemyNotNeutral(final GamePlayer player, final GameState data) {
    return Matches.enemyUnit(player, data.getRelationshipTracker()).and(unitIsNeutral().negate());
  }

  private static Predicate<Unit> unitIsNeutral() {
    return u -> ProUtils.isNeutralPlayer(u.getOwner());
  }

  static Predicate<Unit> unitIsOwnedAir(final GamePlayer player) {
    return Matches.unitOwnedBy(player).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsOwnedAndMatchesTypeAndIsTransporting(
      final GamePlayer player, final UnitType unitType) {
    return Matches.unitIsOwnedBy(player)
        .and(Matches.unitIsOfType(unitType))
        .and(Unit::isTransporting);
  }

  public static Predicate<Unit> unitIsOwnedAndMatchesTypeAndNotTransporting(
      final GamePlayer player, final UnitType unitType) {
    return Matches.unitIsOwnedBy(player)
        .and(Matches.unitIsOfType(unitType))
        .and(Predicate.not(Unit::isTransporting));
  }

  public static Predicate<Unit> unitIsOwnedCarrier(final GamePlayer player) {
    return unit ->
        UnitAttachment.get(unit.getType()).getCarrierCapacity() != -1
            && Matches.unitIsOwnedBy(player).test(unit);
  }

  public static Predicate<Unit> unitIsOwnedNotLand(final GamePlayer player) {
    return Matches.unitIsNotLand().and(Matches.unitIsOwnedBy(player));
  }

  public static Predicate<Unit> unitIsOwnedTransport(final GamePlayer player) {
    return Matches.unitIsOwnedBy(player).and(Matches.unitIsTransport());
  }

  public static Predicate<Unit> unitIsOwnedTransportableUnit(final GamePlayer player) {
    return Matches.unitIsOwnedBy(player)
        .and(Matches.unitCanBeTransported())
        .and(Matches.unitCanMove());
  }

  public static Predicate<Unit> unitIsOwnedCombatTransportableUnit(final GamePlayer player) {
    return unitIsOwnedTransportableUnit(player)
        .and(Matches.unitCanNotMoveDuringCombatMove().negate());
  }

  public static Predicate<Unit> unitIsOwnedTransportableUnitAndCanBeLoaded(
      final GamePlayer player, final Unit transport, final boolean isCombatMove) {
    return u ->
        (!isCombatMove
                || (!Matches.unitCanNotMoveDuringCombatMove().test(u)
                    && UnitAttachment.get(u.getType()).canInvadeFrom(transport)))
            && unitIsOwnedTransportableUnit(player)
                .and(Matches.unitHasNotMoved())
                .and(Matches.unitHasMovementLeft())
                .and(Matches.unitIsBeingTransported().negate())
                .test(u);
  }

  public static Predicate<Unit> unitHasLessMovementThan(final Unit unit) {
    return u -> u.getMovementLeft().compareTo(unit.getMovementLeft()) < 0;
  }
}
