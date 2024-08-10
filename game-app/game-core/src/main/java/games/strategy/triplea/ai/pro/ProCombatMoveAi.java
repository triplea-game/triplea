package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.ai.pro.util.ProUtils.summarizeUnits;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProOtherMoveOptions;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritoryManager;
import games.strategy.triplea.ai.pro.data.ProTransport;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProMoveUtils;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.ai.pro.util.ProSortMoveOptionsUtils;
import games.strategy.triplea.ai.pro.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.pro.util.ProTransportUtils;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.AirBattle;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.triplea.java.collections.CollectionUtils;

/** Pro combat move AI. */
public class ProCombatMoveAi {

  private static final int MIN_BOMBING_SCORE = 4; // Avoid bombing low production factories with AA

  private final AbstractProAi ai;
  private final ProData proData;
  private final ProOddsCalculator calc;
  private GameData data;
  private GamePlayer player;
  private ProTerritoryManager territoryManager;
  private boolean isDefensive;
  private boolean isBombing;

  ProCombatMoveAi(final AbstractProAi ai) {
    this.ai = ai;
    this.proData = ai.getProData();
    calc = ai.getCalc();
  }

  Map<Territory, ProTerritory> doCombatMove(final IMoveDelegate moveDel) {
    ProLogger.info("Starting combat move phase");

    // Current data at the start of combat move
    data = proData.getData();
    player = proData.getPlayer();
    territoryManager = new ProTerritoryManager(calc, proData);

    // Determine whether capital is threatened, and I should be in a defensive stance
    isDefensive =
        !ProBattleUtils.territoryHasLocalLandSuperiority(
            proData, proData.getMyCapital(), ProBattleUtils.MEDIUM_RANGE, player);
    isBombing = false;
    ProLogger.debug("Currently in defensive stance: " + isDefensive);

    // Find the maximum number of units that can attack each territory and max enemy defenders
    territoryManager.populateAttackOptions();
    territoryManager.populateEnemyDefenseOptions();

    // Remove territories that aren't worth attacking and prioritize the remaining ones
    final List<ProTerritory> attackOptions =
        territoryManager.removeTerritoriesThatCantBeConquered();
    List<Territory> clearedTerritories = new ArrayList<>();
    for (final ProTerritory patd : attackOptions) {
      clearedTerritories.add(patd.getTerritory());
    }
    territoryManager.populateEnemyAttackOptions(clearedTerritories, clearedTerritories);
    determineTerritoriesThatCanBeHeld(attackOptions, clearedTerritories);
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
        possibleTransportTerritories.addAll(
            data.getMap().getNeighbors(patd.getTerritory(), Matches.territoryIsWater()));
      }
    }
    possibleTransportTerritories.addAll(clearedTerritories);
    territoryManager.populateEnemyAttackOptions(clearedTerritories, possibleTransportTerritories);
    determineTerritoriesThatCanBeHeld(attackOptions, clearedTerritories);
    removeTerritoriesThatArentWorthAttacking(attackOptions);

    // Determine how many units to attack each territory with
    final List<Unit> alreadyMovedUnits =
        moveOneDefenderToLandTerritoriesBorderingEnemy(attackOptions);
    determineUnitsToAttackWith(attackOptions, alreadyMovedUnits);

    // Get all transport final territories
    ProMoveUtils.calculateAmphibRoutes(
        proData, player, territoryManager.getAttackOptions().getTerritoryMap(), true);

    // Determine max enemy counter-attack units and remove territories where transports are exposed
    removeTerritoriesWhereTransportsAreExposed();

    // Determine if capital can be held if I still own it
    if (proData.getMyCapital() != null && proData.getMyCapital().isOwnedBy(player)) {
      removeAttacksUntilCapitalCanBeHeld(
          attackOptions, proData.getPurchaseOptions().getLandOptions());
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

    final Map<Territory, ProTerritory> result =
        territoryManager.getAttackOptions().getTerritoryMap();
    territoryManager = null;
    return result;
  }

  void doMove(
      final Map<Territory, ProTerritory> attackMap,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    this.data = data;
    this.player = player;

    ProMoveUtils.doMove(
        proData, ProMoveUtils.calculateMoveRoutes(proData, player, attackMap, true), moveDel);
    ProMoveUtils.doMove(
        proData, ProMoveUtils.calculateAmphibRoutes(proData, player, attackMap, true), moveDel);
    ProMoveUtils.doMove(
        proData, ProMoveUtils.calculateBombardMoveRoutes(proData, player, attackMap), moveDel);
    isBombing = true;
    ProMoveUtils.doMove(
        proData, ProMoveUtils.calculateBombingRoutes(proData, player, attackMap), moveDel);
    isBombing = false;
  }

  boolean isBombing() {
    return isBombing;
  }

  private void prioritizeAttackOptions(
      final GamePlayer player, final List<ProTerritory> attackOptions) {

    ProLogger.info("Prioritizing territories to try to attack");

    // Calculate value of attacking territory
    for (final Iterator<ProTerritory> it = attackOptions.iterator(); it.hasNext(); ) {
      final ProTerritory patd = it.next();
      final Territory t = patd.getTerritory();

      // Determine territory attack properties
      final int isLand = !t.isWater() ? 1 : 0;
      final int isNeutral = ProUtils.isNeutralLand(t) ? 1 : 0;
      final int isCanHold = patd.isCanHold() ? 1 : 0;
      final int isAmphib = patd.isNeedAmphibUnits() ? 1 : 0;
      final List<Unit> defendingUnits =
          CollectionUtils.getMatches(
              patd.getMaxEnemyDefenders(player), ProMatches.unitIsEnemyAndNotInfa(player));
      final int isEmptyLand =
          (!t.isWater() && defendingUnits.isEmpty() && !patd.isNeedAmphibUnits()) ? 1 : 0;
      final boolean isAdjacentToMyCapital =
          !data.getMap().getNeighbors(t, Matches.territoryIs(proData.getMyCapital())).isEmpty();
      final int isNotNeutralAdjacentToMyCapital =
          (isAdjacentToMyCapital
                  && ProMatches.territoryIsEnemyNotPassiveNeutralLand(player).test(t))
              ? 1
              : 0;
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
      final double territoryValue =
          (1 + isLand + isCanHold * (1 + 2.0 * isFfa * isLand))
              * (1 + isEmptyLand)
              * (1 + isFactory)
              * (1 - 0.5 * isAmphib)
              * production;
      double attackValue =
          (tuvSwing + territoryValue)
              * (1 + 4.0 * isEnemyCapital)
              * (1 + 2.0 * isNotNeutralAdjacentToMyCapital)
              * (1 - 0.9 * isNeutral);

      // Check if a negative value neutral territory should be attacked
      if (attackValue <= 0 && !patd.isNeedAmphibUnits() && ProUtils.isNeutralLand(t)) {

        // Determine enemy neighbor territory production value for neutral land territories
        double nearbyEnemyValue = 0;
        final List<Territory> cantReachEnemyTerritories = new ArrayList<>();
        final Set<Territory> nearbyTerritories =
            data.getMap().getNeighbors(t, ProMatches.territoryCanMoveLandUnits(player, true));
        final List<Territory> nearbyEnemyTerritories =
            CollectionUtils.getMatches(nearbyTerritories, Matches.isTerritoryEnemy(player));
        final List<Territory> nearbyTerritoriesWithOwnedUnits =
            CollectionUtils.getMatches(nearbyTerritories, Matches.territoryHasUnitsOwnedBy(player));
        for (final Territory nearbyEnemyTerritory : nearbyEnemyTerritories) {
          boolean allAlliedNeighborsHaveRoute = true;
          for (final Territory nearbyAlliedTerritory : nearbyTerritoriesWithOwnedUnits) {
            final int distance =
                data.getMap()
                    .getDistanceIgnoreEndForCondition(
                        nearbyAlliedTerritory,
                        nearbyEnemyTerritory,
                        ProMatches.territoryIsEnemyNotPassiveNeutralOrAllied(player));
            if (distance < 0 || distance > 2) {
              allAlliedNeighborsHaveRoute = false;
              break;
            }
          }
          if (!allAlliedNeighborsHaveRoute) {
            final double value =
                ProTerritoryValueUtils.findTerritoryAttackValue(
                    proData, player, nearbyEnemyTerritory);
            if (value > 0) {
              nearbyEnemyValue += value;
            }
            cantReachEnemyTerritories.add(nearbyEnemyTerritory);
          }
        }
        ProLogger.debug(
            t.getName()
                + " calculated nearby enemy value="
                + nearbyEnemyValue
                + " from "
                + cantReachEnemyTerritories);
        if (nearbyEnemyValue > 0) {
          ProLogger.trace(t.getName() + " updating negative neutral attack value=" + attackValue);
          attackValue = nearbyEnemyValue * .001 / (1 - attackValue);
        } else {

          // Check if overwhelming attack strength (more than 5 times)
          final double strengthDifference =
              ProBattleUtils.estimateStrengthDifference(
                  t, patd.getMaxUnits(), patd.getMaxEnemyDefenders(player));
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
          || (isDefensive
              && attackValue <= 8
              && data.getMap().getDistance(proData.getMyCapital(), t) <= 3)) {
        ProLogger.debug(
            "Removing territory that has a negative attack value: "
                + t.getName()
                + ", AttackValue="
                + patd.getValue());
        it.remove();
      }
    }

    // Sort attack territories by value
    attackOptions.sort(Comparator.comparingDouble(ProTerritory::getValue).reversed());

    // Log prioritized territories
    for (final ProTerritory patd : attackOptions) {
      ProLogger.debug(
          "AttackValue="
              + patd.getValue()
              + ", TUVSwing="
              + patd.getMaxBattleResult().getTuvSwing()
              + ", isAmphib="
              + patd.isNeedAmphibUnits()
              + ", "
              + patd.getTerritory().getName());
    }
  }

  private void determineTerritoriesToAttack(final List<ProTerritory> prioritizedTerritories) {

    ProLogger.info("Determine which territories to attack");

    // Assign units to territories by prioritization
    int numToAttack = Math.min(1, prioritizedTerritories.size());
    boolean haveRemovedAllAmphibTerritories = false;
    while (true) {
      final List<ProTerritory> territoriesToTryToAttack =
          prioritizedTerritories.subList(0, numToAttack);
      ProLogger.debug("Current number of territories: " + numToAttack);
      tryToAttackTerritories(territoriesToTryToAttack, List.of());

      // Determine if all attacks are successful
      boolean areSuccessful = true;
      for (final ProTerritory patd : territoriesToTryToAttack) {
        final Territory t = patd.getTerritory();
        if (patd.getBattleResult() == null) {
          patd.estimateBattleResult(calc, player);
        }
        ProLogger.trace(patd.getResultString() + " with attackers: " + patd.getUnits());
        final double estimate =
            ProBattleUtils.estimateStrengthDifference(
                t, patd.getUnits(), patd.getMaxEnemyDefenders(player));
        final ProBattleResult result = patd.getBattleResult();
        if (!patd.isStrafing()
            && estimate < patd.getStrengthEstimate()
            && (result.getWinPercentage() < proData.getMinWinPercentage()
                || !result.isHasLandUnitRemaining())) {
          areSuccessful = false;
        }
      }

      // Determine whether to try more territories, remove a territory, or end
      if (areSuccessful) {
        for (final ProTerritory patd : territoriesToTryToAttack) {
          patd.setCanAttack(true);
          final double estimate =
              ProBattleUtils.estimateStrengthDifference(
                  patd.getTerritory(), patd.getUnits(), patd.getMaxEnemyDefenders(player));
          if (estimate < patd.getStrengthEstimate()) {
            patd.setStrengthEstimate(estimate);
          }
        }

        // If already used all transports then remove any remaining amphib territories
        if (!haveRemovedAllAmphibTerritories && territoryManager.haveUsedAllAttackTransports()) {
          final List<ProTerritory> amphibTerritoriesToRemove = new ArrayList<>();
          for (int i = numToAttack; i < prioritizedTerritories.size(); i++) {
            if (prioritizedTerritories.get(i).isNeedAmphibUnits()) {
              amphibTerritoriesToRemove.add(prioritizedTerritories.get(i));
              ProLogger.debug(
                  "Removing amphib territory since already used all transports: "
                      + prioritizedTerritories.get(i).getTerritory().getName());
            }
          }
          prioritizedTerritories.removeAll(amphibTerritoriesToRemove);
          haveRemovedAllAmphibTerritories = true;
        }

        // Can attack all territories in list so end
        numToAttack++;
        if (numToAttack > prioritizedTerritories.size()) {
          break;
        }
      } else {
        ProLogger.debug(
            "Removing territory: "
                + prioritizedTerritories.get(numToAttack - 1).getTerritory().getName());
        prioritizedTerritories.remove(numToAttack - 1);
        if (numToAttack > prioritizedTerritories.size()) {
          numToAttack--;
        }
      }
    }
    ProLogger.debug("Final number of territories: " + (numToAttack - 1));
  }

  private void determineTerritoriesThatCanBeHeld(
      final List<ProTerritory> prioritizedTerritories, final List<Territory> clearedTerritories) {

    ProLogger.info("Check if we should try to hold attack territories");

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();

    // Determine which territories to try and hold
    final Set<Territory> territoriesToCheck = new HashSet<>();
    for (final ProTerritory patd : prioritizedTerritories) {
      final Territory t = patd.getTerritory();
      territoriesToCheck.add(t);
      final List<Unit> nonAirAttackers =
          CollectionUtils.getMatches(patd.getMaxUnits(), Matches.unitIsNotAir());
      for (final Unit u : nonAirAttackers) {
        territoriesToCheck.add(proData.getUnitTerritory(u));
      }
    }
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(
            proData, player, List.of(), clearedTerritories, territoriesToCheck);
    for (final ProTerritory patd : prioritizedTerritories) {
      final Territory t = patd.getTerritory();

      // If strafing then can't hold
      if (patd.isStrafing()) {
        patd.setCanHold(false);
        ProLogger.debug(t + ", strafing so CanHold=false");
        continue;
      }

      // Set max enemy attackers
      final ProTerritory enemyAttackMax = enemyAttackOptions.getMax(t);
      if (enemyAttackMax != null) {
        final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackMax.getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackMax.getMaxAmphibUnits());
        patd.setMaxEnemyUnits(enemyAttackingUnits);
        patd.setMaxEnemyBombardUnits(enemyAttackMax.getMaxBombardUnits());
      }

      // Add strategic value for factories
      int isFactory = 0;
      if (ProMatches.territoryHasInfraFactoryAndIsLand().test(t)) {
        isFactory = 1;
      }

      // Determine whether its worth trying to hold territory
      double totalValue = 0.0;
      final List<Unit> nonAirAttackers =
          CollectionUtils.getMatches(patd.getMaxUnits(), Matches.unitIsNotAir());
      for (final Unit u : nonAirAttackers) {
        totalValue += territoryValueMap.get(proData.getUnitTerritory(u));
      }
      final double averageValue = totalValue / nonAirAttackers.size() * 0.75;
      final double territoryValue = territoryValueMap.get(t) * (1 + 4.0 * isFactory);
      if (!t.isWater() && territoryValue < averageValue) {
        attackMap.get(t).setCanHold(false);
        ProLogger.debug(
            t
                + ", CanHold=false, value="
                + territoryValueMap.get(t)
                + ", averageAttackFromValue="
                + averageValue);
        continue;
      }
      if (enemyAttackOptions.getMax(t) != null) {

        // Find max remaining defenders
        final Set<Unit> attackingUnits = new HashSet<>(patd.getMaxUnits());
        attackingUnits.addAll(patd.getMaxAmphibUnits());
        final ProBattleResult result =
            calc.estimateAttackBattleResults(
                proData,
                t,
                attackingUnits,
                patd.getMaxEnemyDefenders(player),
                patd.getMaxBombardUnits());
        final List<Unit> remainingUnitsToDefendWith =
            CollectionUtils.getMatches(
                result.getAverageAttackersRemaining(), Matches.unitIsAir().negate());
        ProLogger.debug(
            t
                + ", value="
                + territoryValueMap.get(t)
                + ", averageAttackFromValue="
                + averageValue
                + ", MyAttackers="
                + attackingUnits.size()
                + ", RemainingUnits="
                + remainingUnitsToDefendWith.size());

        // Determine counter-attack results to see if I can hold it
        final ProBattleResult result2 =
            calc.calculateBattleResults(
                proData,
                t,
                patd.getMaxEnemyUnits(),
                remainingUnitsToDefendWith,
                enemyAttackOptions.getMax(t).getMaxBombardUnits());
        final boolean canHold =
            (!result2.isHasLandUnitRemaining() && !t.isWater())
                || (result2.getTuvSwing() < 0)
                || (result2.getWinPercentage() < proData.getMinWinPercentage());
        patd.setCanHold(canHold);
        ProLogger.debug(
            t
                + ", CanHold="
                + canHold
                + ", MyDefenders="
                + remainingUnitsToDefendWith.size()
                + ", EnemyAttackers="
                + patd.getMaxEnemyUnits().size()
                + ", win%="
                + result2.getWinPercentage()
                + ", EnemyTUVSwing="
                + result2.getTuvSwing()
                + ", hasLandUnitRemaining="
                + result2.isHasLandUnitRemaining());
      } else {
        attackMap.get(t).setCanHold(true);
        ProLogger.debug(
            t
                + ", CanHold=true since no enemy counter attackers, value="
                + territoryValueMap.get(t)
                + ", averageAttackFromValue="
                + averageValue);
      }
    }
  }

  private void removeTerritoriesThatArentWorthAttacking(
      final List<ProTerritory> prioritizedTerritories) {
    ProLogger.info("Remove territories that aren't worth attacking");

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through all prioritized territories
    for (final Iterator<ProTerritory> it = prioritizedTerritories.iterator(); it.hasNext(); ) {
      final ProTerritory patd = it.next();
      final Territory t = patd.getTerritory();
      ProLogger.debug(
          "Checking territory="
              + patd.getTerritory().getName()
              + " with isAmphib="
              + patd.isNeedAmphibUnits());

      // Remove empty convoy zones that can't be held
      if (!patd.isCanHold()
          && enemyAttackOptions.getMax(t) != null
          && t.isWater()
          && !t.anyUnitsMatch(Matches.enemyUnit(player))) {
        ProLogger.debug(
            "Removing convoy zone that can't be held: "
                + t.getName()
                + ", enemyAttackers="
                + summarizeUnits(enemyAttackOptions.getMax(t).getMaxUnits()));
        it.remove();
        continue;
      }

      // Remove neutral and low value amphib land territories that can't be held
      final boolean isNeutral = ProUtils.isNeutralLand(t);
      final double strengthDifference =
          ProBattleUtils.estimateStrengthDifference(
              t, patd.getMaxUnits(), patd.getMaxEnemyDefenders(player));
      if (!patd.isCanHold() && enemyAttackOptions.getMax(t) != null && !t.isWater()) {
        if (isNeutral && strengthDifference <= 500) {

          // Remove neutral territories that can't be held and don't have overwhelming attack
          // strength
          ProLogger.debug(
              "Removing neutral territory that can't be held: "
                  + t.getName()
                  + ", enemyAttackers="
                  + summarizeUnits(enemyAttackOptions.getMax(t).getMaxUnits())
                  + ", enemyAmphibAttackers="
                  + summarizeUnits(enemyAttackOptions.getMax(t).getMaxAmphibUnits())
                  + ", strengthDifference="
                  + strengthDifference);
          it.remove();
          continue;
        } else if (patd.isNeedAmphibUnits() && patd.getValue() < 2) {

          // Remove amphib territories that aren't worth attacking
          ProLogger.debug(
              "Removing low value amphib territory that can't be held: "
                  + t.getName()
                  + ", enemyAttackers="
                  + summarizeUnits(enemyAttackOptions.getMax(t).getMaxUnits())
                  + ", enemyAmphibAttackers="
                  + summarizeUnits(enemyAttackOptions.getMax(t).getMaxAmphibUnits()));
          it.remove();
          continue;
        }
      }
      // Remove neutral territories where attackers are adjacent to enemy territories that aren't
      // being attacked
      if (isNeutral && !t.isWater() && strengthDifference <= 500) {

        // Get list of territories I'm attacking
        final List<Territory> prioritizedTerritoryList = new ArrayList<>();
        for (final ProTerritory prioritizedTerritory : prioritizedTerritories) {
          prioritizedTerritoryList.add(prioritizedTerritory.getTerritory());
        }

        // Find all territories units are attacking from that are adjacent to territory
        final Set<Territory> attackFromTerritories = new HashSet<>();
        for (final Unit u : patd.getMaxUnits()) {
          attackFromTerritories.add(proData.getUnitTerritory(u));
        }
        attackFromTerritories.retainAll(data.getMap().getNeighbors(t));

        // Determine if any of the attacking from territories has enemy neighbors that aren't being
        // attacked
        // Note: Use territoryIsEnemyNotNeutralLand(), not territoryIsEnemyNotPassiveNeutralLand()
        // so that the neutrality check is consistent with logic for isNeutral.
        Predicate<Territory> enemyTerritory = ProMatches.territoryIsEnemyNotNeutralLand(player);
        Territory attackFromTerritoryWithEnemyNeighbors =
            attackFromTerritories.stream()
                .filter(
                    attackFromTerritory -> {
                      final Set<Territory> enemyNeighbors =
                          data.getMap().getNeighbors(attackFromTerritory, enemyTerritory);
                      return !prioritizedTerritoryList.containsAll(enemyNeighbors);
                    })
                .findAny()
                .orElse(null);
        if (attackFromTerritoryWithEnemyNeighbors != null) {
          ProLogger.debug(
              "Removing neutral territory that has attackers that are adjacent to enemies: "
                  + t.getName()
                  + ", attackFromTerritory="
                  + attackFromTerritoryWithEnemyNeighbors);
          it.remove();
        }
      }
    }
  }

  private List<Unit> moveOneDefenderToLandTerritoriesBorderingEnemy(
      final List<ProTerritory> prioritizedTerritories) {

    ProLogger.info("Determine which territories to defend with one land unit");

    final Map<Unit, Set<Territory>> unitMoveMap =
        territoryManager.getAttackOptions().getUnitMoveMap();

    // Get list of territories to attack
    final List<Territory> territoriesToAttack = new ArrayList<>();
    for (final ProTerritory patd : prioritizedTerritories) {
      territoriesToAttack.add(patd.getTerritory());
    }

    // Find land territories without units and adjacent to enemy land units
    final List<Unit> alreadyMovedUnits = new ArrayList<>();
    for (final Territory t : proData.getMyUnitTerritories()) {
      final boolean hasAlliedLandUnits =
          t.anyUnitsMatch(ProMatches.unitCantBeMovedAndIsAlliedDefenderAndNotInfra(player, t));
      final Set<Territory> enemyNeighbors =
          data.getMap()
              .getNeighbors(
                  t,
                  Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(
                      player,
                      Matches.unitIsLand()
                          .and(Matches.unitIsNotInfrastructure())
                          .and(Matches.unitCanMove())));
      enemyNeighbors.removeAll(territoriesToAttack);
      if (!t.isWater() && !hasAlliedLandUnits && !enemyNeighbors.isEmpty()) {
        int minCost = Integer.MAX_VALUE;
        Unit minUnit = null;
        for (final Unit u :
            t.getMatches(Matches.unitIsOwnedBy(player).and(Matches.unitIsNotInfrastructure()))) {
          if (proData.getUnitValue(u.getType()) < minCost) {
            minCost = proData.getUnitValue(u.getType());
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

    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();
    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Find maximum defenders for each transport territory
    final List<Territory> clearedTerritories =
        attackMap.entrySet().stream()
            .filter(e -> !e.getValue().getUnits().isEmpty())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    territoryManager.populateDefenseOptions(clearedTerritories);
    final Map<Territory, ProTerritory> defendMap =
        territoryManager.getDefendOptions().getTerritoryMap();

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
    for (final Map.Entry<Territory, ProTerritory> attackEntry : attackMap.entrySet()) {
      final Territory t = attackEntry.getKey();
      final ProTerritory patd = attackEntry.getValue();
      ProLogger.debug(
          "Checking territory="
              + patd.getTerritory().getName()
              + " with transport size="
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

        // Determine counter-attack results for each transport territory
        double enemyTuvSwing = 0.0;
        for (final Territory unloadTerritory : territoryTransportAndBombardMap.keySet()) {
          if (enemyAttackOptions.getMax(unloadTerritory) != null) {
            final Set<Unit> defenders =
                new HashSet<>(unloadTerritory.getMatches(ProMatches.unitIsAlliedNotOwned(player)));
            defenders.addAll(territoryTransportAndBombardMap.get(unloadTerritory));
            if (defendMap.get(unloadTerritory) != null) {
              defenders.addAll(defendMap.get(unloadTerritory).getMaxUnits());
            }
            final Set<Unit> enemyAttackers =
                enemyAttackOptions.getMax(unloadTerritory).getMaxUnits();
            final ProBattleResult result =
                calc.calculateBattleResults(
                    proData, unloadTerritory, enemyAttackers, defenders, List.of());
            final ProBattleResult minResult =
                calc.calculateBattleResults(
                    proData,
                    unloadTerritory,
                    enemyAttackOptions.getMax(unloadTerritory).getMaxUnits(),
                    territoryTransportAndBombardMap.get(unloadTerritory),
                    List.of());
            final double minTuvSwing = Math.min(result.getTuvSwing(), minResult.getTuvSwing());
            if (minTuvSwing > 0) {
              enemyTuvSwing += minTuvSwing;
            }
            ProLogger.trace(
                unloadTerritory
                    + ", EnemyAttackers="
                    + enemyAttackers.size()
                    + ", MaxDefenders="
                    + defenders.size()
                    + ", MaxEnemyTUVSwing="
                    + result.getTuvSwing()
                    + ", MinDefenders="
                    + territoryTransportAndBombardMap.get(unloadTerritory).size()
                    + ", MinEnemyTUVSwing="
                    + minResult.getTuvSwing());
          } else {
            ProLogger.trace("Territory=" + unloadTerritory.getName() + " has no enemy attackers");
          }
        }

        // Determine whether its worth attacking
        final ProBattleResult result =
            calc.calculateBattleResults(
                proData,
                t,
                patd.getUnits(),
                patd.getMaxEnemyDefenders(player),
                patd.getBombardTerritoryMap().keySet());
        int production = 0;
        int isEnemyCapital = 0;
        final TerritoryAttachment ta = TerritoryAttachment.get(t);
        if (ta != null) {
          production = ta.getProduction();
          if (ta.isCapital()) {
            isEnemyCapital = 1;
          }
        }
        final double attackValue = result.getTuvSwing() + production * (1 + 3.0 * isEnemyCapital);
        if (!patd.isStrafing() && (0.75 * enemyTuvSwing) > attackValue) {
          ProLogger.debug(
              "Removing amphib territory: "
                  + patd.getTerritory()
                  + ", enemyTUVSwing="
                  + enemyTuvSwing
                  + ", attackValue="
                  + attackValue);
          patd.getUnits().clear();
          patd.getAmphibAttackMap().clear();
          patd.getBombardTerritoryMap().clear();
        } else {
          ProLogger.debug(
              "Keeping amphib territory: "
                  + patd.getTerritory()
                  + ", enemyTUVSwing="
                  + enemyTuvSwing
                  + ", attackValue="
                  + attackValue);
        }
      }
    }
  }

  private void determineUnitsToAttackWith(
      final List<ProTerritory> prioritizedTerritories, final List<Unit> alreadyMovedUnits) {

    ProLogger.info("Determine units to attack each territory with");

    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();
    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    final Map<Unit, Set<Territory>> unitAttackMap =
        territoryManager.getAttackOptions().getUnitMoveMap();

    // Assign units to territories by prioritization
    while (true) {
      Map<Unit, Set<Territory>> sortedUnitAttackOptions =
          tryToAttackTerritories(prioritizedTerritories, alreadyMovedUnits);

      // Clear bombers
      attackMap.values().forEach(proTerritory -> proTerritory.getBombers().clear());

      // Get all units that have already moved
      final Set<Unit> alreadyAttackedWithUnits = new HashSet<>();
      for (final ProTerritory t : attackMap.values()) {
        alreadyAttackedWithUnits.addAll(t.getUnits());
        alreadyAttackedWithUnits.addAll(t.getAmphibAttackMap().keySet());
      }

      // Check to see if any territories can be bombed
      determineTerritoriesThatCanBeBombed(
          attackMap, sortedUnitAttackOptions, alreadyAttackedWithUnits);

      // Re-sort attack options
      sortedUnitAttackOptions =
          ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(
              proData, player, sortedUnitAttackOptions, attackMap, calc);
      final List<Unit> addedUnits = new ArrayList<>();

      // Set air units in any territory with no AA (don't move planes to empty territories)
      for (final Unit unit : sortedUnitAttackOptions.keySet()) {
        final boolean isAirUnit = unit.getUnitAttachment().isAir();
        if (!isAirUnit) {
          continue; // skip non-air units
        }
        Territory minWinTerritory = null;
        double minWinPercentage = Double.MAX_VALUE;
        for (final Territory t : sortedUnitAttackOptions.get(unit)) {
          final ProTerritory patd = attackMap.get(t);

          // Check if air unit should avoid this territory due to no guaranteed safe landing
          // location
          final boolean isEnemyFactory =
              ProMatches.territoryHasInfraFactoryAndIsEnemyLand(player).test(t);
          if (!isEnemyFactory && !canAirSafelyLandAfterAttack(unit, t)) {
            continue;
          }
          if (patd.getBattleResult() == null) {
            patd.estimateBattleResult(calc, player);
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            final List<Unit> attackingUnits = patd.getUnits();
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
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
          attackMap.get(minWinTerritory).setBattleResult(null);
          attackMap.get(minWinTerritory).addUnit(unit);
          addedUnits.add(unit);
        }
      }
      sortedUnitAttackOptions.keySet().removeAll(addedUnits);

      // Re-sort attack options
      sortedUnitAttackOptions =
          ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(
              proData, player, sortedUnitAttackOptions, attackMap, calc);

      // Find territory that we can try to hold that needs unit
      for (final Unit unit : sortedUnitAttackOptions.keySet()) {
        if (addedUnits.contains(unit)) {
          continue;
        }
        Territory minWinTerritory = null;
        for (final Territory t : sortedUnitAttackOptions.get(unit)) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.isCanHold()) {

            // Check if I already have enough attack units to win in 2 rounds
            if (patd.getBattleResult() == null) {
              patd.estimateBattleResult(calc, player);
            }
            final ProBattleResult result = patd.getBattleResult();
            final List<Unit> attackingUnits = patd.getUnits();
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, attackingUnits, defendingUnits);
            if (!isOverwhelmingWin && result.getBattleRounds() > 2) {
              minWinTerritory = t;
              break;
            }
          }
        }
        if (minWinTerritory != null) {
          attackMap.get(minWinTerritory).setBattleResult(null);
          final List<Unit> unitsToAdd =
              ProTransportUtils.getUnitsToAdd(proData, unit, alreadyMovedUnits, attackMap);
          attackMap.get(minWinTerritory).addUnits(unitsToAdd);
          addedUnits.addAll(unitsToAdd);
        }
      }
      sortedUnitAttackOptions.keySet().removeAll(addedUnits);

      // Re-sort attack options
      sortedUnitAttackOptions =
          ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(
              proData, player, sortedUnitAttackOptions, attackMap, calc);

      // Add sea units to any territory that significantly increases TUV gain
      for (final Unit unit : sortedUnitAttackOptions.keySet()) {
        final boolean isSeaUnit = unit.getUnitAttachment().isSea();
        if (!isSeaUnit) {
          continue; // skip non-sea units
        }
        for (final Territory t : sortedUnitAttackOptions.get(unit)) {
          final ProTerritory patd = attackMap.get(t);
          if (patd.getBattleResult() == null) {
            patd.estimateBattleResult(calc, player);
          }
          final ProBattleResult result = patd.getBattleResult();
          final List<Unit> attackers = new ArrayList<>(patd.getUnits());
          attackers.add(unit);
          final ProBattleResult result2 =
              calc.estimateAttackBattleResults(
                  proData,
                  t,
                  attackers,
                  patd.getMaxEnemyDefenders(player),
                  patd.getBombardTerritoryMap().keySet());
          final double unitValue = proData.getUnitValue(unit.getType());
          if ((result2.getTuvSwing() - unitValue / 3) > result.getTuvSwing()) {
            patd.setBattleResult(null);
            patd.addUnit(unit);
            addedUnits.add(unit);
            break;
          }
        }
      }
      sortedUnitAttackOptions.keySet().removeAll(addedUnits);

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
          patd.estimateBattleResult(calc, player);
        }
        final ProBattleResult result = patd.getBattleResult();

        // Determine enemy counter-attack results
        boolean canHold = true;
        double enemyCounterTuvSwing = 0;
        if (enemyAttackOptions.getMax(t) != null
            && !ProMatches.territoryIsWaterAndAdjacentToOwnedFactory(player).test(t)) {
          List<Unit> remainingUnitsToDefendWith =
              CollectionUtils.getMatches(
                  result.getAverageAttackersRemaining(), Matches.unitIsAir().negate());
          ProBattleResult result2 =
              calc.calculateBattleResults(
                  proData,
                  t,
                  patd.getMaxEnemyUnits(),
                  remainingUnitsToDefendWith,
                  patd.getMaxBombardUnits());
          if (patd.isCanHold() && result2.getTuvSwing() > 0) {
            final List<Unit> unusedUnits = new ArrayList<>(patd.getMaxUnits());
            unusedUnits.addAll(patd.getMaxAmphibUnits());
            unusedUnits.removeAll(usedUnits);
            unusedUnits.addAll(remainingUnitsToDefendWith);
            final ProBattleResult result3 =
                calc.calculateBattleResults(
                    proData, t, patd.getMaxEnemyUnits(), unusedUnits, patd.getMaxBombardUnits());
            if (result3.getTuvSwing() < result2.getTuvSwing()) {
              result2 = result3;
              remainingUnitsToDefendWith = unusedUnits;
            }
          }
          canHold =
              (!result2.isHasLandUnitRemaining() && !t.isWater())
                  || (result2.getTuvSwing() < 0)
                  || (result2.getWinPercentage() < proData.getMinWinPercentage());
          if (result2.getTuvSwing() > 0) {
            enemyCounterTuvSwing = result2.getTuvSwing();
          }
          ProLogger.trace(
              "Territory="
                  + t.getName()
                  + ", CanHold="
                  + canHold
                  + ", MyDefenders="
                  + remainingUnitsToDefendWith.size()
                  + ", EnemyAttackers="
                  + patd.getMaxEnemyUnits().size()
                  + ", win%="
                  + result2.getWinPercentage()
                  + ", EnemyTUVSwing="
                  + result2.getTuvSwing()
                  + ", hasLandUnitRemaining="
                  + result2.isHasLandUnitRemaining());
        }

        // Find attack value
        final boolean isNeutral = ProUtils.isNeutralLand(t);
        final int isLand = !t.isWater() ? 1 : 0;
        final int isCanHold = canHold ? 1 : 0;
        final int isCantHoldAmphib = !canHold && !patd.getAmphibAttackMap().isEmpty() ? 1 : 0;
        final int isFactory = ProMatches.territoryHasInfraFactoryAndIsLand().test(t) ? 1 : 0;
        int capturableUnits = 0;
        if (Matches.territoryIsLand().test(t)) {
          capturableUnits =
              t.getUnitCollection()
                  .countMatches(Matches.unitCanBeCapturedOnEnteringThisTerritory(player, t));
        }
        final int isFfa = ProUtils.isFfa(data, player) ? 1 : 0;
        final int production = TerritoryAttachment.getProduction(t);
        double capitalValue = 0;
        final TerritoryAttachment ta = TerritoryAttachment.get(t);
        if (ta != null && ta.isCapital()) {
          capitalValue = ProUtils.getPlayerProduction(t.getOwner(), data);
        }
        final double territoryValue =
            (1
                        + isLand
                        - isCantHoldAmphib
                        + isFactory
                        + isCanHold * (1 + 2.0 * isFfa + 1.5 * isFactory + 0.5 * capturableUnits))
                    * production
                + capitalValue;
        double tuvSwing = result.getTuvSwing();
        if (isFfa == 1 && tuvSwing > 0) {
          tuvSwing *= 0.5;
        }
        final double attackValue =
            1
                + tuvSwing
                + territoryValue * result.getWinPercentage() / 100
                - enemyCounterTuvSwing * 2 / 3;
        boolean allUnitsCanAttackOtherTerritory = true;
        if (isNeutral && attackValue < 0) {
          for (final Unit u : patd.getUnits()) {
            boolean canAttackOtherTerritory = false;
            for (final ProTerritory patd2 : prioritizedTerritories) {
              if (!patd.equals(patd2)
                  && unitAttackMap.get(u) != null
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
        if (!patd.isStrafing()
            && (result.getWinPercentage() < proData.getMinWinPercentage()
                || !result.isHasLandUnitRemaining()
                || (isNeutral && !canHold)
                || (attackValue < 0
                    && (!isNeutral
                        || allUnitsCanAttackOtherTerritory
                        || result.getBattleRounds() >= 4)))) {
          territoryToRemove = patd;
        }
        ProLogger.debug(
            patd.getResultString()
                + ", attackValue="
                + attackValue
                + ", territoryValue="
                + territoryValue
                + ", allUnitsCanAttackOtherTerritory="
                + allUnitsCanAttackOtherTerritory
                + " with attackers="
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

  private void determineTerritoriesThatCanBeBombed(
      final Map<Territory, ProTerritory> attackMap,
      final Map<Unit, Set<Territory>> sortedUnitAttackOptions,
      final Set<Unit> alreadyAttackedWithUnits) {
    final boolean raidsMayBePrecededByAirBattles =
        Properties.getRaidsMayBePreceededByAirBattles(data.getProperties());
    for (final Map.Entry<Unit, Set<Territory>> bomberEntry :
        territoryManager.getAttackOptions().getBomberMoveMap().entrySet()) {
      final Unit bomber = bomberEntry.getKey();
      if (alreadyAttackedWithUnits.contains(bomber)) {
        return; // already attacked bombers cannot move
      }
      Collection<Territory> bomberTargetTerritories = bomberEntry.getValue();
      if (raidsMayBePrecededByAirBattles) {
        // Avoid territories with the potential of air battles
        bomberTargetTerritories =
            CollectionUtils.getMatches(
                bomberTargetTerritories,
                terr ->
                    !AirBattle.territoryCouldPossiblyHaveAirBattleDefenders(
                        terr, player, data, true));
      }
      determineBestBombingAttackForBomber(
          attackMap, sortedUnitAttackOptions, bomberTargetTerritories, bomber);
    }
  }

  /**
   * Determines the best bombing attack territory for a single {@code bomber}.
   *
   * @param attackMap Attack map
   * @param sortedUnitAttackOptions Sorted attack options
   * @param bomberTargetTerritories Target territories
   * @param bomber Bomber unit
   */
  private void determineBestBombingAttackForBomber(
      final Map<Territory, ProTerritory> attackMap,
      final Map<Unit, Set<Territory>> sortedUnitAttackOptions,
      final Collection<Territory> bomberTargetTerritories,
      final Unit bomber) {
    final Predicate<Unit> bombingTargetMatch =
        Matches.unitCanProduceUnitsAndCanBeDamaged()
            .and(Matches.unitIsLegalBombingTargetBy(bomber));
    Optional<Territory> maxBombingTerritory = Optional.empty();
    int maxBombingScore = MIN_BOMBING_SCORE;
    for (final Territory t : bomberTargetTerritories) {
      final List<Unit> targetUnits = t.getMatches(bombingTargetMatch);
      if (!targetUnits.isEmpty() && canAirSafelyLandAfterAttack(bomber, t)) {
        final int noAaBombingDefense =
            t.anyUnitsMatch(Matches.unitIsAaForBombingThisUnitOnly()) ? 0 : 1;
        int maxDamageProduction = TerritoryAttachment.getProduction(t);
        // determine damage need for unit (if property allows) and number of same target bombers
        int neededDamageUnits = 0;
        int sameTargetBombersCount = 0;
        final List<Unit> existingAttackingBombers = attackMap.get(t).getBombers();
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
          final Set<Unit> sameTargetBombers = new HashSet<>();
          for (final Unit target : targetUnits) {
            neededDamageUnits += target.getHowMuchMoreDamageCanThisUnitTake(t);
            final Predicate<Unit> canBombTarget =
                u -> Matches.unitIsLegalBombingTargetBy(u).test(target);
            sameTargetBombers.addAll(
                CollectionUtils.getMatches(existingAttackingBombers, canBombTarget));
          }
          sameTargetBombersCount = sameTargetBombers.size();
        } else {
          sameTargetBombersCount = existingAttackingBombers.size();
        }
        // assume each other bomber causes a damage of 3
        final int remainingDamagePotential =
            maxDamageProduction + neededDamageUnits - 3 * sameTargetBombersCount;
        final int bombingScore = (1 + 9 * noAaBombingDefense) * remainingDamagePotential;
        if (bombingScore >= maxBombingScore) {
          maxBombingScore = bombingScore;
          maxBombingTerritory = Optional.of(t);
        }
      }
    }
    if (maxBombingTerritory.isPresent()) {
      final Territory t = maxBombingTerritory.get();
      attackMap.get(t).getBombers().add(bomber);
      sortedUnitAttackOptions.remove(bomber);
      ProLogger.debug("Add bomber (" + bomber + ") to " + t);
    }
  }

  private Map<Unit, Set<Territory>> tryToAttackTerritories(
      final List<ProTerritory> prioritizedTerritories, final List<Unit> alreadyMovedUnits) {

    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();
    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    final Map<Unit, Set<Territory>> unitAttackMap =
        territoryManager.getAttackOptions().getUnitMoveMap();
    final Map<Unit, Set<Territory>> transportAttackMap =
        territoryManager.getAttackOptions().getTransportMoveMap();
    final Map<Unit, Set<Territory>> bombardMap =
        territoryManager.getAttackOptions().getBombardMap();
    final List<ProTransport> transportMapList =
        territoryManager.getAttackOptions().getTransportList();

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
    for (final Map.Entry<Unit, Set<Territory>> unitAttackEntry : unitAttackMap.entrySet()) {

      // Find number of attack options
      final Set<Territory> canAttackTerritories = new HashSet<>();
      for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
        if (unitAttackEntry.getValue().contains(attackTerritoryData.getTerritory())) {
          canAttackTerritories.add(attackTerritoryData.getTerritory());
        }
      }

      // Add units with attack options to map
      if (!canAttackTerritories.isEmpty()) {
        unitAttackOptions.put(unitAttackEntry.getKey(), canAttackTerritories);
      }
    }

    // Sort units by number of attack options and cost
    Map<Unit, Set<Territory>> sortedUnitAttackOptions =
        ProSortMoveOptionsUtils.sortUnitMoveOptions(proData, unitAttackOptions);
    final List<Unit> addedUnits = new ArrayList<>();

    // Try to set at least one destroyer in each sea territory with subs
    for (final Unit unit : sortedUnitAttackOptions.keySet()) {
      final boolean isDestroyerUnit = unit.getUnitAttachment().isDestroyer();
      if (!isDestroyerUnit) {
        continue; // skip non-destroyer units
      }
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {

        // Add destroyer if territory has subs and a destroyer has been already added
        final ProTerritory patd = attackMap.get(t);
        final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
        if (defendingUnits.stream().anyMatch(Matches.unitHasSubBattleAbilities())
            && patd.getUnits().stream().noneMatch(Matches.unitIsDestroyer())) {
          patd.addUnit(unit);
          addedUnits.add(unit);
          break;
        }
      }
    }
    sortedUnitAttackOptions.keySet().removeAll(addedUnits);

    // Set enough land and sea units in territories to have at least a chance of winning
    for (final Unit unit : sortedUnitAttackOptions.keySet()) {
      final boolean isAirUnit = unit.getUnitAttachment().isAir();
      final boolean isExpensiveLandUnit =
          Matches.unitIsLand().test(unit)
              && proData.getUnitValue(unit.getType()) > 2 * proData.getMinCostPerHitPoint();
      if (isAirUnit || isExpensiveLandUnit || addedUnits.contains(unit)) {
        continue; // skip air and expensive units
      }
      final TreeMap<Double, Territory> estimatesMap = new TreeMap<>();
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory proTerritory = attackMap.get(t);
        if (t.isWater() && !proTerritory.isCanHold()) {
          continue; // ignore sea territories that can't be held
        }
        final List<Unit> defendingUnits = proTerritory.getMaxEnemyDefenders(player);
        double estimate =
            ProBattleUtils.estimateStrengthDifference(t, proTerritory.getUnits(), defendingUnits);
        final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
        if (hasAa) {
          estimate -= 10;
        }
        estimatesMap.put(estimate, t);
      }
      if (!estimatesMap.isEmpty() && estimatesMap.firstKey() < 40) {
        final Territory minWinTerritory =
            CollectionUtils.getAny(estimatesMap.entrySet()).getValue();
        final List<Unit> unitsToAdd =
            ProTransportUtils.getUnitsToAdd(proData, unit, alreadyMovedUnits, attackMap);
        attackMap.get(minWinTerritory).addUnits(unitsToAdd);
        addedUnits.addAll(unitsToAdd);
      }
    }
    sortedUnitAttackOptions.keySet().removeAll(addedUnits);

    // Re-sort attack options
    sortedUnitAttackOptions =
        ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(
            proData, player, sortedUnitAttackOptions, attackMap, calc);

    // Set non-air units in territories that can be held
    for (final Unit unit : sortedUnitAttackOptions.keySet()) {
      final boolean isAirUnit = unit.getUnitAttachment().isAir();
      if (isAirUnit || addedUnits.contains(unit)) {
        continue; // skip air units
      }
      Territory minWinTerritory = null;
      double minWinPercentage = proData.getWinPercentage();
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins() && patd.isCanHold()) {
          if (patd.getBattleResult() == null) {
            patd.estimateBattleResult(calc, player);
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            minWinPercentage = result.getWinPercentage();
            minWinTerritory = t;
          }
        }
      }
      if (minWinTerritory != null) {
        attackMap.get(minWinTerritory).setBattleResult(null);
        final List<Unit> unitsToAdd =
            ProTransportUtils.getUnitsToAdd(proData, unit, alreadyMovedUnits, attackMap);
        attackMap.get(minWinTerritory).addUnits(unitsToAdd);
        addedUnits.addAll(unitsToAdd);
      }
    }
    addedUnits.forEach(sortedUnitAttackOptions.keySet()::remove);

    // Re-sort attack options
    sortedUnitAttackOptions =
        ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(
            proData, player, sortedUnitAttackOptions, attackMap, calc);

    // Set air units in territories that can't be held (don't move planes to empty territories)
    for (final Unit unit : sortedUnitAttackOptions.keySet()) {
      final boolean isAirUnit = unit.getUnitAttachment().isAir();
      if (!isAirUnit) {
        continue; // skip non-air units
      }
      Territory minWinTerritory = null;
      double minWinPercentage = proData.getWinPercentage();
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins() && !patd.isCanHold()) {

          // Check if air unit should avoid this territory due to no guaranteed safe landing
          // location
          final boolean isEnemyCapital = ProUtils.getLiveEnemyCapitals(data, player).contains(t);
          final boolean isAdjacentToAlliedCapital =
              Matches.territoryHasNeighborMatching(
                      data.getMap(), ProUtils.getLiveAlliedCapitals(data, player)::contains)
                  .test(t);
          final int range = unit.getMovementLeft().intValue();
          final int distance =
              data.getMap()
                  .getDistanceIgnoreEndForCondition(
                      proData.getUnitTerritory(unit),
                      t,
                      ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, true));
          final boolean usesMoreThanHalfOfRange = distance > range / 2;
          if (!isEnemyCapital && !isAdjacentToAlliedCapital && usesMoreThanHalfOfRange) {
            continue;
          }

          // Check battle results
          if (patd.getBattleResult() == null) {
            patd.estimateBattleResult(calc, player);
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
            final boolean hasNoDefenders =
                defendingUnits.stream().noneMatch(ProMatches.unitIsEnemyAndNotInfa(player));
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, patd.getUnits(), defendingUnits);
            final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
            if (!hasNoDefenders
                && !isOverwhelmingWin
                && (!hasAa || result.getWinPercentage() < minWinPercentage)) {
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
        attackMap.get(minWinTerritory).setBattleResult(null);
        attackMap.get(minWinTerritory).addUnit(unit);
        addedUnits.add(unit);
      }
    }
    sortedUnitAttackOptions.keySet().removeAll(addedUnits);

    // Re-sort attack options
    sortedUnitAttackOptions =
        ProSortMoveOptionsUtils.sortUnitNeededOptionsThenAttack(
            proData, player, sortedUnitAttackOptions, attackMap, calc);

    // Set remaining units in any territory that needs it (don't move planes to empty territories)
    for (final Unit unit : sortedUnitAttackOptions.keySet()) {
      if (addedUnits.contains(unit)) {
        continue;
      }
      final boolean isAirUnit = unit.getUnitAttachment().isAir();
      Territory minWinTerritory = null;
      double minWinPercentage = proData.getWinPercentage();
      for (final Territory t : sortedUnitAttackOptions.get(unit)) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins()) {

          // Check if air unit should avoid this territory due to no guaranteed safe landing
          // location
          final boolean isAdjacentToAlliedFactory =
              Matches.territoryHasNeighborMatching(
                      data.getMap(), ProMatches.territoryHasInfraFactoryAndIsAlliedLand(player))
                  .test(t);
          final int range = unit.getMovementLeft().intValue();
          final int distance =
              data.getMap()
                  .getDistanceIgnoreEndForCondition(
                      proData.getUnitTerritory(unit),
                      t,
                      ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, true));
          final boolean usesMoreThanHalfOfRange = distance > range / 2;
          final boolean territoryValueIsLessThanUnitValue =
              patd.getValue() < proData.getUnitValue(unit.getType());
          if (isAirUnit
              && !isAdjacentToAlliedFactory
              && usesMoreThanHalfOfRange
              && (territoryValueIsLessThanUnitValue || (!t.isWater() && !patd.isCanHold()))) {
            continue;
          }
          if (patd.getBattleResult() == null) {
            patd.estimateBattleResult(calc, player);
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {
            final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
            final boolean hasNoDefenders =
                defendingUnits.stream().noneMatch(ProMatches.unitIsEnemyAndNotInfa(player));
            final boolean isOverwhelmingWin =
                ProBattleUtils.checkForOverwhelmingWin(t, patd.getUnits(), defendingUnits);
            final boolean hasAa = defendingUnits.stream().anyMatch(Matches.unitIsAaForAnything());
            if (!isAirUnit
                || (!hasNoDefenders
                    && !isOverwhelmingWin
                    && (!hasAa || result.getWinPercentage() < minWinPercentage))) {
              minWinPercentage = result.getWinPercentage();
              minWinTerritory = t;
            }
          }
        }
      }
      if (minWinTerritory != null) {
        attackMap.get(minWinTerritory).setBattleResult(null);
        final List<Unit> unitsToAdd =
            ProTransportUtils.getUnitsToAdd(proData, unit, alreadyMovedUnits, attackMap);
        attackMap.get(minWinTerritory).addUnits(unitsToAdd);
        addedUnits.addAll(unitsToAdd);
      }
    }
    sortedUnitAttackOptions.keySet().removeAll(addedUnits);

    // Re-sort attack options
    sortedUnitAttackOptions =
        ProSortMoveOptionsUtils.sortUnitNeededOptions(
            proData, player, sortedUnitAttackOptions, attackMap, calc);

    // If transports can take casualties try placing in naval battles first
    final List<Unit> alreadyAttackedWithTransports = new ArrayList<>();
    if (!Properties.getTransportCasualtiesRestricted(data.getProperties())) {

      // Loop through all my transports and see which territories they can attack from current list
      final Map<Unit, Set<Territory>> transportAttackOptions = new HashMap<>();
      for (final Map.Entry<Unit, Set<Territory>> transportAttackEntry :
          transportAttackMap.entrySet()) {

        // Find number of attack options
        final Set<Territory> canAttackTerritories = new HashSet<>();
        for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
          if (transportAttackEntry.getValue().contains(attackTerritoryData.getTerritory())) {
            canAttackTerritories.add(attackTerritoryData.getTerritory());
          }
        }
        if (!canAttackTerritories.isEmpty()) {
          transportAttackOptions.put(transportAttackEntry.getKey(), canAttackTerritories);
        }
      }

      // Loop through transports with attack options and determine if any naval battle needs it
      for (final Map.Entry<Unit, Set<Territory>> transportAttackOptionsEntry :
          transportAttackOptions.entrySet()) {
        final Unit transport = transportAttackOptionsEntry.getKey();
        // Find current naval battle that needs transport if it isn't transporting units
        for (final Territory t : transportAttackOptionsEntry.getValue()) {
          final ProTerritory patd = attackMap.get(t);
          final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
          if (!patd.isCurrentlyWins()
              && !transport.isTransporting(proData.getUnitTerritory(transport))
              && !defendingUnits.isEmpty()) {
            if (patd.getBattleResult() == null) {
              patd.estimateBattleResult(calc, player);
            }
            final ProBattleResult result = patd.getBattleResult();
            if (result.getWinPercentage() < proData.getWinPercentage()
                || !result.isHasLandUnitRemaining()) {
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
    for (final Map.Entry<Unit, Set<Territory>> amphibAttackOptionsEntry :
        amphibAttackOptions.entrySet()) {
      final Unit transport = amphibAttackOptionsEntry.getKey();
      // Find current land battle results for territories that unit can amphib attack
      Territory minWinTerritory = null;
      double minWinPercentage = proData.getWinPercentage();
      List<Unit> minAmphibUnitsToAdd = null;
      Territory minUnloadFromTerritory = null;
      for (final Territory t : amphibAttackOptionsEntry.getValue()) {
        final ProTerritory patd = attackMap.get(t);
        if (!patd.isCurrentlyWins()) {
          if (patd.getBattleResult() == null) {
            patd.estimateBattleResult(calc, player);
          }
          final ProBattleResult result = patd.getBattleResult();
          if (result.getWinPercentage() < minWinPercentage
              || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {

            // Find units that haven't attacked and can be transported
            final Set<Unit> alreadyAttackedWithUnits =
                ProTransportUtils.getMovedUnits(alreadyMovedUnits, attackMap);
            for (final ProTransport proTransportData : transportMapList) {
              if (proTransportData.getTransport().equals(transport)) {

                // Find units to load
                final Set<Territory> territoriesCanLoadFrom =
                    proTransportData.getTransportMap().get(t);
                final List<Unit> amphibUnitsToAdd =
                    ProTransportUtils.getUnitsToTransportFromTerritories(
                        player, transport, territoriesCanLoadFrom, alreadyAttackedWithUnits);
                if (amphibUnitsToAdd.isEmpty()) {
                  continue;
                }

                // Find the best territory to move transport
                double minStrengthDifference = Double.POSITIVE_INFINITY;
                minUnloadFromTerritory = null;
                final Set<Territory> territoriesToMoveTransport =
                    data.getMap()
                        .getNeighbors(t, ProMatches.territoryCanMoveSeaUnits(player, false));
                final Set<Territory> loadFromTerritories = new HashSet<>();
                for (final Unit u : amphibUnitsToAdd) {
                  loadFromTerritories.add(proData.getUnitTerritory(u));
                }
                for (final Territory destination : territoriesToMoveTransport) {
                  if (proTransportData.getSeaTransportMap().containsKey(destination)
                      && proTransportData
                          .getSeaTransportMap()
                          .get(destination)
                          .containsAll(loadFromTerritories)) {
                    Set<Unit> attackers = Set.of();
                    if (enemyAttackOptions.getMax(destination) != null) {
                      attackers = enemyAttackOptions.getMax(destination).getMaxUnits();
                    }
                    final List<Unit> defenders =
                        destination.getMatches(Matches.isUnitAllied(player));
                    defenders.add(transport);
                    final double strengthDifference =
                        ProBattleUtils.estimateStrengthDifference(
                            destination, attackers, defenders);
                    if (strengthDifference <= minStrengthDifference) {
                      minStrengthDifference = strengthDifference;
                      minUnloadFromTerritory = destination;
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
          attackMap
              .get(minWinTerritory)
              .getTransportTerritoryMap()
              .put(transport, minUnloadFromTerritory);
        }
        attackMap.get(minWinTerritory).addUnits(minAmphibUnitsToAdd);
        attackMap.get(minWinTerritory).putAmphibAttackMap(transport, minAmphibUnitsToAdd);
        attackMap.get(minWinTerritory).setBattleResult(null);
        for (final Unit unit : minAmphibUnitsToAdd) {
          sortedUnitAttackOptions.remove(unit);
        }
        ProLogger.trace(
            "Adding amphibious attack to "
                + minWinTerritory
                + ", units="
                + minAmphibUnitsToAdd.size()
                + ", unloadFrom="
                + minUnloadFromTerritory);
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
        final List<Unit> defendingUnits = patd.getMaxEnemyDefenders(player);
        final boolean hasDefenders =
            defendingUnits.stream().anyMatch(Matches.unitIsInfrastructure().negate());
        if (bombardMap.get(u).contains(patd.getTerritory())
            && !patd.getTransportTerritoryMap().isEmpty()
            && hasDefenders
            && !u.isTransporting(proData.getUnitTerritory(u))) {
          canBombardTerritories.add(patd.getTerritory());
        }
      }
      if (!canBombardTerritories.isEmpty()) {
        bombardOptions.put(u, canBombardTerritories);
      }
    }

    // Loop through bombard units to see if any amphib battles need
    for (final Map.Entry<Unit, Set<Territory>> bombardOptionsEntry : bombardOptions.entrySet()) {
      final Unit unit = bombardOptionsEntry.getKey();
      // Find current land battle results for territories that unit can bombard
      Territory minWinTerritory = null;
      double minWinPercentage = Double.MAX_VALUE;
      Territory minBombardFromTerritory = null;
      for (final Territory t : bombardOptionsEntry.getValue()) {
        final ProTerritory patd = attackMap.get(t);
        if (patd.getBattleResult() == null) {
          patd.estimateBattleResult(calc, player);
        }
        final ProBattleResult result = patd.getBattleResult();
        if (result.getWinPercentage() < minWinPercentage
            || (!result.isHasLandUnitRemaining() && minWinTerritory == null)) {

          // Find territory to bombard from
          Territory bombardFromTerritory = null;
          for (final Territory unloadFromTerritory : patd.getTransportTerritoryMap().values()) {
            if (patd.getBombardOptionsMap().get(unit).contains(unloadFromTerritory)) {
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
        attackMap.get(minWinTerritory).getBombardTerritoryMap().put(unit, minBombardFromTerritory);
        attackMap.get(minWinTerritory).setBattleResult(null);
        sortedUnitAttackOptions.remove(unit);
        ProLogger.trace(
            "Adding bombard to "
                + minWinTerritory
                + ", units="
                + unit
                + ", bombardFrom="
                + minBombardFromTerritory);
      }
    }
    return sortedUnitAttackOptions;
  }

  private void removeAttacksUntilCapitalCanBeHeld(
      final List<ProTerritory> prioritizedTerritories,
      final List<ProPurchaseOption> landPurchaseOptions) {

    ProLogger.info("Check capital defenses after attack moves");

    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();

    final Territory myCapital = proData.getMyCapital();

    // Add max purchase defenders to capital for non-mobile factories (don't consider mobile
    // factories since they may move elsewhere)
    final List<Unit> placeUnits = new ArrayList<>();
    if (ProMatches.territoryHasNonMobileFactoryAndIsNotConqueredOwnedLand(player).test(myCapital)) {
      placeUnits.addAll(
          ProPurchaseUtils.findMaxPurchaseDefenders(
              proData, player, myCapital, landPurchaseOptions));
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
      territoryManager.populateEnemyAttackOptions(territoriesToAttack, List.of(myCapital));
      final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
      if (enemyAttackOptions.getMax(myCapital) == null) {
        break;
      }

      // Find max remaining defenders
      final Set<Territory> territoriesAdjacentToCapital =
          data.getMap().getNeighbors(myCapital, Matches.territoryIsLand());
      final List<Unit> defenders = myCapital.getMatches(Matches.isUnitAllied(player));
      defenders.addAll(placeUnits);
      for (final Territory t : territoriesAdjacentToCapital) {
        defenders.addAll(t.getMatches(ProMatches.unitCanBeMovedAndIsOwnedLand(player, false)));
      }
      for (final ProTerritory t : attackMap.values()) {
        defenders.removeAll(t.getUnits());
      }

      // Determine counter-attack results to see if I can hold it
      final Set<Unit> enemyAttackingUnits =
          new HashSet<>(enemyAttackOptions.getMax(myCapital).getMaxUnits());
      enemyAttackingUnits.addAll(enemyAttackOptions.getMax(myCapital).getMaxAmphibUnits());
      final ProBattleResult result =
          calc.estimateDefendBattleResults(
              proData,
              myCapital,
              enemyAttackingUnits,
              defenders,
              enemyAttackOptions.getMax(myCapital).getMaxBombardUnits());
      ProLogger.trace(
          "Current capital result hasLandUnitRemaining="
              + result.isHasLandUnitRemaining()
              + ", TUVSwing="
              + result.getTuvSwing()
              + ", defenders="
              + defenders.size()
              + ", attackers="
              + enemyAttackingUnits.size());

      // Determine attack that uses the most units per value from capital and remove it
      if (result.isHasLandUnitRemaining()) {
        double maxUnitsNearCapitalPerValue = 0.0;
        Territory maxTerritory = null;
        final Set<Territory> territoriesNearCapital =
            data.getMap().getNeighbors(myCapital, Matches.territoryIsLand());
        territoriesNearCapital.add(myCapital);
        for (final Map.Entry<Territory, ProTerritory> attackEntry : attackMap.entrySet()) {
          final Territory t = attackEntry.getKey();
          int unitsNearCapital = 0;
          for (final Unit u : attackEntry.getValue().getUnits()) {
            if (territoriesNearCapital.contains(proData.getUnitTerritory(u))) {
              unitsNearCapital++;
            }
          }
          final double unitsNearCapitalPerValue = unitsNearCapital / attackMap.get(t).getValue();
          ProLogger.trace(
              t.getName() + " has unit near capital per value: " + unitsNearCapitalPerValue);
          if (unitsNearCapitalPerValue > maxUnitsNearCapitalPerValue) {
            maxUnitsNearCapitalPerValue = unitsNearCapitalPerValue;
            maxTerritory = t;
          }
        }
        if (maxTerritory != null) {
          final ProTerritory patdMax = attackMap.get(maxTerritory);
          prioritizedTerritories.remove(patdMax);
          patdMax.getUnits().clear();
          patdMax.getAmphibAttackMap().clear();
          patdMax.setBattleResult(null);
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

    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();

    for (final Territory t : proData.getMyUnitTerritories()) {
      if (t.isWater()
          && Matches.territoryHasEnemyUnits(player).test(t)
          && (attackMap.get(t) == null || attackMap.get(t).getUnits().isEmpty())) {

        // Move into random adjacent safe sea territory
        final Set<Territory> possibleMoveTerritories =
            data.getMap().getNeighbors(t, ProMatches.territoryCanMoveSeaUnitsThrough(player, true));
        if (!possibleMoveTerritories.isEmpty()) {
          final Territory moveToTerritory = CollectionUtils.getAny(possibleMoveTerritories);
          final List<Unit> mySeaUnits =
              t.getMatches(ProMatches.unitCanBeMovedAndIsOwnedSea(player, true));
          proData.getProTerritory(attackMap, moveToTerritory).addUnits(mySeaUnits);
          ProLogger.info(t + " is a contested territory so moving subs to " + moveToTerritory);
        }
      }
    }
  }

  private void logAttackMoves(final List<ProTerritory> prioritizedTerritories) {

    final Map<Territory, ProTerritory> attackMap =
        territoryManager.getAttackOptions().getTerritoryMap();

    // Print prioritization
    ProLogger.debug("Prioritized territories:");
    for (final ProTerritory attackTerritoryData : prioritizedTerritories) {
      ProLogger.trace(
          "  "
              + attackTerritoryData.getMaxBattleResult().getTuvSwing()
              + "  "
              + attackTerritoryData.getValue()
              + "  "
              + attackTerritoryData.getTerritory().getName());
    }

    // Print enemy territories with enemy units vs my units
    ProLogger.debug("Territories that can be attacked:");
    int count = 0;
    for (final Map.Entry<Territory, ProTerritory> attackEntry : attackMap.entrySet()) {
      final Territory t = attackEntry.getKey();
      count++;
      ProLogger.trace(count + ". ---" + t.getName());
      final Set<Unit> combinedUnits = new HashSet<>(attackEntry.getValue().getMaxUnits());
      combinedUnits.addAll(attackEntry.getValue().getMaxAmphibUnits());
      ProLogger.trace("  --- My max units ---");
      final Map<String, Integer> printMap = new HashMap<>();
      for (final Unit unit : combinedUnits) {
        if (printMap.containsKey(unit.toStringNoOwner())) {
          printMap.put(unit.toStringNoOwner(), printMap.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap.put(unit.toStringNoOwner(), 1);
        }
      }
      writeProLog(printMap);
      ProLogger.trace("  --- My max bombard units ---");
      final Map<String, Integer> printBombardMap = new HashMap<>();
      for (final Unit unit : attackEntry.getValue().getMaxBombardUnits()) {
        if (printBombardMap.containsKey(unit.toStringNoOwner())) {
          printBombardMap.put(
              unit.toStringNoOwner(), printBombardMap.get(unit.toStringNoOwner()) + 1);
        } else {
          printBombardMap.put(unit.toStringNoOwner(), 1);
        }
      }
      writeProLog(printBombardMap);
      final List<Unit> units3 = attackEntry.getValue().getUnits();
      ProLogger.trace("  --- My actual units ---");
      final Map<String, Integer> printMap3 = new HashMap<>();
      for (final Unit unit : units3) {
        if (printMap3.containsKey(unit.toStringNoOwner())) {
          printMap3.put(unit.toStringNoOwner(), printMap3.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap3.put(unit.toStringNoOwner(), 1);
        }
      }
      writeProLog(printMap3);
      ProLogger.trace("  --- Enemy units ---");
      final Map<String, Integer> printMap2 = new HashMap<>();
      final List<Unit> units2 = attackEntry.getValue().getMaxEnemyDefenders(player);
      for (final Unit unit : units2) {
        if (printMap2.containsKey(unit.toStringNoOwner())) {
          printMap2.put(unit.toStringNoOwner(), printMap2.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap2.put(unit.toStringNoOwner(), 1);
        }
      }
      writeProLog(printMap2);
      ProLogger.trace("  --- Enemy Counter Attack Units ---");
      final Map<String, Integer> printMap4 = new HashMap<>();
      final List<Unit> units4 = attackEntry.getValue().getMaxEnemyUnits();
      for (final Unit unit : units4) {
        if (printMap4.containsKey(unit.toStringNoOwner())) {
          printMap4.put(unit.toStringNoOwner(), printMap4.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap4.put(unit.toStringNoOwner(), 1);
        }
      }
      writeProLog(printMap4);
      ProLogger.trace("  --- Enemy Counter Bombard Units ---");
      final Map<String, Integer> printMap5 = new HashMap<>();
      final Set<Unit> units5 = attackEntry.getValue().getMaxEnemyBombardUnits();
      for (final Unit unit : units5) {
        if (printMap5.containsKey(unit.toStringNoOwner())) {
          printMap5.put(unit.toStringNoOwner(), printMap5.get(unit.toStringNoOwner()) + 1);
        } else {
          printMap5.put(unit.toStringNoOwner(), 1);
        }
      }
      writeProLog(printMap5);
    }
  }

  private void writeProLog(final Map<String, Integer> printMap) {
    for (final Map.Entry<String, Integer> printEntry : printMap.entrySet()) {
      ProLogger.trace("    " + printEntry.getValue() + " " + printEntry.getKey());
    }
  }

  private boolean canAirSafelyLandAfterAttack(final Unit unit, final Territory t) {
    final boolean isAdjacentToAlliedFactory =
        Matches.territoryHasNeighborMatching(
                data.getMap(), ProMatches.territoryHasInfraFactoryAndIsAlliedLand(player))
            .test(t);
    final int range = unit.getMovementLeft().intValue();
    final int distance =
        data.getMap()
            .getDistanceIgnoreEndForCondition(
                proData.getUnitTerritory(unit),
                t,
                ProMatches.territoryCanMoveAirUnitsAndNoAa(data, player, true));
    final boolean usesMoreThanHalfOfRange = distance > range / 2;
    return isAdjacentToAlliedFactory || !usesMoreThanHalfOfRange;
  }
}
