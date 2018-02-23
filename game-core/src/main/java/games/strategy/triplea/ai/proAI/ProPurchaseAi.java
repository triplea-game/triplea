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
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.proAI.data.ProBattleResult;
import games.strategy.triplea.ai.proAI.data.ProOtherMoveOptions;
import games.strategy.triplea.ai.proAI.data.ProPlaceTerritory;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOption;
import games.strategy.triplea.ai.proAI.data.ProPurchaseOptionMap;
import games.strategy.triplea.ai.proAI.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.proAI.data.ProResourceTracker;
import games.strategy.triplea.ai.proAI.data.ProTerritoryManager;
import games.strategy.triplea.ai.proAI.logging.ProLogger;
import games.strategy.triplea.ai.proAI.logging.ProMetricUtils;
import games.strategy.triplea.ai.proAI.util.ProBattleUtils;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.ai.proAI.util.ProOddsCalculator;
import games.strategy.triplea.ai.proAI.util.ProPurchaseUtils;
import games.strategy.triplea.ai.proAI.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.proAI.util.ProTransportUtils;
import games.strategy.triplea.ai.proAI.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * Pro purchase AI.
 */
class ProPurchaseAi {

  private final ProOddsCalculator calc;
  private GameData data;
  private GameData startOfTurnData; // Used to count current units on map for maxBuiltPerPlayer
  private PlayerID player;
  private ProResourceTracker resourceTracker;
  private ProTerritoryManager territoryManager;
  private boolean isBid = false;

  ProPurchaseAi(final ProAi ai) {
    calc = ai.getCalc();
  }

  int repair(int pusRemaining, final IPurchaseDelegate purchaseDelegate, final GameData data,
      final PlayerID player) {

    ProLogger.info("Repairing factories with PUsRemaining=" + pusRemaining);

    // Current data at the start of combat move
    this.data = data;
    this.player = player;
    final Predicate<Unit> ourFactories = Matches.unitIsOwnedBy(player)
        .and(Matches.unitCanProduceUnits())
        .and(Matches.unitIsInfrastructure());
    final List<Territory> rfactories = CollectionUtils.getMatches(data.getMap().getTerritories(),
        ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data));
    if ((player.getRepairFrontier() != null)
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
      ProLogger.debug("Factories can be damaged");
      final Map<Unit, Territory> unitsThatCanProduceNeedingRepair = new HashMap<>();
      for (final Territory fixTerr : rfactories) {
        if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(player, Matches.unitCanProduceUnitsAndCanBeDamaged())
            .test(fixTerr)) {
          continue;
        }
        final Unit possibleFactoryNeedingRepair = TripleAUnit.getBiggestProducer(
            CollectionUtils.getMatches(fixTerr.getUnits().getUnits(), ourFactories), fixTerr, player, data, false);
        if (Matches.unitHasTakenSomeBombingUnitDamage().test(possibleFactoryNeedingRepair)) {
          unitsThatCanProduceNeedingRepair.put(possibleFactoryNeedingRepair, fixTerr);
        }
      }
      ProLogger.debug("Factories that need repaired: " + unitsThatCanProduceNeedingRepair);
      final List<RepairRule> rrules = player.getRepairFrontier().getRules();
      for (final RepairRule rrule : rrules) {
        for (final Unit fixUnit : unitsThatCanProduceNeedingRepair.keySet()) {
          if ((fixUnit == null) || !fixUnit.getType().equals(rrule.getResults().keySet().iterator().next())) {
            continue;
          }
          if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(player, Matches.unitCanProduceUnitsAndCanBeDamaged())
              .test(unitsThatCanProduceNeedingRepair.get(fixUnit))) {
            continue;
          }
          final TripleAUnit taUnit = (TripleAUnit) fixUnit;
          final int diff = taUnit.getUnitDamage();
          if (diff > 0) {
            final IntegerMap<RepairRule> repairMap = new IntegerMap<>();
            repairMap.add(rrule, diff);
            final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<>();
            repair.put(fixUnit, repairMap);
            pusRemaining -= diff;
            ProLogger.debug("Repairing factory=" + fixUnit + ", damage=" + diff + ", repairRule=" + rrule);
            purchaseDelegate.purchaseRepair(repair);
          }
        }
      }
    }
    return pusRemaining;
  }

  /**
   * Default settings for bidding:
   * 1) Limit one bid unit in a territory or sea zone (until set in all territories then 2, etc).
   * 2) The nation placing a unit in a territory or sea zone must have started with a unit in said territory or sea zone
   * prior to placing the bid.
   */
  Map<Territory, ProPurchaseTerritory> bid(final int pus, final IPurchaseDelegate purchaseDelegate,
      final GameData startOfTurnData) {

    // Current data fields
    data = ProData.getData();
    this.startOfTurnData = startOfTurnData;
    player = ProData.getPlayer();
    resourceTracker = new ProResourceTracker(pus, data);
    territoryManager = new ProTerritoryManager(calc);
    isBid = true;
    final ProPurchaseOptionMap purchaseOptions = ProData.purchaseOptions;

    ProLogger.info("Starting bid phase with resources: " + resourceTracker);
    if (!player.getUnits().getUnits().isEmpty()) {
      ProLogger.info("Starting bid phase with unplaced units=" + player.getUnits().getUnits());
    }

    // Find all purchase/place territories
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories = ProPurchaseUtils.findBidTerritories(player);

    int previousNumUnits = 0;
    while (true) {

      // Determine max enemy attack units and current allied defenders
      territoryManager.populateEnemyAttackOptions(new ArrayList<>(), new ArrayList<>(purchaseTerritories.keySet()));
      findDefendersInPlaceTerritories(purchaseTerritories);

      // Prioritize land territories that need defended and purchase additional defenders
      final List<ProPlaceTerritory> needToDefendLandTerritories =
          prioritizeTerritoriesToDefend(purchaseTerritories, true);
      purchaseDefenders(purchaseTerritories, needToDefendLandTerritories, purchaseOptions.getLandFodderOptions(),
          purchaseOptions.getAirOptions(), true);

      // Find strategic value for each territory
      ProLogger.info("Find strategic value for place territories");
      final Map<Territory, Double> territoryValueMap =
          ProTerritoryValueUtils.findTerritoryValues(player, new ArrayList<>(), new ArrayList<>());
      for (final Territory t : purchaseTerritories.keySet()) {
        for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
          ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
          ProLogger.debug(ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
        }
      }

      // Prioritize land place options purchase AA then land units
      final List<ProPlaceTerritory> prioritizedLandTerritories = prioritizeLandTerritories(purchaseTerritories);
      purchaseAaUnits(purchaseTerritories, prioritizedLandTerritories, purchaseOptions.getAaOptions());
      purchaseLandUnits(purchaseTerritories, prioritizedLandTerritories, purchaseOptions, territoryValueMap);

      // Prioritize sea territories that need defended and purchase additional defenders
      final List<ProPlaceTerritory> needToDefendSeaTerritories =
          prioritizeTerritoriesToDefend(purchaseTerritories, false);
      purchaseDefenders(purchaseTerritories, needToDefendSeaTerritories, purchaseOptions.getSeaDefenseOptions(),
          purchaseOptions.getAirOptions(), false);

      // Prioritize sea place options and purchase units
      final List<ProPlaceTerritory> prioritizedSeaTerritories = prioritizeSeaTerritories(purchaseTerritories);
      purchaseSeaAndAmphibUnits(purchaseTerritories, prioritizedSeaTerritories, territoryValueMap, purchaseOptions);

      // Try to use any remaining PUs on high value units
      purchaseUnitsWithRemainingProduction(purchaseTerritories, purchaseOptions.getLandOptions(),
          purchaseOptions.getAirOptions());
      upgradeUnitsWithRemainingPUs(purchaseTerritories, purchaseOptions);

      // Check if no remaining PUs or no unit built this iteration
      int numUnits = 0;
      for (final Territory t : purchaseTerritories.keySet()) {
        numUnits += purchaseTerritories.get(t).getCanPlaceTerritories().get(0).getPlaceUnits().size();
      }
      if (resourceTracker.isEmpty() || (numUnits == previousNumUnits)) {
        break;
      }
      previousNumUnits = numUnits;
      ProPurchaseUtils.incrementUnitProductionForBidTerritories(purchaseTerritories);
    }

    // Determine final count of each production rule
    final IntegerMap<ProductionRule> purchaseMap = populateProductionRuleMap(purchaseTerritories, purchaseOptions);

    // Purchase units
    ProMetricUtils.collectPurchaseStats(purchaseMap);
    final String error = purchaseDelegate.purchase(purchaseMap);
    if (error != null) {
      ProLogger.warn("Purchase error: " + error);
    }
    return purchaseTerritories;
  }

  Map<Territory, ProPurchaseTerritory> purchase(final IPurchaseDelegate purchaseDelegate,
      final GameData startOfTurnData) {

    // Current data fields
    data = ProData.getData();
    this.startOfTurnData = startOfTurnData;
    player = ProData.getPlayer();
    resourceTracker = new ProResourceTracker(player);
    territoryManager = new ProTerritoryManager(calc);
    isBid = false;
    final ProPurchaseOptionMap purchaseOptions = ProData.purchaseOptions;

    ProLogger.info("Starting purchase phase with resources: " + resourceTracker);
    if (!player.getUnits().getUnits().isEmpty()) {
      ProLogger.info("Starting purchase phase with unplaced units=" + player.getUnits().getUnits());
    }

    // Find all purchase/place territories
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories = ProPurchaseUtils.findPurchaseTerritories(player);
    final Set<Territory> placeTerritories = new HashSet<>();
    placeTerritories
        .addAll(CollectionUtils.getMatches(data.getMap().getTerritoriesOwnedBy(player), Matches.territoryIsLand()));
    for (final Territory t : purchaseTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
        placeTerritories.add(ppt.getTerritory());
      }
    }

    // Determine max enemy attack units and current allied defenders
    territoryManager.populateEnemyAttackOptions(new ArrayList<>(), new ArrayList<>(placeTerritories));
    findDefendersInPlaceTerritories(purchaseTerritories);

    // Prioritize land territories that need defended and purchase additional defenders
    final List<ProPlaceTerritory> needToDefendLandTerritories =
        prioritizeTerritoriesToDefend(purchaseTerritories, true);
    purchaseDefenders(purchaseTerritories, needToDefendLandTerritories, purchaseOptions.getLandFodderOptions(),
        purchaseOptions.getAirOptions(), true);

    // Find strategic value for each territory
    ProLogger.info("Find strategic value for place territories");
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(player, new ArrayList<>(), new ArrayList<>());
    for (final Territory t : purchaseTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
        ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
        ProLogger.debug(ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
      }
    }

    // Prioritize land place options purchase AA then land units
    final List<ProPlaceTerritory> prioritizedLandTerritories = prioritizeLandTerritories(purchaseTerritories);
    purchaseAaUnits(purchaseTerritories, prioritizedLandTerritories, purchaseOptions.getAaOptions());
    purchaseLandUnits(purchaseTerritories, prioritizedLandTerritories, purchaseOptions, territoryValueMap);

    // Prioritize sea territories that need defended and purchase additional defenders
    final List<ProPlaceTerritory> needToDefendSeaTerritories =
        prioritizeTerritoriesToDefend(purchaseTerritories, false);
    purchaseDefenders(purchaseTerritories, needToDefendSeaTerritories, purchaseOptions.getSeaDefenseOptions(),
        purchaseOptions.getAirOptions(), false);

    // Determine whether to purchase new land factory
    final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories = new HashMap<>();
    purchaseFactory(factoryPurchaseTerritories, purchaseTerritories, prioritizedLandTerritories, purchaseOptions,
        false);

    // Prioritize sea place options and purchase units
    final List<ProPlaceTerritory> prioritizedSeaTerritories = prioritizeSeaTerritories(purchaseTerritories);
    purchaseSeaAndAmphibUnits(purchaseTerritories, prioritizedSeaTerritories, territoryValueMap, purchaseOptions);

    // Try to use any remaining PUs on high value units
    purchaseUnitsWithRemainingProduction(purchaseTerritories, purchaseOptions.getLandOptions(),
        purchaseOptions.getAirOptions());
    upgradeUnitsWithRemainingPUs(purchaseTerritories, purchaseOptions);

    // Try to purchase land/sea factory with extra PUs
    purchaseFactory(factoryPurchaseTerritories, purchaseTerritories, prioritizedLandTerritories, purchaseOptions, true);

    // Add factory purchase territory to list if not empty
    if (!factoryPurchaseTerritories.isEmpty()) {
      purchaseTerritories.putAll(factoryPurchaseTerritories);
    }

    // Determine final count of each production rule
    final IntegerMap<ProductionRule> purchaseMap = populateProductionRuleMap(purchaseTerritories, purchaseOptions);

    // Purchase units
    ProMetricUtils.collectPurchaseStats(purchaseMap);
    final String error = purchaseDelegate.purchase(purchaseMap);
    if (error != null) {
      ProLogger.warn("Purchase error: " + error);
    }
    return purchaseTerritories;
  }

  void place(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final IAbstractPlaceDelegate placeDelegate) {
    ProLogger.info("Starting place phase");

    data = ProData.getData();
    player = ProData.getPlayer();
    territoryManager = new ProTerritoryManager(calc);

    if (purchaseTerritories != null) {

      // Place all units calculated during purchase phase (land then sea to reduce failed placements)
      for (final Territory t : purchaseTerritories.keySet()) {
        for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
          if (!ppt.getTerritory().isWater()) {
            final Collection<Unit> myUnits = player.getUnits().getUnits();
            final List<Unit> unitsToPlace = new ArrayList<>();
            for (final Unit placeUnit : ppt.getPlaceUnits()) {
              for (final Unit myUnit : myUnits) {
                if (myUnit.getType().equals(placeUnit.getType()) && !unitsToPlace.contains(myUnit)) {
                  unitsToPlace.add(myUnit);
                  break;
                }
              }
            }
            doPlace(data.getMap().getTerritory(ppt.getTerritory().getName()), unitsToPlace, placeDelegate);
            ProLogger.debug(ppt.getTerritory() + " placed units: " + unitsToPlace);
          }
        }
      }
      for (final Territory t : purchaseTerritories.keySet()) {
        for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
          if (ppt.getTerritory().isWater()) {
            final Collection<Unit> myUnits = player.getUnits().getUnits();
            final List<Unit> unitsToPlace = new ArrayList<>();
            for (final Unit placeUnit : ppt.getPlaceUnits()) {
              for (final Unit myUnit : myUnits) {
                if (myUnit.getType().equals(placeUnit.getType()) && !unitsToPlace.contains(myUnit)) {
                  unitsToPlace.add(myUnit);
                  break;
                }
              }
            }
            doPlace(data.getMap().getTerritory(ppt.getTerritory().getName()), unitsToPlace, placeDelegate);
            ProLogger.debug(ppt.getTerritory() + " placed units: " + unitsToPlace);
          }
        }
      }
    }

    // Place remaining units (currently only implemented to handle land units, ex. WW2v3 China)
    if (player.getUnits().getUnits().isEmpty()) {
      return;
    }

    // Current data at the start of place
    ProLogger.debug("Remaining units to place: " + player.getUnits().getUnits());

    // Find all place territories
    final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories =
        ProPurchaseUtils.findPurchaseTerritories(player);

    // Determine max enemy attack units and current allied defenders
    findDefendersInPlaceTerritories(placeNonConstructionTerritories);

    // Prioritize land territories that need defended and place additional defenders
    final List<ProPlaceTerritory> needToDefendLandTerritories =
        prioritizeTerritoriesToDefend(placeNonConstructionTerritories, true);
    placeDefenders(placeNonConstructionTerritories, needToDefendLandTerritories, placeDelegate);

    // Find strategic value for each territory
    ProLogger.info("Find strategic value for place territories");
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(player, new ArrayList<>(), new ArrayList<>());
    for (final Territory t : placeNonConstructionTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : placeNonConstructionTerritories.get(t).getCanPlaceTerritories()) {
        ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
        ProLogger.debug(ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
      }
    }

    // Prioritize land place territories, add all territories, and then place units
    final List<ProPlaceTerritory> prioritizedLandTerritories =
        prioritizeLandTerritories(placeNonConstructionTerritories);
    for (final ProPurchaseTerritory ppt : placeNonConstructionTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if (!t.isWater() && !prioritizedLandTerritories.contains(placeTerritory)) {
          prioritizedLandTerritories.add(placeTerritory);
        }
      }
    }
    // Place regular land units
    placeLandUnits(prioritizedLandTerritories, placeDelegate, false);

    // Place isConstruction land units (needs separated since placeDelegate.getPlaceableUnits doesn't handle combined)
    placeLandUnits(prioritizedLandTerritories, placeDelegate, true);
  }

  private void findDefendersInPlaceTerritories(final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    ProLogger.info("Find defenders in possible place territories");
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        final List<Unit> units = t.getUnits().getMatches(Matches.isUnitAllied(player, data));
        placeTerritory.setDefendingUnits(units);
        ProLogger.debug(t + " has numDefenders=" + units.size());
      }
    }
  }

  private List<ProPlaceTerritory> prioritizeTerritoriesToDefend(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final boolean isLand) {

    ProLogger.info("Prioritize territories to defend with isLand=" + isLand);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Determine which territories need defended
    final Set<ProPlaceTerritory> needToDefendTerritories = new HashSet<>();
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {

      // Check if any of the place territories can't be held with current defenders
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if ((enemyAttackOptions.getMax(t) == null) || (t.isWater() && placeTerritory.getDefendingUnits().isEmpty())
            || (isLand && t.isWater()) || (!isLand && !t.isWater())) {
          continue;
        }

        // Find current battle result
        final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        final ProBattleResult result = calc.calculateBattleResults(t, new ArrayList<>(enemyAttackingUnits),
            placeTerritory.getDefendingUnits(), enemyAttackOptions.getMax(t).getMaxBombardUnits());
        placeTerritory.setMinBattleResult(result);
        double holdValue = 0;
        if (t.isWater()) {
          final double unitValue = TuvUtils.getTuv(
              CollectionUtils.getMatches(placeTerritory.getDefendingUnits(), Matches.unitIsOwnedBy(player)),
              ProData.unitValueMap);
          holdValue = unitValue / 8;
        }
        ProLogger.trace(t.getName() + " TUVSwing=" + result.getTuvSwing() + ", win%=" + result.getWinPercentage()
            + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining() + ", holdValue=" + holdValue
            + ", enemyAttackers=" + enemyAttackingUnits + ", defenders=" + placeTerritory.getDefendingUnits());

        // If it can't currently be held then add to list
        final boolean isLandAndCanOnlyBeAttackedByAir =
            !t.isWater() && !enemyAttackingUnits.isEmpty()
                && enemyAttackingUnits.stream().allMatch(Matches.unitIsAir());
        if ((!t.isWater() && result.isHasLandUnitRemaining()) || (result.getTuvSwing() > holdValue)
            || (t.equals(ProData.myCapital) && !isLandAndCanOnlyBeAttackedByAir
            && (result.getWinPercentage() > (100 - ProData.winPercentage)))) {
          needToDefendTerritories.add(placeTerritory);
        }
      }
    }

    // Calculate value of defending territory
    for (final ProPlaceTerritory placeTerritory : needToDefendTerritories) {
      final Territory t = placeTerritory.getTerritory();

      // Determine if it is my capital or adjacent to my capital
      int isMyCapital = 0;
      if (t.equals(ProData.myCapital)) {
        isMyCapital = 1;
      }

      // Determine if it has a factory
      int isFactory = 0;
      if (ProMatches.territoryHasInfraFactoryAndIsOwnedLand(player).test(t)) {
        isFactory = 1;
      }

      // Determine production value and if it is an enemy capital
      int production = 0;
      final TerritoryAttachment ta = TerritoryAttachment.get(t);
      if (ta != null) {
        production = ta.getProduction();
      }

      // Determine defending unit value
      double defendingUnitValue = TuvUtils.getTuv(placeTerritory.getDefendingUnits(), ProData.unitValueMap);
      if (t.isWater() && placeTerritory.getDefendingUnits().stream().noneMatch(Matches.unitIsOwnedBy(player))) {
        defendingUnitValue = 0;
      }

      // Calculate defense value for prioritization
      final double territoryValue =
          ((2 * production) + (4 * isFactory) + (0.5 * defendingUnitValue)) * (1 + isFactory) * (1 + (10
              * isMyCapital));
      placeTerritory.setDefenseValue(territoryValue);
    }

    // Remove any territories with negative defense value
    for (final Iterator<ProPlaceTerritory> it = needToDefendTerritories.iterator(); it.hasNext();) {
      final ProPlaceTerritory ppt = it.next();
      if (ppt.getDefenseValue() <= 0) {
        it.remove();
      }
    }

    // Sort territories by value
    final List<ProPlaceTerritory> sortedTerritories = new ArrayList<>(needToDefendTerritories);
    Collections.sort(sortedTerritories, (t1, t2) -> {
      final double value1 = t1.getDefenseValue();
      final double value2 = t2.getDefenseValue();
      return Double.compare(value2, value1);
    });
    for (final ProPlaceTerritory placeTerritory : sortedTerritories) {
      ProLogger.debug(placeTerritory.toString() + " defenseValue=" + placeTerritory.getDefenseValue());
    }
    return sortedTerritories;
  }

  private void purchaseDefenders(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> needToDefendTerritories, final List<ProPurchaseOption> defensePurchaseOptions,
      final List<ProPurchaseOption> airPurchaseOptions, final boolean isLand) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase defenders with resources: " + resourceTracker + ", isLand=" + isLand);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through prioritized territories and purchase defenders
    for (final ProPlaceTerritory placeTerritory : needToDefendTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Purchasing defenders for " + t.getName() + ", enemyAttackers="
          + enemyAttackOptions.getMax(t).getMaxUnits() + ", amphibEnemyAttackers="
          + enemyAttackOptions.getMax(t).getMaxAmphibUnits() + ", defenders=" + placeTerritory.getDefendingUnits());

      // Find local owned units
      final List<Unit> ownedLocalUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
      int unusedCarrierCapacity = Math.min(0, ProTransportUtils.getUnusedCarrierCapacity(player, t, new ArrayList<>()));
      int unusedLocalCarrierCapacity = ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, new ArrayList<>());
      ProLogger.trace(t + ", unusedCarrierCapacity=" + unusedCarrierCapacity + ", unusedLocalCarrierCapacity="
          + unusedLocalCarrierCapacity);

      // Determine if need destroyer
      boolean needDestroyer = false;
      if (enemyAttackOptions.getMax(t).getMaxUnits().stream().anyMatch(Matches.unitIsSub())
          && ownedLocalUnits.stream().noneMatch(Matches.unitIsDestroyer())) {
        needDestroyer = true;
      }

      // Find all purchase territories for place territory
      final List<Unit> unitsToPlace = new ArrayList<>();
      ProBattleResult finalResult = new ProBattleResult();
      final List<ProPurchaseTerritory> selectedPurchaseTerritories =
          getPurchaseTerritories(placeTerritory, purchaseTerritories);
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {

        // Check remaining production
        int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
        ProLogger.debug(purchaseTerritory.getTerritory() + ", remainingUnitProduction=" + remainingUnitProduction);
        if (remainingUnitProduction <= 0) {
          continue;
        }

        // Find defenders that can be produced in this territory
        final List<ProPurchaseOption> purchaseOptionsForTerritory =
            ProPurchaseUtils.findPurchaseOptionsForTerritory(player, defensePurchaseOptions, t, isBid);
        purchaseOptionsForTerritory.addAll(airPurchaseOptions);

        // Purchase necessary defenders
        while (true) {

          // Select purchase option
          ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, purchaseOptionsForTerritory,
              resourceTracker, remainingUnitProduction, unitsToPlace, purchaseTerritories);
          final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
            if (isLand) {
              defenseEfficiencies.put(ppo, ppo.getDefenseEfficiency2(1, data, ownedLocalUnits, unitsToPlace));
            } else {
              defenseEfficiencies.put(ppo, ppo.getSeaDefenseEfficiency(data, ownedLocalUnits, unitsToPlace,
                  needDestroyer, unusedCarrierCapacity, unusedLocalCarrierCapacity));
            }
          }
          final Optional<ProPurchaseOption> optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Defense");
          if (!optionalSelectedOption.isPresent()) {
            break;
          }
          final ProPurchaseOption selectedOption = optionalSelectedOption.get();
          if (selectedOption.isDestroyer()) {
            needDestroyer = false;
          }

          // Create new temp units
          resourceTracker.tempPurchase(selectedOption);
          remainingUnitProduction -= selectedOption.getQuantity();
          unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
          if (selectedOption.isCarrier() || selectedOption.isAir()) {
            unusedCarrierCapacity = ProTransportUtils.getUnusedCarrierCapacity(player, t, unitsToPlace);
            unusedLocalCarrierCapacity = ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, unitsToPlace);
          }
          ProLogger.trace("Selected unit=" + selectedOption.getUnitType().getName() + ", unusedCarrierCapacity="
              + unusedCarrierCapacity + ", unusedLocalCarrierCapacity=" + unusedLocalCarrierCapacity);

          // Find current battle result
          final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
          enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
          final List<Unit> defenders = new ArrayList<>(placeTerritory.getDefendingUnits());
          defenders.addAll(unitsToPlace);
          finalResult = calc.calculateBattleResults(t, new ArrayList<>(enemyAttackingUnits), defenders,
              enemyAttackOptions.getMax(t).getMaxBombardUnits());

          // Break if it can be held
          if ((!t.equals(ProData.myCapital) && !finalResult.isHasLandUnitRemaining() && (finalResult.getTuvSwing()
              <= 0))
              || (t.equals(ProData.myCapital) && (finalResult.getWinPercentage() < (100 - ProData.winPercentage))
              && (finalResult.getTuvSwing() <= 0))) {
            break;
          }
        }
      }

      // Check to see if its worth trying to defend the territory
      final boolean hasLocalSuperiority =
          ProBattleUtils.territoryHasLocalLandSuperiority(t, ProBattleUtils.SHORT_RANGE, player, purchaseTerritories);
      if (!finalResult.isHasLandUnitRemaining()
          || ((finalResult.getTuvSwing() - (resourceTracker.getTempPUs(data) / 2)) < placeTerritory.getMinBattleResult()
              .getTuvSwing())
          || t.equals(ProData.myCapital)
          || (!t.isWater() && hasLocalSuperiority)) {
        resourceTracker.confirmTempPurchases();
        ProLogger.trace(
            t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTuvSwing() + ", hasLandUnitRemaining="
                + finalResult.isHasLandUnitRemaining() + ", hasLocalSuperiority=" + hasLocalSuperiority);
        addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
      } else {
        resourceTracker.clearTempPurchases();
        setCantHoldPlaceTerritory(placeTerritory, purchaseTerritories);
        ProLogger.trace(t + ", unable to defend with placedUnits=" + unitsToPlace + ", TUVSwing="
            + finalResult.getTuvSwing() + ", minTUVSwing=" + placeTerritory.getMinBattleResult().getTuvSwing());
      }
    }
  }

  private List<ProPlaceTerritory> prioritizeLandTerritories(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    ProLogger.info("Prioritize land territories to place");

    // Get all land place territories
    final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<>();
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if (!t.isWater() && (placeTerritory.getStrategicValue() >= 1) && placeTerritory.isCanHold()) {
          final boolean hasEnemyNeighbors =
              !data.getMap().getNeighbors(t, ProMatches.territoryIsEnemyLand(player, data)).isEmpty();
          final Set<Territory> nearbyLandTerritories =
              data.getMap().getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player, data));
          final int numNearbyEnemyTerritories = CollectionUtils.countMatches(nearbyLandTerritories,
              Matches.isTerritoryOwnedBy(ProUtils.getPotentialEnemyPlayers(player)));
          final boolean hasLocalLandSuperiority =
              ProBattleUtils.territoryHasLocalLandSuperiority(t, ProBattleUtils.SHORT_RANGE, player);
          if (hasEnemyNeighbors || (numNearbyEnemyTerritories >= 3) || !hasLocalLandSuperiority) {
            prioritizedLandTerritories.add(placeTerritory);
          }
        }
      }
    }

    // Sort territories by value
    Collections.sort(prioritizedLandTerritories, (t1, t2) -> {
      final double value1 = t1.getStrategicValue();
      final double value2 = t2.getStrategicValue();
      return Double.compare(value2, value1);
    });
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      ProLogger.debug(placeTerritory.toString() + " strategicValue=" + placeTerritory.getStrategicValue());
    }
    return prioritizedLandTerritories;
  }

  private void purchaseAaUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedLandTerritories, final List<ProPurchaseOption> specialPurchaseOptions) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase AA units with resources: " + resourceTracker);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through prioritized territories and purchase AA units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking AA place for " + t);

      // Check if any enemy attackers
      if (enemyAttackOptions.getMax(t) == null) {
        continue;
      }

      // Check remaining production
      final int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
      ProLogger.debug(t + ", remainingUnitProduction=" + remainingUnitProduction);
      if (remainingUnitProduction <= 0) {
        continue;
      }

      // Check if territory needs AA
      final boolean enemyCanBomb =
          enemyAttackOptions.getMax(t).getMaxUnits().stream().anyMatch(Matches.unitIsStrategicBomber());
      final boolean territoryCanBeBombed = t.getUnits().anyMatch(Matches.unitCanProduceUnitsAndCanBeDamaged());
      final boolean hasAaBombingDefense = t.getUnits().anyMatch(Matches.unitIsAaForBombingThisUnitOnly());
      ProLogger.debug(t + ", enemyCanBomb=" + enemyCanBomb + ", territoryCanBeBombed=" + territoryCanBeBombed
          + ", hasAABombingDefense=" + hasAaBombingDefense);
      if (!enemyCanBomb || !territoryCanBeBombed || hasAaBombingDefense) {
        continue;
      }

      // Remove options that cost too much PUs or production
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, specialPurchaseOptions, t, isBid);
      ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, purchaseOptionsForTerritory,
          resourceTracker, remainingUnitProduction, new ArrayList<>(), purchaseTerritories);
      if (purchaseOptionsForTerritory.isEmpty()) {
        continue;
      }

      // Determine most cost efficient units that can be produced in this territory
      ProPurchaseOption bestAaOption = null;
      int minCost = Integer.MAX_VALUE;
      for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
        final boolean isAaForBombing = Matches.unitTypeIsAaForBombingThisUnitOnly().test(ppo.getUnitType());
        if (isAaForBombing && (ppo.getCost() < minCost)
            && !Matches.unitTypeConsumesUnitsOnCreation().test(ppo.getUnitType())) {
          bestAaOption = ppo;
          minCost = ppo.getCost();
        }
      }

      // Check if there aren't any available units
      if (bestAaOption == null) {
        continue;
      }
      ProLogger.trace("Best AA unit: " + bestAaOption.getUnitType().getName());

      // Create new temp units
      resourceTracker.purchase(bestAaOption);
      final List<Unit> unitsToPlace = bestAaOption.getUnitType().create(bestAaOption.getQuantity(), player, true);
      placeTerritory.getPlaceUnits().addAll(unitsToPlace);
      ProLogger.trace(t + ", placedUnits=" + unitsToPlace);
    }
  }

  private void purchaseLandUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedLandTerritories, final ProPurchaseOptionMap purchaseOptions,
      final Map<Territory, Double> territoryValueMap) {

    final List<Unit> unplacedUnits = player.getUnits().getMatches(Matches.unitIsNotSea());
    if (resourceTracker.isEmpty() && unplacedUnits.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase land units with resources: " + resourceTracker);
    if (!unplacedUnits.isEmpty()) {
      ProLogger.info("Purchase land units with unplaced units=" + unplacedUnits);
    }

    // Loop through prioritized territories and purchase land units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking land place for " + t.getName());

      // Check remaining production
      int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
      ProLogger.debug(t + ", remainingUnitProduction=" + remainingUnitProduction);
      if (remainingUnitProduction <= 0) {
        continue;
      }

      // Determine most cost efficient units that can be produced in this territory
      final List<ProPurchaseOption> landFodderOptions =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandFodderOptions(), t, isBid);
      final List<ProPurchaseOption> landAttackOptions =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandAttackOptions(), t, isBid);
      final List<ProPurchaseOption> landDefenseOptions =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandDefenseOptions(), t, isBid);

      // Determine enemy distance and locally owned units
      int enemyDistance = ProUtils.getClosestEnemyOrNeutralLandTerritoryDistance(data, player, t, territoryValueMap);
      if (enemyDistance <= 0) {
        enemyDistance = 10;
      }
      final int fodderPercent = 80 - (enemyDistance * 5);
      ProLogger.debug(t + ", enemyDistance=" + enemyDistance + ", fodderPercent=" + fodderPercent);
      final Set<Territory> neighbors =
          data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveLandUnits(player, data, false));
      neighbors.add(t);
      final List<Unit> ownedLocalUnits = new ArrayList<>();
      for (final Territory neighbor : neighbors) {
        ownedLocalUnits.addAll(neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
      }

      // Check for unplaced units
      final List<Unit> unitsToPlace = new ArrayList<>();
      for (final Iterator<Unit> it = unplacedUnits.iterator(); it.hasNext();) {
        final Unit u = it.next();
        if ((remainingUnitProduction > 0)
            && ProPurchaseUtils.canUnitsBePlaced(Collections.singletonList(u), player, t, isBid)) {
          remainingUnitProduction--;
          unitsToPlace.add(u);
          it.remove();
          ProLogger.trace("Selected unplaced unit=" + u);
        }
      }

      // Purchase as many units as possible
      int addedFodderUnits = 0;
      double attackAndDefenseDifference = 0;
      boolean selectFodderUnit = true;
      while (true) {

        // Remove options that cost too much PUs or production
        ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, landFodderOptions, resourceTracker,
            remainingUnitProduction, unitsToPlace, purchaseTerritories);
        ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, landAttackOptions, resourceTracker,
            remainingUnitProduction, unitsToPlace, purchaseTerritories);
        ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, landDefenseOptions, resourceTracker,
            remainingUnitProduction, unitsToPlace, purchaseTerritories);

        // Select purchase option
        Optional<ProPurchaseOption> optionalSelectedOption = Optional.empty();
        if (!selectFodderUnit && (attackAndDefenseDifference > 0) && !landDefenseOptions.isEmpty()) {
          final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : landDefenseOptions) {
            defenseEfficiencies.put(ppo, ppo.getDefenseEfficiency2(enemyDistance, data, ownedLocalUnits, unitsToPlace));
          }
          optionalSelectedOption = ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Land Defense");
        } else if (!selectFodderUnit && !landAttackOptions.isEmpty()) {
          final Map<ProPurchaseOption, Double> attackEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : landAttackOptions) {
            attackEfficiencies.put(ppo, ppo.getAttackEfficiency2(enemyDistance, data, ownedLocalUnits, unitsToPlace));
          }
          optionalSelectedOption = ProPurchaseUtils.randomizePurchaseOption(attackEfficiencies, "Land Attack");
        } else if (!landFodderOptions.isEmpty()) {
          final Map<ProPurchaseOption, Double> fodderEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : landFodderOptions) {
            fodderEfficiencies.put(ppo, ppo.getFodderEfficiency(enemyDistance, data, ownedLocalUnits, unitsToPlace));
          }
          optionalSelectedOption = ProPurchaseUtils.randomizePurchaseOption(fodderEfficiencies, "Land Fodder");
          if (optionalSelectedOption.isPresent()) {
            addedFodderUnits += optionalSelectedOption.get().getQuantity();
          }
        }
        if (!optionalSelectedOption.isPresent()) {
          break;
        }
        final ProPurchaseOption selectedOption = optionalSelectedOption.get();

        // Create new temp units
        resourceTracker.purchase(selectedOption);
        remainingUnitProduction -= selectedOption.getQuantity();
        unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
        attackAndDefenseDifference += (selectedOption.getAttack() - selectedOption.getDefense());
        selectFodderUnit = (((double) addedFodderUnits / unitsToPlace.size()) * 100) <= fodderPercent;
        ProLogger.trace("Selected unit=" + selectedOption.getUnitType().getName());
      }

      // Add units to place territory
      placeTerritory.getPlaceUnits().addAll(unitsToPlace);
      ProLogger.debug(t + ", placedUnits=" + unitsToPlace);
    }
  }

  private void purchaseFactory(final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedLandTerritories, final ProPurchaseOptionMap purchaseOptions,
      final boolean hasExtraPUs) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase factory with resources: " + resourceTracker + ", hasExtraPUs=" + hasExtraPUs);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Only try to purchase a factory if all production was used in prioritized land territories
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      for (final Territory t : purchaseTerritories.keySet()) {
        if (placeTerritory.getTerritory().equals(t) && (purchaseTerritories.get(t).getRemainingUnitProduction() > 0)) {
          ProLogger.debug("Not purchasing a factory since remaining land production in " + t);
          return;
        }
      }
    }

    // Find all owned land territories that weren't conquered and don't already have a factory
    final List<Territory> possibleFactoryTerritories = CollectionUtils.getMatches(data.getMap().getTerritories(),
        ProMatches.territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(player, data));
    possibleFactoryTerritories.removeAll(factoryPurchaseTerritories.keySet());
    final Set<Territory> purchaseFactoryTerritories = new HashSet<>();
    final List<Territory> territoriesThatCantBeHeld = new ArrayList<>();
    for (final Territory t : possibleFactoryTerritories) {

      // Only consider territories with production of at least 3 unless there are still remaining PUs
      final int production = TerritoryAttachment.get(t).getProduction();
      if (((production < 3) && !hasExtraPUs) || (production < 2)) {
        continue;
      }

      // Check if no enemy attackers and that it wasn't conquered this turn
      if (enemyAttackOptions.getMax(t) == null) {
        purchaseFactoryTerritories.add(t);
        ProLogger.trace("Possible factory since no enemy attackers: " + t.getName());
      } else {

        // Find current battle result
        final List<Unit> defenders = t.getUnits().getMatches(Matches.isUnitAllied(player, data));
        final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        final ProBattleResult result = calc.estimateDefendBattleResults(t, new ArrayList<>(enemyAttackingUnits),
            defenders, enemyAttackOptions.getMax(t).getMaxBombardUnits());

        // Check if it can't be held or if it can then that it wasn't conquered this turn
        if (result.isHasLandUnitRemaining() || (result.getTuvSwing() > 0)) {
          territoriesThatCantBeHeld.add(t);
          ProLogger.trace("Can't hold territory: " + t.getName() + ", hasLandUnitRemaining="
              + result.isHasLandUnitRemaining() + ", TUVSwing=" + result.getTuvSwing() + ", enemyAttackers="
              + enemyAttackingUnits.size() + ", myDefenders=" + defenders.size());
        } else {
          purchaseFactoryTerritories.add(t);
          ProLogger.trace("Possible factory: " + t.getName() + ", hasLandUnitRemaining="
              + result.isHasLandUnitRemaining() + ", TUVSwing=" + result.getTuvSwing() + ", enemyAttackers="
              + enemyAttackingUnits.size() + ", myDefenders=" + defenders.size());
        }
      }
    }
    ProLogger.debug("Possible factory territories: " + purchaseFactoryTerritories);

    // Remove any territories that don't have local land superiority
    if (!hasExtraPUs) {
      for (final Iterator<Territory> it = purchaseFactoryTerritories.iterator(); it.hasNext();) {
        final Territory t = it.next();
        if (!ProBattleUtils.territoryHasLocalLandSuperiority(t, ProBattleUtils.MEDIUM_RANGE, player,
            purchaseTerritories)) {
          it.remove();
        }
      }
      ProLogger.debug("Possible factory territories that have land superiority: " + purchaseFactoryTerritories);
    }

    // Find strategic value for each territory
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(player, territoriesThatCantBeHeld, new ArrayList<>());
    double maxValue = 0.0;
    Territory maxTerritory = null;
    for (final Territory t : purchaseFactoryTerritories) {
      final int production = TerritoryAttachment.get(t).getProduction();
      final double value = (territoryValueMap.get(t) * production) + (0.1 * production);
      final boolean isAdjacentToSea = Matches.territoryHasNeighborMatching(data, Matches.territoryIsWater()).test(t);
      final Set<Territory> nearbyLandTerritories =
          data.getMap().getNeighbors(t, 9, ProMatches.territoryCanMoveLandUnits(player, data, false));
      final int numNearbyEnemyTerritories =
          CollectionUtils.countMatches(nearbyLandTerritories, Matches.isTerritoryEnemy(player, data));
      ProLogger.trace(t + ", strategic value=" + territoryValueMap.get(t) + ", value=" + value
          + ", numNearbyEnemyTerritories=" + numNearbyEnemyTerritories);
      if (value > maxValue
          && ((numNearbyEnemyTerritories >= 4 && territoryValueMap.get(t) >= 1) || (isAdjacentToSea && hasExtraPUs))) {
        maxValue = value;
        maxTerritory = t;
      }
    }
    ProLogger.debug("Try to purchase factory for territory: " + maxTerritory);

    // Determine whether to purchase factory
    if (maxTerritory != null) {

      // Find most expensive placed land unit to consider removing for a factory
      ProPurchaseOption maxPlacedOption = null;
      ProPlaceTerritory maxPlacedTerritory = null;
      Unit maxPlacedUnit = null;
      for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
        for (final Unit u : placeTerritory.getPlaceUnits()) {
          for (final ProPurchaseOption ppo : purchaseOptions.getLandOptions()) {
            if (u.getType().equals(ppo.getUnitType()) && (ppo.getQuantity() == 1)
                && ((maxPlacedOption == null) || (ppo.getCost() >= maxPlacedOption.getCost()))) {
              maxPlacedOption = ppo;
              maxPlacedTerritory = placeTerritory;
              maxPlacedUnit = u;
            }
          }
        }
      }

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getFactoryOptions(), maxTerritory,
              isBid);
      resourceTracker.removeTempPurchase(maxPlacedOption);
      ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, purchaseOptionsForTerritory,
          resourceTracker, 1, new ArrayList<>(), purchaseTerritories);
      resourceTracker.clearTempPurchases();

      // Determine most expensive factory option (currently doesn't buy mobile factories)
      ProPurchaseOption bestFactoryOption = null;
      double maxFactoryEfficiency = 0;
      for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
        if ((ppo.getMovement() == 0) && (ppo.getCost() > maxFactoryEfficiency)) {
          bestFactoryOption = ppo;
          maxFactoryEfficiency = ppo.getCost();
        }
      }

      // Check if there are enough PUs to buy a factory
      if (bestFactoryOption != null) {
        ProLogger.debug("Best factory unit: " + bestFactoryOption.getUnitType().getName());
        final ProPurchaseTerritory factoryPurchaseTerritory = new ProPurchaseTerritory(maxTerritory, data, player, 0);
        factoryPurchaseTerritories.put(maxTerritory, factoryPurchaseTerritory);
        for (final ProPlaceTerritory ppt : factoryPurchaseTerritory.getCanPlaceTerritories()) {
          if (ppt.getTerritory().equals(maxTerritory)) {
            final List<Unit> factory =
                bestFactoryOption.getUnitType().create(bestFactoryOption.getQuantity(), player, true);
            ppt.getPlaceUnits().addAll(factory);
            if (resourceTracker.hasEnough(bestFactoryOption)) {
              resourceTracker.purchase(bestFactoryOption);
              ProLogger.debug(maxTerritory + ", placedFactory=" + factory);
            } else {
              resourceTracker.purchase(bestFactoryOption);
              resourceTracker.removePurchase(maxPlacedOption);
              maxPlacedTerritory.getPlaceUnits().remove(maxPlacedUnit);
              ProLogger.debug(maxTerritory + ", placedFactory=" + factory + ", removedUnit=" + maxPlacedUnit);
            }
          }
        }
      }
    }
  }

  private List<ProPlaceTerritory> prioritizeSeaTerritories(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    ProLogger.info("Prioritize sea territories");

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Determine which sea territories can be placed in
    final Set<ProPlaceTerritory> seaPlaceTerritories = new HashSet<>();
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if (t.isWater() && (placeTerritory.getStrategicValue() > 0) && placeTerritory.isCanHold()) {
          seaPlaceTerritories.add(placeTerritory);
        }
      }
    }

    // Calculate value of territory
    ProLogger.debug("Determine sea place value:");
    for (final ProPlaceTerritory placeTerritory : seaPlaceTerritories) {
      final Territory t = placeTerritory.getTerritory();

      // Find number of local naval units
      final List<Unit> units = new ArrayList<>(placeTerritory.getDefendingUnits());
      units.addAll(ProPurchaseUtils.getPlaceUnits(t, purchaseTerritories));
      final List<Unit> myUnits = CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(player));
      final int numMyTransports = CollectionUtils.countMatches(myUnits, Matches.unitIsTransport());
      final int numSeaDefenders = CollectionUtils.countMatches(units, Matches.unitIsNotTransport());

      // Determine needed defense strength
      int needDefenders = 0;
      if (enemyAttackOptions.getMax(t) != null) {
        final double strengthDifference =
            ProBattleUtils.estimateStrengthDifference(t, enemyAttackOptions.getMax(t).getMaxUnits(), units);
        if (strengthDifference > 50) {
          needDefenders = 1;
        }
      }
      final boolean hasLocalNavalSuperiority =
          ProBattleUtils.territoryHasLocalNavalSuperiority(t, player, null, new ArrayList<>());
      if (!hasLocalNavalSuperiority) {
        needDefenders = 1;
      }

      // Calculate sea value for prioritization
      final double territoryValue =
          (placeTerritory.getStrategicValue() * (1 + numMyTransports + (0.1 * numSeaDefenders))) / (1 + (3
              * needDefenders));
      ProLogger.debug(t + ", value=" + territoryValue + ", strategicValue=" + placeTerritory.getStrategicValue()
          + ", numMyTransports=" + numMyTransports + ", numSeaDefenders=" + numSeaDefenders + ", needDefenders="
          + needDefenders);
      placeTerritory.setStrategicValue(territoryValue);
    }

    // Sort territories by value
    final List<ProPlaceTerritory> sortedTerritories = new ArrayList<>(seaPlaceTerritories);
    Collections.sort(sortedTerritories, (t1, t2) -> {
      final double value1 = t1.getStrategicValue();
      final double value2 = t2.getStrategicValue();
      return Double.compare(value2, value1);
    });
    ProLogger.debug("Sorted sea territories:");
    for (final ProPlaceTerritory placeTerritory : sortedTerritories) {
      ProLogger.debug(placeTerritory.toString() + " value=" + placeTerritory.getStrategicValue());
    }
    return sortedTerritories;
  }

  private void purchaseSeaAndAmphibUnits(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedSeaTerritories, final Map<Territory, Double> territoryValueMap,
      final ProPurchaseOptionMap purchaseOptions) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase sea and amphib units with resources: " + resourceTracker);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through prioritized territories and purchase sea units
    for (final ProPlaceTerritory placeTerritory : prioritizedSeaTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking sea place for " + t.getName());

      // Find all purchase territories for place territory
      final List<ProPurchaseTerritory> selectedPurchaseTerritories =
          getPurchaseTerritories(placeTerritory, purchaseTerritories);

      // Find local owned units
      final Set<Territory> neighbors =
          data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveSeaUnits(player, data, false));
      neighbors.add(t);
      final List<Unit> ownedLocalUnits = new ArrayList<>();
      for (final Territory neighbor : neighbors) {
        ownedLocalUnits.addAll(neighbor.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
      }
      int unusedCarrierCapacity = Math.min(0, ProTransportUtils.getUnusedCarrierCapacity(player, t, new ArrayList<>()));
      int unusedLocalCarrierCapacity = ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, new ArrayList<>());
      boolean needDestroyer = false;
      ProLogger.trace(t + ", unusedCarrierCapacity=" + unusedCarrierCapacity + ", unusedLocalCarrierCapacity="
          + unusedLocalCarrierCapacity);

      // If any enemy attackers then purchase sea defenders until it can be held
      if (enemyAttackOptions.getMax(t) != null) {

        // Determine if need destroyer
        if (enemyAttackOptions.getMax(t).getMaxUnits().stream().anyMatch(Matches.unitIsSub())
            && t.getUnits().getMatches(Matches.unitIsOwnedBy(player)).stream().noneMatch(Matches.unitIsDestroyer())) {
          needDestroyer = true;
        }
        ProLogger.trace(t + ", needDestroyer=" + needDestroyer + ", checking defense since has enemy attackers: "
            + enemyAttackOptions.getMax(t).getMaxUnits());
        final List<Unit> unitsToPlace = new ArrayList<>();
        final List<Unit> initialDefendingUnits = new ArrayList<>(placeTerritory.getDefendingUnits());
        initialDefendingUnits.addAll(ProPurchaseUtils.getPlaceUnits(t, purchaseTerritories));
        ProBattleResult result = calc.calculateBattleResults(t, enemyAttackOptions.getMax(t).getMaxUnits(),
            initialDefendingUnits, enemyAttackOptions.getMax(t).getMaxBombardUnits());
        boolean hasOnlyRetreatingSubs =
            Properties.getSubRetreatBeforeBattle(data)
                && !initialDefendingUnits.isEmpty() && initialDefendingUnits.stream().allMatch(Matches.unitIsSub())
                && enemyAttackOptions.getMax(t).getMaxUnits().stream().noneMatch(Matches.unitIsDestroyer());
        for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {

          // Check remaining production
          int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
          ProLogger.trace(t + ", purchaseTerritory=" + purchaseTerritory.getTerritory() + ", remainingUnitProduction="
              + remainingUnitProduction);
          if (remainingUnitProduction <= 0) {
            continue;
          }

          // Determine sea and transport units that can be produced in this territory
          final List<ProPurchaseOption> seaPurchaseOptionsForTerritory =
              ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getSeaDefenseOptions(), t,
                  isBid);
          seaPurchaseOptionsForTerritory.addAll(purchaseOptions.getAirOptions());

          // Purchase enough sea defenders to hold territory
          while (true) {

            // If it can be held then break
            if (!hasOnlyRetreatingSubs
                && ((result.getTuvSwing() < -1) || (result.getWinPercentage() < ProData.winPercentage))) {
              break;
            }

            // Select purchase option
            ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, seaPurchaseOptionsForTerritory,
                resourceTracker, remainingUnitProduction, unitsToPlace, purchaseTerritories);
            final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
            for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory) {
              defenseEfficiencies.put(ppo, ppo.getSeaDefenseEfficiency(data, ownedLocalUnits, unitsToPlace,
                  needDestroyer, unusedCarrierCapacity, unusedLocalCarrierCapacity));
            }
            final Optional<ProPurchaseOption> optionalSelectedOption =
                ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Sea Defense");
            if (!optionalSelectedOption.isPresent()) {
              break;
            }
            final ProPurchaseOption selectedOption = optionalSelectedOption.get();
            if (selectedOption.isDestroyer()) {
              needDestroyer = false;
            }

            // Create new temp defenders
            resourceTracker.tempPurchase(selectedOption);
            remainingUnitProduction -= selectedOption.getQuantity();
            unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
            if (selectedOption.isCarrier() || selectedOption.isAir()) {
              unusedCarrierCapacity = ProTransportUtils.getUnusedCarrierCapacity(player, t, unitsToPlace);
              unusedLocalCarrierCapacity = ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, unitsToPlace);
            }
            ProLogger
                .trace(t + ", added sea defender for defense: " + selectedOption.getUnitType().getName() + ", TUVSwing="
                    + result.getTuvSwing() + ", win%=" + result.getWinPercentage() + ", unusedCarrierCapacity="
                    + unusedCarrierCapacity + ", unusedLocalCarrierCapacity=" + unusedLocalCarrierCapacity);

            // Find current battle result
            final List<Unit> defendingUnits = new ArrayList<>(placeTerritory.getDefendingUnits());
            defendingUnits.addAll(ProPurchaseUtils.getPlaceUnits(t, purchaseTerritories));
            defendingUnits.addAll(unitsToPlace);
            result = calc.estimateDefendBattleResults(t, enemyAttackOptions.getMax(t).getMaxUnits(),
                defendingUnits, enemyAttackOptions.getMax(t).getMaxBombardUnits());
            hasOnlyRetreatingSubs =
                Properties.getSubRetreatBeforeBattle(data) && !defendingUnits.isEmpty()
                    && defendingUnits.stream().allMatch(Matches.unitIsSub())
                    && enemyAttackOptions.getMax(t).getMaxUnits().stream().noneMatch(Matches.unitIsDestroyer());
          }
        }

        // Check to see if its worth trying to defend the territory
        if ((result.getTuvSwing() < 0) || (result.getWinPercentage() < ProData.winPercentage)) {
          resourceTracker.confirmTempPurchases();
          ProLogger.trace(t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + result.getTuvSwing()
              + ", hasLandUnitRemaining=" + result.isHasLandUnitRemaining());
          addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
        } else {
          resourceTracker.clearTempPurchases();
          setCantHoldPlaceTerritory(placeTerritory, purchaseTerritories);
          ProLogger.trace(t + ", can't defend TUVSwing=" + result.getTuvSwing() + ", win%=" + result.getWinPercentage()
              + ", tried to placeDefenders=" + unitsToPlace + ", enemyAttackers="
              + enemyAttackOptions.getMax(t).getMaxUnits());
          continue;
        }
      }

      // TODO: update to use ProBattleUtils method
      // Check to see if local naval superiority
      int landDistance = ProUtils.getClosestEnemyLandTerritoryDistanceOverWater(data, player, t);
      if (landDistance <= 0) {
        landDistance = 10;
      }
      final int enemyDistance = Math.max(3, (landDistance + 1));
      final int alliedDistance = (enemyDistance + 1) / 2;
      final Set<Territory> nearbyTerritories =
          data.getMap().getNeighbors(t, enemyDistance, ProMatches.territoryCanMoveAirUnits(player, data, false));
      final List<Territory> nearbyLandTerritories =
          CollectionUtils.getMatches(nearbyTerritories, Matches.territoryIsLand());
      final Set<Territory> nearbyEnemySeaTerritories =
          data.getMap().getNeighbors(t, enemyDistance, Matches.territoryIsWater());
      nearbyEnemySeaTerritories.add(t);
      final Set<Territory> nearbyAlliedSeaTerritories =
          data.getMap().getNeighbors(t, alliedDistance, Matches.territoryIsWater());
      nearbyAlliedSeaTerritories.add(t);
      final List<Unit> enemyUnitsInSeaTerritories = new ArrayList<>();
      final List<Unit> enemyUnitsInLandTerritories = new ArrayList<>();
      final List<Unit> myUnitsInSeaTerritories = new ArrayList<>();
      for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
        enemyUnitsInLandTerritories
            .addAll(nearbyLandTerritory.getUnits().getMatches(ProMatches.unitIsEnemyAir(player, data)));
      }
      for (final Territory nearbySeaTerritory : nearbyEnemySeaTerritories) {
        final List<Unit> enemySeaUnits =
            nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsEnemyNotLand(player, data));
        if (enemySeaUnits.isEmpty()) {
          continue;
        }
        final Route route = data.getMap().getRoute_IgnoreEnd(t, nearbySeaTerritory, Matches.territoryIsWater());
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
        myUnitsInSeaTerritories
            .addAll(nearbySeaTerritory.getUnits().getMatches(ProMatches.unitIsOwnedNotLand(player)));
        myUnitsInSeaTerritories.addAll(ProPurchaseUtils.getPlaceUnits(nearbySeaTerritory, purchaseTerritories));
      }

      // Check if destroyer is needed
      final int numEnemySubs = CollectionUtils.countMatches(enemyUnitsInSeaTerritories, Matches.unitIsSub());
      final int numMyDestroyers = CollectionUtils.countMatches(myUnitsInSeaTerritories, Matches.unitIsDestroyer());
      if (numEnemySubs > (2 * numMyDestroyers)) {
        needDestroyer = true;
      }
      ProLogger.trace(t + ", enemyDistance=" + enemyDistance + ", alliedDistance=" + alliedDistance + ", enemyAirUnits="
          + enemyUnitsInLandTerritories + ", enemySeaUnits=" + enemyUnitsInSeaTerritories + ", mySeaUnits="
          + myUnitsInSeaTerritories + ", needDestroyer=" + needDestroyer);

      // Purchase naval defenders until I have local naval superiority
      final List<Unit> unitsToPlace = new ArrayList<>();
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {

        // Check remaining production
        int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
        ProLogger.trace(t + ", purchaseTerritory=" + purchaseTerritory.getTerritory() + ", remainingUnitProduction="
            + remainingUnitProduction);
        if (remainingUnitProduction <= 0) {
          continue;
        }

        // Determine sea and transport units that can be produced in this territory
        final List<ProPurchaseOption> seaPurchaseOptionsForTerritory =
            ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getSeaDefenseOptions(), t, isBid);
        seaPurchaseOptionsForTerritory.addAll(purchaseOptions.getAirOptions());
        while (true) {

          // If I have naval attack/defense superiority then break
          if (ProBattleUtils.territoryHasLocalNavalSuperiority(t, player, purchaseTerritories, unitsToPlace)) {
            break;
          }

          // Select purchase option
          ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, seaPurchaseOptionsForTerritory,
              resourceTracker, remainingUnitProduction, unitsToPlace, purchaseTerritories);
          final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory) {
            defenseEfficiencies.put(ppo, ppo.getSeaDefenseEfficiency(data, ownedLocalUnits, unitsToPlace, needDestroyer,
                unusedCarrierCapacity, unusedLocalCarrierCapacity));
          }
          final Optional<ProPurchaseOption> optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Sea Defense");
          if (!optionalSelectedOption.isPresent()) {
            break;
          }
          final ProPurchaseOption selectedOption = optionalSelectedOption.get();
          if (selectedOption.isDestroyer()) {
            needDestroyer = false;
          }

          // Create new temp units
          resourceTracker.purchase(selectedOption);
          remainingUnitProduction -= selectedOption.getQuantity();
          unitsToPlace.addAll(selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true));
          if (selectedOption.isCarrier() || selectedOption.isAir()) {
            unusedCarrierCapacity = ProTransportUtils.getUnusedCarrierCapacity(player, t, unitsToPlace);
            unusedLocalCarrierCapacity = ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, unitsToPlace);
          }
          ProLogger.trace(t + ", added sea defender for naval superiority: " + selectedOption.getUnitType().getName()
              + ", unusedCarrierCapacity=" + unusedCarrierCapacity + ", unusedLocalCarrierCapacity="
              + unusedLocalCarrierCapacity);
        }
      }

      // Add sea defender units to place territory
      addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);

      // Loop through adjacent purchase territories and purchase transport/amphib units
      final int distance = ProTransportUtils.findMaxMovementForTransports(purchaseOptions.getSeaTransportOptions());
      ProLogger.trace(t + ", transportMovement=" + distance);
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {
        final Territory landTerritory = purchaseTerritory.getTerritory();

        // Check if territory can produce units and has remaining production
        int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
        ProLogger
            .trace(t + ", purchaseTerritory=" + landTerritory + ", remainingUnitProduction=" + remainingUnitProduction);
        if (remainingUnitProduction <= 0) {
          continue;
        }

        // Find local owned units
        final List<Unit> ownedLocalAmphibUnits = landTerritory.getUnits().getMatches(Matches.unitIsOwnedBy(player));

        // Determine sea and transport units that can be produced in this territory
        final List<ProPurchaseOption> seaTransportPurchaseOptionsForTerritory =
            ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getSeaTransportOptions(), t,
                isBid);
        final List<ProPurchaseOption> amphibPurchaseOptionsForTerritory =
            ProPurchaseUtils.findPurchaseOptionsForTerritory(player, purchaseOptions.getLandOptions(), landTerritory,
                isBid);

        // Find transports that need loaded and units to ignore that are already paired up
        final List<Unit> transportsThatNeedUnits = new ArrayList<>();
        final Set<Unit> potentialUnitsToLoad = new HashSet<>();
        final Set<Territory> seaTerritories = data.getMap().getNeighbors(landTerritory, distance,
            ProMatches.territoryCanMoveSeaUnits(player, data, false));
        for (final Territory seaTerritory : seaTerritories) {
          final List<Unit> unitsInTerritory = ProPurchaseUtils.getPlaceUnits(seaTerritory, purchaseTerritories);
          unitsInTerritory.addAll(seaTerritory.getUnits().getUnits());
          final List<Unit> transports =
              CollectionUtils.getMatches(unitsInTerritory, ProMatches.unitIsOwnedTransport(player));
          for (final Unit transport : transports) {
            transportsThatNeedUnits.add(transport);
            final Set<Territory> territoriesToLoadFrom =
                new HashSet<>(data.getMap().getNeighbors(seaTerritory, distance));
            for (final Iterator<Territory> it = territoriesToLoadFrom.iterator(); it.hasNext();) {
              final Territory potentialTerritory = it.next();
              if (potentialTerritory.isWater() || (territoryValueMap.get(potentialTerritory) > 0.25)) {
                it.remove();
              }
            }
            final List<Unit> units =
                ProTransportUtils.getUnitsToTransportFromTerritories(player, transport, territoriesToLoadFrom,
                    new ArrayList<>(potentialUnitsToLoad), ProMatches.unitIsOwnedCombatTransportableUnit(player));
            potentialUnitsToLoad.addAll(units);
          }
        }

        // Determine whether transports, amphib units, or both are needed
        final Set<Territory> landNeighbors = data.getMap().getNeighbors(t, Matches.territoryIsLand());
        for (final Territory neighbor : landNeighbors) {
          if (territoryValueMap.get(neighbor) <= 0.25) {
            final List<Unit> unitsInTerritory = new ArrayList<>(neighbor.getUnits().getUnits());
            unitsInTerritory.addAll(ProPurchaseUtils.getPlaceUnits(neighbor, purchaseTerritories));
            potentialUnitsToLoad.addAll(
                CollectionUtils.getMatches(unitsInTerritory, ProMatches.unitIsOwnedCombatTransportableUnit(player)));
          }
        }
        ProLogger.trace(t + ", potentialUnitsToLoad=" + potentialUnitsToLoad + ", transportsThatNeedUnits="
            + transportsThatNeedUnits);

        // Purchase transports and amphib units
        final List<Unit> amphibUnitsToPlace = new ArrayList<>();
        final List<Unit> transportUnitsToPlace = new ArrayList<>();
        while (true) {
          if (!transportsThatNeedUnits.isEmpty()) {

            // Get next empty transport and find its capacity
            final Unit transport = transportsThatNeedUnits.get(0);
            int transportCapacity = UnitAttachment.get(transport.getType()).getTransportCapacity();

            // Find any existing units that can be transported
            final List<Unit> selectedUnits =
                ProTransportUtils.selectUnitsToTransportFromList(transport, new ArrayList<>(potentialUnitsToLoad));
            if (!selectedUnits.isEmpty()) {
              potentialUnitsToLoad.removeAll(selectedUnits);
              transportCapacity -= ProTransportUtils.findUnitsTransportCost(selectedUnits);
            }

            // Purchase units until transport is full
            while (transportCapacity > 0) {

              // Select amphib purchase option and add units
              ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, amphibPurchaseOptionsForTerritory,
                  resourceTracker, remainingUnitProduction, amphibUnitsToPlace, purchaseTerritories);
              final Map<ProPurchaseOption, Double> amphibEfficiencies = new HashMap<>();
              for (final ProPurchaseOption ppo : amphibPurchaseOptionsForTerritory) {
                if (ppo.getTransportCost() <= transportCapacity) {
                  amphibEfficiencies.put(ppo, ppo.getAmphibEfficiency(data, ownedLocalAmphibUnits, amphibUnitsToPlace));
                }
              }
              final Optional<ProPurchaseOption> optionalSelectedOption =
                  ProPurchaseUtils.randomizePurchaseOption(amphibEfficiencies, "Amphib");
              if (!optionalSelectedOption.isPresent()) {
                break;
              }
              final ProPurchaseOption ppo = optionalSelectedOption.get();

              // Add amphib unit
              final List<Unit> amphibUnits = ppo.getUnitType().create(ppo.getQuantity(), player, true);
              amphibUnitsToPlace.addAll(amphibUnits);
              resourceTracker.purchase(ppo);
              remainingUnitProduction -= ppo.getQuantity();
              transportCapacity -= ppo.getTransportCost();
              ProLogger.trace("Selected unit=" + ppo.getUnitType().getName());
            }
            transportsThatNeedUnits.remove(transport);
          } else {

            // Select purchase option
            ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData,
                seaTransportPurchaseOptionsForTerritory, resourceTracker, remainingUnitProduction,
                transportUnitsToPlace, purchaseTerritories);
            final Map<ProPurchaseOption, Double> transportEfficiencies = new HashMap<>();
            for (final ProPurchaseOption ppo : seaTransportPurchaseOptionsForTerritory) {
              transportEfficiencies.put(ppo, ppo.getTransportEfficiencyRatio());
            }
            final Optional<ProPurchaseOption> optionalSelectedOption =
                ProPurchaseUtils.randomizePurchaseOption(transportEfficiencies, "Sea Transport");
            if (!optionalSelectedOption.isPresent()) {
              break;
            }
            final ProPurchaseOption ppo = optionalSelectedOption.get();

            // Add transports
            final List<Unit> transports = ppo.getUnitType().create(ppo.getQuantity(), player, true);
            transportUnitsToPlace.addAll(transports);
            resourceTracker.purchase(ppo);
            remainingUnitProduction -= ppo.getQuantity();
            transportsThatNeedUnits.addAll(transports);
            ProLogger.trace("Selected unit=" + ppo.getUnitType().getName() + ", potentialUnitsToLoad="
                + potentialUnitsToLoad + ", transportsThatNeedUnits=" + transportsThatNeedUnits);
          }
        }

        // Add transport units to sea place territory and amphib units to land place territory
        for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {
          if (landTerritory.equals(ppt.getTerritory())) {
            ppt.getPlaceUnits().addAll(amphibUnitsToPlace);
          } else if (placeTerritory.equals(ppt)) {
            ppt.getPlaceUnits().addAll(transportUnitsToPlace);
          }
        }
        ProLogger.trace(t + ", purchaseTerritory=" + landTerritory + ", transportUnitsToPlace=" + transportUnitsToPlace
            + ", amphibUnitsToPlace=" + amphibUnitsToPlace);
      }
    }
  }

  private void purchaseUnitsWithRemainingProduction(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPurchaseOption> landPurchaseOptions, final List<ProPurchaseOption> airPurchaseOptions) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase units in territories with remaining production with resources: " + resourceTracker);

    // Get all safe/unsafe land place territories with remaining production
    final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<>();
    final List<ProPlaceTerritory> prioritizedCantHoldLandTerritories = new ArrayList<>();
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if (!t.isWater()
            && placeTerritory.isCanHold()
            && (purchaseTerritories.get(t).getRemainingUnitProduction() > 0)) {
          prioritizedLandTerritories.add(placeTerritory);
        } else if (!t.isWater() && (purchaseTerritories.get(t).getRemainingUnitProduction() > 0)) {
          prioritizedCantHoldLandTerritories.add(placeTerritory);
        }
      }
    }

    // Sort territories by value
    Collections.sort(prioritizedLandTerritories, (t1, t2) -> {
      final double value1 = t1.getStrategicValue();
      final double value2 = t2.getStrategicValue();
      return Double.compare(value2, value1);
    });
    ProLogger.debug("Sorted land territories with remaining production: " + prioritizedLandTerritories);

    // Loop through territories and purchase long range attack units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking territory: " + t);

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<>(airPurchaseOptions);
      airAndLandPurchaseOptions.addAll(landPurchaseOptions);
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t, isBid);

      // Purchase long range attack units for any remaining production
      int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
      while (true) {

        // Remove options that cost too much PUs or production
        ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, purchaseOptionsForTerritory,
            resourceTracker, remainingUnitProduction, new ArrayList<>(), purchaseTerritories);
        if (purchaseOptionsForTerritory.isEmpty()) {
          break;
        }

        // Determine best long range attack option (prefer air units)
        ProPurchaseOption bestAttackOption = null;
        double maxAttackEfficiency = 0;
        for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
          double attackEfficiency = (ppo.getAttackEfficiency() * ppo.getMovement()) / ppo.getQuantity();
          if (ppo.isAir()) {
            attackEfficiency *= 10;
          }
          if (attackEfficiency > maxAttackEfficiency) {
            bestAttackOption = ppo;
            maxAttackEfficiency = attackEfficiency;
          }
        }
        if (bestAttackOption == null) {
          break;
        }

        // Purchase unit
        resourceTracker.purchase(bestAttackOption);
        remainingUnitProduction -= bestAttackOption.getQuantity();
        final List<Unit> newUnit = bestAttackOption.getUnitType().create(bestAttackOption.getQuantity(), player, true);
        placeTerritory.getPlaceUnits().addAll(newUnit);
        ProLogger.trace(t + ", addedUnit=" + newUnit);
      }
    }

    // Sort territories by value
    Collections.sort(prioritizedCantHoldLandTerritories, (t1, t2) -> {
      final double value1 = t1.getDefenseValue();
      final double value2 = t2.getDefenseValue();
      return Double.compare(value2, value1);
    });
    ProLogger
        .debug("Sorted can't hold land territories with remaining production: " + prioritizedCantHoldLandTerritories);

    // Loop through territories and purchase defense units
    for (final ProPlaceTerritory placeTerritory : prioritizedCantHoldLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking territory: " + t);

      // Find local owned units
      final List<Unit> ownedLocalUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<>(airPurchaseOptions);
      airAndLandPurchaseOptions.addAll(landPurchaseOptions);
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t, isBid);

      // Purchase defense units for any remaining production
      int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
      while (true) {

        // Select purchase option
        ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, purchaseOptionsForTerritory,
            resourceTracker, remainingUnitProduction, new ArrayList<>(), purchaseTerritories);
        final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
        for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
          defenseEfficiencies.put(ppo, Math.pow(ppo.getCost(), 2)
              * ppo.getDefenseEfficiency2(1, data, ownedLocalUnits, placeTerritory.getPlaceUnits()));
        }
        final Optional<ProPurchaseOption> optionalSelectedOption =
            ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Defense");
        if (!optionalSelectedOption.isPresent()) {
          break;
        }
        final ProPurchaseOption selectedOption = optionalSelectedOption.get();

        // Purchase unit
        resourceTracker.purchase(selectedOption);
        remainingUnitProduction -= selectedOption.getQuantity();
        final List<Unit> newUnit = selectedOption.getUnitType().create(selectedOption.getQuantity(), player, true);
        placeTerritory.getPlaceUnits().addAll(newUnit);
        ProLogger.trace(t + ", addedUnit=" + newUnit);
      }
    }
  }

  private void upgradeUnitsWithRemainingPUs(final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final ProPurchaseOptionMap purchaseOptions) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Upgrade units with resources: " + resourceTracker);

    // Get all safe land place territories
    final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<>();
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if (!t.isWater() && placeTerritory.isCanHold()) {
          prioritizedLandTerritories.add(placeTerritory);
        }
      }
    }

    // Sort territories by ascending value (try upgrading units in far away territories first)
    Collections.sort(prioritizedLandTerritories, (t1, t2) -> {
      final double value1 = t1.getStrategicValue();
      final double value2 = t2.getStrategicValue();
      return Double.compare(value1, value2);
    });
    ProLogger.debug("Sorted land territories: " + prioritizedLandTerritories);

    // Loop through territories and upgrade units to long range attack units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking territory: " + t);

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<>(purchaseOptions.getAirOptions());
      airAndLandPurchaseOptions.addAll(purchaseOptions.getLandOptions());
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseUtils.findPurchaseOptionsForTerritory(player, airAndLandPurchaseOptions, t, isBid);

      // Purchase long range attack units for any remaining production
      int remainingUpgradeUnits = purchaseTerritories.get(t).getUnitProduction() / 3;
      while (true) {
        if (remainingUpgradeUnits <= 0) {
          break;
        }

        // Find cheapest placed purchase option
        ProPurchaseOption minPurchaseOption = null;
        for (final Unit u : placeTerritory.getPlaceUnits()) {
          for (final ProPurchaseOption ppo : airAndLandPurchaseOptions) {
            if (u.getType().equals(ppo.getUnitType())
                && ((minPurchaseOption == null) || (ppo.getCost() < minPurchaseOption.getCost()))) {
              minPurchaseOption = ppo;
            }
          }
        }
        if (minPurchaseOption == null) {
          break;
        }

        // Remove options that cost too much PUs or production
        resourceTracker.removeTempPurchase(minPurchaseOption);
        ProPurchaseUtils.removeInvalidPurchaseOptions(player, startOfTurnData, purchaseOptionsForTerritory,
            resourceTracker, 1, new ArrayList<>(), purchaseTerritories);
        resourceTracker.clearTempPurchases();
        if (purchaseOptionsForTerritory.isEmpty()) {
          break;
        }

        // Determine best long range attack option (prefer air units)
        ProPurchaseOption bestAttackOption = null;
        double maxAttackEfficiency = (minPurchaseOption.getAttackEfficiency() * minPurchaseOption.getMovement()
            * minPurchaseOption.getCost()) / minPurchaseOption.getQuantity();
        for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
          if ((ppo.getCost() > minPurchaseOption.getCost()) && (ppo.isAir() || (placeTerritory.getStrategicValue()
              >= 0.25)
              || (ppo.getTransportCost() <= minPurchaseOption.getTransportCost()))) {
            double attackEfficiency =
                (ppo.getAttackEfficiency() * ppo.getMovement() * ppo.getCost()) / ppo.getQuantity();
            if (ppo.isAir()) {
              attackEfficiency *= 10;
            }
            if (ppo.getCarrierCost() > 0) {
              final int unusedLocalCarrierCapacity =
                  ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, placeTerritory.getPlaceUnits());
              final int neededFighters = unusedLocalCarrierCapacity / ppo.getCarrierCost();
              attackEfficiency *= (1 + neededFighters);
            }
            if (attackEfficiency > maxAttackEfficiency) {
              bestAttackOption = ppo;
              maxAttackEfficiency = attackEfficiency;
            }
          }
        }
        if (bestAttackOption == null) {
          airAndLandPurchaseOptions.remove(minPurchaseOption);
          continue;
        }

        // Find units to remove
        final List<Unit> unitsToRemove = new ArrayList<>();
        int numUnitsToRemove = minPurchaseOption.getQuantity();
        for (final Unit u : placeTerritory.getPlaceUnits()) {
          if (numUnitsToRemove <= 0) {
            break;
          }
          if (u.getType().equals(minPurchaseOption.getUnitType())) {
            unitsToRemove.add(u);
            numUnitsToRemove--;
          }
        }
        if (numUnitsToRemove > 0) {
          airAndLandPurchaseOptions.remove(minPurchaseOption);
          continue;
        }

        // Replace units
        resourceTracker.removePurchase(minPurchaseOption);
        remainingUpgradeUnits -= minPurchaseOption.getQuantity();
        placeTerritory.getPlaceUnits().removeAll(unitsToRemove);
        ProLogger.trace(t + ", removedUnits=" + unitsToRemove);
        for (int i = 0; i < unitsToRemove.size(); i++) {
          if (resourceTracker.hasEnough(bestAttackOption)) {
            resourceTracker.purchase(bestAttackOption);
            final List<Unit> newUnit =
                bestAttackOption.getUnitType().create(bestAttackOption.getQuantity(), player, true);
            placeTerritory.getPlaceUnits().addAll(newUnit);
            ProLogger.trace(t + ", addedUnit=" + newUnit);
          }
        }
      }
    }
  }

  private IntegerMap<ProductionRule> populateProductionRuleMap(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories, final ProPurchaseOptionMap purchaseOptions) {

    ProLogger.info("Populate production rule map");
    final List<Unit> unplacedUnits = player.getUnits().getMatches(Matches.unitIsNotSea());
    final IntegerMap<ProductionRule> purchaseMap = new IntegerMap<>();
    for (final ProPurchaseOption ppo : purchaseOptions.getAllOptions()) {
      int numUnits = 0;
      for (final Territory t : purchaseTerritories.keySet()) {
        for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
          for (final Unit u : ppt.getPlaceUnits()) {
            if (u.getType().equals(ppo.getUnitType()) && !unplacedUnits.contains(u)) {
              numUnits++;
            }
          }
        }
      }
      if (numUnits > 0) {
        final int numProductionRule = numUnits / ppo.getQuantity();
        purchaseMap.put(ppo.getProductionRule(), numProductionRule);
        ProLogger.info(numProductionRule + " " + ppo.getProductionRule());
      }
    }
    return purchaseMap;
  }

  private void placeDefenders(final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories,
      final List<ProPlaceTerritory> needToDefendTerritories, final IAbstractPlaceDelegate placeDelegate) {

    ProLogger.info("Place defenders with units=" + player.getUnits().getUnits());

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through prioritized territories and purchase defenders
    for (final ProPlaceTerritory placeTerritory : needToDefendTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Placing defenders for " + t.getName() + ", enemyAttackers="
          + enemyAttackOptions.getMax(t).getMaxUnits() + ", amphibEnemyAttackers="
          + enemyAttackOptions.getMax(t).getMaxAmphibUnits() + ", defenders=" + placeTerritory.getDefendingUnits());

      // Check if any units can be placed
      final PlaceableUnits placeableUnits =
          placeDelegate.getPlaceableUnits(player.getUnits().getMatches(Matches.unitIsNotConstruction()), t);
      if (placeableUnits.isError()) {
        ProLogger.trace(t + " can't place units with error: " + placeableUnits.getErrorMessage());
        continue;
      }

      // Find remaining unit production
      int remainingUnitProduction = placeableUnits.getMaxUnits();
      if (remainingUnitProduction == -1) {
        remainingUnitProduction = Integer.MAX_VALUE;
      }
      ProLogger.trace(t + ", remainingUnitProduction=" + remainingUnitProduction);

      // Place defenders and check battle results
      final List<Unit> unitsThatCanBePlaced = new ArrayList<>(placeableUnits.getUnits());
      final int landPlaceCount = Math.min(remainingUnitProduction, unitsThatCanBePlaced.size());
      final List<Unit> unitsToPlace = new ArrayList<>();
      ProBattleResult finalResult = new ProBattleResult();
      for (int i = 0; i < landPlaceCount; i++) {

        // Add defender
        unitsToPlace.add(unitsThatCanBePlaced.get(i));

        // Find current battle result
        final Set<Unit> enemyAttackingUnits = new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        final List<Unit> defenders = new ArrayList<>(placeTerritory.getDefendingUnits());
        defenders.addAll(unitsToPlace);
        finalResult = calc.calculateBattleResults(t, new ArrayList<>(enemyAttackingUnits), defenders,
            enemyAttackOptions.getMax(t).getMaxBombardUnits());

        // Break if it can be held
        if ((!t.equals(ProData.myCapital) && !finalResult.isHasLandUnitRemaining() && (finalResult.getTuvSwing() <= 0))
            || (t.equals(ProData.myCapital) && (finalResult.getWinPercentage() < (100 - ProData.winPercentage))
            && (finalResult.getTuvSwing() <= 0))) {
          break;
        }
      }

      // Check to see if its worth trying to defend the territory
      if (!finalResult.isHasLandUnitRemaining()
          || (finalResult.getTuvSwing() < placeTerritory.getMinBattleResult().getTuvSwing())
          || t.equals(ProData.myCapital)) {
        ProLogger.trace(t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTuvSwing());
        doPlace(t, unitsToPlace, placeDelegate);
      } else {
        setCantHoldPlaceTerritory(placeTerritory, placeNonConstructionTerritories);
        ProLogger.trace(t + ", unable to defend with placedUnits=" + unitsToPlace + ", TUVSwing="
            + finalResult.getTuvSwing() + ", minTUVSwing=" + placeTerritory.getMinBattleResult().getTuvSwing());
      }
    }
  }

  private void placeLandUnits(final List<ProPlaceTerritory> prioritizedLandTerritories,
      final IAbstractPlaceDelegate placeDelegate, final boolean isConstruction) {

    ProLogger.info("Place land with isConstruction=" + isConstruction + ", units=" + player.getUnits().getUnits());

    Predicate<Unit> unitMatch = Matches.unitIsNotConstruction();
    if (isConstruction) {
      unitMatch = Matches.unitIsConstruction();
    }

    // Loop through prioritized territories and place land units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking land place for " + t.getName());

      // Check if any units can be placed
      final PlaceableUnits placeableUnits = placeDelegate.getPlaceableUnits(player.getUnits().getMatches(unitMatch), t);
      if (placeableUnits.isError()) {
        ProLogger.trace(t + " can't place units with error: " + placeableUnits.getErrorMessage());
        continue;
      }

      // Find remaining unit production
      int remainingUnitProduction = placeableUnits.getMaxUnits();
      if (remainingUnitProduction == -1) {
        remainingUnitProduction = Integer.MAX_VALUE;
      }
      ProLogger.trace(t + ", remainingUnitProduction=" + remainingUnitProduction);

      // Place as many units as possible
      final List<Unit> unitsThatCanBePlaced = new ArrayList<>(placeableUnits.getUnits());
      final int landPlaceCount = Math.min(remainingUnitProduction, unitsThatCanBePlaced.size());
      final List<Unit> unitsToPlace = unitsThatCanBePlaced.subList(0, landPlaceCount);
      ProLogger.trace(t + ", placedUnits=" + unitsToPlace);
      doPlace(t, unitsToPlace, placeDelegate);
    }
  }

  private static void addUnitsToPlaceTerritory(final ProPlaceTerritory placeTerritory, final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    // Add units to place territory
    for (final ProPurchaseTerritory purchaseTerritory : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {

        // If place territory is equal to the current place territory and has remaining production
        if (placeTerritory.equals(ppt) && (purchaseTerritories.get(purchaseTerritory).getRemainingUnitProduction() > 0)) {

          // Place max number of units
          final int numUnits = Math.min(purchaseTerritory.getRemainingUnitProduction(), unitsToPlace.size());
          final List<Unit> units = unitsToPlace.subList(0, numUnits);
          ppt.getPlaceUnits().addAll(units);
          units.clear();
        }
      }
    }
  }

  private static void setCantHoldPlaceTerritory(final ProPlaceTerritory placeTerritory,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    // Add units to place territory
    for (final Territory purchaseTerritory : purchaseTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : purchaseTerritories.get(purchaseTerritory).getCanPlaceTerritories()) {

        // If place territory is equal to the current place territory
        if (placeTerritory.equals(ppt)) {
          ppt.setCanHold(false);
        }
      }
    }
  }

  private static List<ProPurchaseTerritory> getPurchaseTerritories(final ProPlaceTerritory placeTerritory,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    final List<ProPurchaseTerritory> territories = new ArrayList<>();
    for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
        if (placeTerritory.equals(ppt)) {
          territories.add(t);
        }
      }
    }
    return territories;
  }

  private static void doPlace(final Territory t, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del) {
    for (final Unit unit : toPlace) {
      final String message =
          del.placeUnits(new ArrayList<>(Collections.singletonList(unit)), t, IAbstractPlaceDelegate.BidMode.NOT_BID);
      if (message != null) {
        ProLogger.warn(message);
        ProLogger.warn("Attempt was at: " + t + " with: " + unit);
      }
    }
    ProUtils.pause();
  }
}
