package games.strategy.triplea.ai.pro.util;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.RelationshipTracker;
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
                    player,
                    data.getProperties(),
                    data.getRelationshipTracker(),
                    isCombatMove,
                    false,
                    false,
                    true,
                    true))
            .and(not(enemyTerritories::contains));
    if (!isCombatMove) {
      match =
          match.and(
              Matches.territoryIsNeutralButNotWater()
                  .or(
                      Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassableOrRestricted(
                          player, data.getProperties(), data.getRelationshipTracker()))
                  .negate());
    }
    return ((Predicate<Territory>) alliedTerritories::contains).or(match);
  }

  public static Predicate<Territory> territoryCanMoveAirUnits(
      final GameState data, final GamePlayer player, final boolean isCombatMove) {
    return Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
        .and(
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                player,
                data.getProperties(),
                data.getRelationshipTracker(),
                isCombatMove,
                false,
                false,
                true,
                false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveAirUnits(
      final GamePlayer player, final GameProperties properties) {
    return Matches.territoryDoesNotCostMoneyToEnter(properties)
        .and(Matches.territoryIsPassableAndNotRestricted(player, properties));
  }

  public static Predicate<Territory> territoryCanMoveAirUnitsAndNoAa(
      final GameState data, final GamePlayer player, final boolean isCombatMove) {
    return territoryCanMoveAirUnits(data, player, isCombatMove)
        .and(Matches.territoryHasEnemyAaForFlyOver(player, data.getRelationshipTracker()).negate());
  }

  public static Predicate<Territory> territoryCanMoveSpecificLandUnit(
      final GameState data, final GamePlayer player, final boolean isCombatMove, final Unit unit) {
    return t -> {
      final Predicate<Territory> territoryMatch =
          Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
              .and(
                  Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                      player,
                      data.getProperties(),
                      data.getRelationshipTracker(),
                      isCombatMove,
                      true,
                      false,
                      false,
                      false));
      final Predicate<Unit> unitMatch =
          Matches.unitIsOfTypes(
                  TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t))
              .negate();
      return territoryMatch.test(t) && unitMatch.test(unit);
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
      final GameState data, final GamePlayer player, final boolean isCombatMove) {
    return Matches.territoryDoesNotCostMoneyToEnter(data.getProperties())
        .and(
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                player,
                data.getProperties(),
                data.getRelationshipTracker(),
                isCombatMove,
                true,
                false,
                false,
                false));
  }

  public static Predicate<Territory> territoryCanPotentiallyMoveLandUnits(
      final GamePlayer player, final GameProperties properties) {
    return Matches.territoryIsLand()
        .and(Matches.territoryDoesNotCostMoneyToEnter(properties))
        .and(Matches.territoryIsPassableAndNotRestricted(player, properties));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsAndIsAllied(
      final GameState data, final GamePlayer player) {
    return Matches.isTerritoryAllied(player, data.getRelationshipTracker())
        .and(territoryCanMoveLandUnits(data, player, false));
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThrough(
      final GameData data,
      final GamePlayer player,
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
        return territoryCanMoveSpecificLandUnit(data, player, isCombatMove, u)
            .and(alliedOrBlitzableMatch)
            .and(not(enemyTerritories::contains))
            .test(t);
      }
      return territoryCanMoveSpecificLandUnit(data, player, isCombatMove, u)
          .and(Matches.isTerritoryAllied(player, data.getRelationshipTracker()))
          .and(Matches.territoryHasNoEnemyUnits(player, data.getRelationshipTracker()))
          .and(not(enemyTerritories::contains))
          .test(t);
    };
  }

  public static Predicate<Territory> territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(
      final GameData data,
      final GamePlayer player,
      final Unit u,
      final Territory startTerritory,
      final boolean isCombatMove,
      final List<Territory> blockedTerritories,
      final List<Territory> clearedTerritories) {
    Predicate<Territory> alliedMatch =
        Matches.isTerritoryAllied(player, data.getRelationshipTracker())
            .or(clearedTerritories::contains);
    if (isCombatMove
        && Matches.unitCanBlitz().test(u)
        && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
      alliedMatch =
          Matches.isTerritoryAllied(player, data.getRelationshipTracker())
              .or(clearedTerritories::contains)
              .or(territoryIsBlitzable(player, data, u));
    }
    return territoryCanMoveSpecificLandUnit(data, player, isCombatMove, u)
        .and(alliedMatch)
        .and(not(blockedTerritories::contains));
  }

  private static Predicate<Territory> territoryIsBlitzable(
      final GamePlayer player, final GameData data, final Unit u) {
    return t ->
        Matches.territoryIsBlitzable(player, data).test(t)
            && TerritoryEffectHelper.unitKeepsBlitz(u, t);
  }

  public static Predicate<Territory> territoryCanMoveSeaUnits(
      final GameState data, final GamePlayer player, final boolean isCombatMove) {
    return t -> {
      final GameProperties properties = data.getProperties();
      final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
      final boolean navalMayNotNonComIntoControlled =
          Properties.getWW2V2(properties)
              || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(properties);
      if (!isCombatMove
          && navalMayNotNonComIntoControlled
          && Matches.isTerritoryEnemyAndNotUnownedWater(player, relationshipTracker).test(t)) {
        return false;
      }
      final Predicate<Territory> match =
          Matches.territoryDoesNotCostMoneyToEnter(properties)
              .and(
                  Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                      player,
                      properties,
                      relationshipTracker,
                      isCombatMove,
                      false,
                      true,
                      false,
                      false));
      return match.test(t);
    };
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThrough(
      final GameState data, final GamePlayer player, final boolean isCombatMove) {
    return territoryCanMoveSeaUnits(data, player, isCombatMove)
        .and(territoryHasOnlyIgnoredUnits(player, data.getRelationshipTracker()));
  }

  public static Predicate<Territory> territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(
      final GameState data,
      final GamePlayer player,
      final boolean isCombatMove,
      final List<Territory> clearedTerritories,
      final List<Territory> notTerritories) {
    final Predicate<Territory> onlyIgnoredOrClearedMatch =
        territoryHasOnlyIgnoredUnits(player, data.getRelationshipTracker())
            .or(clearedTerritories::contains);
    return territoryCanMoveSeaUnits(data, player, isCombatMove)
        .and(onlyIgnoredOrClearedMatch)
        .and(not(notTerritories::contains));
  }

  private static Predicate<Territory> territoryHasOnlyIgnoredUnits(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return t -> {
      final Predicate<Unit> subOnly =
          Matches.unitIsInfrastructure()
              .or(Matches.unitCanBeMovedThroughByEnemies())
              .or(Matches.enemyUnit(player, relationshipTracker).negate());
      return t.getUnitCollection().allMatch(subOnly)
          || Matches.territoryHasNoEnemyUnits(player, relationshipTracker).test(t);
    };
  }

  public static Predicate<Territory> territoryHasEnemyUnitsOrCantBeHeld(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.territoryHasEnemyUnits(player, relationshipTracker)
        .or(territoriesThatCantBeHeld::contains);
  }

  public static Predicate<Territory> territoryHasPotentialEnemyUnits(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<GamePlayer> players) {
    return Matches.territoryHasEnemyUnits(player, relationshipTracker)
        .or(Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(players)));
  }

  public static Predicate<Territory> territoryHasNoEnemyUnitsOrCleared(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<Territory> clearedTerritories) {
    return Matches.territoryHasNoEnemyUnits(player, relationshipTracker)
        .or(clearedTerritories::contains);
  }

  public static Predicate<Territory> territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, relationshipTracker)
        .or(Matches.territoryHasEnemyUnits(player, relationshipTracker))
        .or(territoriesThatCantBeHeld::contains);
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsLand() {
    final Predicate<Unit> infraFactory =
        Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure());
    return Matches.territoryIsLand().and(Matches.territoryHasUnitsThatMatch(infraFactory));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsEnemyLand(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return territoryHasInfraFactoryAndIsLand()
        .and(Matches.isTerritoryEnemy(player, relationshipTracker));
  }

  static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(
      final GamePlayer player,
      final List<GamePlayer> players,
      final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Territory> ownedAndCantBeHeld =
        Matches.isTerritoryOwnedBy(player).and(territoriesThatCantBeHeld::contains);
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
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    final Predicate<Unit> infraFactoryMatch =
        Matches.unitCanProduceUnits().and(Matches.unitIsInfrastructure());
    return Matches.isTerritoryAllied(player, relationshipTracker)
        .and(Matches.territoryIsLand())
        .and(Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
  }

  public static Predicate<Territory> territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(
      final GamePlayer player, final GameMap gameMap) {
    return territoryHasInfraFactoryAndIsOwnedLand(player)
        .and(Matches.territoryHasNeighborMatching(gameMap, Matches.territoryIsWater()));
  }

  public static Predicate<Territory> territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(
      final GamePlayer player, final GameData data) {
    return territoryIsNotConqueredOwnedLand(player, data)
        .and(territoryHasInfraFactoryAndIsOwnedLand(player).negate());
  }

  public static Predicate<Territory> territoryHasNeighborOwnedByAndHasLandUnit(
      final GameMap gameMap, final List<GamePlayer> players) {
    final Predicate<Territory> territoryMatch =
        Matches.isTerritoryOwnedBy(players)
            .and(Matches.territoryHasUnitsThatMatch(Matches.unitIsLand()));
    return Matches.territoryHasNeighborMatching(gameMap, territoryMatch);
  }

  static Predicate<Territory> territoryIsAlliedLandAndHasNoEnemyNeighbors(
      final GameState data, final GamePlayer player) {
    final Predicate<Territory> alliedLand =
        territoryCanMoveLandUnits(data, player, false)
            .and(Matches.isTerritoryAllied(player, data.getRelationshipTracker()));
    final Predicate<Territory> hasNoEnemyNeighbors =
        Matches.territoryHasNeighborMatching(
                data.getMap(), territoryIsEnemyNotNeutralLand(data, player))
            .negate();
    return alliedLand.and(hasNoEnemyNeighbors);
  }

  public static Predicate<Territory> territoryIsEnemyLand(
      final GameState data, final GamePlayer player) {
    return territoryCanMoveLandUnits(data, player, false)
        .and(Matches.isTerritoryEnemy(player, data.getRelationshipTracker()));
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralLand(
      final GameState data, final GamePlayer player) {
    return territoryIsEnemyLand(data, player)
        .and(Matches.territoryIsNeutralButNotWater().negate())
        .and(t -> !ProUtils.isPassiveNeutralPlayer(t.getOwner()));
  }

  public static Predicate<Territory> territoryIsOrAdjacentToEnemyNotNeutralLand(
      final GameState data, final GamePlayer player) {
    final Predicate<Territory> isMatch =
        territoryIsEnemyLand(data, player)
            .and(Matches.territoryIsNeutralButNotWater().negate())
            .and(t -> !ProUtils.isPassiveNeutralPlayer(t.getOwner()));
    final Predicate<Territory> adjacentMatch =
        territoryCanMoveLandUnits(data, player, false)
            .and(Matches.territoryHasNeighborMatching(data.getMap(), isMatch));
    return isMatch.or(adjacentMatch);
  }

  public static Predicate<Territory> territoryIsEnemyNotNeutralOrAllied(
      final GameState data, final GamePlayer player) {
    return territoryIsEnemyNotNeutralLand(data, player)
        .or(
            Matches.territoryIsLand()
                .and(Matches.isTerritoryAllied(player, data.getRelationshipTracker())));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeld(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<Territory> territoriesThatCantBeHeld) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, relationshipTracker)
        .or(territoriesThatCantBeHeld::contains);
  }

  public static Predicate<Territory> territoryIsPotentialEnemy(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<GamePlayer> players) {
    return Matches.isTerritoryEnemyAndNotUnownedWater(player, relationshipTracker)
        .or(Matches.isTerritoryOwnedBy(players));
  }

  public static Predicate<Territory> territoryIsPotentialEnemyOrHasPotentialEnemyUnits(
      final GamePlayer player,
      final RelationshipTracker relationshipTracker,
      final List<GamePlayer> players) {
    return territoryIsPotentialEnemy(player, relationshipTracker, players)
        .or(territoryHasPotentialEnemyUnits(player, relationshipTracker, players));
  }

  public static Predicate<Territory> territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(
      final GamePlayer player,
      final GameMap gameMap,
      final RelationshipTracker relationshipTracker,
      final List<Territory> territoriesThatCantBeHeld) {
    final Predicate<Unit> myUnitIsLand = Matches.unitIsOwnedBy(player).and(Matches.unitIsLand());
    final Predicate<Territory> territoryIsLandAndAdjacentToMyLandUnits =
        Matches.territoryIsLand()
            .and(
                Matches.territoryHasNeighborMatching(
                    gameMap, Matches.territoryHasUnitsThatMatch(myUnitIsLand)));
    return territoryIsLandAndAdjacentToMyLandUnits.and(
        territoryIsEnemyOrCantBeHeld(player, relationshipTracker, territoriesThatCantBeHeld));
  }

  public static Predicate<Territory> territoryIsNotConqueredOwnedLand(
      final GamePlayer player, final GameData data) {
    return t ->
        !AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)
            && Matches.isTerritoryOwnedBy(player).and(Matches.territoryIsLand()).test(t);
  }

  public static Predicate<Territory> territoryIsWaterAndAdjacentToOwnedFactory(
      final GameState data, final GamePlayer player) {
    final Predicate<Territory> hasOwnedFactoryNeighbor =
        Matches.territoryHasNeighborMatching(
            data.getMap(), ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player));
    return hasOwnedFactoryNeighbor.and(territoryCanMoveSeaUnits(data, player, true));
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
      final GamePlayer player, final RelationshipTracker relationshipTracker, final Territory t) {
    final Predicate<Unit> myUnitHasNoMovementMatch =
        Matches.unitIsOwnedBy(player).and(Matches.unitHasMovementLeft().negate());
    final Predicate<Unit> alliedUnitMatch =
        Matches.unitIsOwnedBy(player)
            .negate()
            .and(Matches.isUnitAllied(player, relationshipTracker))
            .and(
                Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(
                        t.getUnits(), player, relationshipTracker, false)
                    .negate());
    return myUnitHasNoMovementMatch.or(alliedUnitMatch);
  }

  public static Predicate<Unit> unitCantBeMovedAndIsAlliedDefenderAndNotInfra(
      final GamePlayer player, final RelationshipTracker relationshipTracker, final Territory t) {
    return unitCantBeMovedAndIsAlliedDefender(player, relationshipTracker, t)
        .and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedLandAndNotInfra(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.unitIsLand()
        .and(Matches.isUnitAllied(player, relationshipTracker))
        .and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsAlliedNotOwned(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.unitIsOwnedBy(player)
        .negate()
        .and(Matches.isUnitAllied(player, relationshipTracker));
  }

  public static Predicate<Unit> unitIsAlliedNotOwnedAir(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return unitIsAlliedNotOwned(player, relationshipTracker).and(Matches.unitIsAir());
  }

  static Predicate<Unit> unitIsAlliedAir(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.isUnitAllied(player, relationshipTracker).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAir(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.enemyUnit(player, relationshipTracker).and(Matches.unitIsAir());
  }

  public static Predicate<Unit> unitIsEnemyAndNotInfa(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.enemyUnit(player, relationshipTracker).and(Matches.unitIsNotInfrastructure());
  }

  public static Predicate<Unit> unitIsEnemyNotLand(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.enemyUnit(player, relationshipTracker).and(Matches.unitIsNotLand());
  }

  static Predicate<Unit> unitIsEnemyNotNeutral(
      final GamePlayer player, final RelationshipTracker relationshipTracker) {
    return Matches.enemyUnit(player, relationshipTracker).and(unitIsNeutral().negate());
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
