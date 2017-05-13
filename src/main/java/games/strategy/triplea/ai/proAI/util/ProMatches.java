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
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

/**
 * Pro AI matches.
 */
public class ProMatches {

  public static Match<Territory> territoryCanLandAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> enemyTerritories, final List<Territory> alliedTerritories) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.territoryIsInList(alliedTerritories),
            new CompositeMatchAnd<>(Matches.airCanLandOnThisAlliedNonConqueredLandTerritory(player, data),
                Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false,
                    false, true, true),
                Matches.territoryIsInList(enemyTerritories).invert()));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveAirUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.territoryDoesNotCostMoneyToEnter(data),
            Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, false,
                true, false));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanPotentiallyMoveAirUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.territoryDoesNotCostMoneyToEnter(data),
            Matches.TerritoryIsPassableAndNotRestricted(player, data));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveAirUnitsAndNoAA(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match =
            new CompositeMatchAnd<>(ProMatches.territoryCanMoveAirUnits(player, data, isCombatMove),
                Matches.territoryHasEnemyAAforAnything(player, data).invert());
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveSpecificLandUnit(final PlayerID player, final GameData data,
      final boolean isCombatMove, final Unit u) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> territoryMatch = new CompositeMatchAnd<>(Matches.territoryDoesNotCostMoneyToEnter(data),
            Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
                false, false));
        final Match<Unit> unitMatch =
            Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).invert();
        return territoryMatch.match(t) && unitMatch.match(u);
      }
    };
  }

  public static Match<Territory> territoryCanPotentiallyMoveSpecificLandUnit(final PlayerID player, final GameData data,
      final Unit u) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> territoryMatch = new CompositeMatchAnd<>(Matches.territoryDoesNotCostMoneyToEnter(data),
            Matches.TerritoryIsPassableAndNotRestricted(player, data));
        final Match<Unit> unitMatch =
            Matches.unitIsOfTypes(TerritoryEffectHelper.getUnitTypesForUnitsNotAllowedIntoTerritory(t)).invert();
        return territoryMatch.match(t) && unitMatch.match(u);
      }
    };
  }

  public static Match<Territory> territoryCanMoveLandUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.territoryDoesNotCostMoneyToEnter(data),
            Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, true, false,
                false, false));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanPotentiallyMoveLandUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.TerritoryIsLand,
            Matches.territoryDoesNotCostMoneyToEnter(data), Matches.TerritoryIsPassableAndNotRestricted(player, data));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveLandUnitsAndIsAllied(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.isTerritoryAllied(player, data),
            territoryCanMoveLandUnits(player, data, false));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveLandUnitsThrough(final PlayerID player, final GameData data,
      final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> enemyTerritories) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        Match<Territory> match =
            new CompositeMatchAnd<>(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
                Matches.isTerritoryAllied(player, data), Matches.territoryHasNoEnemyUnits(player, data),
                Matches.territoryIsInList(enemyTerritories).invert());
        if (isCombatMove && Matches.UnitCanBlitz.match(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
          final Match<Territory> alliedWithNoEnemiesMatch = new CompositeMatchAnd<>(
              Matches.isTerritoryAllied(player, data), Matches.territoryHasNoEnemyUnits(player, data));
          final Match<Territory> alliedOrBlitzableMatch =
              new CompositeMatchOr<>(alliedWithNoEnemiesMatch, territoryIsBlitzable(player, data, u));
          match = new CompositeMatchAnd<>(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
              alliedOrBlitzableMatch, Matches.territoryIsInList(enemyTerritories).invert());
        }
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveLandUnitsThroughIgnoreEnemyUnits(final PlayerID player,
      final GameData data, final Unit u, final Territory startTerritory, final boolean isCombatMove,
      final List<Territory> blockedTerritories, final List<Territory> clearedTerritories) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        Match<Territory> alliedMatch = new CompositeMatchOr<>(Matches.isTerritoryAllied(player, data),
            Matches.territoryIsInList(clearedTerritories));
        if (isCombatMove && Matches.UnitCanBlitz.match(u) && TerritoryEffectHelper.unitKeepsBlitz(u, startTerritory)) {
          alliedMatch = new CompositeMatchOr<>(Matches.isTerritoryAllied(player, data),
              Matches.territoryIsInList(clearedTerritories), territoryIsBlitzable(player, data, u));
        }
        final Match<Territory> match =
            new CompositeMatchAnd<>(ProMatches.territoryCanMoveSpecificLandUnit(player, data, isCombatMove, u),
                alliedMatch, Matches.territoryIsInList(blockedTerritories).invert());
        return match.match(t);
      }
    };
  }

  private static Match<Territory> territoryIsBlitzable(final PlayerID player, final GameData data, final Unit u) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        return Matches.TerritoryIsBlitzable(player, data).match(t) && TerritoryEffectHelper.unitKeepsBlitz(u, t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveSeaUnits(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final boolean navalMayNotNonComIntoControlled =
            Properties.getWW2V2(data) || Properties.getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(data);
        if (!isCombatMove && navalMayNotNonComIntoControlled
            && Matches.isTerritoryEnemyAndNotUnownedWater(player, data).match(t)) {
          return false;
        }
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.territoryDoesNotCostMoneyToEnter(data),
            Matches.TerritoryIsPassableAndNotRestrictedAndOkByRelationships(player, data, isCombatMove, false, true,
                false, false));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveSeaUnitsThrough(final PlayerID player, final GameData data,
      final boolean isCombatMove) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryCanMoveSeaUnits(player, data, isCombatMove),
            territoryHasOnlyIgnoredUnits(player, data));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveSeaUnitsAndNotInList(final PlayerID player, final GameData data,
      final boolean isCombatMove, final List<Territory> notTerritories) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryCanMoveSeaUnits(player, data, isCombatMove),
            Matches.territoryIsNotInList(notTerritories));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryCanMoveSeaUnitsThroughOrClearedAndNotInList(final PlayerID player,
      final GameData data, final boolean isCombatMove, final List<Territory> clearedTerritories,
      final List<Territory> notTerritories) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> onlyIgnoredOrClearedMatch = new CompositeMatchOr<>(
            territoryHasOnlyIgnoredUnits(player, data), Matches.territoryIsInList(clearedTerritories));
        final Match<Territory> match = new CompositeMatchAnd<>(territoryCanMoveSeaUnits(player, data, isCombatMove),
            onlyIgnoredOrClearedMatch, Matches.territoryIsNotInList(notTerritories));
        return match.match(t);
      }
    };
  }

  private static Match<Territory> territoryHasOnlyIgnoredUnits(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final CompositeMatch<Unit> subOnly = new CompositeMatchOr<>(Matches.UnitIsInfrastructure, Matches.UnitIsSub,
            Matches.enemyUnit(player, data).invert());
        return (Properties.getIgnoreSubInMovement(data) && t.getUnits().allMatch(subOnly))
            || Matches.territoryHasNoEnemyUnits(player, data).match(t);
      }
    };
  }

  public static Match<Territory> territoryHasEnemyUnitsOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.territoryHasEnemyUnits(player, data),
            Matches.territoryIsInList(territoriesThatCantBeHeld));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasPotentialEnemyUnits(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.territoryHasEnemyUnits(player, data),
            Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(players)));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasNoEnemyUnitsOrCleared(final PlayerID player, final GameData data,
      final List<Territory> clearedTerritories) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.territoryHasNoEnemyUnits(player, data),
            Matches.territoryIsInList(clearedTerritories));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyOrHasEnemyUnitsOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
            Matches.territoryHasEnemyUnits(player, data), Matches.territoryIsInList(territoriesThatCantBeHeld));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsLand() {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Unit> infraFactoryMatch =
            new CompositeMatchAnd<>(Matches.UnitCanProduceUnits, Matches.UnitIsInfrastructure);
        final Match<Territory> match =
            new CompositeMatchAnd<>(Matches.TerritoryIsLand, Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsEnemyLand(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match =
            new CompositeMatchAnd<>(territoryHasInfraFactoryAndIsLand(), Matches.isTerritoryEnemy(player, data));
        return match.match(t);
      }
    };
  }

  static Match<Territory> territoryHasInfraFactoryAndIsOwnedByPlayersOrCantBeHeld(final PlayerID player,
      final List<PlayerID> players, final List<Territory> territoriesThatCantBeHeld) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> ownedAndCantBeHeld = new CompositeMatchAnd<>(Matches.isTerritoryOwnedBy(player),
            Matches.territoryIsInList(territoriesThatCantBeHeld));
        final Match<Territory> enemyOrOwnedCantBeHeld =
            new CompositeMatchOr<>(Matches.isTerritoryOwnedBy(players), ownedAndCantBeHeld);
        final Match<Territory> match =
            new CompositeMatchAnd<>(territoryHasInfraFactoryAndIsLand(), enemyOrOwnedCantBeHeld);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryIsNotConqueredOwnedLand(player, data),
            territoryHasInfraFactoryAndIsOwnedLand(player));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasNonMobileInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryHasNonMobileInfraFactory(),
            territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
        return match.match(t);
      }
    };
  }

  private static Match<Territory> territoryHasNonMobileInfraFactory() {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Unit> nonMobileInfraFactoryMatch = new CompositeMatchAnd<>(Matches.UnitCanProduceUnits,
            Matches.UnitIsInfrastructure, Matches.unitHasMovementLeft.invert());
        final Match<Territory> match = Matches.territoryHasUnitsThatMatch(nonMobileInfraFactoryMatch);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsOwnedLand(final PlayerID player) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Unit> infraFactoryMatch = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player),
            Matches.UnitCanProduceUnits, Matches.UnitIsInfrastructure);
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.isTerritoryOwnedBy(player),
            Matches.TerritoryIsLand, Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsAlliedLand(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Unit> infraFactoryMatch =
            new CompositeMatchAnd<>(Matches.UnitCanProduceUnits, Matches.UnitIsInfrastructure);
        final Match<Territory> match = new CompositeMatchAnd<>(Matches.isTerritoryAllied(player, data),
            Matches.TerritoryIsLand, Matches.territoryHasUnitsThatMatch(infraFactoryMatch));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasInfraFactoryAndIsOwnedLandAdjacentToSea(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryHasInfraFactoryAndIsOwnedLand(player),
            Matches.territoryHasNeighborMatching(data, Matches.TerritoryIsWater));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryIsNotConqueredOwnedLand(player, data),
            territoryHasInfraFactoryAndIsOwnedLand(player).invert());
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryHasNeighborOwnedByAndHasLandUnit(final GameData data,
      final List<PlayerID> players) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> territoryMatch = new CompositeMatchAnd<>(Matches.isTerritoryOwnedBy(players),
            Matches.territoryHasUnitsThatMatch(Matches.UnitIsLand));
        final Match<Territory> match = Matches.territoryHasNeighborMatching(data, territoryMatch);
        return match.match(t);
      }
    };
  }

  static Match<Territory> territoryIsAlliedLandAndHasNoEnemyNeighbors(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> alliedLand = new CompositeMatchAnd<>(territoryCanMoveLandUnits(player, data, false),
            Matches.isTerritoryAllied(player, data));
        final Match<Territory> hasNoEnemyNeighbors = Matches
            .territoryHasNeighborMatching(data, ProMatches.territoryIsEnemyNotNeutralLand(player, data)).invert();
        final Match<Territory> match = new CompositeMatchAnd<>(alliedLand, hasNoEnemyNeighbors);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyLand(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchAnd<>(territoryCanMoveLandUnits(player, data, false),
            Matches.isTerritoryEnemy(player, data));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyNotNeutralLand(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match =
            new CompositeMatchAnd<>(territoryIsEnemyLand(player, data), Matches.TerritoryIsNeutralButNotWater.invert());
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsOrAdjacentToEnemyNotNeutralLand(final PlayerID player,
      final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> isMatch =
            new CompositeMatchAnd<>(territoryIsEnemyLand(player, data), Matches.TerritoryIsNeutralButNotWater.invert());
        final Match<Territory> adjacentMatch = new CompositeMatchAnd<>(territoryCanMoveLandUnits(player, data, false),
            Matches.territoryHasNeighborMatching(data, isMatch));
        final Match<Territory> match = new CompositeMatchOr<>(isMatch, adjacentMatch);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyNotNeutralOrAllied(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> alliedLand =
            new CompositeMatchAnd<>(Matches.TerritoryIsLand, Matches.isTerritoryAllied(player, data));
        final Match<Territory> match = new CompositeMatchOr<>(territoryIsEnemyNotNeutralLand(player, data), alliedLand);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyOrCantBeHeld(final PlayerID player, final GameData data,
      final List<Territory> territoriesThatCantBeHeld) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
            Matches.territoryIsInList(territoriesThatCantBeHeld));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsPotentialEnemy(final PlayerID player, final GameData data,
      final List<PlayerID> players) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(Matches.isTerritoryEnemyAndNotUnownedWater(player, data),
            Matches.isTerritoryOwnedBy(players));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsPotentialEnemyOrHasPotentialEnemyUnits(final PlayerID player,
      final GameData data, final List<PlayerID> players) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> match = new CompositeMatchOr<>(territoryIsPotentialEnemy(player, data, players),
            territoryHasPotentialEnemyUnits(player, data, players));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsEnemyOrCantBeHeldAndIsAdjacentToMyLandUnits(final PlayerID player,
      final GameData data, final List<Territory> territoriesThatCantBeHeld) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Unit> myUnitIsLand = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand);
        final Match<Territory> territoryIsLandAndAdjacentToMyLandUnits =
            new CompositeMatchAnd<>(Matches.TerritoryIsLand,
                Matches.territoryHasNeighborMatching(data, Matches.territoryHasUnitsThatMatch(myUnitIsLand)));
        final Match<Territory> match = new CompositeMatchAnd<>(territoryIsLandAndAdjacentToMyLandUnits,
            territoryIsEnemyOrCantBeHeld(player, data, territoriesThatCantBeHeld));
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsNotConqueredAlliedLand(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
          return false;
        }
        final Match<Territory> match =
            new CompositeMatchAnd<>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsNotConqueredOwnedLand(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        if (AbstractMoveDelegate.getBattleTracker(data).wasConquered(t)) {
          return false;
        }
        final Match<Territory> match =
            new CompositeMatchAnd<>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
        return match.match(t);
      }
    };
  }

  public static Match<Territory> territoryIsWaterAndAdjacentToOwnedFactory(final PlayerID player, final GameData data) {
    return new Match<Territory>() {
      @Override
      public boolean match(final Territory t) {
        final Match<Territory> hasOwnedFactoryNeighbor =
            Matches.territoryHasNeighborMatching(data, ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player));
        final Match<Territory> match =
            new CompositeMatchAnd<>(hasOwnedFactoryNeighbor, ProMatches.territoryCanMoveSeaUnits(player, data, true));
        return match.match(t);
      }
    };
  }

  private static Match<Unit> unitCanBeMovedAndIsOwned(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedAir(final PlayerID player, final boolean isCombatMove) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (isCombatMove && Matches.UnitCanNotMoveDuringCombatMove.match(u)) {
          return false;
        }
        final Match<Unit> match = new CompositeMatchAnd<>(unitCanBeMovedAndIsOwned(player), Matches.UnitIsAir);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedLand(final PlayerID player, final boolean isCombatMove) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (isCombatMove && Matches.UnitCanNotMoveDuringCombatMove.match(u)) {
          return false;
        }
        final Match<Unit> match = new CompositeMatchAnd<>(unitCanBeMovedAndIsOwned(player), Matches.UnitIsLand,
            Matches.unitIsBeingTransported().invert());
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedSea(final PlayerID player, final boolean isCombatMove) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (isCombatMove && Matches.UnitCanNotMoveDuringCombatMove.match(u)) {
          return false;
        }
        final Match<Unit> match = new CompositeMatchAnd<>(unitCanBeMovedAndIsOwned(player), Matches.UnitIsSea);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedTransport(final PlayerID player, final boolean isCombatMove) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (isCombatMove && Matches.UnitCanNotMoveDuringCombatMove.match(u)) {
          return false;
        }
        final Match<Unit> match = new CompositeMatchAnd<>(unitCanBeMovedAndIsOwned(player), Matches.UnitIsTransport);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedBombard(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (Matches.UnitCanNotMoveDuringCombatMove.match(u)) {
          return false;
        }
        final Match<Unit> match =
            new CompositeMatchAnd<>(unitCanBeMovedAndIsOwned(player), Matches.unitCanBombard(player));
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCanBeMovedAndIsOwnedNonCombatInfra(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(unitCanBeMovedAndIsOwned(player),
            Matches.UnitCanNotMoveDuringCombatMove, Matches.UnitIsInfrastructure);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCantBeMovedAndIsAlliedDefender(final PlayerID player, final GameData data,
      final Territory t) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> myUnitHasNoMovementMatch =
            new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.unitHasMovementLeft.invert());
        final Match<Unit> alliedUnitMatch =
            new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data),
                Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(t.getUnits().getUnits(), null, player,
                    data, false).invert());
        final Match<Unit> match = new CompositeMatchOr<>(myUnitHasNoMovementMatch, alliedUnitMatch);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitCantBeMovedAndIsAlliedDefenderAndNotInfra(final PlayerID player, final GameData data,
      final Territory t) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(unitCantBeMovedAndIsAlliedDefender(player, data, t),
            Matches.UnitIsNotInfrastructure);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsAlliedLandAndNotInfra(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.UnitIsLand, Matches.isUnitAllied(player, data),
            Matches.UnitIsNotInfrastructure);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsAlliedNotOwned(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match =
            new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player).invert(), Matches.isUnitAllied(player, data));
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsAlliedNotOwnedAir(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(unitIsAlliedNotOwned(player, data), Matches.UnitIsAir);
        return match.match(u);
      }
    };
  }

  static Match<Unit> unitIsAlliedAir(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.isUnitAllied(player, data), Matches.UnitIsAir);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsEnemyAir(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.enemyUnit(player, data), Matches.UnitIsAir);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsEnemyAndNotAA(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match =
            new CompositeMatchAnd<>(Matches.enemyUnit(player, data), Matches.UnitIsAAforAnything.invert());
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsEnemyAndNotInfa(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match =
            new CompositeMatchAnd<>(Matches.enemyUnit(player, data), Matches.UnitIsNotInfrastructure);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsEnemyNotLand(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.enemyUnit(player, data), Matches.UnitIsNotLand);
        return match.match(u);
      }
    };
  }

  static Match<Unit> unitIsEnemyNotNeutral(final PlayerID player, final GameData data) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.enemyUnit(player, data), unitIsNeutral().invert());
        return match.match(u);
      }
    };
  }

  private static Match<Unit> unitIsNeutral() {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (u.getOwner().isNull()) {
          return true;
        }
        return false;
      }
    };
  }

  static Match<Unit> unitIsOwnedAir(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.unitOwnedBy(player), Matches.UnitIsAir);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedAndMatchesTypeAndIsTransporting(final PlayerID player, final UnitType unitType) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(unitType),
            Matches.unitIsTransporting());
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedAndMatchesTypeAndNotTransporting(final PlayerID player,
      final UnitType unitType) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.unitIsOfType(unitType),
            Matches.unitIsTransporting().invert());
        return match.match(u);
      }
    };
  }

  public static Match<Unit> UnitIsOwnedCarrier(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final UnitAttachment ua = UnitAttachment.get(u.getType());
        return ua.getCarrierCapacity() != -1 && Matches.unitIsOwnedBy(player).match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedNotLand(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.UnitIsNotLand, Matches.unitIsOwnedBy(player));
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedTransport(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedTransportableUnit(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match =
            new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanMove);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedCombatTransportableUnit(final PlayerID player) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        final Match<Unit> match = new CompositeMatchAnd<>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported,
            Matches.UnitCanNotMoveDuringCombatMove.invert(), Matches.UnitCanMove);
        return match.match(u);
      }
    };
  }

  public static Match<Unit> unitIsOwnedTransportableUnitAndCanBeLoaded(final PlayerID player,
      final boolean isCombatMove) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit u) {
        if (isCombatMove && Matches.UnitCanNotMoveDuringCombatMove.match(u)) {
          return false;
        }
        final Match<Unit> match = new CompositeMatchAnd<>(unitIsOwnedTransportableUnit(player), Matches.unitHasNotMoved,
            Matches.unitHasMovementLeft, Matches.unitIsBeingTransported().invert());
        return match.match(u);
      }
    };
  }

  /**
   * Check what units a territory can produce.
   *
   * @param t
   *        territory we are testing for required units
   * @return whether the territory contains one of the required combos of units
   */
  public static Match<Unit> unitWhichRequiresUnitsHasRequiredUnits(final Territory t) {
    return new Match<Unit>() {
      @Override
      public boolean match(final Unit unitWhichRequiresUnits) {
        if (!Matches.UnitRequiresUnitsOnCreation.match(unitWhichRequiresUnits)) {
          return true;
        }
        final Collection<Unit> unitsAtStartOfTurnInProducer = t.getUnits().getUnits();
        if (Matches.UnitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInProducer)
            .match(unitWhichRequiresUnits)) {
          return true;
        }
        if (t.isWater() && Matches.UnitIsSea.match(unitWhichRequiresUnits)) {
          for (final Territory neighbor : t.getData().getMap().getNeighbors(t, Matches.TerritoryIsLand)) {
            final Collection<Unit> unitsAtStartOfTurnInCurrent = neighbor.getUnits().getUnits();
            if (Matches.UnitWhichRequiresUnitsHasRequiredUnitsInList(unitsAtStartOfTurnInCurrent)
                .match(unitWhichRequiresUnits)) {
              return true;
            }
          }
        }
        return false;
      }
    };
  }
}
