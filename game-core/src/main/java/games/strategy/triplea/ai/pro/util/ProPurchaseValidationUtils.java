package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.engine.data.GamePlayer;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import org.triplea.java.collections.CollectionUtils;

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
    final List<ProPurchaseOption> result = new ArrayList<>();
    for (final ProPurchaseOption ppo : purchaseOptions) {
      if (ProPurchaseValidationUtils.canTerritoryUsePurchaseOption(
          proData, player, ppo, t, factoryTerritory, isBid)) {
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
    final List<Unit> units = ppo.getUnitType().createTemp(ppo.getQuantity(), player);
    return ProPurchaseValidationUtils.canUnitsBePlaced(
        proData, units, player, t, factoryTerritory, isBid);
  }

  public static boolean canUnitsBePlaced(
      final ProData proData,
      final List<Unit> units,
      final GamePlayer player,
      final Territory t,
      final boolean isBid) {
    return ProPurchaseValidationUtils.canUnitsBePlaced(proData, units, player, t, t, isBid);
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
        && Properties.getProduceFightersOnCarriers(t.getData().getProperties())
        && units.stream().anyMatch(Matches.unitIsAir())
        && units.stream().anyMatch(Matches.unitIsCarrier());
  }

  public static void removeInvalidPurchaseOptions(
      final GamePlayer player,
      final GameDataInjections data,
      final List<ProPurchaseOption> purchaseOptions,
      final ProResourceTracker resourceTracker,
      final int remainingUnitProduction,
      final List<Unit> unitsToPlace,
      final Map<Territory, ProPurchaseTerritory> purchaseTerritories) {
    ProPurchaseValidationUtils.removeInvalidPurchaseOptions(
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
      final GameDataInjections data,
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
              purchaseOption, resourceTracker, remainingUnitProduction, remainingConstructions)
          || hasReachedMaxUnitBuiltPerPlayer(
              purchaseOption, player, data, unitsToPlace, purchaseTerritories)
          || hasReachedConstructionLimits(
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
      final GameDataInjections data,
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
      if (allowedBuild - purchaseOption.getQuantity() < 0) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasReachedConstructionLimits(
      final ProPurchaseOption purchaseOption,
      final GameDataInjections data,
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
      if ((numConstructionTypeToPlace + numExistingConstructionType) >= maxConstructionType) {
        return true;
      }
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
      final ProPurchaseOption purchaseOption,
      final GameDataInjections data,
      final Territory territory) {

    int maxConstructionType = purchaseOption.getMaxConstructionType();
    final String constructionType = purchaseOption.getConstructionType();
    if (!constructionType.equals(Constants.CONSTRUCTION_TYPE_FACTORY)
        && !constructionType.endsWith(Constants.CONSTRUCTION_TYPE_STRUCTURE)) {
      if (Properties.getUnlimitedConstructions(data.getProperties())) {
        maxConstructionType = Integer.MAX_VALUE;
      } else if (Properties.getMoreConstructionsWithFactory(data.getProperties())) {
        int production = 0;
        final TerritoryAttachment terrAttachment = TerritoryAttachment.get(territory);
        if (terrAttachment != null) {
          production = terrAttachment.getProduction();
        }
        maxConstructionType = Math.max(maxConstructionType, production);
      }
    }
    return maxConstructionType;
  }
}
