package games.strategy.triplea.ai.pro.util;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProPlaceTerritory;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Pro AI purchase utilities. */
@UtilityClass
public final class ProPurchaseUtils {

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
    return Optional.of(CollectionUtils.getAny(purchasePercentages.keySet()));
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
    final GameState data = proData.getData();

    // Determine most cost efficient defender that can be produced in this territory
    final Resource pus = data.getResourceList().getResource(Constants.PUS).orElse(null);
    final int pusRemaining = player.getResources().getQuantity(pus);
    final List<ProPurchaseOption> purchaseOptionsForTerritory =
        ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
            proData, player, landPurchaseOptions, t, false);
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
      int remainingUnitProduction = getUnitProduction(t, player);
      int pusSpent = 0;
      while (bestDefenseOption.getCost() <= (pusRemaining - pusSpent)
          && remainingUnitProduction >= bestDefenseOption.getQuantity()) {

        // If out of PUs or production then break

        // Create new temp defenders
        pusSpent += bestDefenseOption.getCost();
        remainingUnitProduction -= bestDefenseOption.getQuantity();
        placeUnits.addAll(
            bestDefenseOption.getUnitType().createTemp(bestDefenseOption.getQuantity(), player));
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
                player, false, false, false, false, false));

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
              ProMatches.territoryHasFactoryAndIsNotConqueredOwnedLand(player));
    }
    ownedAndNotConqueredFactoryTerritories =
        CollectionUtils.getMatches(
            ownedAndNotConqueredFactoryTerritories,
            ProMatches.territoryCanMoveLandUnits(player, false));

    // Create purchase territory holder for each factory territory
    final Map<Territory, ProPurchaseTerritory> purchaseTerritories = new HashMap<>();
    for (final Territory t : ownedAndNotConqueredFactoryTerritories) {
      final int unitProduction = getUnitProduction(t, player);
      final ProPurchaseTerritory ppt = new ProPurchaseTerritory(t, data, player, unitProduction);
      purchaseTerritories.put(t, ppt);
      ProLogger.debug(ppt.toString());
    }
    return purchaseTerritories;
  }

  private static int getUnitProduction(final Territory territory, final GamePlayer player) {
    final Predicate<Unit> factoryMatch =
        Matches.unitIsOwnedAndIsFactoryOrCanProduceUnits(player)
            .and(Matches.unitIsBeingTransported().negate())
            .and((territory.isWater() ? Matches.unitIsLand() : Matches.unitIsSea()).negate());
    final Collection<Unit> factoryUnits = territory.getMatches(factoryMatch);
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
    return UnitUtils.getProductionPotentialOfTerritory(
        territory.getUnits(), territory, player, true, true);
  }

  /**
   * Calculates how many of each of the specified construction units can be placed in the specified
   * territory.
   */
  public static int getMaxConstructions(
      final List<ProPurchaseOption> zeroMoveDefensePurchaseOptions) {
    final IntegerMap<String> constructionTypesPerTurn = new IntegerMap<>();
    for (final ProPurchaseOption ppo : zeroMoveDefensePurchaseOptions) {
      if (ppo.isConstruction()) {
        constructionTypesPerTurn.put(ppo.getConstructionType(), ppo.getConstructionTypePerTurn());
      }
    }
    return constructionTypesPerTurn.totalValues();
  }

  private static GamePlayer getOriginalFactoryOwner(
      final Territory territory, final GamePlayer player) {

    final Collection<Unit> factoryUnits = territory.getMatches(Matches.unitCanProduceUnits());
    if (factoryUnits.isEmpty()) {
      throw new IllegalStateException("No factory in territory: " + territory);
    }
    for (final Unit factory2 : factoryUnits) {
      if (player.equals(factory2.getOriginalOwner())) {
        return factory2.getOriginalOwner();
      }
    }
    return CollectionUtils.getAny(factoryUnits).getOriginalOwner();
  }

  /** Comparator that sorts cheaper units before expensive ones. */
  public static Comparator<Unit> getCostComparator(final ProData proData) {
    return Comparator.comparingDouble((unit) -> ProPurchaseUtils.getCost(proData, unit));
  }

  /**
   * How many PU's does it cost the given player to produce the given unit including any dependents.
   */
  public static double getCost(final ProData proData, final Unit unit) {
    final Resource pus = unit.getData().getResourceList().getResource(Constants.PUS).orElse(null);
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
    for (final ProPurchaseTerritory purchaseTerritory : purchaseTerritories.values()) {
      for (final ProPlaceTerritory ppt : purchaseTerritory.getCanPlaceTerritories()) {
        if (t.equals(ppt.getTerritory())) {
          placeUnits.addAll(ppt.getPlaceUnits());
        }
      }
    }
    return placeUnits;
  }

  public static Collection<Unit> getUnitsToConsume(
      GamePlayer player, Collection<Unit> existingUnits, Collection<Unit> unitsToPlace) {
    Collection<Unit> unitsThatConsume =
        CollectionUtils.getMatches(unitsToPlace, Matches.unitConsumesUnitsOnCreation());
    Set<Unit> unitsToConsume = new HashSet<>();
    for (Unit unitToBuild : unitsThatConsume) {
      IntegerMap<UnitType> needed = unitToBuild.getUnitAttachment().getConsumesUnits();
      for (UnitType neededType : needed.keySet()) {
        final Predicate<Unit> matcher =
            Matches.eligibleUnitToConsume(player, neededType).and(u -> !unitsToConsume.contains(u));
        int neededCount = needed.getInt(neededType);
        Collection<Unit> found = CollectionUtils.getNMatches(existingUnits, neededCount, matcher);
        // The caller should have already validated that the required units are present.
        Preconditions.checkState(
            found.size() == neededCount,
            "Not found: " + neededCount + " of " + neededType + " for " + unitsToPlace);
        unitsToConsume.addAll(found);
      }
    }
    return unitsToConsume;
  }
}
