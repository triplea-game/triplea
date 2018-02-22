package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.ai.proAI.data.ProOtherMoveOptions;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOption;
import games.strategy.triplea.ai.proAI.data.ProTerritory;
import games.strategy.triplea.ai.proAI.data.ProTerritoryManager;
import games.strategy.triplea.ai.proAI.data.ProTransport;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProMoveUtils;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProSortMoveOptionsUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.CollectionUtils;

/**
 * Pro combat move AI.
 */
class ProCombatMoveAi {

  private static final int MIN_BOMBING_SCORE = 4; // Avoid bombing low production factories with AA

  private final ProAi ai;
  private final ProOddsCalculator calc;
  private GameData data;
  private PlayerID player;
  private ProTerritoryManager territoryManager;
  private boolean isDefensive;
  private boolean isBombing;

  ProCombatMoveAi(final ProAi ai) {
    this.ai = ai;
    calc = ai.getCalc();
  }

  Map<Territory, ProTerritory> doCombatMove(final IMoveDelegate moveDel) {
    ProLogger.info("Starting combat move phase");

    // Current data at the start of combat move
    data = ProData.getData();
    player = ProData.getPlayer();
    territoryManager = new ProTerritoryManager(calc);

    // Determine whether capital is threatened and I should be in a defensive stance
    isDefensive =
        !ProBattleUtils.territoryHasLocalLandSuperiority(ProData.myCapital, ProBattleUtils.MEDIUM_RANGE, player);
    isBombing = false;
    ProLogger.debug("Currently in defensive stance: " + isDefensive);

    // Find the maximum number of units that can attack each territory and max enemy defenders
    territoryManager.populateAttackOptions();
    territoryManager.populateEnemyDefenseOptions();

    // Remove territories that aren't worth attacking and prioritize the remaining ones
    final List<ProTerritory> attackOptions = territoryManager.removeTerritoriesThatCantBeConquered();
    List<Territory> clearedTerritories = new ArrayList<>();
    for (final ProTerritory patd : attackOptions) {
      clearedTerritories.add(patd.getTerritory());
    }
    territoryManager.populateEnemyAttackOptions(clearedTerritories, new ArrayList<>());
    Set<Territory> territoriesToCheck = new HashSet<>(clearedTerritories);
    territoriesToCheck.addAll(ProData.myUnitTerritories);
    Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(player, new ArrayList<>(), clearedTerritories, territoriesToCheck);
    determineTerritoriesThatCanBeHeld(attackOptions, territoryValueMap);
    prioritizeAttackOptions(player, attackOptions);
    removeTerritoriesThatArentWorthAttacking(attackOptions);

    // Determine which territories to attack
    determineTerritoriesToAttack(attackOptions);

    // Determine which territories can be held and remove any that aren't worth attacking
    clearedTerritories = new ArrayList<>();
    final Set<Territory> possibleTransportTerritories = new HashSet<>();
    for (final ProTerritory patd : attackOptions) {
      clearedTerritories.add(patd.getTerritory());
      if (!patd.getAmphibAttackMap().isEmpty()) {
        possibleTransportTerritories
            .addAll(data.getMap().getNeighbors(patd.getTerritory(), Matches.territoryIsWater()));
      }
    }
    territoryManager.populateEnemyAttackOptions(clearedTerritories, new ArrayList<>(possibleTransportTerritories));
    territoriesToCheck = new HashSet<>(clearedTerritories);
    territoriesToCheck.addAll(ProData.myUnitTerritories);
    territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(player, new ArrayList<>(), clearedTerritories, territoriesToCheck);
    determineTerritoriesThatCanBeHeld(attackOptions, territoryValueMap);
    removeTerritoriesThatArentWorthAttacking(attackOptions);

    // Determine how many units to attack each territory with
    final List<Unit> alreadyMovedUnits = moveOneDefenderToLandTerritoriesBorderingEnemy(attackOptions);
    determineUnitsToAttackWith(attackOptions, alreadyMovedUnits);

    // Get all transport final territories
    ProMoveUtils.calculateAmphibRoutes(player, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
        territoryManager.getAttackOptions().getTerritoryMap(), true);

    // Determine max enemy counter attack units and remove territories where transports are exposed
    removeTerritoriesWhereTransportsAreExposed();

    // Determine if capital can be held if I still own it
    if (ProData.myCapital != null && ProData.myCapital.getOwner().equals(player)) {
      removeAttacksUntilCapitalCanBeHeld(attackOptions, ProData.purchaseOptions.getLandOptions());
    }

    // Check if any subs in contested territory that's not being attacked
    checkContestedSeaTerritories();

    // Calculate attack routes and perform moves
    doMove(territoryManager.getAttackOptions().getTerritoryMap(), moveDel, data, player);

    // Set strafing territories to avoid retreats
    ai.setStoredStrafingTerritories(territoryManager.getStrafingTerritories());
    ProLogger.info("Strafing territories: " + territoryManager.getStrafingTerritories());

    // Log results
    ProLogger.info("Logging results");
    logAttackMoves(attackOptions);

    return territoryManager.getAttackOptions().getTerritoryMap();
  }

  void doMove(final Map<Territory, ProTerritory> attackMap, final IMoveDelegate moveDel, final GameData data,
      final PlayerID player) {
    this.data = data;
    this.player = player;

    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    ProMoveUtils.calculateMoveRoutes(player, moveUnits, moveRoutes, attackMap, true);
    ProMoveUtils.doMove(moveUnits, moveRoutes, moveDel);

    moveUnits.clear();
    moveRoutes.clear();
    final List<Collection<Unit>> transportsToLoad = new ArrayList<>();
    ProMoveUtils.calculateAmphibRoutes(player, moveUnits, moveRoutes, transportsToLoad, attackMap, true);
    ProMoveUtils.doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);

    moveUnits.clear();
    moveRoutes.clear();
    ProMoveUtils.calculateBombardMoveRoutes(player, moveUnits, moveRoutes, attackMap);
    ProMoveUtils.doMove(moveUnits, moveRoutes, moveDel);

    moveUnits.clear();
    moveRoutes.clear();
    isBombing = true;
    ProMoveUtils.calculateBombingRoutes(player, moveUnits, moveRoutes, attackMap);
    ProMoveUtils.doMove(moveUnits, moveRoutes, moveDel);
    isBombing = false;
  }

  boolean isBombing() {
    return isBombing;
  }

  private List<ProTerritory> prioritizeAttackOptions(final PlayerID player, final List<ProTerritory> attackOptions) {

    ProLogger.info("Prioritizing territories to try to attack");

    // Calculate value of attacking territory
    for (final Iterator<ProTerritory> it = attackOptions.iterator(); it.hasNext();) {
      final ProTerritory patd = it.next();
      final Territory t = patd.getTerritory();

      // Determine territory attack properties
      final int isLand = !t.isWater() ? 1 : 0;
      final int isNeutral = (!t.isWater() && t.getOwner().isNull()) ? 1 : 0;
      final int isCanHold = patd.isCanHold() ? 1 : 0;
      final int isAmphib = patd.isNeedAmphibUnits() ? 1 : 0;
      final List<Unit> defendingUnits = CollectionUtils.getMatches(patd.getMaxEnemyDefenders(player, data),
          ProMatches.unitIsEnemyAndNotInfa(player, data));
      final int isEmptyLand = (defendingUnits.isEmpty() && !patd.isNeedAmphibUnits()) ? 1 : 0;
      final boolean isAdjacentToMyCapital =
          !data.getMap().getNeighbors(t, Matches.territoryIs(ProData.myCapital)).isEmpty();
      final int isNotNeutralAdjacentToMyCapital =
          (isAdjacentToMyCapital && ProMatches.territoryIsEnemyNotNeutralLand(player, data).test(t)) ? 1 : 0;
      final int isFactory = ProMatches.territoryHasInfraFactoryAndIsLand().test(t) ? 1 : 0;
      final int isFfa = ProUtils.isFfa(data, player) ? 1 : 0;

      // Determine production value and if it is an enemy capital
      int production = 0;
      int isEnemyCapital = 0;
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta != null) {
        production = ta.getProduction();
        if (ta.isCapital()) {
          isEnemyCapital = 1;
        }
      }

      // Calculate attack value for prioritization
      double tuvSwing = patd.getMaxBattleResult().getTuvSwing();
      if (isFfa == 1 && tuvSwing > 0) {
        tuvSwing *= 0.5;
      }
      final double territoryValue = (1 + isLand + isCanHold * (1 + 2 * isFfa)) * (1 + isEmptyLand) * (1 + isFactory)
          * (1 - 0.5 * isAmphib) * production;
      double attackValue = (tuvSwing + territoryValue) * (1 + 4 * isEnemyCapital)
          * (1 + 2 * isNotNeutralAdjacentToMyCapital) * (1 - 0.9 * isNeutral);

      // Check if a negative value neutral territory should be attacked
      if (attackValue <= 0 && !patd.isNeedAmphibUnits() && !t.isWater() && t.getOwner().isNull()) {

        // Determine enemy neighbor territory production value for neutral land territories
        double nearbyEnemyValue = 0;
        final List<Territory> cantReachEnemyTerritories = new ArrayList<>();
        final Set<Territory> nearbyTerritories =
            data.getMap().getNeighbors(t, ProMatches.territoryCanMoveLandUnits(player, data, true));
        final List<Territory> nearbyEnemyTerritories =
            CollectionUtils.getMatches(nearbyTerritories, Matches.isTerritoryEnemy(player, data));
        final List<Territory> nearbyTerritoriesWithOwnedUnits =
            CollectionUtils.getMatches(nearbyTerritories, Matches.territoryHasUnitsOwnedBy(player));
        for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories) {
          boolean allAlliedNeighborsHaveRoute = true;
          for (final Territory nearbyAlliedTerritory : nearbyTerritoriesWithOwnedUnits) {
            final int distance = data.getMap().getDistance_IgnoreEndForCondition(nearbyAlliedTerritory,
                nearbyEnemyTerritory, ProMatches.territoryIsEnemyNotNeutralOrAllied(player, data));
            if (distance < 0 || distance > 2) {
              allAlliedNeighborsHaveRoute = false;
              break;
            }
          }
          if (!allAlliedNeighborsHaveRoute) {
            final double value = ProTerritoryValueUtils.findTerritoryAttackValue(player, nearbyEnemyTerritory);
            if (value > 0) {
              nearbyEnemyValue += value;
            }
            cantReachEnemyTerritories.add(nearbyEnemyTerritory);
          }
        }
        ProLogger.debug(
            t.getName() + " calculated nearby enemy value=" + nearbyEnemyValue + " from " + cantReachEnemyTerritories);
        if (nearbyEnemyValue > 0) {
          ProLogger.trace(t.getName() + " updating negative neutral attack value=" + attackValue);
          attackValue = nearbyEnemyValue * .001 / (1 - attackValue);
        } else {

          // Check if overwhelming attack strength (more than 5 times)
          final double strengthDifference =
              ProBattleUtils.estimateStrengthDifference(t, patd.getMaxUnits(), patd.getMaxEnemyDefenders(player, data));
          ProLogger.debug(t.getName() + " calculated strengthDifference=" + strengthDifference);
          if (strengthDifference > 500) {
            ProLogger.trace(t.getName() + " updating negative neutral attack value=" + attackValue);
            attackValue = strengthDifference * .00001 / (1 - attackValue);
          }
        }
      }

      // Remove negative value territories
      patd.setValue(attackValue);
      if (attackValue <= 0
          || (isDefensive && attackValue <= 8 && data.getMap().getDistance(ProData.myCapital, t) <= 3)) {
        ProLogger.debug(
            "Removing territory that has a negative attack value: " + t.getName() + ", AttackValue=" + patd.getValue());
        it.remove();
      }
    }

    // Sort attack territories by value
    Collections.sort(attackOptions, (t1, t2) -> {
      final double value1 = t1.getValue();
      final double value2 = t2.getValue();
      return Double.compare(value2, value1);
    });

    // Log prioritized territories
    for (final ProTerritory patd : attackOptions) {
      ProLogger.debug("AttackValue=" + patd.getValue() + ", TUVSwing=" + patd.getMaxBattleResult().getTuvSwing()
          + ", isAmphib=" + patd.isNeedAmphibUnits() + ", " + patd.getTerritory().getName());
    }
    return attackOptions;
  }

  private void determineTerritoriesToAttack(final List<ProTerritory> prioritizedTerritories) {

    ProLogger.info("Determine which territories to attack");

    // Assign units to territories by prioritization
    int numToAttack = Math.min(1, prioritizedTerritories.size());
    boolean haveRemovedAllAmphibTerritories = false;
    while (true) {
      final List<ProTerritory> territoriesToTryToAttack = prioritizedTerritories.subList(0, numToAttack);
      ProLogger.debug("Current number of territories: " + numToAttack);
      tryToAttackTerritories(territoriesToTryToAttack, new ArrayList<>());

      // Determine if all attacks are successful
      boolean areSuccessful = true;
      for (final ProTerritory patd : territoriesToTryToAttack) {
        final Territory t = patd.getTerritory();
        if (patd.getBattleResult() == null) {
          patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
              patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
        }
        ProLogger.trace(patd.getResultString() + " with attackers: " + patd.getUnits());
        final double estimate =
            ProBattleUtils.estimateStrengthDifference(t, patd.getUnits(), patd.getMaxEnemyDefenders(player, data));
        final ProBattleResult result = patd.getBattleResult();
        if (!patd.isStrafing() && estimate < patd.getStrengthEstimate()
            && (result.getWinPercentage() < ProData.minWinPercentage || !result.isHasLandUnitRemaining())) {
          areSuccessful = false;
        }
      }

      // Determine whether to try more territories, remove a territory, or end
      if (areSuccessful) {
        for (final ProTerritory patd : territoriesToTryToAttack) {
          patd.setCanAttack(true);
          final double estimate = ProBattleUtils.estimateStrengthDifference(patd.getTerritory(), patd.getUnits(),
              patd.getMaxEnemyDefenders(player, data));
          if (estimate < patd.getStrengthEstimate()) {
            patd.setStrengthEstimate(estimate);
          }
        }

        // If already used all transports then remove any remaining amphib territories
        if (!haveRemovedAllAmphibTerritories) {
          if (territoryManager.haveUsedAllAttackTransports()) {
            final List<ProTerritory> amphibTerritoriesToRemove = new ArrayList<>();
            for (int i = numToAttack; i < prioritizedTerritories.size(); i++) {
              if (prioritizedTerritories.get(i).isNeedAmphibUnits()) {
                amphibTerritoriesToRemove.add(prioritizedTerritories.get(i));
                ProLogger.debug("Removing amphib territory since already used all transports: "
                    + prioritizedTerritories.get(i).getTerritory().getName());
              }
            }
            prioritizedTerritories.removeAll(amphibTerritoriesToRemove);
            haveRemovedAllAmphibTerritories = true;
          }
        }

        // Can attack all territories in list so end
        numToAttack++;
        if (numToAttack > prioritizedTerritories.size()) {
          break;
        }
      } else {
        ProLogger.debug("Removing territory: " + prioritizedTerritories.get(numToAttack - 1).getTerritory().getName());
        prioritizedTerritories.remove(numToAttack - 1);
        if (numToAttack > prioritizedTerritories.size()) {
          numToAttack--;
        }
      }
    }
    ProLogger.debug("Final number of territories: " + (numToAttack - 1));
  }

  private void determineTerritoriesThatCanBeHeld(final List<ProTerritory> prioritizedTerritories,
      final Map<Territory, Double> territoryValueMap) {

    ProLogger.info("Check if we should try to hold attack territories");

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();

    // Determine which territories to try and hold
    for (final ProTerritory patd : prioritizedTerritories) {
      final Territory t = patd.getTerritory();

      // If strafing then can't hold
      if (patd.isStrafing()) {
        patd.setCanHold(false);
        ProLogger.debug(t + ", strafing so CanHold=false");
        continue;
      }

      // Set max enemy attackers
      if (enemyAttackOptions.getMax(t) != null) {
        final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        patd.setMaxEnemyUnits(new ArrayList<>(enemyAttackingUnits));
        patd.setMaxEnemyBombardUnits(enemyAttackOptions.getMax(t).getMaxBombardUnits());
      }

      // Add strategic value for factories
      int isFactory = 0;
      if (ProMatches.territoryHasInfraFactoryAndIsLand().test(t)) {
        isFactory = 1;
      }

      // Determine whether its worth trying to hold territory
      double totalValue = 0.0;
      final List<Unit> nonAirAttackers = CollectionUtils.getMatches(patd.getMaxUnits(), Matches.unitIsNotAir());
      for (final Unit u : nonAirAttackers) {
        totalValue += territoryValueMap.get(ProData.unitTerritoryMap.get(u));
      }
      final double averageValue = totalValue / nonAirAttackers.size() * 0.75;
      final double territoryValue = territoryValueMap.get(t) * (1 + 4 * isFactory);
      if (!t.isWater() && territoryValue < averageValue) {
        attackMap.get(t).setCanHold(false);
        ProLogger.debug(
            t + ", CanHold=false, value=" + territoryValueMap.get(t) + ", averageAttackFromValue=" + averageValue);
        continue;
      }
      if (enemyAttackOptions.getMax(t) != null) {

        // Find max remaining defenders
        final Set<Unit> attackingUnits = new HashSet<>(patd.getMaxUnits());
        attackingUnits.addAll(patd.getMaxAmphibUnits());
        final ProBattleResult result = calc.estimateAttackBattleResults(t, new ArrayList<>(attackingUnits),
            patd.getMaxEnemyDefenders(player, data), patd.getMaxBombardUnits());
        final List<Unit> remainingUnitsToDefendWith =
            CollectionUtils.getMatches(result.getAverageAttackersRemaining(), Matches.unitIsAir().negate());
        ProLogger.debug(t + ", value=" + territoryValueMap.get(t) + ", averageAttackFromValue=" + averageValue
            + ", MyAttackers=" + attackingUnits.size() + ", RemainingUnits=" + remainingUnitsToDefendWith.size());

        // Determine counter attack results to see if I can hold it
        final ProBattleResult result2 = calc.calculateBattleResults(t, patd.getMaxEnemyUnits(),
            remainingUnitsToDefendWith, enemyAttackOptions.getMax(t).getMaxBombardUnits());
        final boolean canHold = (!result2.isHasLandUnitRemaining() && !t.isWater()) || (result2.getTuvSwing() < 0)
            || (result2.getWinPercentage() < ProData.minWinPercentage);
        patd.setCanHold(canHold);
        ProLogger.debug(
            t + ", CanHold=" + canHold + ", MyDefenders=" + remainingUnitsToDefendWith.size() + ", EnemyAttackers="
                + patd.getMaxEnemyUnits().size() + ", win%=" + result2.getWinPercentage() + ", EnemyTUVSwing="
                + result2.getTuvSwing() + ", hasLandUnitRemaining=" + result2.isHasLandUnitRemaining());
      } else {
        attackMap.get(t).setCanHold(true);
        ProLogger.debug(t + ", CanHold=true since no enemy counter attackers, value=" + territoryValueMap.get(t)
            + ", averageAttackFromValue=" + averageValue);
      }
    }
  }

  private void removeTerritoriesThatArentWorthAttacking(final List<ProTerritory> prioritizedTerritories) {
    ProLogger.info("Remove territories that aren't worth attacking");

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through all prioritized territories
    for (final Iterator<ProTerritory> it = prioritizedTerritories.iterator(); it.hasNext();) {
      final ProTerritory patd = it.next();
      final Territory t = patd.getTerritory();
      ProLogger
          .debug("Checking territory=" + patd.getTerritory().getName() + " with isAmphib=" + patd.isNeedAmphibUnits());

      // Remove empty convoy zones that can't be held
      if (!patd.isCanHold() && enemyAttackOptions.getMax(t) != null && t.isWater()
          && !t.getUnits().anyMatch(Matches.enemyUnit(player, data))) {
        ProLogger.debug("Removing convoy zone that can't be held: " + t.getName() + ", enemyAttackers="
            + enemyAttackOptions.getMax(t).getMaxUnits());
        it.remove();
        continue;
      }

      // Remove neutral and low value amphib land territories that can't be held
      final boolean isNeutral = t.getOwner().isNull();
      final double strengthDifference =
          ProBattleUtils.estimateStrengthDifference(t, patd.getMaxUnits(), patd.getMaxEnemyDefenders(player, data));
      if (!patd.isCanHold() && enemyAttackOptions.getMax(t) != null && !t.isWater()) {
        if (isNeutral && strengthDifference <= 500) {

          // Remove neutral territories that can't be held and don't have overwhelming attack strength
          ProLogger.debug("Removing neutral territory that can't be held: " + t.getName() + ", enemyAttackers="
              + enemyAttackOptions.getMax(t).getMaxUnits() + ", enemyAmphibAttackers="
              + enemyAttackOptions.getMax(t).getMaxAmphibUnits() + ", strengthDifference=" + strengthDifference);
          it.remove();
          continue;
        } else if (patd.isNeedAmphibUnits() && patd.getValue() < 2) {

          // Remove amphib territories that aren't worth attacking
          ProLogger.debug("Removing low value amphib territory that can't be held: " + t.getName() + ", enemyAttackers="
              + enemyAttackOptions.getMax(t).getMaxUnits() + ", enemyAmphibAttackers="
              + enemyAttackOptions.getMax(t).getMaxAmphibUnits());
          it.remove();
          continue;
        }
      }
      // Remove neutral territories where attackers are adjacent to enemy territories that aren't being attacked
      if (isNeutral && !t.isWater() && strengthDifference <= 500) {

        // Get list of territories I'm attacking
        final List<Territory> prioritizedTerritoryList = new ArrayList<>();
        for (final ProTerritory prioritizedTerritory : prioritizedTerritories) {
          prioritizedTerritoryList.add(prioritizedTerritory.getTerritory());
        }

        // Find all territories units are attacking from that are adjacent to territory
        final Set<Territory> attackFromTerritories = new HashSet<>();
        for (final Unit u : patd.getMaxUnits()) {
          attackFromTerritories.add(ProData.unitTerritoryMap.get(u));
        }
        attackFromTerritories.retainAll(data.getMap().getNeighbors(t));

        // Determine if any of the attacking from territories has enemy neighbors that aren't being attacked
        boolean attackersHaveEnemyNeighbors = false;
        Territory attackFromTerritoryWithEnemyNeighbors = null;
        for (final Territory attackFromTerritory : attackFromTerritories) {
          final Set<Territory> enemyNeighbors =
              data.getMap().getNeighbors(attackFromTerritory, ProMatches.territoryIsEnemyNotNeutralLand(player, data));
          if (!prioritizedTerritoryList.containsAll(enemyNeighbors)) {
            attackersHaveEnemyNeighbors = true;
            attackFromTerritoryWithEnemyNeighbors = attackFromTerritory;
            break;
          }
        }
        if (attackersHaveEnemyNeighbors) {
          ProLogger.debug("Removing neutral territory that has attackers that are adjacent to enemies: " + t.getName()
              + ", attackFromTerritory=" + attackFromTerritoryWithEnemyNeighbors);
          it.remove();
        }
      }
    }
  }

  private List<Unit> moveOneDefenderToLandTerritoriesBorderingEnemy(final List<ProTerritory> prioritizedTerritories) {

    ProLogger.info("Determine which territories to defend with one land unit");

    final Map<Unit, Set<Territory>> unitMoveMap = territoryManager.getAttackOptions().getUnitMoveMap();

    // Get list of territories to attack
    final List<Territory> territoriesToAttack = new ArrayList<>();
    for (final ProTerritory patd : prioritizedTerritories) {
      territoriesToAttack.add(patd.getTerritory());
    }

    // Find land territories with no can't move units and adjacent to enemy land units
    final List<Unit> alreadyMovedUnits = new ArrayList<>();
    for (final Territory t : ProData.myUnitTerritories) {
      final boolean hasAlliedLandUnits =
          t.getUnits().anyMatch(ProMatches.unitCantBeMovedAndIsAlliedDefenderAndNotInfra(player, data, t));
      final Set<Territory> enemyNeighbors = data.getMap().getNeighbors(t,
          Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.unitIsLand()));
      enemyNeighbors.removeAll(territoriesToAttack);
      if (!t.isWater() && !hasAlliedLandUnits && !enemyNeighbors.isEmpty()) {
        int minCost = Integer.MAX_VALUE;
        Unit minUnit = null;
        for (final Unit u : t.getUnits().getMatches(Matches.unitIsOwnedBy(player))) {
          if (ProData.unitValueMap.getInt(u.getType()) < minCost) {
            minCost = ProData.unitValueMap.getInt(u.getType());
            minUnit = u;
          }
        }
        if (minUnit != null) {
          unitMoveMap.remove(minUnit);
          alreadyMovedUnits.add(minUnit);
          ProLogger.debug(t + ", added one land unit: " + minUnit);
        }
      }
    }
    return alreadyMovedUnits;
  }

  private void removeTerritoriesWhereTransportsAreExposed() {

    ProLogger.info("Remove territories where transports are exposed");

    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();
    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Find maximum defenders for each transport territory
    final List<Territory> clearedTerritories = attackMap.entrySet().stream()
        .filter(e -> !e.getValue().getUnits().isEmpty())
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
    territoryManager.populateDefenseOptions(clearedTerritories);
    final Map<Territory, ProTerritory> defendMap = territoryManager.getDefendOptions().getTerritoryMap();

    // Remove units that have already attacked
    final Set<Unit> alreadyAttackedWithUnits = new HashSet<>();
    for (final ProTerritory t : attackMap.values()) {
      alreadyAttackedWithUnits.addAll(t.getUnits());
      alreadyAttackedWithUnits.addAll(t.getAmphibAttackMap().keySet());
    }
    for (final ProTerritory t : defendMap.values()) {
      t.getMaxUnits().removeAll(alreadyAttackedWithUnits);
    }

    // Loop through all prioritized territories
    for (final Territory t : attackMap.keySet()) {
      final ProTerritory patd = attackMap.get(t);
      ProLogger.debug("Checking territory=" + patd.getTerritory().getName() + " with tranports size="
          + patd.getTransportTerritoryMap().size());
      if (!patd.getTerritory().isWater() && !patd.getTransportTerritoryMap().isEmpty()) {

        // Find all transports for each unload territory
        final Map<Territory, List<Unit>> territoryTransportAndBombardMap = new HashMap<>();
        for (final Unit u : patd.getTransportTerritoryMap().keySet()) {
          final Territory unloadTerritory = patd.getTransportTerritoryMap().get(u);
          if (territoryTransportAndBombardMap.containsKey(unloadTerritory)) {
            territoryTransportAndBombardMap.get(unloadTerritory).add(u);
          } else {
            final List<Unit> transports = new ArrayList<>();
            transports.add(u);
            territoryTransportAndBombardMap.put(unloadTerritory, transports);
          }
        }

        // Find all bombard units for each unload territory
        for (final Unit u : patd.getBombardTerritoryMap().keySet()) {
          final Territory unloadTerritory = patd.getBombardTerritoryMap().get(u);
          if (territoryTransportAndBombardMap.containsKey(unloadTerritory)) {
            territoryTransportAndBombardMap.get(unloadTerritory).add(u);
          } else {
            final List<Unit> transports = new ArrayList<>();
            transports.add(u);
            territoryTransportAndBombardMap.put(unloadTerritory, transports);
          }
        }

        // Determine counter attack results for each transport territory
        double enemyTuvSwing = 0.0;
        for (final Territory unloadTerritory : territoryTransportAndBombardMap.keySet()) {
          if (enemyAttackOptions.getMax(unloadTerritory) != null) {
            final List<Unit> enemyAttackers = enemyAttackOptions.getMax(unloadTerritory).getMaxUnits();
            final Set<Unit> defenders =
                new HashSet<>(unloadTerritory.getUnits().getMatches(ProMatches.unitIsAlliedNotOwned(player, data)));
            defenders.addAll(territoryTransportAndBombardMap.get(unloadTerritory));
            if (defendMap.get(unloadTerritory) != null) {
              defenders.addAll(defendMap.get(unloadTerritory).getMaxUnits());
            }
            final ProBattleResult result = calc.calculateBattleResults(unloadTerritory,
                enemyAttackOptions.getMax(unloadTerritory).getMaxUnits(), new ArrayList<>(defenders), new HashSet<>());
            final ProBattleResult minResult = calc.calculateBattleResults(unloadTerritory,
                enemyAttackOptions.getMax(unloadTerritory).getMaxUnits(),
                territoryTransportAndBombardMap.get(unloadTerritory), new HashSet<>());
            final double minTuvSwing = Math.min(result.getTuvSwing(), minResult.getTuvSwing());
            if (minTuvSwing > 0) {
              enemyTuvSwing += minTuvSwing;
            }
            ProLogger.trace(unloadTerritory + ", EnemyAttackers=" + enemyAttackers.size() + ", MaxDefenders="
                + defenders.size() + ", MaxEnemyTUVSwing=" + result.getTuvSwing() + ", MinDefenders="
                + territoryTransportAndBombardMap.get(unloadTerritory).size() + ", MinEnemyTUVSwing="
                + minResult.getTuvSwing());
          } else {
            ProLogger.trace("Territory=" + unloadTerritory.getName() + " has no enemy attackers");
          }
        }

        // Determine whether its worth attacking
        final ProBattleResult result = calc.calculateBattleResults(t, patd.getUnits(),
            patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet());
        int production = 0;
        int isEnemyCapital = 0;
        final TerritoryAttachment ta = TerritoryAttachment.get(t);
        if (ta != null) {
          production = ta.getProduction();
          if (ta.isCapital()) {
            isEnemyCapital = 1;
          }
        }
        final double attackValue = result.getTuvSwing() + production * (1 + 3 * isEnemyCapital);
        if (!patd.isStrafing() && (0.75 * enemyTuvSwing) > attackValue) {
          ProLogger.debug("Removing amphib territory: " + patd.getTerritory() + ", enemyTUVSwing="
              + enemyTuvSwing + ", attackValue=" + attackValue);
          attackMap.get(t).getUnits().clear();
          attackMap.get(t).getAmphibAttackMap().clear();
          attackMap.get(t).getBombardTerritoryMap().clear();
        } else {
          ProLogger.debug("Keeping amphib territory: " + patd.getTerritory() + ", enemyTUVSwing="
              + enemyTuvSwing + ", attackValue=" + attackValue);
        }
      }
    }
  }

  private void determineUnitsToAttackWith(final List<ProTerritory> prioritizedTerritories,
      final List<Unit> alreadyMovedUnits) {

    ProLogger.info("Determine units to attack each territory with");

    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();
    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    final Map<Unit, Set<Territory>> unitAttackMap = territoryManager.getAttackOptions().getUnitMoveMap();

    // Assign units to territories by prioritization
    while (true) {
      Map<Unit, Set<Territory>> sortedUnitAttackOptions =
          tryToAttackTerritories(prioritizedTerritories, alreadyMovedUnits);

      // Clear bombers
      for (final ProTerritory t : attackMap.values()) {
        t.getBombers().clear();
      }

      // Get all units that have already moved
      final Set<Unit> alreadyAttackedWithUnits = new HashSet<>();
      for (final ProTerritory t : attackMap.values()) {
        alreadyAttackedWithUnits.addAll(t.getUnits());
        alreadyAttackedWithUnits.addAll(t.getAmphibAttackMap().keySet());
      }

      // Check to see if any territories can be bombed
      final Map<Unit, Set<Territory>> bomberMoveMap = territoryManager.getAttackOptions().getBomberMoveMap();
      for (final Unit unit : bomberMoveMap.keySet()) {
        if (alreadyAttackedWithUnits.contains(unit)) {
          continue;
        }
        Optional<Territory> maxBombingTerritory = Optional.empty();
        int maxBombingScore = MIN_BOMBING_SCORE;
        for (final Territory t : bomberMoveMap.get(unit)) {
          final boolean territoryCanBeBombed = t.getUnits().anyMatch(Matches.unitCanProduceUnitsAndCanBeDamaged());
          if (territoryCanBeBombed && canAirSafelyLandAfterAttack(unit, t)) {
            final int noAaBombingDefense = t.getUnits().anyMatch(Matches.unitIsAaForBombingThisUnitOnly()) ? 0 : 1;
            int maxDamage = 0;
            final TerritoryAttachment ta = TerritoryAttachment.get(t);
            if (ta != null) {
              maxDamage = ta.getProduction();
            }
            final int numExistingBombers = attackMap.get(t).getBombers().size();
            final int remainingDamagePotential = maxDamage - 3 * numExistingBombers;
            final int bombingScore = (1 + 9 * noAaBombingDefense) * remainingDamagePotential;
            if (bombingScore >= maxBombingScore) {
              maxBombingScore = bombingScore;
              maxBombingTerritory = Optional.of(t);
            }
          }
        }
        if (maxBombingTerritory.isPresent()) {
          final Territory t = maxBombingTerritory.get();
          attackMap.get(t).getBombers().add(unit);
          sortedUnitAttackOptions.remove(unit);
          ProLogger.debug("Add bomber (" + unit + ") to " + t);
        }
      }

      // Re-sort attack options
      sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(player, sortedUnitAttackOptions,
          attackMap, ProData.unitTerritoryMap, calc);

      // Set air units in any territory with no AA (don't move planes to empty territories)
      for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
        final Unit unit = it.next();
        final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
        if (!isAirUnit) {
          continue; // skip non-air units
        }
        Territory minWinTerritory = null;
        double minWinPercentage = Double.MAX_VALUE;
        for (final Territory t : sortedUnitAttackOptions.get(unit)) {
          final ProTerritory patd = attackMap.get(t);

          // Check if air unit should avoid this territory due to no guaranteed safe landing location
          final boolean isEnemyFactory = ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player, data).test(t);
          if (!isEnemyFactory && !canAirSafelyLandAfterAttack(unit, t)) {
            continue;
          }
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            final List<Unit> attackingUnits = patd.getUnits();
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player, data);
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, attackingUnits, defendingUnits);
            final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
            if (!hasAa && !isOverwhelmingWin) {
              minWinPercentage = result.getWinPercentage();
              minWinTerritory = t;
            }
          }
        }
        if (minWinTerritory != null) {
          attackMap.get(minWinTerritory).addUnit(unit);
          attackMap.get(minWinTerritory).setBattleResult(null);
          it.remove();
        }
      }

      // Re-sort attack options
      sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(player, sortedUnitAttackOptions,
          attackMap, ProData.unitTerritoryMap, calc);

      // Find territory that we can try to hold that needs unit
      for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
        final Unit unit = it.next();
        Territory minWinTerritory = null;
        for (final Territory t : sortedUnitAttackOptions.get(unit)) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.isCanHold()) {

            // Check if I already have enough attack units to win in 2 rounds
            if (patd.getBattleResult() == null) {
              patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                  patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
            }
            final ProBattleResult result = patd.getBattleResult();
            final List<Unit> attackingUnits = patd.getUnits();
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player, data);
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, attackingUnits, defendingUnits);
            if (!isOverwhelmingWin && result.getBattleRounds() > 2) {
              minWinTerritory = t;
              break;
            }
          }
        }
        if (minWinTerritory != null) {
          attackMap.get(minWinTerritory).addUnit(unit);
          attackMap.get(minWinTerritory).setBattleResult(null);
          it.remove();
        }
      }

      // Re-sort attack options
      sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(player, sortedUnitAttackOptions,
          attackMap, ProData.unitTerritoryMap, calc);

      // Add sea units to any territory that significantly increases TUV gain
      for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
        final Unit unit = it.next();
        final boolean isSeaUnit = UnitAttachment.get(unit.getType()).getIsSea();
        if (!isSeaUnit) {
          continue; // skip non-sea units
        }
        for (final Territory t : sortedUnitAttackOptions.get(unit)) {
          final ProTerritory patd = attackMap.get(t);
          if (attackMap.get(t).getBattleResult() == null) {
            attackMap.get(t).setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          final ProBattleResult result = attackMap.get(t).getBattleResult();
          final List<Unit> attackers = new ArrayList<>(patd.getUnits());
          attackers.add(unit);
          final ProBattleResult result2 = calc.estimateAttackBattleResults(t, attackers,
              patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet());
          final double unitValue = ProData.unitValueMap.getInt(unit.getType());
          if ((result2.getTuvSwing() - unitValue / 3) > result.getTuvSwing()) {
            attackMap.get(t).addUnit(unit);
            attackMap.get(t).setBattleResult(null);
            it.remove();
            break;
          }
        }
      }

      // Determine if all attacks are worth it
      final List<Unit> usedUnits = new ArrayList<>();
      for (final ProTerritory patd : prioritizedTerritories) {
        usedUnits.addAll(patd.getUnits());
      }
      ProTerritory territoryToRemove = null;
      for (final ProTerritory patd : prioritizedTerritories) {
        final Territory t = patd.getTerritory();

        // Find battle result
        if (patd.getBattleResult() == null) {
          patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
              patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
        }
        final ProBattleResult result = patd.getBattleResult();

        // Determine enemy counter attack results
        boolean canHold = true;
        double enemyCounterTuvSwing = 0;
        if (enemyAttackOptions.getMax(t) != null
            && !ProMatches.territoryIsWaterAndAdjacentToOwnedFactory(player, data).test(t)) {
          List<Unit> remainingUnitsToDefendWith =
              CollectionUtils.getMatches(result.getAverageAttackersRemaining(), Matches.unitIsAir().negate());
          ProBattleResult result2 = calc.calculateBattleResults(t, patd.getMaxEnemyUnits(),
              remainingUnitsToDefendWith, patd.getMaxBombardUnits());
          if (patd.isCanHold() && result2.getTuvSwing() > 0) {
            final List<Unit> unusedUnits = new ArrayList<>(patd.getMaxUnits());
            unusedUnits.addAll(patd.getMaxAmphibUnits());
            unusedUnits.removeAll(usedUnits);
            unusedUnits.addAll(remainingUnitsToDefendWith);
            final ProBattleResult result3 = calc.calculateBattleResults(t, patd.getMaxEnemyUnits(), unusedUnits,
                patd.getMaxBombardUnits());
            if (result3.getTuvSwing() < result2.getTuvSwing()) {
              result2 = result3;
              remainingUnitsToDefendWith = unusedUnits;
            }
          }
          canHold = (!result2.isHasLandUnitRemaining() && !t.isWater()) || (result2.getTuvSwing() < 0)
              || (result2.getWinPercentage() < ProData.minWinPercentage);
          if (result2.getTuvSwing() > 0) {
            enemyCounterTuvSwing = result2.getTuvSwing();
          }
          ProLogger.trace("Territory=" + t.getName() + ", CanHold=" + canHold + ", MyDefenders="
              + remainingUnitsToDefendWith.size() + ", EnemyAttackers=" + patd.getMaxEnemyUnits().size() + ", win%="
              + result2.getWinPercentage() + ", EnemyTUVSwing=" + result2.getTuvSwing() + ", hasLandUnitRemaining="
              + result2.isHasLandUnitRemaining());
        }

        // Find attack value
        final boolean isNeutral = (!t.isWater() && t.getOwner().isNull());
        final int isLand = !t.isWater() ? 1 : 0;
        final int isCanHold = canHold ? 1 : 0;
        final int isCantHoldAmphib = !canHold && !patd.getAmphibAttackMap().isEmpty() ? 1 : 0;
        final int isFactory = ProMatches.territoryHasInfraFactoryAndIsLand().test(t) ? 1 : 0;
        final int isFfa = ProUtils.isFfa(data, player) ? 1 : 0;
        final int production = TerritoryAttachment.getProduction(t);
        double capitalValue = 0;
        final TerritoryAttachment ta = TerritoryAttachment.get(t);
        if (ta != null && ta.isCapital()) {
          capitalValue = ProUtils.getPlayerProduction(t.getOwner(), data);
        }
        final double territoryValue =
            (1 + isLand - isCantHoldAmphib + isFactory + isCanHold * (1 + 2 * isFfa + 2 * isFactory)) * production
                + capitalValue;
        double tuvSwing = result.getTuvSwing();
        if (isFfa == 1 && tuvSwing > 0) {
          tuvSwing *= 0.5;
        }
        final double attackValue =
            tuvSwing + territoryValue * result.getWinPercentage() / 100 - enemyCounterTuvSwing * 2 / 3;
        boolean allUnitsCanAttackOtherTerritory = true;
        if (isNeutral && attackValue < 0) {
          for (final Unit u : patd.getUnits()) {
            boolean canAttackOtherTerritory = false;
            for (final ProTerritory patd2 : prioritizedTerritories) {
              if (!patd.equals(patd2) && unitAttackMap.get(u) != null
                  && unitAttackMap.get(u).contains(patd2.getTerritory())) {
                canAttackOtherTerritory = true;
                break;
              }
            }
            if (!canAttackOtherTerritory) {
              allUnitsCanAttackOtherTerritory = false;
              break;
            }
          }
        }

        // Determine whether to remove attack
        if (!patd.isStrafing() && (result.getWinPercentage() < ProData.minWinPercentage
            || !result.isHasLandUnitRemaining() || (isNeutral && !canHold)
            || (attackValue < 0 && (!isNeutral || allUnitsCanAttackOtherTerritory || result.getBattleRounds() >= 4)))) {
          territoryToRemove = patd;
        }
        ProLogger.debug(patd.getResultString() + ", attackValue=" + attackValue + ", territoryValue=" + territoryValue
            + ", allUnitsCanAttackOtherTerritory=" + allUnitsCanAttackOtherTerritory + " with attackers="
            + patd.getUnits());
      }

      // Determine whether all attacks are successful or try to hold fewer territories
      if (territoryToRemove == null) {
        break;
      }

      prioritizedTerritories.remove(territoryToRemove);
      ProLogger.debug("Removing " + territoryToRemove.getTerritory().getName());
    }
  }

  private Map<Unit, Set<Territory>> tryToAttackTerritories(final List<ProTerritory> prioritizedTerritories,
      final List<Unit> alreadyMovedUnits) {

    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();
    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    final Map<Unit, Set<Territory>> unitAttackMap = territoryManager.getAttackOptions().getUnitMoveMap();
    final Map<Unit, Set<Territory>> transportAttackMap = territoryManager.getAttackOptions().getTransportMoveMap();
    final Map<Unit, Set<Territory>> bombardMap = territoryManager.getAttackOptions().getBombardMap();
    final List<ProTransport> transportMapList = territoryManager.getAttackOptions().getTransportList();

    // Reset lists
    for (final ProTerritory t : attackMap.values()) {
      t.getUnits().clear();
      t.getBombardTerritoryMap().clear();
      t.getAmphibAttackMap().clear();
      t.getTransportTerritoryMap().clear();
      t.setBattleResult(null);
    }

    // Loop through all units and determine attack options
    final Map<Unit, Set<Territory>> unitAttackOptions = new HashMap<>();
    for (final Unit unit : unitAttackMap.keySet()) {

      // Find number of attack options
      final Set<Territory> canAttackTerritories = new HashSet<>();
      for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
        if (unitAttackMap.get(unit).contains(attackTerritoryData.getTerritory())) {
          canAttackTerritories.add(attackTerritoryData.getTerritory());
        }
      }

      // Add units with attack options to map
      if (canAttackTerritories.size() >= 1) {
        unitAttackOptions.put(unit, canAttackTerritories);
      }
    }

    // Sort units by number of attack options and cost
    Map<Unit, Set<Territory>> sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitMoveOptions(unitAttackOptions);

    // Try to set at least one destroyer in each sea territory with subs
    for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
      final Unit unit = it.next();
      final boolean isDestroyerUnit = UnitAttachment.get(unit.getType()).getIsDestroyer();
      if (!isDestroyerUnit) {
        continue; // skip non-destroyer units
      }
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {

        // Add destroyer if territory has subs and a destroyer has been already added
        final List<Unit> defendingUnits = attackMap.get(t).getMaxEnemyDefenders(player, data);
        if (defendingUnits.stream().anyMatch(Matches.unitIsSub())
            && attackMap.get(t).getUnits().stream().noneMatch(Matches.unitIsDestroyer())) {
          attackMap.get(t).addUnit(unit);
          it.remove();
          break;
        }
      }
    }

    // Set enough land and sea units in territories to have at least a chance of winning
    for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
      final Unit unit = it.next();
      final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
      if (isAirUnit) {
        continue; // skip air units
      }
      final TreeMap<Double, Territory> estimatesMap = new TreeMap<>();
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        if (t.isWater() && !attackMap.get(t).isCanHold()) {
          continue; // ignore sea territories that can't be held
        }
        final List<Unit> defendingUnits = attackMap.get(t).getMaxEnemyDefenders(player, data);
        double estimate = ProBattleUtils.estimateStrengthDifference(t, attackMap.get(t).getUnits(), defendingUnits);
        final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
        if (hasAa) {
          estimate -= 10;
        }
        estimatesMap.put(estimate, t);
      }
      if (!estimatesMap.isEmpty() && estimatesMap.firstKey() < 40) {
        final Territory minWinTerritory = estimatesMap.entrySet().iterator().next().getValue();
        attackMap.get(minWinTerritory).addUnit(unit);
        it.remove();
      }
    }

    // Re-sort attack options
    sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(player, sortedUnitAttackOptions,
        attackMap, ProData.unitTerritoryMap, calc);

    // Set non-air units in territories that can be held
    for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
      final Unit unit = it.next();
      final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
      if (isAirUnit) {
        continue; // skip air units
      }
      Territory minWinTerritory = null;
      double minWinPercentage = ProData.winPercentage;
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory patd = attackMap.get(t);
        if (!attackMap.get(t).isCurrentlyWins() && attackMap.get(t).isCanHold()) {
          if (attackMap.get(t).getBattleResult() == null) {
            attackMap.get(t).setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          final ProBattleResult result = attackMap.get(t).getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            minWinPercentage = result.getWinPercentage();
            minWinTerritory = t;
          }
        }
      }
      if (minWinTerritory != null) {
        attackMap.get(minWinTerritory).addUnit(unit);
        attackMap.get(minWinTerritory).setBattleResult(null);
        it.remove();
      }
    }

    // Re-sort attack options
    sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(player, sortedUnitAttackOptions,
        attackMap, ProData.unitTerritoryMap, calc);

    // Set air units in territories that can't be held (don't move planes to empty territories)
    for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
      final Unit unit = it.next();
      final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
      if (!isAirUnit) {
        continue; // skip non-air units
      }
      Territory minWinTerritory = null;
      double minWinPercentage = ProData.winPercentage;
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins() && !patd.isCanHold()) {

          // Check if air unit should avoid this territory due to no guaranteed safe landing location
          final boolean isEnemyCapital = ProUtils.getLiveEnemyCapitals(data, player).contains(t);
          final boolean isAdjacentToAlliedCapital = Matches.territoryHasNeighborMatching(data,
              Matches.territoryIsInList(ProUtils.getLiveAlliedCapitals(data, player))).test(t);
          final int range = TripleAUnit.get(unit).getMovementLeft();
          final int distance = data.getMap().getDistance_IgnoreEndForCondition(ProData.unitTerritoryMap.get(unit), t,
              ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true));
          final boolean usesMoreThanHalfOfRange = distance > range / 2;
          if (isAirUnit && !isEnemyCapital && !isAdjacentToAlliedCapital && usesMoreThanHalfOfRange) {
            continue;
          }

          // Check battle results
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player, data);
            final boolean hasNoDefenders =
                defendingUnits.stream().noneMatch(ProMatches.unitIsEnemyAndNotInfa(player, data));
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, patd.getUnits(), defendingUnits);
            final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
            if (!hasNoDefenders && !isOverwhelmingWin && (!hasAa || result.getWinPercentage() < minWinPercentage)) {
              minWinPercentage = result.getWinPercentage();
              minWinTerritory = t;
              if (patd.isStrafing()) {
                break;
              }
            }
          }
        }
      }
      if (minWinTerritory != null) {
        attackMap.get(minWinTerritory).addUnit(unit);
        attackMap.get(minWinTerritory).setBattleResult(null);
        it.remove();
      }
    }

    // Re-sort attack options
    sortedUnitAttackOptions = ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(player, sortedUnitAttackOptions,
        attackMap, ProData.unitTerritoryMap, calc);

    // Set remaining units in any territory that needs it (don't move planes to empty territories)
    for (final Iterator<Unit> it = sortedUnitAttackOptions.keySet().iterator(); it.hasNext();) {
      final Unit unit = it.next();
      final boolean isAirUnit = UnitAttachment.get(unit.getType()).getIsAir();
      Territory minWinTerritory = null;
      double minWinPercentage = ProData.winPercentage;
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins()) {

          // Check if air unit should avoid this territory due to no guaranteed safe landing location
          final boolean isAdjacentToAlliedFactory = Matches
              .territoryHasNeighborMatching(data, ProMatches.territoryHasInfraFactoryAndIsAlliedLand(player, data))
              .test(t);
          final int range = TripleAUnit.get(unit).getMovementLeft();
          final int distance = data.getMap().getDistance_IgnoreEndForCondition(ProData.unitTerritoryMap.get(unit), t,
              ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true));
          final boolean usesMoreThanHalfOfRange = distance > range / 2;
          final boolean territoryValueIsLessThanUnitValue =
              patd.getValue() < ProData.unitValueMap.getInt(unit.getType());
          if (isAirUnit && !isAdjacentToAlliedFactory && usesMoreThanHalfOfRange
              && (territoryValueIsLessThanUnitValue || (!t.isWater() && !patd.isCanHold()))) {
            continue;
          }
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player, data);
            final boolean hasNoDefenders =
                defendingUnits.stream().noneMatch(ProMatches.unitIsEnemyAndNotInfa(player, data));
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, patd.getUnits(), defendingUnits);
            final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
            if (!isAirUnit || (!hasNoDefenders && !isOverwhelmingWin
                && (!hasAa || result.getWinPercentage() < minWinPercentage))) {
              minWinPercentage = result.getWinPercentage();
              minWinTerritory = t;
            }
          }
        }
      }
      if (minWinTerritory != null) {
        attackMap.get(minWinTerritory).addUnit(unit);
        attackMap.get(minWinTerritory).setBattleResult(null);
        it.remove();
      }
    }

    // Re-sort attack options
    sortedUnitAttackOptions =
        ProSortMoveOptionsUtils.sortUnitNeededOptions(player, sortedUnitAttackOptions, attackMap, calc);

    // If transports can take casualties try placing in naval battles first
    final List<Unit> alreadyAttackedWithTransports = new ArrayList<>();
    if (!Properties.getTransportCasualtiesRestricted(data)) {

      // Loop through all my transports and see which territories they can attack from current list
      final Map<Unit, Set<Territory>> transportAttackOptions = new HashMap<>();
      for (final Unit unit : transportAttackMap.keySet()) {

        // Find number of attack options
        final Set<Territory> canAttackTerritories = new HashSet<>();
        for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
          if (transportAttackMap.get(unit).contains(attackTerritoryData.getTerritory())) {
            canAttackTerritories.add(attackTerritoryData.getTerritory());
          }
        }
        if (!canAttackTerritories.isEmpty()) {
          transportAttackOptions.put(unit, canAttackTerritories);
        }
      }

      // Loop through transports with attack options and determine if any naval battle needs it
      for (final Unit transport : transportAttackOptions.keySet()) {

        // Find current naval battle that needs transport if it isn't transporting units
        for (final Territory t : transportAttackOptions.get(transport)) {
          final ProTerritory patd = attackMap.get(t);
          final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player, data);
          if (!patd.isCurrentlyWins() && !TransportTracker.isTransporting(transport) && !defendingUnits.isEmpty()) {
            if (patd.getBattleResult() == null) {
              patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                  patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
            }
            final ProBattleResult result = patd.getBattleResult();
            if (result.getWinPercentage() < ProData.winPercentage || !result.isHasLandUnitRemaining()) {
              patd.addUnit(transport);
              patd.setBattleResult(null);
              alreadyAttackedWithTransports.add(transport);
              ProLogger.trace("Adding attack transport to: " + t.getName());
              break;
            }
          }
        }
      }
    }

    // Loop through all my transports and see which can make amphib attack
    final Map<Unit, Set<Territory>> amphibAttackOptions = new HashMap<>();
    for (final ProTransport proTransportData : transportMapList) {

      // If already used to attack then ignore
      if (alreadyAttackedWithTransports.contains(proTransportData.getTransport())) {
        continue;
      }

      // Find number of attack options
      final Set<Territory> canAmphibAttackTerritories = new HashSet<>();
      for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
        if (proTransportData.getTransportMap().containsKey(attackTerritoryData.getTerritory())) {
          canAmphibAttackTerritories.add(attackTerritoryData.getTerritory());
        }
      }
      if (!canAmphibAttackTerritories.isEmpty()) {
        amphibAttackOptions.put(proTransportData.getTransport(), canAmphibAttackTerritories);
      }
    }

    // Loop through transports with amphib attack options and determine if any land battle needs it
    for (final Unit transport : amphibAttackOptions.keySet()) {

      // Find current land battle results for territories that unit can amphib attack
      Territory minWinTerritory = null;
      double minWinPercentage = ProData.winPercentage;
      List<Unit> minAmphibUnitsToAdd = null;
      Territory minUnloadFromTerritory = null;
      for (final Territory t : amphibAttackOptions.get(transport)) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins()) {
          if (patd.getBattleResult() == null) {
            patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
                patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {

            // Get all units that have already attacked
            final List<Unit> alreadyAttackedWithUnits = new ArrayList<>(alreadyMovedUnits);
            alreadyAttackedWithUnits.addAll(attackMap.values().stream()
                .map(ProTerritory::getUnits)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

            // Find units that haven't attacked and can be transported
            for (final ProTransport proTransportData : transportMapList) {
              if (proTransportData.getTransport().equals(transport)) {

                // Find units to load
                final Set<Territory> territoriesCanLoadFrom = proTransportData.getTransportMap().get(t);
                final List<Unit> amphibUnitsToAdd = ProTransportUtils.getUnitsToTransportFromTerritories(player,
                    transport, territoriesCanLoadFrom, alreadyAttackedWithUnits);
                if (amphibUnitsToAdd.isEmpty()) {
                  continue;
                }

                // Find best territory to move transport
                double minStrengthDifference = Double.POSITIVE_INFINITY;
                minUnloadFromTerritory = null;
                final Set<Territory> territoriesToMoveTransport =
                    data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, data, false));
                final Set<Territory> loadFromTerritories = new HashSet<>();
                for (final Unit u : amphibUnitsToAdd) {
                  loadFromTerritories.add(ProData.unitTerritoryMap.get(u));
                }
                for (final Territory territoryToMoveTransport : territoriesToMoveTransport) {
                  if (proTransportData.getSeaTransportMap().containsKey(territoryToMoveTransport) && proTransportData
                      .getSeaTransportMap().get(territoryToMoveTransport).containsAll(loadFromTerritories)) {
                    List<Unit> attackers = new ArrayList<>();
                    if (enemyAttackOptions.getMax(territoryToMoveTransport) != null) {
                      attackers = enemyAttackOptions.getMax(territoryToMoveTransport).getMaxUnits();
                    }
                    final List<Unit> defenders =
                        territoryToMoveTransport.getUnits().getMatches(Matches.isUnitAllied(player, data));
                    defenders.add(transport);
                    final double strengthDifference =
                        ProBattleUtils.estimateStrengthDifference(territoryToMoveTransport, attackers, defenders);
                    if (strengthDifference < minStrengthDifference) {
                      minStrengthDifference = strengthDifference;
                      minUnloadFromTerritory = territoryToMoveTransport;
                    }
                  }
                }
                minWinTerritory = t;
                minWinPercentage = result.getWinPercentage();
                minAmphibUnitsToAdd = amphibUnitsToAdd;
                break;
              }
            }
          }
        }
      }
      if (minWinTerritory != null) {
        if (minUnloadFromTerritory != null) {
          attackMap.get(minWinTerritory).getTransportTerritoryMap().put(transport, minUnloadFromTerritory);
        }
        attackMap.get(minWinTerritory).addUnits(minAmphibUnitsToAdd);
        attackMap.get(minWinTerritory).putAmphibAttackMap(transport, minAmphibUnitsToAdd);
        attackMap.get(minWinTerritory).setBattleResult(null);
        for (final Unit unit : minAmphibUnitsToAdd) {
          sortedUnitAttackOptions.remove(unit);
        }
        ProLogger.trace("Adding amphibious attack to " + minWinTerritory + ", units=" + minAmphibUnitsToAdd.size()
            + ", unloadFrom=" + minUnloadFromTerritory);
      }
    }

    // Get all units that have already moved
    final Set<Unit> alreadyAttackedWithUnits = new HashSet<>();
    for (final ProTerritory t : attackMap.values()) {
      alreadyAttackedWithUnits.addAll(t.getUnits());
      alreadyAttackedWithUnits.addAll(t.getAmphibAttackMap().keySet());
    }

    // Loop through all my bombard units and see which can bombard
    final Map<Unit, Set<Territory>> bombardOptions = new HashMap<>();
    for (final Unit u : bombardMap.keySet()) {

      // If already used to attack then ignore
      if (alreadyAttackedWithUnits.contains(u)) {
        continue;
      }

      // Find number of bombard options
      final Set<Territory> canBombardTerritories = new HashSet<>();
      for (final ProTerritory patd : prioritizedTerritories) {
        final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player, data);
        final boolean hasDefenders = defendingUnits.stream().anyMatch(Matches.unitIsInfrastructure().negate());
        if (bombardMap.get(u).contains(patd.getTerritory()) && !patd.getTransportTerritoryMap().isEmpty()
            && hasDefenders && !TransportTracker.isTransporting(u)) {
          canBombardTerritories.add(patd.getTerritory());
        }
      }
      if (!canBombardTerritories.isEmpty()) {
        bombardOptions.put(u, canBombardTerritories);
      }
    }

    // Loop through bombard units to see if any amphib battles need
    for (final Unit u : bombardOptions.keySet()) {

      // Find current land battle results for territories that unit can bombard
      Territory minWinTerritory = null;
      double minWinPercentage = Double.MAX_VALUE;
      Territory minBombardFromTerritory = null;
      for (final Territory t : bombardOptions.get(u)) {
        final ProTerritory patd = attackMap.get(t);
        if (patd.getBattleResult() == null) {
          patd.setBattleResult(calc.estimateAttackBattleResults(t, patd.getUnits(),
              patd.getMaxEnemyDefenders(player, data), patd.getBombardTerritoryMap().keySet()));
        }
        final ProBattleResult result = patd.getBattleResult();
        if (result.getWinPercentage() < minWinPercentage
            || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {

          // Find territory to bombard from
          Territory bombardFromTerritory = null;
          for (final Territory unloadFromTerritory : patd.getTransportTerritoryMap().values()) {
            if (patd.getBombardOptionsMap().get(u).contains(unloadFromTerritory)) {
              bombardFromTerritory = unloadFromTerritory;
            }
          }
          if (bombardFromTerritory != null) {
            minWinTerritory = t;
            minWinPercentage = result.getWinPercentage();
            minBombardFromTerritory = bombardFromTerritory;
          }
        }
      }
      if (minWinTerritory != null) {
        attackMap.get(minWinTerritory).getBombardTerritoryMap().put(u, minBombardFromTerritory);
        attackMap.get(minWinTerritory).setBattleResult(null);
        sortedUnitAttackOptions.remove(u);
        ProLogger.trace(
            "Adding bombard to " + minWinTerritory + ", units=" + u + ", bombardFrom=" + minBombardFromTerritory);
      }
    }
    return sortedUnitAttackOptions;
  }

  private void removeAttacksUntilCapitalCanBeHeld(final List<ProTerritory> prioritizedTerritories,
      final List<ProPurchaseOption> landPurchaseOptions) {

    ProLogger.info("Check capital defenses after attack moves");

    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();

    final Territory myCapital = ProData.myCapital;

    // Add max purchase defenders to capital for non-mobile factories (don't consider mobile factories since they may
    // move elsewhere)
    final List<Unit> placeUnits = new ArrayList<>();
    if (ProMatches.territoryHasNonMobileInfraFactoryAndIsNotConqueredOwnedLand(player, data).test(myCapital)) {
      placeUnits.addAll(ProPurchaseUtils.findMaxPurchaseDefenders(player, myCapital, landPurchaseOptions));
    }

    // Remove attack until capital can be defended
    while (true) {
      if (prioritizedTerritories.isEmpty()) {
        break;
      }

      // Determine max enemy counter attack units
      final List<Territory> territoriesToAttack = new ArrayList<>();
      for (final ProTerritory t : prioritizedTerritories) {
        territoriesToAttack.add(t.getTerritory());
      }
      ProLogger.trace("Remaining territories to attack=" + territoriesToAttack);
      final List<Territory> territoriesToCheck = new ArrayList<>();
      territoriesToCheck.add(myCapital);
      territoryManager.populateEnemyAttackOptions(territoriesToAttack, territoriesToCheck);
      final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
      if (enemyAttackOptions.getMax(myCapital) == null) {
        break;
      }

      // Find max remaining defenders
      final Set<Territory> territoriesAdjacentToCapital =
          data.getMap().getNeighbors(myCapital, Matches.territoryIsLand());
      final List<Unit> defenders = myCapital.getUnits().getMatches(Matches.isUnitAllied(player, data));
      defenders.addAll(placeUnits);
      for (final Territory t : territoriesAdjacentToCapital) {
        defenders.addAll(t.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, false)));
      }
      for (final ProTerritory t : attackMap.values()) {
        defenders.removeAll(t.getUnits());
      }

      // Determine counter attack results to see if I can hold it
      final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackOptions.getMax(myCapital).getMaxUnits());
      enemyAttackingUnits.addAll(enemyAttackOptions.getMax(myCapital).getMaxAmphibUnits());
      final ProBattleResult result = calc.estimateDefendBattleResults(myCapital,
          new ArrayList<>(enemyAttackingUnits), defenders, enemyAttackOptions.getMax(myCapital).getMaxBombardUnits());
      ProLogger.trace("Current capital result hasLandUnitRemaining=" + result.isHasLandUnitRemaining() + ", TUVSwing="
          + result.getTuvSwing() + ", defenders=" + defenders.size() + ", attackers=" + enemyAttackingUnits.size());

      // Determine attack that uses the most units per value from capital and remove it
      if (result.isHasLandUnitRemaining()) {
        double maxUnitsNearCapitalPerValue = 0.0;
        Territory maxTerritory = null;
        final Set<Territory> territoriesNearCapital = data.getMap().getNeighbors(myCapital, Matches.territoryIsLand());
        territoriesNearCapital.add(myCapital);
        for (final Territory t : attackMap.keySet()) {
          int unitsNearCapital = 0;
          for (final Unit u : attackMap.get(t).getUnits()) {
            if (territoriesNearCapital.contains(ProData.unitTerritoryMap.get(u))) {
              unitsNearCapital++;
            }
          }
          final double unitsNearCapitalPerValue = unitsNearCapital / attackMap.get(t).getValue();
          ProLogger.trace(t.getName() + " has unit near capital per value: " + unitsNearCapitalPerValue);
          if (unitsNearCapitalPerValue > maxUnitsNearCapitalPerValue) {
            maxUnitsNearCapitalPerValue = unitsNearCapitalPerValue;
            maxTerritory = t;
          }
        }
        if (maxTerritory != null) {
          prioritizedTerritories.remove(attackMap.get(maxTerritory));
          attackMap.get(maxTerritory).getUnits().clear();
          attackMap.get(maxTerritory).getAmphibAttackMap().clear();
          attackMap.get(maxTerritory).setBattleResult(null);
          ProLogger.debug("Removing territory to try to hold capital: " + maxTerritory.getName());
        } else {
          break;
        }
      } else {
        ProLogger.debug("Can hold capital: " + myCapital.getName());
        break;
      }
    }
  }

  private void checkContestedSeaTerritories() {

    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();

    for (final Territory t : ProData.myUnitTerritories) {
      if (t.isWater() && Matches.territoryHasEnemyUnits(player, data).test(t)
          && (attackMap.get(t) == null || attackMap.get(t).getUnits().isEmpty())) {

        // Move into random adjacent safe sea territory
        final Set<Territory> possibleMoveTerritories =
            data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnitsThrough(player, data, true));
        if (!possibleMoveTerritories.isEmpty()) {
          final Territory moveToTerritory = possibleMoveTerritories.iterator().next();
          final List<Unit> mySeaUnits = t.getUnits().getMatches(ProMatches.unitCanBeMovedAndIsOwnedSea(player, true));
          if (attackMap.containsKey(moveToTerritory)) {
            attackMap.get(moveToTerritory).addUnits(mySeaUnits);
          } else {
            final ProTerritory moveTerritoryData = new ProTerritory(moveToTerritory);
            moveTerritoryData.addUnits(mySeaUnits);
            attackMap.put(moveToTerritory, moveTerritoryData);
          }
          ProLogger.info(t + " is a contested territory so moving subs to " + moveToTerritory);
        }
      }
    }
  }

  private void logAttackMoves(final List<ProTerritory> prioritizedTerritories) {

    final Map<Territory, ProTerritory> attackMap = territoryManager.getAttackOptions().getTerritoryMap();

    // Print prioritization
    ProLogger.debug("Prioritized territories:");
    for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
      ProLogger.trace("  " + attackTerritoryData.getMaxBattleResult().getTuvSwing() + "  "
          + attackTerritoryData.getValue() + "  " + attackTerritoryData.getTerritory().getName());
    }

    // Print enemy territories with enemy units vs my units
    ProLogger.debug("Territories that can be attacked:");
    int count = 0;
    for (final Territory t : attackMap.keySet()) {
      count++;
      ProLogger.trace(count + ". ---" + t.getName());
      final Set<Unit> combinedUnits = new HashSet<>(attackMap.get(t).getMaxUnits());
      combinedUnits.addAll(attackMap.get(t).getMaxAmphibUnits());
      ProLogger.trace("  --- My max units ---");
      final Map<String, Integer> printMap = new HashMap<>();
      for (final Unit unit : combinedUnits) {
        if (printMap.containsKey(unit.toStringNoOwner())) {
          printMap.put(unit.toStringNoOwner(), printMap.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap.put(unit.toStringNoOwner(), 1);
        }
      }
      for (final String key : printMap.keySet()) {
        ProLogger.trace("    " + printMap.get(key) + " " + key);
      }
      ProLogger.trace("  --- My max bombard units ---");
      final Map<String, Integer> printBombardMap = new HashMap<>();
      for (final Unit unit : attackMap.get(t).getMaxBombardUnits()) {
        if (printBombardMap.containsKey(unit.toStringNoOwner())) {
          printBombardMap.put(unit.toStringNoOwner(), printBombardMap.get(unit.toStringNoOwner()) + 1);
        } else {
          printBombardMap.put(unit.toStringNoOwner(), 1);
        }
      }
      for (final String key : printBombardMap.keySet()) {
        ProLogger.trace("    " + printBombardMap.get(key) + " " + key);
      }
      final List<Unit> units3 = attackMap.get(t).getUnits();
      ProLogger.trace("  --- My actual units ---");
      final Map<String, Integer> printMap3 = new HashMap<>();
      for (final Unit unit : units3) {
        if (printMap3.containsKey(unit.toStringNoOwner())) {
          printMap3.put(unit.toStringNoOwner(), printMap3.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap3.put(unit.toStringNoOwner(), 1);
        }
      }
      for (final String key : printMap3.keySet()) {
        ProLogger.trace("    " + printMap3.get(key) + " " + key);
      }
      ProLogger.trace("  --- Enemy units ---");
      final Map<String, Integer> printMap2 = new HashMap<>();
      final List<Unit> units2 = attackMap.get(t).getMaxEnemyDefenders(player, data);
      for (final Unit unit : units2) {
        if (printMap2.containsKey(unit.toStringNoOwner())) {
          printMap2.put(unit.toStringNoOwner(), printMap2.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap2.put(unit.toStringNoOwner(), 1);
        }
      }
      for (final String key : printMap2.keySet()) {
        ProLogger.trace("    " + printMap2.get(key) + " " + key);
      }
      ProLogger.trace("  --- Enemy Counter Attack Units ---");
      final Map<String, Integer> printMap4 = new HashMap<>();
      final List<Unit> units4 = attackMap.get(t).getMaxEnemyUnits();
      for (final Unit unit : units4) {
        if (printMap4.containsKey(unit.toStringNoOwner())) {
          printMap4.put(unit.toStringNoOwner(), printMap4.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap4.put(unit.toStringNoOwner(), 1);
        }
      }
      for (final String key : printMap4.keySet()) {
        ProLogger.trace("    " + printMap4.get(key) + " " + key);
      }
      ProLogger.trace("  --- Enemy Counter Bombard Units ---");
      final Map<String, Integer> printMap5 = new HashMap<>();
      final Set<Unit> units5 = attackMap.get(t).getMaxEnemyBombardUnits();
      for (final Unit unit : units5) {
        if (printMap5.containsKey(unit.toStringNoOwner())) {
          printMap5.put(unit.toStringNoOwner(), printMap5.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap5.put(unit.toStringNoOwner(), 1);
        }
      }
      for (final String key : printMap5.keySet()) {
        ProLogger.trace("    " + printMap4.get(key) + " " + key);
      }
    }
  }

  private boolean canAirSafelyLandAfterAttack(final Unit unit, final Territory t) {
    final boolean isAdjacentToAlliedFactory = Matches
        .territoryHasNeighborMatching(data, ProMatches.territoryHasInfraFactoryAndIsAlliedLand(player, data)).test(t);
    final int range = TripleAUnit.get(unit).getMovementLeft();
    final int distance = data.getMap().getDistance_IgnoreEndForCondition(ProData.unitTerritoryMap.get(unit), t,
        ProMatches.territoryCanMoveAirUnitsAndNoAa(player, data, true));
    final boolean usesMoreThanHalfOfRange = distance > range / 2;
    return isAdjacentToAlliedFactory || !usesMoreThanHalfOfRange;
  }

}
