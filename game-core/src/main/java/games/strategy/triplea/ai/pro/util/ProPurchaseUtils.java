package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProPlaceTerritory;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.pro.data.ProResourceTracker;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractPlaceDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.TransportTracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Pro AI purchase utilities. */
public final class ProPurchaseUtils {
  private ProPurchaseUtils() {}

  public static List<ProPurchaseOption> findPurchaseOptionsForTerritory(
      final ProData proData,
      final GamePlayer player,
      final List<ProPurchaseOption> purchaseOptions,
      final Territory t,
      final boolean isBid) {
    return findPurchaseOptionsForTerritory(proData, player, purchaseOptions, t, t, isBid);
  }

  public static List<ProPurchaseOption> findPurchaseOptionsForTerritory(
      final ProData proData,
      final GamePlayer player,
      final List<ProPurchaseOption> purchaseOptions,
      final Territory t,
      final Territory factoryTerritory,
      final boolean isBid) {
    final List<ProPurchaseOption> result = new ArrayList<>();
    for (final ProPurchaseOption ppo : purchaseOptions) {
      if (canTerritoryUsePurchaseOption(proData, player, ppo, t, factoryTerritory, isBid)) {
        result.add(ppo);
      }
    }
    return result;
  }

  private static boolean canTerritoryUsePurchaseOption(
      final ProData proData,
      final GamePlayer player,
      final ProPurchaseOption ppo,
      final Territory t,
      final Territory factoryTerritory,
      final boolean isBid) {
    if (ppo == null) {
      return false;
    }
    final List<Unit> units = ppo.getUnitType().create(ppo.getQuantity(), player, true);
    return canUnitsBePlaced(proData, units, player, t, factoryTerritory, isBid);
  }

  public static boolean canUnitsBePlaced(
      final ProData proData,
      final List<Unit> units,
      final GamePlayer player,
      final Territory t,
      final boolean isBid) {
    return canUnitsBePlaced(proData, units, player, t, t, isBid);
  }

  /** Check if units can be placed in given territory by specified factory. */
  public static boolean canUnitsBePlaced(
      final ProData proData,
      final List<Unit> units,
      final GamePlayer player,
      final Territory t,
      final Territory factoryTerritory,
      final boolean isBid) {
    final GameData data = player.getData();
    AbstractPlaceDelegate placeDelegate = (AbstractPlaceDelegate) data.getDelegate("place");
    if (isBid) {
      placeDelegate = (AbstractPlaceDelegate) data.getDelegate("placeBid");
    } else if (!t.equals(factoryTerritory)
        && !units.stream()
            .allMatch(
                Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(
                    placeDelegate.unitsAtStartOfStepInTerritory(factoryTerritory)))) {
      return false;
    }
    final IDelegateBridge bridge = new ProDummyDelegateBridge(proData.getProAi(), player, data);
    placeDelegate.setDelegateBridgeAndPlayer(bridge);
    return isPlacingFightersOnNewCarriers(t, units)
        ? placeDelegate.canUnitsBePlaced(
                t, CollectionUtils.getMatches(units, Matches.unitIsNotAir()), player)
            == null
        : placeDelegate.canUnitsBePlaced(t, units, player) == null;
  }

  private static boolean isPlacingFightersOnNewCarriers(final Territory t, final List<Unit> units) {
    return t.isWater()
        && Properties.getProduceFightersOnCarriers(t.getData())
        && units.stream().anyMatch(Matches.unitIsAir())
        && units.stream().anyMatch(Matches.unitIsCarrier());
  }

  public static void removeInvalidPurchaseOptions(
      final GamePlayer player,
      final GameData data,
      final List<ProPurchaseOption> purchaseOptions,
      final ProResourceTracker resourceTracker,
      final int remainingUnitProduction,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    ProPurchaseUtils.removeInvalidPurchaseOptions(
        player,
        data,
        purchaseOptions,
        resourceTracker,
        remainingUnitProduction,
        unitsToPlace,
        purchaseTerritories,
        0,
        null);
  }

  /** Removes any invalid purchase options from {@code purchaseOptions}. */
  public static void removeInvalidPurchaseOptions(
      final GamePlayer player,
      final GameData data,
      final List<ProPurchaseOption> purchaseOptions,
      final ProResourceTracker resourceTracker,
      final int remainingUnitProduction,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final int remainingConstructions,
      final Territory territory) {

    for (final Iterator<ProPurchaseOption> it = purchaseOptions.iterator(); it.hasNext(); ) {
      final ProPurchaseOption purchaseOption = it.next();
      if (!hasEnoughResourcesAndProduction(
          purchaseOption, resourceTracker, remainingUnitProduction, remainingConstructions)) {
        it.remove();
        continue;
      }
      if (hasReachedMaxUnitBuiltPerPlayer(
          purchaseOption, player, data, unitsToPlace, purchaseTerritories)) {
        it.remove();
        continue;
      }
      if (hasReachedConstructionLimits(
          purchaseOption, data, unitsToPlace, purchaseTerritories, territory)) {
        it.remove();
      }
    }
  }

  private static boolean hasEnoughResourcesAndProduction(
      final ProPurchaseOption purchaseOption,
      final ProResourceTracker resourceTracker,
      final int remainingUnitProduction,
      final int remainingConstructions) {
    return resourceTracker.hasEnough(purchaseOption)
        && purchaseOption.getQuantity()
            <= (purchaseOption.isConstruction() ? remainingConstructions : remainingUnitProduction);
  }

  private static boolean hasReachedMaxUnitBuiltPerPlayer(
      final ProPurchaseOption purchaseOption,
      final GamePlayer player,
      final GameData data,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    // Check max unit limits (-1 is unlimited)
    final int maxBuilt = purchaseOption.getMaxBuiltPerPlayer();
    final UnitType type = purchaseOption.getUnitType();
    if (maxBuilt == 0) {
      return true;
    } else if (maxBuilt > 0) {

      // Find number of unit type that are already built and about to be placed
      int currentlyBuilt = 0;
      final Predicate<Unit> unitTypeOwnedBy =
          Matches.unitIsOfType(type).and(Matches.unitIsOwnedBy(player));
      final List<Territory> allTerritories = data.getMap().getTerritories();
      for (final Territory t : allTerritories) {
        currentlyBuilt += t.getUnitCollection().countMatches(unitTypeOwnedBy);
      }
      currentlyBuilt += CollectionUtils.countMatches(unitsToPlace, unitTypeOwnedBy);
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory placeTerritory : t.getCanPlaceTerritories()) {
          currentlyBuilt +=
              CollectionUtils.countMatches(placeTerritory.getPlaceUnits(), unitTypeOwnedBy);
        }
      }
      final int allowedBuild = maxBuilt - currentlyBuilt;
      if (allowedBuild - purchaseOption.getQuantity() < 0) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasReachedConstructionLimits(
      final ProPurchaseOption purchaseOption,
      final GameData data,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final Territory territory) {

    // Check construction limits
    if (purchaseOption.isConstruction() && territory != null) {

      // Check constructions per turn
      int numConstructionTypeToPlace =
          CollectionUtils.countMatches(
              unitsToPlace, Matches.unitIsOfType(purchaseOption.getUnitType()));
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory placeTerritory : t.getCanPlaceTerritories()) {
          if (placeTerritory.getTerritory().equals(territory)) {
            numConstructionTypeToPlace +=
                CollectionUtils.countMatches(
                    placeTerritory.getPlaceUnits(),
                    Matches.unitIsOfType(purchaseOption.getUnitType()));
          }
        }
      }
      if (numConstructionTypeToPlace >= purchaseOption.getConstructionTypePerTurn()) {
        return true;
      }

      // Check max constructions (assumes constructions being placed at a factory)
      int maxConstructionType = purchaseOption.getMaxConstructionType();
      final String constructionType = purchaseOption.getConstructionType();
      if (!constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
          && !constructionType.endsWith("structure")) {
        if (Properties.getUnlimitedConstructions(data)) {
          maxConstructionType = Integer.MAX_VALUE;
        } else if (Properties.getMoreConstructionsWithFactory(data)) {
          int production = 0;
          final TerritoryAttachment terrAttachment = TerritoryAttachment.get(territory);
          if (terrAttachment != null) {
            production = terrAttachment.getProduction();
          }
          maxConstructionType = Math.max(maxConstructionType, production);
        }
      }
      final int numTotalConstructionType =
          numConstructionTypeToPlace
              + CollectionUtils.countMatches(
                  territory.getUnits(), Matches.unitIsOfType(purchaseOption.getUnitType()));
      if (numTotalConstructionType >= maxConstructionType) {
        return true;
      }
    }
    return false;
  }

  /**
   * Randomly selects one of the specified purchase options of the specified type.
   *
   * @return The selected purchase option or empty if no purchase option of the specified type is
   *     available.
   */
  public static Optional<ProPurchaseOption> randomizePurchaseOption(
      final Map<ProPurchaseOption, Double> purchaseEfficiencies, final String type) {

    ProLogger.trace("Select purchase option for " + type);
    double totalEfficiency = 0;
    for (final Double efficiency : purchaseEfficiencies.values()) {
      totalEfficiency += efficiency;
    }
    if (totalEfficiency == 0) {
      return Optional.empty();
    }
    final Map<ProPurchaseOption, Double> purchasePercentages = new LinkedHashMap<>();
    double upperBound = 0.0;
    for (final ProPurchaseOption ppo : purchaseEfficiencies.keySet()) {
      final double chance = purchaseEfficiencies.get(ppo) / totalEfficiency * 100;
      upperBound += chance;
      purchasePercentages.put(ppo, upperBound);
      ProLogger.trace(
          ppo.getUnitType().getName() + ", probability=" + chance + ", upperBound=" + upperBound);
    }
    final double randomNumber = Math.random() * 100;
    ProLogger.trace("Random number: " + randomNumber);
    for (final ProPurchaseOption ppo : purchasePercentages.keySet()) {
      if (randomNumber <= purchasePercentages.get(ppo)) {
        return Optional.of(ppo);
      }
    }
    return Optional.of(purchasePercentages.keySet().iterator().next());
  }

  /**
   * Returns the list of units to purchase that will maximize defense in the specified territory
   * based on the specified purchase options.
   */
  public static List<Unit> findMaxPurchaseDefenders(
      final ProData proData,
      final GamePlayer player,
      final Territory t,
      final List<ProPurchaseOption> landPurchaseOptions) {

    ProLogger.info("Find max purchase defenders for " + t.getName());
    final GameData data = proData.getData();

    // Determine most cost efficient defender that can be produced in this territory
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int pusRemaining = player.getResources().getQuantity(pus);
    final List<ProPurchaseOption> purchaseOptionsForTerritory =
        findPurchaseOptionsForTerritory(proData, player, landPurchaseOptions, t, false);
    ProPurchaseOption bestDefenseOption = null;
    double maxDefenseEfficiency = 0;
    for (final ProPurchaseOption ppo : purchaseOptionsForTerritory) {
      if (ppo.getDefenseEfficiency() > maxDefenseEfficiency && ppo.getCost() <= pusRemaining) {
        bestDefenseOption = ppo;
        maxDefenseEfficiency = ppo.getDefenseEfficiency();
      }
    }

    // Determine number of defenders I can purchase
    final List<Unit> placeUnits = new ArrayList<>();
    if (bestDefenseOption != null) {
      ProLogger.debug("Best defense option: " + bestDefenseOption.getUnitType().getName());
      int remainingUnitProduction = getUnitProduction(t, data, player);
      int pusSpent = 0;
      while (bestDefenseOption.getCost() <= (pusRemaining - pusSpent)
          && remainingUnitProduction >= bestDefenseOption.getQuantity()) {

        // If out of PUs or production then break

        // Create new temp defenders
        pusSpent += bestDefenseOption.getCost();
        remainingUnitProduction -= bestDefenseOption.getQuantity();
        placeUnits.addAll(
            bestDefenseOption.getUnitType().create(bestDefenseOption.getQuantity(), player, true));
      }
      ProLogger.debug("Potential purchased defenders: " + placeUnits);
    }
    return placeUnits;
  }

  /**
   * Find all territories that bid units can be placed in and initialize data holders for them.
   *
   * @param proData - the pro AI data
   * @param player - current AI player
   * @return - map of all available purchase and place territories
   */
  public static Map<Territory, ProPurchaseTerritory> findBidTerritories(
      final ProData proData, final GamePlayer player) {

    ProLogger.info("Find all bid territories");
    final GameData data = proData.getData();

    // Find all territories that I can place units on
    final Set<Territory> ownedOrHasUnitTerritories =
        new HashSet<>(data.getMap().getTerritoriesOwnedBy(player));
    ownedOrHasUnitTerritories.addAll(proData.getMyUnitTerritories());
    final List<Territory> potentialTerritories =
        CollectionUtils.getMatches(
            ownedOrHasUnitTerritories,
            Matches.territoryIsPassableAndNotRestrictedAndOkByRelationships(
                player, data, false, false, false, false, false));

    // Create purchase territory holder for each factory territory
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories = new HashMap<>();
    for (final Territory t : potentialTerritories) {
      final ProPurchaseTerritory ppt = new ProPurchaseTerritory(t, data, player, 1, true);
      purchaseTerritories.put(t, ppt);
      ProLogger.debug(ppt.toString());
    }
    return purchaseTerritories;
  }

  public static void incrementUnitProductionForBidTerritories(
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    purchaseTerritories.values().forEach(ppt -> ppt.setUnitProduction(ppt.getUnitProduction() + 1));
  }

  /** Returns all possible territories within which {@code player} can place purchased units. */
  public static Map<Territory, ProPurchaseTerritory> findPurchaseTerritories(
      final ProData proData, final GamePlayer player) {

    ProLogger.info("Find all purchase territories");
    final GameData data = proData.getData();

    // Find all territories that I can place units on
    final RulesAttachment ra = player.getRulesAttachment();
    List<Territory> ownedAndNotConqueredFactoryTerritories;
    if (ra != null && ra.getPlacementAnyTerritory()) {
      ownedAndNotConqueredFactoryTerritories = data.getMap().getTerritoriesOwnedBy(player);
    } else {
      ownedAndNotConqueredFactoryTerritories =
          CollectionUtils.getMatches(
              data.getMap().getTerritories(),
              ProMatches.territoryHasFactoryAndIsNotConqueredOwnedLand(player, data));
    }
    ownedAndNotConqueredFactoryTerritories =
        CollectionUtils.getMatches(
            ownedAndNotConqueredFactoryTerritories,
            ProMatches.territoryCanMoveLandUnits(player, data, false));

    // Create purchase territory holder for each factory territory
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories = new HashMap<>();
    for (final Territory t : ownedAndNotConqueredFactoryTerritories) {
      final int unitProduction = getUnitProduction(t, data, player);
      final ProPurchaseTerritory ppt = new ProPurchaseTerritory(t, data, player, unitProduction);
      purchaseTerritories.put(t, ppt);
      ProLogger.debug(ppt.toString());
    }
    return purchaseTerritories;
  }

  private static int getUnitProduction(
      final Territory territory, final GameData data, final GamePlayer player) {
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            .and((territory.isWater() ? Matches.unitIsLand() : Matches.unitIsSea()).negate());
    final Collection<Unit> factoryUnits = territory.getUnitCollection().getMatches(factoryMatch);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    final boolean originalFactory = (ta != null && ta.getOriginalFactory());
    final boolean playerIsOriginalOwner =
        !factoryUnits.isEmpty() && player.equals(getOriginalFactoryOwner(territory, player));
    final RulesAttachment ra = player.getRulesAttachment();
    if (originalFactory && playerIsOriginalOwner) {
      if (ra != null && ra.getMaxPlacePerTerritory() != -1) {
        return Math.max(0, ra.getMaxPlacePerTerritory());
      }
      return Integer.MAX_VALUE;
    }
    if (ra != null && ra.getPlacementAnyTerritory()) {
      return Integer.MAX_VALUE;
    }
    return TripleAUnit.getProductionPotentialOfTerritory(
        territory.getUnits(), territory, player, data, true, true);
  }

  /**
   * Calculates how many of each of the specified construction units can be placed in the specified
   * territory.
   */
  public static int getMaxConstructions(
      final Territory territory,
      final GameData data,
      final GamePlayer player,
      final List<ProPurchaseOption> zeroMoveDefensePurchaseOptions) {
    final IntegerMap<String> constructionTypesPerTurn = new IntegerMap<>();
    for (final ProPurchaseOption ppo : zeroMoveDefensePurchaseOptions) {
      final UnitAttachment ua = UnitAttachment.get(ppo.getUnitType());
      if (ua.getIsConstruction()) {
        final String constructionType = ua.getConstructionType();
        final int constructionTypePerTurn = ua.getConstructionsPerTerrPerTypePerTurn();
        constructionTypesPerTurn.put(constructionType, constructionTypePerTurn);
      }
    }
    return constructionTypesPerTurn.totalValues();
  }

  private static GamePlayer getOriginalFactoryOwner(
      final Territory territory, final GamePlayer player) {

    final Collection<Unit> factoryUnits =
        territory.getUnitCollection().getMatches(Matches.unitCanProduceUnits());
    if (factoryUnits.isEmpty()) {
      throw new IllegalStateException("No factory in territory:" + territory);
    }
    for (final Unit factory2 : factoryUnits) {
      if (player.equals(OriginalOwnerTracker.getOriginalOwner(factory2))) {
        return OriginalOwnerTracker.getOriginalOwner(factory2);
      }
    }
    final Unit factory = factoryUnits.iterator().next();
    return OriginalOwnerTracker.getOriginalOwner(factory);
  }

  /** Comparator that sorts cheaper units before expensive ones. */
  public static Comparator<Unit> getCostComparator(final ProData proData) {
    return Comparator.comparingDouble((unit) -> ProPurchaseUtils.getCost(proData, unit));
  }

  /**
   * How many PU's does it cost the given player to produce the given unit including any dependents.
   */
  public static double getCost(final ProData proData, final Unit unit) {
    final Resource pus = unit.getData().getResourceList().getResource(Constants.PUS);
    final Collection<Unit> units = TransportTracker.transportingAndUnloaded(unit);
    units.add(unit);
    double cost = 0.0;
    for (final Unit u : units) {
      final ProductionRule rule = getProductionRule(u.getType(), u.getOwner());
      if (rule == null) {
        cost += proData.getUnitValue(u.getType());
      } else {
        cost += ((double) rule.getCosts().getInt(pus)) / rule.getResults().totalValues();
      }
    }
    return cost;
  }

  /**
   * Get the production rule for the given player, for the given unit type.
   *
   * <p>If no such rule can be found, then return null.
   */
  private static ProductionRule getProductionRule(
      final UnitType unitType, final GamePlayer player) {
    final ProductionFrontier frontier = player.getProductionFrontier();
    if (frontier == null) {
      return null;
    }
    for (final ProductionRule rule : frontier) {
      if (rule.getResults().getInt(unitType) > 0) {
        return rule;
      }
    }
    return null;
  }

  /**
   * Returns the list of units to place in the specified territory based on the specified purchases.
   */
  public static List<Unit> getPlaceUnits(
      final Territory t, final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {

    final List<Unit> placeUnits = new ArrayList<>();
    if (purchaseTerritories == null) {
      return placeUnits;
    }
    for (final ProPurchaseTerritory purchaseTerritory : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {
        if (t.equals(ppt.getTerritory())) {
          placeUnits.addAll(ppt.getPlaceUnits());
        }
      }
    }
    return placeUnits;
  }
}
