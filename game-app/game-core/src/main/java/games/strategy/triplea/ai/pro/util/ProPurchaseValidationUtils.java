package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProPlaceTerritory;
import games.strategy.triplea.ai.pro.data.ProPurchaseOption;
import games.strategy.triplea.ai.pro.data.ProPurchaseTerritory;
import games.strategy.triplea.ai.pro.data.ProResourceTracker;
import games.strategy.triplea.ai.pro.simulate.ProDummyDelegateBridge;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.AbstractPlaceDelegate;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Pro AI utility methods for finding purchase options and validating which ones a territory can
 * use.
 */
@UtilityClass
public final class ProPurchaseValidationUtils {

  public static List<ProPurchaseOption> findPurchaseOptionsForTerritory(
      final ProData proData,
      final GamePlayer player,
      final List<ProPurchaseOption> purchaseOptions,
      final Territory t,
      final boolean isBid) {
    return ProPurchaseValidationUtils.findPurchaseOptionsForTerritory(
        proData, player, purchaseOptions, t, t, isBid);
  }

  public static List<ProPurchaseOption> findPurchaseOptionsForTerritory(
      final ProData proData,
      final GamePlayer player,
      final List<ProPurchaseOption> purchaseOptions,
      final Territory t,
      final Territory factoryTerritory,
      final boolean isBid) {
    final Predicate<ProPurchaseOption> canUsePurchaseOption =
        ppo -> {
          final List<Unit> units = ppo.getUnitType().createTemp(ppo.getQuantity(), player);
          return ProPurchaseValidationUtils.canUnitsBePlaced(
              proData, units, player, t, factoryTerritory, isBid);
        };
    return purchaseOptions.stream().filter(canUsePurchaseOption).collect(Collectors.toList());
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
    final var placeDelegate =
        (AbstractPlaceDelegate) data.getDelegate(isBid ? "placeBid" : "place");
    if (!isBid
        && !t.equals(factoryTerritory)
        && !units.stream()
            .allMatch(
                Matches.unitWhichRequiresUnitsHasRequiredUnitsInList(
                    placeDelegate.unitsAtStartOfStepInTerritory(factoryTerritory)))) {
      return false;
    }
    final IDelegateBridge bridge = new ProDummyDelegateBridge(proData.getProAi(), player, data);
    placeDelegate.setDelegateBridgeAndPlayer(bridge);
    final String error;
    if (isPlacingFightersOnNewCarriers(t, units)) {
      Collection<Unit> nonAirUnits = CollectionUtils.getMatches(units, Matches.unitIsNotAir());
      error = placeDelegate.canUnitsBePlaced(t, nonAirUnits, player);
    } else {
      error = placeDelegate.canUnitsBePlaced(t, units, player);
    }
    if (error != null) {
      return false;
    }
    return unitsToConsumeAreAllPresent(proData, player, t, units);
  }

  private boolean unitsToConsumeAreAllPresent(
      ProData proData, GamePlayer player, Territory t, Collection<Unit> unitsToBuild) {
    // Check if units that must be consumed are all present, taking into account units that we
    // are already planning to consume.
    IntegerMap<UnitType> requiredUnits = new IntegerMap<>();
    for (Unit unitToBuild : unitsToBuild) {
      requiredUnits.add(unitToBuild.getUnitAttachment().getConsumesUnits());
    }
    if (requiredUnits.isEmpty()) {
      return true;
    }
    IntegerMap<UnitType> eligibleTerritoryUnits = new IntegerMap<>();
    // TODO: This will need to change if consumed units may come from other territories.
    for (Unit u : t.getUnits()) {
      // Don't consider units that we've already marked for use.
      if (!proData.getUnitsToBeConsumed().contains(u)
          && Matches.eligibleUnitToConsume(player, u.getType()).test(u)) {
        eligibleTerritoryUnits.add(u.getType(), 1);
      }
    }
    return eligibleTerritoryUnits.greaterThanOrEqualTo(requiredUnits);
  }

  private static boolean isPlacingFightersOnNewCarriers(final Territory t, final List<Unit> units) {
    return t.isWater()
        && Properties.getProduceFightersOnCarriers(t.getData().getProperties())
        && units.stream().anyMatch(Matches.unitIsAir())
        && units.stream().anyMatch(Matches.unitIsCarrier());
  }

  /** Removes any invalid purchase options from {@code purchaseOptions}. */
  public static void removeInvalidPurchaseOptions(
      final ProData proData,
      final GamePlayer player,
      final GameState data,
      final List<ProPurchaseOption> purchaseOptions,
      final ProResourceTracker resourceTracker,
      final int remainingUnitProduction,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final int remainingConstructions,
      final Territory territory) {
    purchaseOptions.removeIf(
        purchaseOption ->
            !hasEnoughResourcesAndProduction(
                    purchaseOption,
                    resourceTracker,
                    remainingUnitProduction,
                    remainingConstructions)
                || hasReachedMaxUnitBuiltPerPlayer(
                    purchaseOption, player, data, unitsToPlace, purchaseTerritories)
                || hasReachedConstructionLimits(
                    purchaseOption, data, unitsToPlace, purchaseTerritories, territory)
                || !unitsToConsumeAreAllPresent(
                    proData,
                    player,
                    territory,
                    combineLists(unitsToPlace, purchaseOption.createTempUnits())));
  }

  private List<Unit> combineLists(List<Unit> l1, List<Unit> l2) {
    return Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList());
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
      final GameState data,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    // Check max unit limits (-1 is unlimited)
    final int maxBuilt = purchaseOption.getMaxBuiltPerPlayer();
    final UnitType type = purchaseOption.getUnitType();
    if (maxBuilt == 0) {
      return true;
    } else if (maxBuilt > 0) {
      // Find number of unit type that are already built and about to be placed
      final Predicate<Unit> unitTypeOwnedBy =
          Matches.unitIsOfType(type).and(Matches.unitIsOwnedBy(player));
      int currentlyBuilt = CollectionUtils.countMatches(unitsToPlace, unitTypeOwnedBy);
      final List<Territory> allTerritories = data.getMap().getTerritories();
      for (final Territory t : allTerritories) {
        currentlyBuilt += t.getUnitCollection().countMatches(unitTypeOwnedBy);
      }
      for (final ProPurchaseTerritory t : purchaseTerritories.values()) {
        for (final ProPlaceTerritory placeTerritory : t.getCanPlaceTerritories()) {
          currentlyBuilt +=
              CollectionUtils.countMatches(placeTerritory.getPlaceUnits(), unitTypeOwnedBy);
        }
      }
      final int allowedBuild = maxBuilt - currentlyBuilt;
      return allowedBuild - purchaseOption.getQuantity() < 0;
    }
    return false;
  }

  private static boolean hasReachedConstructionLimits(
      final ProPurchaseOption purchaseOption,
      final GameState data,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final Territory territory) {
    if (purchaseOption.isConstruction() && territory != null) {
      final int numConstructionTypeToPlace =
          ProPurchaseValidationUtils.findNumberOfConstructionTypeToPlace(
              purchaseOption, unitsToPlace, purchaseTerritories, territory);
      if (numConstructionTypeToPlace >= purchaseOption.getConstructionTypePerTurn()) {
        return true;
      }

      final int maxConstructionType =
          ProPurchaseValidationUtils.findMaxConstructionTypeAllowed(
              purchaseOption, data, territory);
      final int numExistingConstructionType =
          CollectionUtils.countMatches(
              territory.getUnits(), Matches.unitIsOfType(purchaseOption.getUnitType()));
      return (numConstructionTypeToPlace + numExistingConstructionType) >= maxConstructionType;
    }
    return false;
  }

  private static int findNumberOfConstructionTypeToPlace(
      final ProPurchaseOption purchaseOption,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories,
      final Territory territory) {
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
    return numConstructionTypeToPlace;
  }

  private static int findMaxConstructionTypeAllowed(
      final ProPurchaseOption purchaseOption, final GameState data, final Territory territory) {
    int maxConstructionType = purchaseOption.getMaxConstructionType();
    final String constructionType = purchaseOption.getConstructionType();
    if (!constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
        && !constructionType.endsWith(Constants.CONSTRUCTION_TYPE_STRUCTURE)) {
      if (Properties.getUnlimitedConstructions(data.getProperties())) {
        maxConstructionType = Integer.MAX_VALUE;
      } else if (Properties.getMoreConstructionsWithFactory(data.getProperties())) {
        int production =
            TerritoryAttachment.get(territory).map(TerritoryAttachment::getProduction).orElse(0);
        maxConstructionType = Math.max(maxConstructionType, production);
      }
    }
    return maxConstructionType;
  }
}
