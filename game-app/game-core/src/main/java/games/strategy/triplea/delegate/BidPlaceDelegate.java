package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.move.validation.UnitStackingLimitFilter.PLACEMENT_LIMIT;
import static java.util.function.Predicate.not;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.move.validation.UnitStackingLimitFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/** Logic for unit placement when bid mode is active. */
public class BidPlaceDelegate extends AbstractPlaceDelegate {
  // Allow production of any number of units
  @Override
  protected @Nullable String checkProduction(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
    return null;
  }

  // Return whether we can place bid in a certain territory
  @Override
  protected @Nullable String canProduce(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
    return canProduce(to, to, units, player);
  }

  @Override
  protected @Nullable String canProduce(
      final Territory producer,
      final Territory to,
      final Collection<Unit> units,
      final GamePlayer player) {
    // we can place if no enemy units and its water
    if (to.isWater()) {
      if (units.stream().anyMatch(Matches.unitIsLand())) {
        return "Can't place land units at sea";
      } else if (to.anyUnitsMatch(Matches.enemyUnit(player))) {
        return "Can't place in sea zone containing enemy units";
      } else if (!to.anyUnitsMatch(Matches.unitIsOwnedBy(player))) {
        return "Can't place in sea zone that does not contain a unit owned by you";
      } else {
        return null;
      }
    }

    // we can place on territories we own
    if (units.stream().anyMatch(Matches.unitIsSea())) {
      return "Can't place sea units on land";
    } else if (!to.isOwnedBy(player)) {
      final PlayerAttachment pa = PlayerAttachment.get(to.getOwner());
      if (pa != null && pa.getGiveUnitControl().contains(player)) {
        return null;
      } else if (to.anyUnitsMatch(Matches.unitIsOwnedBy(player))) {
        return null;
      }
      return "You don't own " + to.getName();
    } else {
      return null;
    }
  }

  @Override
  protected int getMaxUnitsToBePlaced(
      final @Nullable Collection<Unit> units, final Territory to, final GamePlayer player) {
    if (units == null) {
      return -1;
    }
    return units.size();
  }

  @Override
  protected int getMaxUnitsToBePlacedFrom(
      final Territory producer,
      final @Nullable Collection<Unit> units,
      final Territory to,
      final GamePlayer player,
      final boolean countSwitchedProductionToNeighbors,
      final Collection<Territory> notUsableAsOtherProducers,
      final Map<Territory, Integer> currentAvailablePlacementForOtherProducers) {
    if (units == null) {
      return -1;
    }
    return units.size();
  }

  // Return collection of bid units which can placed in a land territory
  @Override
  protected Collection<Unit> getUnitsToBePlaced(
      final Territory to, final Collection<Unit> units, final GamePlayer player) {
    if (to.isWater()) {
      return super.getUnitsToBePlaced(to, units, player);
    }
    final Collection<Unit> unitsAtStartOfTurnInTo = unitsAtStartOfStepInTerritory(to);
    final Collection<Unit> placeableUnits = new ArrayList<>();
    // we add factories and constructions later
    final Predicate<Unit> groundUnits = Matches.unitIsLand().and(Matches.unitIsNotConstruction());
    final Predicate<Unit> airUnits = Matches.unitIsAir().and(Matches.unitIsNotConstruction());
    placeableUnits.addAll(CollectionUtils.getMatches(units, groundUnits));
    placeableUnits.addAll(CollectionUtils.getMatches(units, airUnits));
    if (units.stream().anyMatch(Matches.unitIsConstruction())) {
      final IntegerMap<String> constructionsMap =
          howManyOfEachConstructionCanPlace(to, to, units, player);
      final Collection<Unit> skipUnit = new ArrayList<>();
      for (final Unit currentUnit :
          CollectionUtils.getMatches(units, Matches.unitIsConstruction())) {
        final int maxUnits = howManyOfConstructionUnit(currentUnit, constructionsMap);
        if (maxUnits > 0) {
          // we are doing this because we could have multiple unitTypes with the same
          // constructionType, so we have to be able to place the max placement by constructionType
          // of each unitType
          if (skipUnit.contains(currentUnit)) {
            continue;
          }
          placeableUnits.addAll(
              CollectionUtils.getNMatches(
                  units, maxUnits, Matches.unitIsOfType(currentUnit.getType())));
          skipUnit.addAll(
              CollectionUtils.getMatches(units, Matches.unitIsOfType(currentUnit.getType())));
        }
      }
    }
    // remove any units that require other units to be consumed on creation (veqryn)
    placeableUnits.removeIf(
        Matches.unitConsumesUnitsOnCreation()
            .and(not(Matches.unitWhichConsumesUnitsHasRequiredUnits(unitsAtStartOfTurnInTo))));
    // now check stacking limits
    return applyStackingLimitsPerUnitType(placeableUnits, to);
  }

  @Override
  protected List<Territory> getAllProducers(
      final Territory to, final GamePlayer player, final Collection<Unit> unitsToPlace) {
    final List<Territory> producers = new ArrayList<>();
    producers.add(to);
    return producers;
  }

  @Override
  protected Predicate<Unit> unitWhichRequiresUnitsHasRequiredUnits(
      final Territory to, final boolean countNeighbors) {
    // Ignore "require units" for bid placements, since that's used for custom factory types, which
    // bids should be ignoring.
    return u -> true;
  }
}
