package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.ai.proAI.data.ProPlaceTerritory;
import games.strategy.triplea.ai.proAI.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.proAI.data.ProTerritory;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pro AI battle utilities.
 */
public class ProBattleUtils {

  public final static int SHORT_RANGE = 2;
  public final static int MEDIUM_RANGE = 3;

  private static IOddsCalculator calc;
  private static boolean isCanceled = false;

  public static void setOddsCalculator(final IOddsCalculator calc) {
    ProBattleUtils.calc = calc;
  }

  public static void setData(final GameData data) {
    calc.setGameData(data);
  }

  public static void cancelCalcs() {
    calc.cancel();
    isCanceled = true;
  }

  public static void clearData() {
    if (calc != null) {
      calc.setGameData(null);
    }
  }

  public static boolean checkForOverwhelmingWin(final PlayerID player, final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits) {
    final GameData data = ProData.getData();

    if (defendingUnits.isEmpty() && !attackingUnits.isEmpty()) {
      return true;
    }

    // Check that defender has at least 1 power
    final double power = estimatePower(defendingUnits.get(0).getOwner(), t, defendingUnits, attackingUnits, false);
    if (power == 0 && !attackingUnits.isEmpty()) {
      return true;
    }

    // Determine if enough attack power to win in 1 round
    final List<Unit> sortedUnitsList = new ArrayList<Unit>(attackingUnits);
    Collections.sort(sortedUnitsList,
        new UnitBattleComparator(false, ProData.unitValueMap, TerritoryEffectHelper.getEffects(t), data, false, false));
    Collections.reverse(sortedUnitsList);
    final int attackPower =
        DiceRoll.getTotalPowerAndRolls(
            DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, defendingUnits, false,
                false, player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
    final List<Unit> defendersWithHitPoints = Match.getMatches(defendingUnits, Matches.UnitIsInfrastructure.invert());
    final int totalDefenderHitPoints = BattleCalculator.getTotalHitpointsLeft(defendersWithHitPoints);
    return ((attackPower / data.getDiceSides()) >= totalDefenderHitPoints);
  }

  public static double estimateStrengthDifference(final Territory t, final List<Unit> attackingUnits,
      final List<Unit> defendingUnits) {

    if (attackingUnits.size() == 0) {
      return 0;
    }
    final List<Unit> actualDefenders = Match.getMatches(defendingUnits, Matches.UnitIsInfrastructure.invert());
    if (actualDefenders.size() == 0) {
      return 100;
    }
    final double attackerStrength =
        estimateStrength(attackingUnits.get(0).getOwner(), t, attackingUnits, actualDefenders, true);
    final double defenderStrength =
        estimateStrength(actualDefenders.get(0).getOwner(), t, actualDefenders, attackingUnits, false);
    return ((attackerStrength - defenderStrength) / Math.pow(defenderStrength, 0.85) * 50 + 50);
  }

  public static double estimateStrength(final PlayerID player, final Territory t, final List<Unit> myUnits,
      final List<Unit> enemyUnits, final boolean attacking) {
    final GameData data = ProData.getData();

    List<Unit> unitsThatCanFight =
        Match.getMatches(myUnits, Matches.UnitCanBeInBattle(attacking, !t.isWater(), data, 1, false, true, true));
    if (Properties.getTransportCasualtiesRestricted(data)) {
      unitsThatCanFight = Match.getMatches(unitsThatCanFight, Matches.UnitIsTransportButNotCombatTransport.invert());
    }
    final int myHP = BattleCalculator.getTotalHitpointsLeft(unitsThatCanFight);
    final double myPower = estimatePower(player, t, myUnits, enemyUnits, attacking);
    return (2 * myHP) + myPower;
  }

  private static double estimatePower(final PlayerID player, final Territory t, final List<Unit> myUnits,
      final List<Unit> enemyUnits, final boolean attacking) {
    final GameData data = ProData.getData();

    final List<Unit> unitsThatCanFight =
        Match.getMatches(myUnits, Matches.UnitCanBeInBattle(attacking, !t.isWater(), data, 1, false, true, true));
    final List<Unit> sortedUnitsList = new ArrayList<Unit>(unitsThatCanFight);
    Collections.sort(sortedUnitsList,
        new UnitBattleComparator(!attacking, ProData.unitValueMap, TerritoryEffectHelper.getEffects(t), data, false,
            false));
    Collections.reverse(sortedUnitsList);
    final int myPower =
        DiceRoll.getTotalPowerAndRolls(
            DiceRoll.getUnitPowerAndRollsForNormalBattles(sortedUnitsList, sortedUnitsList, enemyUnits, !attacking,
                false, player, data, t, TerritoryEffectHelper.getEffects(t), false, null), data).getFirst();
    return (myPower * 6.0 / data.getDiceSides());
  }

  public static ProBattleResult estimateAttackBattleResults(final PlayerID player, final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {

    final ProBattleResult result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
    if (result != null) {
      return result;
    }

    // Determine if attackers have no chance
    final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
    if (strengthDifference < 45) {
      return new ProBattleResult(0, -999, false, new ArrayList<Unit>(), defendingUnits, 1);
    }
    return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits);
  }

  public static ProBattleResult estimateDefendBattleResults(final PlayerID player, final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {

    final ProBattleResult result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
    if (result != null) {
      return result;
    }

    // Determine if defenders have no chance
    final double strengthDifference = estimateStrengthDifference(t, attackingUnits, defendingUnits);
    if (strengthDifference > 55) {
      final boolean isLandAndCanOnlyBeAttackedByAir = !t.isWater() && Match.allMatch(attackingUnits, Matches.UnitIsAir);
      return new ProBattleResult(100 + strengthDifference, 999 + strengthDifference, !isLandAndCanOnlyBeAttackedByAir,
          attackingUnits, new ArrayList<Unit>(), 1);
    }
    return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits);
  }

  public static ProBattleResult calculateBattleResults(final PlayerID player, final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits,
      final boolean isAttacker) {

    final ProBattleResult result = checkIfNoAttackersOrDefenders(t, attackingUnits, defendingUnits);
    if (result != null) {
      return result;
    }
    return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits);
  }

  private static ProBattleResult checkIfNoAttackersOrDefenders(final Territory t, final List<Unit> attackingUnits,
      final List<Unit> defendingUnits) {
    final GameData data = ProData.getData();

    final boolean hasNoDefenders = Match.noneMatch(defendingUnits, Matches.UnitIsNotInfrastructure);
    final boolean isLandAndCanOnlyBeAttackedByAir = !t.isWater() && Match.allMatch(attackingUnits, Matches.UnitIsAir);
    if (attackingUnits.size() == 0) {
      return new ProBattleResult();
    } else if (hasNoDefenders && isLandAndCanOnlyBeAttackedByAir) {
      return new ProBattleResult();
    } else if (hasNoDefenders) {
      return new ProBattleResult(100, 0.1, true, attackingUnits, new ArrayList<Unit>(), 0);
    } else if (Properties.getSubRetreatBeforeBattle(data) && Match.allMatch(defendingUnits, Matches.UnitIsSub)
        && Match.noneMatch(attackingUnits, Matches.UnitIsDestroyer)) {
      return new ProBattleResult();
    }
    return null;
  }


  public static ProBattleResult callBattleCalculator(final PlayerID player, final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits) {
    return callBattleCalculator(player, t, attackingUnits, defendingUnits, bombardingUnits, false);
  }

  public static ProBattleResult callBattleCalculator(final PlayerID player, final Territory t,
      final List<Unit> attackingUnits, final List<Unit> defendingUnits, final Set<Unit> bombardingUnits,
      final boolean retreatWhenOnlyAirLeft) {
    final GameData data = ProData.getData();

    if (isCanceled || attackingUnits.isEmpty() || defendingUnits.isEmpty()) {
      return new ProBattleResult();
    }

    // Use battle calculator (hasLandUnitRemaining is always true for naval territories)
    AggregateResults results = null;
    final int minArmySize = Math.min(attackingUnits.size(), defendingUnits.size());
    final int runCount = Math.max(16, 100 - minArmySize);
    final PlayerID attacker = attackingUnits.get(0).getOwner();
    final PlayerID defender = defendingUnits.get(0).getOwner();
    if (retreatWhenOnlyAirLeft) {
      calc.setRetreatWhenOnlyAirLeft(true);
    }
    results =
        calc.setCalculateDataAndCalculate(attacker, defender, t, attackingUnits, defendingUnits, new ArrayList<Unit>(
            bombardingUnits), TerritoryEffectHelper.getEffects(t), runCount);
    if (retreatWhenOnlyAirLeft) {
      calc.setRetreatWhenOnlyAirLeft(false);
    }

    // Find battle result statistics
    final double winPercentage = results.getAttackerWinPercent() * 100;
    final List<Unit> averageAttackersRemaining = results.GetAverageAttackingUnitsRemaining();
    final List<Unit> averageDefendersRemaining = results.GetAverageDefendingUnitsRemaining();
    final List<Unit> mainCombatAttackers =
        Match.getMatches(attackingUnits, Matches.UnitCanBeInBattle(true, !t.isWater(), data, 1, false, true, true));
    final List<Unit> mainCombatDefenders =
        Match.getMatches(defendingUnits, Matches.UnitCanBeInBattle(false, !t.isWater(), data, 1, false, true, true));
    double TUVswing = results.getAverageTUVswing(attacker, mainCombatAttackers, defender, mainCombatDefenders, data);
    if (Matches.TerritoryIsNeutralButNotWater.match(t)) // Set TUV swing for neutrals
    {
      final double attackingUnitValue = BattleCalculator.getTUV(mainCombatAttackers, ProData.unitValueMap);
      final double remainingUnitValue =
          results.getAverageTUVofUnitsLeftOver(ProData.unitValueMap, ProData.unitValueMap).getFirst();
      TUVswing = remainingUnitValue - attackingUnitValue;
    }
    final List<Unit> defendingTransportedUnits = Match.getMatches(defendingUnits, Matches.unitIsBeingTransported());
    if (t.isWater() && !defendingTransportedUnits.isEmpty()) // Add TUV swing for transported units
    {
      final double transportedUnitValue = BattleCalculator.getTUV(defendingTransportedUnits, ProData.unitValueMap);
      TUVswing += transportedUnitValue * winPercentage / 100;
    }

    // Create battle result object
    final List<Territory> tList = new ArrayList<Territory>();
    tList.add(t);
    if (Match.allMatch(tList, Matches.TerritoryIsLand)) {
      return new ProBattleResult(winPercentage, TUVswing,
          Match.someMatch(averageAttackersRemaining, Matches.UnitIsLand), averageAttackersRemaining,
          averageDefendersRemaining, results.getAverageBattleRoundsFought());
    } else {
      return new ProBattleResult(winPercentage, TUVswing, !averageAttackersRemaining.isEmpty(),
          averageAttackersRemaining, averageDefendersRemaining, results.getAverageBattleRoundsFought());
    }
  }

  public static boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerID player) {
    return territoryHasLocalLandSuperiority(t, distance, player, new HashMap<Territory, ProPurchaseTerritory>());
  }

  public static boolean territoryHasLocalLandSuperiority(final Territory t, final int distance, final PlayerID player,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    final GameData data = ProData.getData();
    if (t == null) {
      return true;
    }

    for (int i = 2; i <= distance; i++) {

      // Find enemy strength
      final Set<Territory> nearbyTerritoriesForEnemy =
          data.getMap().getNeighbors(t, i, ProMatches.territoryCanMoveLandUnits(player, data, false));
      nearbyTerritoriesForEnemy.add(t);
      final List<Unit> enemyUnits = new ArrayList<Unit>();
      for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy) {
        enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutral(player, data)));
      }

      // Find allied strength
      final Set<Territory> nearbyTerritoriesForAllied =
          data.getMap().getNeighbors(t, i - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
      nearbyTerritoriesForAllied.add(t);
      final List<Unit> alliedUnits = new ArrayList<Unit>();
      for (final Territory nearbyTerritory : nearbyTerritoriesForAllied) {
        alliedUnits.addAll(nearbyTerritory.getUnits().getMatches(Matches.isUnitAllied(player, data)));
      }
      for (final Territory purchaseTerritory : purchaseTerritories.keySet()) {
        for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories()) {
          if (nearbyTerritoriesForAllied.contains(ppt.getTerritory())) {
            alliedUnits.addAll(ppt.getPlaceUnits());
          }
        }
      }

      // Determine strength difference
      final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
      ProLogger.trace(t + ", current enemy land strengthDifference=" + strengthDifference + ", distance=" + i
          + ", enemySize=" + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
      if (strengthDifference > 50) {
        return false;
      }
    }
    return true;
  }

  public static boolean territoryHasLocalLandSuperiorityAfterMoves(final Territory t, final int distance,
      final PlayerID player, final Map<Territory, ProTerritory> moveMap) {
    final GameData data = ProData.getData();

    // Find enemy strength
    final Set<Territory> nearbyTerritoriesForEnemy =
        data.getMap().getNeighbors(t, distance, ProMatches.territoryCanMoveLandUnits(player, data, false));
    nearbyTerritoriesForEnemy.add(t);
    final List<Unit> enemyUnits = new ArrayList<Unit>();
    for (final Territory nearbyTerritory : nearbyTerritoriesForEnemy) {
      enemyUnits.addAll(nearbyTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotNeutral(player, data)));
    }

    // Find allied strength
    final Set<Territory> nearbyTerritoriesForAllied =
        data.getMap().getNeighbors(t, distance - 1, ProMatches.territoryCanMoveLandUnits(player, data, false));
    nearbyTerritoriesForAllied.add(t);
    final List<Unit> alliedUnits = new ArrayList<Unit>();
    for (final Territory nearbyTerritory : nearbyTerritoriesForAllied) {
      if (moveMap.get(nearbyTerritory) != null) {
        alliedUnits.addAll(moveMap.get(nearbyTerritory).getAllDefenders());
      }
    }

    // Determine strength difference
    final double strengthDifference = estimateStrengthDifference(t, enemyUnits, alliedUnits);
    ProLogger.trace(t + ", current enemy land strengthDifference=" + strengthDifference + ", enemySize="
        + enemyUnits.size() + ", alliedSize=" + alliedUnits.size());
    if (strengthDifference > 50) {
      return false;
    } else {
      return true;
    }
  }

  public static boolean territoryHasLocalNavalSuperiority(final Territory t, final PlayerID player,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final List<Unit> unitsToPlace) {
    final GameData data = ProData.getData();

    int landDistance = ProUtils.getClosestEnemyLandTerritoryDistanceOverWater(data, player, t);
    if (landDistance <= 0) {
      landDistance = 10;
    }
    final int enemyDistance = Math.max(3, (landDistance + 1));
    final int alliedDistance = (enemyDistance + 1) / 2;
    final Set<Territory> nearbyTerritories = data.getMap().getNeighbors(t, enemyDistance);
    final List<Territory> nearbyLandTerritories = Match.getMatches(nearbyTerritories, Matches.TerritoryIsLand);
    final Set<Territory> nearbyEnemySeaTerritories =
        data.getMap().getNeighbors(t, enemyDistance, Matches.TerritoryIsWater);
    nearbyEnemySeaTerritories.add(t);
    final Set<Territory> nearbyAlliedSeaTerritories =
        data.getMap().getNeighbors(t, alliedDistance, Matches.TerritoryIsWater);
    nearbyAlliedSeaTerritories.add(t);
    final List<Unit> enemyUnitsInSeaTerritories = new ArrayList<Unit>();
    final List<Unit> enemyUnitsInLandTerritories = new ArrayList<Unit>();
    final List<Unit> myUnitsInSeaTerritories = new ArrayList<Unit>();
    final List<Unit> alliedUnitsInSeaTerritories = new ArrayList<Unit>();
    for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
      enemyUnitsInLandTerritories.addAll(nearbyLandTerritory.getUnits().getMatches(
          ProMatches.unitIsEnemyAir(player, data)));
    }
    for (final Territory nearbySeaTerritory : nearbyEnemySeaTerritories) {
      final List<Unit> enemySeaUnits =
          nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotLand(player, data));
      if (enemySeaUnits.isEmpty()) {
        continue;
      }
      final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbySeaTerritory, Matches.TerritoryIsWater);
      if (route == null) {
        continue;
      }
      if (MoveValidator.validateCanal(route, enemySeaUnits, enemySeaUnits.get(0).getOwner(), data) != null) {
        continue;
      }
      final int routeLength = route.numberOfSteps();
      if (routeLength <= enemyDistance) {
        enemyUnitsInSeaTerritories.addAll(enemySeaUnits);
      }
    }
    for (final Territory nearbySeaTerritory : nearbyAlliedSeaTerritories) {
      myUnitsInSeaTerritories.addAll(nearbySeaTerritory.getUnits().getMatches(
          ProMatches.unitIsOwnedNotLand(player, data)));
      myUnitsInSeaTerritories.addAll(ProPurchaseUtils.getPlaceUnits(nearbySeaTerritory, purchaseTerritories));
      alliedUnitsInSeaTerritories.addAll(nearbySeaTerritory.getUnits().getMatches(
          ProMatches.unitIsAlliedNotOwned(player, data)));
    }
    ProLogger.trace(t + ", enemyDistance=" + enemyDistance + ", alliedDistance=" + alliedDistance + ", enemyAirUnits="
        + enemyUnitsInLandTerritories + ", enemySeaUnits=" + enemyUnitsInSeaTerritories + ", mySeaUnits="
        + myUnitsInSeaTerritories);

    // Find current naval defense strength
    final List<Unit> myUnits = new ArrayList<Unit>(myUnitsInSeaTerritories);
    myUnits.addAll(unitsToPlace);
    myUnits.addAll(alliedUnitsInSeaTerritories);
    final List<Unit> enemyAttackers = new ArrayList<Unit>(enemyUnitsInSeaTerritories);
    enemyAttackers.addAll(enemyUnitsInLandTerritories);
    final double defenseStrengthDifference = estimateStrengthDifference(t, enemyAttackers, myUnits);
    ProLogger.trace(t + ", current enemy naval attack strengthDifference=" + defenseStrengthDifference + ", enemySize="
        + enemyAttackers.size() + ", alliedSize=" + myUnits.size());

    // Find current naval attack strength
    double attackStrengthDifference = estimateStrengthDifference(t, myUnits, enemyUnitsInSeaTerritories);
    attackStrengthDifference +=
        0.5 * estimateStrengthDifference(t, alliedUnitsInSeaTerritories, enemyUnitsInSeaTerritories);
    ProLogger.trace(t + ", current allied naval attack strengthDifference=" + attackStrengthDifference
        + ", alliedSize=" + myUnits.size() + ", enemySize=" + enemyUnitsInSeaTerritories.size());

    // If I have naval attack/defense superiority then break
    return (defenseStrengthDifference < 50 && attackStrengthDifference > 50);
  }

}
