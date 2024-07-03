package games.strategy.triplea.ai.pro;

import static games.strategy.triplea.ai.pro.util.ProUtils.summarizeUnits;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.data.ProOtherMoveOptions;
import games.strategy.triplea.ai.pro.data.ProPlaceTerritory;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseOptionMap;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.pro.data.ProResourceTracker;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritoryManager;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProPurchaseUtils;
import games.strategy.triplea.ai.pro.util.ProPurchaseValidationUtils;
import games.strategy.triplea.ai.pro.util.ProTerritoryValueUtils;
import games.strategy.triplea.ai.pro.util.ProTransportUtils;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.util.TuvUtils;
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
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Pro purchase AI. */
class ProPurchaseAi {

  private final ProOddsCalculator calc;
  private final ProData proData;
  private GameData data;
  private GameState startOfTurnData; // Used to count current units on map for maxBuiltPerPlayer
  private GamePlayer player;
  private ProResourceTracker resourceTracker;
  private ProTerritoryManager territoryManager;
  private boolean isBid = false;

  ProPurchaseAi(final AbstractProAi ai) {
    this.calc = ai.getCalc();
    this.proData = ai.getProData();
  }

  void repair(
      final int initialPusRemaining,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {
    int pusRemaining = initialPusRemaining;
    ProLogger.info("Repairing factories with PUsRemaining=" + pusRemaining);

    // Current data at the start of combat move
    this.data = data;
    this.player = player;
    final Predicate<Unit> ourFactories =
        Matches.unitIsOwnedBy(player)
            .and(Matches.unitCanProduceUnits())
            .and(Matches.unitIsInfrastructure());
    final List<Territory> rfactories =
        CollectionUtils.getMatches(
            data.getMap().getTerritories(),
            ProMatches.territoryHasFactoryAndIsNotConqueredOwnedLand(player));
    if (player.getRepairFrontier() != null
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data.getProperties())) {
      ProLogger.debug("Factories can be damaged");
      final Map<Unit, Territory> unitsThatCanProduceNeedingRepair = new HashMap<>();
      for (final Territory fixTerr : rfactories) {
        if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(
                player, Matches.unitCanProduceUnitsAndCanBeDamaged())
            .test(fixTerr)) {
          continue;
        }
        final Unit possibleFactoryNeedingRepair =
            UnitUtils.getBiggestProducer(
                CollectionUtils.getMatches(fixTerr.getUnits(), ourFactories),
                fixTerr,
                player,
                false);
        if (Matches.unitHasTakenSomeBombingUnitDamage().test(possibleFactoryNeedingRepair)) {
          unitsThatCanProduceNeedingRepair.put(possibleFactoryNeedingRepair, fixTerr);
        }
      }
      ProLogger.debug("Factories that need repaired: " + unitsThatCanProduceNeedingRepair);
      for (final var repairRule : player.getRepairFrontier().getRules()) {
        for (final Unit fixUnit : unitsThatCanProduceNeedingRepair.keySet()) {
          if (fixUnit == null || !fixUnit.getType().equals(repairRule.getAnyResultKey())) {
            continue;
          }
          if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(
                  player, Matches.unitCanProduceUnitsAndCanBeDamaged())
              .test(unitsThatCanProduceNeedingRepair.get(fixUnit))) {
            continue;
          }
          final int diff = fixUnit.getUnitDamage();
          if (diff > 0) {
            final IntegerMap<RepairRule> repairMap = new IntegerMap<>();
            repairMap.add(repairRule, diff);
            final Map<Unit, IntegerMap<RepairRule>> repair = new HashMap<>();
            repair.put(fixUnit, repairMap);
            pusRemaining -= diff;
            ProLogger.debug(
                "Repairing factory=" + fixUnit + ", damage=" + diff + ", repairRule=" + repairRule);
            purchaseDelegate.purchaseRepair(repair);
          }
        }
      }
    }
  }

  /**
   * Default settings for bidding: 1) Limit one bid unit in a territory or sea zone (until set in
   * all territories then 2, etc). 2) The nation placing a unit in a territory or sea zone must have
   * started with a unit in said territory or sea zone prior to placing the bid.
   */
  Map<Territory, ProPurchaseTerritory> bid(
      final int pus, final IPurchaseDelegate purchaseDelegate, final GameState startOfTurnData) {
    // Current data fields
    data = proData.getData();
    this.startOfTurnData = startOfTurnData;
    player = proData.getPlayer();
    resourceTracker = new ProResourceTracker(pus, data);
    territoryManager = new ProTerritoryManager(calc, proData);
    isBid = true;
    final ProPurchaseOptionMap purchaseOptions = proData.getPurchaseOptions();

    ProLogger.info("Starting bid phase with resources: " + resourceTracker);
    if (!player.getUnits().isEmpty()) {
      ProLogger.info("Starting bid phase with unplaced units=" + player.getUnits());
    }

    // Find all purchase/place territories
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories =
        ProPurchaseUtils.findBidTerritories(proData, player);

    int previousNumUnits = 0;
    while (true) {
      // Determine max enemy attack units and current allied defenders
      territoryManager.populateEnemyAttackOptions(
          new ArrayList<>(), new ArrayList<>(purchaseTerritories.keySet()));
      findDefendersInPlaceTerritories(purchaseTerritories);

      // Prioritize land territories that need defended and purchase additional defenders
      final List<ProPlaceTerritory> needToDefendLandTerritories =
          prioritizeTerritoriesToDefend(purchaseTerritories, true);
      purchaseDefenders(
          purchaseTerritories,
          needToDefendLandTerritories,
          purchaseOptions.getLandFodderOptions(),
          purchaseOptions.getLandZeroMoveOptions(),
          purchaseOptions.getAirOptions(),
          true);

      // Find strategic value for each territory
      ProLogger.info("Find strategic value for place territories");
      final Set<Territory> territoriesToCheck = new HashSet<>();
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
          territoriesToCheck.add(ppt.getTerritory());
        }
      }
      final Map<Territory, Double> territoryValueMap =
          ProTerritoryValueUtils.findTerritoryValues(
              proData, player, List.of(), List.of(), territoriesToCheck);
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
          ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
          ProLogger.debug(
              ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
        }
      }

      // Prioritize land place options purchase AA then land units
      final List<ProPlaceTerritory> prioritizedLandTerritories =
          prioritizeLandTerritories(purchaseTerritories);
      purchaseAaUnits(
          purchaseTerritories, prioritizedLandTerritories, purchaseOptions.getAaOptions());
      purchaseLandUnits(purchaseTerritories, prioritizedLandTerritories, purchaseOptions);

      // Prioritize sea territories that need defended and purchase additional defenders
      final List<ProPlaceTerritory> needToDefendSeaTerritories =
          prioritizeTerritoriesToDefend(purchaseTerritories, false);
      purchaseDefenders(
          purchaseTerritories,
          needToDefendSeaTerritories,
          purchaseOptions.getSeaDefenseOptions(),
          List.of(),
          purchaseOptions.getAirOptions(),
          false);

      // Prioritize sea place options and purchase units
      final List<ProPlaceTerritory> prioritizedSeaTerritories =
          prioritizeSeaTerritories(purchaseTerritories);
      purchaseSeaAndAmphibUnits(purchaseTerritories, prioritizedSeaTerritories, purchaseOptions);

      // Try to use any remaining PUs on high value units
      purchaseUnitsWithRemainingProduction(
          purchaseTerritories, purchaseOptions.getLandOptions(), purchaseOptions.getAirOptions());
      upgradeUnitsWithRemainingPUs(purchaseTerritories, purchaseOptions);

      // Check if no remaining PUs or no unit built this iteration
      final int numUnits =
          purchaseTerritories.values().stream()
              .map(ProPurchaseTerritory::getCanPlaceTerritories)
              .map(t -> t.get(0))
              .map(ProPlaceTerritory::getPlaceUnits)
              .mapToInt(List::size)
              .sum();
      if (resourceTracker.isEmpty() || numUnits == previousNumUnits) {
        break;
      }
      previousNumUnits = numUnits;
      ProPurchaseUtils.incrementUnitProductionForBidTerritories(purchaseTerritories);
    }

    // Determine final count of each production rule
    final IntegerMap<ProductionRule> purchaseMap =
        populateProductionRuleMap(purchaseTerritories, purchaseOptions);

    // Purchase units
    final String error = purchaseDelegate.purchase(purchaseMap);
    if (error != null) {
      ProLogger.warn("Purchase error: " + error);
    }

    territoryManager = null;
    return purchaseTerritories;
  }

  Map<Territory, ProPurchaseTerritory> purchase(
      final IPurchaseDelegate purchaseDelegate, final GameState startOfTurnData) {
    // Current data fields
    data = proData.getData();
    this.startOfTurnData = startOfTurnData;
    player = proData.getPlayer();
    resourceTracker = new ProResourceTracker(player);
    territoryManager = new ProTerritoryManager(calc, proData);
    isBid = false;
    final ProPurchaseOptionMap purchaseOptions = proData.getPurchaseOptions();

    ProLogger.info("Starting purchase phase with resources: " + resourceTracker);
    if (!player.getUnits().isEmpty()) {
      ProLogger.info("Starting purchase phase with unplaced units=" + player.getUnits());
    }

    // Find all purchase/place territories
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories =
        ProPurchaseUtils.findPurchaseTerritories(proData, player);
    final Set<Territory> placeTerritories =
        new HashSet<>(
            CollectionUtils.getMatches(
                data.getMap().getTerritoriesOwnedBy(player), Matches.territoryIsLand()));
    for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
        placeTerritories.add(ppt.getTerritory());
      }
    }

    // Determine max enemy attack units and current allied defenders
    territoryManager.populateEnemyAttackOptions(List.of(), placeTerritories);
    findDefendersInPlaceTerritories(purchaseTerritories);

    // Prioritize land territories that need defended and purchase additional defenders
    final List<ProPlaceTerritory> needToDefendLandTerritories =
        prioritizeTerritoriesToDefend(purchaseTerritories, true);
    purchaseDefenders(
        purchaseTerritories,
        needToDefendLandTerritories,
        purchaseOptions.getLandFodderOptions(),
        purchaseOptions.getLandZeroMoveOptions(),
        purchaseOptions.getAirOptions(),
        true);

    // Find strategic value for each territory
    ProLogger.info("Find strategic value for place territories");
    final Set<Territory> territoriesToCheck = new HashSet<>();
    for (final Territory t : purchaseTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
        territoriesToCheck.add(ppt.getTerritory());
      }
    }
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(
            proData, player, List.of(), List.of(), territoriesToCheck);
    for (final Territory t : purchaseTerritories.keySet()) {
      for (final ProPlaceTerritory ppt : purchaseTerritories.get(t).getCanPlaceTerritories()) {
        ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
        ProLogger.debug(
            ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
      }
    }

    // Prioritize land place options purchase AA then land units
    final List<ProPlaceTerritory> prioritizedLandTerritories =
        prioritizeLandTerritories(purchaseTerritories);
    purchaseAaUnits(
        purchaseTerritories, prioritizedLandTerritories, purchaseOptions.getAaOptions());
    purchaseLandUnits(purchaseTerritories, prioritizedLandTerritories, purchaseOptions);

    // Prioritize sea territories that need defended and purchase additional defenders
    final List<ProPlaceTerritory> needToDefendSeaTerritories =
        prioritizeTerritoriesToDefend(purchaseTerritories, false);
    purchaseDefenders(
        purchaseTerritories,
        needToDefendSeaTerritories,
        purchaseOptions.getSeaDefenseOptions(),
        List.of(),
        purchaseOptions.getAirOptions(),
        false);

    // Determine whether to purchase new land factory
    final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories = new HashMap<>();
    purchaseFactory(
        factoryPurchaseTerritories,
        purchaseTerritories,
        prioritizedLandTerritories,
        purchaseOptions,
        false);

    // Prioritize sea place options and purchase units
    final List<ProPlaceTerritory> prioritizedSeaTerritories =
        prioritizeSeaTerritories(purchaseTerritories);
    final boolean shouldSaveUpForAFleet =
        purchaseSeaAndAmphibUnits(purchaseTerritories, prioritizedSeaTerritories, purchaseOptions);

    // Try to use any remaining PUs on high value units, except if we need to save up for a fleet.
    if (!shouldSaveUpForAFleet) {
      purchaseUnitsWithRemainingProduction(
          purchaseTerritories, purchaseOptions.getLandOptions(), purchaseOptions.getAirOptions());

      upgradeUnitsWithRemainingPUs(purchaseTerritories, purchaseOptions);

      // Try to purchase land/sea factory with extra PUs
      purchaseFactory(
          factoryPurchaseTerritories,
          purchaseTerritories,
          prioritizedLandTerritories,
          purchaseOptions,
          true);
    }

    // Add factory purchase territory to list
    purchaseTerritories.putAll(factoryPurchaseTerritories);

    // Determine final count of each production rule
    final IntegerMap<ProductionRule> purchaseMap =
        populateProductionRuleMap(purchaseTerritories, purchaseOptions);

    // Purchase units
    final String error = purchaseDelegate.purchase(purchaseMap);
    if (error != null) {
      ProLogger.warn("Purchase error: " + error);
    }

    territoryManager = null;
    return purchaseTerritories;
  }

  private boolean shouldSaveUpForAFleet(
      final ProPurchaseOptionMap purchaseOptions,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    if (resourceTracker.isEmpty()
        || purchaseOptions.getSeaDefenseOptions().isEmpty()
        || purchaseOptions.getSeaTransportOptions().isEmpty()) {
      return false;
    }
    Optional<Territory> enemyTerritoryReachableByLand =
        territoryManager.findClosestTerritory(
            purchaseTerritories.keySet(),
            ProMatches.territoryCanPotentiallyMoveLandUnits(player),
            Matches.isTerritoryEnemy(player).and(Matches.territoryIsLand()));
    if (enemyTerritoryReachableByLand.isPresent()) {
      // An enemy territory is reachable by land, no need to save for a fleet.
      return false;
    }
    // See if we can reach the enemy by sea from a sea placement territory.
    var placeSeaTerritories = new HashSet<Territory>();
    int maxSeaUnitsThatCanBePlaced = 0;
    for (ProPurchaseTerritory purchaseTerritory : purchaseTerritories.values()) {
      boolean canProduceSeaUnits = false;
      for (ProPlaceTerritory placeTerritory : purchaseTerritory.getCanPlaceTerritories()) {
        if (placeTerritory.getTerritory().isWater()) {
          placeSeaTerritories.add(placeTerritory.getTerritory());
          canProduceSeaUnits = true;
        }
      }
      if (canProduceSeaUnits) {
        maxSeaUnitsThatCanBePlaced += purchaseTerritory.getUnitProduction();
      }
    }
    Optional<Territory> enemyLandReachableBySea =
        territoryManager.findClosestTerritory(
            placeSeaTerritories,
            ProMatches.territoryCanMoveSeaUnits(player, true),
            Matches.isTerritoryEnemy(player).and(Matches.territoryIsLand()));
    if (enemyLandReachableBySea.isEmpty()) {
      return false;
    }
    // Don't save up more if we already have enough PUs to buy the biggest fleet we can.
    IntegerMap<Resource> maxShipCost = new IntegerMap<>();
    Resource pus = player.getData().getResourceList().getResource(Constants.PUS);
    for (ProPurchaseOption option : purchaseOptions.getSeaDefenseOptions()) {
      if (option.getCost() > maxShipCost.getInt(pus)) {
        maxShipCost = option.getCosts();
      }
    }
    maxShipCost.multiplyAllValuesBy(maxSeaUnitsThatCanBePlaced);
    if (resourceTracker.hasEnough(maxShipCost)) {
      return false;
    }
    ProLogger.info("Saving up for a fleet, since enemy territories are only reachable by sea");
    return true;
  }

  void place(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final IAbstractPlaceDelegate placeDelegate) {
    ProLogger.info("Starting place phase");

    data = proData.getData();
    player = proData.getPlayer();
    territoryManager = new ProTerritoryManager(calc, proData);

    // Clear list of units to be consumed, since we only used for movement phase.
    // Additionally, we omit units in this list when checking for eligible units to consume, so
    // we need to clear it before we actually do placement.
    proData.getUnitsToBeConsumed().clear();

    if (purchaseTerritories != null) {
      // Place all units calculated during purchase phase (land then sea to reduce failed
      // placements)
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
          if (!ppt.getTerritory().isWater()) {
            final List<Unit> unitsToPlace = new ArrayList<>();
            for (final Unit placeUnit : ppt.getPlaceUnits()) {
              for (final Unit myUnit : player.getUnitCollection()) {
                if (myUnit.getType().equals(placeUnit.getType())
                    && !unitsToPlace.contains(myUnit)) {
                  unitsToPlace.add(myUnit);
                  break;
                }
              }
            }
            doPlace(
                data.getMap().getTerritory(ppt.getTerritory().getName()),
                unitsToPlace,
                placeDelegate);
            ProLogger.debug(ppt.getTerritory() + " placed units: " + unitsToPlace);
          }
        }
      }
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
          if (ppt.getTerritory().isWater()) {
            final List<Unit> unitsToPlace = new ArrayList<>();
            for (final Unit placeUnit : ppt.getPlaceUnits()) {
              for (final Unit myUnit : player.getUnitCollection()) {
                if (myUnit.getType().equals(placeUnit.getType())
                    && !unitsToPlace.contains(myUnit)) {
                  unitsToPlace.add(myUnit);
                  break;
                }
              }
            }
            doPlace(
                data.getMap().getTerritory(ppt.getTerritory().getName()),
                unitsToPlace,
                placeDelegate);
            ProLogger.debug(ppt.getTerritory() + " placed units: " + unitsToPlace);
          }
        }
      }
    }

    // Place remaining units (currently only implemented to handle land units, ex. WW2v3 China)
    if (player.getUnits().isEmpty()) {
      return;
    }

    // Current data at the start of place
    ProLogger.debug("Remaining units to place: " + player.getUnits());

    // Find all place territories
    final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories =
        ProPurchaseUtils.findPurchaseTerritories(proData, player);
    final Set<Territory> placeTerritories = new HashSet<>();
    for (final Territory t : placeNonConstructionTerritories.keySet()) {
      for (final ProPlaceTerritory ppt :
          placeNonConstructionTerritories.get(t).getCanPlaceTerritories()) {
        placeTerritories.add(ppt.getTerritory());
      }
    }

    // Determine max enemy attack units and current allied defenders
    territoryManager.populateEnemyAttackOptions(
        new ArrayList<>(), new ArrayList<>(placeTerritories));
    findDefendersInPlaceTerritories(placeNonConstructionTerritories);

    // Prioritize land territories that need defended and place additional defenders
    final List<ProPlaceTerritory> needToDefendLandTerritories =
        prioritizeTerritoriesToDefend(placeNonConstructionTerritories, true);
    placeDefenders(placeNonConstructionTerritories, needToDefendLandTerritories, placeDelegate);

    // Prioritize sea territories that need defended and place additional defenders
    final List<ProPlaceTerritory> needToDefendSeaTerritories =
        prioritizeTerritoriesToDefend(placeNonConstructionTerritories, false);
    placeDefenders(placeNonConstructionTerritories, needToDefendSeaTerritories, placeDelegate);

    // Find strategic value for each territory
    ProLogger.info("Find strategic value for place territories");
    final Set<Territory> territoriesToCheck = new HashSet<>();
    for (final ProPurchaseTerritory t : placeNonConstructionTerritories.values()) {
      for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
        territoriesToCheck.add(ppt.getTerritory());
      }
    }
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(
            proData, player, List.of(), List.of(), territoriesToCheck);
    for (final ProPurchaseTerritory t : placeNonConstructionTerritories.values()) {
      for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
        ppt.setStrategicValue(territoryValueMap.get(ppt.getTerritory()));
        ProLogger.debug(
            ppt.getTerritory() + ", strategicValue=" + territoryValueMap.get(ppt.getTerritory()));
      }
    }

    // Prioritize place territories
    final List<ProPlaceTerritory> prioritizedTerritories =
        prioritizeLandTerritories(placeNonConstructionTerritories);
    for (final ProPurchaseTerritory ppt : placeNonConstructionTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        if (!prioritizedTerritories.contains(placeTerritory)) {
          prioritizedTerritories.add(placeTerritory);
        }
      }
    }

    // Place regular then isConstruction units (placeDelegate.getPlaceableUnits doesn't handle
    // combined)
    placeUnits(prioritizedTerritories, placeDelegate, Matches.unitIsNotConstruction());
    placeUnits(prioritizedTerritories, placeDelegate, Matches.unitIsConstruction());

    territoryManager = null;
  }

  private void findDefendersInPlaceTerritories(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    ProLogger.info("Find defenders in possible place territories");
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        final List<Unit> units = t.getMatches(Matches.isUnitAllied(player));
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
        if (enemyAttackOptions.getMax(t) == null
            || (t.isWater() && placeTerritory.getDefendingUnits().isEmpty())
            || (isLand && t.isWater())
            || (!isLand && !t.isWater())) {
          continue;
        }

        // Find current battle result
        final Collection<Unit> enemyAttackingUnits =
            new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        final ProBattleResult result =
            calc.calculateBattleResults(
                proData,
                t,
                enemyAttackingUnits,
                placeTerritory.getDefendingUnits(),
                enemyAttackOptions.getMax(t).getMaxBombardUnits());
        placeTerritory.setMinBattleResult(result);
        double holdValue = 0;
        if (t.isWater()) {
          final double unitValue =
              TuvUtils.getTuv(
                  CollectionUtils.getMatches(
                      placeTerritory.getDefendingUnits(), Matches.unitIsOwnedBy(player)),
                  proData.getUnitValueMap());
          holdValue = unitValue / 8;
        }
        ProLogger.trace(
            t.getName()
                + " TUVSwing="
                + result.getTuvSwing()
                + ", win%="
                + result.getWinPercentage()
                + ", hasLandUnitRemaining="
                + result.isHasLandUnitRemaining()
                + ", holdValue="
                + holdValue
                + ", enemyAttackers="
                + summarizeUnits(enemyAttackingUnits)
                + ", defenders="
                + summarizeUnits(placeTerritory.getDefendingUnits()));

        // If it can't currently be held then add to list
        final boolean isLandAndCanOnlyBeAttackedByAir =
            !t.isWater()
                && !enemyAttackingUnits.isEmpty()
                && enemyAttackingUnits.stream().allMatch(Matches.unitIsAir());
        if ((!t.isWater() && result.isHasLandUnitRemaining())
            || result.getTuvSwing() > holdValue
            || (t.equals(proData.getMyCapital())
                && !isLandAndCanOnlyBeAttackedByAir
                && result.getWinPercentage() > (100 - proData.getWinPercentage()))) {
          needToDefendTerritories.add(placeTerritory);
        }
      }
    }

    // Calculate value of defending territory
    for (final ProPlaceTerritory placeTerritory : needToDefendTerritories) {
      final Territory t = placeTerritory.getTerritory();

      // Determine if it is my capital or adjacent to my capital
      int isMyCapital = 0;
      if (t.equals(proData.getMyCapital())) {
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
      double defendingUnitValue =
          TuvUtils.getTuv(placeTerritory.getDefendingUnits(), proData.getUnitValueMap());
      if (t.isWater()
          && placeTerritory.getDefendingUnits().stream().noneMatch(Matches.unitIsOwnedBy(player))) {
        defendingUnitValue = 0;
      }

      // Calculate defense value for prioritization
      final double territoryValue =
          (2.0 * production + 4.0 * isFactory + 0.5 * defendingUnitValue)
              * (1 + isFactory)
              * (1 + 10.0 * isMyCapital);
      placeTerritory.setDefenseValue(territoryValue);
    }

    // Remove any territories with negative defense value
    needToDefendTerritories.removeIf(ppt -> ppt.getDefenseValue() <= 0);

    // Sort territories by value
    final List<ProPlaceTerritory> sortedTerritories = new ArrayList<>(needToDefendTerritories);
    sortedTerritories.sort(
        Comparator.comparingDouble(ProPlaceTerritory::getDefenseValue).reversed());
    for (final ProPlaceTerritory placeTerritory : sortedTerritories) {
      ProLogger.debug(
          placeTerritory.toString() + " defenseValue=" + placeTerritory.getDefenseValue());
    }
    return sortedTerritories;
  }

  private void purchaseDefenders(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> needToDefendTerritories,
      final List<ProPurchaseOption> defensePurchaseOptions,
      final List<ProPurchaseOption> zeroMoveDefensePurchaseOptions,
      final List<ProPurchaseOption> airPurchaseOptions,
      final boolean isLand) {
    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase defenders with resources: " + resourceTracker + ", isLand=" + isLand);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through prioritized territories and purchase defenders
    for (final ProPlaceTerritory placeTerritory : needToDefendTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug(
          "Purchasing defenders for "
              + t.getName()
              + ", enemyAttackers="
              + summarizeUnits(enemyAttackOptions.getMax(t).getMaxUnits())
              + ", amphibEnemyAttackers="
              + summarizeUnits(enemyAttackOptions.getMax(t).getMaxAmphibUnits())
              + ", defenders="
              + summarizeUnits(placeTerritory.getDefendingUnits()));

      // Find local owned units
      final List<Unit> ownedLocalUnits = t.getMatches(Matches.unitIsOwnedBy(player));
      int unusedCarrierCapacity =
          Math.min(0, ProTransportUtils.getUnusedCarrierCapacity(player, t, new ArrayList<>()));
      int unusedLocalCarrierCapacity =
          ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, new ArrayList<>());
      ProLogger.trace(
          t
              + ", unusedCarrierCapacity="
              + unusedCarrierCapacity
              + ", unusedLocalCarrierCapacity="
              + unusedLocalCarrierCapacity);

      // Determine if need destroyer
      boolean needDestroyer =
          enemyAttackOptions.getMax(t).getMaxUnits().stream()
                  .anyMatch(Matches.unitHasSubBattleAbilities())
              && ownedLocalUnits.stream().noneMatch(Matches.unitIsDestroyer());

      // Find all purchase territories for place territory
      final List<Unit> unitsToPlace = new ArrayList<>();
      ProBattleResult finalResult = new ProBattleResult();
      final List<ProPurchaseTerritory> selectedPurchaseTerritories =
          getPurchaseTerritories(placeTerritory, purchaseTerritories);
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {
        // Check remaining production
        int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
        int remainingConstructions =
            ProPurchaseUtils.getMaxConstructions(zeroMoveDefensePurchaseOptions);
        ProLogger.debug(
            purchaseTerritory.getTerritory()
                + ", remainingUnitProduction="
                + remainingUnitProduction
                + ", remainingConstructions="
                + remainingConstructions);
        if (remainingUnitProduction <= 0 && remainingConstructions <= 0) {
          continue;
        }

        // Find defenders that can be produced in this territory
        final List<ProPurchaseOption> allDefensePurchaseOptions =
            new ArrayList<>(defensePurchaseOptions);
        allDefensePurchaseOptions.addAll(zeroMoveDefensePurchaseOptions);
        final List<ProPurchaseOption> purchaseOptionsForTerritory =
            ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
                proData,
                player,
                allDefensePurchaseOptions,
                t,
                purchaseTerritory.getTerritory(),
                isBid);
        purchaseOptionsForTerritory.addAll(airPurchaseOptions);

        // Purchase necessary defenders
        while (true) {
          // Select purchase option
          ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
              proData,
              player,
              startOfTurnData,
              purchaseOptionsForTerritory,
              resourceTracker,
              remainingUnitProduction,
              unitsToPlace,
              purchaseTerritories,
              remainingConstructions,
              t);
          final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
            if (isLand) {
              defenseEfficiencies.put(
                  ppo, ppo.getDefenseEfficiency(1, data, ownedLocalUnits, unitsToPlace));
            } else {
              defenseEfficiencies.put(
                  ppo,
                  ppo.getSeaDefenseEfficiency(
                      data,
                      ownedLocalUnits,
                      unitsToPlace,
                      needDestroyer,
                      unusedCarrierCapacity,
                      unusedLocalCarrierCapacity));
            }
          }
          final Optional<ProPurchaseOption> optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Defense");
          if (optionalSelectedOption.isEmpty()) {
            break;
          }
          final ProPurchaseOption selectedOption = optionalSelectedOption.get();
          if (selectedOption.isDestroyer()) {
            needDestroyer = false;
          }

          // Create new temp units
          resourceTracker.tempPurchase(selectedOption);
          if (selectedOption.isConstruction()) {
            remainingConstructions -= selectedOption.getQuantity();
          } else {
            remainingUnitProduction -= selectedOption.getQuantity();
          }
          unitsToPlace.addAll(selectedOption.createTempUnits());
          if (selectedOption.isCarrier() || selectedOption.isAir()) {
            unusedCarrierCapacity =
                ProTransportUtils.getUnusedCarrierCapacity(player, t, unitsToPlace);
            unusedLocalCarrierCapacity =
                ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, unitsToPlace);
          }
          ProLogger.trace(
              "Selected unit="
                  + selectedOption.getUnitType().getName()
                  + ", unusedCarrierCapacity="
                  + unusedCarrierCapacity
                  + ", unusedLocalCarrierCapacity="
                  + unusedLocalCarrierCapacity);

          // Find current battle result
          final Set<Unit> enemyAttackingUnits =
              new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
          enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
          final List<Unit> defenders = new ArrayList<>(placeTerritory.getDefendingUnits());
          defenders.addAll(unitsToPlace);
          finalResult =
              calc.calculateBattleResults(
                  proData,
                  t,
                  enemyAttackingUnits,
                  defenders,
                  enemyAttackOptions.getMax(t).getMaxBombardUnits());

          // Break if it can be held
          if ((!t.equals(proData.getMyCapital())
                  && !finalResult.isHasLandUnitRemaining()
                  && finalResult.getTuvSwing() <= 0)
              || (t.equals(proData.getMyCapital())
                  && finalResult.getWinPercentage() < (100 - proData.getWinPercentage())
                  && finalResult.getTuvSwing() <= 0)) {
            break;
          }
        }
      }

      // Check to see if its worth trying to defend the territory
      final boolean hasLocalSuperiority =
          ProBattleUtils.territoryHasLocalLandSuperiority(
              proData, t, ProBattleUtils.SHORT_RANGE, player, purchaseTerritories);
      if (!finalResult.isHasLandUnitRemaining()
          || (finalResult.getTuvSwing() - resourceTracker.getTempPUs(data) / 2f)
              < placeTerritory.getMinBattleResult().getTuvSwing()
          || t.equals(proData.getMyCapital())
          || (!t.isWater() && hasLocalSuperiority)) {
        resourceTracker.confirmTempPurchases();
        ProLogger.trace(
            t
                + ", placedUnits="
                + unitsToPlace
                + ", TUVSwing="
                + finalResult.getTuvSwing()
                + ", hasLandUnitRemaining="
                + finalResult.isHasLandUnitRemaining()
                + ", hasLocalSuperiority="
                + hasLocalSuperiority);
        addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
      } else {
        resourceTracker.clearTempPurchases();
        setCantHoldPlaceTerritory(placeTerritory, purchaseTerritories);
        ProLogger.trace(
            t
                + ", unable to defend with placedUnits="
                + unitsToPlace
                + ", TUVSwing="
                + finalResult.getTuvSwing()
                + ", minTUVSwing="
                + placeTerritory.getMinBattleResult().getTuvSwing());
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
        if (!t.isWater() && placeTerritory.getStrategicValue() >= 1 && placeTerritory.isCanHold()) {
          final boolean hasEnemyNeighbors =
              !data.getMap().getNeighbors(t, ProMatches.territoryIsEnemyLand(player)).isEmpty();
          final Set<Territory> nearbyLandTerritories =
              data.getMap()
                  .getNeighbors(t, 9, ProMatches.territoryCanPotentiallyMoveLandUnits(player));
          final int numNearbyEnemyTerritories =
              CollectionUtils.countMatches(
                  nearbyLandTerritories,
                  Matches.isTerritoryOwnedByAnyOf(ProUtils.getPotentialEnemyPlayers(player)));
          final boolean hasLocalLandSuperiority =
              ProBattleUtils.territoryHasLocalLandSuperiority(
                  proData, t, ProBattleUtils.SHORT_RANGE, player);
          if (hasEnemyNeighbors || numNearbyEnemyTerritories >= 3 || !hasLocalLandSuperiority) {
            prioritizedLandTerritories.add(placeTerritory);
          }
        }
      }
    }

    // Sort territories by value
    prioritizedLandTerritories.sort(
        Comparator.comparingDouble(ProPlaceTerritory::getStrategicValue).reversed());
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      ProLogger.debug(
          placeTerritory.toString() + " strategicValue=" + placeTerritory.getStrategicValue());
    }
    return prioritizedLandTerritories;
  }

  private void purchaseAaUnits(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedLandTerritories,
      final List<ProPurchaseOption> specialPurchaseOptions) {

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
          enemyAttackOptions.getMax(t).getMaxUnits().stream()
              .anyMatch(Matches.unitIsStrategicBomber());
      final boolean territoryCanBeBombed =
          t.anyUnitsMatch(Matches.unitCanProduceUnitsAndCanBeDamaged());
      final boolean hasAaBombingDefense = t.anyUnitsMatch(Matches.unitIsAaForBombingThisUnitOnly());
      ProLogger.debug(
          t
              + ", enemyCanBomb="
              + enemyCanBomb
              + ", territoryCanBeBombed="
              + territoryCanBeBombed
              + ", hasAABombingDefense="
              + hasAaBombingDefense);
      if (!enemyCanBomb || !territoryCanBeBombed || hasAaBombingDefense) {
        continue;
      }

      // Remove options that cost too much PUs or production
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, specialPurchaseOptions, t, isBid);
      ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
          proData,
          player,
          startOfTurnData,
          purchaseOptionsForTerritory,
          resourceTracker,
          remainingUnitProduction,
          List.of(),
          purchaseTerritories,
          0,
          t);
      if (purchaseOptionsForTerritory.isEmpty()) {
        continue;
      }

      // Determine most cost efficient units that can be produced in this territory
      ProPurchaseOption bestAaOption = null;
      int minCost = Integer.MAX_VALUE;
      for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
        final boolean isAaForBombing =
            Matches.unitTypeIsAaForBombingThisUnitOnly().test(ppo.getUnitType());
        if (isAaForBombing
            && ppo.getCost() < minCost
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
      addUnitsToPlace(placeTerritory, bestAaOption.createTempUnits());
    }
  }

  private void purchaseLandUnits(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedLandTerritories,
      final ProPurchaseOptionMap purchaseOptions) {

    final List<Unit> unplacedUnits = player.getMatches(Matches.unitIsNotSea());
    if (resourceTracker.isEmpty() && unplacedUnits.isEmpty()) {
      return;
    }
    ProLogger.info("Purchase land units with resources: " + resourceTracker);
    if (!unplacedUnits.isEmpty()) {
      ProLogger.info("Purchase land units with unplaced units=" + unplacedUnits);
    }

    // Loop through prioritized territories and purchase land units
    final Set<Territory> territoriesToCheck = new HashSet<>();
    final Predicate<Territory> canMoveLandUnits =
        ProMatches.territoryCanPotentiallyMoveLandUnits(player);
    final Predicate<Territory> isEnemyTerritory =
        Matches.isTerritoryOwnedByAnyOf(ProUtils.getEnemyPlayers(player));
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Set<Territory> landTerritories =
          data.getMap().getNeighbors(placeTerritory.getTerritory(), 9, canMoveLandUnits);
      territoriesToCheck.addAll(CollectionUtils.getMatches(landTerritories, isEnemyTerritory));
    }
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(
            proData, player, List.of(), List.of(), territoriesToCheck);
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
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, purchaseOptions.getLandFodderOptions(), t, isBid);
      final List<ProPurchaseOption> landAttackOptions =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, purchaseOptions.getLandAttackOptions(), t, isBid);
      final List<ProPurchaseOption> landDefenseOptions =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, purchaseOptions.getLandDefenseOptions(), t, isBid);

      // Determine enemy distance and locally owned units
      int enemyDistance =
          ProUtils.getClosestEnemyOrNeutralLandTerritoryDistance(
              data, player, t, territoryValueMap);
      if (enemyDistance <= 0) {
        enemyDistance = 10;
      }
      final int fodderPercent = 80 - enemyDistance * 5;
      ProLogger.debug(t + ", enemyDistance=" + enemyDistance + ", fodderPercent=" + fodderPercent);
      final Set<Territory> neighbors =
          data.getMap().getNeighbors(t, 2, ProMatches.territoryCanMoveLandUnits(player, false));
      neighbors.add(t);
      final List<Unit> ownedLocalUnits = new ArrayList<>();
      for (final Territory neighbor : neighbors) {
        ownedLocalUnits.addAll(neighbor.getMatches(Matches.unitIsOwnedBy(player)));
      }

      // Check for unplaced units
      final List<Unit> unitsToPlace = new ArrayList<>();
      for (final Iterator<Unit> it = unplacedUnits.iterator();
          it.hasNext() && remainingUnitProduction > 0; ) {
        final Unit u = it.next();
        unitsToPlace.add(u);
        if (ProPurchaseValidationUtils.canUnitsBePlaced(
            proData, unitsToPlace, player, t, t, isBid)) {
          remainingUnitProduction--;
          it.remove();
          ProLogger.trace("Selected unplaced unit=" + u);
        } else {
          unitsToPlace.remove(unitsToPlace.size() - 1);
        }
      }

      // Purchase as many units as possible
      int addedFodderUnits = 0;
      double attackAndDefenseDifference = 0;
      boolean selectFodderUnit = true;
      while (true) {
        // Remove options that cost too much PUs or production
        ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
            proData,
            player,
            startOfTurnData,
            landFodderOptions,
            resourceTracker,
            remainingUnitProduction,
            unitsToPlace,
            purchaseTerritories,
            0,
            t);
        ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
            proData,
            player,
            startOfTurnData,
            landAttackOptions,
            resourceTracker,
            remainingUnitProduction,
            unitsToPlace,
            purchaseTerritories,
            0,
            t);
        ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
            proData,
            player,
            startOfTurnData,
            landDefenseOptions,
            resourceTracker,
            remainingUnitProduction,
            unitsToPlace,
            purchaseTerritories,
            0,
            t);

        // Select purchase option
        Optional<ProPurchaseOption> optionalSelectedOption = Optional.empty();
        if (!selectFodderUnit && attackAndDefenseDifference > 0 && !landDefenseOptions.isEmpty()) {
          final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : landDefenseOptions) {
            defenseEfficiencies.put(
                ppo, ppo.getDefenseEfficiency(enemyDistance, data, ownedLocalUnits, unitsToPlace));
          }
          optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Land Defense");
        } else if (!selectFodderUnit && !landAttackOptions.isEmpty()) {
          final Map<ProPurchaseOption, Double> attackEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : landAttackOptions) {
            attackEfficiencies.put(
                ppo, ppo.getAttackEfficiency(enemyDistance, data, ownedLocalUnits, unitsToPlace));
          }
          optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(attackEfficiencies, "Land Attack");
        } else if (!landFodderOptions.isEmpty()) {
          final Map<ProPurchaseOption, Double> fodderEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : landFodderOptions) {
            fodderEfficiencies.put(
                ppo, ppo.getFodderEfficiency(enemyDistance, data, ownedLocalUnits, unitsToPlace));
          }
          optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(fodderEfficiencies, "Land Fodder");
          if (optionalSelectedOption.isPresent()) {
            addedFodderUnits += optionalSelectedOption.get().getQuantity();
          }
        }
        if (optionalSelectedOption.isEmpty()) {
          break;
        }
        final ProPurchaseOption selectedOption = optionalSelectedOption.get();

        // Create new temp units
        resourceTracker.purchase(selectedOption);
        remainingUnitProduction -= selectedOption.getQuantity();
        unitsToPlace.addAll(selectedOption.createTempUnits());
        attackAndDefenseDifference += (selectedOption.getAttack() - selectedOption.getDefense());
        selectFodderUnit = ((double) addedFodderUnits / unitsToPlace.size() * 100) <= fodderPercent;
        ProLogger.trace("Selected unit=" + selectedOption.getUnitType().getName());
      }

      // Add units to place territory
      addUnitsToPlace(placeTerritory, unitsToPlace);
    }
  }

  private void purchaseFactory(
      final Map<Territory, ProPurchaseTerritory> factoryPurchaseTerritories,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedLandTerritories,
      final ProPurchaseOptionMap purchaseOptions,
      final boolean hasExtraPUs) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info(
        "Purchase factory with resources: " + resourceTracker + ", hasExtraPUs=" + hasExtraPUs);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Only try to purchase a factory if all production was used in prioritized land territories
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      for (final Territory t : purchaseTerritories.keySet()) {
        if (placeTerritory.getTerritory().equals(t)
            && purchaseTerritories.get(t).getRemainingUnitProduction() > 0) {
          ProLogger.debug("Not purchasing a factory since remaining land production in " + t);
          return;
        }
      }
    }

    // Find all owned land territories that weren't conquered and don't already have a factory
    final List<Territory> possibleFactoryTerritories =
        CollectionUtils.getMatches(
            data.getMap().getTerritories(),
            ProMatches.territoryHasNoInfraFactoryAndIsNotConqueredOwnedLand(player));
    possibleFactoryTerritories.removeAll(factoryPurchaseTerritories.keySet());
    final Set<Territory> purchaseFactoryTerritories = new HashSet<>();
    final List<Territory> territoriesThatCantBeHeld = new ArrayList<>();
    for (final Territory t : possibleFactoryTerritories) {

      // Only consider territories with production of at least 3 unless there are still remaining
      // PUs
      final int production = TerritoryAttachment.get(t).getProduction();
      if ((production < 3 && !hasExtraPUs) || production < 2) {
        continue;
      }

      // Check if no enemy attackers and that it wasn't conquered this turn
      if (enemyAttackOptions.getMax(t) == null) {
        purchaseFactoryTerritories.add(t);
        ProLogger.trace("Possible factory since no enemy attackers: " + t.getName());
      } else {

        // Find current battle result
        final List<Unit> defenders = t.getMatches(Matches.isUnitAllied(player));
        final Set<Unit> enemyAttackingUnits =
            new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        final ProBattleResult result =
            calc.estimateDefendBattleResults(
                proData,
                t,
                enemyAttackingUnits,
                defenders,
                enemyAttackOptions.getMax(t).getMaxBombardUnits());

        // Check if it can't be held or if it can then that it wasn't conquered this turn
        if (result.isHasLandUnitRemaining() || result.getTuvSwing() > 0) {
          territoriesThatCantBeHeld.add(t);
          ProLogger.trace(
              "Can't hold territory: "
                  + t.getName()
                  + ", hasLandUnitRemaining="
                  + result.isHasLandUnitRemaining()
                  + ", TUVSwing="
                  + result.getTuvSwing()
                  + ", enemyAttackers="
                  + enemyAttackingUnits.size()
                  + ", myDefenders="
                  + defenders.size());
        } else {
          purchaseFactoryTerritories.add(t);
          ProLogger.trace(
              "Possible factory: "
                  + t.getName()
                  + ", hasLandUnitRemaining="
                  + result.isHasLandUnitRemaining()
                  + ", TUVSwing="
                  + result.getTuvSwing()
                  + ", enemyAttackers="
                  + enemyAttackingUnits.size()
                  + ", myDefenders="
                  + defenders.size());
        }
      }
    }
    ProLogger.debug("Possible factory territories: " + purchaseFactoryTerritories);

    // Remove any territories that don't have local land superiority
    if (!hasExtraPUs) {
      purchaseFactoryTerritories.removeIf(
          t ->
              !ProBattleUtils.territoryHasLocalLandSuperiority(
                  proData, t, ProBattleUtils.MEDIUM_RANGE, player, purchaseTerritories));
      ProLogger.debug(
          "Possible factory territories that have land superiority: " + purchaseFactoryTerritories);
    }

    // Find strategic value for each territory
    final Map<Territory, Double> territoryValueMap =
        ProTerritoryValueUtils.findTerritoryValues(
            proData, player, territoriesThatCantBeHeld, List.of(), purchaseFactoryTerritories);
    double maxValue = 0.0;
    Territory maxTerritory = null;
    for (final Territory t : purchaseFactoryTerritories) {
      final int production = TerritoryAttachment.get(t).getProduction();
      final double value = territoryValueMap.get(t) * production + 0.1 * production;
      final boolean isAdjacentToSea =
          Matches.territoryHasNeighborMatching(data.getMap(), Matches.territoryIsWater()).test(t);
      final Set<Territory> nearbyLandTerritories =
          data.getMap().getNeighbors(t, 9, ProMatches.territoryCanMoveLandUnits(player, false));
      final int numNearbyEnemyTerritories =
          CollectionUtils.countMatches(nearbyLandTerritories, Matches.isTerritoryEnemy(player));
      ProLogger.trace(
          t
              + ", strategic value="
              + territoryValueMap.get(t)
              + ", value="
              + value
              + ", numNearbyEnemyTerritories="
              + numNearbyEnemyTerritories);
      if (value > maxValue
          && ((numNearbyEnemyTerritories >= 4 && territoryValueMap.get(t) >= 1)
              || (isAdjacentToSea && hasExtraPUs))) {
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
            if (u.getType().equals(ppo.getUnitType())
                && ppo.getQuantity() == 1
                && (maxPlacedOption == null || ppo.getCost() >= maxPlacedOption.getCost())) {
              maxPlacedOption = ppo;
              maxPlacedTerritory = placeTerritory;
              maxPlacedUnit = u;
            }
          }
        }
      }

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, purchaseOptions.getFactoryOptions(), maxTerritory, isBid);
      resourceTracker.removeTempPurchase(maxPlacedOption);
      ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
          proData,
          player,
          startOfTurnData,
          purchaseOptionsForTerritory,
          resourceTracker,
          0,
          List.of(),
          purchaseTerritories,
          1,
          maxTerritory);
      resourceTracker.clearTempPurchases();

      // Determine most expensive factory option (currently doesn't buy mobile factories)
      ProPurchaseOption bestFactoryOption = null;
      double maxFactoryEfficiency = 0;
      for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
        if (ppo.getMovement() == 0 && ppo.getCost() > maxFactoryEfficiency) {
          bestFactoryOption = ppo;
          maxFactoryEfficiency = ppo.getCost();
        }
      }

      // Check if there are enough PUs to buy a factory
      if (bestFactoryOption != null) {
        ProLogger.debug("Best factory unit: " + bestFactoryOption.getUnitType().getName());
        final ProPurchaseTerritory factoryPurchaseTerritory =
            new ProPurchaseTerritory(maxTerritory, data, player, 0);
        factoryPurchaseTerritories.put(maxTerritory, factoryPurchaseTerritory);
        for (final ProPlaceTerritory ppt : factoryPurchaseTerritory.getCanPlaceTerritories()) {
          if (ppt.getTerritory().equals(maxTerritory)) {
            final List<Unit> factory = bestFactoryOption.createTempUnits();
            addUnitsToPlace(ppt, factory);
            if (resourceTracker.hasEnough(bestFactoryOption)) {
              resourceTracker.purchase(bestFactoryOption);
              ProLogger.debug(maxTerritory + ", placedFactory=" + factory);
            } else {
              resourceTracker.purchase(bestFactoryOption);
              resourceTracker.removePurchase(maxPlacedOption);
              if (maxPlacedTerritory != null) {
                maxPlacedTerritory.getPlaceUnits().remove(maxPlacedUnit);
              }
              ProLogger.debug(
                  maxTerritory + ", placedFactory=" + factory + ", removedUnit=" + maxPlacedUnit);
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
        if (t.isWater() && placeTerritory.getStrategicValue() > 0 && placeTerritory.isCanHold()) {
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
      final int numMyTransports =
          CollectionUtils.countMatches(myUnits, Matches.unitIsSeaTransport());
      final int numSeaDefenders =
          CollectionUtils.countMatches(units, Matches.unitIsNotSeaTransport());

      // Determine needed defense strength
      int needDefenders = 0;
      if (enemyAttackOptions.getMax(t) != null) {
        final double strengthDifference =
            ProBattleUtils.estimateStrengthDifference(
                t, enemyAttackOptions.getMax(t).getMaxUnits(), units);
        if (strengthDifference > 50) {
          needDefenders = 1;
        }
      }
      final boolean hasLocalNavalSuperiority =
          ProBattleUtils.territoryHasLocalNavalSuperiority(
              proData, calc, t, player, Map.of(), List.of());
      if (!hasLocalNavalSuperiority) {
        needDefenders = 1;
      }

      // Calculate sea value for prioritization
      final double territoryValue =
          placeTerritory.getStrategicValue()
              * (1 + numMyTransports + 0.1 * numSeaDefenders)
              / (1 + 3.0 * needDefenders);
      ProLogger.debug(
          t
              + ", value="
              + territoryValue
              + ", strategicValue="
              + placeTerritory.getStrategicValue()
              + ", numMyTransports="
              + numMyTransports
              + ", numSeaDefenders="
              + numSeaDefenders
              + ", needDefenders="
              + needDefenders);
      placeTerritory.setStrategicValue(territoryValue);
    }

    // Sort territories by value
    final List<ProPlaceTerritory> sortedTerritories = new ArrayList<>(seaPlaceTerritories);
    sortedTerritories.sort(
        Comparator.comparingDouble(ProPlaceTerritory::getStrategicValue).reversed());
    ProLogger.debug("Sorted sea territories:");
    for (final ProPlaceTerritory placeTerritory : sortedTerritories) {
      ProLogger.debug(placeTerritory.toString() + " value=" + placeTerritory.getStrategicValue());
    }
    return sortedTerritories;
  }

  // Returns true if we should try to save up resources for a fleet next turn.
  private boolean purchaseSeaAndAmphibUnits(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPlaceTerritory> prioritizedSeaTerritories,
      final ProPurchaseOptionMap purchaseOptions) {
    if (resourceTracker.isEmpty()) {
      return false;
    }
    ProLogger.info("Purchase sea and amphib units with resources: " + resourceTracker);

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();
    boolean boughtUnits = false;
    boolean wantedToBuyUnitsButCouldNotDefendThem = false;

    final Predicate<Territory> canMoveSea = ProMatches.territoryCanMoveSeaUnits(player, false);
    // Loop through prioritized territories and purchase sea units
    for (final ProPlaceTerritory placeTerritory : prioritizedSeaTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking sea place for " + t.getName());

      // Find all purchase territories for place territory
      final List<ProPurchaseTerritory> selectedPurchaseTerritories =
          getPurchaseTerritories(placeTerritory, purchaseTerritories);

      // Find local owned units
      final Set<Territory> neighbors = data.getMap().getNeighbors(t, 2, canMoveSea);
      neighbors.add(t);
      final List<Unit> ownedLocalUnits = new ArrayList<>();
      for (final Territory neighbor : neighbors) {
        ownedLocalUnits.addAll(neighbor.getMatches(Matches.unitIsOwnedBy(player)));
      }
      int unusedCarrierCapacity =
          Math.min(0, ProTransportUtils.getUnusedCarrierCapacity(player, t, List.of()));
      int unusedLocalCarrierCapacity =
          ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, List.of());
      ProLogger.trace(
          String.format(
              "%s, unusedCarrierCapacity=%s, unusedLocalCarrierCapacity=%s",
              t, unusedCarrierCapacity, unusedLocalCarrierCapacity));

      // If any enemy attackers then purchase sea defenders until it can be held
      boolean needDestroyer = false;
      ProTerritory maxEnemyAttackTerritory = enemyAttackOptions.getMax(t);
      if (maxEnemyAttackTerritory != null) {
        final Collection<Unit> attackers = maxEnemyAttackTerritory.getMaxUnits();
        // Determine if need destroyer
        Predicate<Unit> ownDestroyer = Matches.unitIsOwnedBy(player).and(Matches.unitIsDestroyer());
        if (attackers.stream().anyMatch(Matches.unitHasSubBattleAbilities())
            && t.getUnits().stream().noneMatch(ownDestroyer)) {
          needDestroyer = true;
        }
        ProLogger.trace(
            String.format(
                "%s, needDestroyer=%s, checking defense since has enemy attackers: %s",
                t, needDestroyer, attackers));
        List<Unit> defendingUnits = new ArrayList<>(placeTerritory.getDefendingUnits());
        defendingUnits.addAll(ProPurchaseUtils.getPlaceUnits(t, purchaseTerritories));
        ProBattleResult result =
            calc.calculateBattleResults(
                proData,
                t,
                attackers,
                defendingUnits,
                maxEnemyAttackTerritory.getMaxBombardUnits());
        final List<Unit> unitsToPlace = new ArrayList<>();
        for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {
          // Check remaining production
          int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
          ProLogger.trace(
              String.format(
                  "%s, purchaseTerritory=%s, remainingUnitProduction=%s",
                  t, purchaseTerritory.getTerritory(), remainingUnitProduction));
          if (remainingUnitProduction <= 0) {
            continue;
          }

          // Determine sea and transport units that can be produced in this territory
          final List<ProPurchaseOption> seaPurchaseOptionsForTerritory =
              ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
                  proData,
                  player,
                  purchaseOptions.getSeaDefenseOptions(),
                  t,
                  purchaseTerritory.getTerritory(),
                  isBid);
          seaPurchaseOptionsForTerritory.addAll(purchaseOptions.getAirOptions());

          // Purchase enough sea defenders to hold territory
          while (true) {
            final boolean hasOnlyRetreatingSubs =
                Properties.getSubRetreatBeforeBattle(data.getProperties())
                    && !defendingUnits.isEmpty()
                    && defendingUnits.stream().allMatch(Matches.unitCanEvade())
                    && attackers.stream().noneMatch(Matches.unitIsDestroyer());
            // If it can be held then break
            if (!hasOnlyRetreatingSubs
                && (result.getTuvSwing() < -1
                    || result.getWinPercentage() < (100.0 - proData.getWinPercentage()))) {
              break;
            }

            // Select purchase option
            ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
                proData,
                player,
                startOfTurnData,
                seaPurchaseOptionsForTerritory,
                resourceTracker,
                remainingUnitProduction,
                unitsToPlace,
                purchaseTerritories,
                0,
                t);
            final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
            for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory) {
              defenseEfficiencies.put(
                  ppo,
                  ppo.getSeaDefenseEfficiency(
                      data,
                      ownedLocalUnits,
                      unitsToPlace,
                      needDestroyer,
                      unusedCarrierCapacity,
                      unusedLocalCarrierCapacity));
            }
            final Optional<ProPurchaseOption> optionalSelectedOption =
                ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Sea Defense");
            if (optionalSelectedOption.isEmpty()) {
              break;
            }
            final ProPurchaseOption selectedOption = optionalSelectedOption.get();
            if (selectedOption.isDestroyer()) {
              needDestroyer = false;
            }

            // Create new temp defenders
            resourceTracker.tempPurchase(selectedOption);
            remainingUnitProduction -= selectedOption.getQuantity();
            unitsToPlace.addAll(selectedOption.createTempUnits());
            if (selectedOption.isCarrier() || selectedOption.isAir()) {
              unusedCarrierCapacity =
                  ProTransportUtils.getUnusedCarrierCapacity(player, t, unitsToPlace);
              unusedLocalCarrierCapacity =
                  ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, unitsToPlace);
            }
            ProLogger.trace(
                t
                    + ", added sea defender for defense: "
                    + selectedOption.getUnitType().getName()
                    + ", TUVSwing="
                    + result.getTuvSwing()
                    + ", win%="
                    + result.getWinPercentage()
                    + ", unusedCarrierCapacity="
                    + unusedCarrierCapacity
                    + ", unusedLocalCarrierCapacity="
                    + unusedLocalCarrierCapacity);

            // Find current battle result
            defendingUnits = new ArrayList<>(placeTerritory.getDefendingUnits());
            defendingUnits.addAll(ProPurchaseUtils.getPlaceUnits(t, purchaseTerritories));
            defendingUnits.addAll(unitsToPlace);
            result =
                calc.estimateDefendBattleResults(
                    proData,
                    t,
                    maxEnemyAttackTerritory.getMaxUnits(),
                    defendingUnits,
                    maxEnemyAttackTerritory.getMaxBombardUnits());
          }
        }

        // Check to see if its worth trying to defend the territory
        if (result.getTuvSwing() < 0
            || result.getWinPercentage() < (100.0 - proData.getWinPercentage())) {
          resourceTracker.confirmTempPurchases();
          ProLogger.trace(
              String.format(
                  "%s, placedUnits=%s, TUVSwing=%s, win%%=%s",
                  t, unitsToPlace, result.getTuvSwing(), result.getWinPercentage()));
          addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
          boughtUnits = true;
        } else {
          resourceTracker.clearTempPurchases();
          setCantHoldPlaceTerritory(placeTerritory, purchaseTerritories);
          ProLogger.trace(
              String.format(
                  "%s, can't defend TUVSwing=%s, win%%=%s, tried to placeDefenders=%s, "
                      + "enemyAttackers=%s, defendingUnits=%s",
                  t,
                  result.getTuvSwing(),
                  result.getWinPercentage(),
                  summarizeUnits(unitsToPlace),
                  summarizeUnits(attackers),
                  summarizeUnits(defendingUnits)));
          wantedToBuyUnitsButCouldNotDefendThem = true;
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
      Predicate<Territory> canMoveAir = ProMatches.territoryCanMoveAirUnits(data, player, false);
      final Set<Territory> nearbyTerritories =
          data.getMap().getNeighbors(t, enemyDistance, canMoveAir);
      final List<Territory> nearbyLandTerritories =
          CollectionUtils.getMatches(nearbyTerritories, Matches.territoryIsLand());
      final Set<Territory> nearbyEnemySeaTerritories =
          data.getMap().getNeighbors(t, enemyDistance, Matches.territoryIsWater());
      nearbyEnemySeaTerritories.add(t);
      final int alliedDistance = (enemyDistance + 1) / 2;
      final Set<Territory> nearbyAlliedSeaTerritories =
          data.getMap().getNeighbors(t, alliedDistance, Matches.territoryIsWater());
      nearbyAlliedSeaTerritories.add(t);
      final List<Unit> enemyUnitsInLandTerritories = new ArrayList<>();
      for (final Territory nearbyLandTerritory : nearbyLandTerritories) {
        enemyUnitsInLandTerritories.addAll(
            nearbyLandTerritory.getMatches(ProMatches.unitIsEnemyAir(player)));
      }
      final Predicate<Unit> enemyNonLandUnit = ProMatches.unitIsEnemyNotLand(player);
      final List<Unit> enemyUnitsInSeaTerritories = new ArrayList<>();
      for (final Territory nearbySeaTerritory : nearbyEnemySeaTerritories) {
        final List<Unit> enemySeaUnits = nearbySeaTerritory.getMatches(enemyNonLandUnit);
        if (enemySeaUnits.isEmpty()) {
          continue;
        }
        final Route route =
            data.getMap()
                .getRouteForUnits(
                    t,
                    nearbySeaTerritory,
                    Matches.territoryIsWater(),
                    enemySeaUnits,
                    enemySeaUnits.get(0).getOwner());
        if (route == null) {
          continue;
        }
        final int routeLength = route.numberOfSteps();
        if (routeLength <= enemyDistance) {
          enemyUnitsInSeaTerritories.addAll(enemySeaUnits);
        }
      }
      final List<Unit> myUnitsInSeaTerritories = new ArrayList<>();
      for (final Territory nearbySeaTerritory : nearbyAlliedSeaTerritories) {
        myUnitsInSeaTerritories.addAll(
            nearbySeaTerritory.getMatches(ProMatches.unitIsOwnedNotLand(player)));
        myUnitsInSeaTerritories.addAll(
            ProPurchaseUtils.getPlaceUnits(nearbySeaTerritory, purchaseTerritories));
      }

      // Check if destroyer is needed
      final int numEnemySubs =
          CollectionUtils.countMatches(
              enemyUnitsInSeaTerritories, Matches.unitHasSubBattleAbilities());
      final int numMyDestroyers =
          CollectionUtils.countMatches(myUnitsInSeaTerritories, Matches.unitIsDestroyer());
      if (numEnemySubs > 2 * numMyDestroyers) {
        needDestroyer = true;
      }
      ProLogger.trace(
          t
              + ", enemyDistance="
              + enemyDistance
              + ", alliedDistance="
              + alliedDistance
              + ", enemyAirUnits="
              + summarizeUnits(enemyUnitsInLandTerritories)
              + ", enemySeaUnits="
              + summarizeUnits(enemyUnitsInSeaTerritories)
              + ", mySeaUnits="
              + summarizeUnits(myUnitsInSeaTerritories)
              + ", needDestroyer="
              + needDestroyer);

      // Purchase naval defenders until I have local naval superiority
      final List<Unit> unitsToPlace = new ArrayList<>();
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {

        // Check remaining production
        int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
        ProLogger.trace(
            t
                + ", purchaseTerritory="
                + purchaseTerritory.getTerritory()
                + ", remainingUnitProduction="
                + remainingUnitProduction);
        if (remainingUnitProduction <= 0) {
          continue;
        }

        // Determine sea and transport units that can be produced in this territory
        final List<ProPurchaseOption> seaPurchaseOptionsForTerritory =
            ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
                proData,
                player,
                purchaseOptions.getSeaDefenseOptions(),
                t,
                purchaseTerritory.getTerritory(),
                isBid);
        seaPurchaseOptionsForTerritory.addAll(purchaseOptions.getAirOptions());
        while (true) {

          // If I have naval attack/defense superiority then break
          if (ProBattleUtils.territoryHasLocalNavalSuperiority(
              proData, calc, t, player, purchaseTerritories, unitsToPlace)) {
            break;
          }

          // Select purchase option
          ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
              proData,
              player,
              startOfTurnData,
              seaPurchaseOptionsForTerritory,
              resourceTracker,
              remainingUnitProduction,
              unitsToPlace,
              purchaseTerritories,
              0,
              t);
          final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
          for (final ProPurchaseOption ppo : seaPurchaseOptionsForTerritory) {
            defenseEfficiencies.put(
                ppo,
                ppo.getSeaDefenseEfficiency(
                    data,
                    ownedLocalUnits,
                    unitsToPlace,
                    needDestroyer,
                    unusedCarrierCapacity,
                    unusedLocalCarrierCapacity));
          }
          final Optional<ProPurchaseOption> optionalSelectedOption =
              ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Sea Defense");
          if (optionalSelectedOption.isEmpty()) {
            break;
          }
          final ProPurchaseOption selectedOption = optionalSelectedOption.get();
          if (selectedOption.isDestroyer()) {
            needDestroyer = false;
          }

          // Create new temp units
          resourceTracker.purchase(selectedOption);
          remainingUnitProduction -= selectedOption.getQuantity();
          unitsToPlace.addAll(selectedOption.createTempUnits());
          if (selectedOption.isCarrier() || selectedOption.isAir()) {
            unusedCarrierCapacity =
                ProTransportUtils.getUnusedCarrierCapacity(player, t, unitsToPlace);
            unusedLocalCarrierCapacity =
                ProTransportUtils.getUnusedLocalCarrierCapacity(player, t, unitsToPlace);
          }
          ProLogger.trace(
              String.format(
                  "%s, added sea defender for naval superiority: %s, "
                      + "unusedCarrierCapacity=%s, unusedLocalCarrierCapacity=%s",
                  t,
                  selectedOption.getUnitType().getName(),
                  unusedCarrierCapacity,
                  unusedLocalCarrierCapacity));
        }
      }

      // Add sea defender units to place territory
      if (!unitsToPlace.isEmpty()) {
        addUnitsToPlaceTerritory(placeTerritory, unitsToPlace, purchaseTerritories);
        boughtUnits = true;
      }

      // Loop through adjacent purchase territories and purchase transport/amphib units
      final int distance =
          ProTransportUtils.findMaxMovementForTransports(purchaseOptions.getSeaTransportOptions());
      final Set<Territory> territoriesToCheck = new HashSet<>();
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {
        final Territory landTerritory = purchaseTerritory.getTerritory();
        final Set<Territory> seaTerritories =
            data.getMap().getNeighbors(landTerritory, distance, canMoveSea);
        for (final Territory seaTerritory : seaTerritories) {
          territoriesToCheck.addAll(data.getMap().getNeighbors(seaTerritory, distance));
        }
        final Set<Territory> landNeighbors =
            data.getMap().getNeighbors(t, Matches.territoryIsLand());
        territoriesToCheck.addAll(landNeighbors);
      }
      final Map<Territory, Double> territoryValueMap =
          ProTerritoryValueUtils.findTerritoryValues(
              proData, player, List.of(), List.of(), territoriesToCheck);
      ProLogger.trace(t + ", transportMovement=" + distance);
      for (final ProPurchaseTerritory purchaseTerritory : selectedPurchaseTerritories) {
        final Territory landTerritory = purchaseTerritory.getTerritory();

        // Check if territory can produce units and has remaining production
        int remainingUnitProduction = purchaseTerritory.getRemainingUnitProduction();
        ProLogger.trace(
            String.format(
                "%s, purchaseTerritory=%s, remainingUnitProduction=%s",
                t, landTerritory, remainingUnitProduction));
        if (remainingUnitProduction <= 0) {
          continue;
        }

        // Find local owned units
        final List<Unit> ownedLocalAmphibUnits =
            landTerritory.getMatches(Matches.unitIsOwnedBy(player));

        // Determine sea and transport units that can be produced in this territory
        final List<ProPurchaseOption> seaTransportPurchaseOptionsForTerritory =
            ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
                proData, player, purchaseOptions.getSeaTransportOptions(), t, landTerritory, isBid);
        final List<ProPurchaseOption> amphibPurchaseOptionsForTerritory =
            ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
                proData, player, purchaseOptions.getLandOptions(), landTerritory, isBid);

        // Find transports that need loaded and units to ignore that are already paired up
        final List<Unit> transportsThatNeedUnits = new ArrayList<>();
        final Set<Unit> potentialUnitsToLoad = new HashSet<>();
        final Set<Territory> seaTerritories =
            data.getMap().getNeighbors(landTerritory, distance, canMoveSea);
        for (final Territory seaTerritory : seaTerritories) {
          final List<Unit> unitsInTerritory =
              ProPurchaseUtils.getPlaceUnits(seaTerritory, purchaseTerritories);
          unitsInTerritory.addAll(seaTerritory.getUnits());
          final List<Unit> transports =
              CollectionUtils.getMatches(unitsInTerritory, ProMatches.unitIsOwnedTransport(player));
          for (final Unit transport : transports) {
            transportsThatNeedUnits.add(transport);
            final Set<Territory> territoriesToLoadFrom =
                new HashSet<>(data.getMap().getNeighbors(seaTerritory, distance));
            territoriesToLoadFrom.removeIf(
                potentialTerritory ->
                    potentialTerritory.isWater()
                        || territoryValueMap.get(potentialTerritory) > 0.25);
            final List<Unit> units =
                ProTransportUtils.getUnitsToTransportFromTerritories(
                    player,
                    transport,
                    territoriesToLoadFrom,
                    potentialUnitsToLoad,
                    ProMatches.unitIsOwnedCombatTransportableUnit(player));
            potentialUnitsToLoad.addAll(units);
          }
        }

        // Determine whether transports, amphib units, or both are needed
        for (final Territory neighbor : data.getMap().getNeighbors(t, Matches.territoryIsLand())) {
          if (territoryValueMap.get(neighbor) <= 0.25) {
            final List<Unit> unitsInTerritory = new ArrayList<>(neighbor.getUnits());
            unitsInTerritory.addAll(ProPurchaseUtils.getPlaceUnits(neighbor, purchaseTerritories));
            potentialUnitsToLoad.addAll(
                CollectionUtils.getMatches(
                    unitsInTerritory, ProMatches.unitIsOwnedCombatTransportableUnit(player)));
          }
        }
        ProLogger.trace(
            String.format(
                "%s, potentialUnitsToLoad=%s,  transportsThatNeedUnits=%s",
                t, summarizeUnits(potentialUnitsToLoad), summarizeUnits(transportsThatNeedUnits)));

        // Purchase transports and amphib units
        final List<Unit> amphibUnitsToPlace = new ArrayList<>();
        final List<Unit> transportUnitsToPlace = new ArrayList<>();
        while (true) {
          if (!transportsThatNeedUnits.isEmpty()) {
            // Get next empty transport and find its capacity
            final Unit transport = transportsThatNeedUnits.get(0);
            int transportCapacity = transport.getUnitAttachment().getTransportCapacity();

            // Find any existing units that can be transported
            final List<Unit> selectedUnits =
                ProTransportUtils.selectUnitsToTransportFromList(
                    transport, new ArrayList<>(potentialUnitsToLoad));
            if (!selectedUnits.isEmpty()) {
              potentialUnitsToLoad.removeAll(selectedUnits);
              transportCapacity -= ProTransportUtils.findUnitsTransportCost(selectedUnits);
            }

            // Purchase units until transport is full
            while (transportCapacity > 0) {
              // Select amphib purchase option and add units
              ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
                  proData,
                  player,
                  startOfTurnData,
                  amphibPurchaseOptionsForTerritory,
                  resourceTracker,
                  remainingUnitProduction,
                  amphibUnitsToPlace,
                  purchaseTerritories,
                  0,
                  t);
              final Map<ProPurchaseOption, Double> amphibEfficiencies = new HashMap<>();
              for (final ProPurchaseOption ppo : amphibPurchaseOptionsForTerritory) {
                if (ppo.getTransportCost() <= transportCapacity) {
                  amphibEfficiencies.put(
                      ppo,
                      ppo.getAmphibEfficiency(data, ownedLocalAmphibUnits, amphibUnitsToPlace));
                }
              }
              final Optional<ProPurchaseOption> optionalSelectedOption =
                  ProPurchaseUtils.randomizePurchaseOption(amphibEfficiencies, "Amphib");
              if (optionalSelectedOption.isEmpty()) {
                break;
              }
              final ProPurchaseOption ppo = optionalSelectedOption.get();

              // Add amphib unit
              amphibUnitsToPlace.addAll(ppo.createTempUnits());
              resourceTracker.purchase(ppo);
              remainingUnitProduction -= ppo.getQuantity();
              transportCapacity -= ppo.getTransportCost();
              ProLogger.trace("Selected unit=" + ppo.getUnitType().getName());
            }
            transportsThatNeedUnits.remove(transport);
          } else {
            // Select purchase option
            ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
                proData,
                player,
                startOfTurnData,
                seaTransportPurchaseOptionsForTerritory,
                resourceTracker,
                remainingUnitProduction,
                transportUnitsToPlace,
                purchaseTerritories,
                0,
                t);
            final Map<ProPurchaseOption, Double> transportEfficiencies = new HashMap<>();
            for (final ProPurchaseOption ppo : seaTransportPurchaseOptionsForTerritory) {
              transportEfficiencies.put(ppo, ppo.getTransportEfficiencyRatio());
            }
            final Optional<ProPurchaseOption> optionalSelectedOption =
                ProPurchaseUtils.randomizePurchaseOption(transportEfficiencies, "Sea Transport");
            if (optionalSelectedOption.isEmpty()) {
              break;
            }
            final ProPurchaseOption ppo = optionalSelectedOption.get();

            // Add transports
            final List<Unit> transports = ppo.createTempUnits();
            transportUnitsToPlace.addAll(transports);
            resourceTracker.purchase(ppo);
            remainingUnitProduction -= ppo.getQuantity();
            transportsThatNeedUnits.addAll(transports);
            ProLogger.trace(
                "Selected unit="
                    + ppo.getUnitType().getName()
                    + ", potentialUnitsToLoad="
                    + potentialUnitsToLoad
                    + ", transportsThatNeedUnits="
                    + transportsThatNeedUnits);
          }
        }

        // Add transport units to sea place territory and amphib units to land place territory
        if (!amphibUnitsToPlace.isEmpty() || !transportUnitsToPlace.isEmpty()) {
          for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {
            if (landTerritory.equals(ppt.getTerritory())) {
              addUnitsToPlace(ppt, amphibUnitsToPlace);
            } else if (placeTerritory.equals(ppt)) {
              addUnitsToPlace(ppt, transportUnitsToPlace);
            }
          }
          boughtUnits = true;
        }
        ProLogger.trace(
            String.format(
                "%s, purchaseTerritory=%s, transportUnitsToPlace=%s, amphibUnitsToPlace=%s",
                t, landTerritory, transportUnitsToPlace, amphibUnitsToPlace));
      }
    }

    // If we wanted to buy a fleet, but didn't because it couldn't be defended, check if we should
    // save up for one. If so, check if we should save up.
    return !boughtUnits
        && wantedToBuyUnitsButCouldNotDefendThem
        && shouldSaveUpForAFleet(purchaseOptions, purchaseTerritories);
  }

  private void purchaseUnitsWithRemainingProduction(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final List<ProPurchaseOption> landPurchaseOptions,
      final List<ProPurchaseOption> airPurchaseOptions) {

    if (resourceTracker.isEmpty()) {
      return;
    }
    ProLogger.info(
        "Purchase units in territories with remaining production with resources: "
            + resourceTracker);

    // Get all safe/unsafe land place territories with remaining production
    final List<ProPlaceTerritory> prioritizedLandTerritories = new ArrayList<>();
    final List<ProPlaceTerritory> prioritizedCantHoldLandTerritories = new ArrayList<>();
    for (final ProPurchaseTerritory ppt : purchaseTerritories.values()) {
      for (final ProPlaceTerritory placeTerritory : ppt.getCanPlaceTerritories()) {
        final Territory t = placeTerritory.getTerritory();
        if (!t.isWater()
            && placeTerritory.isCanHold()
            && purchaseTerritories.get(t).getRemainingUnitProduction() > 0) {
          prioritizedLandTerritories.add(placeTerritory);
        } else if (!t.isWater() && purchaseTerritories.get(t).getRemainingUnitProduction() > 0) {
          prioritizedCantHoldLandTerritories.add(placeTerritory);
        }
      }
    }

    // Sort territories by value
    prioritizedLandTerritories.sort(
        Comparator.comparingDouble(ProPlaceTerritory::getStrategicValue).reversed());
    ProLogger.debug(
        "Sorted land territories with remaining production: " + prioritizedLandTerritories);

    // Loop through territories and purchase long range attack units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking territory: " + t);

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<>(airPurchaseOptions);
      airAndLandPurchaseOptions.addAll(landPurchaseOptions);
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, airAndLandPurchaseOptions, t, isBid);

      // Purchase long range attack units for any remaining production
      int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
      while (true) {
        // Remove options that cost too much PUs or production
        ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
            proData,
            player,
            startOfTurnData,
            purchaseOptionsForTerritory,
            resourceTracker,
            remainingUnitProduction,
            List.of(),
            purchaseTerritories,
            0,
            t);
        if (purchaseOptionsForTerritory.isEmpty()) {
          break;
        }

        // Determine best long range attack option (prefer air units)
        ProPurchaseOption bestAttackOption = null;
        double maxAttackEfficiency = 0;
        for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
          double attackEfficiency =
              ppo.getAttackEfficiency() * ppo.getMovement() / ppo.getQuantity();
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
        addUnitsToPlace(placeTerritory, bestAttackOption.createTempUnits());
      }
    }

    // Sort territories by value
    prioritizedCantHoldLandTerritories.sort(
        Comparator.comparingDouble(ProPlaceTerritory::getDefenseValue).reversed());
    ProLogger.debug(
        "Sorted can't hold land territories with remaining production: "
            + prioritizedCantHoldLandTerritories);

    // Loop through territories and purchase defense units
    for (final ProPlaceTerritory placeTerritory : prioritizedCantHoldLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking territory: " + t);

      // Find local owned units
      final List<Unit> ownedLocalUnits = t.getMatches(Matches.unitIsOwnedBy(player));

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> airAndLandPurchaseOptions = new ArrayList<>(airPurchaseOptions);
      airAndLandPurchaseOptions.addAll(landPurchaseOptions);
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, airAndLandPurchaseOptions, t, isBid);

      // Purchase defense units for any remaining production
      int remainingUnitProduction = purchaseTerritories.get(t).getRemainingUnitProduction();
      while (true) {
        // Select purchase option
        ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
            proData,
            player,
            startOfTurnData,
            purchaseOptionsForTerritory,
            resourceTracker,
            remainingUnitProduction,
            List.of(),
            purchaseTerritories,
            0,
            t);
        final Map<ProPurchaseOption, Double> defenseEfficiencies = new HashMap<>();
        for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
          defenseEfficiencies.put(
              ppo,
              Math.pow(ppo.getCost(), 2)
                  * ppo.getDefenseEfficiency(
                      1, data, ownedLocalUnits, placeTerritory.getPlaceUnits()));
        }
        final Optional<ProPurchaseOption> optionalSelectedOption =
            ProPurchaseUtils.randomizePurchaseOption(defenseEfficiencies, "Defense");
        if (optionalSelectedOption.isEmpty()) {
          break;
        }
        final ProPurchaseOption selectedOption = optionalSelectedOption.get();

        // Purchase unit
        resourceTracker.purchase(selectedOption);
        remainingUnitProduction -= selectedOption.getQuantity();
        addUnitsToPlace(placeTerritory, selectedOption.createTempUnits());
      }
    }
  }

  private void upgradeUnitsWithRemainingPUs(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
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
    prioritizedLandTerritories.sort(
        Comparator.comparingDouble(ProPlaceTerritory::getStrategicValue));
    ProLogger.debug("Sorted land territories: " + prioritizedLandTerritories);

    // Loop through territories and upgrade units to long range attack units
    for (final ProPlaceTerritory placeTerritory : prioritizedLandTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking territory: " + t);

      // Determine units that can be produced in this territory
      final List<ProPurchaseOption> airAndLandPurchaseOptions =
          new ArrayList<>(purchaseOptions.getAirOptions());
      airAndLandPurchaseOptions.addAll(purchaseOptions.getLandOptions());
      final List<ProPurchaseOption> purchaseOptionsForTerritory =
          ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
              proData, player, airAndLandPurchaseOptions, t, isBid);

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
                && (minPurchaseOption == null || ppo.getCost() < minPurchaseOption.getCost())) {
              minPurchaseOption = ppo;
            }
          }
        }
        if (minPurchaseOption == null) {
          break;
        }

        // Remove options that cost too much PUs or production
        resourceTracker.removeTempPurchase(minPurchaseOption);
        ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
            proData,
            player,
            startOfTurnData,
            purchaseOptionsForTerritory,
            resourceTracker,
            1,
            List.of(),
            purchaseTerritories,
            0,
            t);
        resourceTracker.clearTempPurchases();
        if (purchaseOptionsForTerritory.isEmpty()) {
          break;
        }

        // Determine best upgrade option (prefer air units)
        // TODO: ensure map has carriers or air unit has range to reach enemy land mass
        ProPurchaseOption bestUpgradeOption = null;
        double maxEfficiency =
            findUpgradeUnitEfficiency(minPurchaseOption, placeTerritory.getStrategicValue());
        for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
          if (!ppo.isConsumesUnits()
              && ppo.getCost() > minPurchaseOption.getCost()
              && (ppo.isAir()
                  || placeTerritory.getStrategicValue() >= 0.25
                  || ppo.getTransportCost() <= minPurchaseOption.getTransportCost())) {
            double efficiency = findUpgradeUnitEfficiency(ppo, placeTerritory.getStrategicValue());
            if (ppo.isAir()) {
              efficiency *= 10;
            }
            if (ppo.getCarrierCost() > 0) {
              final int unusedLocalCarrierCapacity =
                  ProTransportUtils.getUnusedLocalCarrierCapacity(
                      player, t, placeTerritory.getPlaceUnits());
              final int neededFighters = unusedLocalCarrierCapacity / ppo.getCarrierCost();
              efficiency *= (1 + neededFighters);
            }
            if (efficiency > maxEfficiency) {
              bestUpgradeOption = ppo;
              maxEfficiency = efficiency;
            }
          }
        }
        if (bestUpgradeOption == null) {
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
          if (resourceTracker.hasEnough(bestUpgradeOption)) {
            resourceTracker.purchase(bestUpgradeOption);
            addUnitsToPlace(placeTerritory, bestUpgradeOption.createTempUnits());
          }
        }
      }
    }
  }

  /**
   * Determine efficiency value for upgrading to the given purchase option. If the strategic value
   * of the territory is low then favor high movement units as its far from the enemy otherwise
   * favor high defense.
   */
  private static double findUpgradeUnitEfficiency(
      final ProPurchaseOption ppo, final double strategicValue) {
    final double multiplier =
        (strategicValue >= 1) ? ppo.getDefenseEfficiency() : ppo.getMovement();
    return ppo.getAttackEfficiency() * multiplier * ppo.getCost() / ppo.getQuantity();
  }

  private IntegerMap<ProductionRule> populateProductionRuleMap(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final ProPurchaseOptionMap purchaseOptions) {

    ProLogger.info("Populate production rule map");
    final List<Unit> unplacedUnits = player.getMatches(Matches.unitIsNotSea());
    final IntegerMap<ProductionRule> purchaseMap = new IntegerMap<>();
    for (final ProPurchaseOption ppo : purchaseOptions.getAllOptions()) {
      final int numUnits =
          (int)
              purchaseTerritories.values().stream()
                  .map(ProPurchaseTerritory::getCanPlaceTerritories)
                  .flatMap(Collection::stream)
                  .map(ProPlaceTerritory::getPlaceUnits)
                  .flatMap(Collection::stream)
                  .filter(u -> u.getType().equals(ppo.getUnitType()))
                  .filter(u -> !unplacedUnits.contains(u))
                  .count();
      if (numUnits > 0) {
        final int numProductionRule = numUnits / ppo.getQuantity();
        purchaseMap.put(ppo.getProductionRule(), numProductionRule);
        ProLogger.info(numProductionRule + " " + ppo.getProductionRule());
      }
    }
    return purchaseMap;
  }

  private void placeDefenders(
      final Map<Territory, ProPurchaseTerritory> placeNonConstructionTerritories,
      final List<ProPlaceTerritory> needToDefendTerritories,
      final IAbstractPlaceDelegate placeDelegate) {
    ProLogger.info("Place defenders with units=" + player.getUnits());

    final ProOtherMoveOptions enemyAttackOptions = territoryManager.getEnemyAttackOptions();

    // Loop through prioritized territories and purchase defenders
    for (final ProPlaceTerritory placeTerritory : needToDefendTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug(
          "Placing defenders for "
              + t.getName()
              + ", enemyAttackers="
              + summarizeUnits(enemyAttackOptions.getMax(t).getMaxUnits())
              + ", amphibEnemyAttackers="
              + summarizeUnits(enemyAttackOptions.getMax(t).getMaxAmphibUnits())
              + ", defenders="
              + summarizeUnits(placeTerritory.getDefendingUnits()));

      // Check if any units can be placed
      final PlaceableUnits placeableUnits =
          placeDelegate.getPlaceableUnits(player.getMatches(Matches.unitIsNotConstruction()), t);
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
        final Set<Unit> enemyAttackingUnits =
            new HashSet<>(enemyAttackOptions.getMax(t).getMaxUnits());
        enemyAttackingUnits.addAll(enemyAttackOptions.getMax(t).getMaxAmphibUnits());
        final List<Unit> defenders = new ArrayList<>(placeTerritory.getDefendingUnits());
        defenders.addAll(unitsToPlace);
        finalResult =
            calc.calculateBattleResults(
                proData,
                t,
                enemyAttackingUnits,
                defenders,
                enemyAttackOptions.getMax(t).getMaxBombardUnits());

        // Break if it can be held
        if ((!t.equals(proData.getMyCapital())
                && !finalResult.isHasLandUnitRemaining()
                && finalResult.getTuvSwing() <= 0)
            || (t.equals(proData.getMyCapital())
                && finalResult.getWinPercentage() < (100 - proData.getWinPercentage())
                && finalResult.getTuvSwing() <= 0)) {
          break;
        }
      }

      // Check to see if its worth trying to defend the territory
      if (!finalResult.isHasLandUnitRemaining()
          || finalResult.getTuvSwing() < placeTerritory.getMinBattleResult().getTuvSwing()
          || t.equals(proData.getMyCapital())) {
        ProLogger.trace(
            t + ", placedUnits=" + unitsToPlace + ", TUVSwing=" + finalResult.getTuvSwing());
        doPlace(t, unitsToPlace, placeDelegate);
      } else {
        setCantHoldPlaceTerritory(placeTerritory, placeNonConstructionTerritories);
        ProLogger.trace(
            t
                + ", unable to defend with placedUnits="
                + unitsToPlace
                + ", TUVSwing="
                + finalResult.getTuvSwing()
                + ", minTUVSwing="
                + placeTerritory.getMinBattleResult().getTuvSwing());
      }
    }
  }

  private void placeUnits(
      final List<ProPlaceTerritory> prioritizedTerritories,
      final IAbstractPlaceDelegate placeDelegate,
      final Predicate<Unit> unitMatch) {
    ProLogger.info("Place units=" + player.getUnits());

    // Loop through prioritized territories and place units
    for (final ProPlaceTerritory placeTerritory : prioritizedTerritories) {
      final Territory t = placeTerritory.getTerritory();
      ProLogger.debug("Checking place for " + t.getName());

      // Check if any units can be placed
      final PlaceableUnits placeableUnits =
          placeDelegate.getPlaceableUnits(player.getMatches(unitMatch), t);
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
      final int placeCount = Math.min(remainingUnitProduction, unitsThatCanBePlaced.size());
      final List<Unit> unitsToPlace = unitsThatCanBePlaced.subList(0, placeCount);
      ProLogger.trace(t + ", placedUnits=" + unitsToPlace);
      doPlace(t, unitsToPlace, placeDelegate);
    }
  }

  private void addUnitsToPlaceTerritory(
      final ProPlaceTerritory placeTerritory,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    // Add units to place territory
    for (final ProPurchaseTerritory purchaseTerritory : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {

        // If place territory is equal to the current place territory and has remaining production
        if (placeTerritory.equals(ppt)
            && purchaseTerritory.getRemainingUnitProduction() > 0
            && ProPurchaseValidationUtils.canUnitsBePlaced(
                proData,
                unitsToPlace,
                player,
                ppt.getTerritory(),
                purchaseTerritory.getTerritory(),
                isBid)) {
          final List<Unit> constructions =
              CollectionUtils.getMatches(unitsToPlace, Matches.unitIsConstruction());
          unitsToPlace.removeAll(constructions);
          addUnitsToPlace(ppt, constructions);
          final int numUnits =
              Math.min(purchaseTerritory.getRemainingUnitProduction(), unitsToPlace.size());
          final List<Unit> units = unitsToPlace.subList(0, numUnits);
          addUnitsToPlace(ppt, units);
          units.clear();
        }
      }
    }
  }

  private void addUnitsToPlace(ProPlaceTerritory ppt, Collection<Unit> unitsToPlace) {
    if (unitsToPlace.isEmpty()) {
      return;
    }
    ppt.getPlaceUnits().addAll(unitsToPlace);
    ProLogger.trace(ppt.getTerritory() + ", placedUnits=" + unitsToPlace);
    // TODO: If consumed units can come from a different territory, this will need to change.
    Collection<Unit> candidateUnitsToConsume =
        CollectionUtils.difference(ppt.getTerritory().getUnits(), proData.getUnitsToBeConsumed());
    Collection<Unit> toConsume =
        ProPurchaseUtils.getUnitsToConsume(player, candidateUnitsToConsume, unitsToPlace);
    if (!toConsume.isEmpty()) {
      ProLogger.trace(" toConsume=" + toConsume);
      proData.getUnitsToBeConsumed().addAll(toConsume);
    }
  }

  private static void setCantHoldPlaceTerritory(
      final ProPlaceTerritory placeTerritory,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    // Add units to place territory
    for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : t.getCanPlaceTerritories()) {
        // If place territory is equal to the current place territory
        if (placeTerritory.equals(ppt)) {
          ppt.setCanHold(false);
        }
      }
    }
  }

  private static List<ProPurchaseTerritory> getPurchaseTerritories(
      final ProPlaceTerritory placeTerritory,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    final List<ProPurchaseTerritory> territories = new ArrayList<>();
    for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
      if (t.getCanPlaceTerritories().contains(placeTerritory)) {
        territories.add(t);
      }
    }
    return territories;
  }

  private static void doPlace(
      final Territory t, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del) {
    for (final Unit unit : toPlace) {
      final String message =
          del.placeUnits(List.of(unit), t, IAbstractPlaceDelegate.BidMode.NOT_BID);
      if (message != null) {
        ProLogger.warn(message);
        ProLogger.warn("Attempt was at: " + t + " with: " + unit);
      }
    }
    AbstractAi.movePause();
  }
}
